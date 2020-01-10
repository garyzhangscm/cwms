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
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.ShipmentRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


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
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public Shipment findById(Long id) {
        return shipmentRepository.findById(id).orElse(null);
    }


    public List<Shipment> findAll(String number ) {
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
        return shipments;
    }

    public Shipment findByNumber(String number) {
        return shipmentRepository.findByNumber(number);
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

    public void cancelShipment(Shipment shipment) {

    }
    public void cancelShipment(Long id) {
        cancelShipment(findById(id));

    }

    public List<Shipment> planShipments(Wave wave, List<OrderLine> orderLines){
        // Let's split the list of order line by order number first

        Map<Order, List<OrderLine>> orderListMap = new HashMap<>();
        orderLines.forEach(orderLine ->
                {
                    List<OrderLine> existingOrderLines = orderListMap.getOrDefault(orderLine.getOrder(), new ArrayList<>());
                    existingOrderLines.add(orderLine);
                    orderListMap.put(orderLine.getOrder(), existingOrderLines);
                });

        List<Shipment> shipments = new ArrayList<>();
        orderListMap.entrySet().stream().forEach(orderListEntry ->
            shipments.add(createShipment(wave, orderListEntry.getKey(), orderListEntry.getValue()))
        );

        return shipments;
    }
    public List<Shipment> planShipments(List<OrderLine> orderLines){
        // Let's split the list of order line by order number first

        Map<Order, List<OrderLine>> orderListMap = new HashMap<>();
        orderLines.forEach(orderLine ->
        {
            List<OrderLine> existingOrderLines = orderListMap.getOrDefault(orderLine.getOrder(), new ArrayList<>());
            existingOrderLines.add(orderLine);
            orderListMap.put(orderLine.getOrder(), existingOrderLines);
        });

        List<Shipment> shipments = new ArrayList<>();
        orderListMap.entrySet().stream().forEach(orderListEntry ->
                {
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
        shipment.setOrder(order);
        shipment.setStatus(ShipmentStatus.PENDING);

        Shipment newShipment = save(shipment);

        orderLines.forEach(orderLine -> shipment.getShipmentLines().add(shipmentLineService.createShipmentLine(wave, newShipment, orderLine)));

        return newShipment;
    }

    private String getNextShipmentNumber(){
        return commonServiceRestemplateClient.getNextNumber("shipment_number");
    }

}
