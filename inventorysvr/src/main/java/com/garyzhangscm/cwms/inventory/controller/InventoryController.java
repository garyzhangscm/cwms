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
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.FileService;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RestController
public class InventoryController {
    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    @Autowired
    InventoryService inventoryService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Autowired
    FileService fileService;

    @ClientValidationEndpoint
    @RequestMapping(value="/inventories", method = RequestMethod.GET)
    public List<Inventory> findAllInventories(@RequestParam Long warehouseId,
                                              @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
                                              @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                              @RequestParam(name="itemNames", required = false, defaultValue = "") String itemNames,
                                              @RequestParam(name="itemPackageTypeName", required = false, defaultValue = "") String itemPackageTypeName,
                                              @RequestParam(name="client", required = false, defaultValue = "") Long clientId,
                                              @RequestParam(name="clients", required = false, defaultValue = "") String clientIds,
                                              @RequestParam(name="itemFamilies", required = false, defaultValue = "") String itemFamilyIds,
                                              @RequestParam(name="inventoryStatusId", required = false, defaultValue = "") Long inventoryStatusId,
                                              @RequestParam(name="location", required = false, defaultValue = "") String locationName,
                                              @RequestParam(name="locationId", required = false, defaultValue = "") Long locationId,
                                              @RequestParam(name="locationIds", required = false, defaultValue = "") String locationIds,
                                              @RequestParam(name="locationGroupId", required = false, defaultValue = "") Long locationGroupId,
                                              @RequestParam(name="receiptId", required = false, defaultValue = "") String receiptId,
                                              @RequestParam(name = "receiptNumber", defaultValue = "", required = false) String receiptNumber,
                                              @RequestParam(name="customerReturnOrderId", required = false, defaultValue = "") String customerReturnOrderId,
                                              @RequestParam(name="workOrderId", required = false, defaultValue = "") Long workOrderId,
                                              @RequestParam(name="workOrderLineIds", required = false, defaultValue = "") String workOrderLineIds,
                                              @RequestParam(name="workOrderByProductIds", required = false, defaultValue = "") String workOrderByProductIds,
                                              @RequestParam(name="pickIds", required = false, defaultValue = "") String pickIds,
                                              @RequestParam(name="lpn", required = false, defaultValue = "") String lpn,
                                              @RequestParam(name="color", required = false, defaultValue = "") String color,
                                              @RequestParam(name="productSize", required = false, defaultValue = "") String productSize,
                                              @RequestParam(name="style", required = false, defaultValue = "") String style,
                                              @RequestParam(name = "inventoryIds", defaultValue = "", required = false) String inventoryIds,
                                              @RequestParam(name = "notPutawayInventoryOnly", defaultValue = "false", required = false) Boolean notPutawayInventoryOnly,
                                              @RequestParam(name = "includeVirturalInventory", defaultValue = "", required = false) Boolean includeVirturalInventory,
                                              @RequestParam(name = "includeDetails", defaultValue = "true", required = false) Boolean includeDetails,
                                              ClientRestriction clientRestriction) {



        return inventoryService.findAll(warehouseId, itemId, itemName, itemNames,
                itemPackageTypeName, clientId,  clientIds,
                itemFamilyIds,inventoryStatusId,  locationName,
                locationId, locationIds, locationGroupId,  receiptId,  receiptNumber,
                customerReturnOrderId,  workOrderId,
                workOrderLineIds, workOrderByProductIds,
                pickIds, lpn, color, productSize, style, inventoryIds, notPutawayInventoryOnly,
                includeVirturalInventory, clientRestriction,
                includeDetails);


    }

