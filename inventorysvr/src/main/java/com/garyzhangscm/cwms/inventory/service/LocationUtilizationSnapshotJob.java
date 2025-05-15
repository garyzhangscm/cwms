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

import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.model.Company;
import com.garyzhangscm.cwms.inventory.model.InventorySnapshotConfiguration;
import com.garyzhangscm.cwms.inventory.model.Warehouse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

@Component
public class LocationUtilizationSnapshotJob {

    private static final Logger logger = LoggerFactory.getLogger(LocationUtilizationSnapshotJob.class);
    @Autowired
    private LocationUtilizationSnapshotBatchService locationUtilizationSnapshotBatchService;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    @Autowired
    private InventorySnapshotConfigurationService inventorySnapshotConfigurationService;

    @Scheduled(cron = "0 0 0-23 * * ?")
    public void generateLocationUtilizationSnapshot() {


        logger.debug("start to automatically generate the location utilization snapshot");
        List<Company> companies
                = warehouseLayoutServiceRestemplateClient.getAllCompanies();
        companies.forEach(
                company -> generateLocationUtilizationSnapshot(company)
        );
    }
    public void generateLocationUtilizationSnapshot(Company company) {
        warehouseLayoutServiceRestemplateClient.getWarehouseByCompany(company.getId())
                .forEach(
                        warehouse -> generateLocationUtilizationSnapshot(company, warehouse)
                );
    }
    public void generateLocationUtilizationSnapshot(Company company, Warehouse warehouse) {
        if (!isTimingForSnapshot(warehouse)) {
            logger.debug("current time: {} is not setup for location utilization snapshot");
            return;
        }

        logger.debug("start to generate location utilization snapshot for warehouse {} / {}, id: {}",
                company.getName(),
                warehouse.getName(),
                warehouse.getId());

        locationUtilizationSnapshotBatchService.generateLocationUtilizationSnapshotBatch(
                warehouse.getId()
        );

    }

    private boolean isTimingForSnapshot(Warehouse warehouse) {
        InventorySnapshotConfiguration inventorySnapshotConfiguration =
                inventorySnapshotConfigurationService.findByWarehouseId(warehouse.getId());
        if (Objects.isNull(inventorySnapshotConfiguration)) {
            logger.debug("inventory snapshot configuration is not setup for warehouse {} of id {}",
                    warehouse.getName(), warehouse.getId());
            return false;
        }
        // get the timing from the configuration. It is saved as hour in UTC
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        logger.debug("start to compare current hour vs location utilization configuration {}, {}, {}",
                now.getHour(),
                inventorySnapshotConfiguration.getLocationUtilizationSnapshotTiming1(),
                inventorySnapshotConfiguration.getLocationUtilizationSnapshotTiming2(),
                inventorySnapshotConfiguration.getLocationUtilizationSnapshotTiming3());

        return (Objects.nonNull(inventorySnapshotConfiguration.getLocationUtilizationSnapshotTiming1()) &&
                now.getHour() ==  inventorySnapshotConfiguration.getLocationUtilizationSnapshotTiming1())
                ||
                (Objects.nonNull(inventorySnapshotConfiguration.getLocationUtilizationSnapshotTiming2()) &&
                        now.getHour() ==  inventorySnapshotConfiguration.getLocationUtilizationSnapshotTiming2())
                ||
                (Objects.nonNull(inventorySnapshotConfiguration.getLocationUtilizationSnapshotTiming3()) &&
                        now.getHour() ==  inventorySnapshotConfiguration.getLocationUtilizationSnapshotTiming3());

    }
}
