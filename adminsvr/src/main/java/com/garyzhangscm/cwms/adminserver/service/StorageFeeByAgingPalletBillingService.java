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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Storage Fee by Pallet Count, we will use LPN count as an estimation of the pallet count
 */
@Service
public class StorageFeeByAgingPalletBillingService extends  StorageFeeBillingService {
    private static final Logger logger = LoggerFactory.getLogger(StorageFeeByAgingPalletBillingService.class);
    @Autowired
    private BillingRateService billingRateService;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private BillingRequestService billingRequestService;



    public BillableCategory getBillableCategory() {
        return BillableCategory.STORAGE_FEE_BY_AGING_PALLET;
    }


    @Override
    public BillingRequest generateBillingRequest(ZonedDateTime startTime, ZonedDateTime endTime,
                                                 Long companyId, Long warehouseId, Long clientId,
                                                 String number, Boolean serialize) {

        logger.debug("start to generate billing request for aging pallet");
        // progressive rate
        List<ProgressiveBillingRate> progressiveBillingRates = billingRateService.getProgressiveRateByCategory(
                companyId, warehouseId, clientId, getBillableCategory(), false);

        logger.debug("Get {} progressive billing rate \n {}",
                progressiveBillingRates.size(),
                progressiveBillingRates);

        if (Objects.isNull(progressiveBillingRates) || progressiveBillingRates.isEmpty()) {

            logger.debug("progressive billing rate of {} is not defined for company id {}, warehouse id {}, client id {}",
                    getBillableCategory(), companyId, warehouseId, clientId);
            return null;
        }

        // right now we will only assume that the progressive rate is either by day or by week
        return generateBillingRequestByBillingCycle(
                progressiveBillingRates, startTime, endTime,
                companyId, warehouseId, clientId,
                number, serialize
        );

    }

