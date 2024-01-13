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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.workorder.ResponseBodyWrapper;
import com.garyzhangscm.cwms.workorder.model.*;
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
                Long warehouseId,
                @RequestParam(name="workOrderId", required = false, defaultValue = "") Long workOrderId,
                @RequestParam(name="productionLineId", required = false, defaultValue = "") Long productionLineId,
                @RequestParam(name="productionLineIds", required = false, defaultValue = "") String productionLineIds,
                @RequestParam(name="productionLineNames", required = false, defaultValue = "") String productionLineNames,
                @RequestParam(name="includeDeassigned", required = false, defaultValue = "") Boolean includeDeassigned) {
        return productionLineAssignmentService.findAll(warehouseId, productionLineId, productionLineIds, workOrderId,
                productionLineNames, includeDeassigned);
    }


    @RequestMapping(value="/production-line-assignment/{id}", method = RequestMethod.GET)
    public ProductionLineAssignment getProductionLineAssignment(@PathVariable Long id,
                                                                @RequestParam(name = "includeDetails", required = false, defaultValue = "true") Boolean includeDetails) {
        return productionLineAssignmentService.findById(id, includeDetails);
    }

    @BillableEndpoint
    @RequestMapping(value="/production-line-assignments", method = RequestMethod.POST)
    public List<ProductionLineAssignment> assignWorkOrderToProductionLines(
            @RequestParam Long workOrderId,
            @RequestBody List<ProductionLineAssignment> productionLineAssignments) {

        return productionLineAssignmentService.assignWorkOrderToProductionLines(workOrderId,
                productionLineAssignments);
    }


    @RequestMapping(value="/production-line-assignments/assigned-work-orders", method = RequestMethod.GET)
    public List<WorkOrder> getAssignedWorkOrderByProductionLine(
            Long warehouseId,Long productionLineId) {

        return productionLineAssignmentService.getAssignedWorkOrderByProductionLine(warehouseId, productionLineId);
    }


    @BillableEndpoint
    @RequestMapping(value="/production-line-assignments/deassign", method = RequestMethod.POST)
    public ProductionLineAssignment deassignWorkOrderFromProductionLines(
            @RequestParam Long workOrderId,
            @RequestParam Long productionLineId,
            @RequestBody List<Inventory> returnableMaterial) {

        return productionLineAssignmentService.deassignWorkOrderFromProductionLines(workOrderId,
                productionLineId, returnableMaterial);
    }


    @BillableEndpoint
    @RequestMapping(value="/production-line-assignments/{id}/report", method = RequestMethod.POST)
    public ReportHistory generateProductionLineAssignmentReport(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName
    ) throws JsonProcessingException {

        return productionLineAssignmentService.generateProductionLineAssignmentReport(id, locale,
                printerName);
    }


    @BillableEndpoint
    @RequestMapping(value="/production-line-assignments/{id}/label", method = RequestMethod.POST)
    public ReportHistory generateProductionLineAssignmentLabel(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName
    ) throws JsonProcessingException {

        return productionLineAssignmentService.generateProductionLineAssignmentLabel(id, locale,
                printerName);
    }





}
