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


import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.outbound.model.OrderLine;
import com.garyzhangscm.cwms.outbound.service.OrderLineService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrderLineController {

    @Autowired
    OrderLineService orderLineService;


    @RequestMapping(value="/orders/lines", method = RequestMethod.GET)
    public List<OrderLine> findAllOrderLines(@RequestParam Long warehouseId,
                                             @RequestParam(name="shipmentId", required = false, defaultValue = "") Long shipmentId,
                                             @RequestParam(name = "orderNumber", required = false, defaultValue = "") String orderNumber,
                                             @RequestParam(name = "itemName", required = false, defaultValue = "") String itemName) {
        return orderLineService.findAll(warehouseId, shipmentId, orderNumber, itemName);
    }

    @RequestMapping(value="/orders/lines/{id}", method = RequestMethod.GET)
    public OrderLine findOrderLine(@PathVariable Long id) {
        return orderLineService.findById(id);
    }

    /**
     * Register a production line when there's one created for this order line
     * @param id order line id
     * @param productionPlanLineQuantity production plan line quantity
     * @return
     */
    @BillableEndpoint
    @RequestMapping(value="/orders/lines/{id}/production-plan-line/register", method = RequestMethod.POST)
    public OrderLine registerProductionPlanLine(@PathVariable Long id,
                                          @RequestParam Long productionPlanLineQuantity) {
        return orderLineService.registerProductionPlanLine(id, productionPlanLineQuantity);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/lines/{id}/production-plan/produced", method = RequestMethod.POST)
    public OrderLine registerProductionPlanProduced(@PathVariable Long id,
                                            @RequestParam Long producedQuantity) {
        return orderLineService.registerProductionPlanProduced(id, producedQuantity);
    }

    @RequestMapping(value="/orders/lines/production-plan-candidate", method = RequestMethod.GET)
    public List<OrderLine> findProductionPlanCandidate(@RequestParam Long warehouseId,
                                                    @RequestParam(name = "orderNumber", required = false, defaultValue = "") String orderNumber,
                                                       @RequestParam(name = "itemName", required = false, defaultValue = "") String itemName) {
        return orderLineService.findProductionPlanCandidate(warehouseId, orderNumber, itemName);
    }



    @RequestMapping(value="/orders/lines/available-for-mps", method = RequestMethod.GET)
    public List<OrderLine> getAvailableOrderLinesForMPS(
            @RequestParam Long warehouseId,
            @RequestParam Long itemId) {

        return orderLineService.getAvailableOrderLinesForMPS(warehouseId,
                itemId);
    }

}
