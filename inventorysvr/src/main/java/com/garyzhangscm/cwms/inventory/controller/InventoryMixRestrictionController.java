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

import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.exception.InventoryException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.InventoryConfigurationService;
import com.garyzhangscm.cwms.inventory.service.InventoryMixRestrictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InventoryMixRestrictionController {
    private static final Logger logger = LoggerFactory.getLogger(InventoryMixRestrictionController.class);
    @Autowired
    InventoryMixRestrictionService inventoryMixRestrictionService;

    @ClientValidationEndpoint
    @RequestMapping(value="/inventory-mix-restriction", method = RequestMethod.GET)
    public List<InventoryMixRestriction> findInventoryMixRestriction(
            @RequestParam Long warehouseId,
            @RequestParam(name = "locationGroupTypeId", defaultValue = "", required = false) Long locationGroupTypeId,
            @RequestParam(name = "locationGroupId", defaultValue = "", required = false) Long locationGroupId,
            @RequestParam(name = "locationId", defaultValue = "", required = false) Long locationId,
            @RequestParam(name = "locationName", defaultValue = "", required = false) String locationName,
            @RequestParam(name = "clientId", defaultValue = "", required = false) Long clientId,
            ClientRestriction clientRestriction) {
        return inventoryMixRestrictionService.findAll(warehouseId,
                locationGroupTypeId, locationGroupId, locationId, locationName, clientId, clientRestriction);
    }


    @RequestMapping(value="/inventory-mix-restriction", method = RequestMethod.PUT)
    public InventoryMixRestriction addInventoryMixRestriction(
            @RequestParam Long warehouseId,
            @RequestBody InventoryMixRestriction inventoryMixRestriction) {
        return inventoryMixRestrictionService.addInventoryMixRestriction(warehouseId, inventoryMixRestriction);
    }

    @RequestMapping(value="/inventory-mix-restriction/{id}", method = RequestMethod.GET)
    public InventoryMixRestriction getInventoryMixRestriction(
            @PathVariable Long id,
            @RequestParam Long warehouseId) {

        return inventoryMixRestrictionService.findById(id);
    }

    @RequestMapping(value="/inventory-mix-restriction/{id}", method = RequestMethod.POST)
    public InventoryMixRestriction addInventoryMixRestriction(
            @PathVariable Long id,
            @RequestParam Long warehouseId,
            @RequestBody InventoryMixRestriction inventoryMixRestriction) {
        if (!id.equals(inventoryMixRestriction.getId())) {
            throw InventoryException.raiseException("Can't change the inventory mix restriction as the id passed in" +
                    " doesn't match with the inventory restriction");
        }
        return inventoryMixRestrictionService.addInventoryMixRestriction(warehouseId, inventoryMixRestriction);
    }

    @RequestMapping(value="/inventory-mix-restriction/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> addInventoryMixRestriction(
            @PathVariable Long id,
            @RequestParam Long warehouseId) {
        inventoryMixRestrictionService.removeInventoryMixRule(id);
        return ResponseBodyWrapper.success("inventory mix restriction " + id + " is removed");
    }

}
