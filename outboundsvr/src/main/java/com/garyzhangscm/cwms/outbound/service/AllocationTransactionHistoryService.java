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

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.outbound.clients.*;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.AllocationTransactionHistoryRepository;
import com.garyzhangscm.cwms.outbound.repository.CartonRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


@Service
public class AllocationTransactionHistoryService   {
    private static final Logger logger = LoggerFactory.getLogger(AllocationTransactionHistoryService.class);

    @Autowired
    private AllocationTransactionHistoryRepository allocationTransactionHistoryRepository;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private WorkOrderServiceRestemplateClient workOrderServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    @Autowired
    private UserService userService;

    @Autowired
    private HttpSession httpSession;
    @Autowired
    private KafkaSender kafkaSender;

    public AllocationTransactionHistory findById(Long id) {
        return allocationTransactionHistoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("allocation transaction history not found by id: " + id));
    }

    public AllocationTransactionHistory save(AllocationTransactionHistory allocationTransactionHistory) {
        return allocationTransactionHistoryRepository.save(allocationTransactionHistory);
    }
    public List<AllocationTransactionHistory> findAll(Long warehouseId,
                                                      String number,
                                                      String transactionGroupId,
                                                      String orderNumber,
                                                      String workOrderNumber,
                                                      String itemName,
                                                      String locationName,
                                                      Boolean loadDetails) {

        List<AllocationTransactionHistory> allocationTransactionHistories =  allocationTransactionHistoryRepository.findAll(
                (Root<AllocationTransactionHistory> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));
                    }
                    if (StringUtils.isNotBlank(transactionGroupId)) {
                        predicates.add(criteriaBuilder.equal(root.get("transactionGroupId"), transactionGroupId));
                    }
                    if (StringUtils.isNotBlank(orderNumber)) {
                        predicates.add(criteriaBuilder.equal(root.get("orderNumber"), orderNumber));
                    }
                    if (StringUtils.isNotBlank(workOrderNumber)) {
                        predicates.add(criteriaBuilder.equal(root.get("workOrderNumber"), workOrderNumber));
                    }
                    if (StringUtils.isNotBlank(itemName)) {
                        predicates.add(criteriaBuilder.equal(root.get("itemName"), itemName));
                    }
                    if (StringUtils.isNotBlank(locationName)) {
                        predicates.add(criteriaBuilder.equal(root.get("locationName"), locationName));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
        if (!allocationTransactionHistories.isEmpty() && loadDetails) {
            loadAttribute(allocationTransactionHistories);
        }

        return allocationTransactionHistories;
    }

    public void loadAttribute(List<AllocationTransactionHistory> allocationTransactionHistories) {
        allocationTransactionHistories.forEach(this::loadAttribute);
    }

    public void loadAttribute(AllocationTransactionHistory allocationTransactionHistory) {
        if (Objects.nonNull(allocationTransactionHistory.getWorkOrderId()) &&
                Objects.isNull(allocationTransactionHistory.getWorkOrder())) {
            allocationTransactionHistory.setWorkOrder(
                    workOrderServiceRestemplateClient.getWorkOrderById(
                            allocationTransactionHistory.getWorkOrderId()
                    )
            );
        }
        if (Objects.nonNull(allocationTransactionHistory.getItemId()) &&
                Objects.isNull(allocationTransactionHistory.getItem())) {
            allocationTransactionHistory.setItem(
                    inventoryServiceRestemplateClient.getItemById(
                            allocationTransactionHistory.getItemId()
                    )
            );
        }
        if (Objects.nonNull(allocationTransactionHistory.getLocationId()) &&
                Objects.isNull(allocationTransactionHistory.getLocation())) {
            allocationTransactionHistory.setLocation(
                    warehouseLayoutServiceRestemplateClient.getLocationById(
                            allocationTransactionHistory.getLocationId()
                    )
            );
        }
    }

    public AllocationTransactionHistory findByNumber(Long warehouseId, String number) {
        return allocationTransactionHistoryRepository.findByWarehouseIdAndNumber(warehouseId, number);
    }

    public AllocationTransactionHistory saveOrUpdate(AllocationTransactionHistory allocationTransactionHistory) {
        if (Objects.isNull(allocationTransactionHistory.getId()) &&
                Objects.nonNull(findByNumber(
                        allocationTransactionHistory.getWarehouseId(),
                        allocationTransactionHistory.getNumber()))) {
            allocationTransactionHistory.setId(
                    findByNumber(
                            allocationTransactionHistory.getWarehouseId(),
                            allocationTransactionHistory.getNumber()).getId());
        }
        return save(allocationTransactionHistory);
    }
    private String getNextNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "allocation-transaction-history-number");
    }

    private String getNextTransactionGroupId(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "allocation-transaction-history-group-id");
    }
    private String getTransactionGroupId(Long warehouseId) {
        String transactionGroupId;
        if (Objects.isNull(httpSession.getAttribute("allocation-transaction-history-group-id"))) {
            logger.debug("Current session doesn't have any transaction id yet, let's get a new one");
            transactionGroupId = getNextTransactionGroupId(warehouseId);
            httpSession.setAttribute("allocation-transaction-history-group-id", transactionGroupId);
            logger.debug(">> {}", transactionGroupId);
        }
        else {
            transactionGroupId = httpSession.getAttribute("allocation-transaction-history-group-id").toString();
            logger.debug("Get transaction ID {} from current session", transactionGroupId);
        }
        return transactionGroupId;
    }

    /**
     * Create a allocation transaction history
     * @param allocationRequest
     * @param sourceLocation
     * @param currentRequiredQuantity
     * @param totalInventoryQuantity
     * @param totalAvailableQuantity
     * @param totalAllocatedQuantity
     * @param alreadyAllocatedQuantity
     * @param isSkippedFlag
     * @param isAllocatedByLPNFlag
     * @param isRoundUpFlag
     * @param message
     * @return
     */
    private AllocationTransactionHistory createAllocationTransactionHistory(
            AllocationRequest allocationRequest,
            Location sourceLocation,
            Long currentRequiredQuantity,
            Long totalInventoryQuantity,
            Long totalAvailableQuantity,
            Long totalAllocatedQuantity,
            Long alreadyAllocatedQuantity,
            Boolean isSkippedFlag,
            Boolean isAllocatedByLPNFlag,
            Boolean isRoundUpFlag,
            String message
    ) {
        // logger.debug("Start to build allocation transaction history from the allocation request: ====> \n {}", allocationRequest);
        ShipmentLine shipmentLine =
                Objects.nonNull(allocationRequest.getShipmentLines()) &&
                    !allocationRequest.getShipmentLines().isEmpty() ?
                allocationRequest.getShipmentLines().get(0) : null;
        WorkOrderLine workOrderLine =
                Objects.nonNull(allocationRequest.getWorkOrderLines()) &&
                        !allocationRequest.getWorkOrderLines().isEmpty() ?
                        allocationRequest.getWorkOrderLines().get(0) : null;

        Long warehouseId = Objects.nonNull(shipmentLine) ? shipmentLine.getWarehouseId() :
                workOrderLine.getWarehouseId();




        AllocationTransactionHistory.Builder builder =
                new AllocationTransactionHistory.Builder(
                        warehouseId,
                        getNextNumber(warehouseId),
                        getTransactionGroupId(warehouseId)
                );
        // logger.debug("start with AllocationTransactionHistory.Builder: \n {}", builder);
        builder = builder.shipmentLine(shipmentLine)
                .orderNumber(Objects.nonNull(shipmentLine) ?
                        shipmentLine.getOrderNumber() : "")
                .itemName(
                        Objects.nonNull(allocationRequest.getItem()) ?
                                allocationRequest.getItem().getName() : "")
                .item(
                        Objects.nonNull(allocationRequest.getItem()) ?
                                allocationRequest.getItem() : null)
                .itemId(
                        Objects.nonNull(allocationRequest.getItem()) ?
                                allocationRequest.getItem().getId() : null)
                .locationName(
                        Objects.nonNull(sourceLocation) ?
                                sourceLocation.getName() : "")
                .locationId(
                        Objects.nonNull(sourceLocation) ?
                                sourceLocation.getId() : null)
                .location(
                        Objects.nonNull(sourceLocation) ?
                                sourceLocation : null)
                .totalRequiredQuantity(allocationRequest.getQuantity())
                .currentRequiredQuantity(currentRequiredQuantity)
                .totalInventoryQuantity(totalInventoryQuantity)
                .totalAvailableQuantity(totalAvailableQuantity)
                .totalAllocatedQuantity(totalAllocatedQuantity)
                .alreadyAllocatedQuantity(alreadyAllocatedQuantity)
                .isSkippedFlag(isSkippedFlag)
                .isAllocatedByLPNFlag(isAllocatedByLPNFlag)
                .isRoundUpFlag(isRoundUpFlag)
                .username(userService.getCurrentUserName())
                .message(message);
        AllocationTransactionHistory allocationTransactionHistory =
                builder.build();

        // logger.debug("We get allocation transaction history out of builder \n {}",
        //        allocationTransactionHistory);
        return allocationTransactionHistory;
    }

    /**
     * Create an empty allocation transaction history, this happens when we
     * skip a location due to some reason(for example, the location is locked, )
     * then we won't care about the inventory quantity or allocated quantity from
     * the location but we will save the skip reason in the message field
     * @param allocationRequest
     * @param sourceLocation
     * @param currentRequiredQuantity
     * @param isAllocatedByLPNFlag
     * @param isRoundUpFlag
     * @param message
     * @return
     */
    private AllocationTransactionHistory createEmptyAllocationTransactionHistory(
            AllocationRequest allocationRequest,
            Location sourceLocation,
            Long currentRequiredQuantity,
            Boolean isAllocatedByLPNFlag,
            Boolean isRoundUpFlag,
            String message
    ) {
        return createAllocationTransactionHistory(
                allocationRequest,
                sourceLocation,
                currentRequiredQuantity,
                0l, 0l, 0l, 0l,
                true, isAllocatedByLPNFlag, isRoundUpFlag, message
        );

    }

    public void createAndSendEmptyAllocationTransactionHistory(
            AllocationRequest allocationRequest,
            Location sourceLocation,
            Long currentRequiredQuantity,
            Boolean isAllocatedByLPNFlag,
            Boolean isRoundUpFlag,
            String message
    ) {
        send(createEmptyAllocationTransactionHistory(
                allocationRequest, sourceLocation, currentRequiredQuantity,
                isAllocatedByLPNFlag, isRoundUpFlag, message
        ));

    }
    public void createAndSendAllocationTransactionHistory(
            AllocationRequest allocationRequest,
            Location sourceLocation,
            Long currentRequiredQuantity,
            Long totalInventoryQuantity,
            Long totalAvailableQuantity,
            Long totalAllocatedQuantity,
            Long alreadyAllocatedQuantity,
            Boolean isSkippedFlag,
            Boolean isAllocatedByLPNFlag,
            Boolean isRoundUpFlag,
            String message
    ) {
        send(createAllocationTransactionHistory(
                allocationRequest, sourceLocation, currentRequiredQuantity,
                totalInventoryQuantity, totalAvailableQuantity, totalAllocatedQuantity,
                alreadyAllocatedQuantity, isSkippedFlag,
                isAllocatedByLPNFlag, isRoundUpFlag, message
        ));

    }


    public void send(AllocationTransactionHistory allocationTransactionHistory) {
        logger.debug("Start to send allocation transaction history: \n {}",
                allocationTransactionHistory);
        kafkaSender.send(allocationTransactionHistory);
    }


    public AllocationTransactionHistory addAllocationTransactionHistory(AllocationTransactionHistory allocationTransactionHistory) {
        return saveOrUpdate(allocationTransactionHistory);
    }

    public void removeAllocationTransactionHistory(Shipment shipment) {
        for (ShipmentLine shipmentLine : shipment.getShipmentLines()) {
            allocationTransactionHistoryRepository.deleteByShipmentLine(shipmentLine);

        }
    }
}
