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
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class WorkOrderConfigurationService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderConfigurationService.class);
    @Autowired
    private WorkOrderConfigurationRepository workOrderConfigurationRepository;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient layoutServiceRestemplateClient;
    @Autowired
    private FileService fileService;
    @Value("${fileupload.test-data.work-order-configuration:work-order-configuration}")
    String testDataFile;


    public WorkOrderConfiguration findById(Long id) {
        return workOrderConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("web client tab display configuration not found by id: " + id));
    }

    public List<WorkOrderConfiguration> findAll(Long companyId,
                                                Long warehouseId) {

        return workOrderConfigurationRepository.findAll(
                (Root<WorkOrderConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(companyId)) {
                        predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    }
                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }
    public WorkOrderConfiguration save(WorkOrderConfiguration workOrderConfiguration) {
        return workOrderConfigurationRepository.save(workOrderConfiguration);
    }



    public List<WorkOrderConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("materialConsumeTiming").
                addColumn("overConsumeIsAllowed").
                addColumn("overProduceIsAllowed").
                build().withHeader();

        return fileService.loadData(inputStream, schema, WorkOrderConfigurationCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = companyId == null ?
                    "" : layoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            logger.debug("Start to init web client tab display configuration from {}",
                    testDataFileName);
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<WorkOrderConfigurationCSVWrapper> workOrderConfigurationCSVWrappers = loadData(inputStream);

            workOrderConfigurationCSVWrappers.stream().forEach(
                    workOrderConfigurationCSVWrapper ->
                            save(convertFromCSVWrapper(workOrderConfigurationCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private WorkOrderConfiguration convertFromCSVWrapper(
            WorkOrderConfigurationCSVWrapper workOrderConfigurationCSVWrapper) {
        WorkOrderConfiguration workOrderConfiguration = new WorkOrderConfiguration();
        workOrderConfiguration.setMaterialConsumeTiming(
                WorkOrderMaterialConsumeTiming.valueOf(
                        workOrderConfigurationCSVWrapper.getMaterialConsumeTiming()
                ));
        workOrderConfiguration.setOverConsumeIsAllowed(
                workOrderConfigurationCSVWrapper.isOverConsumeIsAllowed());
        workOrderConfiguration.setOverProduceIsAllowed(
                workOrderConfigurationCSVWrapper.isOverProduceIsAllowed());

        if (StringUtils.isNotBlank(workOrderConfigurationCSVWrapper.getCompany())) {
            Company company = layoutServiceRestemplateClient.getCompanyByCode(
                    workOrderConfigurationCSVWrapper.getCompany()
            );
            if (Objects.nonNull(company)) {
                workOrderConfiguration.setCompanyId(company.getId());
                // only set the warehouse if the warehouse name and company name are
                // all present
                if (StringUtils.isNotBlank(workOrderConfigurationCSVWrapper.getWarehouse())) {
                    Warehouse warehouse = layoutServiceRestemplateClient.getWarehouseByName(
                            company.getCode(), workOrderConfigurationCSVWrapper.getWarehouse()
                    );
                    if (Objects.nonNull(warehouse)) {
                        workOrderConfiguration.setWarehouseId(warehouse.getId());
                    }
                }
            }
        }

        return workOrderConfiguration;


    }


    public WorkOrderConfiguration getWorkOrderConfiguration(Long companyId, Long warehouseId) {
        // we will start with most specific configuration until we get the most generic config
        // most specific -> most generic
        // 1. company + warehouse
        // 2. company
        // 3. default

        List<WorkOrderConfiguration> workOrderConfigurations = new ArrayList<>();
        // 1. company + warehouse
        workOrderConfigurations = findAll(companyId, warehouseId);
        if (workOrderConfigurations.size() >= 1) {
            return workOrderConfigurations.get(0);
        }
        // 2. company
        workOrderConfigurations = findAll(companyId, null);
        if (workOrderConfigurations.size() >= 1) {
            return workOrderConfigurations.get(0);
        }
        // 3. default
        workOrderConfigurations = findAll(null, null);
        if (workOrderConfigurations.size() >= 1) {
            return workOrderConfigurations.get(0);
        }

        return null;
    }


}