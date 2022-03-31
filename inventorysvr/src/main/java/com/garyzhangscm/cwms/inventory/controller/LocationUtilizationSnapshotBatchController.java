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

package com.garyzhangscm.cwms.inventory.controller;

import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.LocationUtilizationSnapshotBatchService;
import com.garyzhangscm.cwms.inventory.service.LocationUtilizationSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class LocationUtilizationSnapshotBatchController {
    private static final Logger logger = LoggerFactory.getLogger(LocationUtilizationSnapshotBatchController.class);
    @Autowired
    private LocationUtilizationSnapshotBatchService locationUtilizationSnapshotBatchService;

    @BillableEndpoint
    @RequestMapping(value="/location-utilization-snapshot-batches", method = RequestMethod.POST)
    public LocationUtilizationSnapshotBatch generateLocationUtilizationSnapshotBatch(@RequestParam Long warehouseId) {
        return locationUtilizationSnapshotBatchService.generateLocationUtilizationSnapshotBatch(warehouseId);
    }
    @RequestMapping(value="/location-utilization-snapshot-batches", method = RequestMethod.GET)
    public List<LocationUtilizationSnapshotBatch> getLocationUtilizationSnapshotBatches(
            @RequestParam Long warehouseId,
            @RequestParam(name="status", required = false, defaultValue = "") String status,
            @RequestParam(name="number", required = false, defaultValue = "") String number,
            @RequestParam(name="startTime", required = false, defaultValue = "") LocalDateTime startTime,
            @RequestParam(name="endTime", required = false, defaultValue = "") LocalDateTime endTime) {
        return locationUtilizationSnapshotBatchService.findAll(warehouseId, status,  number, startTime, endTime);
    }



}
