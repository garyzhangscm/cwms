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

package com.garyzhangscm.cwms.outbound.controller;


import com.garyzhangscm.cwms.outbound.model.AllocationConfiguration;
import com.garyzhangscm.cwms.outbound.model.AllocationConfigurationPickableUnitOfMeasure;
import com.garyzhangscm.cwms.outbound.model.AllocationTransactionHistory;
import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.outbound.service.AllocationConfigurationService;
import com.garyzhangscm.cwms.outbound.service.AllocationTransactionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class AllocationTransactionHistoryController {

    @Autowired
    private AllocationTransactionHistoryService allocationTransactionHistoryService;

    @RequestMapping(value="/allocation-transaction-histories", method = RequestMethod.GET)
    public List<AllocationTransactionHistory> getAllocationTransactionHistories(
                                @RequestParam Long warehouseId,
                                @RequestParam(name = "number", required = false, defaultValue =  "") String number,
                                @RequestParam(name = "transactionGroupId", required = false, defaultValue =  "") String transactionGroupId,
                                @RequestParam(name = "orderNumber", required = false, defaultValue =  "") String orderNumber,
                                @RequestParam(name = "workOrderNumber", required = false, defaultValue =  "") String workOrderNumber,
                                @RequestParam(name = "itemName", required = false, defaultValue =  "") String itemName,
                                @RequestParam(name = "locationName", required = false, defaultValue =  "") String locationName,
                                @RequestParam(name = "loadDetails", required = false, defaultValue =  "") Boolean loadDetails) {
        return allocationTransactionHistoryService.findAll(warehouseId, number, transactionGroupId,
                orderNumber, workOrderNumber, itemName,
                locationName, loadDetails);
    }



}
