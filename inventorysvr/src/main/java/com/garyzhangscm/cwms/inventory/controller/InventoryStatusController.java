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
import com.garyzhangscm.cwms.inventory.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.inventory.model.BillableEndpoint;
import com.garyzhangscm.cwms.inventory.model.InventoryStatus;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import com.garyzhangscm.cwms.inventory.service.InventoryStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InventoryStatusController {
    @Autowired
    InventoryStatusService inventoryStatusService;

    @RequestMapping(value="/inventory-statuses", method = RequestMethod.GET)
    public List<InventoryStatus> findAllInventoryStatuses(@RequestParam Long warehouseId,
                                                          @RequestParam(name="name", required = false, defaultValue = "") String name,
                                                          @RequestParam(name="availableStatusFlag", required = false, defaultValue = "") Boolean availableStatusFlag) {
        return inventoryStatusService.findAll(warehouseId, name, availableStatusFlag);
    }

    @RequestMapping(value="/inventory-status/{id}", method = RequestMethod.GET)
    public InventoryStatus getInventoryStatus(@PathVariable Long id) {
        return inventoryStatusService.findById(id);
    }
    @RequestMapping(value="/inventory-statuses/{id}", method = RequestMethod.GET)
    public InventoryStatus findInventoryStatus(@PathVariable Long id) {
        return inventoryStatusService.findById(id);
    }
    @RequestMapping(value="/inventory-statuses/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeInventoryStatus(@PathVariable Long id,
                                                             @RequestParam Long warehouseId) {
        InventoryStatus inventoryStatus =
                inventoryStatusService.removeInventoryStatus(warehouseId, id);
        return ResponseBodyWrapper.success("Inventory Status " + inventoryStatus.getName() +
                " is removed!");
    }

    @RequestMapping(value="/inventory-statuses", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "workorder_inventoryStatus", allEntries = true),
                    @CacheEvict(cacheNames = "outbound_inventoryStatus", allEntries = true),
            }
    )
    public InventoryStatus createInventoryStatus(@RequestParam Long warehouseId,
                                                 @RequestBody InventoryStatus inventoryStatus) {
        return inventoryStatusService.createInventoryStatus(inventoryStatus);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/inventory-statuses/{id}")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "workorder_inventoryStatus", allEntries = true),
                    @CacheEvict(cacheNames = "outbound_inventoryStatus", allEntries = true),
            }
    )
    public InventoryStatus changeInventoryStatus(@PathVariable long id,
                                                 @RequestParam Long warehouseId,
                                                 @RequestBody InventoryStatus inventoryStatus) {
        if (inventoryStatus.getId() != null && inventoryStatus.getId() != id) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; inventoryStatus.getId(): " + inventoryStatus.getId());
        }
        return inventoryStatusService.changeInventoryStatus(inventoryStatus);
    }


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/inventory-statuses")
    public void removeInventoryStatuses(@RequestParam(name = "inventory-status-ids", required = false, defaultValue = "") String inventoryStatusIds) {
        inventoryStatusService.delete(inventoryStatusIds);
    }

    @RequestMapping(method=RequestMethod.GET, value="/inventory-statuses/available")
    public InventoryStatus getAvailableInventoryStatus(Long warehouseId) {
        return inventoryStatusService.getAvailableInventoryStatus(warehouseId).orElse(null);
    }
}
