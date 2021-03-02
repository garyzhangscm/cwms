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

package com.garyzhangscm.cwms.inbound.controller;


import com.garyzhangscm.cwms.inbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inbound.service.TestDataInitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test-data")
public class TestDataInitController {

    @Autowired
    TestDataInitService testDataInitService;
    @RequestMapping(method = RequestMethod.GET)
    public String[] getTestDataNames() {
        return testDataInitService.getTestDataNames();
    }

    @RequestMapping(value = "/init", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> init(@RequestParam Long companyId,
                                            @RequestParam String warehouseName) {
        testDataInitService.init(companyId, warehouseName);
        return ResponseBodyWrapper.success("data init succeed!");
    }

    @RequestMapping(value = "/init/{name}", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> init(@RequestParam Long companyId,
                                            @PathVariable String name,
                                            @RequestParam String warehouseName) {
        testDataInitService.init(companyId, name, warehouseName);
        return ResponseBodyWrapper.success("data " + name + " init succeed!");
    }
    @RequestMapping(value = "/clear", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> clear(@RequestParam Long warehouseId) {

        testDataInitService.clear( warehouseId);
        return ResponseBodyWrapper.success("data from warehouse " + warehouseId + " clear succeed!");
    }
}
