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
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.ShipmentLineRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShipmentLineService {
    private static final Logger logger = LoggerFactory.getLogger(ShipmentLineService.class);

    @Autowired
    private ShipmentLineRepository shipmentLineRepository;
    @Autowired
    private OrderLineService orderLineService;
    @Autowired
    private OrderActivityService orderActivityService;
    @Autowired
    private PickService pickService;
    @Autowired
    private ShortAllocationService shortAllocationService;
    @Autowired
    private AllocationConfigurationService allocationConfigurationService;
    @Autowired
    private ShipmentService shipmentService;
    @Autowired
    private AllocationService allocationService;


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
                                      String orderNumber, Long orderLineId,
                                      Long orderId, Long waveId,
                                      String orderLineIds) {
        return findAll(warehouseId, number,
                orderNumber, orderLineId, orderId, waveId, orderLineIds, true);
    }

    public List<ShipmentLine> findAll(Long warehouseId, String number,
                                       String orderNumber, Long orderLineId,
                                      Long orderId, Long waveId,
                                      String orderLineIds,
                                      boolean loadDetails) {
        List<ShipmentLine> shipmentLines =  shipmentLineRepository.findAll(
                (Root<ShipmentLine> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (!StringUtils.isBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));
                    }

                    if (!StringUtils.isBlank(orderNumber)) {
                        Join<ShipmentLine, OrderLine> joinOrderLine = root.join("orderLine", JoinType.INNER);
                        Join<OrderLine, Order> joinOrder = joinOrderLine.join("order", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinOrder.get("number"), orderNumber));
                    }

                    if (Objects.nonNull(orderLineId)) {
                        Join<ShipmentLine, OrderLine> joinOrderLine = root.join("orderLine", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinOrderLine.get("id"), orderLineId));

                    }
                    if (Objects.nonNull(orderId)) {
                        Join<ShipmentLine, OrderLine> joinOrderLine = root.join("orderLine", JoinType.INNER);
                        Join<OrderLine, Order> joinOrder = joinOrderLine.join("order", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinOrder.get("id"), orderId));

                    }
                    if (Objects.nonNull(waveId)) {
                        Join<ShipmentLine, Wave> joinWave = root.join("wave", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinWave.get("id"), waveId));

                    }
                    if (Strings.isNotBlank(orderLineIds)) {

                        Join<ShipmentLine, OrderLine> joinOrderLine = root.join("orderLine", JoinType.INNER);

                        CriteriaBuilder.In<Long> inOrderLineIds = criteriaBuilder.in(joinOrderLine.get("id"));
                        for(String id : orderLineIds.split(",")) {
                            inOrderLineIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inOrderLineIds));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (loadDetails && !shipmentLines.isEmpty()) {
            loadAttribute(shipmentLines);
        }

        return shipmentLines.stream().distinct().collect(Collectors.toList());
    }

    private void loadAttribute(List<ShipmentLine> shipmentLines) {
        shipmentLines.stream().forEach(this::loadAttribute);
    }
    private void loadAttribute(ShipmentLine shipmentLine) {
        if (shipmentLine.getOrderLine() != null) {
            orderLineService.loadOrderLineAttribute(shipmentLine.getOrderLine());
        }
    }
    public List<ShipmentLine> findByOrderLine(OrderLine orderLine) {
        return findAll(orderLine.getWarehouseId(), null, null, orderLine.getId(), null, null, null);
    }
    public List<ShipmentLine> findByOrderLineId(Long warehouseId, Long orderLineId) {
        return findAll(warehouseId, null, null, orderLineId, null, null, null);
    }
    public List<ShipmentLine> findByOrderLineIds(Long warehouseId, String orderLineIds) {
        return findAll(warehouseId, null, null, null, null, null, orderLineIds);
    }



    public List<ShipmentLine> findByOrder(Order order) {
        return findAll(order.getWarehouseId(), null, order.getNumber(), null, null, null, null);
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
    public ShipmentLine createShipmentLine(Shipment shipment, OrderLine orderLine, Long shipmentLineQuantity) {
        return createShipmentLine(null, shipment, orderLine, shipmentLineQuantity);
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
        shipmentLine = save(shipmentLine);
        logger.debug("Save shipment line # {} with wave # {}",
                shipmentLine.getId(),
                Objects.isNull(wave) ? "N/A" : wave.getNumber());
        return shipmentLine;
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


    @Transactional
    public AllocationResult allocateShipmentLine(ShipmentLine shipmentLine) {
        logger.debug("Start to allocate shipment line: {} / {}", shipmentLine.getId(), shipmentLine.getNumber());
        if (!isAllocatable(shipmentLine) || shipmentLine.getOpenQuantity() <= 0) {
            logger.debug("Shipment line is not allocatable! is allocatable? {}, open quantity? {}",
                            isAllocatable(shipmentLine), shipmentLine.getOpenQuantity());
            return new AllocationResult();
        }

        // AllocationResult allocationResult = allocationConfigurationService.allocate(shipmentLine);
        loadAttribute(shipmentLine);

        AllocationResult allocationResult = allocationService.allocate(shipmentLine);

        OrderActivity orderActivity =
                orderActivityService.createOrderActivity(shipmentLine.getWarehouseId(),
                shipmentLine, OrderActivityType.SHIPMENT_ALLOCATION);
        // Move the open quantity into the in process quantity and start allocation
        logger.debug("Allocation Step 1: Move quantity {} from open quantity to in process quantity",
                shipmentLine.getOpenQuantity());

        shipmentLine.setInprocessQuantity(shipmentLine.getInprocessQuantity() + shipmentLine.getOpenQuantity());
        shipmentLine.setOpenQuantity(0L);
        // setup the shipment line's destination location based on the pick's staging location
        for (Pick pick : allocationResult.getPicks()) {
            shipmentLine.setStageLocationId(pick.getDestinationLocationId());
        }

        orderActivity.setQuantityByNewShipmentLine(shipmentLine);
        orderActivityService.sendOrderActivity(orderActivity);


        save(shipmentLine);

        // Change the shipment's status to 'In Process'
        if (!shipmentLine.getShipment().getStatus().equals(ShipmentStatus.INPROCESS)) {
            logger.debug("Will need to save the shipment {} to in process",
                    shipmentLine.getShipment().getNumber());
            shipmentLine.getShipment().setStatus(ShipmentStatus.INPROCESS);
            shipmentService.save(shipmentLine.getShipment());
        }
        logger.debug("get allocation result for the shipment line {} / {}",
                shipmentLine.getShipment().getNumber(),
                shipmentLine.getNumber());
        // logger.debug(allocationResult.toString());
        return allocationResult;
    }

    public boolean isAllocatable(ShipmentLine shipmentLine) {
        return shipmentLine.getShipment().getStatus().equals(ShipmentStatus.PENDING) ||
                shipmentLine.getShipment().getStatus().equals(ShipmentStatus.INPROCESS);
    }

    /**
     * Recalculate the quantity of the shipment. Note when this function is called, the4 pick
     * has not been cancelled yet
     * @param shipmentLine
     * @param cancelledQuantity
     */
    @Transactional
    public ShipmentLine registerPickCancelled(ShipmentLine shipmentLine, Long cancelledQuantity) {
        logger.debug("registerPickCancelled: shipment line: {}, cancelledQuantity: {}",
                shipmentLine.getId(), cancelledQuantity);


        // return the quantity to the open quantity and not exceed
        // the original total quantity
        Long newOpenQuantity = Math.min(
                shipmentLine.getQuantity(),
                shipmentLine.getOpenQuantity() + cancelledQuantity
        );
        shipmentLine.setOpenQuantity(newOpenQuantity);
        Long newInprocessQuantity = Math.max(
                0l,
                shipmentLine.getInprocessQuantity() - cancelledQuantity
        );
        shipmentLine.setInprocessQuantity(newInprocessQuantity);

        shipmentLine = save(shipmentLine);
        logger.debug("after pick cancelled, shipment line {} has open quantity {}, in process quantity: {}",
                shipmentLine.getNumber(), shipmentLine.getOpenQuantity(), shipmentLine.getInprocessQuantity());

        return shipmentLine;
    }

    /**
     * Recalculate teh shipment's quantity based on the pick been canceled.
     * Please note when this function is called, the pick has not been cancelled yet so the getPicksByShipmentLine
     * function will return the pick being cancelled as well. We will need to exclude this pick when
     * recalculating the quantity
     * @param shipmentLine
     * @param cancelledQuantity
     * @return
     */
    private Long recalculateInprocessQuantity(ShipmentLine shipmentLine, long cancelledQuantity) {
        // Get all the open pick quantities
        List<Pick> picks = pickService.getPicksByShipmentLine(shipmentLine.getId());
        logger.debug("we get {} picks for this shipment line {}",
                picks.size(), shipmentLine.getId());
        Long pickQuantity = picks.stream().mapToLong(pick -> pick.getQuantity()).sum();
        logger.debug("total allocated quantity {} for those picks", pickQuantity);
        logger.debug("quantity to be cancelled {}", cancelledQuantity);
        logger.debug("loaded quantity {}", shipmentLine.getLoadedQuantity());
        logger.debug("shippped quantity {}", shipmentLine.getShippedQuantity());

        return pickQuantity - cancelledQuantity - shipmentLine.getLoadedQuantity()
                - shipmentLine.getShippedQuantity();

    }

    private Long getActualInprocessQuantity(ShipmentLine shipmentLine) {

        // Get all the open pick quantities
        Long pickQuantity = shipmentLine.getPicks().stream().mapToLong(pick -> pick.getQuantity()).sum();

        // Get all the open pick quantities
        Long shortQuantity = shipmentLine.getShortAllocations().stream().mapToLong(
                shortAllocation -> shortAllocation.getQuantity()).sum();

        return (Objects.isNull(pickQuantity) ? 0 : pickQuantity)  +
                (Objects.isNull(shortQuantity) ? 0 : shortQuantity);
    }

    @Transactional
    public ShipmentLine registerShortAllocationCancelled(ShipmentLine shipmentLine, Long cancelledQuantity) {
        logger.debug("registerShortAllocationCancelled: shipment line: {}, cancelledQuantity: {}",
                shipmentLine.getNumber(), cancelledQuantity);
        // we will not allow cancel more than the current inprocess quantity
        if (cancelledQuantity > shipmentLine.getInprocessQuantity()) {
            cancelledQuantity = shipmentLine.getInprocessQuantity();
        }
        shipmentLine.setOpenQuantity(shipmentLine.getOpenQuantity() + cancelledQuantity);
        shipmentLine.setInprocessQuantity(shipmentLine.getInprocessQuantity() - cancelledQuantity);
        shipmentLine = save(shipmentLine);
        logger.debug("after pick cancelled, shipment line {} has open quantity {}, in process quantity: {}",
                shipmentLine.getNumber(), shipmentLine.getOpenQuantity(), shipmentLine.getInprocessQuantity());

        return shipmentLine;
    }

    @Transactional
    // Complete the shipment line
    public void completeShipmentLine(ShipmentLine shipmentLine) {
        // Move the loaded quantity to shipped quantity
        Long quantity = shipmentLine.getLoadedQuantity() +
                shipmentLine.getInprocessQuantity();
        shipmentLine.setLoadedQuantity(0L);
        shipmentLine.setShippedQuantity(quantity);
        shipmentLine = save(shipmentLine);
        orderLineService.registerShipmentLineComplete(shipmentLine, quantity);
    }


    public ShipmentLine getShipmentLineByPickedInventory(Inventory inventory) {
        logger.debug("getShipmentLineByPickedInventory: \n {}", inventory);
        if (Objects.nonNull(inventory.getPick())) {
            logger.debug("Will find by pick: {}", inventory.getPick());
            Optional<ShipmentLine> shipmentLineOptional = Optional.ofNullable(inventory.getPick().getShipmentLine());
            return shipmentLineOptional.orElse(pickService.findById(inventory.getPick().getId()).getShipmentLine());
        }
        else if (Objects.nonNull(inventory.getPickId())){
            logger.debug("Will find by pick id: {}", inventory.getPickId());
            return pickService.findById(inventory.getPickId()).getShipmentLine();
        }
        else {
            return null;
        }
    }



    public void shippingPackage(ShipmentLine shipmentLine, Inventory inventory) {
        shipmentLine.setInprocessQuantity(shipmentLine.getInprocessQuantity() - inventory.getQuantity());
        shipmentLine.setShippedQuantity(shipmentLine.getShippedQuantity() + inventory.getQuantity());
        save(shipmentLine);

        OrderLine orderLine = shipmentLine.getOrderLine();
        orderLineService.shippingPackage(orderLine, inventory);

    }

    public void cancelShipmentLine(Long id) {

        cancelShipmentLine(findById(id));
    }
    public void cancelShipmentLine(ShipmentLine shipmentLine) {

        // return the quantity back to order line
        orderLineService.registerShipmentLineCancelled(shipmentLine.getOrderLine(), shipmentLine.getQuantity());

        shipmentLine.setStatus(ShipmentLineStatus.CANCELLED);
        // remove the shipment from the wave and order
        shipmentLine.setWave(null);
        shipmentLine.setOrderLine(null);
        save(shipmentLine);



    }

    /**
     * Deassign shipment line from the wave
     * @param wave
     * @param shipmentLineId
     */
    public ShipmentLine deassignShipmentLineFromWave(Wave wave, Long shipmentLineId) {
        ShipmentLine shipmentLine = findById(shipmentLineId);
        if (Objects.nonNull(shipmentLine.getWave()) &&
                !shipmentLine.getWave().getId().equals(wave.getId())) {
            throw OrderOperationException.raiseException("Can't deassign shipment  " +
                    shipmentLine.getShipmentNumber() + " / line " + shipmentLine.getNumber() +
                    " from wave " + wave.getNumber() + " as it belongs to another wave " +
                    shipmentLine.getWave().getNumber());
        }
        shipmentLine.setWave(null);

        return save(shipmentLine);

    }
}
