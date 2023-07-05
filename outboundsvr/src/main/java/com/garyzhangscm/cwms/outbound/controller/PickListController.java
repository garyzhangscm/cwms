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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.outbound.model.Pick;
import com.garyzhangscm.cwms.outbound.model.PickList;
import com.garyzhangscm.cwms.outbound.model.ReportHistory;
import com.garyzhangscm.cwms.outbound.service.PickListService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PickListController {

    private static final Logger logger = LoggerFactory.getLogger(PickController.class);

    @Autowired
    PickListService pickListService;

    @RequestMapping(value="/pick-lists", method = RequestMethod.GET)
    public List<PickList> findAllPickLists(@RequestParam Long warehouseId,
                                           @RequestParam(name="number", required = false, defaultValue = "") String number,
                                           @RequestParam(name="numberList", required = false, defaultValue = "") String numberList) {
        return pickListService.findAll(warehouseId, number, numberList);
    }


    @RequestMapping(value="/pick-lists/{id}", method = RequestMethod.GET)
    public PickList findPickList(@PathVariable Long id) {
        return pickListService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/pick-lists/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> cancelPickList(@PathVariable Long id){
        pickListService.cancelPickList(id);
        return ResponseBodyWrapper.success("pick list " + id + " is cancelled");
    }

    @BillableEndpoint
    @RequestMapping(value="/pick-lists/cancel-in-batch", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> cancelPickListInBatch(
            @RequestParam String ids,
            @RequestParam(name = "errorLocation", required = false, defaultValue = "false") Boolean errorLocation,
            @RequestParam(name = "generateCycleCount", required = false, defaultValue = "false") Boolean generateCycleCount){
        String[] idList = ids.split(",");
        for (String id : idList) {

            pickListService.cancelPickList(Long.parseLong(id));
        }
        return ResponseBodyWrapper.success("pick list " + ids + " are cancelled");
    }

    @BillableEndpoint
    @RequestMapping(value="/pick-lists/{id}/confirm", method = RequestMethod.POST)
    public PickList confirmPickList(@PathVariable Long id,
                            @RequestParam Long sourceLocationId,
                            @RequestParam(name="quantity", required = false, defaultValue = "") Long quantity,
                            @RequestParam(name="nextLocationId", required = false, defaultValue = "") Long nextLocationId,
                            @RequestParam(name="nextLocationName", required = false, defaultValue = "") String nextLocationName,
                            @RequestParam(name="pickToContainer", required = false, defaultValue = "false") boolean pickToContainer,
                            @RequestParam(name="containerId", required = false, defaultValue = "") String containerId,
                            @RequestParam(name="lpn", required = false, defaultValue = "") String lpn) {
        logger.debug("======        Start to confirm pick list  ========");
        logger.debug("=> quantity: {}", quantity);
        logger.debug("=> sourceLocationId: {}", sourceLocationId);
        logger.debug("=> nextLocationId: {}", nextLocationId);
        logger.debug("=> nextLocationName: {}", nextLocationName);
        logger.debug("=> pickToContainer: {}", pickToContainer);
        logger.debug("=> containerId: {}", containerId);
        logger.debug("=> pick from LPN: {}", lpn);
        return pickListService.confirmPickList(id, sourceLocationId, quantity, nextLocationId, nextLocationName,  pickToContainer, containerId, lpn);
    }

    @BillableEndpoint
    @RequestMapping(value="/pick-lists/{id}/pick-report", method = RequestMethod.POST)
    public ReportHistory generatePickListReport(
            @PathVariable Long id,
            @RequestParam Long warehouseId,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        logger.debug("start print pick sheet for pick list with id: {}", id);
        return pickListService.generatePickReportByPickList(warehouseId, id, locale);
    }
    @BillableEndpoint
    @RequestMapping(value="/pick-lists/pick-report/batch", method = RequestMethod.POST)
    public List<ReportHistory> generatePickListReportInBatch(
            @RequestParam String ids,
            @RequestParam Long warehouseId,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        return pickListService.generatePickListReportInBatch(warehouseId, ids, locale);
    }
}
