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

import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.outbound.model.Pick;
import com.garyzhangscm.cwms.outbound.service.PickService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
public class PickController {
    private static final Logger logger = LoggerFactory.getLogger(PickController.class);
    @Autowired
    PickService pickService;

    @RequestMapping(value="/picks", method = RequestMethod.GET)
    public List<Pick> findAllPicks(@RequestParam Long warehouseId,
                                   @RequestParam(name="number", required = false, defaultValue = "") String number,
                                   @RequestParam(name="orderId", required = false, defaultValue = "") Long orderId,
                                   @RequestParam(name="shipmentId", required = false, defaultValue = "") Long shipmentId,
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
            @RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails) {

        logger.debug("Start to find pick by: {}", listId);
        if (StringUtils.isNotBlank(containerId)) {
            return pickService.getPicksByContainer(warehouseId, containerId);
        }
        return pickService.findAll(number, orderId, shipmentId, waveId, listId,cartonizationId,  ids,
                itemId, sourceLocationId, destinationLocationId, workOrderLineId, workOrderLineIds,
                shortAllocationId, loadDetails);
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
    public Pick cancelPick(@PathVariable Long id){
        return pickService.cancelPick(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/picks/{id}/unpick", method = RequestMethod.POST)
    public Pick unpick(@PathVariable Long id,
                       @RequestParam Long unpickQuantity){
        return pickService.unpick(id, unpickQuantity);
    }


    @BillableEndpoint
    @RequestMapping(value="/picks", method = RequestMethod.DELETE)
    public List<Pick> cancelPicks(@RequestParam(name = "pick_ids") String pickIds) {
        return pickService.cancelPicks(pickIds);
    }


    @BillableEndpoint
    @RequestMapping(value="/picks/{id}/confirm", method = RequestMethod.POST)
    public Pick confirmPick(@PathVariable Long id,
                            @RequestParam(name="quantity", required = false, defaultValue = "") Long quantity,
                            @RequestParam(name="nextLocationId", required = false, defaultValue = "") Long nextLocationId,
                            @RequestParam(name="nextLocationName", required = false, defaultValue = "") String nextLocationName,
                            @RequestParam(name="pickToContainer", required = false, defaultValue = "false") boolean pickToContainer,
                            @RequestParam(name="containerId", required = false, defaultValue = "") String containerId,
                            @RequestParam(name="lpn", required = false, defaultValue = "") String lpn) {
        logger.debug("======        Start to confirm pick   ========");
        logger.debug("=> quantity: {}", quantity);
        logger.debug("=> nextLocationId: {}", nextLocationId);
        logger.debug("=> nextLocationName: {}", nextLocationName);
        logger.debug("=> pickToContainer: {}", pickToContainer);
        logger.debug("=> containerId: {}", containerId);
        logger.debug("=> pick from LPN: {}", lpn);
            return pickService.confirmPick(id, quantity, nextLocationId, nextLocationName,  pickToContainer, containerId, lpn);
    }


}
