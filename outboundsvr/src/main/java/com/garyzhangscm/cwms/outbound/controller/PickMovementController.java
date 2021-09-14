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
import com.garyzhangscm.cwms.outbound.service.PickMovementService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



@RestController
public class PickMovementController {
    private static final Logger logger = LoggerFactory.getLogger(PickMovementController.class);
    @Autowired
    PickMovementService pickMovementService;

    @BillableEndpoint
    @RequestMapping(value="/pick-movement/refresh", method = RequestMethod.POST)
    public void refreshPickMovement( @RequestParam Long pickId,
                                     @RequestParam Long destinationLocationId,
                                     @RequestParam Long quantity) {
        pickMovementService.refreshPickMovement(pickId, destinationLocationId, quantity);
    }



}
