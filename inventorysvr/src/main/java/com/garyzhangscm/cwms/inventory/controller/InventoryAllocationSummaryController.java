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

import com.garyzhangscm.cwms.inventory.model.InventoryAllocationSummary;
import com.garyzhangscm.cwms.inventory.service.InventoryAllocationSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Collection;
@RestController
public class InventoryAllocationSummaryController {
    @Autowired
    InventoryAllocationSummaryService inventoryAllocationSummaryService;

    @RequestMapping(value="/inventory-allocation-summary", method = RequestMethod.GET)
    public Collection<InventoryAllocationSummary> findAllInventoryAllocationSummary(
            @RequestParam Long warehouseId,
            @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
            @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
            @RequestParam(name="locationId", required = false, defaultValue = "") Long locationId,
            @RequestParam(name="locationName", required = false, defaultValue = "") String locationName) {
        return inventoryAllocationSummaryService.getInventoryAllocationSummary(
                warehouseId, itemId, itemName, locationId, locationName);
    }



}
