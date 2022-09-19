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



import com.garyzhangscm.cwms.workorder.model.BillableEndpoint;
import com.garyzhangscm.cwms.workorder.model.ProductionLine;

import com.garyzhangscm.cwms.workorder.model.ProductionLineStatus;
import com.garyzhangscm.cwms.workorder.service.ProductionLineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class ProductionLineController {
    @Autowired
    ProductionLineService productionLineService;


    @RequestMapping(value="/production-lines", method = RequestMethod.GET)
    public List<ProductionLine> findAllProductionLines(@RequestParam Long warehouseId,
                                                       @RequestParam(name="name", required = false, defaultValue = "") String name,
                                                       @RequestParam(name="ids", required = false, defaultValue = "") String productionLineIds,
                                                       @RequestParam(name="genericMatch", required = false, defaultValue = "false") boolean genericMatch) {
        return productionLineService.findAll(warehouseId, name, productionLineIds, genericMatch);
    }

    @RequestMapping(value="/production-lines/available", method = RequestMethod.GET)
    public List<ProductionLine> findAllAvailableProductionLines(
            @RequestParam Long warehouseId,
            @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId) {
        return productionLineService.findAllAvailableProductionLines(warehouseId, itemId);
    }

    @RequestMapping(value="/production-lines/available-for-mps", method = RequestMethod.GET)
    public List<ProductionLine> findAllAvailableProductionLinesForMPS(
            @RequestParam Long warehouseId,
            Long itemId) {
        return productionLineService.findAllAvailableProductionLinesForMPS(warehouseId, itemId);
    }

    @RequestMapping(value="/production-line/assigned", method = RequestMethod.GET)
    public List<ProductionLine> findAllAssignedProductionLines(
            @RequestParam Long warehouseId ,
            @RequestParam(name="loadDetails", required = false, defaultValue = "true") boolean loadDetails) {
        return productionLineService.findAllAssignedProductionLines(warehouseId, loadDetails);
    }


    @BillableEndpoint
    @RequestMapping(value="/production-lines", method = RequestMethod.POST)
    public ProductionLine addProductionLine(@RequestBody ProductionLine productionLine) {
        return productionLineService.addProductionLine(productionLine);
    }


    @RequestMapping(value="/production-lines/{id}", method = RequestMethod.GET)
    public ProductionLine findProductionLine(@PathVariable Long id) {
        return productionLineService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/production-lines/{id}", method = RequestMethod.DELETE)
    public ProductionLine removeProductionLine(@PathVariable Long id) {

        return productionLineService.removeProductionLine(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/production-lines/{id}", method = RequestMethod.PUT)
    public ProductionLine changeProductionLine(@RequestBody ProductionLine productionLine){
        return productionLineService.changeProductionLine(productionLine);
    }

    @BillableEndpoint
    @RequestMapping(value="/production-lines", method = RequestMethod.DELETE)
    public void removeProductionLines(@RequestParam(name = "productionLineIds", required = false, defaultValue = "") String productionLineIds) {
        productionLineService.delete(productionLineIds);
    }

    @BillableEndpoint
    @RequestMapping(value="/production-lines/{id}/disable", method = RequestMethod.POST)
    public ProductionLine disableProductionLine(@PathVariable Long id,
                                      @RequestParam boolean disabled) {
        return productionLineService.disableProductionLine(id, disabled);
    }


    @RequestMapping(value="/production-lines/status", method = RequestMethod.GET)
    public List<ProductionLineStatus> getProductionLineStatus(@RequestParam Long warehouseId,
                                                              @RequestParam(name="name", required = false, defaultValue = "") String name,
                                                              @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                              @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  LocalDateTime endTime) {
        return productionLineService.getProductionLineStatus(warehouseId, name, startTime, endTime);
    }

}
