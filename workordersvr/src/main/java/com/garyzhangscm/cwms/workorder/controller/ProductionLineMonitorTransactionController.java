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
import com.garyzhangscm.cwms.workorder.model.ProductionLineMonitor;
import com.garyzhangscm.cwms.workorder.model.ProductionLineMonitorTransaction;
import com.garyzhangscm.cwms.workorder.service.ProductionLineMonitorService;
import com.garyzhangscm.cwms.workorder.service.ProductionLineMonitorTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class ProductionLineMonitorTransactionController {
    @Autowired
    ProductionLineMonitorTransactionService productionLineMonitorTransactionService;


    @RequestMapping(value="/production-line-monitor-transactions", method = RequestMethod.GET)
    public List<ProductionLineMonitorTransaction> findAllProductionLineMonitorTransactions(
            @RequestParam Long warehouseId,
            @RequestParam(name="productionLineMonitorName", required = false, defaultValue = "") String productionLineMonitorName,
            @RequestParam(name="productionLineName", required = false, defaultValue = "") String productionLineName,
            @RequestParam(name="productionLineId", required = false, defaultValue = "") Long productionLineId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  LocalDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return productionLineMonitorTransactionService.findAll(
                warehouseId, productionLineMonitorName, productionLineName, productionLineId,
                startTime, endTime, date);
    }

    @BillableEndpoint
    @RequestMapping(value="/production-line-monitor-transactions", method = RequestMethod.PUT)
    public ProductionLineMonitorTransaction addProductionLineMonitorTransaction(
            @RequestParam Long warehouseId,
            @RequestParam String productionLineMonitorName,
            @RequestParam Double cycleTime) {
        return productionLineMonitorTransactionService.addProductionLineMonitorTransaction(
                warehouseId, productionLineMonitorName, cycleTime
        );
    }


    @RequestMapping(value="/production-line-monitor-transactions/{id}", method = RequestMethod.GET)
    public ProductionLineMonitorTransaction findProductionLineMonitorTransaction(@PathVariable Long id) {
        return productionLineMonitorTransactionService.findById(id);
    }


}
