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
import com.garyzhangscm.cwms.resources.model.RF;
import com.garyzhangscm.cwms.resources.service.RFService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RFController {

    @Autowired
    RFService rfService;

    @RequestMapping(value="/rfs", method = RequestMethod.GET)
    public List<RF> findAllRFs(@RequestParam Long warehouseId,
                               @RequestParam(name="rfCode", required = false, defaultValue = "") String rfCode) {
        return rfService.findAll(warehouseId, rfCode);
    }

    @RequestMapping(value = "/validate/rf", method = RequestMethod.GET)
    public Boolean validateRFCode(Long warehouseId, String rfCode) {
        // return ApplicationInformation.getApplicationInformation();
        return rfService.validateRFCode(warehouseId, rfCode);
    }


    @RequestMapping(value="/rfs/{id}", method = RequestMethod.GET)
    public RF findRF(@PathVariable Long id) {
        return rfService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/rfs", method = RequestMethod.PUT)
    public RF addRF(@RequestBody RF rf) {
        return rfService.addRF(rf);
    }

    @BillableEndpoint
    @RequestMapping(value="/rfs/{id}", method = RequestMethod.DELETE)
    public Boolean removeRF(@PathVariable Long id) {
        rfService.delete(id);
        return true;
    }

    @BillableEndpoint
    @RequestMapping(value="/rfs/{id}/change-location", method = RequestMethod.POST)
    public RF changeLocation(@PathVariable Long id,
                             @RequestParam Long warehouseId,
                             @RequestParam Long locationId) {
        return rfService.changeLocation(id, locationId);
    }

}
