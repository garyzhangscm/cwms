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
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.model.lightMES.LightStatus;
import com.garyzhangscm.cwms.workorder.model.lightMES.Machine;
import com.garyzhangscm.cwms.workorder.model.lightMES.MachineStatistics;
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

    private final static String REDIS_KEY_MACHINE_STATUS = "lightMES_machine_status";

    public List<Machine> getMachineList(Long warehouseId) {
        return lightMESRestemplateClient.getMachineList(warehouseId);

    }

    public LightStatus getSingleLightStatus(Long warehouseId, String sim) {
        return lightMESRestemplateClient.getSingleLightStatus(warehouseId, sim);

    }

    public List<Machine> getMachineStatusWithCache(Long warehouseId, String machineNo) throws JsonProcessingException {
        if (Strings.isNotBlank(machineNo)) {
            // for status of single machine, we will get the status without consider the cache
            return getMachineStatus(warehouseId, machineNo);
        }
        else {

            logger.debug("start to get cached machine status");
            Object machineStatus = redisTemplate.opsForValue().get(REDIS_KEY_MACHINE_STATUS);
            if (Objects.nonNull(machineStatus)) {
                logger.debug("get machine status from cache:\n{}", machineStatus);
                String json = objectMapper.writeValueAsString(machineStatus);
                List<Machine> machines = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Machine.class));

                return machines;
            }
            else {
                logger.debug("machine status is not in the redis cache, let's get the real time data and save it to the cache");
                List<Machine> machines = getMachineStatus(warehouseId, null);

                // save the result to the redis
                redisTemplate.opsForValue().set(REDIS_KEY_MACHINE_STATUS, machines, Duration.ofMinutes(2));

                return machines;

            }
        }

    }
    public List<Machine> getMachineStatus(Long warehouseId, String machineNo) {
        logger.debug("start to get machine status, for single machine? {}",
                Strings.isBlank(machineNo) ? "N/A" : machineNo);

        List<Machine> machines = getMachineList(warehouseId);
        if (Strings.isNotBlank(machineNo)) {
            machines = machines.stream().filter(machine -> machineNo.equalsIgnoreCase(machine.getMachineNo())).collect(Collectors.toList());
        }

        // get the production lines and its active work order
        if (machines.isEmpty()) {
            return machines;
        }
        logger.debug("Get {} machines", machines.size());
        // machine from light MES should be configured to have the same machine number
        // as the production line setup in the system
        String productionLineNames = machines.stream().map(machine -> machine.getMachineNo()).collect(Collectors.joining(","));
        List<ProductionLine> productionLines = productionLineService.findAll(warehouseId, null, null,
                productionLineNames, false, true);
        logger.debug("Got {} production lines from the machine list", productionLines.size());

        Map<String, ProductionLine> productionLineMap = new HashMap<>();
        productionLines.forEach(productionLine -> productionLineMap.put(productionLine.getName(), productionLine));


        // setup the current state of the machine
        // 三色灯状态码：001-绿灯，010-黄灯，100-红灯，000-关灯
        List<String> simList  = machines.stream().map(machine -> machine.getSim()).collect(Collectors.toList());
        List<LightStatus> lightStatuses = lightMESRestemplateClient.getLightStatusInBatch(warehouseId, simList);
        logger.debug("Get {} light status", lightStatuses.size());
        Map<String, String> lightStatusMap = new HashMap<>();
        lightStatuses.forEach(
                lightStatus -> lightStatusMap.put(lightStatus.getSim(), lightStatus.getCurrentState())
        );

        Pair<ZonedDateTime, ZonedDateTime> currentShift = workOrderConfigurationService.getCurrentShift(warehouseId);
        Map<String, Long> producedQuantityMap = new HashMap<>();
        if (Objects.nonNull(currentShift)) {
            logger.debug("find shift: [{}, {}]",
                    currentShift.getFirst(), currentShift.getSecond());
            producedQuantityMap = workOrderProduceTransactionService.getProducedQuantityByTimeRange(
                    warehouseId, null, null,
                    currentShift.getFirst(), currentShift.getSecond(), true);
            logger.debug("get {} produced quantity information within the shift",
                    producedQuantityMap.size());
        }
        else {
            logger.debug("we can't find any shift information, we will ignore some of the statistics data for the machine(production line)");
        }


        Map<String, Long> finalProducedQuantityMap = producedQuantityMap;
        machines.forEach(
                machine -> {
                    ProductionLine productionLine = productionLineMap.get(machine.getMachineNo());
                    if (Objects.nonNull(productionLine) && !productionLine.getProductionLineAssignments().isEmpty()) {
                        productionLine.getProductionLineAssignments().forEach(
                                productionLineAssignment -> {
                                    MachineStatistics machineStatistics = getMachineStatistics(
                                            warehouseId,
                                            productionLine, productionLineAssignment,
                                            currentShift, finalProducedQuantityMap
                                    );
                                    machine.addMachineStatistics(machineStatistics);
                                }
                        );
                    }
                    machine.setCurrentState(
                            lightStatusMap.getOrDefault(machine.getSim(), "")
                    );
                }
        );
        Collections.sort(machines, Comparator.comparing(Machine::getMachineNo));

        // setup the machine's pulse count

        for (Machine machine : machines) {

            setupPulseCountAndCycleTime(warehouseId, machine, currentShift);
        }
        return machines;


    }

    private void setupPulseCountAndCycleTime(Long warehouseId,
                                             Machine machine,
                                             Pair<ZonedDateTime, ZonedDateTime> currentShift) {
        if (Strings.isNotBlank(machine.getSim())) {
            // last hour cycle time and pulse count
            ZonedDateTime endTime = ZonedDateTime.now();
            ZonedDateTime startTime = endTime.minusHours(1);
            int lastHourPulseCount = lightMESRestemplateClient.getSingleLightPulseByTimeRange(
                        warehouseId, startTime, endTime, machine.getSim()
            );
            machine.setLastHourPulseCount(lastHourPulseCount);
            if (lastHourPulseCount <= 0) {
                machine.setLastHourCycleTime(0);
            }
            else {
                machine.setLastHourCycleTime(60 * 60 / lastHourPulseCount);
            }
            // sleep 0.1 second as we are only allowed to call the getSingleLightPulseByTimeRange endpoint
            // 10 times per second
            /**
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }**/

            // current shift cycle time and pulse count
            if (Objects.nonNull(currentShift)) {
                // this shift cycle time and pulse count
                int shiftPulseCount = lightMESRestemplateClient.getSingleLightPulseByTimeRange(
                        warehouseId, currentShift.getFirst(), currentShift.getSecond(), machine.getSim()
                );
                machine.setShiftPulseCount(shiftPulseCount);
                if (shiftPulseCount <= 0) {
                    machine.setShiftCycleTime(0);
                }
                else {

                    logger.debug("production line {}, minutes {}, shift pulse {}",
                            machine.getMachineNo(),
                            ChronoUnit.SECONDS.between(currentShift.getFirst(), ZonedDateTime.now()),
                            shiftPulseCount);
                    machine.setShiftCycleTime((int)ChronoUnit.SECONDS.between(currentShift.getFirst(), ZonedDateTime.now()) / shiftPulseCount);
                }
                // sleep 0.1 second as we are only allowed to call the getSingleLightPulseByTimeRange endpoint
                // 10 times per second
                /**
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                 **/
            }

        }

    }

    private MachineStatistics getMachineStatistics(Long warehouseId,
                                                   ProductionLine productionLine,
                                                   ProductionLineAssignment productionLineAssignment,
                                                   Pair<ZonedDateTime, ZonedDateTime> currentShift,
                                                   Map<String, Long> producedQuantityMap) {
        MachineStatistics machineStatistics = new MachineStatistics(
                productionLineAssignment.getWorkOrder().getItem().getName(),
                productionLineAssignment.getWorkOrder().getNumber()
        );
        if (Objects.nonNull(currentShift)) {
            machineStatistics.setShiftStartTime(currentShift.getFirst());
            machineStatistics.setShiftEndTime(currentShift.getSecond());

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
                // see how many we are supposed to produce in this shift
                int hours = (int)ChronoUnit.HOURS.between(currentShift.getFirst(), currentShift.getSecond());
                logger.debug("there're {} hours difference between {} and {}",
                        hours, currentShift.getFirst(), currentShift.getSecond());
                Long expectedProducedQuantity = productionLineCapacityService.getExpectedProduceQuantity(
                        productionLineCapacity, hours
                );
                machineStatistics.setShiftEstimationQuantity(expectedProducedQuantity);

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
                machineStatistics.setProducedQuantity(producedQuantityMap.get(key));

                if (machineStatistics.getShiftEstimationQuantity() > 0) {

                    logger.debug("set the achivement rate to {}",
                            machineStatistics.getProducedQuantity() * 1.0 / machineStatistics.getShiftEstimationQuantity());
                    machineStatistics.setAchievementRate(
                            machineStatistics.getProducedQuantity()  * 1.0 / machineStatistics.getShiftEstimationQuantity()
                    );
                }
                else {
                    machineStatistics.setAchievementRate(0);
                }
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
            List<Machine> machines = getMachineStatus(warehouseId, null);

            // save the result to the redis
            redisTemplate.opsForValue().set(REDIS_KEY_MACHINE_STATUS, machines, Duration.ofMinutes(2));

        }
        catch (Exception ex) {
            // ignore the exception
            logger.debug("Ignore exception {} when refresh the light MES machine status in redis",
                    ex.getMessage());
        }

    }

}
