/**
 * Copyright 2019
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TestDataInitService {
    private static final Logger logger = LoggerFactory.getLogger(TestDataInitService.class);
    @Autowired
    private JdbcTemplate jdbcTemplate;
    ItemService itemService;

    ItemFamilyService itemFamilyService;

    ItemPackageTypeService itemPackageTypeService;

    ItemUnitOfMeasureService itemUnitOfMeasureService;

    InventoryStatusService inventoryStatusService;

    InventoryService inventoryService;

    MovementPathService movementPathService;
    InventoryAdjustmentThresholdService inventoryAdjustmentThresholdService;

    InventorySnapshotConfigurationService inventorySnapshotConfigurationService;

    Map<String, TestDataInitiableService> initiableServices = new HashMap<>();
    List<String> serviceNames = new ArrayList<>();
    @Autowired
    public TestDataInitService(ItemFamilyService itemFamilyService,
                               ItemService itemService,
                               ItemUnitOfMeasureService itemUnitOfMeasureService,
                               ItemPackageTypeService itemPackageTypeService,
                               InventoryStatusService inventoryStatusService,
                               InventoryService inventoryService,
                               MovementPathService movementPathService,
                               InventoryAdjustmentThresholdService inventoryAdjustmentThresholdService,
                               InventorySnapshotConfigurationService inventorySnapshotConfigurationService) {
        this.itemFamilyService = itemFamilyService;
        this.itemService = itemService;
        this.itemPackageTypeService = itemPackageTypeService;
        this.itemUnitOfMeasureService = itemUnitOfMeasureService;
        this.inventoryStatusService = inventoryStatusService;
        this.inventoryService = inventoryService;
        this.movementPathService = movementPathService;
        this.inventoryAdjustmentThresholdService = inventoryAdjustmentThresholdService;
        this.inventorySnapshotConfigurationService = inventorySnapshotConfigurationService;


        initiableServices.put("Item_Family", itemFamilyService);
        serviceNames.add("Item_Family");
        initiableServices.put("Inventory_status", inventoryStatusService);
        serviceNames.add("Inventory_status");
        initiableServices.put("Movement_Path", movementPathService);
        serviceNames.add("Movement_Path");
        initiableServices.put("Inventory_Adjustment_Threshold", inventoryAdjustmentThresholdService);
        serviceNames.add("Inventory_Adjustment_Threshold");
        initiableServices.put("inventory_snapshot_configuration", inventorySnapshotConfigurationService);
        serviceNames.add("inventory_snapshot_configuration");
    }
    public String[] getTestDataNames() {
        return serviceNames.toArray(new String[0]);
    }
    public void init(Long companyId, String warehouseName) {
        serviceNames.forEach(serviceName -> init(companyId, serviceName, warehouseName));
    }
    public void init(Long companyId, String name, String warehouseName) {
        initiableServices.get(name).initTestData(companyId, warehouseName);
    }
    public TestDataInitiableService getInitiableService(String name) {
        return initiableServices.get(name);
    }

    public void clear(Long warehouseId) {

        jdbcTemplate.update("delete from cycle_count_batch where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("cycle_count_batch records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from cycle_count_result where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("cycle_count_result records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from cycle_count_request where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("cycle_count_request records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from audit_count_result where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("audit_count_result records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from audit_count_request where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("audit_count_request records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from inventory_snapshot_configuration where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("inventory_snapshot_configuration records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from inventory_snapshot_detail where inventory_snapshot_id in " +
                " (select inventory_snapshot_id from inventory_snapshot where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("inventory_snapshot_detail records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from inventory_snapshot where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("inventory_snapshot records from warehouse ID {} removed!", warehouseId);


        jdbcTemplate.update("delete from inventory_movement where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("inventory_movement records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from inventory where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("inventory records from warehouse ID {} removed!", warehouseId);


        jdbcTemplate.update("delete from inventory_adjustment_request where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("inventory_adjustment_request records from warehouse ID {} removed!", warehouseId);


        jdbcTemplate.update("delete from inventory_adjustment_threshold where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("inventory_adjustment_threshold records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from inventory_activity where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("inventory_activity records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from inventory_status where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("inventory_status records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from item_unit_of_measure where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("item_unit_of_measure records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from item_package_type where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("item_package_type records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from item where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("item records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from item_family where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("item_family records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from movement_path_detail where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("movement_path_detail records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from movement_path where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("movement_path records from warehouse ID {} removed!", warehouseId);

    }
}
