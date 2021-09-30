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
import com.garyzhangscm.cwms.outbound.model.CartonizationConfiguration;
import com.garyzhangscm.cwms.outbound.model.ShippingStageAreaConfiguration;
import com.garyzhangscm.cwms.outbound.service.CartonizationConfigurationService;
import com.garyzhangscm.cwms.outbound.service.ShippingStageAreaConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ShippingStageAreaConfigurationController {
    @Autowired
    ShippingStageAreaConfigurationService shippingStageAreaConfigurationService;

    @RequestMapping(value="/shipping-stage-area-configuration", method = RequestMethod.GET)
    public List<ShippingStageAreaConfiguration> findAllShippingStageAreaConfiguration(@RequestParam Long warehouseId) {
        return shippingStageAreaConfigurationService.findAll(warehouseId);
    }

    @BillableEndpoint
    @RequestMapping(value="/shipping-stage-area-configuration", method = RequestMethod.PUT)
    public ShippingStageAreaConfiguration addShippingStageAreaConfiguration(
            @RequestBody ShippingStageAreaConfiguration shippingStageAreaConfiguration) {
        return shippingStageAreaConfigurationService.addShippingStageAreaConfiguration(shippingStageAreaConfiguration);
    }


    @RequestMapping(value="/shipping-stage-area-configuration/{id}", method = RequestMethod.GET)
    public ShippingStageAreaConfiguration findShippingStageAreaConfiguration(@PathVariable Long id) {
        return shippingStageAreaConfigurationService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/shipping-stage-area-configuration/{id}", method = RequestMethod.POST)
    public ShippingStageAreaConfiguration changeShippingStageAreaConfiguration(
            @RequestBody ShippingStageAreaConfiguration shippingStageAreaConfiguration){
        return shippingStageAreaConfigurationService.changeShippingStageAreaConfiguration(shippingStageAreaConfiguration);
    }


}
