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

package com.garyzhangscm.cwms.inventory.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.inventory.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.InboundServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.OutbuondServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventorySnapshotConfigurationRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 *
 */
@Service
public class InventorySnapshotConfigurationService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(InventorySnapshotConfigurationService.class);

    @Autowired
    private InventorySnapshotConfigurationRepository inventorySnapshotConfigurationRepository;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;
    @Autowired
    private InboundServiceRestemplateClient inboundServiceRestemplateClient;
    @Autowired
    private IntegrationService integrationService;
    @Autowired
    private FileService fileService;


    // bean that we will need to update the task schedule
    // when we change the inventory snapshot or location utilization snapshot
    // schedule

    // @Autowired
    // private DynamicSchedulingConfig dynamicSchedulingConfig;

    @Value("${fileupload.test-data.inventory-snapshot-configuration:inventory-snapshot-configuration}")
    String testDataFile;

    public InventorySnapshotConfiguration findById(Long id) {
        return findById(id, true);
    }
    public InventorySnapshotConfiguration findById(Long id, boolean includeDetails) {
        InventorySnapshotConfiguration inventorySnapshotConfiguration = inventorySnapshotConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("inventory snapshot not found by id: " + id));
        return inventorySnapshotConfiguration;
    }


    public InventorySnapshotConfiguration save(InventorySnapshotConfiguration inventorySnapshotConfiguration) {
        return inventorySnapshotConfigurationRepository.save(inventorySnapshotConfiguration);
        /***
        InventorySnapshotConfiguration newInventorySnapshotConfiguration =
                inventorySnapshotConfigurationRepository.save(inventorySnapshotConfiguration);
        if (Objects.nonNull(dynamicSchedulingConfig.getCurrentScheduledTaskRegistrar())) {
            logger.debug("We already have setup the dynamic scheduling config, let's refresh it");

            dynamicSchedulingConfig.configureTasks(dynamicSchedulingConfig.getCurrentScheduledTaskRegistrar());
        }
        return newInventorySnapshotConfiguration;
         **/
    }

    public InventorySnapshotConfiguration saveOrUpdate(InventorySnapshotConfiguration inventorySnapshotConfiguration) {
        if (inventorySnapshotConfiguration.getId() == null &&
                findByWarehouseId(inventorySnapshotConfiguration.getWarehouseId()) != null) {
            inventorySnapshotConfiguration.setId(
                    findByWarehouseId(inventorySnapshotConfiguration.getWarehouseId()).getId());
        }
        return save(inventorySnapshotConfiguration);
    }

    public InventorySnapshotConfiguration  findByWarehouseId(Long warehouseId) {
        return inventorySnapshotConfigurationRepository.findByWarehouseId(warehouseId);
    }

    public List<InventorySnapshotConfiguration> findAll(Long warehouseId) {

        List<InventorySnapshotConfiguration> inventorySnapshotConfigurations
                =  inventorySnapshotConfigurationRepository.findAll(
                  (Root<InventorySnapshotConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();


                    if (Objects.nonNull(warehouseId)) {

                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        return inventorySnapshotConfigurations;
    }


    public List<InventorySnapshotConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("cron").
                build().withHeader();

        return fileService.loadData(inputStream, schema, InventorySnapshotConfigurationCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<InventorySnapshotConfigurationCSVWrapper> inventorySnapshotConfigurationCSVWrappers = loadData(inputStream);
            inventorySnapshotConfigurationCSVWrappers.stream()
                    .forEach(inventorySnapshotConfigurationCSVWrapper -> {
                        saveOrUpdate(convertFromWrapper(inventorySnapshotConfigurationCSVWrapper));

            });

        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private InventorySnapshotConfiguration convertFromWrapper(InventorySnapshotConfigurationCSVWrapper inventorySnapshotConfigurationCSVWrapper) {
        InventorySnapshotConfiguration inventorySnapshotConfiguration = new InventorySnapshotConfiguration();
        inventorySnapshotConfiguration.setCron(inventorySnapshotConfigurationCSVWrapper.getCron());

        // warehouse is a mandate field
        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                        inventorySnapshotConfigurationCSVWrapper.getCompany(),
                        inventorySnapshotConfigurationCSVWrapper.getWarehouse());
        inventorySnapshotConfiguration.setWarehouseId(warehouse.getId());

        return inventorySnapshotConfiguration;

    }

}