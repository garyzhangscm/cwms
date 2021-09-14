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

import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.CarrierServiceLevel;
import com.garyzhangscm.cwms.common.service.CarrierServiceLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CarrierServiceLevelController {
    @Autowired
    CarrierServiceLevelService carrierServiceLevelService;

    @RequestMapping(value="/carrier-service-levels", method = RequestMethod.GET)
    public List<CarrierServiceLevel> findAllCarrierServiceLevels() {
        return carrierServiceLevelService.findAll();
    }

    @RequestMapping(value="/carrier-service-levels/{id}", method = RequestMethod.GET)
    public CarrierServiceLevel findCarrierServiceLevel(@PathVariable Long id) {
        return carrierServiceLevelService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/carrier-service-levels", method = RequestMethod.POST)
    public CarrierServiceLevel addCarrierServiceLevel(@RequestBody CarrierServiceLevel carrierServiceLevel) {
        return carrierServiceLevelService.save(carrierServiceLevel);
    }

    @BillableEndpoint
    @RequestMapping(value="/carrier-service-levels/{id}", method = RequestMethod.PUT)
    public CarrierServiceLevel changeCarrierServiceLevel(@PathVariable Long id, @RequestBody CarrierServiceLevel carrierServiceLevel) {
        if (carrierServiceLevel.getId() != null && carrierServiceLevel.getId() != id) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; carrierServiceLevel.getId(): " + carrierServiceLevel.getId());
        }
        return carrierServiceLevelService.save(carrierServiceLevel);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/carrier-service-levels")
    public void deleteCarrierServiceLevels(@RequestParam(name = "carrierServiceLevelIds", required = false, defaultValue = "") String carrierServiceLevelIds) {
        carrierServiceLevelService.delete(carrierServiceLevelIds);
    }
}
