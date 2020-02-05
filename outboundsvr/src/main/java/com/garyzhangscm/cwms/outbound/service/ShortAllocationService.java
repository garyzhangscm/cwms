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
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.PickRepository;
import com.garyzhangscm.cwms.outbound.repository.ShortAllocationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public ShortAllocation findById(Long id, boolean loadDetails) {
        ShortAllocation shortAllocation = shortAllocationRepository.findById(id).orElse(null);
        if (shortAllocation != null && loadDetails) {
            loadOrderAttribute(shortAllocation);
        }
        return shortAllocation;
    }

    public ShortAllocation findById(Long id) {
        return findById(id, true);
    }


    public List<ShortAllocation> findAll(Long workOrderLineId, String workOrderLineIds, boolean loadDetails) {


        List<ShortAllocation> shortAllocations =  shortAllocationRepository.findAll(
                (Root<ShortAllocation> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (workOrderLineId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("workOrderLineId"), workOrderLineId));
                    }
                    else if (!StringUtils.isBlank(workOrderLineIds)){
                        CriteriaBuilder.In<Long> inWorkOrderLineIds = criteriaBuilder.in(root.get("workOrderLineId"));
                        for(String id : workOrderLineIds.split(",")) {
                            inWorkOrderLineIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inWorkOrderLineIds));
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

    public List<ShortAllocation> findAll(Long workOrderLineId, String workOrderLineIds) {
        return findAll(workOrderLineId, workOrderLineIds, true);
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
            throw new GenericException(10000, "Can't cancel short allocation that is already cancelled");
        }
        shortAllocation.setStatus(ShortAllocationStatus.CANCELLED);
        return save(shortAllocation);

    }

    public ShortAllocation generateShortAllocation(Item item, ShipmentLine shipmentLine, Long quantity) {

        ShortAllocation shortAllocation = new ShortAllocation();
        shortAllocation.setItem(item);
        shortAllocation.setItemId(item.getId());
        shortAllocation.setQuantity(quantity);
        shortAllocation.setShipmentLine(shipmentLine);
        shortAllocation.setStatus(ShortAllocationStatus.PENDING);
        shortAllocation.setWarehouseId(shipmentLine.getWarehouseId());

        return save(shortAllocation);

    }

    public ShortAllocation generateShortAllocation(WorkOrder workOrder, Item item, WorkOrderLine workOrderLine, Long quantity) {

        ShortAllocation shortAllocation = new ShortAllocation();
        shortAllocation.setItem(item);
        shortAllocation.setItemId(item.getId());
        shortAllocation.setQuantity(quantity);
        shortAllocation.setStatus(ShortAllocationStatus.PENDING);
        shortAllocation.setWarehouseId(workOrder.getWarehouseId());
        shortAllocation.setWorkOrderLineId(workOrderLine.getId());

        return save(shortAllocation);

    }

    public ShortAllocation allocateShortAllocation(ShortAllocation shortAllocation) {
        logger.debug("Start to allocate short allocation: {} / {}",
                shortAllocation.getItem(), shortAllocation.getItem().getItemFamily());
        if (!isAllocatable(shortAllocation) || shortAllocation.getQuantity() <= 0) {
            logger.debug("short allocation is not allocatable! is allocatable? {}, open quantity? {}",
                    isAllocatable(shortAllocation), shortAllocation.getQuantity());
            return shortAllocation;
        }

        // Let's get all the pickable inventory and existing picking so we can start to calculate
        // how to generate picks for the shipment
        Long itemId = shortAllocation.getItemId();

        List<Pick> existingPicks = pickService.getOpenPicksByItemId(itemId);
        logger.debug("We have {} existing picks against the item with id {}",
                existingPicks.size(), itemId);

        // Get all pickable inventory
        List<Inventory> pickableInventory
                = inventoryServiceRestemplateClient.getPickableInventory(
                        itemId, shortAllocation.getShipmentLine().getOrderLine().getInventoryStatusId());
        logger.debug("We have {} pickable inventory against the item with id {} / {}",
                pickableInventory.size(), itemId, shortAllocation.getShipmentLine().getOrderLine().getInventoryStatusId());


        List<Pick> picks = allocationConfigurationService.allocate(shortAllocation, existingPicks, pickableInventory);

        if (picks.size() > 0) {
            // We allocated some quantity for the short allocation, let's deduct the quantity
            // from the short allocation record

            // Deduct the quantity from short allocation
            // There're 2 options to track the quantity of a short allocation
            // Option 1: keep the original short quantity
            //     -- We will keep the original short quantity. So if we cancel any pick of the short allocation, we
            //        can always re-allocate the cancelled quantity later on
            // Option 2: Not keep the original short quantity
            //     -- We won't track the original short quantity. So if we cancel any pick of the short allocation,
            //        there's no way to re-allocate those cancelled quantity
            // Since we will use a scheduled job to allocate the short allocation, we will use option 2 for now.
            // Should we choose option 1, then in case any use cancels the pick of the allocation, the schedule job
            //     will just pickup the short allocation again and re-allocate the quantity, which may not be
            //     what the user want.
            Long totalPickQuantity = picks.stream().map(Pick::getQuantity).mapToLong(Long::longValue).sum();
            logger.debug("We allocate {} of the item {} for the short allocation",
                    totalPickQuantity, shortAllocation.getItem().getName());

            shortAllocation.setQuantity(shortAllocation.getQuantity() - totalPickQuantity);
        }
        shortAllocation.setStatus(ShortAllocationStatus.INPROCESS);

        return save(shortAllocation);
    }

    private boolean isAllocatable(ShortAllocation shortAllocation) {
        return shortAllocation.getStatus().equals(ShortAllocationStatus.PENDING) ||
                shortAllocation.getStatus().equals(ShortAllocationStatus.INPROCESS);
    }

}
