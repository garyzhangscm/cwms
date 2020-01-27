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
import com.garyzhangscm.cwms.inventory.model.Inventory;
import com.garyzhangscm.cwms.inventory.model.InventoryMovement;
import com.garyzhangscm.cwms.inventory.model.Location;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InventoryController {
    @Autowired
    InventoryService inventoryService;

    @RequestMapping(value="/inventories", method = RequestMethod.GET)
    public List<Inventory> findAllInventories(@RequestParam String warehouseName,
                                              @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                              @RequestParam(name="clients", required = false, defaultValue = "") String clientIds,
                                              @RequestParam(name="item_families", required = false, defaultValue = "") String itemFamilyIds,
                                              @RequestParam(name="inventory_status_id", required = false, defaultValue = "") Long inventoryStatusId,
                                              @RequestParam(name="location", required = false, defaultValue = "") String locationName,
                                              @RequestParam(name="location_group_id", required = false, defaultValue = "") Long locationGroupId,
                                              @RequestParam(name="receipt_id", required = false, defaultValue = "") String receiptId,
                                              @RequestParam(name="pick_ids", required = false, defaultValue = "") String pickIds,
                                              @RequestParam(name="lpn", required = false, defaultValue = "") String lpn) {
        return inventoryService.findAll(warehouseName, itemName, clientIds, itemFamilyIds,inventoryStatusId,  locationName, locationGroupId, receiptId, pickIds, lpn);
    }

    @RequestMapping(value="/inventories/pending", method = RequestMethod.GET)
    public List<Inventory> findPendingInventories(@RequestParam Long locationId) {
        return inventoryService.findPendingInventoryByLocationId(locationId);
    }

    @RequestMapping(value="/inventories/pickable", method = RequestMethod.GET)
    public List<Inventory> findPickableInventories(@RequestParam Long itemId,
                                                   @RequestParam Long inventoryStatusId) {
        return inventoryService.findPickableInventories(itemId, inventoryStatusId);
    }


    @RequestMapping(method=RequestMethod.DELETE, value="/inventory/{id}")
    public void removeInventory(@PathVariable Long id) {
        inventoryService.delete(id);
    }


    // Adjust down the inventory to 0
    @RequestMapping(method=RequestMethod.DELETE, value="/inventory-adj/{id}")
    public void adjustDownInventory(@PathVariable Long id) {
        inventoryService.adjustDownInventory(id);
    }

    @RequestMapping(method=RequestMethod.GET, value="/inventory/{id}")
    public Inventory getInventory(@PathVariable Long id) {
        return inventoryService.findById(id);
    }

    @RequestMapping(method=RequestMethod.POST, value="/inventories")
    public Inventory addInventory(@RequestBody Inventory inventory) {
        return inventoryService.save(inventory);
    }

    @RequestMapping(method=RequestMethod.PUT, value="/inventory/{id}")
    public Inventory changeInventory(@PathVariable long id,
                                     @RequestBody Inventory inventory) {
        if (inventory.getId() != null && inventory.getId() != id) {
            throw new GenericException(10000, "ID in the URL doesn't match with the data passed in the request");
        }
        return inventoryService.save(inventory);
    }


    @RequestMapping(method=RequestMethod.POST, value="/inventory/{id}/movements")
    public Inventory setupMovementPath(@PathVariable long id,
                                       @RequestBody List<InventoryMovement> inventoryMovements) {

        return inventoryService.setupMovementPath(id, inventoryMovements);
    }



    @RequestMapping(method=RequestMethod.POST, value="/inventory/{id}/move")
    public Inventory moveInventory(@PathVariable long id,
                                   @RequestParam(name="pickId", required = false, defaultValue = "") Long pickId,
                                   @RequestBody Location location) {


        return inventoryService.moveInventory(id, location , pickId);
    }


    @RequestMapping(method=RequestMethod.POST, value="/inventory/{id}/split")
    public List<Inventory> splitInventory(@PathVariable long id,
                                          @RequestParam String newLpn,
                                          @RequestParam Long newQuantity) {


        return inventoryService.splitInventory(id, newLpn, newQuantity);
    }


    @RequestMapping(method=RequestMethod.POST, value="/inventory/{id}/unpick")
    public Inventory unpick(@PathVariable long id) {

        return inventoryService.unpick(id);
    }

    @RequestMapping(method=RequestMethod.POST, value="/inventories/unpick")
    public List<Inventory> unpick(@RequestParam String lpn) {

        return inventoryService.unpick(lpn);
    }

}
