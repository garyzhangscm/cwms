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
import com.garyzhangscm.cwms.workorder.model.BillOfMaterial;
import com.garyzhangscm.cwms.workorder.model.ProductionLineActivity;
import com.garyzhangscm.cwms.workorder.service.BillOfMaterialService;
import com.garyzhangscm.cwms.workorder.service.ProductionLineActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductionLineActivityController {
    @Autowired
    ProductionLineActivityService productionLineActivityService;


    @RequestMapping(value="/production-line-activities", method = RequestMethod.GET)
    public List<ProductionLineActivity> findAllProductionLineActivities(
            @RequestParam Long warehouseId,
            @RequestParam(name="workOrderNumber", required = false, defaultValue = "") String workOrderNumber,
            @RequestParam(name="workOrderId", required = false, defaultValue = "") Long workOrderId,
            @RequestParam(name="productionLineName", required = false, defaultValue = "") String productionLineName,
            @RequestParam(name="productionLineId", required = false, defaultValue = "") Long productionLineId,
            @RequestParam(name="username", required = false, defaultValue = "") String username,
            @RequestParam(name="type", required = false, defaultValue = "") String type,
            @RequestParam(name="transactionTimeStart", required = false, defaultValue = "") String transactionTimeStart,
            @RequestParam(name="transactionTimeEnd", required = false, defaultValue = "") String transactionTimeEnd) {
        return productionLineActivityService.findAll(warehouseId, workOrderNumber, workOrderId,
                productionLineName, productionLineId, username, type, transactionTimeStart,
                transactionTimeEnd);
    }


    @RequestMapping(value="/production-line-activities", method = RequestMethod.POST)
    public ProductionLineActivity addProductionLineActivity(@RequestBody ProductionLineActivity productionLineActivity) {
        return productionLineActivityService.addProductionLineActivity(productionLineActivity);
    }


    @RequestMapping(value="/production-line-activities/{id}", method = RequestMethod.GET)
    public ProductionLineActivity findProductionLineActivity(@PathVariable Long id) {
        return productionLineActivityService.findById(id);
    }


    @RequestMapping(value="/production-line-activities/check_in", method = RequestMethod.POST)
    public ProductionLineActivity productionLineCheckIn(
            @RequestParam Long warehouseId,
            @RequestParam Long workOrderId,
            @RequestParam Long productionLineId,
            @RequestParam String username,
            @RequestParam(name="workingTeamMemberCount", required = false, defaultValue = "1") Integer workingTeamMemberCount
    ) {
        return productionLineActivityService.productionLineCheckIn(
                warehouseId, workOrderId, productionLineId, username, workingTeamMemberCount
        );
    }

    @RequestMapping(value="/production-line-activities/check_out", method = RequestMethod.POST)
    public ProductionLineActivity productionLineCheckOut(
            @RequestParam Long warehouseId,
            @RequestParam Long workOrderId,
            @RequestParam Long productionLineId,
            @RequestParam String username,
            @RequestParam(name="workingTeamMemberCount", required = false, defaultValue = "1") Integer workingTeamMemberCount
    ) {
        return productionLineActivityService.productionLineCheckOut(
                warehouseId, workOrderId, productionLineId, username, workingTeamMemberCount
        );
    }



}
