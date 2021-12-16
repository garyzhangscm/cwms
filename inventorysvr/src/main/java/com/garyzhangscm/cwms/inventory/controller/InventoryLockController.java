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
import com.garyzhangscm.cwms.inventory.model.BillableEndpoint;
import com.garyzhangscm.cwms.inventory.model.Inventory;
import com.garyzhangscm.cwms.inventory.model.InventoryLock;
import com.garyzhangscm.cwms.inventory.model.ItemSampling;
import com.garyzhangscm.cwms.inventory.service.InventoryLockService;
import com.garyzhangscm.cwms.inventory.service.ItemSamplingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@RestController
public class InventoryLockController {
    @Autowired
    private  InventoryLockService inventoryLockService;

    @RequestMapping(value="/inventory-lock", method = RequestMethod.GET)
    public List<InventoryLock> findAllInventoryLock(
                            @RequestParam Long warehouseId,
                            @RequestParam(name="name", required = false, defaultValue = "") String name) {
        return inventoryLockService.findAll(warehouseId, name);
    }


    @BillableEndpoint
    @RequestMapping(value="/inventory-lock", method = RequestMethod.PUT)
    public InventoryLock addInventoryLock(
            @RequestParam Long warehouseId,@RequestBody InventoryLock inventoryLock) {
        return inventoryLockService.addInventoryLock(inventoryLock);
    }


    @RequestMapping(value="/inventory-lock/{id}", method = RequestMethod.GET)
    public InventoryLock findInventoryLock(@PathVariable Long id) {

        return inventoryLockService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/inventory-lock/{id}", method = RequestMethod.POST)
    public InventoryLock changeInventoryLock(@PathVariable Long id,
                                               @RequestBody InventoryLock inventoryLock){
        return inventoryLockService.changeInventoryLock(id, inventoryLock);
    }

    @BillableEndpoint
    @RequestMapping(value="/inventory-lock/{id}", method = RequestMethod.DELETE)
    public void removeInventoryLock(@PathVariable Long id,
                                   @RequestParam Long warehouseId) {
        inventoryLockService.removeInventoryLock(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/inventory-lock/{id}/disable", method = RequestMethod.POST)
    public InventoryLock disableInventoryLock(@PathVariable Long id,
                                   @RequestParam Long warehouseId) {
        return inventoryLockService.disableInventoryLock(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/inventory-lock/{id}/enable", method = RequestMethod.POST)
    public InventoryLock enableInventoryLock(@PathVariable Long id,
                                              @RequestParam Long warehouseId) {
        return inventoryLockService.enableInventoryLock(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/inventory-lock/{id}/unlock", method = RequestMethod.POST)
    public InventoryLock unlockInventory(@PathVariable Long id,
                                             @RequestParam Long warehouseId,
                                             @RequestParam Long inventoryId) {
        return inventoryLockService.unlockInventory(id, inventoryId);
    }


    @BillableEndpoint
    @RequestMapping(value="/inventory-lock/{id}/lockedInventory", method = RequestMethod.GET)
    public List<Inventory> getLockedInventory(@PathVariable Long id,
                                              @RequestParam Long warehouseId) {
        return inventoryLockService.getLockedInventory(id);
    }




}
