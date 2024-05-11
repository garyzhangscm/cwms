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
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.PickService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class PickController {
    private static final Logger logger = LoggerFactory.getLogger(PickController.class);
    @Autowired
    PickService pickService;

    @RequestMapping(value="/picks", method = RequestMethod.GET)
    @ClientValidationEndpoint
    public List<Pick> findAllPicks(@RequestParam Long warehouseId,
                                   @RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
                                   @RequestParam(name="number", required = false, defaultValue = "") String number,
                                   @RequestParam(name="orderNumber", required = false, defaultValue = "") String orderNumber,
                                   @RequestParam(name="orderId", required = false, defaultValue = "") Long orderId,
                                   @RequestParam(name="shipmentId", required = false, defaultValue = "") Long shipmentId,
                                   @RequestParam(name="trailerAppointmentId", required = false, defaultValue = "") Long trailerAppointmentId,
                                   @RequestParam(name="waveId", required = false, defaultValue = "") Long waveId,
                                   @RequestParam(name="listId", required = false, defaultValue = "") Long listId,
                                   @RequestParam(name="cartonizationId", required = false, defaultValue = "") Long cartonizationId,
                                   @RequestParam(name="ids", required = false, defaultValue = "") String ids,
                                   @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
                                   @RequestParam(name="sourceLocationId", required = false, defaultValue = "") Long sourceLocationId,
                                   @RequestParam(name="destinationLocationId", required = false, defaultValue = "") Long destinationLocationId,
                                   @RequestParam(name="workOrderLineId", required = false, defaultValue = "") Long workOrderLineId,
                                   @RequestParam(name="workOrderLineIds", required = false, defaultValue = "") String workOrderLineIds,
                                   @RequestParam(name="shortAllocationId", required = false, defaultValue = "") Long shortAllocationId,
                                   @RequestParam(name="containerId", required = false, defaultValue = "") String containerId,
                                   @RequestParam(name="inventoryStatusId", required = false, defaultValue = "") Long inventoryStatusId,
                                   @RequestParam(name="shipmentNumber", required = false, defaultValue = "") String shipmentNumber,
                                   @RequestParam(name="workOrderNumber", required = false, defaultValue = "") String workOrderNumber,
                                   @RequestParam(name="waveNumber", required = false, defaultValue = "") String waveNumber,
                                   @RequestParam(name="cartonizationNumber", required = false, defaultValue = "") String cartonizationNumber,
                                   @RequestParam(name="itemNumber", required = false, defaultValue = "") String itemNumber,
                                   @RequestParam(name="sourceLocationName", required = false, defaultValue = "") String sourceLocationName,
                                   @RequestParam(name="destinationLocationName", required = false, defaultValue = "") String destinationLocationName,
                                   @RequestParam(name="openPickOnly", required = false, defaultValue = "false") Boolean openPickOnly,
                                   @RequestParam(name="includeCompletedPick", required = false, defaultValue = "false") Boolean includeCompletedPick,
                                   @RequestParam(name="includeCancelledPick", required = false, defaultValue = "false") Boolean includeCancelledPick,
            @RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails,
                                   ClientRestriction clientRestriction) {

        logger.debug("Start to find pick by: {}", listId);
        if (StringUtils.isNotBlank(containerId)) {
            return pickService.getPicksByContainer(warehouseId, clientId, containerId);
        }
        return pickService.findAll(warehouseId, clientId, number, orderId, orderNumber, shipmentId, waveId, listId,cartonizationId,  ids,
                itemId, sourceLocationId, destinationLocationId, workOrderLineId, workOrderLineIds,
                shortAllocationId, openPickOnly, inventoryStatusId,
                shipmentNumber, workOrderNumber, waveNumber, cartonizationNumber, itemNumber,
                sourceLocationName, destinationLocationName, trailerAppointmentId,
                includeCompletedPick, includeCancelledPick, clientRestriction,
                loadDetails);

    }
    @RequestMapping(value="/picks/{id}", method = RequestMethod.GET)
    public Pick findPick(@PathVariable Long id) {
        return pickService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/picks", method = RequestMethod.POST)
    public Pick addPick(@RequestBody Pick pick) {
        return pickService.save(pick);
    }

    @BillableEndpoint
    @RequestMapping(value="/picks/{id}", method = RequestMethod.PUT)
    public Pick changePick(@RequestBody Pick pick){
        return pickService.save(pick);
    }

    @BillableEndpoint
    @RequestMapping(value="/picks/{id}", method = RequestMethod.DELETE)
    public Pick cancelPick(@PathVariable Long id,
                           @RequestParam(name = "errorLocation", required = false, defaultValue = "false") Boolean errorLocation,
                           @RequestParam(name = "generateCycleCount", required = false, defaultValue = "false") Boolean generateCycleCount){
        return pickService.cancelPick(id, errorLocation, generateCycleCount);
    }

    @BillableEndpoint
    @RequestMapping(value="/picks/{id}/unpick", method = RequestMethod.POST)
    public Pick unpick(@PathVariable Long id,
                       @RequestParam Long unpickQuantity){
        return pickService.unpick(id, unpickQuantity);
    }


    @BillableEndpoint
    @RequestMapping(value="/picks", method = RequestMethod.DELETE)
    public List<Pick> cancelPicks(@RequestParam(name = "pick_ids") String pickIds,
                                  @RequestParam(name = "errorLocation", required = false, defaultValue = "false") Boolean errorLocation,
                                  @RequestParam(name = "generateCycleCount", required = false, defaultValue = "false") Boolean generateCycleCount) {
        return pickService.cancelPicks(pickIds, errorLocation, generateCycleCount);
    }


    @BillableEndpoint
    @RequestMapping(value="/picks/{id}/confirm", method = RequestMethod.POST)
    public Pick confirmPick(@PathVariable Long id,
                            @RequestParam(name="quantity", required = false, defaultValue = "") Long quantity,
                            @RequestParam(name="nextLocationId", required = false, defaultValue = "") Long nextLocationId,
                            @RequestParam(name="nextLocationName", required = false, defaultValue = "") String nextLocationName,
                            @RequestParam(name="pickToContainer", required = false, defaultValue = "false") boolean pickToContainer,
                            @RequestParam(name="containerId", required = false, defaultValue = "") String containerId,
                            @RequestParam(name="lpn", required = false, defaultValue = "") String lpn,
                            @RequestParam(name="destinationLpn", required = false, defaultValue = "") String destinationLpn) {
        logger.debug("======        Start to confirm pick   ========");
        logger.debug("=> quantity: {}", quantity);
        logger.debug("=> nextLocationId: {}", nextLocationId);
        logger.debug("=> nextLocationName: {}", nextLocationName);
        logger.debug("=> pickToContainer: {}", pickToContainer);
        logger.debug("=> containerId: {}", containerId);
        logger.debug("=> pick from LPN: {}", lpn);
        logger.debug("=> pick to LPN: {}", destinationLpn);
            return pickService.confirmPick(id, quantity, nextLocationId, nextLocationName,
                    pickToContainer, containerId, lpn, destinationLpn);
    }


    @BillableEndpoint
    @RequestMapping(value="/picks/generate-manual-pick-for-work-order", method = RequestMethod.POST)
    public List<Pick> generateManualPick(@RequestParam Long warehouseId,
                                        @RequestParam Long workOrderId,
                                        @RequestParam Long productionLineId,
                                        @RequestParam Long pickableQuantity,
                                        @RequestParam String lpn) {
        logger.debug("======        Start to processManualPick pick   ========");
        logger.debug("=> warehouseId: {}", warehouseId);
        logger.debug("=> workOrderId: {}", workOrderId);
        logger.debug("=> productionLineId: {}", productionLineId);
        logger.debug("=> pickableQuantity: {}", pickableQuantity);
        logger.debug("=> lpn: {}", lpn);
        return pickService.generateManualPickForWorkOrder(warehouseId, workOrderId, productionLineId, lpn, pickableQuantity);
    }


    @BillableEndpoint
    @RequestMapping(value="/picks/{id}/assign-user", method = RequestMethod.POST)
    public Pick assignToUser(@PathVariable Long id,
                                 Long warehouseId,
                                 Long userId) {
        return pickService.assignToUser(id, warehouseId, userId);
    }

    @BillableEndpoint
    @RequestMapping(value="/picks/{id}/unassign-user", method = RequestMethod.POST)
    public Pick unassignUser(@PathVariable Long id,
                             Long warehouseId) {
        return pickService.unassignUser(id, warehouseId);
    }


    @BillableEndpoint
    @RequestMapping(value="/picks/{id}/release", method = RequestMethod.POST)
    public Pick releasePick(@PathVariable Long id,
                                Long warehouseId) {
        return pickService.releasePick(id, warehouseId);
    }
/**
    @BillableEndpoint
    @RequestMapping(value="/picks/system-driven/next-pick", method = RequestMethod.POST)
    public GroupPick getNextPick(@RequestParam Long warehouseId,
                                 @RequestParam Long currentLocationId) {
        return pickService.getNextPick(warehouseId, currentLocationId);
    }
                                 **/

    @RequestMapping(method=RequestMethod.GET, value="/picks/quantity-in-order-pick")
    @ClientValidationEndpoint
    public Long getQuantityInOrderPick(Long warehouseId,
                                   @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId,
                                   Long itemId,
                                   Long inventoryStatusId,
                                   @RequestParam(name = "color", required = false, defaultValue = "") String color,
                                   @RequestParam(name = "productSize", required = false, defaultValue = "") String productSize,
                                   @RequestParam(name = "style", required = false, defaultValue = "") String style,
                                       @RequestParam(name = "inventoryAttribute1", required = false, defaultValue = "") String inventoryAttribute1,
                                       @RequestParam(name = "inventoryAttribute2", required = false, defaultValue = "") String inventoryAttribute2,
                                       @RequestParam(name = "inventoryAttribute3", required = false, defaultValue = "") String inventoryAttribute3,
                                       @RequestParam(name = "inventoryAttribute4", required = false, defaultValue = "") String inventoryAttribute4,
                                       @RequestParam(name = "inventoryAttribute5", required = false, defaultValue = "") String inventoryAttribute5,
                                   boolean exactMatch, ClientRestriction clientRestriction) {


        return pickService.getQuantityInOrderPick(
                warehouseId, clientId, itemId,
                inventoryStatusId, color, productSize, style,
                inventoryAttribute1, inventoryAttribute2, inventoryAttribute3,
                inventoryAttribute4,inventoryAttribute5,exactMatch,
                clientRestriction);

    }

    @BillableEndpoint
    @RequestMapping(value="/picks/pick-report", method = RequestMethod.POST)
    public ReportHistory generatePickReport(
            @RequestParam Long warehouseId,
            @RequestParam String ids,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        logger.debug("start print pick sheet for  pick with id: {}", ids);
        return pickService.generatePickReport(warehouseId, ids, locale);
    }


    @BillableEndpoint
    @RequestMapping(value="/pick/{id}/acknowledge", method = RequestMethod.POST)
    public Pick acknowledgePick(
            @PathVariable Long id,
            @RequestParam Long warehouseId) {

        return pickService.acknowledgePick(warehouseId, id);
    }
    @BillableEndpoint
    @RequestMapping(value="/pick/{id}/unacknowledge", method = RequestMethod.POST)
    public Pick unacknowledgePick(
            @PathVariable Long id,
            @RequestParam Long warehouseId) {

        return pickService.unacknowledgePick(warehouseId, id);
    }

    @ClientValidationEndpoint
    @RequestMapping(value="/picks/count", method = RequestMethod.GET)
    public Integer getPickCount(
            @RequestParam Long warehouseId,
            ClientRestriction clientRestriction) {

        return pickService.getPickCount(warehouseId, clientRestriction);
    }

    @ClientValidationEndpoint
    @RequestMapping(value="/picks/count-by-location-group", method = RequestMethod.GET)
    public Map<String, Integer[]> getPickCountByLocationGroup(
            @RequestParam Long warehouseId,
            ClientRestriction clientRestriction) {

        return pickService.getPickCountByLocationGroup(warehouseId, clientRestriction);
    }

    @BillableEndpoint
    @ClientValidationEndpoint
    @RequestMapping(value="/picks/orders/confirm-manual-pick", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> confirmManualPickForOrder(
            @RequestParam Long warehouseId,
            @RequestParam String orderNumber,
            @RequestParam(name = "clientId", defaultValue = "", required = false) Long clientId,
            @RequestParam String lpn,
            ClientRestriction clientRestriction) {

        return pickService.confirmManualPickForOrder(warehouseId, clientId, orderNumber, lpn, clientRestriction);
    }
}
