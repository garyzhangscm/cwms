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

import com.garyzhangscm.cwms.adminserver.clients.OutbuondServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.model.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;

@Service
public class OrderBillingService implements BillingService {

    private static final Logger logger = LoggerFactory.getLogger(OrderBillingService.class);


    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;

    @Autowired
    private BillingRequestService billingRequestService;


    public BillableCategory getBillableCategory() {

        return BillableCategory.ORDER_PROCESS_FEE;
    }


    @Override
    public BillingRequest generateBillingRequest(ZonedDateTime startTime, ZonedDateTime endTime,
                                                 Long companyId, Long warehouseId, Long clientId,
                                                 String number, Boolean serialize,
                                                 Boolean includeDaysSinceInWarehouseForStorageFee) {

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

        loadOrderLevelActivity(billingRequest,
                companyId, warehouseId, clientId, startTime, endTime);

        return billingRequest;


    }

    private void loadOrderLevelActivity(BillingRequest billingRequest,
                                          Long companyId, Long warehouseId, Long clientId,
                                          ZonedDateTime startTime, ZonedDateTime endTime) {

        // get all the item based billable activity
        List<BillableActivity> billableActivities = outbuondServiceRestemplateClient.getBillableActivities(
                  warehouseId, clientId, startTime, endTime, true
        );

        logger.debug("Get billable activity by order / order line \n{}",
                billableActivities);


        billableActivities.stream()
            .forEach(
                billableActivity -> {

                        BillingRequestLine billingRequestLine =
                                new BillingRequestLine(
                                        billingRequest,
                                        billableActivity.getActivityTime(),
                                        billableActivity.getActivityTime(),
                                        billableActivity.getDocumentNumber(),
                                        billableActivity.getItemName(),
                                        billableActivity.getAmount(),
                                        billableActivity.getTotalCharge(),
                                        billableActivity.getRate()
                                ) ;

                        logger.debug("create billing line from this billable activity \n{}",
                                billingRequestLine);
                        billingRequest.addBillingRequestLine(billingRequestLine);

                }
        );

    }


}
