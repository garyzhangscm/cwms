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
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.CancelledPickRepository;
import com.garyzhangscm.cwms.outbound.repository.PickRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class CancelledPickService {
    private static final Logger logger = LoggerFactory.getLogger(CancelledPickService.class);

    @Autowired
    private CancelledPickRepository cancelledPickRepository;
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

    public CancelledPick findById(Long id, boolean loadDetails) {
        CancelledPick cancelledPick = cancelledPickRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("cancelled pick not found by id: " + id));
        if (loadDetails) {
            loadAttribute(cancelledPick);
        }
        return cancelledPick;
    }

    public CancelledPick findById(Long id) {
        return findById(id, true);
    }


    public List<CancelledPick> findAll(String pickNumber, Long orderId,
                              Long itemId, Long sourceLocationId, Long destinationLocationId,
                              Long workOrderLineId, String workOrderLineIds,  boolean loadDetails) {

        List<CancelledPick> cancelledPicks =  cancelledPickRepository.findAll(
                (Root<CancelledPick> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (!StringUtils.isBlank(pickNumber)) {
                        predicates.add(criteriaBuilder.equal(root.get("pickNumber"), pickNumber));

                    }
                    if (orderId != null) {

                        Join<CancelledPick, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        Join<ShipmentLine, OrderLine> joinOrderLine= joinShipmentLine.join("orderLine", JoinType.INNER);
                        Join<OrderLine, Order> joinOrder = joinOrderLine.join("order", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinOrder.get("id"), orderId));

                    }

                    if (itemId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("itemId"), itemId));
                    }
                    if (sourceLocationId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("sourceLocationId"), sourceLocationId));
                    }
                    if (destinationLocationId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("destinationLocationId"), destinationLocationId));
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

        if (cancelledPicks.size() > 0 && loadDetails) {
            loadAttribute(cancelledPicks);
        }
        return cancelledPicks;
    }

    public List<CancelledPick> findAll(String pickNumber, Long orderId,
                              Long itemId, Long sourceLocationId, Long destinationLocationId,
                              Long workOrderLineId, String workOrderLineIds) {
        return findAll(pickNumber, orderId,
                itemId, sourceLocationId, destinationLocationId,
                workOrderLineId, workOrderLineIds, true);
    }

    public void loadAttribute(List<CancelledPick> cancelledPicks) {
        for (CancelledPick cancelledPick : cancelledPicks) {
            loadAttribute(cancelledPick);
        }
    }

    public void loadAttribute(CancelledPick cancelledPick) {
        // Load the details for client and supplier informaiton
        if (cancelledPick.getSourceLocationId() != null && cancelledPick.getSourceLocation() == null) {
            cancelledPick.setSourceLocation(warehouseLayoutServiceRestemplateClient.getLocationById(cancelledPick.getSourceLocationId()));
        }
        if (cancelledPick.getDestinationLocationId() != null && cancelledPick.getDestinationLocation() == null) {
            cancelledPick.setDestinationLocation(warehouseLayoutServiceRestemplateClient.getLocationById(cancelledPick.getDestinationLocationId()));
        }

        // Load the item and inventory status information for each lines
        if (cancelledPick.getItemId() != null && cancelledPick.getItem() == null) {
            cancelledPick.setItem(inventoryServiceRestemplateClient.getItemById(cancelledPick.getItemId()));
        }

        // load pick's inventory status for
        if (cancelledPick.getInventoryStatusId() != null &&
                cancelledPick.getInventoryStatus() == null) {
            cancelledPick.setInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(
                            cancelledPick.getInventoryStatusId()
                    ));
        }


    }



    public CancelledPick save(CancelledPick cancelledPick) {
        CancelledPick newCancelledPick = cancelledPickRepository.save(cancelledPick);
        loadAttribute(newCancelledPick);
        return newCancelledPick;
    }



    public void delete(CancelledPick cancelledPick) {
        cancelledPickRepository.delete(cancelledPick);
    }

    public void delete(Long id) {
        cancelledPickRepository.deleteById(id);
    }

    public CancelledPick registerPickCancelled(Pick pick, Long cancelledQuantity) {
        CancelledPick cancelledPick = createCancelledPickFromOriginalPick(pick, cancelledQuantity);
        return save(cancelledPick);
    }

    public String getNextCancelledPickNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "cancelled-pick-number");

    }
    private CancelledPick createCancelledPickFromOriginalPick(Pick pick, Long cancelledQuantity) {
        CancelledPick cancelledPick = new CancelledPick();

        cancelledPick.setPickId(pick.getId());
        cancelledPick.setPickNumber(pick.getNumber());
        cancelledPick.setNumber(getNextCancelledPickNumber(pick.getWarehouseId()));

        cancelledPick.setSourceLocationId(pick.getSourceLocationId());
        cancelledPick.setDestinationLocationId(pick.getDestinationLocationId());
        cancelledPick.setItemId(pick.getItemId());
        cancelledPick.setWarehouseId(pick.getWarehouseId());

        cancelledPick.setShipmentLine(pick.getShipmentLine());
        cancelledPick.setWorkOrderLineId(pick.getWorkOrderLineId());
        cancelledPick.setShortAllocation(pick.getShortAllocation());

        cancelledPick.setPickType(pick.getPickType());
        cancelledPick.setQuantity(pick.getQuantity());
        cancelledPick.setPickedQuantity(pick.getPickedQuantity());
        cancelledPick.setCancelledQuantity(cancelledQuantity);

        cancelledPick.setInventoryStatusId(pick.getInventoryStatusId());
        cancelledPick.setPickList(pick.getPickList());

        cancelledPick.setCancelledUsername(userService.getCurrentUserName());
        cancelledPick.setCancelledDate(LocalDateTime.now());

        return cancelledPick;
    }



    @Transactional
    public void removeCancelledPicks(Shipment shipment) {

        shipment.getShipmentLines().forEach(
                shipmentLine -> cancelledPickRepository.deleteByShipmentLine(shipmentLine)
        );
    }

}
