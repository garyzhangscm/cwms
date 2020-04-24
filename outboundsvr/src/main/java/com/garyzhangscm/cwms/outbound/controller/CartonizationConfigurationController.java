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


import com.garyzhangscm.cwms.outbound.model.CartonizationConfiguration;
import com.garyzhangscm.cwms.outbound.service.CartonizationConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CartonizationConfigurationController {
    @Autowired
    CartonizationConfigurationService cartonizationConfigurationService;

    @RequestMapping(value="/cartonization-configuration", method = RequestMethod.GET)
    public List<CartonizationConfiguration> findAllCartonizationConfigurations(@RequestParam Long warehouseId,
                                                                               @RequestParam(name = "clientIds", required = false, defaultValue = "") String clientIds,
                                                                               @RequestParam(name = "pickType", required = false, defaultValue = "") String pickType,
                                                                               @RequestParam(name = "enabled", required = false, defaultValue = "") Boolean enabled,
                                                                               @RequestParam(name = "sequence", required = false, defaultValue = "") Integer sequence) {
        return cartonizationConfigurationService.findAll(warehouseId,clientIds, pickType, enabled, sequence);
    }

    @RequestMapping(value="/cartonization-configuration", method = RequestMethod.POST)
    public CartonizationConfiguration addCartonizationConfiguration(@RequestBody CartonizationConfiguration cartonizationConfiguration) {
        return cartonizationConfigurationService.save(cartonizationConfiguration);
    }


    @RequestMapping(value="/cartonization-configuration/{id}", method = RequestMethod.GET)
    public CartonizationConfiguration findCartonizationConfiguration(@PathVariable Long id) {
        return cartonizationConfigurationService.findById(id);
    }

    @RequestMapping(value="/cartonization-configuration/{id}", method = RequestMethod.PUT)
    public CartonizationConfiguration changeCartonizationConfiguration(@RequestBody CartonizationConfiguration cartonizationConfiguration){
        return cartonizationConfigurationService.save(cartonizationConfiguration);
    }


}
