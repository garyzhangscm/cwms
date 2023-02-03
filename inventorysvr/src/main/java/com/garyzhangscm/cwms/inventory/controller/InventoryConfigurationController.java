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

import com.garyzhangscm.cwms.inventory.model.BillableEndpoint;
import com.garyzhangscm.cwms.inventory.model.InventoryConfiguration;
import com.garyzhangscm.cwms.inventory.model.InventoryConfigurationType;
import com.garyzhangscm.cwms.inventory.model.InventorySnapshotConfiguration;
import com.garyzhangscm.cwms.inventory.service.InventoryConfigurationService;
import com.garyzhangscm.cwms.inventory.service.InventorySnapshotConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InventoryConfigurationController {
    private static final Logger logger = LoggerFactory.getLogger(InventoryConfigurationController.class);
    @Autowired
    InventoryConfigurationService inventoryConfigurationService;

    @RequestMapping(value="/inventory_configuration", method = RequestMethod.GET)
    public InventoryConfiguration findInventoryConfiguration(
            @RequestParam Long warehouseId) {
        return inventoryConfigurationService.findByWarehouseId(warehouseId);
    }

    @BillableEndpoint
    @RequestMapping(value="/inventory_configuration", method = RequestMethod.PUT)
    public InventoryConfiguration addInventoryConfigurations(
            @RequestParam Long warehouseId,
            @RequestBody InventoryConfiguration inventoryConfigurations) {
        return inventoryConfigurationService.addInventoryConfigurations(
                warehouseId, inventoryConfigurations);
    }

    @BillableEndpoint
    @RequestMapping(value="/inventory_configuration/{id}", method = RequestMethod.POST)
    public InventoryConfiguration changeInventoryConfigurations(
            @RequestParam Long warehouseId,
            @RequestBody InventoryConfiguration inventoryConfiguration) {
        return inventoryConfigurationService.changeInventoryConfigurations(warehouseId, inventoryConfiguration);
    }


}