    private BillingRequest generateBillingRequestByBillingCycle(
            List<ProgressiveBillingRate> progressiveBillingRates,
            ZonedDateTime startTime, ZonedDateTime endTime,
            Long companyId, Long warehouseId, Long clientId,
            String number, Boolean serialize
    ) {

        if (startTime.isAfter(endTime)) {
            // the user passed in the wrong date range
            return null;
        }

        // let's get the inventory aging snapshot first
        // we will get a list of inventory aging snapshot, one record for each day
        // between the start time and end time
        List<ClientInventoryAgingSnapshot> clientInventoryAgingSnapshots =
                inventoryServiceRestemplateClient.getClientInventoryAgingSnapshot(warehouseId, clientId, startTime, endTime);

        logger.debug("we get {} clientInventoryAgingSnapshots between {} and {}",
                clientInventoryAgingSnapshots.size(),
                startTime, endTime);

        if (clientInventoryAgingSnapshots.isEmpty()) {
            // ok, nothing needs to be billed for the client but we will still
            // generate a billing request with 0 charge
            return new BillingRequest(companyId, warehouseId, clientId,
                    Strings.isNotBlank(number) ? number : billingRequestService.getNextNumber(warehouseId),
                    getBillableCategory(),
                    0.0, BillingCycle.DAILY,
                    0.0, 0.0);
        }

        // we will generate billing request from the daily average amount
        BillingRequest billingRequest = new BillingRequest(companyId, warehouseId, clientId,
                Strings.isNotBlank(number) ? number : billingRequestService.getNextNumber(warehouseId),
                getBillableCategory(),
                0.0, BillingCycle.DAILY,
                0.0, 0.0);
        clientInventoryAgingSnapshots.forEach(
                clientInventoryAgingSnapshot -> {
                    logger.debug("start to create billing request line with date {}",
                            clientInventoryAgingSnapshot.getCreatedTime());

                    clientInventoryAgingSnapshot.getInventoryAgingByLPNS().forEach(
                            inventoryAgingByLPN -> {
                                logger.debug("add inventory aging by LPN \n{}", inventoryAgingByLPN);
                                BillingRequestLine billingRequestLine = getBillingRequestLine(
                                        inventoryAgingByLPN, progressiveBillingRates, billingRequest,
                                        clientInventoryAgingSnapshot.getCreatedTime());
                                billingRequest.addBillingRequestLine(billingRequestLine);
                            }
                    );
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

    private BillingRequestLine getBillingRequestLine(InventoryAgingByLPN inventoryAgingByLPN,
                                                     List<ProgressiveBillingRate> progressiveBillingRates,
                                                     BillingRequest billingRequest,
                                                     ZonedDateTime date) {

        // check if we have daily rate defined
        ProgressiveBillingRate rateBucket = progressiveBillingRates.stream().filter(
                progressiveBillingRate -> progressiveBillingRate.getEnabled()
        ).filter(
                progressiveBillingRate -> {
                    if (progressiveBillingRate.getBillingCycle().equals(BillingCycle.DAILY)) {
                        return inventoryAgingByLPN.getAgeInDays() >= progressiveBillingRate.getCycleStart() &&
                                inventoryAgingByLPN.getAgeInDays() <= progressiveBillingRate.getCycleEnd();
                    }
                    else if (progressiveBillingRate.getBillingCycle().equals(BillingCycle.WEEKLY)) {
                        return inventoryAgingByLPN.getAgeInWeeks() >= progressiveBillingRate.getCycleStart() &&
                                inventoryAgingByLPN.getAgeInWeeks() <= progressiveBillingRate.getCycleEnd();
                    }
                    return false;
                }
        ).findFirst().orElse(null);

        // see if we will calculate by daily or weekly
        List<ProgressiveBillingRate> matchedRates = new ArrayList<>();
        if (rateBucket.getBillingCycle().equals(BillingCycle.DAILY)) {
            matchedRates = progressiveBillingRates.stream().filter(
                    progressiveBillingRate -> progressiveBillingRate.getEnabled()
            ).filter(
                    progressiveBillingRate -> progressiveBillingRate.getBillingCycle().equals(BillingCycle.DAILY)
            ).filter(
                    progressiveBillingRate -> progressiveBillingRate.getCycleStart() <= rateBucket.getCycleStart() &&
                            progressiveBillingRate.getCycleEnd() <= rateBucket.getCycleEnd()
            ).collect(Collectors.toList());
        }
        else {
            matchedRates = progressiveBillingRates.stream().filter(
                    progressiveBillingRate -> progressiveBillingRate.getEnabled()
            ).filter(
                    progressiveBillingRate -> progressiveBillingRate.getBillingCycle().equals(BillingCycle.WEEKLY)
            ).filter(
                    progressiveBillingRate -> progressiveBillingRate.getCycleStart() <= rateBucket.getCycleStart() &&
                            progressiveBillingRate.getCycleEnd() <= rateBucket.getCycleEnd()
            ).collect(Collectors.toList());
        }
        double totalAmount = 0;
        double totalCharge = 0;
        for (ProgressiveBillingRate rate : matchedRates) {
            // for each rate, if current inventory aging is not in the bucket, then get the fully amount,
            // otherwise, based on the aging and rate
            if (rate.getBillingCycle().equals(BillingCycle.DAILY)) {
                if(inventoryAgingByLPN.getAgeInDays() >= rate.getCycleStart() &&
                        inventoryAgingByLPN.getAgeInDays() <= rate.getCycleEnd()) {
                    // within current bucket
                    double bucketAmount = inventoryAgingByLPN.getAgeInDays() - rate.getCycleStart() + 1;
                    double bucketCharge = bucketAmount * rate.getRate();
                    totalAmount += bucketAmount;
                    totalCharge += bucketCharge;
                }
                else {
                    // bucket should include both the start day/week and end day/week
                    double bucketAmount = rate.getCycleEnd() - rate.getCycleStart() + 1;
                    double bucketCharge = bucketAmount * rate.getRate();
                    totalAmount += bucketAmount;
                    totalCharge += bucketCharge;
                }
            }
            else if (rate.getBillingCycle().equals(BillingCycle.WEEKLY)) {
                if(inventoryAgingByLPN.getAgeInWeeks() >= rate.getCycleStart() &&
                        inventoryAgingByLPN.getAgeInWeeks() <= rate.getCycleEnd()) {
                    // within current bucket
                    double bucketAmount = inventoryAgingByLPN.getAgeInWeeks() - rate.getCycleStart() + 1;
                    double bucketCharge = bucketAmount * rate.getRate();
                    totalAmount += bucketAmount;
                    totalCharge += bucketCharge;
                }
                else {
                    // bucket should include both the start day/week and end day/week
                    double bucketAmount = rate.getCycleEnd() - rate.getCycleStart() + 1;
                    double bucketCharge = bucketAmount * rate.getRate();
                    totalAmount += bucketAmount;
                    totalCharge += bucketCharge;
                }
            }
        }

        return new BillingRequestLine(
                billingRequest,
                date.toLocalDate().atStartOfDay().atZone(ZoneOffset.UTC),
                date.toLocalDate().plusDays(1).atStartOfDay().minusNanos(1).atZone(ZoneOffset.UTC),
                date.toLocalDate(),
                totalAmount,
                totalCharge,
                0.0
        );
    }

    private double getRate(InventoryAgingByLPN inventoryAgingByLPN,
                           List<ProgressiveBillingRate> progressiveBillingRates) {
        // check if we have daily rate defined
        ProgressiveBillingRate rate = progressiveBillingRates.stream().filter(
                progressiveBillingRate -> progressiveBillingRate.getEnabled()
        ).filter(
                progressiveBillingRate -> {
                    if (progressiveBillingRate.getBillingCycle().equals(BillingCycle.DAILY)) {
                        return inventoryAgingByLPN.getAgeInDays() >= progressiveBillingRate.getCycleStart() &&
                                inventoryAgingByLPN.getAgeInDays() <= progressiveBillingRate.getCycleEnd();
                    }
                    else if (progressiveBillingRate.getBillingCycle().equals(BillingCycle.WEEKLY)) {
                        return inventoryAgingByLPN.getAgeInWeeks() >= progressiveBillingRate.getCycleStart() &&
                                inventoryAgingByLPN.getAgeInWeeks() <= progressiveBillingRate.getCycleEnd();
                    }
                    return false;
                }
        ).findFirst().orElse(null);

        if (Objects.nonNull(rate)) {
            return rate.getRate();
        }
        return 0.0;
    }

}
