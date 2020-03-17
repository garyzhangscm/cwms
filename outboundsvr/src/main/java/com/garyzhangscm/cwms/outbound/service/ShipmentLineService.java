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
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.ShipmentLineRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShipmentLineService {
    private static final Logger logger = LoggerFactory.getLogger(ShipmentLineService.class);

    @Autowired
    private ShipmentLineRepository shipmentLineRepository;
    @Autowired
    private OrderLineService orderLineService;
    @Autowired
    private PickService pickService;
    @Autowired
    private AllocationConfigurationService allocationConfigurationService;
    @Autowired
    private ShipmentService shipmentService;


    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public ShipmentLine findById(Long id) {
        return shipmentLineRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("shipment line not found by id: " + id));
    }

    public List<ShipmentLine> findByOrderNumber(Long warehouseId, String orderNumber){
        return shipmentLineRepository.findByOrderNumber(warehouseId, orderNumber);
    }
    public List<ShipmentLine> findAll(Long warehouseId, String number,
                                       String orderNumber, Long orderLineId) {
        List<ShipmentLine> shipmentLines =  shipmentLineRepository.findAll(
                (Root<ShipmentLine> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    if (warehouseId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }
                    if (!StringUtils.isBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));
                    }

                    if (!StringUtils.isBlank(orderNumber)) {
                        Join<ShipmentLine, OrderLine> joinOrderLine = root.join("orderLine", JoinType.INNER);
                        Join<OrderLine, Order> joinOrder = joinOrderLine.join("order", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinOrder.get("number"), orderNumber));
                    }

                    if (orderLineId != null) {
                        Join<ShipmentLine, OrderLine> joinOrderLine = root.join("orderLine", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinOrderLine.get("id"), orderLineId));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        return shipmentLines.stream().distinct().collect(Collectors.toList());
    }

    public List<ShipmentLine> findByOrderLine(OrderLine orderLine) {
        return findAll(orderLine.getWarehouseId(), null, null, orderLine.getId());
    }

    public List<ShipmentLine> findByOrder(Order order) {
        return findAll(order.getWarehouseId(), null, order.getNumber(), null);
    }

    public ShipmentLine save(ShipmentLine shipmentLine) {
        return shipmentLineRepository.save(shipmentLine);
    }


    public void delete(ShipmentLine shipmentLine) {
        shipmentLineRepository.delete(shipmentLine);
    }

    public void delete(Long id) {
        shipmentLineRepository.deleteById(id);
    }

    @Transactional
    public ShipmentLine createShipmentLine(Wave wave, Shipment shipment, OrderLine orderLine) {
        return createShipmentLine(wave, shipment, orderLine, orderLine.getOpenQuantity());
    }

    @Transactional
    public ShipmentLine createShipmentLine(Wave wave, Shipment shipment, OrderLine orderLine, Long shipmentLineQuantity) {
        // Deduct quantity from open quantity to inprocess quantity for the order line
        orderLineService.markQuantityAsInProcess(orderLine, shipmentLineQuantity);
        ShipmentLine shipmentLine = new ShipmentLine();
        shipmentLine.setNumber(getNextShipmentLineNumber(shipment));
        shipmentLine.setOrderLine(orderLine);
        shipmentLine.setWarehouseId(orderLine.getWarehouseId());

        // For new shipment, we will set the quantity and open quantity as the passed in quantity, to begin with
        // The in process quantity and shipped quantity will be 0.
        // After we start to process the shipment line(start from allocation), the 'quantity' will be always
        // the same as the beginning quantity. We will move 'open quantity' to 'in process quantity'
        // starting from allocation. We will move quantity from 'in process quantity' to shipped quantity only
        // when the quantity is shipped
        shipmentLine.setQuantity(shipmentLineQuantity);
        shipmentLine.setOpenQuantity(shipmentLineQuantity);
        shipmentLine.setInprocessQuantity(0L);
        shipmentLine.setLoadedQuantity(0L);
        shipmentLine.setShippedQuantity(0L);
        shipmentLine.setWave(wave);
        shipmentLine.setShipment(shipment);
        return save(shipmentLine);
    }

    private String getNextShipmentLineNumber(Shipment shipment) {
        if (shipment.getShipmentLines().isEmpty()) {
            return "0";
        } else {
            // Suppose the line number is all numeric
            int max = 0;
            for (ShipmentLine shipmentLine : shipment.getShipmentLines()) {
                try {
                    if (Integer.parseInt(shipmentLine.getNumber()) > max) {
                        max = Integer.parseInt(shipmentLine.getNumber());
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            return String.valueOf(max + 1);
        }
    }


    public AllocationResult allocateShipmentLine(ShipmentLine shipmentLine) {
        logger.debug("Start to allocate shipment line: {} / {}", shipmentLine.getId(), shipmentLine.getNumber());
        if (!isAllocatable(shipmentLine) || shipmentLine.getOpenQuantity() <= 0) {
            logger.debug("Shipment line is not allocatable! is allocatable? {}, open quantity? {}",
                            isAllocatable(shipmentLine), shipmentLine.getOpenQuantity());
            return new AllocationResult();
        }

        // Let's get all the pickable inventory and existing picking so we can start to calculate
        // how to generate picks for the shipment
        Long itemId = shipmentLine.getOrderLine().getItemId();

        List<Pick> existingPicks = pickService.getOpenPicksByItemId(itemId);
        logger.debug("We have {} existing picks against the item with id {}",
                existingPicks.size(), itemId);

        // Get all pickable inventory
        List<Inventory> pickableInventory
                = inventoryServiceRestemplateClient.getPickableInventory(
                        itemId, shipmentLine.getOrderLine().getInventoryStatusId());
        logger.debug("We have {} pickable inventory against the item with id {}",
                pickableInventory.size(), itemId);


        AllocationResult allocationResult = allocationConfigurationService.allocate(shipmentLine, existingPicks, pickableInventory);

        // Move the open quantity into the in process quantity and start allocation
        logger.debug("Allocation Step 1: Move quantity {} from open quantity to in process quantity",
                shipmentLine.getOpenQuantity());

        shipmentLine.setInprocessQuantity(shipmentLine.getInprocessQuantity() + shipmentLine.getOpenQuantity());
        shipmentLine.setOpenQuantity(0L);


        save(shipmentLine);

        // Change the shipment's status to 'In Process'
        if (!shipmentLine.getShipment().getStatus().equals(ShipmentStatus.INPROCESS)) {
            shipmentLine.getShipment().setStatus(ShipmentStatus.INPROCESS);
            shipmentService.save(shipmentLine.getShipment());
        }
        return allocationResult;
    }

    private boolean isAllocatable(ShipmentLine shipmentLine) {
        return shipmentLine.getShipment().getStatus().equals(ShipmentStatus.PENDING) ||
                shipmentLine.getShipment().getStatus().equals(ShipmentStatus.INPROCESS);
    }

    @Transactional
    public void registerPickCancelled(ShipmentLine shipmentLine, Long cancelledQuantity) {
        logger.debug("registerPickCancelled: shipment line: {}, cancelledQuantity: {}",
                shipmentLine.getNumber(), cancelledQuantity);
        shipmentLine.setOpenQuantity(shipmentLine.getOpenQuantity() + cancelledQuantity);
        shipmentLine.setInprocessQuantity(shipmentLine.getInprocessQuantity() - cancelledQuantity);
        shipmentLine = save(shipmentLine);
        logger.debug("after pick cancelled, shipment line {} has open quantity {}, in process quantity: {}",
                shipmentLine.getNumber(), shipmentLine.getOpenQuantity(), shipmentLine.getInprocessQuantity());

    }
    @Transactional
    public void registerShortAllocationCancelled(ShipmentLine shipmentLine, Long cancelledQuantity) {
        logger.debug("registerShortAllocationCancelled: shipment line: {}, cancelledQuantity: {}",
                shipmentLine.getNumber(), cancelledQuantity);
        shipmentLine.setOpenQuantity(shipmentLine.getOpenQuantity() + cancelledQuantity);
        shipmentLine.setInprocessQuantity(shipmentLine.getInprocessQuantity() - cancelledQuantity);
        shipmentLine = save(shipmentLine);
        logger.debug("after pick cancelled, shipment line {} has open quantity {}, in process quantity: {}",
                shipmentLine.getNumber(), shipmentLine.getOpenQuantity(), shipmentLine.getInprocessQuantity());

    }

    @Transactional
    // Complete the shipment line
    public void completeShipmentLine(ShipmentLine shipmentLine) {
        // Move the loaded quantity to shipped quantity
        Long quantity = shipmentLine.getLoadedQuantity();
        shipmentLine.setLoadedQuantity(0L);
        shipmentLine.setShippedQuantity(quantity);
        shipmentLine = save(shipmentLine);
        orderLineService.registerShipmentLineComplete(shipmentLine, quantity);
    }


}
