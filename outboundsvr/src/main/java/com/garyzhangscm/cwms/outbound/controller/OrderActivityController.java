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


import com.garyzhangscm.cwms.outbound.model.OrderActivity;
import com.garyzhangscm.cwms.outbound.service.OrderActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class OrderActivityController {
    @Autowired
    private OrderActivityService orderActivityService;

    @RequestMapping(value="/order-activities", method = RequestMethod.GET)
    public List<OrderActivity> findAllOrderActivities(@RequestParam Long warehouseId,
                                                      @RequestParam(name="beginDateTime", required = false, defaultValue = "") String beginDateTime,
                                                      @RequestParam(name="endDateTime", required = false, defaultValue = "") String endDateTime,
                                                      @RequestParam(name="date", required = false, defaultValue = "") String date,
                                                      @RequestParam(name="username", required = false, defaultValue = "") String username,
                                                      @RequestParam(name="rfCode", required = false, defaultValue = "") String rfCode,
                                                      @RequestParam(name="orderNumber", required = false, defaultValue = "") String orderNumber,
                                                      @RequestParam(name="shipmentNumber", required = false, defaultValue = "") String shipmentNumber,
                                                      @RequestParam(name="shipmentLineNumber", required = false, defaultValue = "") String shipmentLineNumber,
                                                      @RequestParam(name="pickNumber", required = false, defaultValue = "") String pickNumber,
                                                      @RequestParam(name="orderId", required = false, defaultValue = "") Long orderId,
                                                      @RequestParam(name="shipmentId", required = false, defaultValue = "") Long shipmentId,
                                                      @RequestParam(name="shipmentLineId", required = false, defaultValue = "") Long shipmentLineId,
                                                      @RequestParam(name="pickId", required = false, defaultValue = "") Long pickId,
                                                      @RequestParam(name="shortAllocationId", required = false, defaultValue = "") Long shortAllocationId) {
        return orderActivityService.findAll(warehouseId,
                beginDateTime,
                endDateTime,
                date,
                orderNumber,
                orderId,
                shipmentNumber,
                shipmentId,
                shipmentLineNumber,
                shipmentLineId,
                pickNumber,
                pickId,
                shortAllocationId,
                username,
                rfCode
        );
    }


}
