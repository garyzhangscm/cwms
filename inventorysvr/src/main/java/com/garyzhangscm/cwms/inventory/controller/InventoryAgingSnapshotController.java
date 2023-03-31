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

import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.model.BillableEndpoint;
import com.garyzhangscm.cwms.inventory.model.InventoryAgingSnapshot;
import com.garyzhangscm.cwms.inventory.model.LocationUtilizationSnapshotBatch;
import com.garyzhangscm.cwms.inventory.service.InventoryAgingSnapshotService;
import com.garyzhangscm.cwms.inventory.service.LocationUtilizationSnapshotBatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class InventoryAgingSnapshotController {
    private static final Logger logger = LoggerFactory.getLogger(InventoryAgingSnapshotController.class);
    @Autowired
    private InventoryAgingSnapshotService inventoryAgingSnapshotService;

    @BillableEndpoint
    @RequestMapping(value="/inventory-aging-snapshots", method = RequestMethod.POST)
    public InventoryAgingSnapshot generateInventoryAgingSnapshot(@RequestParam Long warehouseId) {
        return inventoryAgingSnapshotService.generateInventoryAgingSnapshot(warehouseId);
    }
    @RequestMapping(value="/inventory-aging-snapshots", method = RequestMethod.GET)
    public List<InventoryAgingSnapshot> getInventoryAgingSnapshots(
            @RequestParam Long warehouseId,
            @RequestParam(name="status", required = false, defaultValue = "") String status,
            @RequestParam(name="number", required = false, defaultValue = "") String number,
            @RequestParam(name="startTime", required = false, defaultValue = "") LocalDateTime startTime,
            @RequestParam(name="endTime", required = false, defaultValue = "") LocalDateTime endTime) {
        return inventoryAgingSnapshotService.findAll(warehouseId, status,  number, startTime, endTime);
    }


    @RequestMapping(value="/inventory-aging-snapshots/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeInventoryAgingSnapshot(
            @RequestParam Long warehouseId, @PathVariable Long id) {
        inventoryAgingSnapshotService.remove(warehouseId, id);

        return ResponseBodyWrapper.success("inventory aging snapshot is removed");
    }



}
