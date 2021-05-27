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

package com.garyzhangscm.cwms.inventory.controller;

import com.garyzhangscm.cwms.inventory.model.InventorySnapshot;
import com.garyzhangscm.cwms.inventory.model.InventorySnapshotConfiguration;
import com.garyzhangscm.cwms.inventory.model.InventorySnapshotDetail;
import com.garyzhangscm.cwms.inventory.service.InventorySnapshotConfigurationService;
import com.garyzhangscm.cwms.inventory.service.InventorySnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InventorySnapshotConfigurationController {
    private static final Logger logger = LoggerFactory.getLogger(InventorySnapshotConfigurationController.class);
    @Autowired
    InventorySnapshotConfigurationService inventorySnapshotConfigurationService;

    @RequestMapping(value="/inventory_snapshot_configuration", method = RequestMethod.GET)
    public InventorySnapshotConfiguration findByWarehouseId(
            @RequestParam Long warehouseId) {
        return inventorySnapshotConfigurationService.findByWarehouseId(warehouseId);
    }


    @RequestMapping(value="/inventory_snapshot_configuration", method = RequestMethod.POST)
    public InventorySnapshotConfiguration findAllInventorySnapshotConfiguration(
            @RequestBody InventorySnapshotConfiguration inventorySnapshotConfiguration) {
        return inventorySnapshotConfigurationService.saveOrUpdate(inventorySnapshotConfiguration);
    }


}
