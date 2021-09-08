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
import javax.transaction.Transactional;
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
            loadAttribute(shortAllocation);
        }
        return shortAllocation;
    }

    public ShortAllocation findById(Long id) {
        return findById(id, true);
    }

    public List<ShortAllocation> findAll(boolean loadDetails) {
        return findAll(null, null,
                null, null, null,
                null, null, null, false, loadDetails);
    }

    public List<ShortAllocation> findAll() {
        return findAll(true);
    }

    public List<ShortAllocation> findAll(Long warehouseId,
                                         Long workOrderLineId, String workOrderLineIds,
                                         String itemNumber, Long orderId,  Long workOrderId,
                                         Long shipmentId, Long waveId, Boolean includeCancelledShortAllocation,
                                         boolean loadDetails) {


        List<ShortAllocation> shortAllocations =  shortAllocationRepository.findAll(
                (Root<ShortAllocation> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(warehouseId)) {

                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }

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
                    if (!Boolean.TRUE.equals(includeCancelledShortAllocation)) {
                        // by default, we will skip the cancelled short allocation
                        predicates.add(criteriaBuilder.notEqual(root.get("status"), ShortAllocationStatus.CANCELLED));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (shortAllocations.size() > 0 && loadDetails) {
            loadAttribute(shortAllocations);
        }
        return shortAllocations;
    }



    public List<ShortAllocation> findAll(Long warehouseId,
                                         Long workOrderLineId, String workOrderLineIds,
                                         String itemNumber, Long orderId,
                                         Long workOrderId, Long shipmentId, Long waveId,
                                         Boolean includeCancelledShortAllocation) {
        return findAll(warehouseId, workOrderLineId, workOrderLineIds,
                itemNumber, orderId,  workOrderId,
                shipmentId, waveId, includeCancelledShortAllocation,
                true);
    }

    public List<ShortAllocation> findByOrder(Order order) {
        return findAll(order.getWarehouseId(), null, null,
                null, order.getId(),  null, null, null, null);
    }

    public List<ShortAllocation> findByShipment(Shipment shipment) {
        return findAll(shipment.getWarehouseId(), null, null,
                null, null,  null, shipment.getId(), null, null);
    }


    public void loadAttribute(List<ShortAllocation> shortAllocations) {
        for (ShortAllocation shortAllocation : shortAllocations) {
            loadAttribute(shortAllocation);
        }
    }

    public void loadAttribute(ShortAllocation shortAllocation) {

        // Load the item and inventory status information for each lines
        if (shortAllocation.getItemId() != null && shortAllocation.getItem() == null) {
            shortAllocation.setItem(inventoryServiceRestemplateClient.getItemById(shortAllocation.getItemId()));
        }
        if (Objects.nonNull(shortAllocation.getShipmentLine())) {
            shortAllocation.setOrderNumber(
                    shortAllocation.getShipmentLine().getOrderNumber()
            );
        }
        if (Objects.nonNull(shortAllocation.getWorkOrderLineId())) {
            shortAllocation.setWorkOrderNumber(
                    workOrderServiceRestemplateClient.getWorkOrderById(
                            shortAllocation.getWorkOrderLineId()
                    ).getNumber()
            );
        }


    }
    public ShortAllocation save(ShortAllocation shortAllocation) {
        ShortAllocation newShortAllocation = shortAllocationRepository.save(shortAllocation);
        loadAttribute(newShortAllocation);
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

        if (readyToRemoveShortAllocation(shortAllocation)) {

            // OK, the short allocation doesn't have any emergency replenishment related
            // and the open quantity is already 0, which means we already allocated
            // enough picks from the location, which means we can safely remove the
            // short allocation
            logger.debug("will remove short allocation id: {}", shortAllocation.getId());
            delete(shortAllocation);
            return null;
        }
        else  {

            // After allocation, we either have picks instead of short allocation
            // or we have emergency replenishment
            return resetShortAllocationStatus(shortAllocation);
        }

    }

    /**
     * Check if we can remove the short allocation, only when
     * 1. There's no open quantity
     * 2. There's no open emergency replenishment for the short allocation
     * Then we know that we actually allocate picks instead of emergency replenishment
     * from this short allocation
     * @param shortAllocation
     * @return
     */
    private boolean readyToRemoveShortAllocation(ShortAllocation shortAllocation) {

        if (shortAllocation.getOpenQuantity() > 0) {
            logger.debug("current short allocation id {} still have open quantity {}, NOT ready for removing",
                    shortAllocation.getId(), shortAllocation.getOpenQuantity());
            return false;
        }
        // Let's make sure there's no open emergency replenishment relate to this
        // short allocation
        if (shortAllocation.getPicks().size() == 0) {
            logger.debug("current short allocation id {}  has NO emergency replenishment, READY for removing",
                    shortAllocation.getId());
            return true;
        }
        for(Pick pick: shortAllocation.getPicks()) {
            if (pick.getPickedQuantity() < pick.getQuantity()) {
                logger.debug("current short allocation id {} still have open pick {}, NOT ready for removing",
                        shortAllocation.getId(), pick.getNumber());
                return false;
            }
        }
        logger.debug("current short allocation id {}  pass all checks, READY for removing",
                shortAllocation.getId());
        return true;
    }

    private ShortAllocation resetShortAllocationStatus(ShortAllocation shortAllocation) {
        switch (shortAllocation.getStatus()) {
            case PENDING:
                if (shortAllocation.getInprocessQuantity() > 0 ||
                    shortAllocation.getDeliveredQuantity() > 0) {

                    shortAllocation.setStatus(ShortAllocationStatus.INPROCESS);
                }
                break;
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
                if (shortAllocation.getOpenQuantity()  + shortAllocation.getInprocessQuantity() <= 0 &&
                    shortAllocation.getDeliveredQuantity() > 0) {
                    shortAllocation.setStatus(ShortAllocationStatus.COMPLETED);
                }
                break;
            case COMPLETED:
                // If the short allocation is already complete, which means we already
                // finish the emergency replenishment. Let's reset the status to PENDING
                // so the allocation logic will pickup this short allocaiton again and
                // may allocate from the replenished inventory
                shortAllocation.setStatus(ShortAllocationStatus.PENDING);
                shortAllocation.setOpenQuantity(shortAllocation.getQuantity());
                shortAllocation.setInprocessQuantity(0L);
                shortAllocation.setDeliveredQuantity(0L);
                break;
        }
        return save(shortAllocation);

    }



    public boolean isAllocatable(ShortAllocation shortAllocation) {
        return shortAllocation.getStatus().equals(ShortAllocationStatus.PENDING) ||
                shortAllocation.getStatus().equals(ShortAllocationStatus.INPROCESS);
    }


    public void registerPickCancelled(ShortAllocation shortAllocation, Long cancelledQuantity) {
        shortAllocation.setOpenQuantity(shortAllocation.getOpenQuantity() + cancelledQuantity);
        shortAllocation.setInprocessQuantity(shortAllocation.getInprocessQuantity() - cancelledQuantity);
        save(shortAllocation);
    }

    public void processShortAllocation(ShortAllocation shortAllocation) {
        logger.debug("# Start to process short allocation id {} @ {}, its status is {}",
                shortAllocation.getId(), LocalDateTime.now(), shortAllocation.getStatus());
        if (shortAllocation.getStatus().equals(ShortAllocationStatus.CANCELLED)) {
            return;
        }

        shortAllocation = resetShortAllocationStatus(shortAllocation);
        if (isAllocatable(shortAllocation) && shortAllocation.getOpenQuantity() > 0) {
            allocateShortAllocation(shortAllocation);
        }


    }

    /**
     * When a emergency replenishment has been confirmed, reset the quantity and status
     * of the emergency replenishment accordingly.
     * @param pick The emergency replenishment that has been done
     * @param quantityToBePicked Quantites that has been replenished into destination
     */
    public void processPickConfirmed(Pick pick, Long quantityToBePicked) {
        logger.debug("Will reset short allocation's quantity after pick {} confirmed",
                pick.getNumber());
        ShortAllocation shortAllocation = pick.getShortAllocation();
        if (Objects.isNull(shortAllocation)) {
            logger.debug("The pick doesn't have any short allocation related");
            return;
        }
        logger.debug("# after the pick is confirmed, we will reset short allocation id {} @ {}, its status is {}",
                shortAllocation.getId(), LocalDateTime.now(), shortAllocation.getStatus());

        shortAllocation.setDeliveredQuantity(
                shortAllocation.getDeliveredQuantity() + quantityToBePicked
        );
        logger.debug("Will reset short allocation {} number based on ", shortAllocation.getId());
        logger.debug("quantityToBePicked: {}， shortAllocation.getInprocessQuantity(): {}",
                quantityToBePicked, shortAllocation.getInprocessQuantity());
        if (quantityToBePicked > shortAllocation.getInprocessQuantity()) {
            // we delivered more than necessary, which normally is true
            // for most shortage
            shortAllocation.setInprocessQuantity(0L);
        }
        else {

            shortAllocation.setInprocessQuantity(
                    shortAllocation.getInprocessQuantity() - quantityToBePicked
            );
        }

        logger.debug(">>>>>><<<<<<<");

        // reset the status of the short allocation
        // which will persist the change as well
        shortAllocation = resetShortAllocationStatus(shortAllocation);

        logger.debug(">>>>>><<<<<<<");
        logger.debug("# after reset short allocation id {} @ {}, its status is {}",
                shortAllocation.getId(), LocalDateTime.now(), shortAllocation.getStatus());

    }


    /**
     * Remove the cancelled short allocation. Note if the short allocation is active, then
     * we will need to cancel it first,
     * @param shipment
     */
    @Transactional
    public void removeCancelledShortAllocations(Shipment shipment) {
        List<ShortAllocation> shortAllocations = findByShipment(shipment);
        if (shortAllocations.size() > 0) {
            shortAllocations.stream().filter(
                    shortAllocation -> shortAllocation.getStatus().equals(ShortAllocationStatus.CANCELLED)
            ).forEach(
                    shortAllocation -> delete(shortAllocation)
            );
        }
    }
}
