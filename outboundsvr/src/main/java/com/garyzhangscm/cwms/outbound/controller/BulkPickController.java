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
import com.garyzhangscm.cwms.outbound.model.BulkPick;
import com.garyzhangscm.cwms.outbound.model.Pick;
import com.garyzhangscm.cwms.outbound.model.ReportHistory;
import com.garyzhangscm.cwms.outbound.service.BulkPickService;
import com.garyzhangscm.cwms.outbound.service.PickService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BulkPickController {
    private static final Logger logger = LoggerFactory.getLogger(BulkPickController.class);
    @Autowired
    private BulkPickService bulkPickService;

    @RequestMapping(value="/bulk-picks", method = RequestMethod.GET)
    public List<BulkPick> findAllBulkPicks(@RequestParam Long warehouseId,
                                           @RequestParam(name="number", required = false, defaultValue = "") String number,
                                           @RequestParam(name="numberList", required = false, defaultValue = "") String numberList,
                                           @RequestParam(name="pickType", required = false, defaultValue = "") String pickType,
                                           @RequestParam(name="waveId", required = false, defaultValue = "") Long waveId,
                                           @RequestParam(name="waveNumber", required = false, defaultValue = "") String waveNumber,
                                           @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
                                           @RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
                                           @RequestParam(name="itemNumber", required = false, defaultValue = "") String itemNumber,
                                           @RequestParam(name="sourceLocationId", required = false, defaultValue = "") Long sourceLocationId,
                                           @RequestParam(name="sourceLocationName", required = false, defaultValue = "") String sourceLocationName,
                                           @RequestParam(name="inventoryStatusId", required = false, defaultValue = "") Long inventoryStatusId,
                                           @RequestParam(name="openPickOnly", required = false, defaultValue = "false") Boolean openPickOnly,
                                           @RequestParam(name="color", required = false, defaultValue = "") String color,
                                           @RequestParam(name="style", required = false, defaultValue = "") String style,
                                           @RequestParam(name="productSize", required = false, defaultValue = "") String productSize,
                                           @RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails ) {


        return bulkPickService.findAll(warehouseId,
                pickType, number, numberList, waveId, waveNumber, itemId,
                clientId, itemNumber, sourceLocationId, sourceLocationName, inventoryStatusId, openPickOnly,
                color, style, productSize, loadDetails);

    }
    @RequestMapping(value="/bulk-picks/{id}", method = RequestMethod.GET)
    public BulkPick findPick(@PathVariable Long id) {
        return bulkPickService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/bulk-picks/{id}", method = RequestMethod.POST)
    public BulkPick changePick(@RequestBody BulkPick bulkPick){
        return bulkPickService.changePick(bulkPick);
    }

    @BillableEndpoint
    @RequestMapping(value="/bulk-picks/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> cancelPick(@PathVariable Long id,
                                          @RequestParam(name = "errorLocation", required = false, defaultValue = "false") Boolean errorLocation,
                                          @RequestParam(name = "generateCycleCount", required = false, defaultValue = "false") Boolean generateCycleCount){
        bulkPickService.cancelBulkPick(id, errorLocation, generateCycleCount);
        return ResponseBodyWrapper.success("bulk pick " + id + " is cancelled");
    }

    @BillableEndpoint
    @RequestMapping(value="/bulk-picks/cancel-in-batch", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> cancelBulkPickInBatch(
            @RequestParam String ids,
                                                  @RequestParam(name = "errorLocation", required = false, defaultValue = "false") Boolean errorLocation,
                                                  @RequestParam(name = "generateCycleCount", required = false, defaultValue = "false") Boolean generateCycleCount){
        String[] idList = ids.split(",");
        for (String id : idList) {

            bulkPickService.cancelBulkPick(Long.parseLong(id), errorLocation, generateCycleCount);
        }
        return ResponseBodyWrapper.success("bulk pick " + ids + " are cancelled");
    }


    @BillableEndpoint
    @RequestMapping(value="/bulk-picks/{id}/confirm", method = RequestMethod.POST)
    public BulkPick confirmBulkPick(@PathVariable Long id,
                            @RequestParam(name="nextLocationId", required = false, defaultValue = "") Long nextLocationId,
                            @RequestParam(name="nextLocationName", required = false, defaultValue = "") String nextLocationName,
                            @RequestParam(name="lpn", required = false, defaultValue = "") String lpn) {
        logger.debug("======        Start to confirm pick   ========");
        logger.debug("=> nextLocationId: {}", nextLocationId);
        logger.debug("=> nextLocationName: {}", nextLocationName);
        logger.debug("=> pick from LPN: {}", lpn);
            return bulkPickService.confirmBulkPick(id, nextLocationId, nextLocationName, lpn);
    }

    @BillableEndpoint
    @RequestMapping(value="/bulk-picks/{id}/assign-user", method = RequestMethod.POST)
    public BulkPick assignToUser(@PathVariable Long id,
                                 Long warehouseId,
                                 Long userId) {
        return bulkPickService.assignToUser(id, warehouseId, userId);
    }

    @BillableEndpoint
    @RequestMapping(value="/bulk-picks/{id}/unassign-user", method = RequestMethod.POST)
    public BulkPick unassignUser(@PathVariable Long id,
                                 Long warehouseId) {
        return bulkPickService.unassignUser(id, warehouseId);
    }

    @BillableEndpoint
    @RequestMapping(value="/bulk-picks/{id}/release", method = RequestMethod.POST)
    public BulkPick releasePick(@PathVariable Long id,
                                 Long warehouseId) {
        return bulkPickService.releasePick(id, warehouseId);
    }

    @BillableEndpoint
    @RequestMapping(value="/bulk-picks/{id}/pick-report", method = RequestMethod.POST)
    public ReportHistory generateBulkPickReport(
            @PathVariable Long id,
            @RequestParam Long warehouseId,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        logger.debug("start print pick sheet for bulk pick with id: {}", id);
        return bulkPickService.generatePickReportByBulkPick(warehouseId, id, locale);
    }


    @BillableEndpoint
    @RequestMapping(value="/bulk-picks/pick-report/batch", method = RequestMethod.POST)
    public List<ReportHistory> generateBulkPickReportInBatch(
            @RequestParam String ids,
            @RequestParam Long warehouseId,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        return bulkPickService.generatePickReportByBulkPickInBatch(warehouseId, ids, locale);
    }
}
