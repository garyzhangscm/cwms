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
public class InventorySnapshotJob {

    private static final Logger logger = LoggerFactory.getLogger(InventorySnapshotJob.class);
    @Autowired
    private InventorySnapshotService inventorySnapshotService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    @Autowired
    private InventorySnapshotConfigurationService inventorySnapshotConfigurationService;


    @Scheduled(cron = "0 0 0-23 * * ?")
    public void generateInventorySnapshot() {
        logger.debug("start to automatically generate the inventory snapshot");
        List<Company> companies
                = warehouseLayoutServiceRestemplateClient.getAllCompanies();
        companies.forEach(
                company -> generateInventorySnapshot(company)
        );
    }
    public void generateInventorySnapshot(Company company) {
        logger.debug("see if we will need to generate inventory snapshot for company {} / {}",
                company.getId(), company.getName());
        warehouseLayoutServiceRestemplateClient.getWarehouseByCompany(company.getId())
                .forEach(
                        warehouse -> generateInventorySnapshot(company, warehouse)
                );
    }
    public void generateInventorySnapshot(Company company, Warehouse warehouse) {

        logger.debug("see if we will need to generate inventory snapshot for warehouse {} / {} from company {} / {}",
                warehouse.getId(), warehouse.getName(),
                company.getId(), company.getName());
        if (!isTimingForSnapshot(warehouse)) {
            logger.debug("current time: {} is not setup for inventory snapshot");
            return;
        }

        logger.debug("start to generate inventory snapshot for warehouse {} / {}, id: {}",
                company.getName(),
                warehouse.getName(),
                warehouse.getId());

        inventorySnapshotService.generateInventorySnapshot(warehouse.getId());

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
        logger.debug("start to compare current hour {} vs inventory snapshot configuration {}, {}, {}",
                now.getHour(),
                inventorySnapshotConfiguration.getInventorySnapshotTiming1(),
                inventorySnapshotConfiguration.getInventorySnapshotTiming2(),
                inventorySnapshotConfiguration.getInventorySnapshotTiming3());

        return (Objects.nonNull(inventorySnapshotConfiguration.getInventorySnapshotTiming1()) &&
                now.getHour() ==  inventorySnapshotConfiguration.getInventorySnapshotTiming1())
                ||
                (Objects.nonNull(inventorySnapshotConfiguration.getInventorySnapshotTiming2()) &&
                        now.getHour() ==  inventorySnapshotConfiguration.getInventorySnapshotTiming2())
                ||
                (Objects.nonNull(inventorySnapshotConfiguration.getInventorySnapshotTiming3()) &&
                        now.getHour() ==  inventorySnapshotConfiguration.getInventorySnapshotTiming3());
        
    }
}
