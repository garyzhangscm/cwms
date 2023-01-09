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

import com.garyzhangscm.cwms.inventory.model.ClientLocationUtilizationSnapshotBatch;
import com.garyzhangscm.cwms.inventory.model.LocationUtilizationSnapshot;
import com.garyzhangscm.cwms.inventory.service.ClientLocationUtilizationSnapshotBatchService;
import com.garyzhangscm.cwms.inventory.service.LocationUtilizationSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
public class ClientLocationUtilizationSnapshotController {
    private static final Logger logger = LoggerFactory.getLogger(ClientLocationUtilizationSnapshotController.class);
    @Autowired
    private ClientLocationUtilizationSnapshotBatchService clientLocationUtilizationSnapshotBatchService;

    @RequestMapping(value="/client-location-utilization-snapshots", method = RequestMethod.GET)
    public List<ClientLocationUtilizationSnapshotBatch> getClientLocationUtilizationSnapshotBatch(
            @RequestParam Long warehouseId,
            @RequestParam(name="clientName", required = false, defaultValue = "") String clientName,
            @RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
            @RequestParam(name="startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
            @RequestParam(name="endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endTime,
            @RequestParam(name="loadDetails", required = false, defaultValue = "") Boolean loadDetails) {
        return clientLocationUtilizationSnapshotBatchService.findAll(warehouseId,
                clientName, clientId, startTime, endTime, loadDetails);
    }



}
