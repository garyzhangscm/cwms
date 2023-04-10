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
import com.garyzhangscm.cwms.outbound.model.BulkPickConfiguration;
import com.garyzhangscm.cwms.outbound.model.PickConfiguration;
import com.garyzhangscm.cwms.outbound.service.BulkPickConfigurationService;
import com.garyzhangscm.cwms.outbound.service.PickConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
public class PickConfigurationController {

    @Autowired
    private PickConfigurationService pickConfigurationService;

    @RequestMapping(value="/pick-configuration", method = RequestMethod.GET)
    public PickConfiguration getPickConfiguration(
                                @RequestParam Long warehouseId) {
        return pickConfigurationService.findByWarehouse(warehouseId);
    }


    @BillableEndpoint
    @RequestMapping(value="/pick-configuration", method = RequestMethod.PUT)
    public PickConfiguration addPickConfiguration(
            @RequestParam Long warehouseId,
            @RequestBody PickConfiguration pickConfiguration) {
        return pickConfigurationService.addPickConfiguration(pickConfiguration);
    }

    @BillableEndpoint
    @RequestMapping(value="/pick-configuration/{id}", method = RequestMethod.POST)
    public PickConfiguration changePickConfiguration(
            @RequestParam Long warehouseId,
            @PathVariable Long id,
            @RequestBody PickConfiguration pickConfiguration) {
        return pickConfigurationService.changePickConfiguration(id, pickConfiguration);
    }



}
