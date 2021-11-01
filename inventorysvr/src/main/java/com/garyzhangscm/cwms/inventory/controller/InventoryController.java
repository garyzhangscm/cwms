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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class InventoryController {
    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    @Autowired
    InventoryService inventoryService;

    @RequestMapping(value="/inventories", method = RequestMethod.GET)
    public List<Inventory> findAllInventories(@RequestParam Long warehouseId,
                                              @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
                                              @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                              @RequestParam(name="itemPackageTypeName", required = false, defaultValue = "") String itemPackageTypeName,
                                              @RequestParam(name="clients", required = false, defaultValue = "") String clientIds,
                                              @RequestParam(name="itemFamilies", required = false, defaultValue = "") String itemFamilyIds,
                                              @RequestParam(name="inventoryStatusId", required = false, defaultValue = "") Long inventoryStatusId,
                                              @RequestParam(name="location", required = false, defaultValue = "") String locationName,
                                              @RequestParam(name="locationId", required = false, defaultValue = "") Long locationId,
                                              @RequestParam(name="locationIds", required = false, defaultValue = "") String locationIds,
                                              @RequestParam(name="locationGroupId", required = false, defaultValue = "") Long locationGroupId,
                                              @RequestParam(name="receiptId", required = false, defaultValue = "") String receiptId,
                                              @RequestParam(name="workOrderId", required = false, defaultValue = "") Long workOrderId,
                                              @RequestParam(name="workOrderLineIds", required = false, defaultValue = "") String workOrderLineIds,
                                              @RequestParam(name="workOrderByProductIds", required = false, defaultValue = "") String workOrderByProductIds,
                                              @RequestParam(name="pickIds", required = false, defaultValue = "") String pickIds,
                                              @RequestParam(name="lpn", required = false, defaultValue = "") String lpn,
                                              @RequestParam(name = "inventoryIds", defaultValue = "", required = false) String inventoryIds,
                                              @RequestParam(name = "notPutawayInventoryOnly", defaultValue = "false", required = false) Boolean notPutawayInventoryOnly,
                                              @RequestParam(name = "includeVirturalInventory", defaultValue = "", required = false) Boolean includeVirturalInventory,
                                              @RequestParam(name = "includeDetails", defaultValue = "true", required = false) Boolean includeDetails) {
        return inventoryService.findAll(warehouseId, itemId, itemName, itemPackageTypeName, clientIds,
                itemFamilyIds,inventoryStatusId,  locationName,
                locationId, locationIds, locationGroupId, receiptId, workOrderId,
                workOrderLineIds, workOrderByProductIds,
                pickIds, lpn, inventoryIds, notPutawayInventoryOnly,
                includeVirturalInventory,
                includeDetails);
    }
    @RequestMapping(value="/inventories/count", method = RequestMethod.GET)
    public int getInventoryCount(@RequestParam Long warehouseId,
                                              @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                             @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
                                             @RequestParam(name="itemPackageTypeName", required = false, defaultValue = "") String itemPackageTypeName,
                                              @RequestParam(name="clients", required = false, defaultValue = "") String clientIds,
                                              @RequestParam(name="itemFamilies", required = false, defaultValue = "") String itemFamilyIds,
                                              @RequestParam(name="inventoryStatusId", required = false, defaultValue = "") Long inventoryStatusId,
                                              @RequestParam(name="location", required = false, defaultValue = "") String locationName,
                                              @RequestParam(name="locationId", required = false, defaultValue = "") Long locationId,
                                              @RequestParam(name="locationIds", required = false, defaultValue = "") String locationIds,
                                              @RequestParam(name="locationGroupId", required = false, defaultValue = "") Long locationGroupId,
                                              @RequestParam(name="receiptId", required = false, defaultValue = "") String receiptId,
                                              @RequestParam(name="workOrderId", required = false, defaultValue = "") Long workOrderId,
                                              @RequestParam(name="workOrderLineIds", required = false, defaultValue = "") String workOrderLineIds,
                                              @RequestParam(name="workOrderByProductIds", required = false, defaultValue = "") String workOrderByProductIds,
                                              @RequestParam(name="pickIds", required = false, defaultValue = "") String pickIds,
                                              @RequestParam(name="lpn", required = false, defaultValue = "") String lpn,
                                              @RequestParam(name = "inventoryIds", defaultValue = "", required = false) String inventoryIds,
                                              @RequestParam(name = "notPutawayInventoryOnly", defaultValue = "false", required = false) Boolean notPutawayInventoryOnly,
                                              @RequestParam(name = "includeVirturalInventory", defaultValue = "", required = false) Boolean includeVirturalInventory) {
        return inventoryService.findAll(warehouseId, itemId, itemName, itemPackageTypeName,  clientIds,
                itemFamilyIds,inventoryStatusId,  locationName,
                locationId, locationIds, locationGroupId, receiptId, workOrderId,
                workOrderLineIds, workOrderByProductIds,
                pickIds, lpn, inventoryIds, notPutawayInventoryOnly, includeVirturalInventory, false).size();
    }

    @RequestMapping(value="/inventories/pending", method = RequestMethod.GET)
    public List<Inventory> findPendingInventories(@RequestParam Long locationId) {
        return inventoryService.findPendingInventoryByLocationId(locationId);
    }

    @RequestMapping(value="/inventories/pickable", method = RequestMethod.GET)
    public List<Inventory> findPickableInventories(@RequestParam Long itemId,
                                                   @RequestParam Long inventoryStatusId) {
                                                 //   @RequestParam(name = "includeDetails", defaultValue = "true", required = false) Boolean includeDetails) {
        // return inventoryService.findPickableInventories(itemId, inventoryStatusId, includeDetails);
        return inventoryService.findPickableInventories(itemId, inventoryStatusId);
    }


    @BillableEndpoint
    @RequestMapping(value="/inventories/consume", method = RequestMethod.POST)
    public List<Inventory> consumeInventoriesForWorkOrderLines(@RequestParam Long warehouseId,
                                            @RequestParam String workOrderLineIds) {
        return inventoryService.consumeInventoriesForWorkOrderLines(warehouseId, workOrderLineIds);
    }

    @BillableEndpoint
    @RequestMapping(value="/inventories/consume/workOrderLine/{id}", method = RequestMethod.POST)
    public List<Inventory> consumeInventoriesForWorkOrderLine(@PathVariable Long id,
                                                              @RequestParam Long warehouseId,
                                                              @RequestParam  Long quantity,
                                                              @RequestParam  Long locationId,
                                                              @RequestParam(name = "inventoryId", required = false, defaultValue = "")  Long inventoryId,
                                                              @RequestParam(name = "lpn", required = false, defaultValue = "")  String lpn,
                                                              @RequestParam(name = "nonPickedInventory", required = false, defaultValue = "")  Boolean nonPickedInventory) {
        return inventoryService.consumeInventoriesForWorkOrderLine(
                id,warehouseId,  quantity, locationId, inventoryId,
                lpn, nonPickedInventory);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/inventory/{id}")
    public Inventory removeInventory(@PathVariable Long id) {

        return inventoryService.removeInventory(id, "", "");
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/inventory")
    public List<Inventory> removeInventories(@RequestParam String inventoryIds) {

        return inventoryService.removeInventores(inventoryIds);
    }


    @BillableEndpoint
    // Adjust down the inventory to 0
    @RequestMapping(method=RequestMethod.DELETE, value="/inventory-adj/{id}")
    public Inventory adjustDownInventory(@PathVariable Long id,
                                    @RequestParam(name ="documentNumber", required =  false, defaultValue = "") String documentNumber,
                                    @RequestParam(name ="comment", required =  false, defaultValue = "") String comment) {
        return inventoryService.removeInventory(id, documentNumber, comment);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/reverse-production/{id}")
    public Inventory reverseProduction(@PathVariable Long id,
                                         @RequestParam(name ="documentNumber", required =  false, defaultValue = "") String documentNumber,
                                         @RequestParam(name ="comment", required =  false, defaultValue = "") String comment) {
        return inventoryService.reverseProduction(id, documentNumber, comment);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/reverse-receiving/{id}")
    public Inventory reverseReceiving(@PathVariable Long id,
                                         @RequestParam(name ="documentNumber", required =  false, defaultValue = "") String documentNumber,
                                         @RequestParam(name ="comment", required =  false, defaultValue = "") String comment) {
        return inventoryService.reverseReceiving(id, documentNumber, comment);
    }

    @BillableEndpoint
    // Adjust up the inventory from 0
    @RequestMapping(method=RequestMethod.PUT, value="/inventory-adj")
    public Inventory addInventoryByInventoryAdjust(@RequestBody Inventory inventory,
                                                   @RequestParam(name ="documentNumber", required =  false, defaultValue = "") String documentNumber,
                                                   @RequestParam(name ="comment", required =  false, defaultValue = "") String comment) {

        logger.debug("Start to create inventory \n{}", inventory.getLpn());
        return inventoryService.addInventory(inventory, InventoryQuantityChangeType.INVENTORY_ADJUST, documentNumber, comment);
    }
    @BillableEndpoint
    // Adjust down the inventory to 0
    @RequestMapping(method=RequestMethod.PUT, value="/receive")
    public Inventory addInventoryByReceiving(@RequestBody Inventory inventory,
                                             @RequestParam(name = "documentNumber", required = false, defaultValue = "") String documentNumber,
                                             @RequestParam(name = "comment", required = false, defaultValue = "") String comment) {
        logger.debug("Start to receive inventory: {}, document number: {}, comment: {}",
                inventory.getLpn(), documentNumber, comment);
        // We may receive  from a receipt, or a work order
        if (Objects.nonNull(inventory.getReceiptId())){

            return inventoryService.addInventory(inventory, InventoryQuantityChangeType.RECEIVING, documentNumber, comment);
        }
        else if (Objects.nonNull(inventory.getWorkOrderLineId())){

            return inventoryService.addInventory(inventory, InventoryQuantityChangeType.RETURN_MATERAIL, documentNumber, comment);
        }
        else if (Objects.nonNull(inventory.getWorkOrderByProductId())){

            return inventoryService.addInventory(inventory, InventoryQuantityChangeType.PRODUCING_BY_PRODUCT, documentNumber, comment);
        }
        else if (Objects.nonNull(inventory.getWorkOrderId())){

            return inventoryService.addInventory(inventory, InventoryQuantityChangeType.PRODUCING, documentNumber, comment);
        }
        else {

            return inventoryService.addInventory(inventory, InventoryQuantityChangeType.UNKNOWN, documentNumber, comment);
        }
    }

    @RequestMapping(method=RequestMethod.GET, value="/inventory/{id}")
    public Inventory getInventory(@PathVariable Long id) {
        return inventoryService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.PUT, value="/inventory/{id}")
    public Inventory changeInventory(@PathVariable long id,
                                     @RequestBody Inventory inventory) {
        if (inventory.getId() != null && !inventory.getId().equals(id)) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; inventory.getId(): " + inventory.getId());
        }
        return inventoryService.changeInventory(id, inventory);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/inventory/{id}/adjust-quantity")
    public Inventory adjustInventoryQuantity(@PathVariable long id,
                                             @RequestParam Long newQuantity,
                                             @RequestParam(name ="documentNumber", required =  false, defaultValue = "") String documentNumber,
                                             @RequestParam(name ="comment", required =  false, defaultValue = "") String comment) {
        return inventoryService.adjustInventoryQuantity(id, newQuantity, documentNumber, comment);
    }


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/inventory/{id}/movements")
    public Inventory setupMovementPath(@PathVariable long id,
                                       @RequestBody List<InventoryMovement> inventoryMovements) {

        return inventoryService.setupMovementPath(id, inventoryMovements);
    }
    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/inventory/{id}/movements")
    public Inventory clearMovementPath(@PathVariable long id) {

        return inventoryService.clearMovementPath(id);
    }



    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/inventory/{id}/move")
    public Inventory moveInventory(@PathVariable long id,
                                   @RequestParam(name="pickId", required = false, defaultValue = "") Long pickId,
                                   @RequestParam(name="immediateMove", required = false, defaultValue = "true") boolean immediateMove,
                                   @RequestParam(name="destinationLpn", required = false, defaultValue = "") String destinationLpn,
                                   @RequestBody Location location) {


        return inventoryService.moveInventory(id, location , pickId, immediateMove, destinationLpn);
    }


    /**
     * Split the inventory into 2,
     * the first one in the list is the original inventory with updated quantity
     * the second one in the list is the new inventory
     * @param id
     * @param newLpn
     * @param newQuantity
     * @return
     */
    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/inventory/{id}/split")
    public List<Inventory> splitInventory(@PathVariable long id,
                                          @RequestParam(name = "newLpn", required = false, defaultValue = "")  String newLpn,
                                          @RequestParam Long newQuantity) {


        return inventoryService.splitInventory(id, newLpn, newQuantity);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/inventory/mark-lpn-allocated")
    public List<Inventory> markLPNAllocated(@RequestParam Long warehouseId,
                                            @RequestParam String lpn,
                                            @RequestParam Long allocatedByPickId) {


        return inventoryService.markLPNAllocated(warehouseId, lpn, allocatedByPickId);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/inventory/release-lpn-allocated")
    public List<Inventory> releaseLPNAllocated(@RequestParam Long warehouseId,
                                               @RequestParam String lpn,
                                            @RequestParam Long allocatedByPickId) {


        return inventoryService.releaseLPNAllocated(warehouseId, lpn, allocatedByPickId);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/inventory/{id}/unpick")
    public Inventory unpick(@PathVariable long id,
                            @RequestParam Long warehouseId,
                            @RequestParam(name = "destinationLocationId", required = false, defaultValue = "") Long destinationLocationId,
                            @RequestParam(name = "destinationLocationName", required = false, defaultValue = "") String destinationLocationName,
                            @RequestParam(name = "immediateMove", required = false, defaultValue = "true") boolean immediateMove) {

        return inventoryService.unpick(id, warehouseId,  destinationLocationId, destinationLocationName, immediateMove);
    }



    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/inventory/{id}/reverse-receiving")
    public Inventory reverseReceivedInventory(@PathVariable long id) {

        return inventoryService.reverseReceivedInventory(id);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/inventories/validate-new-lpn")
    public ResponseBodyWrapper<String> validateNewLPN(@RequestParam Long warehouseId,
                                                      @RequestParam String lpn)  {
        inventoryService.validateLPN(warehouseId, lpn, true);

        return ResponseBodyWrapper.success("");
    }


    @BillableEndpoint
    @RequestMapping(value="/inventories/{warehouseId}/{lpn}/lpn-label/ecotech", method = RequestMethod.POST)
    public ReportHistory generateEcotechLPNLabel(
            @PathVariable Long warehouseId,
            @PathVariable String lpn,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        logger.debug("start print lpn with warehouse and LPN: {} / {}",
                warehouseId, lpn);
        return inventoryService.generateEcotechLPNLabel(warehouseId, lpn, locale);
    }


    @RequestMapping(value="/inventories/qc-required", method = RequestMethod.GET)
    public List<Inventory> getQCRequiredInventory(
            @RequestParam Long warehouseId,
            @RequestParam(name = "locationId", defaultValue = "", required = false) Long locationId,
            @RequestParam(name = "locationName", defaultValue = "", required = false) String locationName,
            @RequestParam(name = "locationGroupId", defaultValue = "", required = false) Long locationGroupId,
            @RequestParam(name = "locationGroupIds", defaultValue = "", required = false) String locationGroupIds,
            @RequestParam(name = "itemId", defaultValue = "", required = false) Long itemId,
            @RequestParam(name = "itemName", defaultValue = "", required = false) String itemName) {

        return inventoryService.getQCRequiredInventory(warehouseId,
                locationId, locationName, locationGroupId,
                locationGroupIds,
                itemId, itemName);
    }

}
