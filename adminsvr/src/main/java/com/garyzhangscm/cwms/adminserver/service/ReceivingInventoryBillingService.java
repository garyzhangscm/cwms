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

package com.garyzhangscm.cwms.adminserver.service;

import com.garyzhangscm.cwms.adminserver.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.model.*;
import com.garyzhangscm.cwms.adminserver.model.wms.Item;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class ReceivingInventoryBillingService implements BillingService {

    private static final Logger logger = LoggerFactory.getLogger(ReceivingInventoryBillingService.class);

    @Autowired
    private BillableActivityService billableActivityService;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    @Autowired
    private BillingRequestService billingRequestService;


    public BillableCategory getBillableCategory() {

        return BillableCategory.RECEIVING_CHARGE_BY_QUANTITY;
    }


    @Override
    public BillingRequest generateBillingRequest(ZonedDateTime startTime, ZonedDateTime endTime,
                                                 Long companyId, Long warehouseId, Long clientId,
                                                 String number, Boolean serialize) {

        // we have 2 type of receiving billable activity.
        // 1. item level: every time we receive the item, the system generates record in
        //    BillableActivity table
        // 2. receipt / receipt line level: billable activity will be record at the
        //    receipt and receipt line


        BillingRequest billingRequest = new BillingRequest(companyId, warehouseId, clientId,
                Strings.isNotBlank(number) ? number : billingRequestService.getNextNumber(warehouseId),
                getBillableCategory(),
                0.0, BillingCycle.DAILY,
                0.0, 0.0);

        loadItemLevelActivity(billingRequest,
                companyId, warehouseId, startTime, endTime);
        loadReceiptLevelActivity(billingRequest,
                companyId, warehouseId, startTime, endTime);

        return billingRequest;


    }

    private void loadReceiptLevelActivity(BillingRequest billingRequest,
                                          Long companyId, Long warehouseId,
                                          ZonedDateTime startTime, ZonedDateTime endTime) {
    }

    private void loadItemLevelActivity(BillingRequest billingRequest,
                                       Long companyId, Long warehouseId,
                                       ZonedDateTime startTime, ZonedDateTime endTime) {

        // get all the item based billable activity
        List<BillableActivity> billableActivities = billableActivityService.findAll(
                companyId, warehouseId, startTime, endTime,
                getBillableCategory().name()
        );

        billableActivities.stream().filter(
                billableActivity -> Strings.isNotBlank(billableActivity.getItemNumber())
        ).forEach(
                billableActivity -> {
                    Item item = inventoryServiceRestemplateClient.getItemByName(
                            warehouseId,  billableActivity.getItemNumber()
                    );
                    if (Objects.nonNull(item) && Objects.nonNull(item.getReceivingRateByUnit())
                            && item.getReceivingRateByUnit() > 0) {

                        BillingRequestLine billingRequestLine = new BillingRequestLine(
                                billingRequest, billableActivity.getCreatedTime(),
                                billableActivity.getCreatedTime(),
                                billableActivity.getCreatedTime().toLocalDate(),
                                billableActivity.getAmount(),
                                billableActivity.getAmount() * item.getReceivingRateByUnit(),
                                item.getReceivingRateByUnit()
                        );
                        billingRequest.addBillingRequestLine(billingRequestLine);
                    }

                }
        );
    }

}
