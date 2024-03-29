/**
 * Copyright 2018
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.workorder.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderInstructionTemplateRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;


@Service
public class WorkOrderInstructionTemplateService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderInstructionTemplateService.class);

    @Autowired
    private WorkOrderInstructionTemplateRepository workOrderInstructionTemplateRepository;
    @Autowired
    private BillOfMaterialService billOfMaterialService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.work-order-instruction-template:work-order-instruction-template}")
    String testDataFile;


    public WorkOrderInstructionTemplate findById(Long id) {
        return workOrderInstructionTemplateRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order instruction template not found by id: " + id));
    }


    public List<WorkOrderInstructionTemplate> findAll() {
        return  workOrderInstructionTemplateRepository.findAll();

    }

    private WorkOrderInstructionTemplate findByNumber(String billOfMaterialNumber, Integer sequence) {
        return workOrderInstructionTemplateRepository.findByNumber(billOfMaterialNumber, sequence);

    }

    public WorkOrderInstructionTemplate save(WorkOrderInstructionTemplate workOrderInstructionTemplate) {
        return workOrderInstructionTemplateRepository.save(workOrderInstructionTemplate);

    }

    public WorkOrderInstructionTemplate saveOrUpdate(WorkOrderInstructionTemplate workOrderInstructionTemplate) {
        String billOfMaterialNumber = workOrderInstructionTemplate.getBillOfMaterial().getNumber();
        Integer sequence = workOrderInstructionTemplate.getSequence();
        if (workOrderInstructionTemplate.getId() == null
                && findByNumber(billOfMaterialNumber, sequence) != null) {
            workOrderInstructionTemplate.setId(
                    findByNumber(billOfMaterialNumber, sequence).getId());
        }
        return save(workOrderInstructionTemplate);
    }


    public void delete(WorkOrderInstructionTemplate workOrderInstructionTemplate) {
        workOrderInstructionTemplateRepository.delete(workOrderInstructionTemplate);
    }

    public void delete(Long id) {
        workOrderInstructionTemplateRepository.deleteById(id);
    }

    public void delete(String workOrderInstructionTemplateIds) {
        if (!workOrderInstructionTemplateIds.isEmpty()) {
            long[] workOrderInstructionTemplateIdArray = Arrays.asList(workOrderInstructionTemplateIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : workOrderInstructionTemplateIdArray) {
                delete(id);
            }
        }
    }

    public List<WorkOrderInstructionTemplateCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("billOfMaterial").
                addColumn("sequence").
                addColumn("instruction").
                build().withHeader();

        return fileService.loadData(inputStream, schema, WorkOrderInstructionTemplateCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<WorkOrderInstructionTemplateCSVWrapper> workOrderInstructionTemplateCSVWrappers = loadData(inputStream);
            workOrderInstructionTemplateCSVWrappers.stream()
                    .forEach(workOrderInstructionTemplateCSVWrapper -> saveOrUpdate(convertFromWrapper(workOrderInstructionTemplateCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private WorkOrderInstructionTemplate convertFromWrapper(WorkOrderInstructionTemplateCSVWrapper workOrderInstructionTemplateCSVWrapper) {

        WorkOrderInstructionTemplate workOrderInstructionTemplate = new WorkOrderInstructionTemplate();

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                workOrderInstructionTemplateCSVWrapper.getCompany(),
                workOrderInstructionTemplateCSVWrapper.getWarehouse());
        workOrderInstructionTemplate.setBillOfMaterial(
                billOfMaterialService.findByNumber(warehouse.getId(), workOrderInstructionTemplateCSVWrapper.getBillOfMaterial()));

        workOrderInstructionTemplate.setSequence(workOrderInstructionTemplateCSVWrapper.getSequence());
        workOrderInstructionTemplate.setInstruction(workOrderInstructionTemplateCSVWrapper.getInstruction());

        return workOrderInstructionTemplate;
    }


}
