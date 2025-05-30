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
import com.garyzhangscm.cwms.outbound.service.OrderLineBillableActivityService;
import com.garyzhangscm.cwms.outbound.service.OrderLineService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class OrderLineController {

    @Autowired
    OrderLineService orderLineService;
    @Autowired
    private OrderLineBillableActivityService orderLineBillableActivityService;


    @RequestMapping(value="/orders/lines", method = RequestMethod.GET)
    @ClientValidationEndpoint
    public List<OrderLine> findAllOrderLines(@RequestParam Long warehouseId,
                                             @RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
                                             @RequestParam(name="shipmentId", required = false, defaultValue = "") Long shipmentId,
                                             @RequestParam(name = "orderNumber", required = false, defaultValue = "") String orderNumber,
                                             @RequestParam(name = "itemName", required = false, defaultValue = "") String itemName,
                                             @RequestParam(name = "itemId", required = false, defaultValue = "") Long itemId,
                                             @RequestParam(name = "inventoryStatusId", required = false, defaultValue = "") Long inventoryStatusId,
                                             ClientRestriction clientRestriction) {
        return orderLineService.findAll(warehouseId, clientId, shipmentId, orderNumber, itemName,
                itemId, inventoryStatusId, clientRestriction);
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
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public OrderLine registerProductionPlanLine(@PathVariable Long id,
                                          @RequestParam Long productionPlanLineQuantity) {
        return orderLineService.registerProductionPlanLine(id, productionPlanLineQuantity);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/lines/{id}/production-plan/produced", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
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


    @BillableEndpoint
    @RequestMapping(value="/orders/lines/{id}/add-request-return-quantity", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public OrderLine addRequestReturnQuantity(
            @RequestParam Long warehouseId,
            @PathVariable Long orderLineId,
            @RequestParam Long requestReturnQuantity) {

        return orderLineService.addRequestReturnQuantity(warehouseId, orderLineId,
                requestReturnQuantity);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/lines/{id}/add-actual-return-quantity", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public OrderLine addActualReturnQuantity(
            @RequestParam Long warehouseId,
            @PathVariable Long orderLineId,
            @RequestParam Long actualReturnQuantity) {

        return orderLineService.addActualReturnQuantity(warehouseId, orderLineId,
                actualReturnQuantity);
    }
    @RequestMapping(method=RequestMethod.PUT, value="/orders/lines/{orderLineId}/billable-activities")
    public OrderLineBillableActivity addOrderLineBillableActivity(Long warehouseId,
                                                                    @PathVariable Long orderLineId,
                                                                    @RequestBody OrderLineBillableActivity orderLineBillableActivity) throws IOException {


        return orderLineBillableActivityService.addOrderLineBillableActivity(orderLineId, orderLineBillableActivity);
    }
    @RequestMapping(method=RequestMethod.DELETE, value="/orders/lines/{orderLineId}/billable-activities/{id}")
    public ResponseBodyWrapper<String> removeOrderLineBillableActivity(Long warehouseId,
                                                                         @PathVariable Long orderLineId,
                                                                         @PathVariable Long id) throws IOException {


        orderLineBillableActivityService.removeOrderLineBillableActivity(id);
        return ResponseBodyWrapper.success("order billable activity is removed");
    }
    @RequestMapping(method=RequestMethod.POST, value="/orders/lines/{orderLineId}/billable-activities/{id}")
    public OrderLineBillableActivity changeOrderLineBillableActivity(Long warehouseId,
                                                                         @PathVariable Long orderId,
                                                                         @PathVariable Long id,
                                                                         @RequestBody OrderLineBillableActivity orderLineBillableActivity) throws IOException {


        return orderLineBillableActivityService.changeOrderLineBillableActivity(orderLineBillableActivity);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/lines/{orderLineId}/change-allocation-strategy-type", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public OrderLine changeAllocationStrategyType(
            @RequestParam Long warehouseId,
            @PathVariable Long orderLineId,
            @RequestParam String allocationStrategyType) {

        return orderLineService.changeAllocationStrategyType(warehouseId, orderLineId,
                allocationStrategyType);
    }

}
