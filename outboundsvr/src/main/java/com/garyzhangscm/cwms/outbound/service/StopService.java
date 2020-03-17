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
import com.garyzhangscm.cwms.outbound.repository.ShipmentRepository;
import com.garyzhangscm.cwms.outbound.repository.StopRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;


@Service
public class StopService {
    private static final Logger logger = LoggerFactory.getLogger(StopService.class);

    @Autowired
    private StopRepository stopRepository;
    @Autowired
    private ShipmentService shipmentService;


    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public Stop findById(Long id) {
        return stopRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("stop not found by id: " + id));
    }


    public List<Stop> findAll() {
        return stopRepository.findAll();
    }

    public List<Stop> findByTrailerId(Long trailerId) {

        return stopRepository.findByTrailerId(trailerId);
    }

    public Stop save(Stop stop) {
        return stopRepository.save(stop);
    }
    public Stop saveAndFlush(Stop stop) {
        return stopRepository.saveAndFlush(stop);
    }


    public void delete(Stop stop) {
        stopRepository.delete(stop);
    }

    public void delete(Long id) {
        stopRepository.deleteById(id);
    }

    public void delete(String stopIds) {
        if (!stopIds.isEmpty()) {
            long[] stopIdArray = Arrays.asList(stopIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : stopIdArray) {
                delete(id);
            }
        }
    }

    public Stop createStop(Shipment shipment) {
        Stop stop = new Stop();
        List<Shipment> shipments = new ArrayList<>();
        shipments.add(shipment);
        stop.setShipments(shipments);
        stop.setWarehouseId(shipment.getWarehouseId());
        return saveAndFlush(stop);
    }

    public boolean isAllShipmentLoaded(Stop stop) {
        Long shipmentUnloaded = stop.getShipments()
                .stream()
                .filter(shipment ->
                        !shipment.getStatus().equals(ShipmentStatus.DISPATCHED) &&
                        !shipment.getStatus().equals(ShipmentStatus.LOADED))
                .count();
        return shipmentUnloaded == 0;
    }

    public void completeStop(Stop stop) {
        stop.getShipments().stream().forEach(shipment ->
                shipmentService.dispatchShipment(shipment));
    }

}
