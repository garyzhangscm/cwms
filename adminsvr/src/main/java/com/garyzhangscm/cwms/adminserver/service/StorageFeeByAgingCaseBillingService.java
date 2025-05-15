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
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Storage Fee by Pallet Count, we will use LPN count as an estimation of the pallet count
 */
@Service
public class StorageFeeByAgingCaseBillingService extends  StorageFeeBillingService {
    private static final Logger logger = LoggerFactory.getLogger(StorageFeeByAgingCaseBillingService.class);
    @Autowired
    private BillingRateService billingRateService;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private BillingRequestService billingRequestService;
    @Autowired
    private BillingRateByInventoryAgeService billingRateByInventoryAgeService;



    public BillableCategory getBillableCategory() {
        return BillableCategory.STORAGE_FEE_BY_CASE_COUNT;
    }


    /**
     * To get storage fee by case quantity , we will look at 2 section
     * 1. the quantity shipped during the time windows, then we will charge based on the quantities shipped
     *    times the days in the warehouse within the time windows and the date when the case is shipped
     * 2. the quantity that stays in warehouse until the end of the time window, then we will charge the
     *    actual days that the case stays in the warehouse
     * We will charge by days,
     * @param startTime
     * @param endTime
     * @param companyId
     * @param warehouseId
     * @param clientId
     * @param number
     * @param serialize
     * @param includeDaysSinceInWarehouseForStorageFee
     * @return
     */
    @Override
    public BillingRequest generateBillingRequest(ZonedDateTime startTime, ZonedDateTime endTime,
                                                 Long companyId, Long warehouseId, Long clientId,
                                                 String number, Boolean serialize,
                                                 Boolean includeDaysSinceInWarehouseForStorageFee) {

        // let's get the inventory aging based on the start time and end time
        logger.debug("start to generate billing request for aging case");
        List<InventoryAgingForBilling> inventoryAgingForBillings =
                inventoryServiceRestemplateClient.getInventoryAgingForBilling(
                        warehouseId, clientId, BillableCategory.STORAGE_FEE_BY_CASE_COUNT.name(),
                        startTime, endTime,
                        includeDaysSinceInWarehouseForStorageFee);

        logger.debug("get {} inventoryAgingForBillings records", inventoryAgingForBillings.size());
        inventoryAgingForBillings.forEach(
                inventoryAgingForBilling -> logger.debug("> days: {}, case quantity: {}",
                        inventoryAgingForBilling.getDays(),
                        inventoryAgingForBilling.getQuantity())
        );

        // we will generate billing request from the daily average amount
        BillingRequest billingRequest = new BillingRequest(companyId, warehouseId, clientId,
                Strings.isNotBlank(number) ? number : billingRequestService.getNextNumber(warehouseId),
                getBillableCategory(),
                0.0, BillingCycle.DAILY,
                0.0, 0.0);

        inventoryAgingForBillings.forEach(
                inventoryAgingForBilling -> {
                    BillingRequestLine billingRequestLine = getBillingRequestLine(
                            billingRequest, startTime, endTime,
                            companyId, warehouseId, clientId,
                            inventoryAgingForBilling.getDays(), inventoryAgingForBilling.getQuantity());

                    billingRequest.addBillingRequestLine(billingRequestLine);
                }
        );

        logger.debug("we get billing request: \n{}", billingRequest);
        logger.debug("we will serialize it? {}", serialize);
        // by default, we will save the result into the database
        if (Boolean.FALSE.equals(serialize)) {
            return billingRequest;
        }
        else {

            return billingRequestService.save(billingRequest);
        }
    }

    private BillingRequestLine getBillingRequestLine(BillingRequest billingRequest,
                                                     ZonedDateTime startTime, ZonedDateTime endTime,
                                                     Long companyId, Long warehouseId, Long clientId,
                                                     long days, double caseQuantity) {

        // for each number of days, find the right group
        List<BillingRateByInventoryAge> matchingBillingRateByInventoryAge =
                billingRateByInventoryAgeService.getMatchingBillingRateByInventoryAge(
                        companyId, warehouseId, clientId, days, BillableCategory.STORAGE_FEE_BY_CASE_COUNT);

        logger.debug("Got {} BillingRateByInventoryAge defined that match with days {}",
                matchingBillingRateByInventoryAge.size(), days);
        matchingBillingRateByInventoryAge.forEach(
                billingRateByInventoryAge ->
                        logger.debug("> {} ~ {}",
                                billingRateByInventoryAge.getStartInventoryAge(),
                                billingRateByInventoryAge.getEndInventoryAge())
        );

        // we will consider all the matched group and rate and sum up
        double totalCharge = 0.0;
        double rate = 0.0;
        int matchedRateCount = 0;

        for (BillingRateByInventoryAge billingRateByInventoryAge : matchingBillingRateByInventoryAge) {
            // get the rate
            BillingRate matchedBillingRate = billingRateByInventoryAge.getBillingRates().stream().filter(
                    billingRate -> Boolean.TRUE.equals(billingRate.getEnabled()) &&
                            billingRate.getBillableCategory().equals(BillableCategory.STORAGE_FEE_BY_CASE_COUNT)
            ).findFirst().orElse(null);
            if (Objects.nonNull(matchedBillingRate)) {
                logger.debug("Found a rate {} per {} in the group with time window {} ~ {}",
                        matchedBillingRate.getRate(),
                        matchedBillingRate.getBillingCycle(),
                        billingRateByInventoryAge.getStartInventoryAge(),
                        billingRateByInventoryAge.getEndInventoryAge());

                matchedRateCount++;
                rate = matchedBillingRate.getRate();

                logger.debug("start to get the charge with case quantity {}", caseQuantity);
                totalCharge += getTotalCharge(days, matchedBillingRate, caseQuantity);
            }
            else {
                logger.debug("we can't find any matched rate within the group of time window {} ~ {}",
                        billingRateByInventoryAge.getStartInventoryAge(),
                        billingRateByInventoryAge.getEndInventoryAge());
            }
        }

        return new BillingRequestLine(
                billingRequest,
                startTime,
                endTime,
                "days: " + days + ", case quantity per day: " + caseQuantity,
                days * caseQuantity * 1.0,
                totalCharge,
                matchedRateCount == 1 ? rate : 0.0   // only setup the rate when there's only one matched rate
        );
    }

    public double getTotalCharge(long days, BillingRate billingRate, double caseQuantity) {
        double totalCharge = 0.0;

        switch (billingRate.getBillingCycle()) {
            case DAILY:
                totalCharge = billingRate.getRate() * days * caseQuantity;
                break;
            case WEEKLY:
                totalCharge = billingRate.getRate() * days * caseQuantity/ 7;
                break;
            case BI_WEEKLY:
                totalCharge = billingRate.getRate() * days * caseQuantity/ 14;
                break;
            case MONTHLY:
                totalCharge = billingRate.getRate() * days * caseQuantity/ 30;
                break;
            case BI_MONTHLY:
                totalCharge = billingRate.getRate() * days * caseQuantity/ 60;
                break;
            case QUARTERLY:
                totalCharge = billingRate.getRate() * days * caseQuantity/ 90;
                break;
            case YEARLY:
                totalCharge = billingRate.getRate() * days * caseQuantity/ 365;
                break;
        }
        return totalCharge;
    }
}
