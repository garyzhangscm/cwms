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

import com.garyzhangscm.cwms.inventory.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.model.CycleCountRequest;
import com.garyzhangscm.cwms.inventory.model.CycleCountRequestType;
import com.garyzhangscm.cwms.inventory.model.CycleCountResult;
import com.garyzhangscm.cwms.inventory.service.CycleCountRequestService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class CycleCountRequestController {
    @Autowired
    CycleCountRequestService cycleCountRequestService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;


    @RequestMapping(value = "/cycle-count-requests", method = RequestMethod.POST)
    public List<CycleCountRequest> generateCycleCountRequests(
            @RequestParam Long warehouseId,
            @RequestParam(name = "batchId", required = false, defaultValue = "") String batchId,
            @RequestParam String cycleCountRequestType,
            @RequestParam String beginValue,
            @RequestParam String endValue,
            @RequestParam Boolean includeEmptyLocation) {

        if (StringUtils.isBlank(batchId)) {
            batchId = commonServiceRestemplateClient.getNextCycleCountBatchId();
        }
        return cycleCountRequestService.generateCycleCountRequest(batchId, CycleCountRequestType.valueOf(cycleCountRequestType),
                warehouseId, beginValue, endValue, includeEmptyLocation);
    }


    @RequestMapping(value = "/cycle-count-request/{id}", method = RequestMethod.GET)
    public CycleCountRequest getCycleCountRequestsByBatch(@PathVariable Long id){
        return cycleCountRequestService.findById(id);
    }
    @RequestMapping(value = "/cycle-count-request/batch/{batchId}", method = RequestMethod.GET)
    public List<CycleCountRequest> getCycleCountRequestsByBatch(@PathVariable String batchId){
        return cycleCountRequestService.findByBatchId(batchId);
    }


    @RequestMapping(value = "/cycle-count-request/batch/{batchId}/open", method = RequestMethod.GET)
    public List<CycleCountRequest> getOpenCycleCountRequests(@PathVariable String batchId){
        return cycleCountRequestService.getOpenCycleCountRequests(batchId);
    }

    @RequestMapping(value = "/cycle-count-request/batch/{batchId}/cancelled", method = RequestMethod.GET)
    public List<CycleCountRequest> getCancelledCycleCountRequests(@PathVariable String batchId){
        return cycleCountRequestService.getCancelledCycleCountRequests(batchId);
    }

    @RequestMapping(value = "/cycle-count-request/confirm", method = RequestMethod.POST)
    public List<CycleCountResult> confirmCycleCountRequests(@RequestParam("cycleCountRequestIds") String cycleCountRequestIds){
        return cycleCountRequestService.confirmCycleCountRequests(cycleCountRequestIds);
    }


    @RequestMapping(value = "/cycle-count-request/{cycleCountRequestId}/confirm", method = RequestMethod.POST)
    public List<CycleCountResult> saveCycleCountResults(@PathVariable String cycleCountRequestId,
                                                        @RequestBody List<CycleCountResult> cycleCountResults){
        return cycleCountRequestService.saveCycleCountResults(cycleCountRequestId, cycleCountResults);
    }

    @RequestMapping(value = "/cycle-count-request/cancel", method = RequestMethod.POST)
    public List<CycleCountRequest> cancelCycleCountRequests(@RequestParam("cycleCountRequestIds") String cycleCountRequestIds){
        return cycleCountRequestService.cancelCycleCountRequests(cycleCountRequestIds);
    }

    @RequestMapping(value = "/cycle-count-request/reopen", method = RequestMethod.POST)
    public List<CycleCountRequest> reopenCycleCountRequests(@RequestParam("cycleCountRequestIds") String cycleCountRequestIds){
        return cycleCountRequestService.reopenCancelledCycleCountRequests(cycleCountRequestIds);
    }

    @RequestMapping(value = "/cycle-count-request/{cycleCountRequestId}/inventory-summary", method = RequestMethod.GET)
    public List<CycleCountResult> getInventorySummariesForCount(@PathVariable String cycleCountRequestId){
        return cycleCountRequestService.getInventorySummariesForCount(cycleCountRequestId);
    }


    @RequestMapping(value = "/cycle-count-request/inventory-summary", method = RequestMethod.GET)
    public List<CycleCountResult> getInventorySummariesForCounts(@RequestParam("cycle_count_request_ids") String cycleCountRequestIds){
        return cycleCountRequestService.getInventorySummariesForCount(cycleCountRequestIds);
    }

}
