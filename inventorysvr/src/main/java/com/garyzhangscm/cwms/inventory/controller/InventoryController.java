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
import com.garyzhangscm.cwms.inventory.model.Inventory;
import com.garyzhangscm.cwms.inventory.model.InventoryMovement;
import com.garyzhangscm.cwms.inventory.model.InventoryQuantityChangeType;
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
    public List<Inventory> findAllInventories(@RequestParam Long warehouseId,
                                              @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                              @RequestParam(name="clients", required = false, defaultValue = "") String clientIds,
                                              @RequestParam(name="itemFamilies", required = false, defaultValue = "") String itemFamilyIds,
                                              @RequestParam(name="inventoryStatusId", required = false, defaultValue = "") Long inventoryStatusId,
                                              @RequestParam(name="location", required = false, defaultValue = "") String locationName,
                                              @RequestParam(name="locationId", required = false, defaultValue = "") Long locationId,
                                              @RequestParam(name="locationGroupId", required = false, defaultValue = "") Long locationGroupId,
                                              @RequestParam(name="receiptId", required = false, defaultValue = "") String receiptId,
                                              @RequestParam(name="pickIds", required = false, defaultValue = "") String pickIds,
                                              @RequestParam(name="lpn", required = false, defaultValue = "") String lpn) {
        return inventoryService.findAll(warehouseId, itemName, clientIds, itemFamilyIds,inventoryStatusId,  locationName, locationId, locationGroupId, receiptId, pickIds, lpn);
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
    public void adjustDownInventory(@PathVariable Long id,
                                    @RequestParam(name ="documentNumber", required =  false, defaultValue = "") String documentNumber,
                                    @RequestParam(name ="comment", required =  false, defaultValue = "") String comment) {
        inventoryService.removeInventory(id, documentNumber, comment);
    }
    // Adjust down the inventory to 0
    @RequestMapping(method=RequestMethod.PUT, value="/inventory-adj")
    public Inventory addInventoryByInventoryAdjust(@RequestBody Inventory inventory,
                                                   @RequestParam(name ="documentNumber", required =  false, defaultValue = "") String documentNumber,
                                                   @RequestParam(name ="comment", required =  false, defaultValue = "") String comment) {
        return inventoryService.addInventory(inventory, InventoryQuantityChangeType.INVENTORY_ADJUST, documentNumber, comment);
    }
    // Adjust down the inventory to 0
    @RequestMapping(method=RequestMethod.PUT, value="/receive")
    public Inventory addInventoryByReceiving(@RequestBody Inventory inventory) {
        return inventoryService.addInventory(inventory, InventoryQuantityChangeType.RECEIVING);
    }

    @RequestMapping(method=RequestMethod.GET, value="/inventory/{id}")
    public Inventory getInventory(@PathVariable Long id) {
        return inventoryService.findById(id);
    }


    @RequestMapping(method=RequestMethod.PUT, value="/inventory/{id}")
    public Inventory changeInventory(@PathVariable long id,
                                     @RequestBody Inventory inventory) {
        if (inventory.getId() != null && !inventory.getId().equals(id)) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; inventory.getId(): " + inventory.getId());
        }
        return inventoryService.changeInventory(id, inventory);
    }

    @RequestMapping(method=RequestMethod.POST, value="/inventory/{id}/adjust-quantity")
    public Inventory adjustInventoryQuantity(@PathVariable long id,
                                             @RequestParam Long newQuantity,
                                             @RequestParam(name ="documentNumber", required =  false, defaultValue = "") String documentNumber,
                                             @RequestParam(name ="comment", required =  false, defaultValue = "") String comment) {
        return inventoryService.adjustInventoryQuantity(id, newQuantity, documentNumber, comment);
    }


    @RequestMapping(method=RequestMethod.POST, value="/inventory/{id}/movements")
    public Inventory setupMovementPath(@PathVariable long id,
                                       @RequestBody List<InventoryMovement> inventoryMovements) {

        return inventoryService.setupMovementPath(id, inventoryMovements);
    }



    @RequestMapping(method=RequestMethod.POST, value="/inventory/{id}/move")
    public Inventory moveInventory(@PathVariable long id,
                                   @RequestParam(name="pickId", required = false, defaultValue = "") Long pickId,
                                   @RequestParam(name="immediateMove", required = false, defaultValue = "true") boolean immediateMove,
                                   @RequestBody Location location) {


        return inventoryService.moveInventory(id, location , pickId, immediateMove);
    }


    @RequestMapping(method=RequestMethod.POST, value="/inventory/{id}/split")
    public List<Inventory> splitInventory(@PathVariable long id,
                                          @RequestParam String newLpn,
                                          @RequestParam Long newQuantity) {


        return inventoryService.splitInventory(id, newLpn, newQuantity);
    }


    @RequestMapping(method=RequestMethod.POST, value="/inventory/{id}/unpick")
    public Inventory unpick(@PathVariable long id,
                            @RequestParam Long warehouseId,
                            @RequestParam(name = "destinationLocationId", required = false, defaultValue = "") Long destinationLocationId,
                            @RequestParam(name = "destinationLocationName", required = false, defaultValue = "") String destinationLocationName,
                            @RequestParam(name = "immediateMove", required = false, defaultValue = "true") boolean immediateMove) {

        return inventoryService.unpick(id, warehouseId,  destinationLocationId, destinationLocationName, immediateMove);
    }


}
