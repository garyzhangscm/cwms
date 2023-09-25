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
import com.garyzhangscm.cwms.workorder.clients.LightMESRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.model.lightMES.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class LightMESService {
    private static final Logger logger = LoggerFactory.getLogger(LightMESService.class);

    @Autowired
    private LightMESRestemplateClient lightMESRestemplateClient;
    @Autowired
    private LightMESConfigurationService lightMESConfigurationService;

    @Autowired
    private ProductionLineService productionLineService;

    @Autowired
    private WorkOrderConfigurationService workOrderConfigurationService;

    @Autowired
    private WorkOrderProduceTransactionService workOrderProduceTransactionService;

    @Autowired
    private ProductionLineCapacityService productionLineCapacityService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Autowired
    private RedisTemplate redisTemplate;
    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    private final static String REDIS_KEY_CURRENT_SHIFT_MACHINE_STATUS = "lightMES_machine_current_shift_status";

    public List<Machine> getMachineList(Long warehouseId) {
        return lightMESRestemplateClient.getMachineList(warehouseId);

    }

    public LightStatus getSingleLightStatus(Long warehouseId, String sim) {
        return lightMESRestemplateClient.getSingleLightStatus(warehouseId, sim);

    }

    public List<Machine> getCurrentShiftMachineStatusWithCache(Long warehouseId, String machineNo,
                                                   String type) throws JsonProcessingException {
        if (Strings.isNotBlank(machineNo)) {
            // for status of single machine, we will get the status without consider the cache
            return getCurrentShiftMachineStatus(warehouseId, machineNo,
                    type);
        }
        else {

            logger.debug("start to get cached machine status");
            String redisKey = REDIS_KEY_CURRENT_SHIFT_MACHINE_STATUS + "-" + warehouseId;
            Object machineStatus = redisTemplate.opsForValue().get(redisKey);
            if (Objects.nonNull(machineStatus)) {
                logger.debug("get machine status from cache:\n{}", machineStatus);
                String json = objectMapper.writeValueAsString(machineStatus);
                List<Machine> machines = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Machine.class));
                if (Strings.isNotBlank(type)) {
                    machines = machines.stream().filter(
                            machine -> type.equalsIgnoreCase(machine.getProductionLineTypeName())
                    ).collect(Collectors.toList());
                }

                return machines;
            }
            else {
                logger.debug("machine status is not in the redis cache, let's get the real time data and save it to the cache");
                List<Machine> machines = getCurrentShiftMachineStatus(warehouseId, null, type);

                // save the result to the redis
                redisTemplate.opsForValue().set(redisKey, machines, Duration.ofMinutes(3));

                if (Strings.isNotBlank(type)) {
                    machines = machines.stream().filter(
                            machine -> type.equalsIgnoreCase(machine.getProductionLineTypeName())
                    ).collect(Collectors.toList());
                }

                return machines;

            }
        }

    }

    public List<Machine> getCurrentShiftMachineStatus(Long warehouseId, String machineNo,
                                          String type) {


        Pair<ZonedDateTime, ZonedDateTime> currentShift = workOrderConfigurationService.getCurrentShift(warehouseId);
        if (Objects.isNull(currentShift) || Objects.isNull(currentShift.getFirst()) || Objects.isNull(currentShift.getSecond())) {
            throw WorkOrderException.raiseException("Shift is not setup. fail to get production line data");
        }
        return getMachineStatus(warehouseId, machineNo, type,
                currentShift.getFirst(), currentShift.getSecond());
    }

    /**
     * Get the production line's status from light MES system and the statistics data
     * between the start time and end time
     * @param warehouseId
     * @param machineNo
     * @param type
     * @param startTime
     * @param endTime
     * @return
     */
    public List<Machine> getMachineStatus(Long warehouseId, String machineNo,
                                          String type,
                                          ZonedDateTime startTime,
                                          ZonedDateTime endTime) {
        logger.debug("start to get machine status, for single machine? {}, of type {}",
                Strings.isBlank(machineNo) ? "N/A" : machineNo,
                Strings.isBlank(type) ? "N/A" : type);

        List<ProductionLine> productionLines = productionLineService.findAll(warehouseId, null, null,
                null, type, true, false, true);

        List<Machine> resultMachines = new ArrayList<>();

        logger.debug("Got {} production lines from the machine list", productionLines.size());

        List<Machine> machines = getMachineList(warehouseId);
        if (Strings.isNotBlank(machineNo)) {
            machines = machines.stream().filter(machine -> machineNo.equalsIgnoreCase(machine.getMachineNo())).collect(Collectors.toList());
        }

        // get the production lines and its active work order
        if (machines.isEmpty()) {
            return machines;
        }
        logger.debug("Get {} machines", machines.size());
        // convert the machines from list to map so we can get the machine easily from the production line's name
        // we will always assume that the machine's name is the same as production line's name
        Map<String, Machine> machineMap = new HashMap<>();
        machines.forEach(
                machine -> machineMap.put(machine.getMachineNo(), machine)
        );



        // setup the current state of the machine
        // 三色灯状态码：001-绿灯，010-黄灯，100-红灯，000-关灯
        List<String> simList  = machines.stream().map(machine -> machine.getSim()).collect(Collectors.toList());
        List<LightStatus> lightStatuses = lightMESRestemplateClient.getLightStatusInBatch(warehouseId, simList);
        logger.debug("Get {} light status", lightStatuses.size());
        Map<String, String> lightStatusMap = new HashMap<>();
        lightStatuses.forEach(
                lightStatus -> lightStatusMap.put(lightStatus.getSim(), lightStatus.getCurrentState())
        );

        // key: production line id - work order id
        // value: Pair of LPN quantity and total quantity within the time range
        Map<String, Pair<Integer, Long>> producedQuantityMap =
                workOrderProduceTransactionService.getProducedQuantityByTimeRange(
                    warehouseId, null, null,
                    startTime, endTime, true);
        logger.debug("get {} produced quantity information within the time range[{}, {}]",
                    producedQuantityMap.size(),
                startTime, endTime);

        // loop through each production line. if there's machine setup for the production line, then get the data
        // from the server. otherwise, leave those data blank
        for (ProductionLine productionLine : productionLines) {
            logger.debug("start to get LES machine information for production line {}", productionLine.getName());
            Machine machine = machineMap.getOrDefault(productionLine.getName(), new Machine());
            logger.debug("machine found BY NAME {} ? {}",
                    productionLine.getName(),
                    Strings.isBlank(machine.getSim()) ? "N/A" : machine.getMachineNo());

            machine.setProductionLineTypeName(Objects.isNull(productionLine.getType()) ? "" : productionLine.getType().getName());
            if (Strings.isBlank(machine.getMachineNo())) {
                machine.setMachineNo(productionLine.getName());
            }
            logger.debug("the production line has {} assignment",
                    productionLine.getProductionLineAssignments().size());

            if (!productionLine.getProductionLineAssignments().isEmpty()) {

                logger.debug(">> start to setup the machine's statistics data based on the production line's assignment");
                for (ProductionLineAssignment productionLineAssignment : productionLine.getProductionLineAssignments()) {
                    if (productionLineAssignmentWithinTimeRange(productionLineAssignment, startTime, endTime)) {

                        MachineStatistics machineStatistics = getMachineStatistics(
                                warehouseId,
                                productionLine, productionLineAssignment,
                                startTime, endTime, producedQuantityMap
                        );
                        machine.addMachineStatistics(machineStatistics);
                    }
                }

            }

            machine.setCurrentState(
                    lightStatusMap.getOrDefault(machine.getSim(), "")
            );
            resultMachines.add(machine);
        }

        Collections.sort(resultMachines, Comparator.comparing(Machine::getMachineNo));


        LightMESConfiguration lightMESConfiguration = lightMESConfigurationService.findByWarehouse(warehouseId);

        // setup the machine's pulse count

        for (Machine machine : resultMachines) {

            setupPulseCountAndCycleTime(warehouseId, lightMESConfiguration, machine, startTime, endTime);
        }
        return resultMachines;


    }

    /**
     * Check if the production line assignment is within a certain time range
     * @param productionLineAssignment
     * @return
     */
    private boolean productionLineAssignmentWithinTimeRange(ProductionLineAssignment productionLineAssignment,
                                                            ZonedDateTime startTime,
                                                            ZonedDateTime endTime) {
        // if the production line is assigned before the start time, then
        // make sure if it is still assigned, or deassigned after the start time
        if (productionLineAssignment.getAssignedTime().isBefore(startTime)) {
            return Objects.isNull(productionLineAssignment.getDeassignedTime()) ||
                  productionLineAssignment.getDeassignedTime().isAfter(startTime);
        }
        else if (productionLineAssignment.getAssignedTime().isAfter(endTime)) {
            // the production line is assigned the end time, so there's no chance that the
            // production line assignment is within the time range
            return false;
        }
        else {
            // the production line is assigned within start time and end time
            return true;
        }
    }

    private void setupPulseCountAndCycleTime(Long warehouseId,
                                             LightMESConfiguration lightMESConfiguration,
                                             Machine machine,
                                             ZonedDateTime startTime,
                                             ZonedDateTime endTime) {
        if (Strings.isNotBlank(machine.getSim())) {
            // last hour cycle time and pulse count
            int pulseCountTimeWindow = Objects.isNull(lightMESConfiguration.getCycleTimeWindow()) ? 1 :
                    lightMESConfiguration.getCycleTimeWindow();
            if (pulseCountTimeWindow < 1) {
                pulseCountTimeWindow = 1;
            }
            logger.debug("start to calculate cycle time based on the pulse count window {} for machine {}",
                    pulseCountTimeWindow,
                    machine.getMachineNo());
            ZonedDateTime pulseCountWindowEndTime = ZonedDateTime.now();
            ZonedDateTime pulseCountWindowStartTime = pulseCountWindowEndTime.minusMinutes(pulseCountTimeWindow);

            int lastTimeWindowPulseCount = lightMESRestemplateClient.getSingleLightPulseByTimeRange(
                        warehouseId, pulseCountWindowStartTime, pulseCountWindowEndTime, machine.getSim()
            );
            logger.debug("get {} pulse for machine in the past {} minutes",
                    machine.getMachineNo(),
                    pulseCountTimeWindow);
            machine.setLastTimeWindowPulseCount(lastTimeWindowPulseCount);
            if (lastTimeWindowPulseCount <= 0) {
                machine.setLastTimeWindowCycleTime(0);
            }
            else {
                machine.setLastTimeWindowCycleTime(pulseCountTimeWindow * 60 / lastTimeWindowPulseCount);
            }
            // sleep 0.1 second as we are only allowed to call the getSingleLightPulseByTimeRange endpoint
            // 10 times per second
            /**
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }**/

                // this shift cycle time and pulse count
            if (endTime.isAfter(ZonedDateTime.now())) {
                endTime = ZonedDateTime.now();
            }
            int pulseCountWithinTimeRnage = lightMESRestemplateClient.getSingleLightPulseByTimeRange(
                        warehouseId, startTime, endTime, machine.getSim()
            );

            machine.setPulseCount(pulseCountWithinTimeRnage);
            if (pulseCountWithinTimeRnage <= 0) {
                machine.setCycleTime(0);
            }
            else {
                logger.debug("production line {}, minutes {}, shift pulse {}",
                            machine.getMachineNo(),
                            ChronoUnit.SECONDS.between(startTime, endTime),
                            pulseCountWithinTimeRnage);
                machine.setCycleTime((int)ChronoUnit.SECONDS.between(startTime, endTime) / pulseCountWithinTimeRnage);
            }

        }

    }

    private MachineStatistics getMachineStatistics(Long warehouseId,
                                                   ProductionLine productionLine,
                                                   ProductionLineAssignment productionLineAssignment,
                                                   ZonedDateTime startTime,
                                                   ZonedDateTime endTime,
                                                   Map<String, Pair<Integer, Long>> producedQuantityMap) {
        MachineStatistics machineStatistics = new MachineStatistics(
                productionLineAssignment.getWorkOrder().getItem().getName(),
                productionLineAssignment.getWorkOrder().getNumber()
        );

        // see how many we are supposed to produce in the range of start time and end time
        // note, if the production line is assigned after the start time, or
        // deassigned before the end time, then we will need to consider the time range
        // for the time when the production is assigned
        ZonedDateTime actualStartTime = productionLineAssignment.getAssignedTime().isBefore(startTime) ?
                startTime : productionLineAssignment.getAssignedTime();

        // check if the item is actively assigned to the production line during the start and end time
        boolean isActive = Objects.isNull(productionLineAssignment.getDeassignedTime())
                || productionLineAssignment.getDeassignedTime().isAfter(endTime);
        ZonedDateTime actualEndTime = isActive?
                        endTime : productionLineAssignment.getDeassignedTime();

        machineStatistics.setStartTime(actualStartTime);
        machineStatistics.setEndTime(actualEndTime);
        machineStatistics.setActive(isActive);

        // get the expected produced quantity in this shift
        ProductionLineCapacity productionLineCapacity = productionLineCapacityService.findByProductionLineAndItem(
                    warehouseId, productionLine.getId(),
                    productionLineAssignment.getWorkOrder().getItemId(),
                    false);


        if (Objects.nonNull(productionLineCapacity)) {
            logger.debug("we find the capacity setup for production line {} / {}, item {}",
                        productionLine.getId(),
                        productionLine.getName(),
                        productionLineAssignment.getWorkOrder().getItemId());

            int minutes = (int)ChronoUnit.MINUTES.between(actualStartTime, actualEndTime);
            logger.debug("there're {} minutes difference between {} and {}",
                    minutes, actualStartTime, actualEndTime);


            Long expectedProducedQuantity = productionLineCapacityService.getExpectedProduceQuantityByMinutes(
                    productionLineCapacity, minutes
            );
            machineStatistics.setEstimationQuantity(expectedProducedQuantity);

            logger.debug("we should produce {} unit based on the estimation, within current shift",
                    expectedProducedQuantity);

        }
        else {

                logger.debug("NO capacity setup for production line {} / {}, item {}",
                        productionLine.getId(),
                        productionLine.getName(),
                        productionLineAssignment.getWorkOrder().getItemId());
        }

        String key = productionLine.getId() + "-" + productionLineAssignment.getWorkOrder().getId();
        if (producedQuantityMap.containsKey(key)) {
            machineStatistics.setProducedQuantity(producedQuantityMap.get(key).getSecond());

            if (machineStatistics.getEstimationQuantity() > 0) {

                logger.debug("set the achivement rate to {}",
                        machineStatistics.getProducedQuantity() * 1.0 / machineStatistics.getEstimationQuantity());
                machineStatistics.setAchievementRate(
                        machineStatistics.getProducedQuantity()  * 1.0 / machineStatistics.getEstimationQuantity()
                );
            }
            else {
                machineStatistics.setAchievementRate(0);
            }
        }

        return machineStatistics;

    }

    /**
     * Refresh machine status every minute and save it to the redis. The web user then can get the data
     * from cache
     */
    @Scheduled(fixedDelay = 60000)
    public void refreshMachineStatus(){

        List<Company> companies = warehouseLayoutServiceRestemplateClient.getAllCompanies();
        for (Company company : companies) {
            List<Warehouse> warehouses = warehouseLayoutServiceRestemplateClient.getWarehouseByCompany(company.getId());
            for (Warehouse warehouse : warehouses) {
                logger.debug("Start to refresh the machine status for warehouse {} / {} in redis",
                        company.getName(), warehouse.getName());
                refreshMachineStatus(warehouse.getId());
            }
        }
    }

    private void refreshMachineStatus(Long warehouseId){
        try {
            List<Machine> machines = getCurrentShiftMachineStatus(warehouseId, null, null);

            // save the result to the redis
            redisTemplate.opsForValue().set(REDIS_KEY_CURRENT_SHIFT_MACHINE_STATUS + "-" + warehouseId, machines, Duration.ofMinutes(3));

        }
        catch (Exception ex) {
            // ignore the exception
            logger.debug("Ignore exception {} when refresh the light MES machine status in redis",
                    ex.getMessage());
        }

    }

    /**
     * Get the pulse count for each item in the time range
     * @param warehouseId
     * @param itemName
     * @param startTime
     * @param endTime
     * @return
     */
    public List<PulseCountHistoryByItem> getPulseCountHistory(Long warehouseId, String itemName, ZonedDateTime startTime, ZonedDateTime endTime) {

        // key: item name
        // value: PulseCountHistoryByItem
        Map<String, PulseCountHistoryByItem> pulseCountHistoryByItemMap = new HashMap<>();

        List<Machine> machines = getMachineList(warehouseId);

        // get the production lines and its active work order
        if (machines.isEmpty()) {
            return new ArrayList<>();
        }
        logger.debug("Get {} machines to get pulse count history",
                machines.size());
        // key: machine number
        // value: sim
        Map<String, String> machineSimMap = new HashMap<>();
        machines.forEach(
                machine -> machineSimMap.put(machine.getMachineNo(), machine.getSim())
        );

        String productionLineNames = machines.stream().map(Machine::getMachineNo).collect(Collectors.joining(","));
        List<ProductionLine> productionLines = productionLineService.findAll(warehouseId, null, null,
                productionLineNames, "", true, false, true);
        logger.debug("Get {} production lines out of {} machines, by name {}",
                productionLines.size(),
                machines.size(),
                productionLineNames);

        for (ProductionLine productionLine : productionLines) {
            logger.debug("start to process production line {}", productionLine.getName());
            for (ProductionLineAssignment productionLineAssignment : productionLine.getProductionLineAssignments()) {
                // there're 4 times:
                // 1. production line assigned time
                // 2. production line deassigned time(if deassigned)
                // 3. start time
                // 4. end time
                logger.debug(">> work order {}, item {} of production line {}",
                        productionLineAssignment.getWorkOrderNumber(),
                        productionLineAssignment.getItemName(),
                        productionLine.getName());

                // item name is specified, we will only return the production line assignment with
                // the specified item only
                if (Strings.isNotBlank(itemName) && !itemName.equalsIgnoreCase(productionLineAssignment.getItemName())) {
                    continue;
                }

                if (productionLineAssignmentWithinTimeRange(productionLineAssignment, startTime, endTime)) {
                    ZonedDateTime reportStartTime = productionLineAssignment.getAssignedTime().isBefore(startTime) ?
                            startTime : productionLineAssignment.getAssignedTime();
                    ZonedDateTime reportEndTime = endTime;
                    if (Objects.nonNull(productionLineAssignment.getDeassignedTime()) &&
                        productionLineAssignment.getDeassignedTime().isBefore(endTime)) {
                        reportEndTime = productionLineAssignment.getDeassignedTime();
                    }

                    logger.debug(">>>> start to get pulse count for work order {}, item {} of production line {}   " +
                                    " within required range [{}, {}]",
                            productionLineAssignment.getWorkOrderNumber(),
                            productionLineAssignment.getItemName(),
                            productionLine.getName(),
                            reportStartTime, reportEndTime);

                    // get the pulse count for this production line between the start and end time
                    int pulseCount = lightMESRestemplateClient.getSingleLightPulseByTimeRange(warehouseId,
                            reportStartTime, reportEndTime, machineSimMap.get(productionLine.getName()));
                    PulseCountHistoryByItem pulseCountHistoryByItem = pulseCountHistoryByItemMap.getOrDefault(
                            productionLineAssignment.getItemName(),
                            new PulseCountHistoryByItem(productionLineAssignment.getItemName())
                    );
                    logger.debug(">>>> add count {} to item {}",
                            pulseCount, productionLineAssignment.getItemName());

                    pulseCountHistoryByItem.setCount(pulseCountHistoryByItem.getCount() + pulseCount);
                    pulseCountHistoryByItemMap.put(productionLineAssignment.getItemName(),
                            pulseCountHistoryByItem);
                }
                else {
                    logger.debug(">>>> ignore work order {}, item {} of production line {} as " +
                            "  the production assignment [{}, {}] is not within required range [{}, {}]",
                            productionLineAssignment.getWorkOrderNumber(),
                            productionLineAssignment.getItemName(),
                            productionLine.getName(),
                            productionLineAssignment.getAssignedTime(),
                            Objects.isNull(productionLineAssignment.getDeassignedTime()) ? "N/A" : productionLineAssignment.getDeassignedTime(),
                            startTime,
                            endTime);
                }
            }
        }

        return pulseCountHistoryByItemMap.values().stream().collect(Collectors.toList());

    }
}
