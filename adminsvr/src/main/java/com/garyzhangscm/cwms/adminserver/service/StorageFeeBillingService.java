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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public abstract class StorageFeeBillingService implements BillingService {
    private static final Logger logger = LoggerFactory.getLogger(StorageFeeBillingService.class);
    @Autowired
    private BillingRateService billingRateService;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private BillingRequestService billingRequestService;


    public abstract BillableCategory getBillableCategory();


    @Override
    public BillingRequest generateBillingRequest(ZonedDateTime startTime, ZonedDateTime endTime,
                                                 Long companyId, Long warehouseId, Long clientId,
                                                 String number, Boolean serialize,
                                                 Boolean includeDaysSinceInWarehouseForStorageFee) {

        // Get the rate first
        BillingRate billingRate = billingRateService.findByCategory(companyId, warehouseId, clientId, getBillableCategory(), false);

        if (Objects.isNull(billingRate)) {

            logger.debug("billing rate of {} is not defined for company id {}, warehouse id {}, client id {}",
                    getBillableCategory(), companyId, warehouseId, clientId);
            return null;
        }
        if (!billingRate.getEnabled()) {
            logger.debug("billing rate of {} is not enabled for company id {}, warehouse id {}, client id {}",
                    getBillableCategory(), companyId, warehouseId, clientId);
            return null;
        }

        // we will get daily rate and then get all the

        double dailyRate = 0;


        switch (billingRate.getBillingCycle()) {
            case DAILY:
                dailyRate = billingRate.getRate();
                break;
            case WEEKLY:
                dailyRate = billingRate.getRate() / 7;
                break;
            case BI_WEEKLY:
                dailyRate = billingRate.getRate() / 14;
                break;
            case MONTHLY:
                dailyRate = billingRate.getRate() / 30;
                break;
            case BI_MONTHLY:
                dailyRate = billingRate.getRate() / 60;
                break;
            case QUARTERLY:
                dailyRate = billingRate.getRate() / 90;
                break;
            case YEARLY:
                dailyRate = billingRate.getRate() / 365;
                break;
        }


        if (dailyRate > 0) {
            // ZonedDateTime startTime = startDate.atStartOfDay().atZone(ZoneOffset.UTC);
            // ZonedDateTime endTime = endDate.plusDays(1).atStartOfDay().minusSeconds(1).atZone(ZoneOffset.UTC);
            logger.debug("start to generate billing request between {} and {}, with daily rate {}, for category {}",
                    startTime, endTime,
                    dailyRate,
                    billingRate.getBillableCategory());
            return generateBillingRequestByDailyBillingCycle(billingRate,
                    startTime,
                    endTime,
                    companyId, warehouseId, clientId, number, serialize, dailyRate);
        }
        return null;

    }

    private BillingRequest generateBillingRequestByDailyBillingCycle(
            BillingRate billingRate,
            ZonedDateTime startTime, ZonedDateTime endTime,
            Long companyId, Long warehouseId, Long clientId,
            String number, Boolean serialize,
            double dailyRate
    ) {
        // let's get the location utilization snapshot first
        // since it is for daily billing cycle, we will get the
        // location utilization snapshot from the date of startTime
        // until the date of the endTime, then sort the result
        // by date. If there's multiple records for the same day, then
        // we will average the number for each day

        if (startTime.isAfter(endTime)) {
            // the user passed in the wrong date range
            return null;
        }
        List<ClientLocationUtilizationSnapshotBatch> clientLocationUtilizationSnapshotBatches =
                inventoryServiceRestemplateClient.getLocationUtilizationSnapshotByClient(warehouseId, clientId, startTime, endTime);
        logger.debug("we get {} clientLocationUtilizationSnapshotBatches",
                clientLocationUtilizationSnapshotBatches.size());
        if (clientLocationUtilizationSnapshotBatches.isEmpty()) {
            // ok, nothing needs to be billed for the client but we will still
            // generate a billing request with 0 charge
            return new BillingRequest(companyId, warehouseId, clientId,
                    Strings.isNotBlank(number) ? number : billingRequestService.getNextNumber(warehouseId),
                    getBillableCategory(),
                    billingRate.getRate(), BillingCycle.DAILY,
                    0.0, 0.0);
        }
        // Now we will sort the location utilization snapshot by date
        Map<LocalDate, List<Double>> dailyAmountMap = new HashMap<>();
        clientLocationUtilizationSnapshotBatches.forEach(
                clientLocationUtilizationSnapshotBatch -> {
                    LocalDate date = clientLocationUtilizationSnapshotBatch.getCreatedTime().toLocalDate();

                    List<Double> existingAmounts = dailyAmountMap.getOrDefault(date, new ArrayList<>());
                    switch (getBillableCategory()) {
                        case STORAGE_FEE_BY_NET_VOLUME:
                            existingAmounts.add(clientLocationUtilizationSnapshotBatch.getNetVolume());
                            break;
                        case STORAGE_FEE_BY_GROSS_VOLUME:
                            existingAmounts.add(clientLocationUtilizationSnapshotBatch.getGrossVolume());
                            break;
                        case STORAGE_FEE_BY_LOCATION_COUNT:
                            existingAmounts.add(clientLocationUtilizationSnapshotBatch.getTotalLocations() * 1.0);
                            break;
                        case STORAGE_FEE_BY_PALLET_COUNT:
                            existingAmounts.add(clientLocationUtilizationSnapshotBatch.getTotalLPNs() * 1.0);
                            break;
                    }
                    dailyAmountMap.put(date, existingAmounts);
                }
        );
        logger.debug("after processed the clientLocationUtilizationSnapshotBatches map, we get daily number: \n {} ",
                dailyAmountMap);
        // OK, now let's get the average amount for each day
        Map<LocalDate, Double> dailyAverageAmountMap = new HashMap<>();
        dailyAmountMap.entrySet().stream().forEach(
                entry -> {
                    double averageAmount = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    if (averageAmount > 0) {
                        dailyAverageAmountMap.put(
                                entry.getKey(), averageAmount);
                    }
                }
        );

        logger.debug("after processed the daily number, we get daily average number \n {} ",
                dailyAverageAmountMap);
        // we will generate billing request from the daily average amount
        BillingRequest billingRequest = new BillingRequest(companyId, warehouseId, clientId,
                Strings.isNotBlank(number) ? number : billingRequestService.getNextNumber(warehouseId),
                getBillableCategory(),
                dailyRate, BillingCycle.DAILY,
                0.0, 0.0);
        dailyAverageAmountMap.entrySet().forEach(
                entry -> {
                    logger.debug("start to create billing request line with date {}, between {} and {}, " +
                                    "UTC is between {} and {}",
                            entry.getKey(),
                            entry.getKey().atStartOfDay(),
                            entry.getKey().plusDays(1).atStartOfDay().minusNanos(1),
                            entry.getKey().atStartOfDay().atZone(ZoneOffset.UTC),
                            entry.getKey().plusDays(1).atStartOfDay().minusNanos(1).atZone(ZoneOffset.UTC));

                    BillingRequestLine billingRequestLine = new BillingRequestLine(
                            billingRequest, entry.getKey().atStartOfDay().atZone(ZoneOffset.UTC),
                            entry.getKey().plusDays(1).atStartOfDay().minusNanos(1).atZone(ZoneOffset.UTC),
                            entry.getKey(),
                            entry.getValue(),
                            entry.getValue() * billingRate.getRate(),
                            billingRate.getRate()
                    );
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


}
