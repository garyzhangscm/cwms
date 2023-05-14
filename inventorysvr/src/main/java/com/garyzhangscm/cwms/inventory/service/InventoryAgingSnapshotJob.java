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
import com.garyzhangscm.cwms.inventory.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

@Component
public class InventoryAgingSnapshotJob {

    private static final Logger logger = LoggerFactory.getLogger(InventoryAgingSnapshotJob.class);
    @Autowired
    private InventoryAgingSnapshotService inventoryAgingSnapshotService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    @Autowired
    private InventorySnapshotConfigurationService inventorySnapshotConfigurationService;


    @Scheduled(cron = "0 0 0-23 * * ?")
    public void generateInventoryAgingSnapshot() {
        logger.debug("start to automatically generate the inventory aging snapshot");
        List<Company> companies
                = warehouseLayoutServiceRestemplateClient.getAllCompanies();
        companies.forEach(
                company -> generateInventoryAgingSnapshot(company)
        );
    }
    public void generateInventoryAgingSnapshot(Company company) {
        logger.debug("see if we will need to generate inventory aging snapshot for company {} / {}",
                company.getId(), company.getName());
        warehouseLayoutServiceRestemplateClient.getWarehouseByCompany(company.getId())
                .forEach(
                        warehouse -> generateInventoryAgingSnapshot(company, warehouse)
                );
    }
    public void generateInventoryAgingSnapshot(Company company, Warehouse warehouse) {
        logger.debug("see if we will need to generate inventory aging snapshot for warehouse {} / {} from company {} / {}",
                warehouse.getId(), warehouse.getName(),
                company.getId(), company.getName());
        if (!isTimingForSnapshot(warehouse)) {
            logger.debug("current time: {} is not setup for inventory aging snapshot");
            return;
        }

        logger.debug("start to generate inventory aging snapshot for warehouse {} / {}, id: {}",
                company.getName(),
                warehouse.getName(),
                warehouse.getId());

        inventoryAgingSnapshotService.generateInventoryAgingSnapshot(warehouse.getId());

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
        logger.debug("start to compare current hour {} vs inventory aging configuration {}, {}, {}",
                now.getHour(),
                inventorySnapshotConfiguration.getInventoryAgingSnapshotTiming1(),
                inventorySnapshotConfiguration.getInventoryAgingSnapshotTiming2(),
                inventorySnapshotConfiguration.getInventoryAgingSnapshotTiming3());

        return
                (Objects.nonNull(inventorySnapshotConfiguration.getInventoryAgingSnapshotTiming1()) &&
                        now.getHour() ==  inventorySnapshotConfiguration.getInventoryAgingSnapshotTiming1())
                        ||
                (Objects.nonNull(inventorySnapshotConfiguration.getInventoryAgingSnapshotTiming2()) &&
                        now.getHour() ==  inventorySnapshotConfiguration.getInventoryAgingSnapshotTiming2())
                        ||
                (Objects.nonNull(inventorySnapshotConfiguration.getInventoryAgingSnapshotTiming3()) &&
                        now.getHour() ==  inventorySnapshotConfiguration.getInventoryAgingSnapshotTiming3());
    }
}
