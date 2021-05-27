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
import com.garyzhangscm.cwms.inventory.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import com.garyzhangscm.cwms.inventory.service.InventorySnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class InventorySnapshotController {
    private static final Logger logger = LoggerFactory.getLogger(InventorySnapshotController.class);
    @Autowired
    InventorySnapshotService inventorySnapshotService;

    @RequestMapping(value="/inventory_snapshot", method = RequestMethod.GET)
    public List<InventorySnapshot> findAllInventorySnapshot(
            @RequestParam Long warehouseId,
            @RequestParam(name="status", required = false, defaultValue = "") String status,
            @RequestParam(name="batchNumber", required = false, defaultValue = "") String batchNumber) {
        return inventorySnapshotService.findAll(warehouseId, status, batchNumber);
    }


    @RequestMapping(value="/inventory_snapshot/{batchNumber}/details", method = RequestMethod.GET)
    public List<InventorySnapshotDetail> findAllInventorySnapshotDetails(
            @RequestParam Long warehouseId, @PathVariable String batchNumber) {
        return inventorySnapshotService.findAllInventorySnapshotDetails(warehouseId, batchNumber);
    }


    @RequestMapping(value="/inventory_snapshot", method = RequestMethod.POST)
    public InventorySnapshot generateInventorySnapshot(
            @RequestParam Long warehouseId) {
        return inventorySnapshotService.generateInventorySnapshot(warehouseId);
    }

}
