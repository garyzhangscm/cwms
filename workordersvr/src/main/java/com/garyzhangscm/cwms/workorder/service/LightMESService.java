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
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.ProductionLine;
import com.garyzhangscm.cwms.workorder.model.lightMES.LightMESConfiguration;
import com.garyzhangscm.cwms.workorder.model.lightMES.LightStatus;
import com.garyzhangscm.cwms.workorder.model.lightMES.Machine;
import com.garyzhangscm.cwms.workorder.model.lightMES.MachineStatistics;
import com.garyzhangscm.cwms.workorder.repository.LightMESConfigurationRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class LightMESService {
    private static final Logger logger = LoggerFactory.getLogger(LightMESService.class);

    @Autowired
    private LightMESRestemplateClient lightMESRestemplateClient;

    @Autowired
    private ProductionLineService productionLineService;

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


        machines.forEach(
                machine -> {
                    ProductionLine productionLine = productionLineMap.get(machine.getMachineNo());
                    if (Objects.nonNull(productionLine) && !productionLine.getProductionLineAssignments().isEmpty()) {
                        productionLine.getProductionLineAssignments().forEach(
                                productionLineAssignment -> {
                                    MachineStatistics machineStatistics = new MachineStatistics(
                                            productionLineAssignment.getWorkOrder().getItem().getName(),
                                            productionLineAssignment.getWorkOrder().getNumber()
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
        return machines;


    }
}
