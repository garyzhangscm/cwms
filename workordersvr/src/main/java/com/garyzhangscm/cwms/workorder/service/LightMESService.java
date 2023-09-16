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

import com.garyzhangscm.cwms.workorder.clients.LightMESRestemplateClient;
import com.garyzhangscm.cwms.workorder.model.ProductionLine;
import com.garyzhangscm.cwms.workorder.model.ProductionLineAssignment;
import com.garyzhangscm.cwms.workorder.model.ProductionLineCapacity;
import com.garyzhangscm.cwms.workorder.model.lightMES.LightStatus;
import com.garyzhangscm.cwms.workorder.model.lightMES.Machine;
import com.garyzhangscm.cwms.workorder.model.lightMES.MachineStatistics;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public List<Machine> getMachineList(Long warehouseId) {
        return lightMESRestemplateClient.getMachineList(warehouseId);

    }

    public LightStatus getSingleLightStatus(Long warehouseId, String sim) {
        return lightMESRestemplateClient.getSingleLightStatus(warehouseId, sim);

    }

    public List<Machine> getMachineStatus(Long warehouseId, String machineNo) {
        List<Machine> machines = getMachineList(warehouseId);
        if (Strings.isNotBlank(machineNo)) {
            machines = machines.stream().filter(machine -> machineNo.equalsIgnoreCase(machine.getMachineNo())).collect(Collectors.toList());
        }

        // get the production lines and its active work order
        if (machines.isEmpty()) {
            return machines;
        }

        // machine from light MES should be configured to have the same machine number
        // as the production line setup in the system
        String productionLineNames = machines.stream().map(machine -> machine.getMachineNo()).collect(Collectors.joining(","));
        List<ProductionLine> productionLines = productionLineService.findAll(warehouseId, null, null,
                productionLineNames, false, true);
        Map<String, ProductionLine> productionLineMap = new HashMap<>();
        productionLines.forEach(productionLine -> productionLineMap.put(productionLine.getName(), productionLine));


        // setup the current state of the machine
        // 三色灯状态码：001-绿灯，010-黄灯，100-红灯，000-关灯
        List<String> simList  = machines.stream().map(machine -> machine.getSim()).collect(Collectors.toList());
        List<LightStatus> lightStatuses = lightMESRestemplateClient.getLightStatusInBatch(warehouseId, simList);
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
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

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

                    machine.setShiftCycleTime((int)ChronoUnit.MINUTES.between(currentShift.getFirst(), currentShift.getSecond()) / lastHourPulseCount);
                }
                // sleep 0.1 second as we are only allowed to call the getSingleLightPulseByTimeRange endpoint
                // 10 times per second
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
            String key = productionLine.getId() + "-" + productionLineAssignment.getWorkOrder().getId();
            if (producedQuantityMap.containsKey(key)) {
                machineStatistics.setProducedQuantity(producedQuantityMap.get(key));
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

                    logger.debug("set the achivement rate to {}",
                            machineStatistics.getProducedQuantity() * 1.0 / expectedProducedQuantity);
                    machineStatistics.setAchievementRate(
                            machineStatistics.getProducedQuantity()  * 1.0 / expectedProducedQuantity
                    );

                }
                else {

                    logger.debug("NO capacity setup for production line {} / {}, item {}",
                            productionLine.getId(),
                            productionLine.getName(),
                            productionLineAssignment.getWorkOrder().getItemId());
                }
            }
        }
        return machineStatistics;

    }
}
