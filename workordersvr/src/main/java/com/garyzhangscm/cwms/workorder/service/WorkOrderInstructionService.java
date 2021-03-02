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
import com.garyzhangscm.cwms.workorder.repository.WorkOrderInstructionRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class WorkOrderInstructionService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderInstructionService.class);

    @Autowired
    private WorkOrderInstructionRepository workOrderInstructionRepository;
    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.work-order-instruction:work-order-instruction}")
    String testDataFile;


    public WorkOrderInstruction findById(Long id) {
        return workOrderInstructionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order instruction not found by id: " + id));
    }


    public List<WorkOrderInstruction> findAll() {
        return  workOrderInstructionRepository.findAll();

    }

    private WorkOrderInstruction findByNumber(String workOrderNumber, Integer sequence) {
        return workOrderInstructionRepository.findByNumber(workOrderNumber, sequence);

    }

    public WorkOrderInstruction save(WorkOrderInstruction workOrderInstruction) {
        return workOrderInstructionRepository.save(workOrderInstruction);

    }

    public WorkOrderInstruction saveOrUpdate(WorkOrderInstruction workOrderInstruction) {
        String workOrderNumber = workOrderInstruction.getWorkOrder().getNumber();
        Integer sequence = workOrderInstruction.getSequence();

        if (workOrderInstruction.getId() == null
                && findByNumber(workOrderNumber, sequence) != null) {
            workOrderInstruction.setId(
                    findByNumber(workOrderNumber, sequence).getId());
        }
        return save(workOrderInstruction);
    }


    public void delete(WorkOrderInstruction workOrderInstruction) {
        workOrderInstructionRepository.delete(workOrderInstruction);
    }

    public void delete(Long id) {
        workOrderInstructionRepository.deleteById(id);
    }

    public void delete(String workOrderInstructionIds) {
        if (!workOrderInstructionIds.isEmpty()) {
            long[] workOrderInstructionIdArray = Arrays.asList(workOrderInstructionIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : workOrderInstructionIdArray) {
                delete(id);
            }
        }
    }

    public List<WorkOrderInstructionCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("workOrder").
                addColumn("sequence").
                addColumn("instruction").
                addColumn("warehouse").
                build().withHeader();

        return fileService.loadData(inputStream, schema, WorkOrderInstructionCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<WorkOrderInstructionCSVWrapper> workOrderInstructionCSVWrappers = loadData(inputStream);
            workOrderInstructionCSVWrappers.stream()
                    .forEach(workOrderInstructionCSVWrapper -> saveOrUpdate(convertFromWrapper(workOrderInstructionCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private WorkOrderInstruction convertFromWrapper(WorkOrderInstructionCSVWrapper workOrderInstructionCSVWrapper) {

        WorkOrderInstruction workOrderInstruction = new WorkOrderInstruction();

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                workOrderInstructionCSVWrapper.getCompany(),
                workOrderInstructionCSVWrapper.getWarehouse()
        );

        workOrderInstruction.setWorkOrder(
                workOrderService.findByNumber(warehouse.getId(), workOrderInstructionCSVWrapper.getWorkOrder()));

        workOrderInstruction.setSequence(workOrderInstructionCSVWrapper.getSequence());
        workOrderInstruction.setInstruction(workOrderInstructionCSVWrapper.getInstruction());

        return workOrderInstruction;
    }

    public WorkOrderInstruction createWorkOrderInstructionFromBOMLine(WorkOrder workOrder, WorkOrderInstructionTemplate workOrderInstructionTemplate) {
        WorkOrderInstruction workOrderInstruction = new WorkOrderInstruction();
        workOrderInstruction.setWorkOrder(workOrder);
        workOrderInstruction.setSequence(workOrderInstructionTemplate.getSequence());
        workOrderInstruction.setInstruction(workOrderInstructionTemplate.getInstruction());

        return save(workOrderInstruction);


    }

}
