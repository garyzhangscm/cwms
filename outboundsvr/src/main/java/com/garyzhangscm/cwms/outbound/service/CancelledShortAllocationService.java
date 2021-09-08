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
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.CancelledPickRepository;
import com.garyzhangscm.cwms.outbound.repository.CancelledShortAllocationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class CancelledShortAllocationService {
    private static final Logger logger = LoggerFactory.getLogger(CancelledShortAllocationService.class);

    @Autowired
    private CancelledShortAllocationRepository cancelledShortAllocationRepository;
    @Autowired
    private UserService userService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WorkOrderServiceRestemplateClient workOrderServiceRestemplateClient;

    public CancelledShortAllocation findById(Long id, boolean loadDetails) {
        CancelledShortAllocation cancelledShortAllocation
                = cancelledShortAllocationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("cancelled short allocation not found by id: " + id));
        if (loadDetails) {
            loadAttribute(cancelledShortAllocation);
        }
        return cancelledShortAllocation;
    }

    public CancelledShortAllocation findById(Long id) {
        return findById(id, true);
    }


    public List<CancelledShortAllocation> findAll( Long orderId,
                              Long itemId,
                              Long workOrderLineId, String workOrderLineIds,  boolean loadDetails) {

        List<CancelledShortAllocation> cancelledShortAllocations =  cancelledShortAllocationRepository.findAll(
                (Root<CancelledShortAllocation> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (orderId != null) {

                        Join<CancelledShortAllocation, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, OrderLine> joinOrderLine= joinShipmentLine.join("orderLine", JoinType.INNER);
                        Join<OrderLine, Order> joinOrder = joinOrderLine.join("order", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinOrder.get("id"), orderId));

                    }

                    if (itemId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("itemId"), itemId));
                    }

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

        if (cancelledShortAllocations.size() > 0 && loadDetails) {
            loadAttribute(cancelledShortAllocations);
        }
        return cancelledShortAllocations;
    }

    public List<CancelledShortAllocation> findAll(  Long orderId, Long itemId,
                              Long workOrderLineId, String workOrderLineIds) {
        return findAll( orderId, itemId,
                workOrderLineId, workOrderLineIds, true);
    }

    public void loadAttribute(List<CancelledShortAllocation> cancelledShortAllocations) {
        for (CancelledShortAllocation cancelledShortAllocation : cancelledShortAllocations) {
            loadAttribute(cancelledShortAllocation);
        }
    }

    public void loadAttribute(CancelledShortAllocation cancelledShortAllocation) {

        // Load the item and inventory status information for each lines
        if (cancelledShortAllocation.getItemId() != null && cancelledShortAllocation.getItem() == null) {
            cancelledShortAllocation.setItem(inventoryServiceRestemplateClient.getItemById(cancelledShortAllocation.getItemId()));
        }


    }



    public CancelledShortAllocation save(CancelledShortAllocation cancelledShortAllocation) {
        CancelledShortAllocation newCancelledShortAllocation = cancelledShortAllocationRepository.save(cancelledShortAllocation);
        loadAttribute(newCancelledShortAllocation);
        return newCancelledShortAllocation;
    }



    public void delete(CancelledShortAllocation cancelledShortAllocation) {
        cancelledShortAllocationRepository.delete(cancelledShortAllocation);
    }

    public void delete(Long id) {
        cancelledShortAllocationRepository.deleteById(id);
    }

    public CancelledShortAllocation registerShortAllocationCancelled(ShortAllocation shortAllocation, Long cancelledQuantity) {
        CancelledShortAllocation cancelledShortAllocation = createCancelledShortAllocationFromOriginalShortAllocation(shortAllocation, cancelledQuantity);
        return save(cancelledShortAllocation);
    }

    private CancelledShortAllocation createCancelledShortAllocationFromOriginalShortAllocation(ShortAllocation shortAllocation, Long cancelledQuantity) {
        CancelledShortAllocation cancelledShortAllocation = new CancelledShortAllocation();

        cancelledShortAllocation.setShipmentLine(shortAllocation.getShipmentLine());
        cancelledShortAllocation.setWorkOrderLineId(shortAllocation.getWorkOrderLineId());

        cancelledShortAllocation.setWarehouseId(shortAllocation.getWarehouseId());
        cancelledShortAllocation.setItemId(shortAllocation.getItemId());

        cancelledShortAllocation.setQuantity(shortAllocation.getQuantity());
        cancelledShortAllocation.setOpenQuantity(shortAllocation.getOpenQuantity());
        cancelledShortAllocation.setInprocessQuantity(shortAllocation.getInprocessQuantity());
        cancelledShortAllocation.setDeliveredQuantity(shortAllocation.getDeliveredQuantity());

        cancelledShortAllocation.setAllocationCount(shortAllocation.getAllocationCount());

        cancelledShortAllocation.setStatus(shortAllocation.getStatus());

        cancelledShortAllocation.setCancelledQuantity(cancelledQuantity);
        cancelledShortAllocation.setCancelledUsername(userService.getCurrentUserName());
        cancelledShortAllocation.setCancelledDate(LocalDateTime.now());

        return cancelledShortAllocation;
    }


    @Transactional
    public void removeCancelledShortAllocations(Shipment shipment) {

        for (ShipmentLine shipmentLine : shipment.getShipmentLines()) {
            cancelledShortAllocationRepository.deleteByShipmentLine(shipmentLine);

        }
    }


}
