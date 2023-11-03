/**
 * Copyright 2018
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

package com.garyzhangscm.cwms.workorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.model.lightMES.Machine;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class ItemProductivityReportService   {

    private static final Logger logger = LoggerFactory.getLogger(ItemProductivityReportService.class);

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Autowired
    private ProductionLineAssignmentService productionLineAssignmentService;

    @Autowired
    private WorkOrderConfigurationService workOrderConfigurationService;

    @Autowired
    private ProductionLineCapacityService productionLineCapacityService;

    @Autowired
    private WorkOrderProduceTransactionService workOrderProduceTransactionService;

    private final static String REDIS_KEY_ITEM_PRODUCTIVITY_REPORT = "Item_Productivity_Report";
    private final static Duration REDIS_CACHE_DURATION = Duration.ofMinutes(2l);

    @Autowired
    private RedisTemplate redisTemplate;
    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    public List<ItemProductivityReport> getItemProductivityReportForCurrentShiftWithCache(Long warehouseId,
                                                                                            String itemFamilyName,
                                                                                            String itemName,
                                                                                          Boolean includeNonAvailableQuantity) throws JsonProcessingException {

        // default to include the non available quantity
        if (Objects.isNull(includeNonAvailableQuantity)) {
            includeNonAvailableQuantity = false;
        }

        String redisCacheKey = REDIS_KEY_ITEM_PRODUCTIVITY_REPORT + "-" + warehouseId + "-" + includeNonAvailableQuantity;

        Object itemProductivityReportsObj = redisTemplate.opsForValue().get(redisCacheKey);

        if (Objects.nonNull(itemProductivityReportsObj)) {
            logger.debug("get item productivity reports from cache:\n{}", itemProductivityReportsObj);
            String json = objectMapper.writeValueAsString(itemProductivityReportsObj);
            List<ItemProductivityReport> itemProductivityReports = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ItemProductivityReport.class));

            if (Strings.isNotBlank(itemName)) {
                itemProductivityReports = itemProductivityReports.stream().filter(
                        itemProductivityReport -> itemName.equalsIgnoreCase(itemProductivityReport.getItemName())
                ).collect(Collectors.toList());
            }

            if (Strings.isNotBlank(itemFamilyName)){
                itemProductivityReports = itemProductivityReports.stream().filter(
                        itemProductivityReport -> itemFamilyName.equalsIgnoreCase(itemProductivityReport.getItemFamilyName())
                ).collect(Collectors.toList());
            }

            return itemProductivityReports;
        }
        else {
            logger.debug("item productivity reports is not in the redis cache, let's get the real time data and save it to the cache");
            List<ItemProductivityReport> itemProductivityReports =
                    getItemProductivityReportForCurrentShift(warehouseId, itemFamilyName, itemName, includeNonAvailableQuantity);

            // save the result to the redis
            redisTemplate.opsForValue().set(redisCacheKey, itemProductivityReports, REDIS_CACHE_DURATION);

            if (Strings.isNotBlank(itemName)) {
                itemProductivityReports = itemProductivityReports.stream().filter(
                        itemProductivityReport -> itemName.equalsIgnoreCase(itemProductivityReport.getItemName())
                ).collect(Collectors.toList());
            }

            if (Strings.isNotBlank(itemFamilyName)){
                itemProductivityReports = itemProductivityReports.stream().filter(
                        itemProductivityReport -> itemFamilyName.equalsIgnoreCase(itemProductivityReport.getItemFamilyName())
                ).collect(Collectors.toList());
            }

            return itemProductivityReports;

        }

    }

    public List<ItemProductivityReport> getItemProductivityReportForCurrentShift(Long warehouseId,
                                                                                 String itemFamilyName,
                                                                                 String itemName,
                                                                                 Boolean includeNonAvailableQuantity) {


        Pair<ZonedDateTime, ZonedDateTime> currentShift = workOrderConfigurationService.getCurrentShift(warehouseId);
        return getItemProductivityReports(warehouseId,
                currentShift.getFirst(), currentShift.getSecond(),
                itemFamilyName, itemName,
                includeNonAvailableQuantity);

    }

    public List<ItemProductivityReport> getItemProductivityReports(Long warehouseId,
                                                                   ZonedDateTime startTime,
                                                                   ZonedDateTime endTime,
                                                                   String itemFamilyName,
                                                                   String itemName,
                                                                   Boolean includeNonAvailableQuantity) {
        if (Objects.isNull(startTime)) {
            throw WorkOrderException.raiseException("can't get item productivity report as there's no start time");
        }
        if (Objects.isNull(endTime)) {
            endTime = ZonedDateTime.now(ZoneOffset.UTC);
        }


        logger.debug("start to get item productivity report by time range [{}, {}], for item {}, item family {}",
                startTime, endTime,
                Strings.isBlank(itemName) ? "N/A" : itemName,
                Strings.isBlank(itemFamilyName) ? "N/A" : itemFamilyName);

        // first of all, let's get all the production line assignment between the start time and end time

        List<ProductionLineAssignment> productionLineAssignments =
                productionLineAssignmentService.getProductionAssignmentByTimeRange(warehouseId,
                        startTime, endTime);

        logger.debug("get {} production line assignment within the time range [{}, {}]",
                productionLineAssignments.size(), startTime, endTime);
        if (Strings.isNotBlank(itemName)) {
            productionLineAssignments = productionLineAssignments.stream().filter(
                    productionLineAssignment -> itemName.equalsIgnoreCase(productionLineAssignment.getItemName())
            ).collect(Collectors.toList());
        }
        if (Strings.isNotBlank(itemFamilyName)) {
            productionLineAssignments = productionLineAssignments.stream().filter(
                    productionLineAssignment -> itemFamilyName.equalsIgnoreCase(productionLineAssignment.getItemFamilyName())
            ).collect(Collectors.toList());
        }
        // loop through each production line assign, group the result by item name
        // key: item name
        // value: overall productivity during the time span
        Map<String, ItemProductivityReport> itemProductivityReportMap = new HashMap<>();

        ZonedDateTime currentTime = ZonedDateTime.now(ZoneOffset.UTC);

        for (ProductionLineAssignment productionLineAssignment : productionLineAssignments) {
            logger.debug("start to process production line assignment with work order {}, production line {}",
                    productionLineAssignment.getWorkOrder().getNumber(),
                    productionLineAssignment.getProductionLine().getName());
            if (Strings.isBlank(productionLineAssignment.getItemName())) {
                logger.debug("the process production line assignment with work order {}, production line {}, item name is blank",
                        productionLineAssignment.getWorkOrder().getNumber(),
                        productionLineAssignment.getProductionLine().getName());
                continue;
            }
            ZonedDateTime reportStartTime = startTime;
            if (productionLineAssignment.getAssignedTime().isAfter(reportStartTime)) {
                reportStartTime = productionLineAssignment.getAssignedTime();
            }

            ZonedDateTime reportEndTime = endTime;

            ItemProductivityReport itemProductivityReport =
                    itemProductivityReportMap.getOrDefault(productionLineAssignment.getItemName(),
                            new ItemProductivityReport(
                                    warehouseId,
                                    productionLineAssignment.getItemName(),
                                    productionLineAssignment.getItemFamilyName()
                            ));
            logger.debug("Before process, the item productivity report for item {}  is \n {} ",
                    productionLineAssignment.getItemName(),
                    itemProductivityReport);

            ItemProductionLineProductivityReport itemProductionLineProductivityReport =
                    new ItemProductionLineProductivityReport(
                            warehouseId,
                            productionLineAssignment.getItemName(),
                            productionLineAssignment.getItemFamilyName(),
                            productionLineAssignment.getProductionLine().getName());

            long realTimeGoal = getRealTimeGoal(warehouseId,
                    productionLineAssignment, reportStartTime, reportEndTime, currentTime);
            itemProductionLineProductivityReport.setRealTimeGoal(realTimeGoal);
            logger.debug(">>  real time goal within time range [{}, {}]: {}",
                    reportStartTime, reportEndTime, realTimeGoal);

            Pair<Integer, Long> actualQuantities = getActualQuantity(warehouseId,
                    productionLineAssignment,
                    reportStartTime, reportEndTime, currentTime,
                    includeNonAvailableQuantity);
            itemProductionLineProductivityReport.setActualPalletQuantity(actualQuantities.getFirst());
            itemProductionLineProductivityReport.setActualQuantity(actualQuantities.getSecond());
            logger.debug(">>  actual quantities within time range [{}, {}]: {} / {}",
                    reportStartTime, reportEndTime,
                    actualQuantities.getFirst(), actualQuantities.getSecond());

            Long expectedProducedQuantity = getExpectedProducedQuantity(warehouseId,
                    productionLineAssignment, reportStartTime, reportEndTime);

            itemProductionLineProductivityReport.setExpectedProducedQuantity(expectedProducedQuantity);

            logger.debug(">>  expected produced quantity within time range [{}, {}]: {} / {}",
                    reportStartTime, reportEndTime, itemProductionLineProductivityReport.getExpectedProducedQuantity());

            logger.debug("add itemProductionLineProductivityReport:\n {}", itemProductionLineProductivityReport);
            itemProductivityReport.addItemProductionLineProductivityReport(itemProductionLineProductivityReport);


            logger.debug("after process, the item productivity report for item {}  is \n {} ",
                    productionLineAssignment.getItemName(),
                    itemProductivityReport);

            itemProductivityReportMap.put(productionLineAssignment.getItemName(),
                    itemProductivityReport);

        }
        List<ItemProductivityReport> itemProductivityReports =
                itemProductivityReportMap.values().stream().collect(Collectors.toList());
        logger.debug("We get {} item productivity reports", itemProductivityReports.size());

        // sort by item name
        Collections.sort(itemProductivityReports, Comparator.comparing(ItemProductivityReport::getItemName));
        return itemProductivityReports;

    }

    /**
     * Get the estimated finish rate based on the time, and whether the production line is still assigned
     * @param startTime
     * @param endTime
     * @param productionLineAssignment
     * @return
     */
    private double getEstimatedFinishRate(ZonedDateTime startTime, ZonedDateTime endTime, ProductionLineAssignment productionLineAssignment) {
        ZonedDateTime productionStartTime = startTime;
        ZonedDateTime productionEndTime = endTime;
        if (productionLineAssignment.getAssignedTime().isAfter(startTime)) {
            productionStartTime = productionLineAssignment.getAssignedTime();
        }
        if (Objects.nonNull(productionLineAssignment.getDeassignedTime()) && productionLineAssignment.getDeassignedTime().isBefore(endTime)) {
            productionEndTime = productionLineAssignment.getDeassignedTime();
        }
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneOffset.UTC);

        logger.debug(">>  start to calculate estimated finish rate within time range [{}, {}], by {}: {} / {} = {}",
                productionStartTime, productionEndTime, currentTime);

        if (currentTime.isBefore(productionStartTime)) {
            // the machine is not start yet at the start time
            return 0;
        }
        else if (currentTime.isAfter(productionEndTime)) {
            // the production activity is already end, we assume that the finish
            // rate should be 100% when the production is done
            return 1;
        }
        else {


            return ChronoUnit.MINUTES.between(productionStartTime, currentTime) * 1.0 /
                    ChronoUnit.MINUTES.between(productionStartTime, productionEndTime);
        }

    }


    /**
     * Refresh machine status every minute and save it to the redis. The web user then can get the data
     * from cache
     */
    @Scheduled(fixedDelay = 60000)
    public void refreshItemProductivityReports(){

        List<Company> companies = warehouseLayoutServiceRestemplateClient.getAllCompanies();
        for (Company company : companies) {
            List<Warehouse> warehouses = warehouseLayoutServiceRestemplateClient.getWarehouseByCompany(company.getId());
            for (Warehouse warehouse : warehouses) {
                logger.debug("Start to refresh the item productivity report for warehouse {} / {} in redis",
                        company.getName(), warehouse.getName());
                WorkOrderConfiguration workOrderConfiguration = workOrderConfigurationService.getWorkOrderConfiguration(
                        company.getId(), warehouse.getId()
                );
                if (Boolean.TRUE.equals(workOrderConfiguration.getAutoRecordItemProductivity())) {

                    logger.debug("Auto refresh item productivity report is enabled for warehouse  {} / {}",
                            company.getName(), warehouse.getName());
                    refreshItemProductivityReportForCurrentShift(warehouse.getId());
                }
                else {

                    logger.debug("Auto refresh item productivity report is NOT enabled for warehouse  {} / {}",
                            company.getName(), warehouse.getName());
                }
            }
        }
    }

    private void refreshItemProductivityReportForCurrentShift(Long warehouseId){
        try {
            List<ItemProductivityReport> itemProductivityReports =
                    getItemProductivityReportForCurrentShift(warehouseId, null, null, true);

            // save the result to the redis
            redisTemplate.opsForValue().set(REDIS_KEY_ITEM_PRODUCTIVITY_REPORT + "-" + warehouseId + "-true", itemProductivityReports, REDIS_CACHE_DURATION);

            itemProductivityReports =
                    getItemProductivityReportForCurrentShift(warehouseId, null, null, false);

            // save the result to the redis
            redisTemplate.opsForValue().set(REDIS_KEY_ITEM_PRODUCTIVITY_REPORT + "-" + warehouseId + "-false", itemProductivityReports, REDIS_CACHE_DURATION);


        }
        catch (Exception ex) {
            // ignore the exception
            logger.debug("Ignore exception {} when refresh the light MES machine status in redis",
                    ex.getMessage());
        }

    }

    /**
     * Get the real time goal based on the production line assignment, production line capacity and
     * the time span
     * @param warehouseId
     * @param productionLineAssignment
     * @param startTime
     * @param endTime
     * @param currentTime
     * @return
     */
    public long getRealTimeGoal(Long warehouseId,
                                ProductionLineAssignment productionLineAssignment, ZonedDateTime startTime,
                                ZonedDateTime endTime, ZonedDateTime currentTime) {

        ZonedDateTime reportStartTime = startTime;
        if (productionLineAssignment.getAssignedTime().isAfter(reportStartTime)) {
            reportStartTime = productionLineAssignment.getAssignedTime();
        }

        ZonedDateTime reportEndTime = endTime;
        if (reportEndTime.isAfter(currentTime)) {
            reportEndTime = currentTime;
        }
        if (Objects.nonNull(productionLineAssignment.getDeassignedTime()) &&
            productionLineAssignment.getDeassignedTime().isBefore(reportEndTime)) {
            reportEndTime = productionLineAssignment.getDeassignedTime();
        }
        if (reportStartTime.isAfter(reportEndTime)) {
            return 0;
        }


        // get the expected produced quantity in this shift
        ProductionLineCapacity productionLineCapacity = productionLineCapacityService.findByProductionLineAndItem(
                warehouseId, productionLineAssignment.getProductionLine().getId(),
                productionLineAssignment.getWorkOrder().getItemId(),
                false);

        if (Objects.nonNull(productionLineCapacity)) {
            logger.debug("we find the capacity setup for production line {} / {}, item {}",
                    productionLineAssignment.getProductionLine().getId(),
                    productionLineAssignment.getProductionLine().getName(),
                    productionLineAssignment.getWorkOrder().getItemId());
            // see how many we are supposed to produce in this shift
            int minutes = (int) ChronoUnit.MINUTES.between(reportStartTime, reportEndTime);
            logger.debug("there're {} minutes difference between {} and {}",
                    minutes, reportStartTime, reportEndTime);
            Long expectedProducedQuantity = productionLineCapacityService.getExpectedProduceQuantityByMinutes(
                    productionLineCapacity, minutes
            );


            logger.debug("we should produce {} unit based on the estimation, within time range [{}, {}]",
                    expectedProducedQuantity,
                    reportStartTime,
                    reportEndTime);
            return expectedProducedQuantity;

        }
        else {

            logger.debug("NO capacity setup for production line {} / {}, item {}",
                    productionLineAssignment.getProductionLine().getId(),
                    productionLineAssignment.getProductionLine().getName(),
                    productionLineAssignment.getWorkOrder().getItemId());
            return 0;
        }

    }

    /**
     * Get the actual quantity based on the production line assignment, and the time span
     * @param warehouseId
     * @param productionLineAssignment
     * @param startTime
     * @param endTime
     * @param currentTime
     * @return
     */
    public Pair<Integer, Long> getActualQuantity(Long warehouseId,
                                                 ProductionLineAssignment productionLineAssignment,
                                                 ZonedDateTime startTime,
                                                 ZonedDateTime endTime,
                                                 ZonedDateTime currentTime,
                                                 Boolean includeNonAvailableQuantity) {

        ZonedDateTime reportStartTime = startTime;
        if (productionLineAssignment.getAssignedTime().isAfter(reportStartTime)) {
            reportStartTime = productionLineAssignment.getAssignedTime();
        }

        ZonedDateTime reportEndTime = endTime;
        if (reportEndTime.isAfter(currentTime)) {
            reportEndTime = currentTime;
        }
        if (Objects.nonNull(productionLineAssignment.getDeassignedTime()) &&
                productionLineAssignment.getDeassignedTime().isBefore(reportEndTime)) {
            reportEndTime = productionLineAssignment.getDeassignedTime();
        }
        if (reportStartTime.isAfter(reportEndTime)) {
            return Pair.of(0, 0l);
        }

        // key: production line id - work order id
        // value: Pair of LPN quantity and total quantity within the time range
        Map<String, Pair<Integer, Long>>  producedQuantityMap = workOrderProduceTransactionService.getProducedQuantityByTimeRange(
                warehouseId, productionLineAssignment.getWorkOrder().getNumber(),
                productionLineAssignment.getProductionLine().getId(),
                reportStartTime, reportEndTime, includeNonAvailableQuantity,
                true);

        // there should be only one record in the above map
        if (producedQuantityMap.size() != 1) {
            return Pair.of(0, 0l);
        }
        else {
            return producedQuantityMap.entrySet().iterator().next().getValue();
        }


    }

    public Long getExpectedProducedQuantity(long warehouseId, ProductionLineAssignment productionLineAssignment,
                                ZonedDateTime startTime,
                                ZonedDateTime endTime) {

        ProductionLineCapacity productionLineCapacity = productionLineCapacityService.findByProductionLineAndItem(
                warehouseId, productionLineAssignment.getProductionLine().getId(),
                productionLineAssignment.getWorkOrder().getItemId(),
                false);

        if (Objects.nonNull(productionLineCapacity)) {
            logger.debug("we find the capacity setup for production line {} / {}, item {}",
                    productionLineAssignment.getProductionLine().getId(),
                    productionLineAssignment.getProductionLine().getName(),
                    productionLineAssignment.getWorkOrder().getItemId());

            ZonedDateTime actualStartTime = productionLineAssignment.getAssignedTime().isBefore(startTime) ?
                    startTime : productionLineAssignment.getAssignedTime();

            // check if the item is actively assigned to the production line during the start and end time
            boolean isActive = Objects.isNull(productionLineAssignment.getDeassignedTime())
                    || productionLineAssignment.getDeassignedTime().isAfter(endTime);
            ZonedDateTime actualEndTime = isActive?
                    endTime : productionLineAssignment.getDeassignedTime();

            // see how many we are supposed to produce in this shift
            int minutes = (int) ChronoUnit.MINUTES.between(actualStartTime, actualEndTime);
            logger.debug("there're {} minutes difference between {} and {}",
                    minutes, actualStartTime, actualEndTime);
            Long expectedProducedQuantity = productionLineCapacityService.getExpectedProduceQuantityByMinutes(
                    productionLineCapacity, minutes
            );


            logger.debug("we should produce {} unit based on the estimation, within time range [{}, {}]",
                    expectedProducedQuantity,
                    startTime,
                    endTime);
            return expectedProducedQuantity;

        }
        else {

            logger.debug("NO capacity setup for production line {} / {}, item {}",
                    productionLineAssignment.getProductionLine().getId(),
                    productionLineAssignment.getProductionLine().getName(),
                    productionLineAssignment.getWorkOrder().getItemId());
            return 0l;
        }
    }
}
