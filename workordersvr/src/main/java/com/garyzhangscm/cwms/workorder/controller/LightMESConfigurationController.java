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

package com.garyzhangscm.cwms.workorder.controller;


import com.garyzhangscm.cwms.workorder.model.BillableEndpoint;
import com.garyzhangscm.cwms.workorder.model.lightMES.LightMESConfiguration;
import com.garyzhangscm.cwms.workorder.service.LightMESConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
public class LightMESConfigurationController {

    @Autowired
    private LightMESConfigurationService lightMESConfigurationService;

    @RequestMapping(value="/light-mes-configuration", method = RequestMethod.GET)
    public LightMESConfiguration getLightMESConfiguration(
                                @RequestParam Long warehouseId) {
        return lightMESConfigurationService.findByWarehouse(warehouseId);
    }


    @BillableEndpoint
    @RequestMapping(value="/light-mes-configuration", method = RequestMethod.PUT)
    public LightMESConfiguration addLightMESConfiguration(
            @RequestParam Long warehouseId,
            @RequestBody LightMESConfiguration lightMESConfiguration) {
        return lightMESConfigurationService.addLightMESConfiguration(lightMESConfiguration);
    }

    @BillableEndpoint
    @RequestMapping(value="/light-mes-configuration/{id}", method = RequestMethod.POST)
    public LightMESConfiguration changeLightMESConfiguration(
            @RequestParam Long warehouseId,
            @PathVariable Long id,
            @RequestBody LightMESConfiguration lightMESConfiguration) {
        return lightMESConfigurationService.changeLightMESConfiguration(id, lightMESConfiguration);
    }



}
