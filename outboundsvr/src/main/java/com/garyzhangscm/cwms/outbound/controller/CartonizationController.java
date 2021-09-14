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
import com.garyzhangscm.cwms.outbound.model.Cartonization;
import com.garyzhangscm.cwms.outbound.service.CartonizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CartonizationController {
    @Autowired
    CartonizationService cartonizationService;


    @RequestMapping(value="/cartonization", method = RequestMethod.GET)
    public List<Cartonization> findAllCartonizations(@RequestParam Long warehouseId,
                                                     @RequestParam(name = "number", required = false, defaultValue = "") String number,
                                                     @RequestParam(name = "status", required = false, defaultValue = "") String status,
                                                     @RequestParam(name = "cartonName", required = false, defaultValue = "") String cartonName) {
        return cartonizationService.findAll(warehouseId, number, status, cartonName);
    }

    @BillableEndpoint
    @RequestMapping(value="/cartonization", method = RequestMethod.POST)
    public Cartonization addCartonization(@RequestBody Cartonization cartonization) {
        return cartonizationService.save(cartonization);
    }


    @RequestMapping(value="/cartonization/{id}", method = RequestMethod.GET)
    public Cartonization findCartonization(@PathVariable Long id) {
        return cartonizationService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/cartonization/{id}", method = RequestMethod.PUT)
    public Cartonization changeCartonization(@RequestBody Cartonization cartonization){
        return cartonizationService.saveOrUpdate(cartonization);
    }


}
