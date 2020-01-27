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

import com.garyzhangscm.cwms.inbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inbound.exception.GenericException;
import com.garyzhangscm.cwms.inbound.model.Inventory;
import com.garyzhangscm.cwms.inbound.model.Receipt;
import com.garyzhangscm.cwms.inbound.model.ReceiptLine;
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
    public List<Receipt> findAllReceipts(@RequestParam String warehouseName,
                                         @RequestParam(name="number", required = false, defaultValue = "") String number) {
        return receiptService.findAll(warehouseName, number);
    }

    @RequestMapping(value="/receipts", method = RequestMethod.POST)
    public Receipt addReceipts(@RequestBody Receipt receipt) {
        return receiptService.save(receipt);
    }


    @RequestMapping(value="/receipt/{id}", method = RequestMethod.GET)
    public Receipt findReceipt(@PathVariable Long id) {
        return receiptService.findById(id);
    }

    @RequestMapping(value="/receipt/{id}", method = RequestMethod.PUT)
    public Receipt changeReceipt(@RequestBody Receipt receipt){
        return receiptService.save(receipt);
    }

    @RequestMapping(value="/receipt", method = RequestMethod.DELETE)
    public void removeReceipts(@RequestParam(name = "receipt_ids", required = false, defaultValue = "") String receiptIds) {
        receiptService.delete(receiptIds);
    }


    @RequestMapping(value="/receipt/{id}/next-line-number", method = RequestMethod.GET)
    public ResponseBodyWrapper getNextReceiptLineNumber(@PathVariable Long id) {
        return ResponseBodyWrapper.success(receiptService.getNextReceiptLineNumber(id));
    }

    @RequestMapping(value="/receipt/{id}/lines", method = RequestMethod.POST)
    public ReceiptLine addReceiptLine(@PathVariable Long id,
                                      @RequestBody ReceiptLine receiptLine) {
        return receiptLineService.addReceiptLine(id, receiptLine);
    }

    @RequestMapping(value="/receipt/{receiptId}/line/{receiptLineId}/receive", method = RequestMethod.POST)
    public Inventory receive(@PathVariable Long receiptId,
                               @PathVariable Long receiptLineId,
                               @RequestBody Inventory inventory) {
        try {
            return receiptLineService.receive(receiptId, receiptLineId, inventory);
        }
        catch (IOException ex) {
            throw new GenericException(99999, ex.getMessage());
        }
    }


    @RequestMapping(value="/receipt/{id}/check-in", method = RequestMethod.PUT)
    public Receipt checkInReceipt(@PathVariable Long id){

        try {
            return receiptService.checkInReceipt(id);
        }
        catch (Exception ex) {
            throw new GenericException(99999, ex.getMessage());
        }
    }
    @RequestMapping(value="/receipt/{id}/inventories", method = RequestMethod.GET)
    public List<Inventory> findInventoryByReceipt(@PathVariable Long id){
        return receiptService.findInventoryByReceipt(id);
    }
}
