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

package com.garyzhangscm.cwms.common.controller;

import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.MissingInformationException;
import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.Location;
import com.garyzhangscm.cwms.common.model.Tractor;
import com.garyzhangscm.cwms.common.model.TractorSchedule;
import com.garyzhangscm.cwms.common.service.TractorScheduleService;
import com.garyzhangscm.cwms.common.service.TractorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RestController
public class TractorScheduleController {
    @Autowired
    TractorScheduleService tractorScheduleService;


    @RequestMapping(value="/tractor-schedules", method = RequestMethod.GET)
    public List<TractorSchedule> findAllTractors(@RequestParam Long warehouseId,
                                                 @RequestParam(name="tractorId", required = false, defaultValue = "") Long tractorId,
                                                 @RequestParam(name="tractorNumber", required = false, defaultValue = "") String tractorNumber,
                                         @RequestParam(name="startCheckInTime", required = false, defaultValue = "") LocalDateTime startCheckInTime,
                                         @RequestParam(name="endCheckInTime", required = false, defaultValue = "") LocalDateTime endCheckInTime,
                                         @RequestParam(name="startDispatchTime", required = false, defaultValue = "") LocalDateTime startDispatchTime,
                                         @RequestParam(name="endDispatchTime", required = false, defaultValue = "") LocalDateTime endDispatchTime) {

        return tractorScheduleService.findAll(warehouseId, tractorId, tractorNumber, startCheckInTime, endCheckInTime, startDispatchTime, endDispatchTime);
    }

    @BillableEndpoint
    @RequestMapping(value="/tractor-schedules", method = RequestMethod.POST)
    public TractorSchedule addTractorSchedule(@RequestBody TractorSchedule tractorSchedule,
                              @RequestParam Long warehouseId) {
        return tractorScheduleService.addTractorSchedule(warehouseId, tractorSchedule);
    }


    @RequestMapping(value="/tractor-schedules/{id}", method = RequestMethod.GET)
    public TractorSchedule findTractorSchedule(@PathVariable Long id) {
        return tractorScheduleService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/tractor-schedules/{id}", method = RequestMethod.PUT)
    public TractorSchedule changeTractorSchedule(@RequestBody TractorSchedule tractorSchedule){
        return tractorScheduleService.save(tractorSchedule);
    }




}
