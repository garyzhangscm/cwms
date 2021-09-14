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


import com.garyzhangscm.cwms.outbound.model.AllocationResult;
import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.outbound.model.WorkOrder;
import com.garyzhangscm.cwms.outbound.model.WorkOrderLine;
import com.garyzhangscm.cwms.outbound.service.AllocationConfigurationService;
import com.garyzhangscm.cwms.outbound.service.AllocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
public class AllocationController {

    private static final Logger logger = LoggerFactory.getLogger(AllocationController.class);
    @Autowired
    private AllocationConfigurationService allocationConfigurationService;
    @Autowired
    private AllocationService allocationService;

    @BillableEndpoint
    @RequestMapping(value="/allocation/work-order", method = RequestMethod.POST)
    public AllocationResult allocateWorkOrder(
            @RequestBody WorkOrder workOrder,
            @RequestParam(name = "productionLineId", defaultValue = "", required = false) Long productionId,
            @RequestParam(name = "quantity", defaultValue = "", required = false) Long allocatingWorkOrderQuantity) {
        // return allocationConfigurationService.allocateWorkOrder(workOrder);

        logger.debug("Start to allocate work order {}, by production line id {}, with quantity {}",
                workOrder.getNumber(), productionId, allocatingWorkOrderQuantity);
        return allocationService.allocate(workOrder, productionId, allocatingWorkOrderQuantity);
    }

    @BillableEndpoint
    @RequestMapping(value="/allocation/work-order-line", method = RequestMethod.POST)
    public AllocationResult allocateWorkOrderLine(
            @RequestBody WorkOrderLine workOrderLine,
            @RequestParam(name = "workOrderId", defaultValue = "", required = false) Long workOrderId,
            @RequestParam(name = "productionLineId", defaultValue = "", required = false) Long productionId,
            @RequestParam(name = "quantity", defaultValue = "", required = false) Long allocatingWorkOrderLineQuantity) {
        // return allocationConfigurationService.allocateWorkOrder(workOrder);

        logger.debug("Start to allocate work order line {} / {}, by production line id {}, with quantity {}",
                workOrderLine.getNumber(), workOrderLine.getItem().getName(), productionId, allocatingWorkOrderLineQuantity);
        return allocationService.allocate(workOrderId, workOrderLine, productionId,  allocatingWorkOrderLineQuantity);
    }



}
