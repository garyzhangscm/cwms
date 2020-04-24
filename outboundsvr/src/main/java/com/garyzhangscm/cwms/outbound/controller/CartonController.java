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

import com.garyzhangscm.cwms.outbound.model.Carton;
import com.garyzhangscm.cwms.outbound.model.Stop;
import com.garyzhangscm.cwms.outbound.service.CartonService;
import com.garyzhangscm.cwms.outbound.service.StopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CartonController {
    @Autowired
    CartonService cartonService;

    @RequestMapping(value="/cartons", method = RequestMethod.GET)
    public List<Carton> findAllCartons(@RequestParam Long warehouseId,
                                       @RequestParam(name = "name", required = false, defaultValue = "") String name,
                                       @RequestParam(name = "enabled", required = false, defaultValue = "") Boolean enabled) {
        return cartonService.findAll(warehouseId, name, enabled);
    }

    @RequestMapping(value="/cartons", method = RequestMethod.POST)
    public Carton addCarton(@RequestParam Long warehouseId,
                            @RequestBody Carton carton) {
        return cartonService.addCarton(warehouseId, carton);
    }


    @RequestMapping(value="/cartons/{id}", method = RequestMethod.GET)
    public Carton findCarton(@PathVariable Long id) {
        return cartonService.findById(id);
    }

    @RequestMapping(value="/cartons/{id}", method = RequestMethod.PUT)
    public Carton changeCarton(@RequestBody Carton carton){
        return cartonService.save(carton);
    }


}
