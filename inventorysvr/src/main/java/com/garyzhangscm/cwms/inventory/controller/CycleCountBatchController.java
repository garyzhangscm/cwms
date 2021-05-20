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

import com.garyzhangscm.cwms.inventory.model.CycleCountBatch;
import com.garyzhangscm.cwms.inventory.model.CycleCountRequest;
import com.garyzhangscm.cwms.inventory.model.CycleCountRequestType;
import com.garyzhangscm.cwms.inventory.model.CycleCountResult;
import com.garyzhangscm.cwms.inventory.service.CycleCountBatchService;
import com.garyzhangscm.cwms.inventory.service.CycleCountRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class CycleCountBatchController {
    @Autowired
    CycleCountBatchService cycleCountBatchService;



    @RequestMapping(value = "/cycle-count-batches", method = RequestMethod.GET)
    public List<CycleCountBatch> getCycleCountBatches(
            @RequestParam Long warehouseId,
            @RequestParam(name = "batchId", required = false, defaultValue = "") String batchId) {
        return cycleCountBatchService.findAll(warehouseId, batchId);
    }


    @RequestMapping(value = "/cycle-count-batches/open-with-cycle-count", method = RequestMethod.GET)
    public List<CycleCountBatch> getCycleCountBatchesWithOpenCycleCount() {
        return cycleCountBatchService.getCycleCountBatchesWithOpenCycleCount();
    }
    @RequestMapping(value = "/cycle-count-batches/open-with-audit-count", method = RequestMethod.GET)
    public List<CycleCountBatch> getCycleCountBatchesWithOpenAuditCount() {
        return cycleCountBatchService.getCycleCountBatchesWithOpenAuditCount();
    }
    @RequestMapping(value = "/cycle-count-batches/open", method = RequestMethod.GET)
    public List<CycleCountBatch> getOpenCycleCountBatches() {
        return cycleCountBatchService.getOpenCycleCountBatches();
    }

}
