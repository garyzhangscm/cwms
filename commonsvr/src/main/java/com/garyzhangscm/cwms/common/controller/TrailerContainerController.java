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
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.service.TrailerContainerService;
import com.garyzhangscm.cwms.common.service.TrailerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class TrailerContainerController {
    @Autowired
    TrailerContainerService trailerContainerService;

    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @RequestMapping(value="/trailer-containers", method = RequestMethod.GET)
    public List<TrailerContainer> findAllTrailerContainers(@RequestParam(name="companyId", required = false, defaultValue = "")  Long companyId,
                                                   @RequestParam(name="warehouseId", required = false, defaultValue = "")  Long warehouseId,
                                                   @RequestParam(name = "number", required = false, defaultValue = "") String number) {

        // company ID or warehouse id is required
        if (Objects.isNull(companyId) && Objects.isNull(warehouseId)) {

            throw MissingInformationException.raiseException("company information or warehouse id is required for finding container");
        }
        else if (Objects.isNull(companyId)) {
            // if company Id is empty, but we have company code,
            // then get the company id from the code
            companyId =
                    warehouseLayoutServiceRestemplateClient
                            .getWarehouseById(warehouseId).getCompanyId();
        }

        return trailerContainerService.findAll(companyId, warehouseId, number);
    }

    @BillableEndpoint
    @RequestMapping(value="/trailers-containers", method = RequestMethod.POST)
    public TrailerContainer addTrailerContainer(@RequestBody TrailerContainer trailerContainer) {
        return trailerContainerService.save(trailerContainer);
    }


    @RequestMapping(value="/trailer-containers/{id}", method = RequestMethod.GET)
    public TrailerContainer findTrailerContainer(@PathVariable Long id) {
        return trailerContainerService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/trailer-containers/{id}", method = RequestMethod.PUT)
    public TrailerContainer changeTrailerContainer(@RequestBody TrailerContainer trailerContainer){
        return trailerContainerService.save(trailerContainer);
    }

    @BillableEndpoint
    @RequestMapping(value="/trailer-containers/{id}", method = RequestMethod.DELETE)
    public TrailerContainer removeTrailerContainer(@PathVariable Long id) {
        return trailerContainerService.findById(id);
    }



}
