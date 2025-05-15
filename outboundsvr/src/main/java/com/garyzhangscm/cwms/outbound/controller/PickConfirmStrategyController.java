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


import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.AllocationConfiguration;
import com.garyzhangscm.cwms.outbound.model.AllocationConfigurationPickableUnitOfMeasure;
import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.outbound.model.PickConfirmStrategy;
import com.garyzhangscm.cwms.outbound.service.AllocationConfigurationService;
import com.garyzhangscm.cwms.outbound.service.PickConfirmStrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class PickConfirmStrategyController {

    @Autowired
    private PickConfirmStrategyService pickConfirmStrategyService;

    @RequestMapping(value="/pick-confirm-strategy", method = RequestMethod.GET)
    public List<PickConfirmStrategy> getPickConfirmStrategy(
                                @RequestParam Long warehouseId,
                                @RequestParam(name = "sequence", required = false, defaultValue =  "") Integer sequence,
                                @RequestParam(name = "itemId", required = false, defaultValue =  "") Long itemId,
                                @RequestParam(name = "itemFamilyId", required = false, defaultValue =  "") Long itemFamilyId,
                                @RequestParam(name = "locationId", required = false, defaultValue =  "") Long locationId,
                                @RequestParam(name = "locationGroupId", required = false, defaultValue =  "") Long locationGroupId,
                                @RequestParam(name = "locationGroupTypeId", required = false, defaultValue =  "") Long locationGroupTypeId,
                                @RequestParam(name = "loadDetails", required = false, defaultValue =  "false") Boolean loadDetails) {

        return pickConfirmStrategyService.findAll(warehouseId, sequence, itemId,
                itemFamilyId, locationId,
                locationGroupId, locationGroupTypeId, loadDetails);
    }


    @BillableEndpoint
    @RequestMapping(value="/pick-confirm-strategy", method = RequestMethod.PUT)
    public PickConfirmStrategy addPickConfirmStrategy(@RequestBody PickConfirmStrategy pickConfirmStrategy) {
        return pickConfirmStrategyService.addPickConfirmStrategy(pickConfirmStrategy);
    }

    @RequestMapping(value="/pick-confirm-strategy/{id}", method = RequestMethod.GET)
    public PickConfirmStrategy getPickConfirmStrategy(
                                @PathVariable Long id,
                                @RequestParam Long warehouseId) {
        return pickConfirmStrategyService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/pick-confirm-strategy/{id}", method = RequestMethod.POST)
    public PickConfirmStrategy changePickConfirmStrategy(
            @PathVariable Long id,
            @RequestBody PickConfirmStrategy pickConfirmStrategy) {
        return pickConfirmStrategyService.changePickConfirmStrategy(id, pickConfirmStrategy);
    }

    @BillableEndpoint
    @RequestMapping(value="/pick-confirm-strategy/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removePickConfirmStrategy(
            @PathVariable Long id,
            @RequestParam Long warehouseId) {
        pickConfirmStrategyService.removePickConfirmStrategy(id, warehouseId);
        return ResponseBodyWrapper.success("pick confirm strategy " + id + " removed!");
    }
}
