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
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.service.*;
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
                                         @RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails,
                                         ClientRestriction clientRestriction) {
        return receiptService.findAll(warehouseId, number, receiptStatusList,
                supplierId, supplierName,
                clientId, clientName,
                checkInStartTime, checkInEndTime, checkInDate, purchaseOrderId, loadDetails, clientRestriction);
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
    public Receipt findReceipt(@PathVariable Long id) {
        return receiptService.findById(id);
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
    @RequestMapping(value="/receipts/{id}/lines", method = RequestMethod.POST)
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
                             @RequestParam(name = "receiveToStage", defaultValue = "false", required = false) Boolean receiveToStage,
                             @RequestParam(name = "stageLocation", defaultValue = "", required = false) String stageLocation) {
            return receiptLineService.receive(receiptId, receiptLineId, inventory, receiveToStage, stageLocation);
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
                             @RequestBody List<Inventory> inventoryList) {
        return receiptLineService.receive(receiptId, receiptLineId, inventoryList);
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
            @RequestParam(name = "quantity", defaultValue = "", required = false) Long quantity,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName
    ) throws JsonProcessingException {

        logger.debug("start generate pre-printed lpn label with id: {}", id);
        return receiptService.generatePrePrintLPNLabel(id, lpn, quantity, locale, printerName);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/receipt-lines/{id}/pre-print-lpn-report", method = RequestMethod.POST)
    public ReportHistory generatePrePrintLPNReport(
            @PathVariable Long id,
            @RequestParam String lpn,
            @RequestParam(name = "quantity", defaultValue = "", required = false) Long quantity,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName
    ) throws JsonProcessingException {

        logger.debug("start generate pre-printed lpn report with id: {}", id);
        return receiptService.generatePrePrintLPNReport(id, lpn, quantity, locale, printerName);
    }


    @BillableEndpoint
    @RequestMapping(value="/receipts/receipt-lines/{id}/pre-print-lpn-label/batch", method = RequestMethod.POST)
    public ReportHistory generatePrePrintLPNLabelInBatch(
            @PathVariable Long id,
            @RequestParam String lpn,
            @RequestParam(name = "quantity", defaultValue = "", required = false) Long lpnQuantity,
            @RequestParam(name = "count", defaultValue = "1", required = false) Integer count,
            @RequestParam(name = "copies", defaultValue = "1", required = false) Integer copies,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName
    ) throws JsonProcessingException {

        logger.debug("start generate pre-printed lpn label with id: {}", id);
        return receiptService.generatePrePrintLPNLabelInBatch(id, lpn, lpnQuantity, count, copies, locale, printerName);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/receipt-lines/{id}/pre-print-lpn-report/batch", method = RequestMethod.POST)
    public ReportHistory generatePrePrintLPNReportInBatch(
            @PathVariable Long id,
            @RequestParam String lpn,
            @RequestParam(name = "quantity", defaultValue = "", required = false) Long lpnQuantity,
            @RequestParam(name = "count", defaultValue = "1", required = false) Integer count,
            @RequestParam(name = "copies", defaultValue = "1", required = false) Integer copies,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName
    ) throws JsonProcessingException {

        logger.debug("start generate pre-printed lpn report with id: {}", id);
        return receiptService.generatePrePrintLPNReportInBatch(id, lpn, lpnQuantity, count, copies, locale, printerName);
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
    public ResponseBodyWrapper uploadReceipts(Long warehouseId,
                                            @RequestParam("file") MultipartFile file) throws IOException {


        File localFile = fileService.saveFile(file);
        String fileUploadProgressKey = receiptService.saveReceiptData(warehouseId, localFile);
        return  ResponseBodyWrapper.success(fileUploadProgressKey);
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
    public ResponseBodyWrapper updateReceivingInventory(Long warehouseId,
                                              @RequestParam("file") MultipartFile file) throws IOException {


        File localFile = fileService.saveFile(file);

        String fileUploadProgressKey = receiptService.saveReceivingInventoryData(warehouseId, localFile);
        return  ResponseBodyWrapper.success(fileUploadProgressKey);
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
