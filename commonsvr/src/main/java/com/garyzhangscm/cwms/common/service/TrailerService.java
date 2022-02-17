/**
 * Copyright 2018
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

package com.garyzhangscm.cwms.common.service;

import com.garyzhangscm.cwms.common.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.common.exception.TrailerException;
import com.garyzhangscm.cwms.common.model.Location;
import com.garyzhangscm.cwms.common.model.Trailer;
import com.garyzhangscm.cwms.common.model.TrailerStatus;
import com.garyzhangscm.cwms.common.repository.TrailerRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


@Service
public class TrailerService {
    private static final Logger logger = LoggerFactory.getLogger(TrailerService.class);

    @Autowired
    private TrailerRepository trailerRepository;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public Trailer findById(Long id) {
        return trailerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("trailer not found by id: " + id));
    }


    public List<Trailer> findAll(String number) {
        if (!StringUtils.isBlank(number)) {
            return findByNumber(number);
        }
        else {
            return trailerRepository.findAll();
        }
    }

    public List<Trailer> findByNumber(String number) {
        return trailerRepository.findByNumber(number);

    }


    public Trailer save(Trailer trailer) {
        return trailerRepository.save(trailer);
    }



    public void delete(Trailer trailer) {
        trailerRepository.delete(trailer);
    }

    public void delete(Long id) {
        trailerRepository.deleteById(id);
    }


    // OK, we check in trailer without any suggested dock location. There normally
    // happens when the trailer is a fake trailer and we don't care about
    // the actual dock door we check in
    public Trailer checkInTrailer(Trailer trailer) {
        List<Location> dockLocations = warehouseLayoutServiceRestemplateClient.findEmptyDockLocations(trailer.getWarehouseId());
        if (dockLocations.size() > 0) {
            return checkInTrailer(trailer, dockLocations.get(0));
        }
        throw TrailerException.raiseException(  "Can't find empty dock location to check in");
    }

    public Trailer checkInTrailer(Long trailerId, Location dockLocation) {
        return checkInTrailer(findById(trailerId), dockLocation);
    }
    // Check in the trailer, when the trailer actually arrives at the warehouse
    // We will create a temporary location for the trailer so when we
    // actually load the inventory onto the trailer, we will systematically move
    // the inventory onto the trailer
    public Trailer checkInTrailer(Trailer trailer, Location dockLocation) {

        warehouseLayoutServiceRestemplateClient.checkInTrailerAtDockLocations(dockLocation.getId(), trailer.getId());
        trailer.setLocationId(dockLocation.getId());
        return save(trailer);
    }

    public Trailer dispatchTrailer(Long trailerId) {
        return dispatchTrailer(findById(trailerId));
    }

    public Trailer dispatchTrailer(Trailer trailer) {

        warehouseLayoutServiceRestemplateClient.dispatchTrailerFromDockLocations(trailer.getLocationId());
        trailer.setLocationId(null);
        trailer.setStatus(TrailerStatus.DISPATCHED);

        // complete all the stops in the trailer
        trailer = save(trailer);
        logger.debug("Set the trailer {}'s status to dispatched", trailer.getNumber());
        return trailer;
    }


}
