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


import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.service.ProductionPlanService;
import com.garyzhangscm.cwms.workorder.service.WorkOrderLineService;
import com.garyzhangscm.cwms.workorder.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductionPlanController {
    @Autowired
    ProductionPlanService productionPlanService;


    @RequestMapping(value="/production-plans", method = RequestMethod.GET)
    public List<ProductionPlan> findAllProductionPlans(@RequestParam Long warehouseId,
                                             @RequestParam(name="number", required = false, defaultValue = "") String number,
                                             @RequestParam(name="itemName", required = false, defaultValue = "") String itemName) {
        return productionPlanService.findAll(warehouseId, number, itemName);
    }

    @RequestMapping(value="/production-plans", method = RequestMethod.POST)
    public ProductionPlan addProductionPlan(@RequestBody ProductionPlan productionPlan) {
        return productionPlanService.addProductionPlan(productionPlan);
    }


    @RequestMapping(value="/production-plans/{id}", method = RequestMethod.GET)
    public ProductionPlan findProductionPlan(@PathVariable Long id) {
        return productionPlanService.findById(id);
    }

    @RequestMapping(value="/production-plans/{id}", method = RequestMethod.PUT)
    public ProductionPlan changeProductionPlan(@RequestBody ProductionPlan productionPlan){
        return productionPlanService.save(productionPlan);
    }


}
