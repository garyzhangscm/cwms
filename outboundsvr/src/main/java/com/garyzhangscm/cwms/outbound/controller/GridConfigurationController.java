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
import com.garyzhangscm.cwms.outbound.model.GridConfiguration;

import com.garyzhangscm.cwms.outbound.service.GridConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GridConfigurationController {
    @Autowired
    GridConfigurationService gridConfigurationService;

    @RequestMapping(value="/grid-configuration", method = RequestMethod.GET)
    public List<GridConfiguration> findAllGridConfiguration(@RequestParam Long warehouseId,
                                                            @RequestParam(name = "locationGroupId", required = false, defaultValue = "") Long locationGroupId) {
        return gridConfigurationService.findAll(warehouseId, locationGroupId);
    }

    @BillableEndpoint
    @RequestMapping(value="/grid-configuration", method = RequestMethod.POST)
    public GridConfiguration addGridConfiguration(@RequestParam Long warehouseId,
                            @RequestBody GridConfiguration gridConfiguration) {
        return gridConfigurationService.addGridConfiguration(warehouseId, gridConfiguration);
    }


    @RequestMapping(value="/grid-configuration/{id}", method = RequestMethod.GET)
    public GridConfiguration findGridConfiguration(@PathVariable Long id) {
        return gridConfigurationService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/grid-configuration/{id}", method = RequestMethod.PUT)
    public GridConfiguration changeGridConfiguration(@RequestBody GridConfiguration gridConfiguration){
        return gridConfigurationService.save(gridConfiguration);
    }


}
