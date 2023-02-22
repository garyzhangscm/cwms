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
import com.garyzhangscm.cwms.common.service.TrailerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class TrailerController {
    @Autowired
    TrailerService trailerService;

    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @RequestMapping(value="/trailers", method = RequestMethod.GET)
    public List<Trailer> findAllTrailers(@RequestParam(name="companyId", required = false, defaultValue = "")  Long companyId,
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

        return trailerService.findAll(companyId, warehouseId, number, null);
    }

    @BillableEndpoint
    @RequestMapping(value="/trailers", method = RequestMethod.POST)
    public Trailer addTrailer(@RequestBody Trailer trailerContainer) {
        return trailerService.save(trailerContainer);
    }


    @RequestMapping(value="/trailers/{id}", method = RequestMethod.GET)
    public Trailer findTrailer(@PathVariable Long id) {
        return trailerService.findById(id);
    }

    @RequestMapping(value="/trailers/open-for-tractor", method = RequestMethod.GET)
    public List<Trailer> findTrailersOpenForTractor(@RequestParam Long warehouseId,
                                                    @RequestParam Long companyId) {
        return trailerService.findTrailersOpenForTractor(companyId, warehouseId);
    }

    @BillableEndpoint
    @RequestMapping(value="/trailers/{id}", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "OutboundService_Trailer", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_TrailerAppointment", allEntries = true),
            }
    )
    public Trailer changeTrailer(@RequestBody Trailer trailerContainer){
        return trailerService.save(trailerContainer);
    }


    @RequestMapping(value="/trailers/{id}/add-appointment", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "OutboundService_Trailer", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_TrailerAppointment", allEntries = true),
            }
    )
    public TrailerAppointment addTrailerAppointment(@PathVariable Long id,
                                                    @RequestBody TrailerAppointment trailerAppointment){
        return trailerService.addTrailerAppointment(id, trailerAppointment);
    }

    @RequestMapping(value="/trailers/{id}/current-appointment", method = RequestMethod.GET)
    public TrailerAppointment getTrailerCurrentAppointment(@PathVariable Long id){
        return trailerService.getTrailerCurrentAppointment(id);
    }

    @RequestMapping(value="/trailers/appointments/{id}", method = RequestMethod.GET)
    public TrailerAppointment getTrailerAppointmentById(@PathVariable Long id){
        return trailerService.getTrailerAppointmentById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/trailers/{id}", method = RequestMethod.DELETE)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "OutboundService_Trailer", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_TrailerAppointment", allEntries = true),
            }
    )
    public void removeTrailer(@PathVariable Long id) {
        trailerService.delete(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/trailers/{trailerId}/appointments/{trailerAppointmentId}/cancel", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "OutboundService_Trailer", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_TrailerAppointment", allEntries = true),
            }
    )
    public TrailerAppointment cancelTrailerAppointment(@PathVariable Long trailerId,
                                                       @PathVariable Long trailerAppointmentId) {
        return  trailerService.cancelTrailerAppointment(trailerId, trailerAppointmentId);
    }

    @BillableEndpoint
    @RequestMapping(value="/trailers/{trailerId}/appointments/{trailerAppointmentId}/complete", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "OutboundService_Trailer", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_TrailerAppointment", allEntries = true),
            }
    )
    public TrailerAppointment completeTrailerAppointment(@PathVariable Long trailerId,
                                                         @PathVariable Long trailerAppointmentId) {
        return  trailerService.completeTrailerAppointment(trailerId, trailerAppointmentId);
    }



}
