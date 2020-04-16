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

package com.garyzhangscm.cwms.outbound.controller;


import com.garyzhangscm.cwms.outbound.model.ShortAllocation;

import com.garyzhangscm.cwms.outbound.service.ShortAllocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ShortAllocationController {
    @Autowired
    ShortAllocationService shortAllocationService;

    @RequestMapping(value="/shortAllocations", method = RequestMethod.GET)
    public List<ShortAllocation> findAllShortAllocations(
            @RequestParam Long warehouseId,
            @RequestParam(name="workOrderLineId", required = false, defaultValue = "") Long workOrderLineId,
            @RequestParam(name="workOrderLineIds", required = false, defaultValue = "") String workOrderLineIds,
            @RequestParam(name="itemNumber", required = false, defaultValue = "") String itemNumber,
            @RequestParam(name="orderId", required = false, defaultValue = "") Long orderId,
            @RequestParam(name="workOrderId", required = false, defaultValue = "") Long workOrderId,
            @RequestParam(name="shipmentId", required = false, defaultValue = "") Long shipmentId,
            @RequestParam(name="waveId", required = false, defaultValue = "") Long waveId) {
        return shortAllocationService.findAll(warehouseId, workOrderLineId, workOrderLineIds,
                itemNumber, orderId, workOrderId, shipmentId, waveId);
    }

    @RequestMapping(value="/shortAllocations/{id}", method = RequestMethod.GET)
    public ShortAllocation findShortAllocation(@PathVariable Long id) {
        return shortAllocationService.findById(id);
    }


    @RequestMapping(value="/shortAllocations", method = RequestMethod.POST)
    public ShortAllocation addShortAllocation(@RequestBody ShortAllocation shortAllocation) {
        return shortAllocationService.save(shortAllocation);
    }

    @RequestMapping(value="/shortAllocations/{id}", method = RequestMethod.PUT)
    public ShortAllocation changeShortAllocation(@RequestBody ShortAllocation shortAllocation){
        return shortAllocationService.save(shortAllocation);
    }


    @RequestMapping(value="/shortAllocations", method = RequestMethod.DELETE)
    public List<ShortAllocation> cancelShortAllocations(@RequestParam(name = "shortAllocation_ids") String shortAllocationIds) {
        return shortAllocationService.cancelShortAllocations(shortAllocationIds);
    }

}
