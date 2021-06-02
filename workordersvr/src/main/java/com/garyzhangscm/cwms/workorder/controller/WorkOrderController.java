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

package com.garyzhangscm.cwms.workorder.controller;


import com.garyzhangscm.cwms.workorder.ResponseBodyWrapper;
import com.garyzhangscm.cwms.workorder.model.*;

import com.garyzhangscm.cwms.workorder.service.WorkOrderLineService;
import com.garyzhangscm.cwms.workorder.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WorkOrderController {
    @Autowired
    WorkOrderService workOrderService;

    @Autowired
    WorkOrderLineService workOrderLineService;


    @RequestMapping(value="/work-orders", method = RequestMethod.GET)
    public List<WorkOrder> findAllWorkOrders(@RequestParam Long warehouseId,
                                             @RequestParam(name="number", required = false, defaultValue = "") String number,
                                             @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                             @RequestParam(name="productionPlanId", required = false, defaultValue = "") Long productionPlanId) {
        return workOrderService.findAll(warehouseId, number, itemName, productionPlanId);
    }

    @RequestMapping(value="/work-orders", method = RequestMethod.POST)
    public WorkOrder addWorkOrder(@RequestBody WorkOrder workOrder) {
        return workOrderService.save(workOrder);
    }


    @RequestMapping(value="/work-orders/{id}", method = RequestMethod.GET)
    public WorkOrder findWorkOrder(@PathVariable Long id) {
        return workOrderService.findById(id);
    }

    @RequestMapping(value="/work-orders/{id}", method = RequestMethod.PUT)
    public WorkOrder changeWorkOrder(@RequestBody WorkOrder workOrder){
        return workOrderService.save(workOrder);
    }

    @RequestMapping(value="/work-orders", method = RequestMethod.DELETE)
    public void removeWorkOrders(@RequestParam(name = "workOrderIds", required = false, defaultValue = "") String workOrderIds) {
        workOrderService.delete(workOrderIds);
    }

    @RequestMapping(value="/work-orders/create-from-bom", method = RequestMethod.POST)
    public WorkOrder createWorkOrderFromBOM(@RequestParam Long billOfMaterialId,
                                            @RequestParam String workOrderNumber,
                                            @RequestParam Long expectedQuantity,
                                            @RequestParam(name="productionLineId", required = false) Long productionLineId) {
        return workOrderService.createWorkOrderFromBOM(billOfMaterialId,
                workOrderNumber, expectedQuantity, productionLineId);
    }

    @RequestMapping(value="/work-orders/{id}/allocate", method = RequestMethod.POST)
    public WorkOrder allocateWorkOrder(@PathVariable Long id) {
        return workOrderService.allocateWorkOrder(id);
    }

    @RequestMapping(value="/work-orders/lines/{id}/short-allocation-cancelled", method = RequestMethod.POST)
    public void registerShortAllocationCancelled(@PathVariable Long id,
                                           @RequestParam Long cancelledQuantity) {
        workOrderLineService.registerShortAllocationCancelled(id, cancelledQuantity);
    }

    @RequestMapping(value="/work-orders/lines/{id}", method = RequestMethod.GET)
    public WorkOrderLine findWorkOrderLine(@PathVariable Long id) {
        return workOrderLineService.findById(id);
    }

    @RequestMapping(value="/work-orders/lines/{id}/inventory-being-delivered", method = RequestMethod.POST)
    public WorkOrderLine changeDeliveredQuantity(@PathVariable Long id,
                                                 @RequestParam Long quantityBeingDelivered,
                                                 @RequestParam Long deliveredLocationId) {
        return workOrderLineService.changeDeliveredQuantity(id, quantityBeingDelivered, deliveredLocationId);
    }

    @RequestMapping(value="/work-orders/lines/{id}/pick-cancelled", method = RequestMethod.POST)
    public void registerPickCancelled(@PathVariable Long id,
                                           @RequestParam Long cancelledQuantity) {
        workOrderLineService.registerPickCancelled(id, cancelledQuantity);
    }

    /***
    @RequestMapping(value="/work-orders/{id}/change-production-line", method = RequestMethod.POST)
    public WorkOrder changeProductionLine(@PathVariable Long id,
                                  @RequestParam Long productionLineId) {
        return workOrderService.changeProductionLine(id, productionLineId);
    }
***/

    @RequestMapping(value="/work-orders/{id}/produced-inventory", method = RequestMethod.GET)
    public List<Inventory> getProducedInventory(@PathVariable Long id) {
        return workOrderService.getProducedInventory(id);
    }

    @RequestMapping(value="/work-orders/{id}/produced-by-product", method = RequestMethod.GET)
    public List<Inventory> getProducedByProduct(@PathVariable Long id) {
        return workOrderService.getProducedByProduct(id);
    }


    @RequestMapping(value="/work-orders/{id}/delivered-inventory", method = RequestMethod.GET)
    public List<Inventory> getDeliveredInventory(@PathVariable Long id) {
        return workOrderService.getDeliveredInventory(id);
    }


    @RequestMapping(value="/work-orders/{id}/kpi", method = RequestMethod.GET)
    public List<WorkOrderKPI> getKPIs(@PathVariable Long id) {
        return workOrderService.getKPIs(id);
    }


    @RequestMapping(value="/work-orders/{id}/kpi-transaction", method = RequestMethod.GET)
    public List<WorkOrderKPITransaction> getKPITransactions(@PathVariable Long id) {
        return workOrderService.getKPITransactions(id);
    }


    @RequestMapping(value="/work-orders/{id}/returned-inventory", method = RequestMethod.GET)
    public List<Inventory> getReturnedInventory(@PathVariable Long id) {
        return workOrderService.getReturnedInventory(id);
    }

    @RequestMapping(value="/work-orders/{id}/modify-lines", method = RequestMethod.POST)
    public WorkOrder modifyWorkOrderLines(@PathVariable Long id,
                                          @RequestBody WorkOrder workOrder) {
        return workOrderService.modifyWorkOrderLines(id, workOrder);
    }


    @RequestMapping(value="/work-orders/{id}/unpick-inventory", method = RequestMethod.POST)
    public Inventory unpickInventory(@PathVariable Long id,
                                           @RequestParam Long inventoryId,
                                           @RequestParam(name = "unpickedQuantity", required = false, defaultValue = "") Long unpickedQuantity,
                                           @RequestParam(name = "overrideConsumedQuantity", required = false, defaultValue = "false") Boolean overrideConsumedQuantity,
                                           @RequestParam(name = "consumedQuantity", required = false, defaultValue = "")  Long consumedQuantity,
                                           @RequestParam(name = "destinationLocationId", required = false, defaultValue = "") Long destinationLocationId,
                                           @RequestParam(name = "destinationLocationName", required = false, defaultValue = "") String destinationLocationName,
                                           @RequestParam(name = "immediateMove", required = false, defaultValue = "true") boolean immediateMove) {
        return workOrderService.unpickInventory(id, inventoryId, unpickedQuantity,  overrideConsumedQuantity, consumedQuantity,
                destinationLocationId, destinationLocationName, immediateMove);
    }


    @RequestMapping(value="/work-orders/validate-new-number", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> validateNewNumber(@RequestParam Long warehouseId,
                                                         @RequestParam String number) {
        return ResponseBodyWrapper.success(workOrderService.validateNewNumber(warehouseId, number));
    }

}
