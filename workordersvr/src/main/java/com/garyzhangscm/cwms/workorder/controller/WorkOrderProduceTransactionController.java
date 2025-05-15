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
import com.garyzhangscm.cwms.workorder.model.WorkOrderProduceTransaction;
import com.garyzhangscm.cwms.workorder.service.WorkOrderProduceTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
public class WorkOrderProduceTransactionController {
    @Autowired
    WorkOrderProduceTransactionService workOrderProduceTransactionService;



    @RequestMapping(value="/work-order-produce-transactions", method = RequestMethod.GET)
    public List<WorkOrderProduceTransaction> findAllWorkOrderProduceTransactions(
            @RequestParam Long warehouseId,
            @RequestParam(name="workOrderNumber", required = false, defaultValue = "") String workOrderNumber,
            @RequestParam(name="productionLineId", required = false, defaultValue = "") Long productionLineId,
            @RequestParam(name="genericMatch", required = false, defaultValue = "false") boolean genericQuery,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
                                             ) {
        return workOrderProduceTransactionService.findAll(warehouseId, workOrderNumber,
                productionLineId,  genericQuery, startTime, endTime, date);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-order-produce-transactions", method = RequestMethod.POST)
    public WorkOrderProduceTransaction addWorkOrderProduceTransaction(
            @RequestBody WorkOrderProduceTransaction workOrderProduceTransaction,
            @RequestParam(name = "rfCode", required = false, defaultValue = "") String rfCode) {
        return workOrderProduceTransactionService.startNewTransaction(workOrderProduceTransaction, rfCode);
    }


    @RequestMapping(value="/work-order-produce-transactions/{id}", method = RequestMethod.GET)
    public WorkOrderProduceTransaction findWorkOrderProduceTransaction(@PathVariable Long id) {
        return workOrderProduceTransactionService.findById(id);
    }





}
