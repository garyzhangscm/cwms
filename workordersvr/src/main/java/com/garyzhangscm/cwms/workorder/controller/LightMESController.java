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

package com.garyzhangscm.cwms.workorder.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.workorder.model.BillableEndpoint;
import com.garyzhangscm.cwms.workorder.model.lightMES.LightMESConfiguration;
import com.garyzhangscm.cwms.workorder.model.lightMES.LightStatus;
import com.garyzhangscm.cwms.workorder.model.lightMES.Machine;
import com.garyzhangscm.cwms.workorder.model.lightMES.PulseCountHistoryByItem;
import com.garyzhangscm.cwms.workorder.service.LightMESConfigurationService;
import com.garyzhangscm.cwms.workorder.service.LightMESService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;


@RestController
public class LightMESController {

    @Autowired
    private LightMESService lightMESService;

    @RequestMapping(value="/light-mes/machine-list", method = RequestMethod.GET)
    public List<Machine> getMachineList(
                                @RequestParam Long warehouseId) {
        return lightMESService.getMachineList(warehouseId);
    }


    @RequestMapping(value="/light-mes/machine-status/current-shift", method = RequestMethod.GET)
    public List<Machine> getCurrentShiftMachineStatus(
            @RequestParam Long warehouseId,
            @RequestParam(name = "machineNo", required = false, defaultValue = "") String machineNo,
            @RequestParam(name = "type", required = false, defaultValue = "") String type,
            @RequestParam(name = "includeNonAvailableQuantity", required = false, defaultValue = "false") Boolean includeNonAvailableQuantity) throws JsonProcessingException {
        return lightMESService.getCurrentShiftMachineStatusWithCache(warehouseId, machineNo,  type, includeNonAvailableQuantity);
    }


    @RequestMapping(value="/light-mes/light-status/single", method = RequestMethod.GET)
    public LightStatus getSingleLightStatus(
            @RequestParam Long warehouseId,
            @RequestParam String sim) {
        return lightMESService.getSingleLightStatus(warehouseId, sim);
    }


    @RequestMapping(value="/light-mes/pulse-count-history", method = RequestMethod.GET)
    public List<PulseCountHistoryByItem> getPulseCountHistory(
            @RequestParam Long warehouseId,
            @RequestParam(name = "itemName", required = false, defaultValue = "") String itemName,
            @RequestParam  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
            @RequestParam  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime
            ) throws JsonProcessingException {
        return lightMESService.getPulseCountHistory(warehouseId, itemName,  startTime, endTime);
    }
}
