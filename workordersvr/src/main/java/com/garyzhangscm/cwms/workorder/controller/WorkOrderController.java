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


import com.garyzhangscm.cwms.workorder.model.WorkOrder;

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
                                             @RequestParam(name="itemName", required = false, defaultValue = "") String itemName) {
        return workOrderService.findAll(warehouseId, number, itemName);
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

    @RequestMapping(value="/work-orders/lines/{id}/pick-cancelled", method = RequestMethod.POST)
    public void registerPickCancelled(@PathVariable Long id,
                                           @RequestParam Long cancelledQuantity) {
        workOrderLineService.registerPickCancelled(id, cancelledQuantity);
    }

    @RequestMapping(value="/work-orders/{id}/change-production-line", method = RequestMethod.POST)
    public WorkOrder changeProductionLine(@PathVariable Long id,
                                  @RequestParam Long productionLineId) {
        return workOrderService.changeProductionLine(id, productionLineId);
    }

}
