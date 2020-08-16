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


import com.garyzhangscm.cwms.workorder.model.WorkOrderKPI;
import com.garyzhangscm.cwms.workorder.model.WorkOrderKPITransaction;
import com.garyzhangscm.cwms.workorder.service.WorkOrderKPIService;
import com.garyzhangscm.cwms.workorder.service.WorkOrderKPITransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WorkOrderKPITransactionController {
    @Autowired
    WorkOrderKPITransactionService workOrderKPITransactionService;


    @RequestMapping(value="/work-order-kpi-transactions", method = RequestMethod.GET)
    public List<WorkOrderKPITransaction> findAllWorkOrderKPITransactions(
            @RequestParam Long warehouseId,
                                                              @RequestParam(name="workOrderNumber", required = false, defaultValue = "") String workOrderNumber,
                                                              @RequestParam(name="username", required = false, defaultValue = "") String username,
                                                              @RequestParam(name="workingTeamName", required = false, defaultValue = "") String workingTeamName) {
        return workOrderKPITransactionService.findAll(warehouseId, workOrderNumber, username, workingTeamName);
    }

    @RequestMapping(value="/work-order-kpi-transactions", method = RequestMethod.POST)
    public WorkOrderKPITransaction addWorkOrderKPITransaction(@RequestBody WorkOrderKPITransaction workOrderKPITransaction) {
        return workOrderKPITransactionService.save(workOrderKPITransaction);
    }


    @RequestMapping(value="/work-order-kpi-transactions/{id}", method = RequestMethod.GET)
    public WorkOrderKPITransaction findWorkOrderKPITransaction(@PathVariable Long id) {
        return workOrderKPITransactionService.findById(id);
    }
    @RequestMapping(value="/work-order-kpi-transactions/{id}", method = RequestMethod.DELETE)
    public void removeWorkOrderKPITransaction(@PathVariable Long id) {
        workOrderKPITransactionService.delete(id);
    }

    @RequestMapping(value="/work-order-kpi-transactions/{id}", method = RequestMethod.PUT)
    public WorkOrderKPITransaction changeWorkOrderKPITransaction(@RequestBody WorkOrderKPITransaction workOrderKPITransaction){
        return workOrderKPITransactionService.save(workOrderKPITransaction);
    }




}
