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
import com.garyzhangscm.cwms.inbound.service.CustomerReturnOrderLineService;
import com.garyzhangscm.cwms.inbound.service.CustomerReturnOrderService;
import com.garyzhangscm.cwms.inbound.service.ReceiptLineService;
import com.garyzhangscm.cwms.inbound.service.ReceiptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CustomerReturnOrderController {
    private static final Logger logger = LoggerFactory.getLogger(CustomerReturnOrderController.class);
    @Autowired
    CustomerReturnOrderService customerReturnOrderService;
    @Autowired
    CustomerReturnOrderLineService customerReturnOrderLineService;


    @RequestMapping(value="/customer-return-orders", method = RequestMethod.GET)
    public List<CustomerReturnOrder> findAllCustomerReturnOrders(@RequestParam Long warehouseId,
                                         @RequestParam(name="number", required = false, defaultValue = "") String number,
                                         @RequestParam(name="status_list", required = false, defaultValue = "") String statusList,
                                         @RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails) {
        return customerReturnOrderService.findAll(warehouseId, number, statusList, loadDetails);
    }

    @BillableEndpoint
    @RequestMapping(value="/customer-return-orders", method = RequestMethod.POST)
    public CustomerReturnOrder addCustomerReturnOrder(@RequestBody CustomerReturnOrder customerReturnOrder) {
        return customerReturnOrderService.addCustomerReturnOrder(customerReturnOrder);
    }


    @RequestMapping(value="/customer-return-orders/{id}", method = RequestMethod.GET)
    public CustomerReturnOrder findCustomerReturnOrder(@PathVariable Long id) {
        return customerReturnOrderService.findById(id);
    }

    @RequestMapping(value="/customer-return-orders/lines/{id}", method = RequestMethod.GET)
    public CustomerReturnOrderLine findCustomerReturnOrderLine(@PathVariable Long id) {
        return customerReturnOrderLineService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/customer-return-orders/{id}", method = RequestMethod.PUT)
    public CustomerReturnOrder changeCustomerReturnOrder(@PathVariable Long id,
                                 @RequestBody CustomerReturnOrder customerReturnOrder){
        return customerReturnOrderService.changeCustomerReturnOrder(id, customerReturnOrder);
    }

    @BillableEndpoint
    @RequestMapping(value="/customer-return-orders/{id}/complete", method = RequestMethod.POST)
    public CustomerReturnOrder completeCustomerReturnOrder(@PathVariable Long id){
        return customerReturnOrderService.completeCustomerReturnOrder(id);
    }


    @RequestMapping(value="/customer-return-orders/{id}/next-line-number", method = RequestMethod.GET)
    public ResponseBodyWrapper getNextReceiptLineNumber(@PathVariable Long id) {
        return ResponseBodyWrapper.success(customerReturnOrderService.getNextReceiptLineNumber(id));
    }

    @BillableEndpoint
    @RequestMapping(value="/customer-return-orders/{id}/lines", method = RequestMethod.POST)
    public CustomerReturnOrderLine addCustomerReturnOrderLine(@PathVariable Long id,
                                      @RequestBody CustomerReturnOrderLine customerReturnOrderLine) {
        return customerReturnOrderLineService.addCustomerReturnOrderLine(id, customerReturnOrderLine);
    }

    /**
     * Receive inventory from a customer return order line
     * @param customerReturnOrderId Customer Return Order ID
     * @param customerReturnOrderLineId Customer Return Order Line id
     * @param inventory inventory to be received
     * @return
     */
    @BillableEndpoint
    @RequestMapping(value="/customer-return-orders/{customerReturnOrderId}/lines/{customerReturnOrderLineId}/receive", method = RequestMethod.POST)
    public Inventory receive(@PathVariable Long customerReturnOrderId,
                               @PathVariable Long customerReturnOrderLineId,
                               @RequestBody Inventory inventory) {
            return customerReturnOrderLineService.receive(customerReturnOrderId, customerReturnOrderLineId, inventory);
    }


    /**
     * Recalculate the qc quantity for the customer return order line. We can specify the qc quantity and percentage, or let
     * the system run the configuration again to refresh the qc quantity required
     * @param receiptLineId
     * @param qcQuantity
     * @param qcPercentage
     * @return
     */
    @BillableEndpoint
    @RequestMapping(value="/customer-return-orders/lines/{lineId}/recalculate-qc-quantity", method = RequestMethod.POST)
    public CustomerReturnOrderLine recalculateQCQuantity(@PathVariable Long receiptLineId,
                                           @RequestParam(name = "qcQuantity", required = false, defaultValue = "") Long qcQuantity,
                                           @RequestParam(name = "qcPercentage", required = false, defaultValue = "") Double qcPercentage) {
        return customerReturnOrderLineService.recalculateQCQuantity(receiptLineId, qcQuantity, qcPercentage);
    }

    /***
     * Receive multiple LPNs with the same quantity
     * @param customerReturnOrderId
     * @param customerReturnOrderLineId
     * @param inventoryList
     * @return
     */
    @BillableEndpoint
    @RequestMapping(value="/customer-return-orders/{customerReturnOrderId}/lines/{customerReturnOrderLineId}/receive-multiple-lpns", method = RequestMethod.POST)
    public List<Inventory> receive(@PathVariable Long customerReturnOrderId,
                                   @PathVariable Long customerReturnOrderLineId,
                             @RequestBody List<Inventory> inventoryList) {
        return customerReturnOrderLineService.receive(customerReturnOrderId, customerReturnOrderLineId, inventoryList);
    }

    @BillableEndpoint
    @RequestMapping(value="/customer-return-orders/{customerReturnOrderId}/lines/{customerReturnOrderLineId}/reverse", method = RequestMethod.POST)
    public CustomerReturnOrderLine reverseReceivedInventory(@PathVariable Long customerReturnOrderId,
                                                 @PathVariable Long customerReturnOrderLineId,
                                                 @RequestParam Long quantity,
                                                @RequestParam(name = "inboundQCRequired", defaultValue = "", required = false) Boolean inboundQCRequired,
                                                @RequestParam(name = "reverseQCQuantity", defaultValue = "", required = false) Boolean reverseQCQuantity) {
        return customerReturnOrderLineService.reverseReceivedInventory(customerReturnOrderId, customerReturnOrderLineId, quantity, inboundQCRequired, reverseQCQuantity);
    }

    @RequestMapping(value="/customer-return-orders/{id}/inventories", method = RequestMethod.GET)
    public List<Inventory> findInventoryByCustomerReturnOrder(@PathVariable Long id){
        return customerReturnOrderService.findInventoryByCustomerReturnOrder(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/customer-return-orders/{id}/document", method = RequestMethod.POST)
    public ReportHistory generateDocument(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        return customerReturnOrderService.generateReceivingDocument(id, locale);
    }

    @BillableEndpoint
    @RequestMapping(value="/customer-return-orders/{id}/putaway-document", method = RequestMethod.POST)
    public ReportHistory generatePutawayDocument(
            @PathVariable Long id,
            @RequestParam(name = "inventoryIds", defaultValue = "", required = false) String inventoryIds,
            @RequestParam(name = "notPutawayInventoryOnly", defaultValue = "false", required = false) Boolean notPutawayInventoryOnly,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        return customerReturnOrderService.generatePutawayDocument(id, locale, inventoryIds, notPutawayInventoryOnly);
    }


}
