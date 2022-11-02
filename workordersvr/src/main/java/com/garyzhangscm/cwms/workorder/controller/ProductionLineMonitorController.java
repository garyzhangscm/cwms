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
import com.garyzhangscm.cwms.workorder.model.BillableEndpoint;
import com.garyzhangscm.cwms.workorder.model.ProductionLine;
import com.garyzhangscm.cwms.workorder.model.ProductionLineMonitor;
import com.garyzhangscm.cwms.workorder.service.ProductionLineMonitorService;
import com.garyzhangscm.cwms.workorder.service.ProductionLineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductionLineMonitorController {
    @Autowired
    ProductionLineMonitorService productionLineMonitorService;


    @RequestMapping(value="/production-line-monitors", method = RequestMethod.GET)
    public List<ProductionLineMonitor> findAllProductionLineMonitors(@RequestParam Long warehouseId,
                                                                     @RequestParam(name="name", required = false, defaultValue = "") String name,
                                                                     @RequestParam(name="description", required = false, defaultValue = "") String description,
                                                                     @RequestParam(name="productionLineName", required = false, defaultValue = "") String productionLineName) {
        return productionLineMonitorService.findAll(warehouseId, name, description, productionLineName);
    }

    @BillableEndpoint
    @RequestMapping(value="/production-line-monitors", method = RequestMethod.PUT)
    public ProductionLineMonitor addProductionLineMonitor(@RequestBody ProductionLineMonitor productionLineMonitor) {
        return productionLineMonitorService.addProductionLineMonitor(productionLineMonitor);
    }


    @RequestMapping(value="/production-line-monitors/{id}", method = RequestMethod.GET)
    public ProductionLineMonitor findProductionLineMonitor(@PathVariable Long id) {
        return productionLineMonitorService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/production-line-monitors/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeProductionLineMonitor(@PathVariable Long id) {

        productionLineMonitorService.removeProductionLineMonitor(id);
        return ResponseBodyWrapper.success("production line monitor " + id + " is removed");
    }

    @BillableEndpoint
    @RequestMapping(value="/production-line-monitors/{id}", method = RequestMethod.POST)
    public ProductionLineMonitor changeProductionLineMonitor(@RequestBody ProductionLineMonitor productionLineMonitor){
        return productionLineMonitorService.changeProductionLineMonitor(productionLineMonitor);
    }


    @RequestMapping(value="/production-line-monitors/heart-beat", method = RequestMethod.POST)
    public String processProductionLineMonitorHeartBeat(
            @RequestParam Long warehouseId,
            @RequestParam String productionLineMonitorName) {

        return productionLineMonitorService.processProductionLineMonitorHeartBeat(warehouseId, productionLineMonitorName);
    }


}
