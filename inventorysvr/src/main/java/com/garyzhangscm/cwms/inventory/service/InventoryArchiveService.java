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

import com.garyzhangscm.cwms.inventory.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventoryArchiveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class InventoryArchiveService {
    private static final Logger logger = LoggerFactory.getLogger(InventoryArchiveService.class);

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    @Autowired
    private InventoryService inventoryService;

    private final static int MAX_ARCHIVE_INVENTORY_RECORD = 2000;

    @Autowired
    private InventoryArchiveRepository inventoryArchiveRepository;

    public InventoryArchive save(InventoryArchive inventoryArchive) {
        return inventoryArchiveRepository.save(inventoryArchive);
    }


    @Scheduled(cron = "0 0 1 * * ?")
    // @Scheduled(cron = "0 0,5,10,15,20,25,30,35,40,45,50,55 * * * ?")
    public void archiveInventory() {
        logger.debug("start to archive inventory");
        List<Company> companies
                = warehouseLayoutServiceRestemplateClient.getAllCompanies();
        companies.forEach(
                company -> archiveInventory(company)
        );
    }
    public void archiveInventory(Company company) {
        warehouseLayoutServiceRestemplateClient.getWarehouseByCompany(company.getId())
                .forEach(
                        warehouse -> archiveInventory(company, warehouse)
                );
    }
    public void archiveInventory(Company company, Warehouse warehouse) {
        ArchiveConfiguration archiveConfiguration =
                resourceServiceRestemplateClient.getArchiveConfiguration(warehouse.getId());
        if (Objects.isNull(archiveConfiguration)) {
            logger.debug("Archive is not setup for warehouse {}, company {}",
                    warehouse.getName(), company.getName());
            return;
        }

        if (!Boolean.TRUE.equals(archiveConfiguration.getInventoryArchiveEnabled())) {

            logger.debug("Inventory Archive is not enabled for warehouse {}, company {}",
                    warehouse.getName(), company.getName());
            return;
        }

        if (Objects.isNull(archiveConfiguration.getRemovedInventoryArchiveDays()) ||
            archiveConfiguration.getRemovedInventoryArchiveDays() <= 0) {

            logger.debug("Removed Inventory Archive Days is not setup correctly(value: {}) for warehouse {}, company {}",
                    Objects.isNull(archiveConfiguration.getRemovedInventoryArchiveDays()) ? "N/A" :
                            archiveConfiguration.getRemovedInventoryArchiveDays(),
                    warehouse.getName(), company.getName());
        }
        else {

            archiveRemovedInventory(company, warehouse, archiveConfiguration.getRemovedInventoryArchiveDays());
        }

    }

    private void archiveRemovedInventory(Company company, Warehouse warehouse, int removedInventoryArchiveDays) {

        logger.debug("Start to archive removed inventory that removed more than {} days ago for warehouse {}, company {}",
                removedInventoryArchiveDays,
                warehouse.getName(), company.getName());

        // get the location for removed inventory
        Location location
                = warehouseLayoutServiceRestemplateClient.getLocationForInventoryAdjustment(
                        warehouse.getId());
        // get all the inventory
        List<Inventory> inventories = inventoryService.findByLocationId(location.getId(), false);
        location
                = warehouseLayoutServiceRestemplateClient.getDefaultRemovedInventoryLocation( warehouse.getId());
        inventories.addAll(inventoryService.findByLocationId(location.getId(), false));

        logger.debug("get {} inventory records to be archived", inventories.size());

        int index = 1;
        for (Inventory inventory : inventories) {

            InventoryArchive inventoryArchive = new InventoryArchive(inventory);
            save(inventoryArchive);

            inventoryService.delete(inventory.getId());

            logger.debug("{}. Inventory id {}, LPN {} is archived!",
                    index++, inventory.getId(), inventory.getLpn());

            if (index > MAX_ARCHIVE_INVENTORY_RECORD) {
                logger.debug("inventory archive records each time is capped at {}", MAX_ARCHIVE_INVENTORY_RECORD);
                break;
            }
        }

    }

}
