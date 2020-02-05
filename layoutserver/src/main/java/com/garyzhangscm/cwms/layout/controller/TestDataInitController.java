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

package com.garyzhangscm.cwms.layout.controller;

import com.garyzhangscm.cwms.layout.ResponseBodyWrapper;
import com.garyzhangscm.cwms.layout.service.TestDataInitService;
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
    public ResponseBodyWrapper<String> init(@RequestParam String warehouseName) {
        testDataInitService.init(warehouseName);
        return ResponseBodyWrapper.success("data init succeed!");
    }

    @RequestMapping(value = "/init/{name}", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> init(@PathVariable String name,
                                            @RequestParam String warehouseName) {
        testDataInitService.init(name, warehouseName);
        return ResponseBodyWrapper.success("data " + name + " init succeed!");
    }
}
