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

import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.service.PurchaseOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class PurchaseOrderController {
    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderController.class);
    @Autowired
    private PurchaseOrderService purchaseOrderService;


    @RequestMapping(value="/purchase-orders", method = RequestMethod.GET)
    public List<PurchaseOrder> findAllReceipts(@RequestParam Long warehouseId,
                                         @RequestParam(name="number", required = false, defaultValue = "") String number,
                                         @RequestParam(name="purchasOrderStatusList", required = false, defaultValue = "") String purchasOrderStatusList,
                                         @RequestParam(name="supplierName", required = false, defaultValue = "") String supplierName,
                                         @RequestParam(name="supplierId", required = false, defaultValue = "") Long supplierId,
                                         @RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails) {
        return purchaseOrderService.findAll(warehouseId, number, purchasOrderStatusList, supplierId,
                supplierName,  loadDetails);
    }



    @RequestMapping(value="/purchase-orders/{id}", method = RequestMethod.GET)
    public PurchaseOrder findPurchaseOrder(@PathVariable Long id) {
        return purchaseOrderService.findById(id);
    }


    @RequestMapping(value="/purchase-orders/{id}/create-receipt", method = RequestMethod.POST)
    public Receipt createReceiptFromPurchaseOrder(@PathVariable Long id,
                                                  @RequestParam Long warehouseId,
                                                  @RequestParam String receiptNumber,
                                                  @RequestParam(name="allowUnexpectedItem", required = false, defaultValue = "false") Boolean allowUnexpectedItem,
                                                  @RequestBody List<CreateReceiptFromPurchaseOrderRequest> createReceiptFromPurchaseOrderRequests) {
        // receiptQuantityMap
        // key: purchase order line id
        // value: receipt line quantity
        logger.debug("createReceiptFromPurchaseOrder with PO id {}, new receipt number {}",
                id, receiptNumber);
        logger.debug("rcreateReceiptFromPurchaseOrderRequests \n{}", createReceiptFromPurchaseOrderRequests);
        // convert the receipt quantities list into map so it will be easy and efficient to process
        Map<Long, Long> receiptQuantityMap = new HashMap<>();

        createReceiptFromPurchaseOrderRequests.stream().filter(
                createReceiptFromPurchaseOrderRequest -> createReceiptFromPurchaseOrderRequest.getQuantity() > 0
        ).forEach(
                createReceiptFromPurchaseOrderRequest ->  receiptQuantityMap.put(
                        createReceiptFromPurchaseOrderRequest.getPurchaseOrderLineId(),
                        createReceiptFromPurchaseOrderRequest.getQuantity()
                )
        );
        return purchaseOrderService.createReceiptFromPurchaseOrder(id, receiptNumber, allowUnexpectedItem, receiptQuantityMap);
    }
}
