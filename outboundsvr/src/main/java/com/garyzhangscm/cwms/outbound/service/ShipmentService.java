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
import com.garyzhangscm.cwms.outbound.clients.KafkaSender;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.exception.ShippingException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.ShipmentRepository;
import com.garyzhangscm.cwms.outbound.utils.UserContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class ShipmentService {
    private static final Logger logger = LoggerFactory.getLogger(ShipmentService.class);

    @Autowired
    private ShipmentRepository shipmentRepository;
    @Autowired
    private ShipmentLineService shipmentLineService;
    @Autowired
    private WaveService waveService;
    @Autowired
    private OrderLineService orderLineService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private TrailerService trailerService;
    @Autowired
    private StopService stopService;
    @Autowired
    private PickService pickService;
    @Autowired
    private KafkaSender kafkaSender;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public Shipment findById(Long id) {
        return findById(id, true);
    }
    public Shipment findById(Long id, boolean includeDetails) {

        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("shipment not found by id: " + id));
        if (includeDetails) {
            loadAttribute(shipment);
        }
        return shipment;
    }


    public List<Shipment> findAll(Long warehouseId, String number, String orderNumber, Long stopId, Long trailerId) {
        return findAll( warehouseId, number, orderNumber, stopId, trailerId, true);
    }
    public List<Shipment> findAll(Long warehouseId, String number, String orderNumber, Long stopId, Long trailerId, boolean includeDetails) {
        List<Shipment> shipments =  shipmentRepository.findAll(
                (Root<Shipment> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    if (warehouseId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }
                    if (!StringUtils.isBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));
                    }

                    if (!StringUtils.isBlank(orderNumber)) {
                        Join<Shipment, ShipmentLine> joinShipmentLine = root.join("shipmentLines", JoinType.INNER);
                        Join<ShipmentLine, OrderLine> joinOrderLine = joinShipmentLine.join("orderLine", JoinType.INNER);
                        Join<OrderLine, Order> joinOrder = joinOrderLine.join("order", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinOrder.get("number"), orderNumber));
                    }

                    if (stopId != null) {
                        Join<Shipment, Stop> joinStop = root.join("stop", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinStop.get("id"), stopId));
                    }

                    if (trailerId != null) {
                        Join<Shipment, Stop> joinStop = root.join("stop", JoinType.INNER);
                        Join<Stop, Trailer> joinTrailer = joinStop.join("trailer", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinTrailer.get("id"), trailerId));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        shipments = shipments.stream().distinct().collect(Collectors.toList());

        if (shipments.size() > 0 && includeDetails) {
            loadAttribute(shipments);
        }
        return shipments;
    }

    public Shipment findByNumber(String number) {
        return findByNumber(number, true);
    }
    public Shipment findByNumber(String number, boolean includeDetails) {
        Shipment shipment =  shipmentRepository.findByNumber(number);
        if (shipment != null && includeDetails) {
            loadAttribute(shipment);
        }
        return shipment;
    }


    public List<Shipment> findByOrder(Order order) {
        return findByOrder(order, true);
    }
    public List<Shipment> findByOrder(Order order, boolean includeDetails) {
        return findAll(order.getWarehouseId(), null, order.getNumber(), null, null, includeDetails);
    }
    public List<Shipment> findByStop(Long warehouseId, Long stopId) {
        return findAll(warehouseId, null, null, stopId, null);
    }
    public List<Shipment> findByTrailer(Long warehouseId, Long trailerId) {
        return findAll(warehouseId, null, null, null, trailerId);
    }



    public Shipment save(Shipment shipment) {
        return shipmentRepository.save(shipment);
    }


    public void delete(Shipment shipment) {
        shipmentRepository.delete(shipment);
    }

    public void delete(Long id) {
        shipmentRepository.deleteById(id);
    }

    public void delete(String shipmentIds) {
        if (!shipmentIds.isEmpty()) {
            long[] shipmentIdArray = Arrays.asList(shipmentIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : shipmentIdArray) {
                delete(id);
            }
        }
    }

    private void loadAttribute(List<Shipment> shipments) {
        shipments.stream().forEach(this::loadAttribute);
    }
    private void loadAttribute(Shipment shipment) {
        if (shipment.getCarrierId() != null && shipment.getCarrier() == null) {
            shipment.setCarrier(commonServiceRestemplateClient.getCarrierById(shipment.getCarrierId()));
        }
        if (shipment.getCarrierServiceLevelId() != null && shipment.getCarrierServiceLevel() == null) {
            shipment.setCarrierServiceLevel(commonServiceRestemplateClient.getCarrierServiceLevelById(shipment.getCarrierServiceLevelId()));
        }
    }

    public void cancelShipment(Shipment shipment) {

    }
    public void cancelShipment(Long id) {
        cancelShipment(findById(id));

    }

    @Transactional
    public List<Shipment> planShipments(Wave wave, String shipmentNumber, List<OrderLine> orderLines){
        // Let's split the list of order line by order number first

        Map<Order, List<OrderLine>> orderListMap = new HashMap<>();
        // We will only plan shipment for those lines with open quantity
        orderLines.stream()
                .filter(orderLine -> orderLine.getOpenQuantity() > 0)
                .forEach(orderLine ->
                {
                    // in case the order line doesn't have the order setup yet, load the order line again from the db
                    Order order = orderLineService.findById(orderLine.getId()).getOrder();
                    logger.debug("Find order: {} for order line: {}", order.getNumber(), orderLine.getId());
                    orderLine.setOrder(order);
                    List<OrderLine> existingOrderLines = orderListMap.getOrDefault(order, new ArrayList<>());
                    existingOrderLines.add(orderLine);
                    orderListMap.put(order, existingOrderLines);
                });

        List<Shipment> shipments = new ArrayList<>();
        orderListMap.entrySet().stream().forEach(orderListEntry ->{
            logger.debug("Start to process order line entry: key: {}, value: {}", orderListEntry.getKey().getNumber(), orderListEntry.getValue().size());
            logger.debug("## With user: {}", SecurityContextHolder.getContext().getAuthentication().getName());
            kafkaSender.send(OrderActivity.build()
                    .withOrder(orderService.findById(orderListEntry.getKey().getId()))
                    .withOrderActivityType(OrderActivityType.ORDER_PLAN));
            shipments.add(createShipment(wave, shipmentNumber, orderListEntry.getKey(), orderListEntry.getValue()));

        });

        return shipments;
    }

    @Transactional
    public List<Shipment> planShipments(Long warehouseId, List<OrderLine> orderLines){
        // Plan one wave for each shipment, wave number will be the shipment number
        String shipmentNumber = getNextShipmentNumber();

        Wave wave = waveService.createWave(warehouseId, shipmentNumber);
        return planShipments(wave, shipmentNumber, orderLines);


    }
    private Shipment createShipment(Wave wave, String shipmentNumber, Order order, List<OrderLine> orderLines) {

        Shipment shipment = new Shipment();
        shipment.setNumber(shipmentNumber);
        shipment.setStatus(ShipmentStatus.PENDING);
        shipment.setWarehouseId(order.getWarehouseId());

        Shipment newShipment = save(shipment);

        orderLines.forEach(orderLine -> {
            ShipmentLine shipmentLine = shipmentLineService.createShipmentLine(wave, newShipment, orderLine);

            // Add the new shipment line to the shipment
            shipment.getShipmentLines().add(shipmentLine);
        });

        return newShipment;
    }

    public String getNextShipmentNumber(){
        return commonServiceRestemplateClient.getNextNumber("shipment-number");
    }

    // Get a list of inventory that is picked for the shipment
    public List<Inventory> getPickedInventory(Shipment shipment) {
        List<Pick> picks = pickService.getPicksByShipment(shipment.getId());
        return inventoryServiceRestemplateClient.getPickedInventory(shipment.getWarehouseId(), picks);

    }

    // Get a list of the inventory that is picked and staged for the shipment
    public List<Inventory> getStagedInventory(Shipment shipment) {
        // Let's get all the picked inventory and check which is already staged
        List<Inventory> pickedInventories = getPickedInventory(shipment);
        logger.debug("Get {} picked inventory ", pickedInventories.size());
        return pickedInventories.stream()
                .filter(inventory -> inventory.getLocation().getLocationGroup().getLocationGroupType().getShippingStage())
                .collect(Collectors.toList());
    }

    // Get a list of the inventory that is already on the trailer
    public List<Inventory> getLoadedInventory(Shipment shipment) {
        // Get the trailer ID. for each trailer, we will create a location for the trailer.
        // The location's name is the same as the trailer.
        // Then we will move the inventory to the location that represent for the trailer
        if (shipment.getStop() == null || shipment.getStop().getTrailer() == null) {
            return new ArrayList<>();
        }
        Trailer trailer = shipment.getStop().getTrailer();
        // Trailer is already dispatched. We will count all inventory in the trailer as
        // shipped, not loaded
        if (trailer.getStatus().equals(TrailerStatus.DISPATCHED)) {
            return new ArrayList<>();
        }
        List<Inventory> pickedInventories = getPickedInventory(shipment);


        return pickedInventories.stream()
                .filter(inventory -> inventory.getLocation().getName().equals(String.valueOf(trailer.getId())))
                .collect(Collectors.toList());
    }

    // Get a list of the inventory that is picked and staged for the shipment
    public List<Inventory> getShippedInventory(Shipment shipment) {
        // Get the trailer ID. for each trailer, we will create a location for the trailer.
        // The location's name is the same as the trailer.
        // Then we will move the inventory to the location that represent for the trailer
        if (shipment.getStop() == null || shipment.getStop().getTrailer() == null) {
            return new ArrayList<>();
        }
        Trailer trailer = shipment.getStop().getTrailer();
        // Trailer is not dispatched. We will count all inventory in the trailer as
        // loaded, not shipped
        if (!trailer.getStatus().equals(TrailerStatus.DISPATCHED)) {
            return new ArrayList<>();
        }
        List<Inventory> pickedInventories = getPickedInventory(shipment);

        return pickedInventories.stream()
                .filter(inventory -> inventory.getLocation().getName().equals(String.valueOf(trailer.getId())))
                .collect(Collectors.toList());
    }

    public Shipment loadShipment(Shipment shipment, Trailer trailer) throws IOException {
        // load everything of the shipment onto the trailer
        // We will only load the inventory that is picked and staged
        List<Inventory> stagedInventory = getStagedInventory(shipment);

        logger.debug("Get {} staged inventory for the shipment {}",
                stagedInventory.size(), shipment.getNumber());
        stagedInventory.stream().forEach(inventory -> {
            try {
                loadShipment(inventory, trailer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // reload the shipment from db since some quantity informaiton may be changed
        shipment = findById(shipment.getId());
        // check if we can mark the shipment as loaded
        // only if we don't have any in process quantity and open quantity

        Long openShipmentLineQuantity
                = shipment.getShipmentLines().stream()
                     .filter(shipmentLine -> shipmentLine.getOpenQuantity() > 0 || shipmentLine.getInprocessQuantity() > 0).count();
        logger.debug("shipment {} still has open quantity {} from its lines",
                shipment.getNumber(), openShipmentLineQuantity);
        if (openShipmentLineQuantity == 0) {
            // THere's no open quantity so far. let's mark the shipment as loaded
            shipment.setStatus(ShipmentStatus.LOADED);

        }
        else {
            shipment.setStatus(ShipmentStatus.LOADING_IN_PROCESS);
        }
        return save(shipment);


    }

    // Load the inventory onto the trailer
    public void loadShipment(Inventory inventory, Trailer trailer) throws IOException {

        logger.debug("Start to load {} to trailer {} / {}",
                inventory.getLpn(), trailer.getId(), trailer.getNumber());
        ShipmentLine shipmentLine = pickService.findById(inventory.getPickId()).getShipmentLine();

        // Let's move the inventory onto the trailer

        Location trailerLocation =
                warehouseLayoutServiceRestemplateClient.getTrailerLocation(
                         shipmentLine.getWarehouseId(), trailer.getId());

        logger.debug("Start to move inventory {} onto trailer {} ",
                inventory.getLpn(), trailer.getId());
        inventory = inventoryServiceRestemplateClient.moveInventory(inventory, trailerLocation);

        logger.debug("Start to get pick and shipment line by id {} from inventory {}",
                inventory.getPickId(), inventory.getLpn());
        // move the quantity from inprocess to loaded quantity
        shipmentLine.setLoadedQuantity(shipmentLine.getLoadedQuantity() + inventory.getQuantity());
        shipmentLine.setInprocessQuantity(shipmentLine.getInprocessQuantity() - inventory.getQuantity());
        shipmentLineService.save(shipmentLine);


    }

    @Transactional
    // Complete the shipment
    // If we haven't setup the outbound structure yet(stop / trailer / etc), we
    //    will let the trailer service create all the structure automatically so
    //    we can complete the shipment
    public Shipment autoCompleteShipment(Shipment shipment) throws IOException {
        validateShipmentForAutoComplete(shipment);

        logger.debug("Start to complete shipment {}",shipment.getNumber());
        // OK, the shipment is in 'in process' status, let's stage it first
        if (shipment.getStatus().equals(ShipmentStatus.INPROCESS)) {
            logger.debug(">> Shipment {} is in 'In Process' status, let's stage it first", shipment.getNumber());
            shipment = stage(shipment);
        }
        if (shipment.getStatus().equals(ShipmentStatus.STAGED)) {
            logger.debug(">> Shipment {} is in 'Staged' status, let's load it first", shipment.getNumber());
            shipment = loadShipment(shipment);
        }
        if (!shipment.getStatus().equals(ShipmentStatus.LOADED)) {
            logger.debug(">> Shipment {} is in not in 'LOADED' status, we will not complete it", shipment.getNumber());
            throw ShippingException.raiseException("Can't complete the shipment, shipment's status " + shipment.getStatus()
                            + "is not ready for complete.");
        }
        // if we are here, we know for sure
        // 1. the shipment is already loaded
        // 2. there's only one shipment in this trailer
        // Let's dispatch the trailer

        Trailer trailer = shipment.getStop().getTrailer();
        if (trailer.getStops().size() == 0) {
            // Due to the JPA persistent logic, the trailer.getstops
            // may be empty, which I don't know why. But we can always
            // setup the stops manually

            trailer.setStops(stopService.findByTrailerId(trailer.getId()));
        }
        logger.debug("Start to display trailer with {} stops ", trailer.getStops().size());
        trailerService.dispatchTrailer(trailer);

        logger.debug("Update the shipment {}'s status to dispatched", shipment.getNumber());
        logger.debug("Trailer {} dispatched", shipment.getStop().getTrailer().getId());
        return shipment;
    }
    // Check if we can complete the shipment with automatically create all the necessary data structure
    // There's 2 ways to complete a shipment
    // 1. Auto: We will create most of the outbound structure(trailer / stop / carrier / ect) at the background
    //   so as, as long as the end user doens't care about those information, to speed up the outbound
    //   speed
    // 2. Manual: The user will track the process of create the trailer, plan the docker, check in when arrive,
    //    loading and dispatch, every outbound step.
    // This function will check if we can 'Auto' process the shipment only when
    // there's only one shiment in the same stop and trailer(which probably means the system created the fake
    //    stop and trailer)
    private void validateShipmentForAutoComplete(Shipment shipment) {
        if (shipment.getStop() != null) {
            // Let's see how many shipments this stop have
            int shipmentInTheSameStop =
                    findByStop(shipment.getWarehouseId(), shipment.getStop().getId()).size();
            if (shipmentInTheSameStop > 1) {
                throw ShippingException.raiseException(
                        "There's multiple shipments in the same stop, please process each shipment individually");

            }
            // let's see how many shipments in the same trailer
            if (shipment.getStop().getTrailer() != null) {
                int shipmentInTheSameTrailer = findByTrailer(
                        shipment.getWarehouseId(),
                        shipment.getStop().getTrailer().getId()).size();
                if (shipmentInTheSameTrailer > 1) {
                    throw ShippingException.raiseException(
                            "There's multiple shipments in the same trailer, please process each shipment individually");

                }
            }
        }
    }

    public Shipment autoCompleteShipment(Long shipmentId) {
        try {
            return autoCompleteShipment(findById(shipmentId));

        }
        catch (IOException ex) {
            throw  ShippingException.raiseException( ex.getMessage());
        }
    }

    // check if the shipment is ready for stage
    public boolean readyForStage(Shipment shipment, boolean ignoreUnfinishedPicks) {
        // first of all, make sure the shipment is in 'INPROCESS' status

        logger.debug("Check if the shipment is ready for stage");
        logger.debug("Shipment {}'s status is {}",
                shipment.getNumber(), shipment.getStatus());
        if (!shipment.getStatus().equals(ShipmentStatus.INPROCESS)) {
            return false;
        }

        List<Pick> shipmentPicks = pickService.getPicksByShipment(shipment.getId());
        logger.debug("Shipment {} has {} picks",
                shipment.getNumber(), shipmentPicks.size());
        // make sure all the picks are done and the inventories are in the designated
        // stage locations

        if (!ignoreUnfinishedPicks &&
                shipmentPicks.stream()
                    .filter(pick -> pick.getQuantity() > pick.getPickedQuantity()).count()
                    > 0) {
            // the shipment still has unfinished picks
            logger.debug("Shipment {} is not ready for stage as it has open picks",
                    shipment.getNumber());
            return false;
        }

        // make sure all the picked inventory are in the ship stage
        List<Inventory>  pickedInventories
                = inventoryServiceRestemplateClient.getPickedInventory(shipment.getWarehouseId(), shipmentPicks);
        logger.debug("Shipment {} has picked {} inventories",
                shipment.getNumber(), pickedInventories.size());
        if (pickedInventories.stream()
                .filter(inventory -> inventory.getLocation().getLocationGroup().getLocationGroupType().getShippingStage() != true)
                .count() > 0) {
            logger.debug("We have some picked inventory that is not in the shiping stage");
            return false;
        }
        logger.debug("Shipment {} is ready for stage",
                shipment.getNumber());
        return true;
    }

    public Shipment stage(Shipment shipment) {
        return stage(shipment, false);
    }
    public Shipment stage(Shipment shipment, boolean ignoreUnfinishedPicks) {
        logger.debug("Start to stage shipment {}, ignore unfinished picks? {}",
                shipment.getNumber(), ignoreUnfinishedPicks);
        if (!readyForStage(shipment, ignoreUnfinishedPicks)) {
            logger.debug("Shipment {} is not ready for stage",
                    shipment.getNumber());
            throw ShippingException.raiseException("Shipment is not ready for stage yet");
        }

        shipment.setStatus(ShipmentStatus.STAGED);
        return save(shipment);
    }

    public Shipment loadShipment(Shipment shipment) throws IOException {
        // See if we have already had a trailer / stop assigned for this shipment
        if (shipment.getStop() == null) {
            logger.debug("Shipment {} doesn't have any stop yet, let's create a fake stop for this shipment",
                    shipment.getNumber());
            Stop stop = stopService.createStop(shipment);
            shipment.setStop(stop);
            shipment = save(shipment);
            logger.debug("Stop {} created!", shipment.getStop().getId());
        }

        Trailer trailer = shipment.getStop().getTrailer();
        if (trailer == null) {
            logger.debug("Shipment {} doesn't have any trailer yet, let's create a fake trailer for this shipment",
                    shipment.getNumber());

            trailer = trailerService.createFakeTrailer(shipment);
            logger.debug("trailer {} create for shipment {}",
                    trailer.getId(), shipment.getNumber());
            // check in the trailer to a fake dock
            trailer = trailerService.checkInTrailer(trailer);
            logger.debug("trailer {} checked in ",
                    trailer.getId());
            logger.debug(">> The trailer has {} stops");
        }
        return loadShipment(shipment, trailer);

    }


    // When a stop complete, we will complete all the shipments in this
    // stop
    public void dispatchShipment(Shipment shipment) {
        shipment.setStatus(ShipmentStatus.DISPATCHED);
        shipment = save(shipment);

        // Complete the shipment lines
        shipment.getShipmentLines().stream().forEach(
                shipmentLine ->
                        shipmentLineService.completeShipmentLine(shipmentLine)
        );

    }

}
