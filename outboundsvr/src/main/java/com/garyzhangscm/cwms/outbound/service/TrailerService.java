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
import com.garyzhangscm.cwms.outbound.exception.ShippingException;
import com.garyzhangscm.cwms.outbound.model.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;


@Service
public class TrailerService {
    private static final Logger logger = LoggerFactory.getLogger(TrailerService.class);

    @Autowired
    private ShipmentService shipmentService;
    @Autowired
    private StopService stopService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    public Trailer completeShipment(Shipment shipment) throws IOException {
        Trailer trailer = createFakeTrailer(shipment);
        // check in the trailer to a fake dock
        trailer = checkInTrailer(trailer);
        // Now let's start to load all the picked and staged
        // inventory onto the trailer
        return loadShipment(shipment, trailer);

    }
    public Trailer loadShipment(Shipment shipment) throws IOException {
        if (shipment.getStop() == null || shipment.getStop().getTrailerAppointment().getTrailer() == null) {
            throw ShippingException.raiseException( "The shipment is not assigned to any trailer yet");
        }
        return loadShipment(shipment, shipment.getStop().getTrailer());
    }

    public Trailer loadShipment(Shipment shipment, Trailer trailer) throws IOException {
        // load everything of the shipment onto the trailer
        // We will only load the inventory that is picked and staged
        shipmentService.loadShipment(shipment, trailer);

        Long stopNotAllShipmentLoaded = trailer.getStops()
                .stream()
                .filter(stop -> !stopService.isAllShipmentLoaded(stop)).count();

        if (stopNotAllShipmentLoaded > 0) {
            trailer.setStatus(TrailerStatus.LOADING_IN_PROCESS);
        }
        else {
            trailer.setStatus(TrailerStatus.LOADED);
        }

        return trailer;

    }

    // Load the inventory onto the trailer
    public void loadShipment(ShipmentLine shipmentLine, Inventory inventory, Trailer trailer) throws IOException {
        // Let's move the inventory onto the trailer

        Location trailerLocation =
                warehouseLayoutServiceRestemplateClient.getTrailerLocation(
                         shipmentLine.getWarehouseId(), trailer.getId());
        inventoryServiceRestemplateClient.moveInventory(inventory, trailerLocation);


    }

    // OK, we check in trailer without any suggested dock location. There normally
    // happens when the trailer is a fake trailer and we don't care about
    // the actual dock door we check in
    public Trailer checkInTrailer(Trailer trailer) {
        List<Location> dockLocations = warehouseLayoutServiceRestemplateClient.findEmptyDockLocations(trailer.getWarehouseId());
        if (dockLocations.size() > 0) {
            return checkInTrailer(trailer, dockLocations.get(0));
        }
        throw ShippingException.raiseException(  "Can't find empty dock location to check in");
    }

    public Trailer checkInTrailer(Long trailerId, Location dockLocation) {
        return checkInTrailer(
                commonServiceRestemplateClient.getTrailerById(trailerId), dockLocation);
    }


    // Check in the trailer, when the trailer actually arrives at the warehouse
    // We will create a temporary location for the trailer so when we
    // actually load the inventory onto the trailer, we will systematically move
    // the inventory onto the trailer
    public Trailer checkInTrailer(Trailer trailer, Location dockLocation) {

        warehouseLayoutServiceRestemplateClient.checkInTrailerAtDockLocations(dockLocation.getId(), trailer.getId());
        trailer.setLocationId(dockLocation.getId());
        // return save(trailer);
        return  trailer;
    }

    public Trailer dispatchTrailer(Long trailerId) {
        return dispatchTrailer(commonServiceRestemplateClient.getTrailerById(trailerId));
    }

    public Trailer dispatchTrailer(Trailer trailer) {

        warehouseLayoutServiceRestemplateClient.dispatchTrailerFromDockLocations(trailer.getLocationId());
        trailer.setLocationId(null);
        trailer.setStatus(TrailerStatus.DISPATCHED);

        // complete all the stops in the trailer
        // trailer = saveAndFlush(trailer);
        logger.debug("Start to complete all the {} stops for this trailer {}:",
                trailer.getStops().size(), trailer.getId());
        trailer.getStops().stream().forEach(
                stop -> stopService.completeStop(stop)
        );
        logger.debug("Set the trailer {}'s status to dispatched", trailer.getNumber());
        return trailer;
    }
    // Create a fake trailer when we only want to ship the shipment but
    // didn't care about any shipping information about the shipment.
    // One example is the package. We only want to ship it to the customer and
    // the carrier will take care of the deliver
    public Trailer createFakeTrailer(Shipment shipment) {
        // Let's create a fake stop for this shipment

        if (shipment.getStop() == null) {
            Stop stop = stopService.createStop(shipment);
            shipment.setStop(stop);
            shipment = shipmentService.save(shipment);
        }
        Trailer trailer = new Trailer();
        // Check if we can get carrier information from shipment
        if (shipment.getCarrierId() != null) {

            trailer.setCarrierId(shipment.getCarrierId());
        }
        else if (shipment.getCarrier() != null) {
            trailer.setCarrier(shipment.getCarrier());
            trailer.setCarrierId(shipment.getCarrierId());
        }

        if (shipment.getCarrierServiceLevelId() != null) {
            trailer.setCarrierServiceLevelId(shipment.getCarrierServiceLevelId());
        }
        else if (shipment.getCarrierServiceLevel() != null) {
            trailer.setCarrierServiceLevel(shipment.getCarrierServiceLevel());
            trailer.setCarrierServiceLevelId(shipment.getCarrierServiceLevelId());
        }

        trailer.setDriverFirstName("----");
        trailer.setDriverLastName("----");
        trailer.setDriverPhone("----");
        trailer.setLicensePlateNumber("----");
        trailer.setNumber("----");
        trailer.setSize("----");
        trailer.setType(TrailerType.UNKNOUN);
        trailer.setStatus(TrailerStatus.PENDING);
        trailer.setWarehouseId(shipment.getWarehouseId());

        // trailer = save(trailer);
        logger.debug("Trailer {} / {} created!", trailer.getId(), trailer.getNumber());
        // Refresh the stop with the new trailer;
        shipment.getStop().setTrailer(trailer);

        // We will use save and flush so that we can get the stops
        // from the trailer right away
        stopService.save(shipment.getStop());


        logger.debug("attached the trailer {} / {}  to stop {}",
                stopService.findById(shipment.getStop().getId()).getTrailer().getId(),
                stopService.findById(shipment.getStop().getId()).getTrailer().getNumber(),
                shipment.getStop().getId());

        logger.debug("Stop {}'s trailer: {}",
                stopService.findById(shipment.getStop().getId()).getId(),
                stopService.findById(shipment.getStop().getId()).getTrailer().getId());
        logger.debug("Now trailer {} has {} stops",
                trailer.getId(),
                commonServiceRestemplateClient.getTrailerById(trailer.getId()).getStops().size());

        return trailer;
    }



}
