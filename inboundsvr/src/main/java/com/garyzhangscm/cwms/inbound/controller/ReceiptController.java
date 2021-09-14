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
import com.garyzhangscm.cwms.inbound.exception.GenericException;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.service.ReceiptLineService;
import com.garyzhangscm.cwms.inbound.service.ReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class ReceiptController {
    @Autowired
    ReceiptService receiptService;
    @Autowired
    ReceiptLineService receiptLineService;


    @RequestMapping(value="/receipts", method = RequestMethod.GET)
    public List<Receipt> findAllReceipts(@RequestParam Long warehouseId,
                                         @RequestParam(name="number", required = false, defaultValue = "") String number,
                                         @RequestParam(name="receipt_status_list", required = false, defaultValue = "") String receiptStatusList) {
        return receiptService.findAll(warehouseId, number, receiptStatusList);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts", method = RequestMethod.POST)
    public Receipt addReceipts(@RequestBody Receipt receipt) {
        return receiptService.save(receipt);
    }


    @RequestMapping(value="/receipts/{id}", method = RequestMethod.GET)
    public Receipt findReceipt(@PathVariable Long id) {
        return receiptService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/{id}", method = RequestMethod.PUT)
    public Receipt changeReceipt(@PathVariable Long id,
                                 @RequestBody Receipt receipt){
        return receiptService.changeReceipt(id, receipt);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/{id}/complete", method = RequestMethod.POST)
    public Receipt completeReceipt(@PathVariable Long id){
        return receiptService.completeReceipt(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts", method = RequestMethod.DELETE)
    public void removeReceipts(@RequestParam(name = "receipt_ids", required = false, defaultValue = "") String receiptIds) {
        receiptService.delete(receiptIds);
    }


    @RequestMapping(value="/receipts/{id}/next-line-number", method = RequestMethod.GET)
    public ResponseBodyWrapper getNextReceiptLineNumber(@PathVariable Long id) {
        return ResponseBodyWrapper.success(receiptService.getNextReceiptLineNumber(id));
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/{id}/lines", method = RequestMethod.POST)
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
    public Inventory receive(@PathVariable Long receiptId,
                               @PathVariable Long receiptLineId,
                               @RequestBody Inventory inventory) {
            return receiptLineService.receive(receiptId, receiptLineId, inventory);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/{receiptId}/lines/{receiptLineId}/reverse", method = RequestMethod.POST)
    public ReceiptLine reverseReceivedInventory(@PathVariable Long receiptId,
                                     @PathVariable Long receiptLineId,
                                     @RequestParam Long quantity) {
        return receiptLineService.reverseReceivedInventory(receiptId, receiptLineId, quantity);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/{id}/check-in", method = RequestMethod.PUT)
    public Receipt checkInReceipt(@PathVariable Long id){

            return receiptService.checkInReceipt(id);
    }
    @RequestMapping(value="/receipts/{id}/inventories", method = RequestMethod.GET)
    public List<Inventory> findInventoryByReceipt(@PathVariable Long id){
        return receiptService.findInventoryByReceipt(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/receipts/lines", method = RequestMethod.DELETE)
    public void removeReceiptLine(@RequestParam String receiptLineIds) {
        receiptLineService.delete(receiptLineIds);
    }


    @BillableEndpoint
    @RequestMapping(value="/receipts/{id}/receiving-document", method = RequestMethod.POST)
    public ReportHistory generateReceivingDocument(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        return receiptService.generateReceivingDocument(id, locale);
    }

    @BillableEndpoint
    @RequestMapping(value="/receipts/{id}/putaway-document", method = RequestMethod.POST)
    public ReportHistory generatePutawayDocument(
            @PathVariable Long id,
            @RequestParam(name = "inventoryIds", defaultValue = "", required = false) String inventoryIds,
            @RequestParam(name = "notPutawayInventoryOnly", defaultValue = "false", required = false) Boolean notPutawayInventoryOnly,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        return receiptService.generatePutawayDocument(id, locale, inventoryIds, notPutawayInventoryOnly);
    }
}
