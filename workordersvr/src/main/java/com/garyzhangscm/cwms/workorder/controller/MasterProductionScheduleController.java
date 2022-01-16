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


import com.garyzhangscm.cwms.workorder.model.BillableEndpoint;
import com.garyzhangscm.cwms.workorder.model.MasterProductionSchedule;
import com.garyzhangscm.cwms.workorder.model.Mould;
import com.garyzhangscm.cwms.workorder.service.MasterProductionScheduleService;
import com.garyzhangscm.cwms.workorder.service.MouldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RestController
public class MasterProductionScheduleController {
    @Autowired
    MasterProductionScheduleService masterProductionScheduleService;

    @RequestMapping(value="/master-production-schedules", method = RequestMethod.GET)
    public List<MasterProductionSchedule> findAllMasterProductionSchedules(
            @RequestParam Long warehouseId,
            @RequestParam(name="number", required = false, defaultValue = "") String number,
            @RequestParam(name="description", required = false, defaultValue = "") String description,
            @RequestParam(name="productionLineId", required = false, defaultValue = "") Long productionLineId,
            @RequestParam(name="productionLineIds", required = false, defaultValue = "") String productionLineIds,
            @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
            @RequestParam(name="beginDateTime", required = false, defaultValue = "") String beginDateTime,
            @RequestParam(name="endDateTime", required = false, defaultValue = "") String endDateTime) {
        return masterProductionScheduleService.findAll(warehouseId, number, description,
                beginDateTime, endDateTime, productionLineId, productionLineIds, itemName);
    }



    @BillableEndpoint
    @RequestMapping(value="/master-production-schedules", method = RequestMethod.PUT)
    public MasterProductionSchedule addMasterProductionSchedules(@RequestBody MasterProductionSchedule masterProductionSchedule) {
        return masterProductionScheduleService.addMasterProductionSchedule(masterProductionSchedule);
    }


    @RequestMapping(value="/master-production-schedules/{id}", method = RequestMethod.GET)
    public MasterProductionSchedule findMasterProductionSchedule(@PathVariable Long id) {

        return masterProductionScheduleService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/master-production-schedules/{id}", method = RequestMethod.POST)
    public MasterProductionSchedule changeMasterProductionSchedule(@PathVariable Long id,
                                                                   @RequestBody MasterProductionSchedule masterProductionSchedule){
        return masterProductionScheduleService.changeMasterProductionSchedule(id, masterProductionSchedule);
    }

    @BillableEndpoint
    @RequestMapping(value="/master-production-schedule/{id}", method = RequestMethod.DELETE)
    public void removeMasterProductionSchedule(@PathVariable Long id) {
        masterProductionScheduleService.delete(id);
    }

    @RequestMapping(value="/master-production-schedules/available-date", method = RequestMethod.GET)
    public Collection<LocalDateTime> getAvailableDate(
            @RequestParam Long warehouseId,
            @RequestParam Long productionLineId,
            @RequestParam String beginDateTime,
            @RequestParam String endDateTime
            ) {
        return masterProductionScheduleService.getAvailableDate(warehouseId, productionLineId, beginDateTime, endDateTime);
    }

    @RequestMapping(value="/master-production-schedules/existing-mps", method = RequestMethod.GET)
    public Collection<MasterProductionSchedule> getExistingMPSs(
            @RequestParam Long warehouseId,
            @RequestParam Long productionLineId,
            @RequestParam String beginDateTime,
            @RequestParam String endDateTime
    ) {
        return masterProductionScheduleService.getExistingMPSs(warehouseId, productionLineId, beginDateTime, endDateTime);
    }

}
