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

package com.garyzhangscm.cwms.inbound.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.inbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.service.*;
import org.apache.coyote.Request;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
public class ReceiptController {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptController.class);
    @Autowired
    ReceiptService receiptService;
    @Autowired
    ReceiptLineService receiptLineService;

    @Autowired
    ReceiptBillableActivityService receiptBillableActivityService;
    @Autowired
    ReceiptLineBillableActivityService receiptLineBillableActivityService;

    @Autowired
    UploadFileService uploadFileService;
    @Autowired
    FileService fileService;


    @ClientValidationEndpoint
    @RequestMapping(value="/receipts", method = RequestMethod.GET)
    public List<Receipt> findAllReceipts(@RequestParam Long warehouseId,
                                         @RequestParam(name="number", required = false, defaultValue = "") String number,
                                         @RequestParam(name="supplierName", required = false, defaultValue = "") String supplierName,
                                         @RequestParam(name="supplierId", required = false, defaultValue = "") Long supplierId,
                                         @RequestParam(name="clientName", required = false, defaultValue = "") String clientName,
                                         @RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
                                         @RequestParam(name="receipt_status_list", required = false, defaultValue = "") String receiptStatusList,
                                         @RequestParam(name = "checkInStartTime", required = false, defaultValue = "")
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime checkInStartTime,
                                         @RequestParam(name = "checkInEndTime", required = false, defaultValue = "")
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime checkInEndTime,
                                         @RequestParam(name = "checkInDate", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
                                         @RequestParam(name = "purchaseOrderId", required = false, defaultValue = "") Long purchaseOrderId,
                                         @RequestParam(name = "ids", required = false, defaultValue = "") String ids,
                                         @RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails,
                                         ClientRestriction clientRestriction) {
        return receiptService.findAll(warehouseId, number, receiptStatusList,
                supplierId, supplierName,
                clientId, clientName,
                checkInStartTime, checkInEndTime, checkInDate, purchaseOrderId, ids, loadDetails, clientRestriction);
    }


    @RequestMapping(value="/receipts/count-by-supplier", method = RequestMethod.GET)
    public Integer getReceiptCountBySupplier(@RequestParam Long warehouseId,
                                         @RequestParam(name="supplierName", required = false, defaultValue = "") String supplierName,
                                         @RequestParam(name="supplierId", required = false, defaultValue = "") Long supplierId) {
        return receiptService.getReceiptCountBySupplier(warehouseId,  supplierId,
                supplierName);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts", method = RequestMethod.POST)
    public Receipt addReceipts(@RequestBody Receipt receipt) {
        return receiptService.addReceipts(receipt);
    }


    @RequestMapping(value="/receipts/{id}", method = RequestMethod.GET)
    public Receipt findReceipt(@PathVariable Long id,
                               @RequestParam(name="ignoreNotFoundError", required = false, defaultValue = "false") Boolean ignoreNotFoundError) {
        try {
            return receiptService.findById(id);
        }
        catch (ResourceNotFoundException ex) {
            if (Boolean.TRUE.equals(ignoreNotFoundError)) {
                return null;
            }
            throw ResourceNotFoundException.raiseException(ex.getMessage());
        }
    }

    @RequestMapping(value="/receipts/receipt-lines/{id}", method = RequestMethod.GET)
    public ReceiptLine findReceiptLine(@PathVariable Long id) {
        return receiptLineService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/{id}", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public Receipt changeReceipt(@PathVariable Long id,
                                 @RequestBody Receipt receipt){
        return receiptService.changeReceipt(id, receipt);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/{id}/complete", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public Receipt completeReceipt(@PathVariable Long id){
        return receiptService.completeReceipt(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts", method = RequestMethod.DELETE)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public void removeReceipts(@RequestParam(name = "receipt_ids", required = false, defaultValue = "") String receiptIds) {
        receiptService.removeReceipts(receiptIds);
    }


    @RequestMapping(value="/receipts/{id}/next-line-number", method = RequestMethod.GET)
    public ResponseBodyWrapper getNextReceiptLineNumber(@PathVariable Long id) {
        return ResponseBodyWrapper.success(receiptService.getNextReceiptLineNumber(id));
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/{id}/lines", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public ReceiptLine addReceiptLine(@PathVariable Long id,
                                      @RequestBody ReceiptLine receiptLine) {
        return receiptLineService.addReceiptLine(id, receiptLine);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/{receiptId}/lines/{receiptLineId}", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public ReceiptLine changeReceiptLine(@PathVariable Long receiptId,
                                         @PathVariable Long receiptLineId,
                                      @RequestBody ReceiptLine receiptLine) {
        return receiptLineService.changeReceiptLine(receiptId, receiptLineId, receiptLine);
    }

    /**
     * Receive inventory from a receipt line
     * @param receiptId Receipt ID
     * @param receiptLineId receipt line id
     * @param inventory inventory to be received
     * @return
     */
    @BillableEndpoint
    @RequestMapping(value="/receipts/{receiptId}/lines/{receiptLineId}/receive", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public Inventory receive(@PathVariable Long receiptId,
                             @PathVariable Long receiptLineId,
                             @RequestBody Inventory inventory,
                             @RequestParam(name = "rfCode", defaultValue = "", required = false) String rfCode,
                             @RequestParam(name = "receiveToStage", defaultValue = "false", required = false) Boolean receiveToStage,
                             @RequestParam(name = "stageLocation", defaultValue = "", required = false) String stageLocation) {
        logger.debug("start to receive with attribute:");
        logger.debug("color: {}", Strings.isBlank(inventory.getColor()) ? "N/A" : inventory.getColor());
        logger.debug("Product Size: {}", Strings.isBlank(inventory.getProductSize()) ? "N/A" : inventory.getProductSize());
        logger.debug("Style: {}", Strings.isBlank(inventory.getStyle()) ? "N/A" : inventory.getStyle());
        logger.debug("Attribute 1: {}", Strings.isBlank(inventory.getAttribute1()) ? "N/A" : inventory.getAttribute1());
        logger.debug("Attribute 2: {}", Strings.isBlank(inventory.getAttribute2()) ? "N/A" : inventory.getAttribute2());
        logger.debug("Attribute 3: {}", Strings.isBlank(inventory.getAttribute3()) ? "N/A" : inventory.getAttribute3());
        logger.debug("Attribute 4: {}", Strings.isBlank(inventory.getAttribute4()) ? "N/A" : inventory.getAttribute4());
        logger.debug("Attribute 5: {}", Strings.isBlank(inventory.getAttribute5()) ? "N/A" : inventory.getAttribute5());

        if (Boolean.TRUE.equals(inventory.getKitInventoryFlag())) {
            logger.debug("start to receive a kit inventory");
            for (Inventory kitInnerInventory : inventory.getKitInnerInventories()) {
                logger.debug(">> inner inventory: item = {}, quantity = {}",
                        kitInnerInventory.getItem().getName(), kitInnerInventory.getQuantity());

                logger.debug(">>>> inner inventory color: {}",
                        Strings.isBlank(kitInnerInventory.getColor()) ? "N/A" : kitInnerInventory.getColor());
                logger.debug(">>>> inner inventory Product Size: {}",
                        Strings.isBlank(kitInnerInventory.getProductSize()) ? "N/A" : kitInnerInventory.getProductSize());
                logger.debug(">>>> inner inventory Style: {}",
                        Strings.isBlank(kitInnerInventory.getStyle()) ? "N/A" : kitInnerInventory.getStyle());
                logger.debug(">>>> inner inventory Attribute 1: {}",
                        Strings.isBlank(kitInnerInventory.getAttribute1()) ? "N/A" : kitInnerInventory.getAttribute1());
                logger.debug(">>>> inner inventory Attribute 2: {}",
                        Strings.isBlank(kitInnerInventory.getAttribute2()) ? "N/A" : kitInnerInventory.getAttribute2());
                logger.debug(">>>> inner inventory Attribute 3: {}",
                        Strings.isBlank(kitInnerInventory.getAttribute3()) ? "N/A" : kitInnerInventory.getAttribute3());
                logger.debug(">>>> inner inventory Attribute 4: {}",
                        Strings.isBlank(kitInnerInventory.getAttribute4()) ? "N/A" : kitInnerInventory.getAttribute4());
                logger.debug(">>>> inner inventory Attribute 5: {}",
                        Strings.isBlank(kitInnerInventory.getAttribute5()) ? "N/A" : kitInnerInventory.getAttribute5());
            }
        }
            return receiptLineService.receive(receiptId, receiptLineId, inventory, receiveToStage, stageLocation, rfCode);
    }


    /**
     * Recalculate the qc quantity for the receipt line. We can specify the qc quantity and percentage, or let
     * the system run the configuration again to refresh the qc quantity required
     * @param receiptLineId
     * @param qcQuantity
     * @param qcPercentage
     * @return
     */
    @BillableEndpoint
    @RequestMapping(value="/receipts/lines/{receiptLineId}/recalculate-qc-quantity", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public ReceiptLine recalculateQCQuantity(@PathVariable Long receiptLineId,
                                           @RequestParam(name = "qcQuantity", required = false, defaultValue = "") Long qcQuantity,
                                           @RequestParam(name = "qcPercentage", required = false, defaultValue = "") Double qcPercentage) {
        return receiptLineService.recalculateQCQuantity(receiptLineId, qcQuantity, qcPercentage);
    }

    /***
     * Receive multiple LPNs with the same quantity
     * @param receiptId
     * @param receiptLineId
     * @param inventoryList
     * @return
     */
    @BillableEndpoint
    @RequestMapping(value="/receipts/{receiptId}/lines/{receiptLineId}/receive-multiple-lpns", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public List<Inventory> receive(@PathVariable Long receiptId,
                             @PathVariable Long receiptLineId,
                             @RequestBody List<Inventory> inventoryList,
                                   @RequestParam(name = "rfCode", defaultValue = "", required = false) String rfCode) {
        return receiptLineService.receive(receiptId, receiptLineId, inventoryList, rfCode);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/{receiptId}/lines/{receiptLineId}/reverse", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public ReceiptLine reverseReceivedInventory(@PathVariable Long receiptId,
                                                 @PathVariable Long receiptLineId,
                                                 @RequestParam Long quantity,
                                                @RequestParam(name = "inboundQCRequired", defaultValue = "", required = false) Boolean inboundQCRequired,
                                                @RequestParam(name = "reverseQCQuantity", defaultValue = "", required = false) Boolean reverseQCQuantity) {
        return receiptLineService.reverseReceivedInventory(receiptId, receiptLineId, quantity, inboundQCRequired, reverseQCQuantity);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/{id}/check-in", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public Receipt checkInReceipt(@PathVariable Long id){

            return receiptService.checkInReceipt(id);
    }
    @RequestMapping(value="/receipts/{id}/inventories", method = RequestMethod.GET)
    public List<Inventory> findInventoryByReceipt(@PathVariable Long id){
        return receiptService.findInventoryByReceipt(id);
    }

    @RequestMapping(value="/receipts/inventories", method = RequestMethod.GET)
    public List<Inventory> findInventoryByReceipts(@RequestParam Long warehouseId,
                                                   @RequestParam String receiptIds){
        if (Strings.isBlank(receiptIds)) {
            return new ArrayList<>();
        }
        return receiptService.findInventoryByReceipts(warehouseId, receiptIds);
    }


    @BillableEndpoint
    @RequestMapping(value="/receipts/lines", method = RequestMethod.DELETE)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public void removeReceiptLine(@RequestParam String receiptLineIds) {
        receiptLineService.delete(receiptLineIds);
    }


    @BillableEndpoint
    @RequestMapping(value="/receipts/{id}/receiving-document", method = RequestMethod.POST)
    public ReportHistory generateReceivingDocument(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName) throws JsonProcessingException {

        return receiptService.generateReceivingDocument(id, locale, printerName);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/{id}/putaway-document", method = RequestMethod.POST)
    public ReportHistory generatePutawayDocument(
            @PathVariable Long id,
            @RequestParam(name = "inventoryIds", defaultValue = "", required = false) String inventoryIds,
            @RequestParam(name = "notPutawayInventoryOnly", defaultValue = "false", required = false) Boolean notPutawayInventoryOnly,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName) throws JsonProcessingException {

        return receiptService.generatePutawayDocument(id, locale, inventoryIds, notPutawayInventoryOnly, printerName);
    }


    @BillableEndpoint
    @RequestMapping(value="/receipts/receipt-lines/{id}/pre-print-lpn-label", method = RequestMethod.POST)
    public ReportHistory generatePrePrintLPNLabel(
            @PathVariable Long id,
            @RequestParam String lpn,
            @RequestParam(name = "quantity", defaultValue = "", required = false) Long inventoryQuantity,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName,
            @RequestParam(name = "ignoreInventoryQuantity", defaultValue = "false", required = false) Boolean ignoreInventoryQuantity
    ) throws JsonProcessingException {

        logger.debug("start generate pre-printed lpn label with id: {}", id);
        return receiptService.generatePrePrintLPNLabel(id, lpn, inventoryQuantity, ignoreInventoryQuantity,  locale, printerName);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/receipt-lines/{id}/pre-print-lpn-report", method = RequestMethod.POST)
    public ReportHistory generatePrePrintLPNReport(
            @PathVariable Long id,
            @RequestParam String lpn,
            @RequestParam(name = "quantity", defaultValue = "", required = false) Long inventoryQuantity,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName,
            @RequestParam(name = "ignoreInventoryQuantity", defaultValue = "false", required = false) Boolean ignoreInventoryQuantity
    )  {

        logger.debug("start generate pre-printed lpn report with id: {}", id);
        return receiptService.generatePrePrintLPNReport(id, lpn, inventoryQuantity, ignoreInventoryQuantity, locale, printerName);
    }


    @BillableEndpoint
    @RequestMapping(value="/receipts/receipt-lines/{id}/pre-print-lpn-label/batch", method = RequestMethod.POST)
    public ReportHistory generatePrePrintLPNLabelInBatch(
            @PathVariable Long id,
            @RequestParam String lpn,
            @RequestParam(name = "inventoryQuantity", defaultValue = "", required = false) Long inventoryQuantity,
            @RequestParam(name = "count", defaultValue = "1", required = false) Integer count,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName,
            @RequestParam(name = "ignoreInventoryQuantity", defaultValue = "false", required = false) Boolean ignoreInventoryQuantity
    )   {

        logger.debug("start generate pre-printed lpn label with receipt line id: {}", id);
        return receiptService.generateReceiptLinePrePrintLPNLabelInBatch(id, lpn, inventoryQuantity, ignoreInventoryQuantity,
                count,
                locale, printerName);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/{id}/pre-print-lpn-label/batch", method = RequestMethod.POST)
    public ReportHistory generateReceiptPrePrintLPNLabelInBatch(
            @PathVariable Long id,
            @RequestParam(name = "lpn", defaultValue = "", required = false) String lpn,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName,
            @RequestBody PrintingLPNByReceiptParameters printingLPNByReceiptParameters
    )  {

        logger.debug("start generate pre-printed lpn label with receipt id: {}", id);
        logger.debug("lpn : {}", lpn);
        logger.debug("printerName : {}", printerName);
        logger.debug("printingLPNByReceiptParameters : {}", printingLPNByReceiptParameters.toString());
        return receiptService.generateReceiptPrePrintLPNLabelInBatch(id, lpn, printingLPNByReceiptParameters, locale, printerName);
    }


    @BillableEndpoint
    @RequestMapping(value="/receipts/receipt-lines/{id}/pre-print-lpn-report/batch", method = RequestMethod.POST)
    public ReportHistory generatePrePrintLPNReportInBatch(
            @PathVariable Long id,
            @RequestParam String lpn,
            @RequestParam(name = "quantity", defaultValue = "", required = false) Long inventoryQuantity,
            @RequestParam(name = "count", defaultValue = "1", required = false) Integer count,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName,
            @RequestParam(name = "ignoreInventoryQuantity", defaultValue = "false", required = false) Boolean ignoreInventoryQuantity
    )  {

        logger.debug("start generate pre-printed lpn report with id: {}", id);
        return receiptService.generateReceiptLinePrePrintLPNReportInBatch(id, lpn, inventoryQuantity, ignoreInventoryQuantity,
                count,   locale, printerName);
    }


    @RequestMapping(value="/receipts/lines/available-for-mps", method = RequestMethod.GET)
    public List<ReceiptLine> getAvailableReceiptLinesForMPS(
            @RequestParam Long warehouseId,
            @RequestParam Long itemId) {

        return receiptLineService.getAvailableReceiptLinesForMPS(warehouseId,
                itemId);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/receipts/upload")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public ResponseBodyWrapper uploadReceipts(Long companyId,
                                              Long warehouseId,
                                              @RequestParam(name = "ignoreUnknownFields", defaultValue = "false", required = false) Boolean ignoreUnknownFields,
                                              @RequestParam("file") MultipartFile file) {

        try {
            File localFile = uploadFileService.convertToCSVFile(
                    companyId, warehouseId, "receipts", fileService.saveFile(file), ignoreUnknownFields);
            // fileService.validateCSVFile(companyId, warehouseId, "receipts", localFile);
            String fileUploadProgressKey = receiptService.saveReceiptData(warehouseId, localFile);
            return  ResponseBodyWrapper.success(fileUploadProgressKey);
        }
        catch (Exception ex) {
            return new ResponseBodyWrapper(-1, ex.getMessage(), "");
        }
    }

    @RequestMapping(method=RequestMethod.GET, value="/receipts/upload/progress")
    public ResponseBodyWrapper getReceiptFileUploadProgress(Long warehouseId,
                                                                       String key) throws IOException {



        return  ResponseBodyWrapper.success(
                String.format("%.2f",receiptService.getReceiptFileUploadProgress(key)));
    }
    @RequestMapping(method=RequestMethod.GET, value="/receipts/upload/result")
    public List<FileUploadResult> getReceiptFileUploadResult(Long warehouseId,
                                                      String key) throws IOException {


        return receiptService.getReceiptFileUploadResult(warehouseId, key);
    }


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/receipts/receiving-inventory/upload")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Receipt", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_Receipt", allEntries = true),
            }
    )
    public ResponseBodyWrapper updateReceivingInventory(Long companyId, Long warehouseId,
                                                        @RequestParam(name = "ignoreUnknownFields", defaultValue = "false", required = false) Boolean ignoreUnknownFields,
                                                        @RequestParam(name = "removeExistingInventoryWithSameLPN", defaultValue = "true", required = false) Boolean removeExistingInventoryWithSameLPN,
                                                        @RequestParam(name = "emptyLocation", defaultValue = "true", required = false) Boolean emptyLocation,
                                                        @RequestParam("file") MultipartFile file) throws IOException {


        try {

            File localFile = uploadFileService.convertToCSVFile(
                    companyId, warehouseId, "receiving-inventories", fileService.saveFile(file), ignoreUnknownFields);

            String fileUploadProgressKey = receiptService.saveReceivingInventoryData(warehouseId, localFile, removeExistingInventoryWithSameLPN, emptyLocation);
            return  ResponseBodyWrapper.success(fileUploadProgressKey);
        }
        catch (Exception ex) {
            return new ResponseBodyWrapper(-1, ex.getMessage(), "");
        }

    }

    @RequestMapping(method=RequestMethod.GET, value="/receipts/receiving-inventory/upload/progress")
    public ResponseBodyWrapper getReceivingInventoryFileUploadProgress(Long warehouseId,
                                                     String key) throws IOException {



        return  ResponseBodyWrapper.success(
                String.format("%.2f",receiptService.getReceivingInventoryFileUploadProgress(key)));
    }
    @RequestMapping(method=RequestMethod.GET, value="/receipts/receiving-inventory/upload/result")
    public List<FileUploadResult> getFileUploadResult(Long warehouseId,
                                                     String key) throws IOException {


        return receiptService.getReceivingInventoryFileUploadResult(warehouseId, key);
    }

    @RequestMapping(method=RequestMethod.PUT, value="/receipts/{receiptId}/billable-activities")
    public ReceiptBillableActivity addReceiptBillableActivity(Long warehouseId,
                                                              @PathVariable Long receiptId,
                                                              @RequestBody ReceiptBillableActivity receiptBillableActivity) throws IOException {


        return receiptBillableActivityService.addReceiptBillableActivity(receiptId, receiptBillableActivity);
    }

    @RequestMapping(method=RequestMethod.DELETE, value="/receipts/{receiptId}/billable-activities/{id}")
    public ResponseBodyWrapper<String> removeReceiptBillableActivity(Long warehouseId,
                                                              @PathVariable Long receiptId,
                                                                     @PathVariable Long id) throws IOException {


        receiptBillableActivityService.removeReceiptBillableActivity(id);
        return ResponseBodyWrapper.success("receipt billable activity is removed");
    }


    @RequestMapping(method=RequestMethod.POST, value="/receipts/{receiptId}/billable-activities/{id}")
    public ReceiptBillableActivity changeReceiptBillableActivity(Long warehouseId,
                                                              @PathVariable Long receiptId,
                                                                 @PathVariable Long id,
                                                              @RequestBody ReceiptBillableActivity receiptBillableActivity) throws IOException {


        return receiptBillableActivityService.changeReceiptBillableActivity(receiptBillableActivity);
    }

    @RequestMapping(method=RequestMethod.PUT, value="/receipts/lines/{receiptLineId}/billable-activities")
    public ReceiptLineBillableActivity addReceiptLineBillableActivity(Long warehouseId,
                                                              @PathVariable Long receiptLineId,
                                                              @RequestBody ReceiptLineBillableActivity receiptLineBillableActivity) throws IOException {


        return receiptLineBillableActivityService.addReceiptLineBillableActivity(receiptLineId, receiptLineBillableActivity);
    }
    @RequestMapping(method=RequestMethod.DELETE, value="/receipts/lines/{receiptLineId}/billable-activities/{id}")
    public ResponseBodyWrapper<String> removeReceiptLineBillableActivity(Long warehouseId,
                                                                         @PathVariable Long receiptLineId,
                                                                         @PathVariable Long id) throws IOException {


        receiptLineBillableActivityService.removeReceiptLineBillableActivity(id);
        return ResponseBodyWrapper.success("receipt billable activity is removed");
    }
    @RequestMapping(method=RequestMethod.POST, value="/receipts/lines/{receiptLineId}/billable-activities/{id}")
    public ReceiptLineBillableActivity changeReceiptLineBillableActivity(Long warehouseId,
                                                                 @PathVariable Long receiptId,
                                                                 @PathVariable Long id,
                                                                 @RequestBody ReceiptLineBillableActivity receiptLineBillableActivity) throws IOException {


        return receiptLineBillableActivityService.changeReceiptLineBillableActivity(receiptLineBillableActivity);
    }

}
