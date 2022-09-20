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
import com.garyzhangscm.cwms.workorder.model.WorkOrderCompleteTransaction;
import com.garyzhangscm.cwms.workorder.service.WorkOrderCompleteTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WorkOrderCompleteTransactionController {
    @Autowired
    WorkOrderCompleteTransactionService workOrderCompleteTransactionService;



    @RequestMapping(value="/work-order-complete-transactions", method = RequestMethod.GET)
    public List<WorkOrderCompleteTransaction> findAllWorkOrderCompleteTransactions(
            @RequestParam Long warehouseId,
            @RequestParam(name="workOrderNumber", required = false, defaultValue = "") String workOrderNumber
                                             ) {
        return workOrderCompleteTransactionService.findAll(warehouseId, workOrderNumber);
    }

    /**
     * Add a work order complete transaction
     * @param workOrderCompleteTransaction work order complete transaction
     * @param locationId production line outbound location id
     * @return
     */
    @BillableEndpoint
    @RequestMapping(value="/work-order-complete-transactions", method = RequestMethod.POST)
    public WorkOrderCompleteTransaction addWorkOrderCompleteTransaction(
            @RequestParam Long warehouseId,
            @RequestBody WorkOrderCompleteTransaction workOrderCompleteTransaction,
            @RequestParam(name = "locationId", required = false, defaultValue = "") Long locationId) {
        return workOrderCompleteTransactionService.startNewTransaction(
                warehouseId, workOrderCompleteTransaction, locationId);
    }


    @RequestMapping(value="/work-order-complete-transactions/{id}", method = RequestMethod.GET)
    public WorkOrderCompleteTransaction findWorkOrderCompleteTransaction(@PathVariable Long id) {
        return workOrderCompleteTransactionService.findById(id);
    }





}
