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

import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.MissingInformationException;
import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.Location;
import com.garyzhangscm.cwms.common.model.Tractor;
import com.garyzhangscm.cwms.common.model.Trailer;
import com.garyzhangscm.cwms.common.service.TractorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class TractorController {
    @Autowired
    TractorService tractorService;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    @RequestMapping(value="/tractors", method = RequestMethod.GET)
    public List<Tractor> findAllTractors(@RequestParam(name="companyId", required = false, defaultValue = "")  Long companyId,
                                         @RequestParam(name="warehouseId", required = false, defaultValue = "")  Long warehouseId,
                                         @RequestParam(name="number", required = false, defaultValue = "") String number) {
        // company ID or warehouse id is required
        if (Objects.isNull(companyId) && Objects.isNull(warehouseId)) {

            throw MissingInformationException.raiseException("company information or warehouse id is required for item integration");
        }
        else if (Objects.isNull(companyId)) {
            // if company Id is empty, but we have company code,
            // then get the company id from the code
            companyId =
                    warehouseLayoutServiceRestemplateClient
                            .getWarehouseById(warehouseId).getCompanyId();
        }
        return tractorService.findAll(companyId, warehouseId, number);
    }

    @BillableEndpoint
    @RequestMapping(value="/tractors", method = RequestMethod.POST)
    public Tractor addTractor(@RequestBody Tractor tractor) {
        return tractorService.save(tractor);
    }


    @RequestMapping(value="/tractors/{id}", method = RequestMethod.GET)
    public Tractor findTractor(@PathVariable Long id) {
        return tractorService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/tractors/{id}", method = RequestMethod.PUT)
    public Tractor changeTractor(@RequestBody Tractor tractor){
        return tractorService.save(tractor);
    }


    @BillableEndpoint
    @RequestMapping(value="/tractors/{id}/checkin", method = RequestMethod.POST)
    public Tractor checkInTractor(@PathVariable Long id,
                                  @RequestBody Location dockLocation) {
        return tractorService.checkInTractor(id, dockLocation);
    }

    @BillableEndpoint
    @RequestMapping(value="/tractors/{id}/dispatch", method = RequestMethod.POST)
    public Tractor dispatchTractor(@PathVariable Long id) {
        return tractorService.dispatchTractor(id);
    }

}
