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

package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.model.SystemConfiguration;
import com.garyzhangscm.cwms.resources.service.SystemConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
public class SystemConfigurationController {

    @Autowired
    SystemConfigurationService systemConfigurationService;

    @RequestMapping(value="/system-configuration", method = RequestMethod.GET)
    public SystemConfiguration getSystemConfiguration(@RequestParam Long companyId,
                                                      @RequestParam Long warehouseId) {
        return systemConfigurationService.findByCompanyAndWarehouse(
                companyId, warehouseId);
    }


    @BillableEndpoint
    @RequestMapping(value="/system-configuration", method = RequestMethod.POST)
    public SystemConfiguration changeSystemConfiguration(
            @RequestBody SystemConfiguration systemConfiguration ) {
        return systemConfigurationService.saveOrUpdate(
                systemConfiguration);
    }

}
