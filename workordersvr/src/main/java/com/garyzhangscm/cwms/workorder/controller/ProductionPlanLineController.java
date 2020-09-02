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


import com.garyzhangscm.cwms.workorder.model.ProductionPlan;
import com.garyzhangscm.cwms.workorder.model.ProductionPlanLine;
import com.garyzhangscm.cwms.workorder.model.WorkOrder;
import com.garyzhangscm.cwms.workorder.service.ProductionPlanLineService;
import com.garyzhangscm.cwms.workorder.service.ProductionPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductionPlanLineController {
    @Autowired
    ProductionPlanLineService productionPlanLineService;


    @RequestMapping(value="/production-plan/lines", method = RequestMethod.GET)
    public List<ProductionPlanLine> findAllProductionPlanLines(@RequestParam Long warehouseId,
                                                           @RequestParam(name="productionPlannumber", required = false, defaultValue = "") String productionPlannumber,
                                                           @RequestParam(name="itemName", required = false, defaultValue = "") String itemName) {
        return productionPlanLineService.findAll(warehouseId, productionPlannumber, itemName);
    }

    @RequestMapping(value="/production-plans/lines", method = RequestMethod.POST)
    public ProductionPlanLine addProductionPlanLine(@RequestBody ProductionPlanLine productionPlanLine) {
        return productionPlanLineService.addProductionPlanLine(productionPlanLine);
    }


    @RequestMapping(value="/production-plan/lines/{id}", method = RequestMethod.GET)
    public ProductionPlanLine findProductionPlanLine(@PathVariable Long id) {
        return productionPlanLineService.findById(id);
    }

    @RequestMapping(value="/production-plan/lines/{id}", method = RequestMethod.PUT)
    public ProductionPlanLine changeProductionPlanLine(@RequestBody ProductionPlanLine productionPlanLine){
        return productionPlanLineService.save(productionPlanLine);
    }



    @RequestMapping(value="/production-plan/lines/{id}/create-work-order", method = RequestMethod.POST)
    public WorkOrder createWorkOrderFromProductionPlan(@PathVariable Long id,
                                            @RequestParam String workOrderNumber,
                                            @RequestParam Long expectedQuantity,
                                            @RequestParam(name="productionLineId", required = false) Long productionLineId) {
        return productionPlanLineService.createWorkOrder(id,
                workOrderNumber, expectedQuantity, productionLineId);
    }




}
