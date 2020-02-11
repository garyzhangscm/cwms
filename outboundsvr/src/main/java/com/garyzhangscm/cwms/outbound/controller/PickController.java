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

import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.OrderLine;
import com.garyzhangscm.cwms.outbound.model.Pick;
import com.garyzhangscm.cwms.outbound.model.Wave;
import com.garyzhangscm.cwms.outbound.service.PickService;
import com.garyzhangscm.cwms.outbound.service.WaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class PickController {
    @Autowired
    PickService pickService;

    @RequestMapping(value="/picks", method = RequestMethod.GET)
    public List<Pick> findAllPicks(@RequestParam(name="number", required = false, defaultValue = "") String number,
                                   @RequestParam(name="orderId", required = false, defaultValue = "") Long orderId,
                                   @RequestParam(name="itemId", required = false, defaultValue = "") Long itemId,
                                   @RequestParam(name="sourceLocationId", required = false, defaultValue = "") Long sourceLocationId,
                                   @RequestParam(name="destinationLocationId", required = false, defaultValue = "") Long destinationLocationId,
                                   @RequestParam(name="workOrderLineId", required = false, defaultValue = "") Long workOrderLineId,
                                   @RequestParam(name="workOrderLineIds", required = false, defaultValue = "") String workOrderLineIds) {
        return pickService.findAll(number, orderId,
                itemId, sourceLocationId, destinationLocationId, workOrderLineId, workOrderLineIds);
    }
    @RequestMapping(value="/picks/{id}", method = RequestMethod.GET)
    public Pick findPick(@PathVariable Long id) {
        return pickService.findById(id);
    }


    @RequestMapping(value="/picks", method = RequestMethod.POST)
    public Pick addPick(@RequestBody Pick pick) {
        return pickService.save(pick);
    }

    @RequestMapping(value="/picks/{id}", method = RequestMethod.PUT)
    public Pick changePick(@RequestBody Pick pick){
        return pickService.save(pick);
    }

    @RequestMapping(value="/picks/{id}", method = RequestMethod.DELETE)
    public Pick cancelPick(@PathVariable Long id){
        return pickService.cancelPick(id);
    }

    @RequestMapping(value="/picks/{id}/unpick", method = RequestMethod.POST)
    public Pick unpick(@PathVariable Long id,
                       @RequestParam Long unpickQuantity){
        return pickService.unpick(id, unpickQuantity);
    }


    @RequestMapping(value="/picks", method = RequestMethod.DELETE)
    public List<Pick> cancelPicks(@RequestParam(name = "pick_ids") String pickIds) {
        return pickService.cancelPicks(pickIds);
    }


    @RequestMapping(value="/picks/{id}/confirm", method = RequestMethod.POST)
    public Pick confirmPick(@PathVariable Long id,
                            @RequestParam(name="quantity", required = false, defaultValue = "") Long quantity,
                            @RequestParam(name="nextLocationId", required = false, defaultValue = "") Long nextLocationId) {
        try {
            return pickService.confirmPick(id, quantity, nextLocationId);
        }
        catch (IOException exception) {
            throw  new GenericException(10000, exception.getMessage());
        }
    }


}
