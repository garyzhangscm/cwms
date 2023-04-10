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

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.exception.UserOperationException;
import com.garyzhangscm.cwms.resources.service.InitTestDataService;
import com.garyzhangscm.cwms.resources.service.SystemConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test-data")
public class InitTestDataController {

    @Autowired
    InitTestDataService initTestDataService;
    @Autowired
    private SystemConfigurationService systemConfigurationService;


    @RequestMapping(method = RequestMethod.GET)
    public String[] getTestDataNames() {

        return initTestDataService.getTestDataNames();
    }
    /**
    @RequestMapping(value = "/init", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> init(
            @RequestParam Long companyId, @RequestParam String warehouseName) {

        if (systemConfigurationService.allowInitTestData(companyId, warehouseName)) {

            initTestDataService.init(companyId, warehouseName);
            return ResponseBodyWrapper.success("Init all");
        }
        throw UserOperationException.raiseException("Initiate Test Data is not allowed in this server");
    }
    @RequestMapping(value = "/init/{name}", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> init(@PathVariable String name,
                                            @RequestParam Long companyId,
                                            @RequestParam String warehouseName) {


        if (systemConfigurationService.allowInitTestData(companyId, warehouseName)) {

            initTestDataService.init(companyId, name, warehouseName);
            return ResponseBodyWrapper.success("Init " + name);
        }
        throw UserOperationException.raiseException("Initiate Test Data is not allowed in this server");

    }

    @RequestMapping(value = "/clear", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> clear(@RequestParam Long companyId,
                                             @RequestParam Long warehouseId) {

        if (systemConfigurationService.allowInitTestData(companyId, warehouseId)) {

            initTestDataService.clear(warehouseId);
            return ResponseBodyWrapper.success("data from warehouse " + warehouseId + " clear succeed!");
        }
        throw UserOperationException.raiseException("Initiate Test Data is not allowed in this server");
    }

**/

}
