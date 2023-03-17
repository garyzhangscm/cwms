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
import com.garyzhangscm.cwms.workorder.model.Mould;
import com.garyzhangscm.cwms.workorder.model.SiloConfiguration;
import com.garyzhangscm.cwms.workorder.service.MouldService;
import com.garyzhangscm.cwms.workorder.service.SiloConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SiloConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(SiloConfigurationController.class);
    @Autowired
    private SiloConfigurationService siloConfigurationService;

    @RequestMapping(value="/silo-configuration", method = RequestMethod.GET)
    public SiloConfiguration findSiloConfiguration(
            @RequestParam Long warehouseId) {
        return siloConfigurationService.findByWarehouseId(warehouseId);
    }

    @BillableEndpoint
    @RequestMapping(value="/silo-configuration", method = RequestMethod.PUT)
    public SiloConfiguration addSiloConfiguration(
            @RequestParam Long warehouseId,
            @RequestBody SiloConfiguration siloConfiguration) {
        return siloConfigurationService.addSiloConfiguration(siloConfiguration);
    }

    @BillableEndpoint
    @RequestMapping(value="/silo-configuration/{id}", method = RequestMethod.POST)
    public SiloConfiguration changeSiloConfiguration(
            @RequestParam Long warehouseId,
            @RequestBody SiloConfiguration siloConfiguration) {
        return siloConfigurationService.changeSiloConfiguration(warehouseId, siloConfiguration);
    }

}
