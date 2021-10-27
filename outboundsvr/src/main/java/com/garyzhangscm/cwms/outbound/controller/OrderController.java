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

package com.garyzhangscm.cwms.outbound.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.OrderLine;
import com.garyzhangscm.cwms.outbound.model.ReportHistory;
import com.garyzhangscm.cwms.outbound.service.OrderLineService;
import com.garyzhangscm.cwms.outbound.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    OrderService orderService;
    @Autowired
    OrderLineService orderLineService;


    @RequestMapping(value="/orders", method = RequestMethod.GET)
    public List<Order> findAllOrders(@RequestParam Long warehouseId,
                                     @RequestParam(name="number", required = false, defaultValue = "") String number,
                                     @RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails) {
        logger.debug("Start to find order by number {}", number);
        return orderService.findAll(warehouseId, number, loadDetails);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders", method = RequestMethod.POST)
    public Order addOrders(@RequestBody Order order) {
        return orderService.addOrders(order);
    }


    @RequestMapping(value="/orders/{id}", method = RequestMethod.GET)
    public Order findOrder(@PathVariable Long id) {
        return orderService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/orders/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeOrder(@PathVariable Long id) {

         orderService.removeOrder(id);
         return ResponseBodyWrapper.success("Success");
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}", method = RequestMethod.PUT)
    public Order changeOrder(@RequestBody Order order){
        return orderService.save(order);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/allocate", method = RequestMethod.POST)
    public Order allocateOrder(@PathVariable Long id){

        return orderService.allocate(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/change-stage-loctions", method = RequestMethod.POST)
    public Order changeAssignedStageLocations(@PathVariable Long id,
                                      @RequestParam(name = "locationGroupId", required = false, defaultValue = "") Long locationGroupId,
                                      @RequestParam(name = "locationId", required = false, defaultValue = "") Long locationId){

        return orderService.changeAssignedStageLocations(id, locationGroupId, locationId);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/complete", method = RequestMethod.POST)
    public Order completeOrder(@PathVariable Long id){
        return orderService.completeOrder(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/stage", method = RequestMethod.POST)
    public Order stageOrder(@PathVariable Long id,
                            @RequestParam(name = "ignoreUnfinishedPicks", required = false, defaultValue = "false") boolean ignoreUnfinishedPicks){
        return orderService.stage(id, ignoreUnfinishedPicks);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/load", method = RequestMethod.POST)
    public Order loadOrder(@PathVariable Long id,
                           @RequestParam(name = "ignoreUnfinishedPicks", required = false, defaultValue = "false") boolean ignoreUnfinishedPicks){
        return orderService.load(id, ignoreUnfinishedPicks);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/dispatch", method = RequestMethod.POST)
    public Order dispatchOrder(@PathVariable Long id,
                               @RequestParam(name = "ignoreUnfinishedPicks", required = false, defaultValue = "false") boolean ignoreUnfinishedPicks){
        return orderService.dispatch(id, ignoreUnfinishedPicks);
    }


    @RequestMapping(value="/orders/{id}/next-line-number", method = RequestMethod.GET)
    public ResponseBodyWrapper getNextOrderLineNumber(@PathVariable Long id) {
        return ResponseBodyWrapper.success(orderService.getNextOrderLineNumber(id));
    }


    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/pick-report", method = RequestMethod.POST)
    public ReportHistory generateOrderPickReport(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        logger.debug("start print pick sheet for order with id: {}", id);
        return orderService.generatePickReportByOrder(id, locale);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/packing-list-report", method = RequestMethod.POST)
    public ReportHistory generatePackingListReport(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        logger.debug("start print packing list for order with id: {}", id);
        return orderService.generatePackingListByOrder(id, locale);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/bill-of-lading-report", method = RequestMethod.POST)
    public ReportHistory generateBillOfLadingReport(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        logger.debug("start print bill of lading for order with id: {}", id);
        return orderService.generateBillOfLadingByOrder(id, locale);
    }



    @RequestMapping(value="/orders-with-open-pick", method = RequestMethod.GET)
    public List<Order> getOrdersWithOpenPick(@RequestParam Long warehouseId) {
        return orderService.getOrdersWithOpenPick(warehouseId);
    }
}
