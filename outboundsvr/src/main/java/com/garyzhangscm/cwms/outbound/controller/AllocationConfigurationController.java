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
import com.garyzhangscm.cwms.outbound.model.AllocationConfigurationPickableUnitOfMeasure;
import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.outbound.service.AllocationConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class AllocationConfigurationController {

    @Autowired
    private AllocationConfigurationService allocationConfigurationService;

    @RequestMapping(value="/allocation-configuration", method = RequestMethod.GET)
    public List<AllocationConfiguration> getAllocationConfiguration(
                                @RequestParam Long warehouseId,
                                @RequestParam(name = "sequence", required = false, defaultValue =  "") Integer sequence,
                                @RequestParam(name = "itemId", required = false, defaultValue =  "") Long itemId,
                                @RequestParam(name = "itemFamilyId", required = false, defaultValue =  "") Long itemFamilyId,
                                @RequestParam(name = "allocationConfigurationType", required = false, defaultValue =  "") String allocationConfigurationType,
                                @RequestParam(name = "locationId", required = false, defaultValue =  "") Long locationId,
                                @RequestParam(name = "locationGroupId", required = false, defaultValue =  "") Long locationGroupId,
                                @RequestParam(name = "locationGroupTypeId", required = false, defaultValue =  "") Long locationGroupTypeId,
                                @RequestParam(name = "allocationStrategy", required = false, defaultValue =  "") String allocationStrategy) {
        return allocationConfigurationService.findAll(warehouseId, sequence, itemId,
                itemFamilyId, allocationConfigurationType, locationId,
                locationGroupId, locationGroupTypeId, allocationStrategy);
    }


    @BillableEndpoint
    @RequestMapping(value="/allocation-configuration", method = RequestMethod.PUT)
    public AllocationConfiguration addAllocationConfiguration(@RequestBody AllocationConfiguration allocationConfiguration) {
        return allocationConfigurationService.addAllocationConfiguration(allocationConfiguration);
    }

    @BillableEndpoint
    @RequestMapping(value="/allocation-configuration/{id}", method = RequestMethod.POST)
    public AllocationConfiguration changeAllocationConfiguration(
            @PathVariable Long id,
            @RequestBody AllocationConfiguration allocationConfiguration) {
        return allocationConfigurationService.changeAllocationConfiguration(id, allocationConfiguration);
    }


    @BillableEndpoint
    @RequestMapping(value="/allocation-configuration/{id}/pickable-unit-of-measures", method = RequestMethod.POST)
    public AllocationConfiguration addPickableUnitOfMeasure(@PathVariable Long id,
                                                            @RequestBody AllocationConfigurationPickableUnitOfMeasure allocationConfigurationPickableUnitOfMeasure) {
        return allocationConfigurationService.addPickableUnitOfMeasure(id, allocationConfigurationPickableUnitOfMeasure);
    }

}
