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
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.ShipmentRepository;
import com.garyzhangscm.cwms.outbound.utils.UserContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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

        Shipment shipment = shipmentRepository.findById(id).orElse(null);
        if (shipment != null && includeDetails) {
            loadAttribute(shipment);
        }
        return shipment;
    }


    public List<Shipment> findAll(String number) {
        return findAll(number, true);
    }
    public List<Shipment> findAll(String number, boolean includeDetails) {
        List<Shipment> shipments;

        if (StringUtils.isBlank(number)) {
            shipments = shipmentRepository.findAll();
        } else {
            Shipment shipment = shipmentRepository.findByNumber(number);
            if (shipment != null) {
                shipments = Arrays.asList(new Shipment[]{shipment});
            } else {
                shipments = new ArrayList<>();
            }
        }
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
    public List<Shipment> planShipments(Wave wave, List<OrderLine> orderLines){
        // Let's split the list of order line by order number first

        Map<Order, List<OrderLine>> orderListMap = new HashMap<>();
        orderLines.forEach(orderLine ->
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
            shipments.add(createShipment(wave, orderListEntry.getKey(), orderListEntry.getValue()));

        });

        return shipments;
    }
    public List<Shipment> planShipments(List<OrderLine> orderLines){
        // Let's split the list of order line by order number first

        Map<Order, List<OrderLine>> orderListMap = new HashMap<>();
        orderLines.forEach(orderLine ->
        {
            // in case the order line doesn't have the order setup yet, load the order line again from the db
            Order order = orderLineService.findById(orderLine.getId()).getOrder();
            orderLine.setOrder(order);
            logger.debug("Find order: {} for order line: {}", order.getNumber(), orderLine.getId());
            List<OrderLine> existingOrderLines = orderListMap.getOrDefault(order, new ArrayList<>());
            existingOrderLines.add(orderLine);
            orderListMap.put(order, existingOrderLines);
        });

        List<Shipment> shipments = new ArrayList<>();
        orderListMap.entrySet().stream().forEach(orderListEntry ->
                {
                    logger.debug("Start to process order line entry: key: {}, value: {}", orderListEntry.getKey().getNumber(), orderListEntry.getValue().size());
                    // Plan one wave for each shipment, wave number will be the shipment number
                    String shipmentNumber = getNextShipmentNumber();

                    Wave wave = waveService.createWave(shipmentNumber);
                    shipments.add(createShipment(wave, shipmentNumber, orderListEntry.getKey(), orderListEntry.getValue()));

                }
        );

        return shipments;
    }



    private Shipment createShipment(Wave wave, Order order, List<OrderLine> orderLines) {
        return createShipment(wave, getNextShipmentNumber(), order, orderLines);
    }

    private Shipment createShipment(Wave wave, String shipmentNumber, Order order, List<OrderLine> orderLines) {

        Shipment shipment = new Shipment();
        shipment.setNumber(shipmentNumber);
        shipment.setStatus(ShipmentStatus.PENDING);
        shipment.setWarehouseId(order.getWarehouseId());

        Shipment newShipment = save(shipment);

        orderLines.forEach(orderLine -> shipment.getShipmentLines().add(shipmentLineService.createShipmentLine(wave, newShipment, orderLine)));

        return newShipment;
    }

    private String getNextShipmentNumber(){
        return commonServiceRestemplateClient.getNextNumber("shipment-number");
    }

    // Complete the shipment. We will automatically generate the trailer structure
    private Shipment complete(Shipment shipment) throws IOException {
        trailerService.completeShipment(shipment);
        return findById(shipment.getId());

    }

    // Get a list of inventory that is picked for the shipment
    public List<Inventory> getPickedInventory(Shipment shipment) {
        List<Pick> picks = pickService.getPicksByShipment(shipment.getId());
        return inventoryServiceRestemplateClient.getPickedInventory(picks);

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

        // check if we can mark the shipment as loaded
        // only if we don't have any in process quantity and open quantity
        Long openShipmentLineQuantity
                = shipment.getShipmentLines().stream()
                     .filter(shipmentLine -> shipmentLine.getOpenQuantity() > 0 || shipmentLine.getInprocessQuantity() > 0).count();
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

        ShipmentLine shipmentLine = pickService.findById(inventory.getPickId()).getShipmentLine();

        // Let's move the inventory onto the trailer

        Location trailerLocation =
                warehouseLayoutServiceRestemplateClient.getTrailerLocation(
                        getWarehouseName(shipmentLine.getWarehouseId()), trailer.getId());

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

    // Complete the shipment
    // If we haven't setup the outbound structure yet(stop / trailer / etc), we
    //    will let the trailer service create all the structure automatically so
    //    we can complete the shipment
    public Shipment completeShipment(Shipment shipment) {
        try {
            if (shipment.getStop() == null) {
                trailerService.completeShipment(shipment);
                shipment.setStatus(ShipmentStatus.LOADED);
                return save(shipment);
            } else if (shipment.getStop().getTrailer() != null) {
                // OK we have stop assign, let's see if we can load the shipment
                // for the trailer
                trailerService.loadShipment(shipment);
                shipment.setStatus(ShipmentStatus.LOADED);
                return save(shipment);
            }
        }
        catch (IOException ex) {
            throw new GenericException(10000, ex.getMessage());
        }
        return shipment;

    }

    public Shipment completeShipment(Long shipmentId) {
        return completeShipment(findById(shipmentId));
    }

    private String getWarehouseName(Long warehouseId) {
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId);
        if (warehouse == null) {
            return "";
        }
        else  {
            return warehouse.getName();
        }
    }


}
