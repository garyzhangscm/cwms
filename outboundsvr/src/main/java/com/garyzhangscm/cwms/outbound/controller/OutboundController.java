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
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OutboundController {

    private static final Logger logger = LoggerFactory.getLogger(OutboundController.class);

    @Autowired
    private OrderService orderService;
    @Autowired
    OrderLineService orderLineService;
    @Autowired
    PickConfirmStrategyService pickConfirmStrategyService;
    @Autowired
    AllocationConfigurationService allocationConfigurationService;
    @Autowired
    EmergencyReplenishmentConfigurationService emergencyReplenishmentConfigurationService;
    @Autowired
    PickService pickService;
    @Autowired
    ShortAllocationService shortAllocationService;

    @Autowired
    private TrailerAppointmentService trailerAppointmentService;


    @RequestMapping(value="/outbound-configuration/item-override", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> handleItemOverride(
            @RequestParam Long warehouseId,
            @RequestParam Long oldItemId,
            @RequestParam Long newItemId
    ) {
        orderLineService.handleItemOverride(warehouseId,
                oldItemId, newItemId);

        pickConfirmStrategyService.handleItemOverride(warehouseId,
                oldItemId, newItemId);
        allocationConfigurationService.handleItemOverride(warehouseId,
                oldItemId, newItemId);
        emergencyReplenishmentConfigurationService.handleItemOverride(warehouseId,
                oldItemId, newItemId);
        pickService.handleItemOverride(warehouseId,
                oldItemId, newItemId);
        shortAllocationService.handleItemOverride(warehouseId,
                oldItemId, newItemId);

        return ResponseBodyWrapper.success("success");
    }


    @RequestMapping(value="/trailer-appointments/init", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> initTrailerAppointment(
            @RequestBody TrailerAppointment trailerAppointment
            ) {
        trailerAppointmentService.initTrailerAppointment(trailerAppointment);


        return ResponseBodyWrapper.success("success");
    }



    @ClientValidationEndpoint
    @RequestMapping(value="/query/orders", method = RequestMethod.GET)
    public List<OrderQueryWrapper> getOrders(@RequestParam Long warehouseId,
                                             @RequestParam(name="number", required = false, defaultValue = "") String number,
                                             @RequestParam(name="numbers", required = false, defaultValue = "") String numbers,
                                             @RequestParam(name="status", required = false, defaultValue = "") String status,
                                             ClientRestriction clientRestriction) {
       return orderService.getOrdersForQuery(warehouseId, number, numbers, status, clientRestriction);
    }

}
