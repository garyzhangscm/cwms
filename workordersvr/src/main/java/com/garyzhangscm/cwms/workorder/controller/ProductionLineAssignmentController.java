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
import com.garyzhangscm.cwms.workorder.model.ProductionLineAssignment;
import com.garyzhangscm.cwms.workorder.model.ProductionPlan;
import com.garyzhangscm.cwms.workorder.model.WorkOrder;
import com.garyzhangscm.cwms.workorder.service.ProductionLineAssignmentService;
import com.garyzhangscm.cwms.workorder.service.ProductionPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductionLineAssignmentController {
    @Autowired
    ProductionLineAssignmentService productionLineAssignmentService;


    @RequestMapping(value="/production-line-assignments", method = RequestMethod.GET)
    public List<ProductionLineAssignment> findAllProductionLineAssignment(
                @RequestParam(name="workOrderId", required = false, defaultValue = "") Long workOrderId,
                @RequestParam(name="productionLineId", required = false, defaultValue = "") Long productionLineId,
                @RequestParam(name="productionLineIds", required = false, defaultValue = "") String productionLineIds) {
        return productionLineAssignmentService.findAll(productionLineId, productionLineIds, workOrderId);
    }

    @RequestMapping(value="/production-line-assignments", method = RequestMethod.POST)
    public List<ProductionLineAssignment> assignWorkOrderToProductionLines(
            @RequestParam Long workOrderId,
            @RequestBody List<ProductionLineAssignment> productionLineAssignments) {

        return productionLineAssignmentService.assignWorkOrderToProductionLines(workOrderId,
                productionLineAssignments);
    }


    @RequestMapping(value="/production-line-assignments/assigned-work-orders", method = RequestMethod.GET)
    public List<WorkOrder> getAssignedWorkOrderByProductionLine(Long productionLineId) {

        return productionLineAssignmentService.getAssignedWorkOrderByProductionLine(productionLineId);
    }


}