    @ClientValidationEndpoint
    @RequestMapping(value="/inventories/count", method = RequestMethod.GET)
    public int getInventoryCount(@RequestParam Long warehouseId,
                                              @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                 @RequestParam(name="itemNames", required = false, defaultValue = "") String itemNames,
                                             @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
                                             @RequestParam(name="itemPackageTypeName", required = false, defaultValue = "") String itemPackageTypeName,
                                 @RequestParam(name="client", required = false, defaultValue = "") Long clientId,
                                              @RequestParam(name="clients", required = false, defaultValue = "") String clientIds,
                                              @RequestParam(name="itemFamilies", required = false, defaultValue = "") String itemFamilyIds,
                                              @RequestParam(name="inventoryStatusId", required = false, defaultValue = "") Long inventoryStatusId,
                                              @RequestParam(name="location", required = false, defaultValue = "") String locationName,
                                              @RequestParam(name="locationId", required = false, defaultValue = "") Long locationId,
                                              @RequestParam(name="locationIds", required = false, defaultValue = "") String locationIds,
                                              @RequestParam(name="locationGroupId", required = false, defaultValue = "") Long locationGroupId,
                                              @RequestParam(name="receiptId", required = false, defaultValue = "") String receiptId,
                                 @RequestParam(name = "receiptNumber", defaultValue = "", required = false) String receiptNumber,
                                 @RequestParam(name="customerReturnOrderId", required = false, defaultValue = "") String customerReturnOrderId,
                                              @RequestParam(name="workOrderId", required = false, defaultValue = "") Long workOrderId,
                                              @RequestParam(name="workOrderLineIds", required = false, defaultValue = "") String workOrderLineIds,
                                              @RequestParam(name="workOrderByProductIds", required = false, defaultValue = "") String workOrderByProductIds,
                                              @RequestParam(name="pickIds", required = false, defaultValue = "") String pickIds,
                                              @RequestParam(name="lpn", required = false, defaultValue = "") String lpn,
                                             @RequestParam(name = "color", defaultValue = "", required = false) String color,
                                             @RequestParam(name = "productSize", defaultValue = "", required = false) String productSize,
                                             @RequestParam(name = "style", defaultValue = "", required = false) String style,
                                              @RequestParam(name = "inventoryIds", defaultValue = "", required = false) String inventoryIds,
                                              @RequestParam(name = "notPutawayInventoryOnly", defaultValue = "false", required = false) Boolean notPutawayInventoryOnly,
                                              @RequestParam(name = "includeVirturalInventory", defaultValue = "", required = false) Boolean includeVirturalInventory,
                                 ClientRestriction clientRestriction) {
        return inventoryService.findAll(warehouseId, itemId, itemName, itemNames,
                itemPackageTypeName,  clientId, clientIds,
                itemFamilyIds,inventoryStatusId,  locationName,
                locationId, locationIds, locationGroupId, receiptId, receiptNumber ,
                customerReturnOrderId, workOrderId,
                workOrderLineIds, workOrderByProductIds,
                pickIds, lpn, color, productSize, style,
                inventoryIds, notPutawayInventoryOnly, includeVirturalInventory, clientRestriction, false).size();
    }

    @RequestMapping(value="/inventories/pending", method = RequestMethod.GET)
    public List<Inventory> findPendingInventories(@RequestParam Long locationId) {
        return inventoryService.findPendingInventoryByLocationId(locationId);
    }

