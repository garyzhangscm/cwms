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

import com.garyzhangscm.cwms.inventory.model.BillableEndpoint;
import com.garyzhangscm.cwms.inventory.model.InventorySnapshot;
import com.garyzhangscm.cwms.inventory.model.LocationUtilizationSnapshot;
import com.garyzhangscm.cwms.inventory.model.LocationUtilizationSnapshotBatch;
import com.garyzhangscm.cwms.inventory.service.LocationUtilizationSnapshotBatchService;
import com.garyzhangscm.cwms.inventory.service.LocationUtilizationSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
public class LocationUtilizationSnapshotController {
    private static final Logger logger = LoggerFactory.getLogger(LocationUtilizationSnapshotController.class);
    @Autowired
    private LocationUtilizationSnapshotService locationUtilizationSnapshotService;


    @RequestMapping(value="/location-utilization-snapshots", method = RequestMethod.GET)
    public List<LocationUtilizationSnapshot> getLocationUtilizationSnapshot(
            @RequestParam Long warehouseId,
            @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
            @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
            @RequestParam(name="clientName", required = false, defaultValue = "") String clientName,
            @RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
            @RequestParam(name="startTime", required = false, defaultValue = "") ZonedDateTime startTime,
            @RequestParam(name="endTime", required = false, defaultValue = "") ZonedDateTime endTime,
            @RequestParam(name="loadDetails", required = false, defaultValue = "") Boolean loadDetails) {
        return locationUtilizationSnapshotService.findAll(warehouseId,
                itemName, itemId, clientName, clientId, startTime, endTime, loadDetails);
    }





}
