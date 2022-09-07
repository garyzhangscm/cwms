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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.outbound.clients.*;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.exception.ShippingException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.ShipmentRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class ShipmentService {
    private static final Logger logger = LoggerFactory.getLogger(ShipmentService.class);

    @Autowired
    private ShipmentRepository shipmentRepository;
    @Autowired
    private ShipmentLineService shipmentLineService;
    @Autowired
    private OrderActivityService orderActivityService;
    @Autowired
    private AllocationTransactionHistoryService allocationTransactionHistoryService;
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
    private CancelledPickService cancelledPickService;
    @Autowired
    private CancelledShortAllocationService cancelledShortAllocationService;
    @Autowired
    private ShortAllocationService shortAllocationService;
    @Autowired
    private KafkaSender kafkaSender;

    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;
    @Autowired
    private IntegrationService integrationService;

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


    public List<Shipment> findAll(Long warehouseId, String number, String orderNumber, Long stopId, Long trailerId,
                                  Boolean withoutStopOnly,
                                  String shipmentStatusList) {
        return findAll( warehouseId, number, orderNumber, stopId, trailerId, withoutStopOnly, shipmentStatusList, true);
    }
    public List<Shipment> findAll(Long warehouseId, String number,
                                  String orderNumber, Long stopId,
                                  Long trailerId,
                                  Boolean withoutStopOnly,
                                  String shipmentStatusList,
                                  boolean includeDetails) {
        List<Shipment> shipments =  shipmentRepository.findAll(
                (Root<Shipment> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    if (warehouseId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }
                    if (StringUtils.isNotBlank(number)) {
                        if (number.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("number"), number));

                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("number"), number));

                        }
                    }

                    if (StringUtils.isNotBlank(orderNumber)) {
                        Join<Shipment, ShipmentLine> joinShipmentLine = root.join("shipmentLines", JoinType.INNER);
                        Join<ShipmentLine, OrderLine> joinOrderLine = joinShipmentLine.join("orderLine", JoinType.INNER);
                        Join<OrderLine, Order> joinOrder = joinOrderLine.join("order", JoinType.INNER);

                        if (orderNumber.contains("%")) {
                            predicates.add(criteriaBuilder.like(joinOrder.get("number"), orderNumber));

                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinOrder.get("number"), orderNumber));
                        }
                    }

                    if (stopId != null) {
                        Join<Shipment, Stop> joinStop = root.join("stop", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinStop.get("id"), stopId));
                    }

                    if (Boolean.TRUE.equals(withoutStopOnly)) {

                        predicates.add(criteriaBuilder.isNull(root.get("stop")));
                    }
                    if (trailerId != null) {
                        Join<Shipment, Stop> joinStop = root.join("stop", JoinType.INNER);
                        Join<Stop, Trailer> joinTrailer = joinStop.join("trailer", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinTrailer.get("id"), trailerId));
                    }

                    if (Strings.isNotBlank(shipmentStatusList)) {

                        CriteriaBuilder.In<ShipmentStatus> inShipmentStatus = criteriaBuilder.in(root.get("status"));
                        for(String shipmentStatus : shipmentStatusList.split(",")) {
                            inShipmentStatus.value(ShipmentStatus.valueOf(shipmentStatus));
                        }
                        predicates.add(criteriaBuilder.and(inShipmentStatus));
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
        return findAll(order.getWarehouseId(), null, order.getNumber(), null, null, null, null, includeDetails);
    }
    public List<Shipment> findByStop(Long warehouseId, Long stopId) {
        return findAll(warehouseId, null, null, stopId, null, null,null);
    }
    public List<Shipment> findByTrailer(Long warehouseId, Long trailerId) {
        return findAll(warehouseId, null, null, null, trailerId, null,null);
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
    public Shipment allocateShipment(Long id){

        Shipment shipment = findById(id);
        // Allocate each line
        shipment.getShipmentLines().
                forEach(shipmentLine -> shipmentLineService.allocateShipmentLine(shipmentLine));

        // return the result after the allocation
        return findById(id);
    }

    @Transactional
    public List<Shipment> planShipments(Wave wave, List<OrderLine> orderLines){

        List<Shipment> shipments = new ArrayList<>();

        Map<Order, List<OrderLine>> orderListMap = segregateOrderLinesBasedOnOrder(orderLines, false);
        // for each order, generate a shipment number and plan the order lines
        // into the shipment
        orderListMap.entrySet().forEach(entrySet -> {
            String shipmentNumber = getNextShipmentNumber(entrySet.getKey().getWarehouseId());
            Shipment shipment = planShipments(wave, shipmentNumber, entrySet.getValue());
            if (Objects.nonNull(shipment)) {
                shipments.add(shipment);
            }
        });
        return shipments;
    }
    @Transactional
    public Shipment planShipments(String shipmentNumber, List<OrderLine> orderLines){
        return planShipments(null, shipmentNumber, orderLines);
    }
    @Transactional
    public Shipment planShipments(Wave wave, String shipmentNumber, List<OrderLine> orderLines){

        // Let's split the list of order line by order number first

        Map<Order, List<OrderLine>> orderListMap = segregateOrderLinesBasedOnOrder(orderLines, true);

        // Make sure we only have one order
        if (orderListMap.keySet().size() > 1) {
            throw OrderOperationException.raiseException("Can't plan multiple orders into a single shipment");
        }
        else if (orderListMap.keySet().size() == 0) {
            // There's nothing to be planned, return nothing
            return null;
        }

        // If we are here, we know we will only have one order
        // and the orderListMap only contains the plannable lines
        Order order = orderListMap.keySet().iterator().next();
        List<OrderLine> plannableOrderLines = orderListMap.values().iterator().next();
        if (plannableOrderLines.size() == 0) {
            // there's nothing to be planned, return nothing
            return null;
        }

        orderActivityService.createOrderActivity(
                order.getWarehouseId(), order, OrderActivityType.ORDER_PLAN
        );


        return createShipment(wave, shipmentNumber, order, plannableOrderLines);

    }

    @Transactional
    public Shipment planShipments(Long warehouseId, String waveNumber, String shipmentNumber, List<OrderLine> orderLines){

        Wave wave = waveService.createWave(warehouseId, waveNumber);
        return planShipments(wave, shipmentNumber, orderLines);

    }



    @Transactional
    public List<Shipment> planShipments(Long warehouseId, List<OrderLine> orderLines){
        List<Shipment> shipments = new ArrayList<>();
        // Plan one wave for each shipment
        Map<Order, List<OrderLine>> orderListMap
                = segregateOrderLinesBasedOnOrder(orderLines, true);

        orderListMap.entrySet().forEach(entrySet -> {

            String shipmentNumber = getNextShipmentNumber(warehouseId);
            // we will not create a fake wave for the shipment any more
            // in this scenario, we will ship by shipment
            // Wave wave = waveService.createWave(warehouseId, shipmentNumber);
            Shipment shipment = planShipments(shipmentNumber, entrySet.getValue());
            shipments.add(shipment);

        });


        return shipments;

    }

    private Map<Order, List<OrderLine>> segregateOrderLinesBasedOnOrder(
            List<OrderLine> orderLines, boolean plannableOrderLine) {

        Map<Order, List<OrderLine>> orderListMap = new HashMap<>();
        Stream<OrderLine> orderLineStream = orderLines.stream();



        if (plannableOrderLine) {
            orderLineStream = orderLineStream
                    .filter(orderLine -> orderLine.getOpenQuantity() > 0);
        }

        // We will only plan shipment for those lines with open quantity
        // We will plan one shipment per order.
        orderLineStream
                .forEach(orderLine ->
                {
                    // in case the order line doesn't have the order setup yet, load the order line again from the db
                    logger.debug("check order line: {}  ",
                            orderLine.getId());
                    Order order = orderLine.getOrder();
                    if (Objects.isNull(order)) {
                        order = orderLineService.findById(orderLine.getId()).getOrder();
                    }
                    logger.debug("Find order: {} for order line: {}",
                            order.getNumber(), orderLine.getId());
                    List<OrderLine> existingOrderLines = orderListMap.getOrDefault(order, new ArrayList<>());
                    logger.debug("existingOrderLines for order {}, existing order lines: {}",
                            order.getNumber(), existingOrderLines.size());
                    existingOrderLines.add(orderLine);
                    logger.debug("after add current order line we have  {} lines for order {}",
                            existingOrderLines.size(), order.getNumber());
                    orderListMap.put(order, existingOrderLines);
                    logger.debug("now we have {} orders", orderListMap.keySet().size());
                });
        logger.debug("we get {} orders out of {} order lines",
                orderListMap.keySet().size(), orderLines.size());
        return orderListMap;
    }

    private Shipment createShipment(String shipmentNumber, Order order, List<OrderLine> orderLines) {
        return createShipment(null, shipmentNumber, order, orderLines);
    }
    private Shipment createShipment(Wave wave, String shipmentNumber, Order order, List<OrderLine> orderLines) {

        Shipment shipment = new Shipment(shipmentNumber, order);
        Shipment newShipment = save(shipment);

        orderLines.forEach(orderLine -> {
            ShipmentLine shipmentLine = shipmentLineService.createShipmentLine(wave, newShipment, orderLine);

            orderActivityService.saveOrderActivity(
                orderActivityService.createOrderActivity(
                        newShipment.getWarehouseId(), order, newShipment,
                        shipmentLine, OrderActivityType.ORDER_PLAN
                ));


            // Add the new shipment line to the shipment
            newShipment.getShipmentLines().add(shipmentLine);
        });

        return newShipment;
    }

    public String getNextShipmentNumber(Long warehouseId){
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "shipment-number");
    }

    // Get a list of inventory that is picked for the shipment
    public List<Inventory> getPickedInventory(Shipment shipment) {
        List<Pick> picks = pickService.getPicksByShipment(shipment.getId());
        if (picks.size() == 0) {
            return new ArrayList<>();
        }
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
        if (shipment.getStop() == null || shipment.getStop().getTrailerAppointment().getTrailer() == null) {
            return new ArrayList<>();
        }
        Trailer trailer = shipment.getStop().getTrailerAppointment().getTrailer();
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
        if (shipment.getStop() == null || shipment.getStop().getTrailerAppointment().getTrailer() == null) {
            return new ArrayList<>();
        }
        Trailer trailer = shipment.getStop().getTrailerAppointment().getTrailer();
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

    public Shipment loadShipment(Shipment shipment, Trailer trailer)   {
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

        // reload the shipment from db since some quantity information may be changed
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

        Trailer trailer = shipment.getStop().getTrailerAppointment().getTrailer();
        if (trailer.getStops().size() == 0) {

            trailer.addStop(shipment.getStop());
        }
        logger.debug("Start to display trailer with {} stops ", trailer.getStops().size());
        trailerService.dispatchTrailer(trailer);

        logger.debug("Update the shipment {}'s status to dispatched", shipment.getNumber());
        logger.debug("Trailer {} dispatched", shipment.getStop().getTrailerAppointment().getTrailer().getId());
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
            if (shipment.getStop().getTrailerAppointment().getTrailer() != null) {
                int shipmentInTheSameTrailer = findByTrailer(
                        shipment.getWarehouseId(),
                        shipment.getStop().getTrailerAppointment().getTrailer().getId()).size();
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

    public Shipment stage(Long id, boolean ignoreUnfinishedPicks) {
        return stage(findById(id), ignoreUnfinishedPicks);
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

    public Shipment load(Long shipmentId, boolean ignoreUnfinishedPicks)   {
        Shipment shipment = findById(shipmentId);
        // make sure the shipment is already staged, if not, let's try to stage it first
        if (shipment.getStatus() == ShipmentStatus.INPROCESS) {
            shipment = stage(shipment, ignoreUnfinishedPicks);
        }
        return loadShipment(shipment);

    }
    public Shipment loadShipment(Shipment shipment)  {
        // See if we have already had a trailer / stop assigned for this shipment
        if (shipment.getStop() == null) {
            logger.debug("Shipment {} doesn't have any stop yet, let's create a fake stop for this shipment",
                    shipment.getNumber());
            Stop stop = stopService.createStop(shipment);
            shipment.setStop(stop);
            shipment = save(shipment);
            logger.debug("Stop {} created!", shipment.getStop().getId());
        }

        Trailer trailer = shipment.getStop().getTrailerAppointment().getTrailer();
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

    @Transactional
    public Shipment dispatch(Long shipmentId, boolean ignoreUnfinishedPicks) {

        Shipment shipment = findById(shipmentId);
        // make sure the shipment is already staged, if not, let's try to stage it first
        if (shipment.getStatus() == ShipmentStatus.INPROCESS ||
                shipment.getStatus() == ShipmentStatus.STAGED) {
            shipment = load(shipmentId, ignoreUnfinishedPicks);
        }
        return dispatchShipment(shipment);

    }


    // When a stop complete, we will complete all the shipments in this
    // stop
    public Shipment dispatchShipment(Shipment shipment) {

        // Complete the shipment lines
        shipment.getShipmentLines().stream().forEach(
                shipmentLine ->
                        shipmentLineService.completeShipmentLine(shipmentLine)
        );

        shipment.setStatus(ShipmentStatus.DISPATCHED);
        shipment = save(shipment);

        return shipment;

    }

    public List<Shipment> getShipmentsByPickedInventory(List<Inventory> inventories) {

        Map<String, Shipment> shipments = new HashMap<>();

        inventories.forEach(inventory -> {
            Shipment shipment = getShipmentByPickedInventory(inventory);
            if (Objects.nonNull(shipment)) {
                shipments.putIfAbsent(shipment.getNumber(), shipment);
            }
            else {

                shipments.putIfAbsent("NULL-SHIPMENT", null);
            }
        });

        return new ArrayList<>(shipments.values());

    }
    public Shipment getShipmentByPickedInventory(Inventory inventory) {
        logger.debug("getShipmentByPickedInventory: \n {}", inventory);
        ShipmentLine shipmentLine = shipmentLineService.getShipmentLineByPickedInventory(inventory);
        if (Objects.nonNull(shipmentLine)) {
            return shipmentLine.getShipment();
        }
        else {
            return null;
        }
    }

    public void completeShipment(Shipment shipment) {
        if (!validateShipmentReadyForComplete(shipment)) {
            throw ShippingException.raiseException("Shipment  " + shipment.getNumber() +
                    " is not ready for complete yet" +
                    " please check if there's open pick and short allocation");
        }
        if (Objects.isNull(shipment.getStop()) && Objects.isNull(shipment.getOrder())) {

            throw ShippingException.raiseException("System error when closing Shipment  "
                    + shipment.getNumber() +
                    ". NOT able to get trailer or order from the shipment");
        }
        else if (Objects.nonNull(shipment.getOrder())) {
            completeShipmentByOrder(shipment, shipment.getOrder());
        }
        else {
            completeShipmentByTrailerAppointment(shipment,
                    shipment.getStop().getTrailerAppointment());
        }

        // print delivery note after we complete the order
        try {

            printDeliveryNote(shipment);
        }
        catch (Exception ex) {
            logger.debug("Error while printing delivery note during complete shipment {} of order {}",
                    shipment.getNumber());
            ex.printStackTrace();
            logger.debug("We will ignore this error and let the user reprint the document");
        }



        if (Objects.nonNull(shipment.getOrder()) && shipment.getOrder().getCategory().isAutoGenerateReceipt()) {
            logger.debug("Start to create receipt {} for the order {}, into warehouse {}",
                    shipment.getOrder().getTransferReceiptNumber(),
                    shipment.getOrder().getNumber(),
                    shipment.getOrder().getTransferReceiptWarehouseId());
            // for every shipment, we will need to generate a correspondent
            // receipt for it
            generateReceipt(shipment, shipment.getOrder());
        }


        logger.debug("Start to send order confirmation after the order {} is marked as completed",
                shipment.getNumber());
        sendOrderConfirmationIntegration(shipment);

        warehouseLayoutServiceRestemplateClient.releaseLocations(shipment.getWarehouseId(), shipment);
    }

    private void sendOrderConfirmationIntegration(Shipment shipment) {

        integrationService.process(new OrderConfirmation(shipment));
    }
    /**
     * Check if we are ready to complete a shipment when
     * there's no open pick / short allocation
     * @param shipment
     * @return
     */
    public boolean validateShipmentReadyForComplete(Shipment shipment) {
        List<Pick> picks = pickService.getPicksByShipment(shipment.getId());
        if (picks.stream()
                .anyMatch(pick -> pick.getPickedQuantity() < pick.getQuantity())) {
            return false;
        }

        List<ShortAllocation> shortAllocations
                = shortAllocationService.findByShipment(shipment);
        if (!shortAllocations.isEmpty()) {
            return false;
        }

        return true;
    }

    private void generateReceipt(Shipment shipment, Order order) {
        logger.debug("Start to generate receipt when the shipment is completed");
        // key: item id
        // value: shipped quantity
        Map<Long, Long> shippedItem = new HashMap<>();
        shipment.getShipmentLines().forEach(
                shipmentLine -> {
                    logger.debug("shipped {} of item {}",
                            shipmentLine.getShippedQuantity(),
                            shipmentLine.getOrderLine().getItem().getName());
                    shippedItem.put(
                            shipmentLine.getOrderLine().getItem().getId(),
                            shipmentLine.getShippedQuantity());
                }
        );

        WarehouseTransferReceipt warehouseTransferReceipt =
                new WarehouseTransferReceipt(
                        order.getWarehouseId(),
                        order.getTransferReceiptWarehouseId(),
                        shippedItem,
                        order.getTransferReceiptNumber(),
                        order.getNumber());
        logger.debug("will send the transfer receipt request to the destination warehouse {} \n{}",
                warehouseTransferReceipt.getDestinationWarehouseId(),
                warehouseTransferReceipt);

        // add the receipt to Kafka server
        kafkaSender.send(warehouseTransferReceipt);

    }

    private void printDeliveryNote(Order order) throws JsonProcessingException, UnsupportedEncodingException {
        ReportHistory reportHistory
                = generateDeliveryNote(order);
        Long companyId = order.getWarehouse().getCompanyId();
        if (Objects.isNull(companyId)) {
            order.getWarehouse().getCompany().getId();
        }
        logger.debug("Start to print delivery note for company {} / warehouse {}",
                companyId, order.getWarehouseId());
        logger.debug("report file: {}", reportHistory.getFileName());
        String result = resourceServiceRestemplateClient.printReport(
                companyId, order.getWarehouseId(),
                ReportType.DELIVERY_NOTE, reportHistory.getFileName(),
                order.getCategory().toString(), ""
        );
        logger.debug(">> print result: {}", result);
    }

    private void printDeliveryNote(Shipment shipment) throws JsonProcessingException, UnsupportedEncodingException {
        ReportHistory reportHistory
                = generateDeliveryNote(shipment);
        Long companyId = shipment.getWarehouse().getCompanyId();
        if (Objects.isNull(companyId)) {
            companyId = shipment.getWarehouse().getCompany().getId();
        }
        logger.debug("Start to print delivery note for company {} / warehouse {}",
                companyId, shipment.getWarehouseId());
        logger.debug("report file: {}", reportHistory.getFileName());
        String result = resourceServiceRestemplateClient.printReport(
                companyId, shipment.getWarehouseId(),
                ReportType.DELIVERY_NOTE, reportHistory.getFileName(),
                shipment.getNumber(), ""
        );
        logger.debug(">> print result: {}", result);
    }

    private ReportHistory generateDeliveryNote(Order order) throws JsonProcessingException {
        Long warehouseId = order.getWarehouseId();

        Report reportData = new Report();
        setupDelieryNoteParameters(
                reportData, order
        );
        setupDelieryNoteReportData(
                reportData, order
        );

        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.DELIVERY_NOTE, reportData, ""
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;
    }

    private ReportHistory generateDeliveryNote(Shipment shipment) throws JsonProcessingException {
        Long warehouseId = shipment.getWarehouseId();

        Report reportData = new Report();
        setupDelieryNoteParameters(
                reportData, shipment
        );
        setupDelieryNoteReportData(
                reportData, shipment
        );

        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.DELIVERY_NOTE, reportData, ""
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;
    }

    private void setupDelieryNoteParameters(
            Report report, Order order) {

        // set the parameters to be the meta data of
        // the order

        report.addParameter("order_number", order.getNumber());

        report.addParameter("customer_name",
                order.getShipToContactorFirstname() + " " +
                        order.getShipToContactorLastname());

        Integer totalLineCount = order.getTotalLineCount();
        Integer totalItemCount = order.getTotalItemCount();
        Long totalQuantity =
                pickService.findByOrder(order).stream()
                        .mapToLong(Pick::getQuantity).sum();

        report.addParameter("totalLineCount", totalLineCount);
        report.addParameter("totalItemCount", totalItemCount);
        report.addParameter("totalQuantity", totalQuantity);
    }

    private void setupDelieryNoteParameters(
            Report report, Shipment shipment) {

        // set the parameters to be the meta data of
        // the order

        report.addParameter("order_number", shipment.getOrderNumber());

        report.addParameter("customer_name",
                shipment.getShipToContactorFirstname() + " " +
                        shipment.getShipToContactorLastname());
/**
        Integer totalLineCount = shipment.getTotalLineCount();
        Integer totalItemCount = shipment.getTotalItemCount();
        Long totalQuantity =
                pickService.findByOrder(order).stream()
                        .mapToLong(Pick::getQuantity).sum();

        report.addParameter("totalLineCount", totalLineCount);
        report.addParameter("totalItemCount", totalItemCount);
        report.addParameter("totalQuantity", totalQuantity);
 **/
        report.addParameter("totalLineCount", 0);
        report.addParameter("totalItemCount", 0);
        report.addParameter("totalQuantity", 0);
    }


    private void setupDelieryNoteReportData(Report report, Order order) {

        // set data to be all picks
        List<Pick> picks = pickService.findByOrder(order);
        report.setData(picks);
    }
    private void setupDelieryNoteReportData(Report report, Shipment shipment) {

        // set data to be all picks
        List<Pick> picks = pickService.findByShipment(shipment);
        report.setData(picks);
    }

    /**
     * Complete shipment by order, without any outbound
     * structure(trailer / stop / truck / etc)
     * @param shipment
     */
    @Transactional
    public void completeShipmentByOrder(Shipment shipment, Order order) {
        // Move all the staged inventory to a location
        // that stands for the order
        Location location
                = warehouseLayoutServiceRestemplateClient.createOrderLocation(order.getWarehouseId(), order);
        completeShipment(shipment, location);

    }
    /**
     * Complete shipment by trailer appointment
     * structure(trailer / stop / truck / etc)
     * @param shipment
     */
    @Transactional
    public void completeShipmentByTrailerAppointment(Shipment shipment, TrailerAppointment trailerAppointment) {
        // Move all the staged inventory to a location
        // that stands for the order
        Location location
                = warehouseLayoutServiceRestemplateClient.createTrailerAppointmentLocation(
                        shipment.getWarehouseId(), trailerAppointment.getNumber());
        completeShipment(shipment, location);

    }
    /**
     * Complete shipment by order, without any outbound
     * structure(trailer / stop / truck / etc)
     * @param shipment
     * @param inventoryDestination inventory's destination when loading the inventory onto
     */
    @Transactional
    public void completeShipment(Shipment shipment, Location inventoryDestination) {

        List<Inventory> stagedInventory = getStagedInventory(shipment);



        // move inventory to the location and update the shipment line's quantity
        stagedInventory.stream().forEach(inventory -> {

            ShipmentLine shipmentLine = pickService.findById(inventory.getPickId()).getShipmentLine();

            logger.debug("Start to move inventory {} onto order {} ",
                    inventory.getLpn(), inventoryDestination.getName());
            try {
                inventory = inventoryServiceRestemplateClient.moveInventory(inventory, inventoryDestination);
            } catch (IOException e) {
                e.printStackTrace();
                throw OrderOperationException.raiseException("Error when loading inventory  for shipment: "
                        + shipment.getNumber() + " onto destination " +
                        inventoryDestination.getName());
            }
            shipmentLine.setLoadedQuantity(shipmentLine.getLoadedQuantity() + inventory.getQuantity());
            shipmentLine.setShippedQuantity(shipmentLine.getShippedQuantity() + inventory.getQuantity());
            shipmentLine.setInprocessQuantity(shipmentLine.getInprocessQuantity() - inventory.getQuantity());
            shipmentLine.setStatus(ShipmentLineStatus.DISPATCHED);
            shipmentLineService.save(shipmentLine);

            OrderLine orderLine = shipmentLine.getOrderLine();
            orderLine.setInprocessQuantity(orderLine.getInprocessQuantity() - inventory.getQuantity());
            orderLine.setShippedQuantity(orderLine.getShippedQuantity() + inventory.getQuantity());
            orderLineService.saveOrUpdate(orderLine);
        });

        shipment.setStatus(ShipmentStatus.DISPATCHED);
        shipment.setCompleteTime(LocalDateTime.now());
        save(shipment);
        // after we complete the shipment, release all the locations
        // that reserved by the shipment

        logger.debug("# Will release the locations that reserved by the shipment {}",
                shipment.getNumber());
        warehouseLayoutServiceRestemplateClient.releaseLocations(shipment.getWarehouseId(), shipment);

    }

    public void completeShipmentByTrailerAppointment(Shipment shipment, Order order) {}

    @Transactional
    public void removeShipment(Long shipmentId) {
        removeShipment(findById(shipmentId));
    }

    @Transactional
    public void removeShipment(Shipment shipment) {

        // make sure we don't have any pick / short allocation
        if (pickService.findByShipment(shipment).size() > 0) {
            throw OrderOperationException.raiseException("Can't remove shipment while it has open picks");
        }
        if (shortAllocationService.findByShipment(shipment).size() > 0){
            throw OrderOperationException.raiseException("Can't remove shipment while it has open short allocations");
        }

        // remove all the cancelled pick first
        cancelledPickService.removeCancelledPicks(shipment);

        shortAllocationService.removeCancelledShortAllocations(shipment);
        cancelledShortAllocationService.removeCancelledShortAllocations(shipment);

        allocationTransactionHistoryService.removeAllocationTransactionHistory(shipment);



        delete(shipment);
    }

    public List<Shipment> getOpenShipmentsForStop(Long warehouseId, String number,  String orderNumber) {
        String shipmentStatusList = Arrays.asList(
                ShipmentStatus.PENDING,
                ShipmentStatus.INPROCESS,
                ShipmentStatus.STAGED
                ).stream().map(ShipmentStatus::name).collect(Collectors.joining(","));
        return findAll(warehouseId, number, orderNumber, null, null,
                true, shipmentStatusList);

    }

    public void assignTrailerAppointment(long shipmentId, TrailerAppointment trailerAppointment) {
        Shipment shipment  = findById(shipmentId);

        if (Objects.nonNull(shipment.getStop())) {
            // the shipment has a stop, let's assign the stop to teh trailer appointment
            stopService.assignTrailerAppointment(shipment.getStop().getId(), trailerAppointment);
            return;
        }

        // see if we can group the shipment into an existing stop
        Stop stop = stopService.findMatchedStop(trailerAppointment, shipment);
        if (Objects.isNull(stop)) {
            // we didn't find any existing stop matches for this shipment
            // let's create one
            stop = stopService.createStop(shipment);

        }
        shipment.setStop(stop);
        save(shipment);

        stopService.assignTrailerAppointment(stop, trailerAppointment);

    }

    public void assignShipmentToStop(Stop newStop, List<Shipment> plannedShipments) {
        logger.debug("start to assignment shipment to stop {}",
                newStop.getNumber());
        plannedShipments.forEach(
                shipment -> {
                    logger.debug("start to assign shipment {} / {} to stop {}",
                            shipment.getId(),
                            shipment.getNumber(),
                            newStop.getNumber());
                    shipmentRepository.assignShipmentToStop(newStop.getId(), shipment.getId());

                }
        );
    }
}