    @RequestMapping(value="/inventories/pickable", method = RequestMethod.GET)
    public List<Inventory> findPickableInventories(@RequestParam Long itemId,
                                                   @RequestParam Long inventoryStatusId,
                                                   @RequestParam(name = "lpn", defaultValue = "", required = false) String lpn,
                                                   @RequestParam(name = "color", defaultValue = "", required = false) String color,
                                                   @RequestParam(name = "productSize", defaultValue = "", required = false) String productSize,
                                                   @RequestParam(name = "style", defaultValue = "", required = false) String style,
                                                   @RequestParam(name = "receiptNumber", defaultValue = "", required = false) String receiptNumber,
                                                   @RequestParam(name = "locationId", defaultValue = "", required = false) Long locationId) {
                                                 //   @RequestParam(name = "includeDetails", defaultValue = "true", required = false) Boolean includeDetails) {
        // return inventoryService.findPickableInventories(itemId, inventoryStatusId, includeDetails);
        return inventoryService.findPickableInventories(itemId, inventoryStatusId, locationId, lpn,
                color, productSize, style, receiptNumber);
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


    /**
     * Remove inventory from a warehouse and meet certain criteria
     * @param warehouseId
     * @param itemId
     * @param itemName
     * @param itemPackageTypeName
     * @param clientId
     * @param clientIds
     * @param itemFamilyIds
     * @param inventoryStatusId
     * @param locationName
     * @param locationId
     * @param locationIds
     * @param locationGroupId
     * @param receiptId
     * @param customerReturnOrderId
     * @param workOrderId
     * @param workOrderLineIds
     * @param workOrderByProductIds
     * @param pickIds
     * @param lpn
     * @param inventoryIds
     * @param notPutawayInventoryOnly
     * @param includeVirturalInventory
     * @param clientRestriction
     * @return
     */
    @ClientValidationEndpoint
    @BillableEndpoint
    @RequestMapping(value="/inventories", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeAllInventories(@RequestParam Long warehouseId,
                                              @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
                                              @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                                            @RequestParam(name="itemNames", required = false, defaultValue = "") String itemNames,
                                              @RequestParam(name="itemPackageTypeName", required = false, defaultValue = "") String itemPackageTypeName,
                                              @RequestParam(name="client", required = false, defaultValue = "") Long clientId,
                                              @RequestParam(name="clients", required = false, defaultValue = "") String clientIds,
                                              @RequestParam(name="itemFamilies", required = false, defaultValue = "") String itemFamilyIds,
                                              @RequestParam(name="inventoryStatusId", required = false, defaultValue = "") Long inventoryStatusId,
                                              @RequestParam(name="location", required = false, defaultValue = "") String locationName,
                                              @RequestParam(name="locationId", required = false, defaultValue = "") Long locationId,
                                              @RequestParam(name="locationIds", required = false, defaultValue = "") String locationIds,
                                              @RequestParam(name="locationGroupId", required = false, defaultValue = "") Long locationGroupId,
                                              @RequestParam(name="receiptId", required = false, defaultValue = "") String receiptId,
                                                            @RequestParam(name = "receiptNumber", defaultValue = "", required = false) String receiptNumber,
                                              @RequestParam(name="customerReturnOrderId", required = false, defaultValue = "") String customerReturnOrderId,
                                              @RequestParam(name="workOrderId", required = false, defaultValue = "") Long workOrderId,
                                              @RequestParam(name="workOrderLineIds", required = false, defaultValue = "") String workOrderLineIds,
                                              @RequestParam(name="workOrderByProductIds", required = false, defaultValue = "") String workOrderByProductIds,
                                              @RequestParam(name="pickIds", required = false, defaultValue = "") String pickIds,
                                              @RequestParam(name="lpn", required = false, defaultValue = "") String lpn,
                                                            @RequestParam(name = "color", defaultValue = "", required = false) String color,
                                                            @RequestParam(name = "productSize", defaultValue = "", required = false) String productSize,
                                                            @RequestParam(name = "style", defaultValue = "", required = false) String style,
                                              @RequestParam(name = "inventoryIds", defaultValue = "", required = false) String inventoryIds,
                                              @RequestParam(name = "notPutawayInventoryOnly", defaultValue = "false", required = false) Boolean notPutawayInventoryOnly,
                                              @RequestParam(name = "includeVirturalInventory", defaultValue = "", required = false) Boolean includeVirturalInventory,
                                              ClientRestriction clientRestriction) {



        inventoryService.removeAllInventories(warehouseId, itemId, itemName, itemNames, itemPackageTypeName,clientId,  clientIds,
                itemFamilyIds,inventoryStatusId,  locationName,
                locationId, locationIds, locationGroupId,  receiptId, receiptNumber,
                customerReturnOrderId,  workOrderId,
                workOrderLineIds, workOrderByProductIds,
                pickIds, lpn,
                color, productSize, style,
                inventoryIds, notPutawayInventoryOnly,
                includeVirturalInventory, clientRestriction);

        return ResponseBodyWrapper.success("success");


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
    @RequestMapping(method=RequestMethod.POST, value="/reverse-by-product/{id}")
    public Inventory reverseByProduct(@PathVariable Long id,
                                       @RequestParam(name ="documentNumber", required =  false, defaultValue = "") String documentNumber,
                                       @RequestParam(name ="comment", required =  false, defaultValue = "") String comment) {
        return inventoryService.reverseByProduct(id, documentNumber, comment);
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

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/inventory/move")
    public List<Inventory> moveInventory(@RequestParam Long warehouseId,
                                   @RequestParam(name="inventoryId", required = false, defaultValue = "") Long inventoryId,
                                   @RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
                                   @RequestParam(name="pickId", required = false, defaultValue = "") Long pickId,
                                   @RequestParam(name="immediateMove", required = false, defaultValue = "true") boolean immediateMove,
                                   @RequestParam(name="destinationLpn", required = false, defaultValue = "") String destinationLpn,
                                   @RequestParam(name="lpn", required = false, defaultValue = "") String lpn,
                                   @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                   @RequestParam(name="quantity", required = false, defaultValue = "") Long quantity,
                                   @RequestParam(name="unitOfMeasureName", required = false, defaultValue = "") String unitOfMeasureName,
                                   @RequestBody Location location) {


        // if the inventory id is passed in, then we will only move the specific inventory
        if (Objects.nonNull(inventoryId)) {

            return List.of(inventoryService.moveInventory(inventoryId, location , pickId, immediateMove, destinationLpn));
        }
        else {
            return inventoryService.moveInventory(warehouseId, clientId, lpn, itemName, quantity, unitOfMeasureName, location ,
                    pickId, immediateMove, destinationLpn);
        }
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
    public Inventory reverseReceivedInventory(@PathVariable long id,
                                              @RequestParam(name =  "reverseQCQuantity", defaultValue = "", required = false) Boolean reverseQCQuantity,
                                              @RequestParam(name =  "allowReuseLPN", defaultValue = "", required = false) Boolean allowReuseLPN) {

        return inventoryService.reverseReceivedInventory(id, reverseQCQuantity, allowReuseLPN);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/inventories/validate-new-lpn")
    public ResponseBodyWrapper<String> validateNewLPN(@RequestParam Long warehouseId,
                                                      @RequestParam String lpn)  {
        try {

            inventoryService.validateLPN(warehouseId, lpn, true);
            return ResponseBodyWrapper.success("");
        }
        catch (Exception ex) {

            logger.debug("Get exception while validate new LPN: {}", ex.getMessage());
            return ResponseBodyWrapper.success(ex.getMessage());
        }

    }


    @BillableEndpoint
    @RequestMapping(value="/inventories/{warehouseId}/{lpn}/lpn-label", method = RequestMethod.POST)
    public ReportHistory generateLPNLabel(
            @PathVariable Long warehouseId,
            @PathVariable String lpn,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName,
            @RequestParam(name = "quantity", defaultValue = "", required = false) Long quantity) throws JsonProcessingException {

        logger.debug("start print lpn with warehouse and LPN: {} / {}, quantity: {}, from printer {}",
                warehouseId, lpn, quantity, printerName);
        return inventoryService.generateLPNLabel(warehouseId, lpn, locale, quantity, printerName);
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

    @RequestMapping(value="/inventories/available-for-mps/quantity-ignore-order", method = RequestMethod.GET)
    public Long getAvailableQuantityForMPS(
            @RequestParam Long warehouseId,
            @RequestParam(name = "itemId", defaultValue = "", required = false) Long itemId,
            @RequestParam(name = "itemName", defaultValue = "", required = false) String itemName) {

        return inventoryService.getAvailableQuantityForMPS(warehouseId,

                itemId, itemName);
    }

    @RequestMapping(value="/inventories/available-for-mps/inventory-ignore-order", method = RequestMethod.GET)
    public List<Inventory> getAvailableInventoryForMPS(
            @RequestParam Long warehouseId,
            @RequestParam(name = "itemId", defaultValue = "", required = false) Long itemId,
            @RequestParam(name = "itemName", defaultValue = "", required = false) String itemName) {

        return inventoryService.getAvailableInventoryForMPS(warehouseId,

                itemId, itemName);
    }

    @RequestMapping(value="/inventories/summary/quickbook-desktop", method = RequestMethod.GET)
    public List<QuickbookDesktopInventorySummary> getQuickbookDesktopInventorySummary(

            @RequestParam String companyCode,
            @RequestParam String warehouseName,
            @RequestParam(name = "itemName", defaultValue = "", required = false) String itemName) {


        return inventoryService.getQuickbookDesktopInventorySummary(
                companyCode, warehouseName, itemName
        );
    }

    /**
     *
     * Upload files to initialize the inventory
     * @param warehouseId
     * @param file
     * @return
     * @throws IOException
     */
    @RequestMapping(method=RequestMethod.POST, value="/inventories/upload")
    public ResponseBodyWrapper uploadInventories(Long warehouseId,
                                                 @RequestParam("file") MultipartFile file,
                                                 @RequestParam(name = "removeExistingInventory", defaultValue = "true", required = false) Boolean removeExistingInventory) throws IOException {


        File localFile = fileService.saveFile(file);
        try {
            fileService.validateCSVFile(warehouseId, "inventory", localFile);
        }
        catch (Exception ex) {
            return new ResponseBodyWrapper(-1, ex.getMessage(), "");
        }
        String fileUploadProgressKey = inventoryService.uploadInventoryData(warehouseId, localFile, removeExistingInventory);
        return  ResponseBodyWrapper.success(fileUploadProgressKey);
    }

    @RequestMapping(method=RequestMethod.GET, value="/inventories/upload/progress")
    public ResponseBodyWrapper getFileUploadProgress(Long warehouseId,
                                                 String key) throws IOException {



        return  ResponseBodyWrapper.success(
                String.format("%.2f",inventoryService.getInventoryFileUploadProgress(key)));
    }
    @RequestMapping(method=RequestMethod.GET, value="/inventories/upload/result")
    public List<FileUploadResult> getFileUploadResult(Long warehouseId,
                                                     String key) throws IOException {


        return inventoryService.getFileUploadResult(warehouseId, key);
    }

    /**
     *
     * Upload files to putaway the inventory
     * @param warehouseId
     * @param file
     * @return
     * @throws IOException
     */
    @RequestMapping(method=RequestMethod.POST, value="/inventories/putaway-inventory/upload")
    public ResponseBodyWrapper uploadPutawayInventories(Long warehouseId,
                                                 @RequestParam("file") MultipartFile file,
                                                 @RequestParam(name = "removeExistingInventory", defaultValue = "true", required = false) Boolean removeExistingInventory) throws IOException {


        File localFile = fileService.saveFile(file);
        try {
            fileService.validateCSVFile(warehouseId, "putaway-inventories", localFile);
        }
        catch (Exception ex) {
            return new ResponseBodyWrapper(-1, ex.getMessage(), "");
        }
        String fileUploadProgressKey = inventoryService.uploadPutawayInventoryData(warehouseId, localFile);
        return  ResponseBodyWrapper.success(fileUploadProgressKey);
    }

    @RequestMapping(method=RequestMethod.GET, value="/inventories/putaway-inventory/upload/progress")
    public ResponseBodyWrapper getPutawayFileUploadProgress(Long warehouseId,
                                                     String key) throws IOException {



        return  ResponseBodyWrapper.success(
                String.format("%.2f",inventoryService.getPutawayInventoryFileUploadProgress(key)));
    }
    @RequestMapping(method=RequestMethod.GET, value="/inventories/putaway-inventory/upload/result")
    public List<FileUploadResult> getPutawayFileUploadResult(Long warehouseId,
                                                      String key) throws IOException {


        return inventoryService.getPutawayFileUploadResult(warehouseId, key);
    }

    /**
     * Process bulk pick for the inventory
     * 1. move the lpn to the next location
     * 2. consolidate and split the inventory so that the result of the inventory
     *    matches with the picks in the bulk pick
     * 3. Setup the pick id for each inventory
     * @param warehouseId
     * @param nextLocationId
     * @param lpn
     * @return
     */
    @RequestMapping(value="/inventories/process-bulk-pick", method = RequestMethod.POST)
    public List<Inventory> processBulkPick(
            @RequestParam Long warehouseId,
            @RequestParam String lpn,
            @RequestParam Long nextLocationId,
            @RequestBody BulkPick bulkPick) {


        return inventoryService.processBulkPick(
                warehouseId, lpn, nextLocationId, bulkPick
        );
    }


    @BillableEndpoint
    @RequestMapping(value="/inventories/relabel-lpn", method = RequestMethod.POST)
    public List<Inventory> relabelLPN(
            @RequestParam Long warehouseId,
            @RequestParam String lpn,
            @RequestParam String newLPN,
            @RequestParam(name = "mergeWithExistingInventory", defaultValue = "false", required = false) Boolean  mergeWithExistingInventory) {


        logger.debug("Relabel from {} to {}, mergeWithExistingInventory? {} ",
                lpn, newLPN, mergeWithExistingInventory);
        return inventoryService.relabelLPN(
                warehouseId, lpn, newLPN, mergeWithExistingInventory
        );
    }

    @BillableEndpoint
    @RequestMapping(value="/inventories/{id}/relabel", method = RequestMethod.POST)
    public Inventory relabelLPN(
            @RequestParam Long warehouseId,
            @PathVariable Long id,
            @RequestParam String newLPN,
            @RequestParam(name = "mergeWithExistingInventory", defaultValue = "false", required = false) Boolean  mergeWithExistingInventory) {


        logger.debug("Relabel for inventory id {} to {}, mergeWithExistingInventory? {} ",
                id, newLPN, mergeWithExistingInventory);
        return inventoryService.relabelLPN(
                id, newLPN, mergeWithExistingInventory
        );
    }

    @BillableEndpoint
    @RequestMapping(value="/inventories/relabel", method = RequestMethod.POST)
    public List<Inventory> relabelInventories(
            @RequestParam Long warehouseId,
            @RequestParam String ids,
            @RequestParam String newLPN,
            @RequestParam(name = "mergeWithExistingInventory", defaultValue = "false", required = false) Boolean  mergeWithExistingInventory) {


        logger.debug("Relabel for inventory with ids {} to {}, mergeWithExistingInventory? {} ",
                ids, newLPN, mergeWithExistingInventory);
        return inventoryService.relabelInventories(
                ids, newLPN, mergeWithExistingInventory
        );
    }
}
