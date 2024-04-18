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

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ShippingInventoryBillingService implements BillingService {

    private static final Logger logger = LoggerFactory.getLogger(ShippingInventoryBillingService.class);

    @Autowired
    private BillableActivityService billableActivityService;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    @Autowired
    private BillingRequestService billingRequestService;


    public BillableCategory getBillableCategory() {

        return BillableCategory.SHIPPING_CHARGE_BY_QUANTITY;
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

        loadItemLevelActivity(billingRequest,
                companyId, warehouseId, clientId, startTime, endTime);

        return billingRequest;


    }


    private void loadItemLevelActivity(BillingRequest billingRequest,
                                       Long companyId, Long warehouseId, Long clientId,
                                       ZonedDateTime startTime, ZonedDateTime endTime) {

        // get all the item based billable activity
        List<BillableActivity> billableActivities = billableActivityService.findAll(
                companyId, warehouseId, clientId, startTime, endTime,
                getBillableCategory().name()
        );

        logger.debug("Get billable activity by items \n{}",
                billableActivities);

        // we will group by item and document and then create
        // the lines for each item and document combination
        // key: item-document number
        // value: BillingRequestLine
        Map<String, BillingRequestLine> billingRequestLineMap = new HashMap<>();
        billableActivities.stream().filter(
                billableActivity -> Strings.isNotBlank(billableActivity.getItemName())
        ).forEach(
                billableActivity -> {
                    Item item = inventoryServiceRestemplateClient.getItemByName(
                            warehouseId,  clientId, billableActivity.getItemName()
                    );
                    logger.debug("Get item by name : {} / {}, item\n{}",
                            warehouseId,
                            billableActivity.getItemName(),
                            Objects.nonNull(item) ? item : "N/A");

                    if (Objects.nonNull(item) && Objects.nonNull(item.getShippingRateByUnit())
                            && item.getShippingRateByUnit() > 0) {
                        String key = billableActivity.getItemName() + "-" + billableActivity.getDocumentNumber();
                        logger.debug("Start to get from map by key: {}", key);

                        BillingRequestLine billingRequestLine = billingRequestLineMap.getOrDefault(key,
                                new BillingRequestLine(
                                        billingRequest,
                                        startTime,
                                        endTime,
                                        billableActivity.getDocumentNumber(),
                                        billableActivity.getItemName(),
                                        0.0,
                                        0.0,
                                        item.getShippingRateByUnit()
                                ));

                        billingRequestLine.addAmount(billableActivity.getAmount());
                        logger.debug("create billing request line from this billable activity by item \n{}",
                                billingRequestLine);
                        billingRequestLineMap.put(key, billingRequestLine);
                    }

                }
        );
        billingRequestLineMap.values().forEach(
                billingRequestLine ->
                        billingRequest.addBillingRequestLine(billingRequestLine)
        );

    }

}
