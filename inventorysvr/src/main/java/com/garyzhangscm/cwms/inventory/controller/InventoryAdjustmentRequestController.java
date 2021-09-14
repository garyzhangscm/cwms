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
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.InventoryAdjustmentRequestService;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InventoryAdjustmentRequestController {
    @Autowired
    InventoryAdjustmentRequestService inventoryAdjustmentRequestService;

    @RequestMapping(value="/inventory-adjustment-requests", method = RequestMethod.GET)
    public List<InventoryAdjustmentRequest> findAllInventories(@RequestParam Long warehouseId,
                                                              @RequestParam(name="inventoryQuantityChangeType", required = false, defaultValue = "") String inventoryQuantityChangeType,
                                                              @RequestParam(name="status", required = false, defaultValue = "") String status,
                                                              @RequestParam(name="locationId", required = false, defaultValue = "") Long locationId,
                                                               @RequestParam(name="locationName", required = false, defaultValue = "") String locationName,
                                                               @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                                               @RequestParam(name="inventoryId", required = false, defaultValue = "") Long inventoryId) {
        return inventoryAdjustmentRequestService.findAll(warehouseId,
                inventoryQuantityChangeType,status,
                itemName, locationId, locationName,  inventoryId);
    }



    @RequestMapping(value="/inventory-adjustment-requests/{id}", method = RequestMethod.GET)
    public InventoryAdjustmentRequest getInventoryAdjustmentRequest(@PathVariable Long id) {
        return inventoryAdjustmentRequestService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/inventory-adjustment-requests/{id}/process", method = RequestMethod.POST)
    public InventoryAdjustmentRequest processInventoryAdjustmentRequest(@PathVariable Long id,
                                                                        @RequestParam Boolean approved,
                                                                        @RequestParam(name="comment", required = false, defaultValue = "") String comment) {
        return inventoryAdjustmentRequestService.processInventoryAdjustmentRequest(id, approved, comment);
    }
}
