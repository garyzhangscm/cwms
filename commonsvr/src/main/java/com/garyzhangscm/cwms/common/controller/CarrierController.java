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

import com.garyzhangscm.cwms.common.exception.GenericException;
import com.garyzhangscm.cwms.common.model.Carrier;
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.service.CarrierService;
import com.garyzhangscm.cwms.common.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CarrierController {
    @Autowired
    CarrierService carrierService;

    @RequestMapping(value="/carriers", method = RequestMethod.GET)
    public List<Carrier> findAllCarriers(@RequestParam(name = "name", required = false, defaultValue = "") String name) {
        return carrierService.findAll(name);
    }

    @RequestMapping(value="/carriers/{id}", method = RequestMethod.GET)
    public Carrier findCarrier(@PathVariable Long id) {
        return carrierService.findById(id);
    }

    @RequestMapping(value="/carriers", method = RequestMethod.POST)
    public Carrier addCarrier(@RequestBody Carrier carrier) {
        return carrierService.save(carrier);
    }

    @RequestMapping(value="/carriers/{id}", method = RequestMethod.PUT)
    public Carrier changeCarrier(@PathVariable Long id, @RequestBody Carrier carrier) {
        if (carrier.getId() != null && carrier.getId() != id) {
            throw new GenericException(10000, "ID in the URL doesn't match with the data passed in the request");
        }
        return carrierService.save(carrier);
    }

    @RequestMapping(method=RequestMethod.DELETE, value="/carriers")
    public void deleteCarriers(@RequestParam(name = "carrier_ids", required = false, defaultValue = "") String carrierIds) {
        carrierService.delete(carrierIds);
    }
}
