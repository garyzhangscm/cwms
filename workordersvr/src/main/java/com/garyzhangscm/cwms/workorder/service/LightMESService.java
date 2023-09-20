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
import com.garyzhangscm.cwms.workorder.model.lightMES.LightMESConfiguration;
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

    private final static String REDIS_KEY_MACHINE_STATUS = "lightMES_machine_status";

    public List<Machine> getMachineList(Long warehouseId) {
        return lightMESRestemplateClient.getMachineList(warehouseId);

    }

    public LightStatus getSingleLightStatus(Long warehouseId, String sim) {
        return lightMESRestemplateClient.getSingleLightStatus(warehouseId, sim);

    }

    public List<Machine> getMachineStatusWithCache(Long warehouseId, String machineNo,
                                                   String type) throws JsonProcessingException {
        if (Strings.isNotBlank(machineNo)) {
            // for status of single machine, we will get the status without consider the cache
            return getMachineStatus(warehouseId, machineNo,
                    type);
        }
        else {

            logger.debug("start to get cached machine status");
            Object machineStatus = redisTemplate.opsForValue().get(REDIS_KEY_MACHINE_STATUS);
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
                List<Machine> machines = getMachineStatus(warehouseId, null, type);

                // save the result to the redis
                redisTemplate.opsForValue().set(REDIS_KEY_MACHINE_STATUS, machines, Duration.ofMinutes(3));

                if (Strings.isNotBlank(type)) {
                    machines = machines.stream().filter(
                            machine -> type.equalsIgnoreCase(machine.getProductionLineTypeName())
                    ).collect(Collectors.toList());
                }

                return machines;

            }
        }

    }
    public List<Machine> getMachineStatus(Long warehouseId, String machineNo,
                                          String type) {
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

                logger.debug(">> start to setup the machine's statictis data based on the production line's assignment");
                for (ProductionLineAssignment productionLineAssignment : productionLine.getProductionLineAssignments()) {
                    MachineStatistics machineStatistics = getMachineStatistics(
                            warehouseId,
                            productionLine, productionLineAssignment,
                            currentShift, producedQuantityMap
                    );
                    machine.addMachineStatistics(machineStatistics);
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

            setupPulseCountAndCycleTime(warehouseId, lightMESConfiguration, machine, currentShift);
        }
        return resultMachines;


    }

    private void setupPulseCountAndCycleTime(Long warehouseId,
                                             LightMESConfiguration lightMESConfiguration,
                                             Machine machine,
                                             Pair<ZonedDateTime, ZonedDateTime> currentShift) {
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
            ZonedDateTime endTime = ZonedDateTime.now();
            ZonedDateTime startTime = endTime.minusMinutes(pulseCountTimeWindow);

            int lastTimeWindowPulseCount = lightMESRestemplateClient.getSingleLightPulseByTimeRange(
                        warehouseId, startTime, endTime, machine.getSim()
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
            List<Machine> machines = getMachineStatus(warehouseId, null, null);

            // save the result to the redis
            redisTemplate.opsForValue().set(REDIS_KEY_MACHINE_STATUS, machines, Duration.ofMinutes(3));

        }
        catch (Exception ex) {
            // ignore the exception
            logger.debug("Ignore exception {} when refresh the light MES machine status in redis",
                    ex.getMessage());
        }

    }

}
