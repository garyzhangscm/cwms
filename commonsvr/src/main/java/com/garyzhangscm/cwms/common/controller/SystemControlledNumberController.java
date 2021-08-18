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

package com.garyzhangscm.cwms.common.controller;


import com.garyzhangscm.cwms.common.ResponseBodyWrapper;
import com.garyzhangscm.cwms.common.model.SystemControlledNumber;
import com.garyzhangscm.cwms.common.service.SystemControlledNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class SystemControlledNumberController {
    @Autowired
    SystemControlledNumberService systemControlledNumberService;

    @RequestMapping(value="/system-controlled-number/{variable}/next", method = RequestMethod.GET)
    public SystemControlledNumber getNextNumber(@RequestParam Long warehouseId,
                                                @PathVariable String variable) {
        return systemControlledNumberService.getNextNumber(warehouseId, variable);

    }


    @RequestMapping(value="/system-controlled-numbers", method = RequestMethod.GET)
    public List<SystemControlledNumber> getSystemControlledNumbers(@RequestParam Long warehouseId,
                                                                   @RequestParam(name = "variable", required = false, defaultValue = "") String variable) {
        return systemControlledNumberService.findAll(warehouseId, variable);
    }

    @RequestMapping(value="/system-controlled-numbers/{id}", method = RequestMethod.GET)
    public SystemControlledNumber getSystemControlledNumber(@PathVariable Long id) {
        return systemControlledNumberService.findById(id);

    }

    @RequestMapping(value="/system-controlled-numbers/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeSystemControlledNumber(@PathVariable Long id) {
        systemControlledNumberService.delete(id);
        return ResponseBodyWrapper.success("removed!");

    }


    @RequestMapping(value="/system-controlled-numbers", method = RequestMethod.PUT)
    public SystemControlledNumber addSystemControlledNumbers(@RequestBody SystemControlledNumber systemControlledNumber) {
        return systemControlledNumberService.addSystemControlledNumbers(systemControlledNumber);
    }

    @RequestMapping(value="/system-controlled-numbers/{id}", method = RequestMethod.POST)
    public SystemControlledNumber changeSystemControlledNumbers(@PathVariable Long id,
                                                             @RequestBody SystemControlledNumber systemControlledNumber) {
        return systemControlledNumberService.changeSystemControlledNumbers(systemControlledNumber);
    }


}
