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

package com.garyzhangscm.cwms.outbound.service;


import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;

import com.garyzhangscm.cwms.outbound.repository.PickMovementRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.*;


@Service
public class PickMovementService{
    private static final Logger logger = LoggerFactory.getLogger(PickMovementService.class);

    @Autowired
    private PickMovementRepository pickMovementRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public PickMovement findById(Long id, boolean loadDetails) {
        PickMovement pickMovement = pickMovementRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("pick movement not found by id: " + id));;
        if (loadDetails) {
            loadAttribute(pickMovement);
        }
        return pickMovement;
    }

    public PickMovement findById(Long id) {
        return findById(id, true);
    }



    public void loadAttribute(List<PickMovement> pickMovements) {
        for (PickMovement pickMovement : pickMovements) {
            loadAttribute(pickMovement);
        }
    }

    public void loadAttribute(PickMovement pickMovement) {
        // Load the details for client and supplier informaiton
        if (pickMovement.getLocationId() != null && pickMovement.getLocation() == null) {
            pickMovement.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(pickMovement.getLocationId()));
        }

    }


    public PickMovement save(PickMovement pickMovement) {
        PickMovement newPickMovement = pickMovementRepository.save(pickMovement);
        loadAttribute(newPickMovement);
        return newPickMovement;
    }


    public void delete(PickMovement pickMovement) {
        pickMovementRepository.delete(pickMovement);
    }

    public void delete(Long id) {
        pickMovementRepository.deleteById(id);
    }

    public void delete(String pickMovementIds) {
        if (!pickMovementIds.isEmpty()) {
            long[] pickMovementIdArray = Arrays.asList(pickMovementIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : pickMovementIdArray) {
                delete(id);
            }
        }
    }


    /**
     * Find all the pick movement that will hop through one specific location
     * @param locationId Hop through location's id
     * @return
     */
    public List<PickMovement> findByHopLocation(Long locationId) {
        return pickMovementRepository.findByLocationId(locationId);

    }

    private PickMovement findByPickIdAndLocationId(Long pickId, Long locationId){
        return  pickMovementRepository.findByPickIdAndLocationId(pickId, locationId);
    }

    public void refreshPickMovement(Long pickId,
                                    Long destinationLocationId,
                                    Long quantity) {

        PickMovement pickMovement =findByPickIdAndLocationId(pickId, destinationLocationId);
        if (Objects.nonNull(pickMovement)) {
            pickMovement.setArrivedQuantity(pickMovement.getArrivedQuantity() + quantity);
            save(pickMovement);
        }

    }



}
