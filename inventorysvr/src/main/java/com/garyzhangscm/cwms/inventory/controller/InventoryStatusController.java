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

import com.garyzhangscm.cwms.inventory.exception.GenericException;
import com.garyzhangscm.cwms.inventory.model.InventoryStatus;
import com.garyzhangscm.cwms.inventory.model.ItemFamily;
import com.garyzhangscm.cwms.inventory.service.InventoryStatusService;
import com.garyzhangscm.cwms.inventory.service.ItemFamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InventoryStatusController {
    @Autowired
    InventoryStatusService inventoryStatusService;

    @RequestMapping(value="/inventory-statuses", method = RequestMethod.GET)
    public List<InventoryStatus> findAllInventoryStatuses(@RequestParam(name="name", required = false, defaultValue = "") String name) {
        return inventoryStatusService.findAll(name);
    }

    @RequestMapping(value="/inventory-status/{id}", method = RequestMethod.GET)
    public InventoryStatus findInventoryStatus(@PathVariable Long id) {
        return inventoryStatusService.findById(id);
    }

    @RequestMapping(value="/inventory-status", method = RequestMethod.POST)
    public InventoryStatus createInventoryStatus(@RequestBody InventoryStatus inventoryStatus) {
        return inventoryStatusService.save(inventoryStatus);
    }

    @RequestMapping(method=RequestMethod.PUT, value="/inventory-status/{id}")
    public InventoryStatus changeInventoryStatus(@PathVariable long id,
                                       @RequestBody InventoryStatus inventoryStatus) {
        if (inventoryStatus.getId() != null && inventoryStatus.getId() != id) {
            throw new GenericException(10000, "ID in the URL doesn't match with the data passed in the request");
        }
        return inventoryStatusService.save(inventoryStatus);
    }


    @RequestMapping(method=RequestMethod.DELETE, value="/inventory-status")
    public void removeInventoryStatuses(@RequestParam(name = "item_family_ids", required = false, defaultValue = "") String inventoryStatusIds) {
        inventoryStatusService.delete(inventoryStatusIds);
    }

}
