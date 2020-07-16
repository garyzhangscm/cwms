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


import com.garyzhangscm.cwms.outbound.model.AllocationConfiguration;
import com.garyzhangscm.cwms.outbound.model.PickableUnitOfMeasure;
import com.garyzhangscm.cwms.outbound.service.AllocationConfigurationService;
import com.garyzhangscm.cwms.outbound.service.PickableUnitOfMeasureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class PickableUnitOfMeasureController {

    @Autowired
    private PickableUnitOfMeasureService pickableUnitOfMeasureService;

    @RequestMapping(value="/pickable-unit-of-measures", method = RequestMethod.GET)
    public List<PickableUnitOfMeasure> getPickableUnitOfMeasureService(
            @RequestParam Long warehouseId,
            @RequestParam(name = "allocationConfigurationId", required =  false, defaultValue = "") Long allocationConfigurationId
            ) {
        return pickableUnitOfMeasureService.findAll(warehouseId, allocationConfigurationId);
    }


    @RequestMapping(value="/pickable-unit-of-measures", method = RequestMethod.POST)
    public PickableUnitOfMeasure addPickableUnitOfMeasure(@RequestBody PickableUnitOfMeasure pickableUnitOfMeasure) {
        return pickableUnitOfMeasureService.save(pickableUnitOfMeasure);
    }


}
