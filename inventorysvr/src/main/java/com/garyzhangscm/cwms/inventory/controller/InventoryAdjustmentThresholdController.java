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

import com.garyzhangscm.cwms.inventory.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.inventory.model.InventoryAdjustmentRequest;
import com.garyzhangscm.cwms.inventory.model.InventoryAdjustmentThreshold;
import com.garyzhangscm.cwms.inventory.service.InventoryAdjustmentRequestService;
import com.garyzhangscm.cwms.inventory.service.InventoryAdjustmentThresholdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InventoryAdjustmentThresholdController {
    @Autowired
    InventoryAdjustmentThresholdService inventoryAdjustmentThresholdService;

    @RequestMapping(value="/inventory-adjustment-thresholds", method = RequestMethod.GET)
    public List<InventoryAdjustmentThreshold> findAllInventoryAdjustmentThresholds(@RequestParam Long warehouseId,
                                                                                   @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                                                                   @RequestParam(name="clientIds", required = false, defaultValue = "") String clientIds,
                                                                                   @RequestParam(name="itemFamilyIds", required = false, defaultValue = "") String itemFamilyIds,
                                                                                   @RequestParam(name="inventoryQuantityChangeTypes", required = false, defaultValue = "") String inventoryQuantityChangeTypes,
                                                                                   @RequestParam(name="username", required = false, defaultValue = "") String username,
                                                                                   @RequestParam(name="roleName", required = false, defaultValue = "") String roleName,
                                                                                   @RequestParam(name="enabled", required = false, defaultValue = "") Boolean enabled) {
        return inventoryAdjustmentThresholdService.findAll(warehouseId,
                itemName,clientIds,
                itemFamilyIds, inventoryQuantityChangeTypes, username,  roleName, enabled);
    }



    @RequestMapping(value="/inventory-adjustment-thresholds/{id}", method = RequestMethod.GET)
    public InventoryAdjustmentThreshold getInventoryAdjustmentThreshold(@PathVariable Long id) {
        return inventoryAdjustmentThresholdService.findById(id);
    }


    @RequestMapping(value="/inventory-adjustment-thresholds", method = RequestMethod.PUT)
    public InventoryAdjustmentThreshold addInventoryAdjustmentThreshold( @RequestBody InventoryAdjustmentThreshold inventoryAdjustmentThreshold) {
        return inventoryAdjustmentThresholdService.addInventoryAdjustmentThreshold(inventoryAdjustmentThreshold);
    }

    @RequestMapping(value="/inventory-adjustment-thresholds/{id}", method = RequestMethod.POST)
    public InventoryAdjustmentThreshold changeInventoryAdjustmentThreshold(@PathVariable Long id,
                                                                           @RequestBody InventoryAdjustmentThreshold inventoryAdjustmentThreshold) {

        if (inventoryAdjustmentThreshold.getId() != null && !inventoryAdjustmentThreshold.getId().equals(id)) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; inventoryAdjustmentThreshold.getId(): " + inventoryAdjustmentThreshold.getId());
        }
        return inventoryAdjustmentThresholdService.changeInventoryAdjustmentThreshold(inventoryAdjustmentThreshold);
    }

    @RequestMapping(value="/inventory-adjustment-thresholds/{id}", method = RequestMethod.DELETE)
    public void removeInventoryAdjustmentThreshold(@PathVariable Long id) {
        inventoryAdjustmentThresholdService.removeInventoryAdjustmentThreshold(id);
    }

}
