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
import com.garyzhangscm.cwms.outbound.clients.WorkOrderServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.exception.ShortAllocationException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.PickRepository;
import com.garyzhangscm.cwms.outbound.repository.ShortAllocationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class ShortAllocationService {
    private static final Logger logger = LoggerFactory.getLogger(ShortAllocationService.class);

    @Autowired
    private ShortAllocationRepository shortAllocationRepository;
    @Autowired
    private PickService pickService;
    @Autowired
    private AllocationConfigurationService allocationConfigurationService;
    @Autowired
    private CancelledShortAllocationService cancelledShortAllocationService;

    @Autowired
    private ShipmentLineService shipmentLineService;
    @Autowired
    private ShortAllocationConfigurationService shortAllocationConfigurationService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WorkOrderServiceRestemplateClient workOrderServiceRestemplateClient;


    public ShortAllocation findById(Long id, boolean loadDetails) {
        ShortAllocation shortAllocation
                = shortAllocationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("short allocation not found by id: " + id));
        if (loadDetails) {
            loadOrderAttribute(shortAllocation);
        }
        return shortAllocation;
    }

    public ShortAllocation findById(Long id) {
        return findById(id, true);
    }


    public List<ShortAllocation> findAll(Long warehouseId,
                                         Long workOrderLineId, String workOrderLineIds,
                                         String itemNumber, Long orderId,  Long workOrderId,
                                         Long shipmentId, Long waveId, boolean loadDetails) {


        List<ShortAllocation> shortAllocations =  shortAllocationRepository.findAll(
                (Root<ShortAllocation> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(workOrderLineId)) {
                        predicates.add(criteriaBuilder.equal(root.get("workOrderLineId"), workOrderLineId));
                    }
                    else if (StringUtils.isNotBlank(workOrderLineIds)){
                        CriteriaBuilder.In<Long> inWorkOrderLineIds = criteriaBuilder.in(root.get("workOrderLineId"));
                        for(String id : workOrderLineIds.split(",")) {
                            inWorkOrderLineIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inWorkOrderLineIds));
                    }

                    if (StringUtils.isNotBlank(itemNumber)) {
                        Item item = inventoryServiceRestemplateClient.getItemByName(warehouseId, itemNumber);
                        predicates.add(criteriaBuilder.equal(root.get("itemId"), item.getId()));
                    }

                    if (Objects.nonNull(orderId)) {

                        Join<ShortAllocation, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, OrderLine> joinOrderLine = joinShipmentLine.join("orderLine", JoinType.INNER);
                        Join<OrderLine, Order> joinOrder = joinOrderLine.join("order", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinOrder.get("id"), orderId));

                    }

                    if (Objects.nonNull(shipmentId)) {

                        Join<ShortAllocation, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, Shipment> joinShipment = joinShipmentLine.join("shipment", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinShipment.get("id"), shipmentId));
                    }

                    if (Objects.nonNull(waveId)) {

                        Join<ShortAllocation, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, Wave> joinWave = joinShipmentLine.join("wave", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinWave.get("id"), waveId));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (shortAllocations.size() > 0 && loadDetails) {
            loadOrderAttribute(shortAllocations);
        }
        return shortAllocations;
    }

    public List<ShortAllocation> findAll(Long warehouseId,
                                         Long workOrderLineId, String workOrderLineIds,
                                         String itemNumber, Long orderId,  Long workOrderId, Long shipmentId, Long waveId) {
        return findAll(warehouseId, workOrderLineId, workOrderLineIds,
                itemNumber, orderId,  workOrderId, shipmentId, waveId, true);
    }

    public void loadOrderAttribute(List<ShortAllocation> shortAllocations) {
        for (ShortAllocation shortAllocation : shortAllocations) {
            loadOrderAttribute(shortAllocation);
        }
    }

    public void loadOrderAttribute(ShortAllocation shortAllocation) {

        // Load the item and inventory status information for each lines
        if (shortAllocation.getItemId() != null && shortAllocation.getItem() == null) {
            shortAllocation.setItem(inventoryServiceRestemplateClient.getItemById(shortAllocation.getItemId()));
        }


    }
    public ShortAllocation save(ShortAllocation shortAllocation) {
        ShortAllocation newShortAllocation = shortAllocationRepository.save(shortAllocation);
        loadOrderAttribute(newShortAllocation);
        return newShortAllocation;
    }

    public void delete(ShortAllocation shortAllocation) {
        shortAllocationRepository.delete(shortAllocation);
    }

    public void delete(Long id) {
        shortAllocationRepository.deleteById(id);
    }


    public List<ShortAllocation> cancelShortAllocations(String ShortAllocationIds) {

        return Arrays.stream(ShortAllocationIds.split(",")).mapToLong(Long::parseLong).mapToObj(this::cancelShortAllocation).collect(Collectors.toList());
    }

    public ShortAllocation cancelShortAllocation(Long id) {
        return cancelShortAllocation(findById(id));
    }
    public ShortAllocation cancelShortAllocation(ShortAllocation shortAllocation) {
        if (shortAllocation.getStatus().equals(ShortAllocationStatus.COMPLETED)) {
            throw ShortAllocationException.raiseException("Can't cancel short allocation that is already cancelled");
        }


        return cancelShortAllocation(shortAllocation, shortAllocation.getQuantity() - shortAllocation.getDeliveredQuantity());
    }

    public ShortAllocation cancelShortAllocation(ShortAllocation shortAllocation, Long cancelledQuantity) {
        // we have nothing left to cancel
        if (cancelledQuantity == 0) {
            return shortAllocation;
        }
        shortAllocation.setStatus(ShortAllocationStatus.CANCELLED);

        // Return the quantity back to shipment line / work order line
        registerShortAllocationCancelled(shortAllocation, cancelledQuantity);

        return save(shortAllocation);

    }

    private void registerShortAllocationCancelled(ShortAllocation shortAllocation, Long cancelledQuantity) {
        if (shortAllocation.getShipmentLine() != null) {
            shipmentLineService.registerShortAllocationCancelled(shortAllocation.getShipmentLine(), shortAllocation.getQuantity());
        }
        else if (shortAllocation.getWorkOrderLineId() != null) {
            workOrderServiceRestemplateClient.registerShortAllocationCancelled(shortAllocation.getWorkOrderLineId(), shortAllocation.getQuantity());
        }

        // When we cancel the short allocation, do we still want to finish the
        // related emergency replenishment picks, if the picks are already
        // generated?
        // Yes: Then next time when the orders with same item comes in, we won't
        //      short again
        // No: we will cancel the related pick so that we won't bother pick now,
        //     as we don't know when the order with the same
        //     item will come in again.
        // For now, we will choose 'No' as it won't require us to do anything at this moment.
        // The drawback of this choice is that, next time when we allocate a short allocation,
        // we may need to consider the existing emergency picks as well

        cancelledShortAllocationService.registerShortAllocationCancelled(shortAllocation, cancelledQuantity);

    }

    public ShortAllocation generateShortAllocation(Item item, ShipmentLine shipmentLine, Long quantity) {

        ShortAllocation shortAllocation = new ShortAllocation();
        shortAllocation.setItem(item);
        shortAllocation.setItemId(item.getId());
        shortAllocation.setQuantity(quantity);
        shortAllocation.setOpenQuantity(quantity);
        shortAllocation.setInprocessQuantity(0L);
        shortAllocation.setDeliveredQuantity(0L);
        shortAllocation.setShipmentLine(shipmentLine);
        shortAllocation.setStatus(ShortAllocationStatus.PENDING);
        shortAllocation.setWarehouseId(shipmentLine.getWarehouseId());
        shortAllocation.setAllocationCount(0L);

        return save(shortAllocation);

    }

    public ShortAllocation generateShortAllocation(WorkOrder workOrder, Item item, WorkOrderLine workOrderLine, Long quantity) {

        ShortAllocation shortAllocation = new ShortAllocation();
        shortAllocation.setItem(item);
        shortAllocation.setItemId(item.getId());
        shortAllocation.setQuantity(quantity);
        shortAllocation.setOpenQuantity(quantity);
        shortAllocation.setInprocessQuantity(0L);
        shortAllocation.setDeliveredQuantity(0L);
        shortAllocation.setStatus(ShortAllocationStatus.PENDING);
        shortAllocation.setWarehouseId(workOrder.getWarehouseId());
        shortAllocation.setWorkOrderLineId(workOrderLine.getId());
        shortAllocation.setAllocationCount(0L);

        return save(shortAllocation);

    }

    public ShortAllocation allocateShortAllocation(Long id) {
        return allocateShortAllocation(findById(id));
    }
    public ShortAllocation allocateShortAllocation(ShortAllocation shortAllocation) {
        logger.debug("Start to allocate short allocation: {} / {}",
                shortAllocation.getItem(), shortAllocation.getItem().getItemFamily());
        if (!isAllocatable(shortAllocation) || shortAllocation.getOpenQuantity() <= 0) {
            logger.debug("short allocation is not allocatable! is allocatable? {}, open quantity? {}",
                    isAllocatable(shortAllocation), shortAllocation.getOpenQuantity());
            return shortAllocation;
        }
        shortAllocation =  allocationConfigurationService.allocate(shortAllocation);

        return resetShortAllocationStatus(shortAllocation);
    }

    private ShortAllocation resetShortAllocationStatus(ShortAllocation shortAllocation) {
        switch (shortAllocation.getStatus()) {
            case ALLOCATION_FAIL:
                // OK, it seems we were fail to full fill the short allocation with either
                // pick or emergency replenishment during last round of allocation, let's reset
                // the status back to Inprocess so the background job can pickup this
                // short allocation again
                ShortAllocationConfiguration shortAllocationConfiguration =
                        shortAllocationConfigurationService.getShortAllocationConfiguration(
                                shortAllocation.getWarehouseId());

                if (Objects.nonNull(shortAllocationConfiguration) ||
                        shortAllocationConfiguration.getEnabled()) {
                    // let's see if we are good to reset the short allocation's status
                    Long retryInterval = shortAllocationConfiguration.getRetryInterval();
                    // see how many seconds has been passed since we last fail
                    if (Objects.isNull(shortAllocation.getLastAllocationDatetime())) {
                        shortAllocation.setLastAllocationDatetime(LocalDateTime.now());
                    }
                    else if (shortAllocation.getLastAllocationDatetime().
                            plusSeconds(retryInterval).isBefore(LocalDateTime.now())){
                        shortAllocation.setStatus(ShortAllocationStatus.INPROCESS);
                    }
                }
                break;

            case INPROCESS:
                // Let's see if we can complete this short allocation
                if (shortAllocation.getOpenQuantity() == 0 &&
                    shortAllocation.getDeliveredQuantity() >= shortAllocation.getInprocessQuantity()) {
                    shortAllocation.setStatus(ShortAllocationStatus.COMPLETED);
                }
                break;
        }
        return save(shortAllocation);

    }



    private boolean isAllocatable(ShortAllocation shortAllocation) {
        return shortAllocation.getStatus().equals(ShortAllocationStatus.PENDING) ||
                shortAllocation.getStatus().equals(ShortAllocationStatus.INPROCESS);
    }


    public void registerPickCancelled(ShortAllocation shortAllocation, Long cancelledQuantity) {
        shortAllocation.setOpenQuantity(shortAllocation.getOpenQuantity() + cancelledQuantity);
        shortAllocation.setInprocessQuantity(shortAllocation.getInprocessQuantity() - cancelledQuantity);
        save(shortAllocation);
    }
}
