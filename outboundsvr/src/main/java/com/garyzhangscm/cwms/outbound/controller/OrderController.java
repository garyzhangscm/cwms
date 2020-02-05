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

import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.OrderLine;
import com.garyzhangscm.cwms.outbound.service.OrderLineService;
import com.garyzhangscm.cwms.outbound.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
public class OrderController {
    @Autowired
    OrderService orderService;
    @Autowired
    OrderLineService orderLineService;


    @RequestMapping(value="/orders", method = RequestMethod.GET)
    public List<Order> findAllOrders(@RequestParam(name="number", required = false, defaultValue = "") String number) {
        return orderService.findAll(number);
    }

    @RequestMapping(value="/orders", method = RequestMethod.POST)
    public Order addOrders(@RequestBody Order order) {
        return orderService.save(order);
    }


    @RequestMapping(value="/orders/{id}", method = RequestMethod.GET)
    public Order findOrder(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @RequestMapping(value="/orders/{id}", method = RequestMethod.PUT)
    public Order changeOrder(@RequestBody Order order){
        return orderService.save(order);
    }

    @RequestMapping(value="/orders", method = RequestMethod.DELETE)
    public void removeOrders(@RequestParam(name = "order_ids", required = false, defaultValue = "") String orderIds) {
        orderService.delete(orderIds);
    }


    @RequestMapping(value="/orders/{id}/next-line-number", method = RequestMethod.GET)
    public ResponseBodyWrapper getNextOrderLineNumber(@PathVariable Long id) {
        return ResponseBodyWrapper.success(orderService.getNextOrderLineNumber(id));
    }

}
