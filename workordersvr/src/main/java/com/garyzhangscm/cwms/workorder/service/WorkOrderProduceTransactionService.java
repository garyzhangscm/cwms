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

package com.garyzhangscm.cwms.workorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.OutboundServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderProduceTransactionRepository;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class WorkOrderProduceTransactionService  {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderProduceTransactionService.class);

    @Autowired
    private WorkOrderProduceTransactionRepository workOrderProduceTransactionRepository;
    @Autowired
    private WorkOrderByProductService workOrderByProductService;
    @Autowired
    private WorkOrderKPIService workOrderKPIService;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private BillOfMaterialService billOfMaterialService;
    @Autowired
    private BillOfMaterialLineService billOfMaterialLineService;
    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private ProductionLineDeliveryService productionLineDeliveryService;
    @Autowired
    private WorkOrderLineService workOrderLineService;
    @Autowired
    private WorkOrderKPITransactionService workOrderKPITransactionService;
    @Autowired
    private ProductionLineService productionLineService;
    @Autowired
    private WorkOrderConfigurationService workOrderConfigurationService;

    public WorkOrderProduceTransaction findById(Long id, boolean loadDetails) {
        WorkOrderProduceTransaction workOrderProduceTransaction
                = workOrderProduceTransactionRepository.findById(id)
                    .orElseThrow(() -> ResourceNotFoundException.raiseException("work order transaction not found by id: " + id));
        if (loadDetails) {
            loadAttribute(workOrderProduceTransaction);
        }
        return workOrderProduceTransaction;
    }

    public WorkOrderProduceTransaction findById(Long id) {
        return findById(id, true);
    }


    public List<WorkOrderProduceTransaction> findAll(Long warehouseId, String workOrderNumber,
                                                     Long productionLineId, boolean genericQuery,
                                                     LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return findAll(warehouseId, workOrderNumber, productionLineId, genericQuery,
                startTime, endTime, date, true);
    }
    public List<WorkOrderProduceTransaction> findAll(Long warehouseId, String workOrderNumber,
                                                     Long productionLineId,
                                                     boolean genericQuery,
                                                     LocalDateTime startTime, LocalDateTime endTime, LocalDate date,
                                                     boolean loadDetails) {

        List<WorkOrderProduceTransaction> workOrderProduceTransactions
                =  workOrderProduceTransactionRepository.findAll(
                (Root<WorkOrderProduceTransaction> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();


                    Join<WorkOrderProduceTransaction, WorkOrder> joinWorkOrder = root.join("workOrder", JoinType.INNER);

                    predicates.add(criteriaBuilder.equal(joinWorkOrder.get("warehouseId"), warehouseId));

                    if (!StringUtils.isBlank(workOrderNumber)) {


                        if (genericQuery) {

                            predicates.add(criteriaBuilder.like(joinWorkOrder.get("number"), workOrderNumber));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinWorkOrder.get("number"), workOrderNumber));

                        }

                    }
                    if (Objects.nonNull(productionLineId)) {

                        Join<WorkOrderProduceTransaction, ProductionLine> joinProductionLine
                                = root.join("productionLine", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinProductionLine.get("id"), productionLineId));
                    }

                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdTime"), endTime));

                    }
                    logger.debug(">> Date is passed in {}", date);
                    if (Objects.nonNull(date)) {
                        LocalDateTime dateStartTime = date.atTime(0, 0, 0, 0);
                        LocalDateTime dateEndTime = date.atTime(23, 59, 59, 999999999);
                        predicates.add(criteriaBuilder.between(
                                root.get("createdTime"), dateStartTime, dateEndTime));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (workOrderProduceTransactions.size() > 0 && loadDetails) {
            loadAttribute(workOrderProduceTransactions);
        }
        return workOrderProduceTransactions;
    }


    public void loadAttribute(List<WorkOrderProduceTransaction> workOrderProduceTransactions) {
        for (WorkOrderProduceTransaction workOrderProduceTransaction : workOrderProduceTransactions) {
            loadAttribute(workOrderProduceTransaction);
        }
    }

    public void loadAttribute(WorkOrderProduceTransaction workOrderProduceTransaction) {

        if (workOrderProduceTransaction.getWorkOrder().getItemId() != null &&
                workOrderProduceTransaction.getWorkOrder().getItem() == null) {
            workOrderProduceTransaction.getWorkOrder().setItem(
                    inventoryServiceRestemplateClient.getItemById(
                            workOrderProduceTransaction.getWorkOrder().getItemId()));
        }
        if (workOrderProduceTransaction.getWorkOrder().getWarehouseId() != null &&
                workOrderProduceTransaction.getWorkOrder().getWarehouse() == null) {
            workOrderProduceTransaction.getWorkOrder().setWarehouse(
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(
                            workOrderProduceTransaction.getWorkOrder().getWarehouseId()));
        }



    }



    public WorkOrderProduceTransaction save(WorkOrderProduceTransaction workOrderProduceTransaction) {
        // setup the work order produce transaction reference for
        // WorkOrderLineConsumeTransaction and WorkOrderProducedInventory array so that
        // the change can be cascaded
        workOrderProduceTransaction.getWorkOrderLineConsumeTransactions().forEach(
                workOrderLineConsumeTransaction ->
                    workOrderLineConsumeTransaction.setWorkOrderProduceTransaction(workOrderProduceTransaction));
        workOrderProduceTransaction.getWorkOrderProducedInventories().forEach(
                workOrderProducedInventory ->
                        workOrderProducedInventory.setWorkOrderProduceTransaction(workOrderProduceTransaction)
        );


        WorkOrderProduceTransaction newWorkOrderProduceTransaction
                = workOrderProduceTransactionRepository.save(workOrderProduceTransaction);
        loadAttribute(newWorkOrderProduceTransaction);
        return newWorkOrderProduceTransaction;
    }




    public void delete(WorkOrderProduceTransaction workOrderProduceTransaction) {
        workOrderProduceTransactionRepository.delete(workOrderProduceTransaction);
    }

    public void delete(Long id) {
        workOrderProduceTransactionRepository.deleteById(id);
    }

    /**
     * Start a new transaction. Save the transaction to database and update the work order line's
     * total consumed quantity as well
     * @param workOrderProduceTransaction
     * @return
     */
    @Transactional
    public WorkOrderProduceTransaction startNewTransaction(
            WorkOrderProduceTransaction workOrderProduceTransaction) {

        setupNewWorkOrderProduceTransactionData(workOrderProduceTransaction);

        // get the latest information
        WorkOrder workOrder = workOrderService.findById(workOrderProduceTransaction.getWorkOrder().getId());
        workOrderProduceTransaction.setWorkOrder(workOrder);

        // make sure
        // 1. we are not over produce
        // 2. we are not over consume
        validateWorkOrderProduceTransaction(workOrderProduceTransaction);


        // save the transaction first
        WorkOrderProduceTransaction newWorkOrderProduceTransaction = save(workOrderProduceTransaction);

        // total work order produced quantity
        Long totalProducedQuantity = 0L;
        for(WorkOrderProducedInventory workOrderProducedInventory :
                newWorkOrderProduceTransaction.getWorkOrderProducedInventories()) {
            // skip the record with incorrect value
            if (StringUtils.isBlank(workOrderProducedInventory.getLpn()) ||
                    Objects.isNull(workOrderProducedInventory.getInventoryStatus()) ||
                    Objects.isNull(workOrderProducedInventory.getItemPackageType()) ||
                    Objects.isNull(workOrderProducedInventory.getQuantity())  ) {
                continue;
            }
            totalProducedQuantity += workOrderProducedInventory.getQuantity();
            // Let's create the inventory
            receiveInventoryFromWorkOrder(workOrder, workOrderProducedInventory, newWorkOrderProduceTransaction);

        }
        // Change the produced quantity of the work order
        workOrderService.produce(workOrder, totalProducedQuantity);

        // change each work order line's consumed quantity
        for (WorkOrderLine workOrderLine : workOrder.getWorkOrderLines()) {
            consumeQuantity(workOrderLine, workOrderProduceTransaction, totalProducedQuantity);
        }

        // produce the byproduct if there's any
        workOrderProduceTransaction.getWorkOrderByProductProduceTransactions().forEach(
                workOrderByProductProduceTransaction ->
                        workOrderByProductService.processWorkOrderByProductProduceTransaction(
                                workOrder, workOrderByProductProduceTransaction,
                                workOrderProduceTransaction.getProductionLine().getOutboundStageLocation()
                        )
        );

        // save the transaction itself
        // before we save everything, we will need to setup some missing information so
        // the whole objects will be saved in one transaction
        workOrderProduceTransaction.getWorkOrderLineConsumeTransactions().forEach(
                workOrderLineConsumeTransaction -> {
                    workOrderLineConsumeTransaction.getWorkOrderLineConsumeLPNTransactions().forEach(
                            workOrderLineConsumeLPNTransaction ->
                                    workOrderLineConsumeLPNTransaction.setWorkOrderLineConsumeTransaction(
                                            workOrderLineConsumeTransaction
                                    )
                    );
                }
        );

        processWorkOrderKPI(newWorkOrderProduceTransaction, totalProducedQuantity);


        return newWorkOrderProduceTransaction;

    }

    private void setupNewWorkOrderProduceTransactionData(WorkOrderProduceTransaction workOrderProduceTransaction) {

        // setup the location for later use
        if (Objects.isNull(workOrderProduceTransaction.getProductionLine().getInboundStageLocation())) {
            workOrderProduceTransaction.getProductionLine().setInboundStageLocation(
                    warehouseLayoutServiceRestemplateClient.getLocationById(
                            workOrderProduceTransaction.getProductionLine().getInboundStageLocationId()
                    )
            );
        }

        if (Objects.isNull(workOrderProduceTransaction.getProductionLine().getOutboundStageLocation())) {
            workOrderProduceTransaction.getProductionLine().setOutboundStageLocation(
                    warehouseLayoutServiceRestemplateClient.getLocationById(
                            workOrderProduceTransaction.getProductionLine().getOutboundStageLocationId()
                    )
            );
        }

        // trim for the data
        workOrderProduceTransaction.getWorkOrderProducedInventories().forEach(
                workOrderProducedInventory -> workOrderProducedInventory.setLpn(
                        workOrderProducedInventory.getLpn().trim()
                )
        );
        workOrderProduceTransaction.getWorkOrderReverseProductionInventories().forEach(
                workOrderReverseProductionInventory -> workOrderReverseProductionInventory.setLpn(
                        workOrderReverseProductionInventory.getLpn().trim()
                )
        );
        workOrderProduceTransaction.getWorkOrderByProductProduceTransactions().forEach(
                workOrderByProductProduceTransaction -> workOrderByProductProduceTransaction.setLpn(
                        workOrderByProductProduceTransaction.getLpn().trim()
                )
        );

    }

    private void processWorkOrderKPI(WorkOrderProduceTransaction newWorkOrderProduceTransaction,
                                     Long totalProducedQuantity) {
        // save the KPI
        // if it is explicitly specified , then save the KPI from user input
        // otherwise, check if there's any person that already checked in the production and
        //  log the KPI under this person's name
        if (newWorkOrderProduceTransaction.getWorkOrderKPITransactions().size() > 0) {
            workOrderKPITransactionService.processWorkOrderKIPTransaction(newWorkOrderProduceTransaction);
        }
        else {
            // check if we have anyone that is working on the production line
            logger.debug("work order kpi is not passed in, let's check if we have anyone that sign into the production: {}",
                    newWorkOrderProduceTransaction.getProductionLine().getName());
            ProductionLineActivity checkedInUser
                    = productionLineService.getCheckedInUser(newWorkOrderProduceTransaction.getProductionLine());

            if (Objects.nonNull(checkedInUser)) {
                // OK someone is working on this production line, let's give them the KPI
                logger.debug("Will save the work order KPI to user {}, work order: {}, production line： {}" +
                                ", quantity: {}",
                        checkedInUser.getUsername(),
                        newWorkOrderProduceTransaction.getWorkOrder().getNumber(),
                        newWorkOrderProduceTransaction.getProductionLine().getName(),
                        totalProducedQuantity);
                WorkOrderKPI workOrderKPI =
                        workOrderKPIService.recordWorkOrderKPIForCheckedInUser(
                                newWorkOrderProduceTransaction.getWorkOrder(),
                                newWorkOrderProduceTransaction.getProductionLine(),
                                checkedInUser.getUsername(),
                                checkedInUser.getWorkingTeamMemberCount(),
                                totalProducedQuantity
                        );

                logger.debug("Auto generated work order kpi: \n{}", workOrderKPI);

            }

        }
    }

    private void validateWorkOrderProduceTransaction(
            WorkOrderProduceTransaction workOrderProduceTransaction) {
        // make sure we won't over consume

        boolean isOverProduceAllowed =
                workOrderConfigurationService.isOverProduceAllowed(
                        workOrderProduceTransaction.getWorkOrder()
                );
        boolean isOverConsumeAllowed =
                workOrderConfigurationService.isOverConsumeAllowed(
                        workOrderProduceTransaction.getWorkOrder()
                );
        logger.debug("Start to validate the produce transaction with " +
                " isOverProduceAllowed: {}, isOverConsumeAllowed: {}",
                isOverProduceAllowed,
                isOverConsumeAllowed);
        // we need to make sure the LPNs of the produced inventory
        // is passed in
        if (workOrderProduceTransaction
                .getWorkOrderProducedInventories().stream()
                .anyMatch(workOrderProducedInventory -> Strings.isBlank(workOrderProducedInventory.getLpn()))) {
            throw  WorkOrderException.raiseException(
                    "Can't produce the inventory since the LPN is empty ");
        }

        // see if we are over produce inventory only if over produce is not allowed

        if (!isOverProduceAllowed) {
            validateWorkOrderOverProduced(workOrderProduceTransaction);
        }

        // only validate the consume transaction
        // if we consume the material line per each produce transaction
        if (workOrderConfigurationService.getWorkOrderMaterialConsumeTiming(
                workOrderProduceTransaction.getWorkOrder()).equals(WorkOrderMaterialConsumeTiming.BY_TRANSACTION)) {

            if(Boolean.TRUE.equals(workOrderProduceTransaction.getWorkOrder().getConsumeByBomOnly())) {
                validateWorkOrderLineConsumeTransactionByBom(workOrderProduceTransaction, isOverConsumeAllowed);
            }
            else {

                for (WorkOrderLineConsumeTransaction workOrderLineConsumeTransaction
                        : workOrderProduceTransaction.getWorkOrderLineConsumeTransactions()) {
                    validateWorkOrderLineConsumeTransaction(
                            workOrderLineConsumeTransaction,
                            workOrderProduceTransaction.getProductionLine(),
                            isOverConsumeAllowed);
                }
            }


        }

    }

    /**
     * Make sure we are not over produce from the work order
     * @param workOrderProduceTransaction
     */
    private void validateWorkOrderOverProduced(WorkOrderProduceTransaction workOrderProduceTransaction) {
        Long currentProducedQuantity = workOrderProduceTransaction.getWorkOrder().getProducedQuantity();
        Long expectedProduceQuantity = workOrderProduceTransaction.getWorkOrder().getExpectedQuantity();

        Long producingQuantity = workOrderProduceTransaction.getWorkOrderProducedInventories()
                .stream().mapToLong(
                        producedInventory -> producedInventory.getQuantity()
                ).sum();
        if (currentProducedQuantity + producingQuantity > expectedProduceQuantity) {
            logger.debug("Over Producing found!");
            logger.debug("expectedProduceQuantity: {}, currentProducedQuantity: {}, " +
                    " producingQuantity: {}, (currentProducedQuantity + producingQuantity) = {}",
                    expectedProduceQuantity,
                    currentProducedQuantity,
                    producingQuantity,
                    (currentProducedQuantity + producingQuantity));
            throw WorkOrderException.raiseException("Over produce is not allowed! Work order " +
                    workOrderProduceTransaction.getWorkOrder().getNumber() +
                    " we already produced " + currentProducedQuantity +
                    ", total quantity in this transaction is " + producingQuantity +
                    ", which makes the total quantity become " + (currentProducedQuantity + producingQuantity) +
                    " and exceeds the expected total quantity of " + expectedProduceQuantity);
        }
    }

    private void validateWorkOrderLineConsumeTransactionByBom(
            WorkOrderProduceTransaction workOrderProduceTransaction,
            boolean isOverConsumeAllowed) {
        WorkOrder workOrder = workOrderProduceTransaction.getWorkOrder();
        if (Objects.isNull(workOrder.getConsumeByBom())) {
            throw WorkOrderException.raiseException("BOM is not setup in the work order");
        }

        if (!workOrderProduceTransaction.getConsumeByBomQuantity()) {
            throw WorkOrderException.raiseException("The work order is not setup to be consumed by BOM only");
        }
        // we allow the user to override the default BOM
        // 1. if so we will use the override BOM
        BillOfMaterial billOfMaterial = workOrderProduceTransaction.getConsumeByBom();
        if (Objects.isNull(billOfMaterial)) {
            // if the user didn't specify a bom
            // then find most suitable bom
            // billOfMaterial = billOfMaterialService.getMatchedBillOfMaterial(workOrderProduceTransaction.getWorkOrder());
            billOfMaterial = workOrder.getConsumeByBom();
        }


        if (Objects.isNull(billOfMaterial)) {
            throw WorkOrderException.raiseException("Can't consume the work order by bill of material as none is defined for this work order");
        }


        Long totalProducedQuantity = workOrderProduceTransaction.getWorkOrderProducedInventories()
                .stream().mapToLong(inventory -> inventory.getQuantity()).sum();


        for (WorkOrderLineConsumeTransaction workOrderLineConsumeTransaction : workOrderProduceTransaction.getWorkOrderLineConsumeTransactions()) {

            WorkOrderLine workOrderLine = workOrderLineConsumeTransaction.getWorkOrderLine();
            // find the right bill of material line. there should be only one line matches
            // with the work order line
            List<BillOfMaterialLine> billOfMaterialLines =
                    billOfMaterial.getBillOfMaterialLines().stream().filter(
                        billOfMaterialLine -> billOfMaterialLineService.match(billOfMaterialLine, workOrderLine)
                    ).collect(Collectors.toList());
            if (billOfMaterialLines.size() == 0) {
                throw WorkOrderException.raiseException("Can't find a right BOM to consume for this work order, no line defined for item " +
                        workOrderLine.getItem().getName());
            }
            else if (billOfMaterialLines.size() > 1) {
                throw WorkOrderException.raiseException("Can't find a right BOM to consume for this work order. multiple lines defined for item " +
                        workOrderLine.getItem().getName());
            }
            BillOfMaterialLine billOfMaterialLine = billOfMaterialLines.get(0);
            Double billOfMaterialLineConsumeQuantity = billOfMaterialLine.getExpectedQuantity();
            Long consumingQuantity = (long)
                    Math.ceil(billOfMaterialLineConsumeQuantity * totalProducedQuantity * 1.0 / billOfMaterial.getExpectedQuantity());
            logger.debug("Start to check if we can consume {} of item {} by BOM {}, in order to create {} of item {}",
                    consumingQuantity,
                    workOrderLineConsumeTransaction.getWorkOrderLine().getItem().getName(),
                    billOfMaterial.getNumber(),
                    totalProducedQuantity,
                    workOrderProduceTransaction.getWorkOrder().getItem().getName());

            // make sure we have enough quantity delivered for consume

            validateWorkOrderLineConsumeTransactionByDeliveredQuantity(
                    workOrderLineConsumeTransaction.getWorkOrderLine(),
                    workOrderProduceTransaction.getProductionLine(),
                    consumingQuantity, isOverConsumeAllowed
            );

        }
    }

    private void validateWorkOrderLineConsumeTransaction(
            WorkOrderLineConsumeTransaction workOrderLineConsumeTransaction,
            ProductionLine productionLine,
            boolean isOverConsumeAllowed) {

        // if we consume from the delivered inventory, make sure
        // we have enough quantity being delivered
        if (workOrderLineConsumeTransaction.getConsumedQuantity() > 0) {
            validateWorkOrderLineConsumeTransactionByDeliveredQuantity(workOrderLineConsumeTransaction,
                    productionLine,
                    isOverConsumeAllowed);
        }

        if (!workOrderLineConsumeTransaction.getWorkOrderLineConsumeLPNTransactions().isEmpty()) {

            // the user is try to consume by non picked LPN, let's make sure the LPN is in
            // right place and consume quantity not exceed the inventory's quantity
            validateWorkOrderLineConsumeTransactionByNonPickedInventory(workOrderLineConsumeTransaction,
                    productionLine);

        }




    }

    private void validateWorkOrderLineConsumeTransactionByNonPickedInventory(
            WorkOrderLineConsumeTransaction workOrderLineConsumeTransaction,
            ProductionLine productionLine) {
        if (workOrderLineConsumeTransaction.getWorkOrderLineConsumeLPNTransactions().isEmpty()) {
            return;
        }

        Long locationId = productionLine.getInboundStageLocationId();
        List<Inventory> inventories = inventoryServiceRestemplateClient.findInventoryByLocation(
                productionLine.getWarehouseId(), locationId);
        // only return the inventory without any pick work
        // key: LPN
        // value: quantity
        // we will save the result into a map since we only care about the LPN and the quantity on it
        Map<String, Long> lpnQuantityMap = new HashMap<>();
        inventories.stream().filter(inventory -> Objects.isNull(inventory.getPickId()))
                .forEach(inventory -> {
                    String lpn = inventory.getLpn();
                    Long quantity = lpnQuantityMap.getOrDefault(lpn, 0L);
                    lpnQuantityMap.put(lpn, quantity + inventory.getQuantity());
                });
        // make sure each LPN being consumed is in the location
        // and the consumed quantity doesn't exceed the LPN's quantity
        for (WorkOrderLineConsumeLPNTransaction workOrderLineConsumeLPNTransaction
                : workOrderLineConsumeTransaction.getWorkOrderLineConsumeLPNTransactions()) {

            if (!lpnQuantityMap.containsKey(workOrderLineConsumeLPNTransaction.getLpn())) {

                throw  WorkOrderException.raiseException(
                        "Can't consume the quantity for work order line " +
                                workOrderLineConsumeTransaction.getWorkOrderLine().getWorkOrder().getNumber() +
                                " / " + workOrderLineConsumeTransaction.getWorkOrderLine().getItem().getName() +
                                " from Production line " + productionLine.getName() +
                                ". LPN  " + workOrderLineConsumeLPNTransaction.getLpn() + " doesn't exists");
            }
            if(workOrderLineConsumeLPNTransaction.getConsumedQuantity()
                    > lpnQuantityMap.get(workOrderLineConsumeLPNTransaction.getLpn())) {

                throw  WorkOrderException.raiseException(
                        "Can't consume the quantity for work order line " +
                                workOrderLineConsumeTransaction.getWorkOrderLine().getWorkOrder().getNumber() +
                                " / " + workOrderLineConsumeTransaction.getWorkOrderLine().getItem().getName() +
                                " from Production line " + productionLine.getName() +
                                ". LPN's quantity " + lpnQuantityMap.get(workOrderLineConsumeLPNTransaction.getLpn()) +
                                " is less than the required quantity " + workOrderLineConsumeLPNTransaction.getConsumedQuantity() );
            }

        }
    }

    private void validateWorkOrderLineConsumeTransactionByDeliveredQuantity(
            WorkOrderLine workOrderLine,
            ProductionLine productionLine,
            Long consumingQuantity,
            boolean isOverConsumeAllowed) {
        Optional<ProductionLineDelivery> productionLineDelivery =
                Optional.ofNullable(
                        productionLineDeliveryService.getProductionLineDelivery(
                                productionLine,
                                workOrderLine
                        )
                );
        Long totalDeliveredQuantity = 0L;
        Long totalConsumedQuantity = 0L;
        if (productionLineDelivery.isPresent()) {
            totalDeliveredQuantity = productionLineDelivery.get().getDeliveredQuantity();
            totalConsumedQuantity = productionLineDelivery.get().getConsumedQuantity();
        }
        else {
            logger.debug(
                    "Can't consume for work order line  {}" +
                            " from Production line  {}. There's nothing delivered yet",

                    workOrderLine.getItem().getName(),
                    productionLine.getName()
            );


            throw  WorkOrderException.raiseException(
                    "Can't consume the quantity for work order line " +
                            workOrderLine.getItem().getName() +
                            " from Production line " + productionLine.getName() +
                            ". There's nothing delivered yet");
        }

        logger.debug("Validate by quantity / For work order line  {} 's consume transaction, " +
                        " we have total deliver quantity {}, consumed quantity {}, consuming quantity {}",

                workOrderLine.getNumber(),
                totalDeliveredQuantity,
                totalConsumedQuantity,
                consumingQuantity);
        if (isOverConsumeAllowed) {
            logger.debug(" over consume is allowed, skip quantity validation");
            return;
        }
        if  (totalDeliveredQuantity - totalConsumedQuantity < consumingQuantity) {

            throw  WorkOrderException.raiseException(
                    "Can't consume the quantity for work order line " +
                            workOrderLine.getWorkOrderNumber() +
                            " / " + workOrderLine.getItem().getName() +
                            " from Production line " + productionLine.getName() +
                            ". Not enough quantity left. we can only consume quantity " + (totalDeliveredQuantity - totalConsumedQuantity));
        }

    }
    private void validateWorkOrderLineConsumeTransactionByDeliveredQuantity(

            WorkOrderLineConsumeTransaction workOrderLineConsumeTransaction,
            ProductionLine productionLine,
            boolean isOverConsumeAllowed) {


        validateWorkOrderLineConsumeTransactionByDeliveredQuantity(
                workOrderLineConsumeTransaction.getWorkOrderLine(),
                productionLine,
                workOrderLineConsumeTransaction.getConsumedQuantity(),
                isOverConsumeAllowed
        );
    }

    private boolean validateWorkOrderLineConsumeTransaction(
            WorkOrderLineConsumeTransaction workOrderLineConsumeTransaction,
            List<Inventory> deliveredInventories) {
        Long totalDeliveredQuantity = deliveredInventories.stream().mapToLong(Inventory::getQuantity).sum();

        logger.debug("Validate by inventory / For work order line  {} 's consume transaction, we have total deliver quantity {}, consumed quantity {}",

                workOrderLineConsumeTransaction.getWorkOrderLine().getNumber(),
                totalDeliveredQuantity,
                workOrderLineConsumeTransaction.getConsumedQuantity());
        return totalDeliveredQuantity >= workOrderLineConsumeTransaction.getConsumedQuantity();
    }

    @Transactional
    public void consumeQuantity(WorkOrderLine workOrderLine, WorkOrderProduceTransaction workOrderProduceTransaction,
                                 Long totalProducedQuantity) {
        // only continue if we consume the quantity per transaction
        /***
        WorkOrderConfiguration workOrderConfiguration =
                workOrderConfigurationService.getWorkOrderConfiguration(
                        workOrderProduceTransaction.getWorkOrder().getWarehouse().getCompanyId(),
                        workOrderProduceTransaction.getWorkOrder().getWarehouseId()
                );
         **/
        WorkOrderMaterialConsumeTiming workOrderMaterialConsumeTiming =
                workOrderConfigurationService.getWorkOrderMaterialConsumeTiming(
                        workOrderProduceTransaction.getWorkOrder()
                );
        logger.debug("We configured to consume the work order line at {}",
                workOrderMaterialConsumeTiming);
        if (!workOrderMaterialConsumeTiming.equals(
                WorkOrderMaterialConsumeTiming.BY_TRANSACTION)){
            logger.debug("So we won't consume the work order line during the produce transaction");
            return;
        }

        // consume by BOM
        if (workOrderProduceTransaction.getConsumeByBomQuantity() == true) {
            logger.debug("we will consume by BOM quantity");
            consumeQuantityByBomQuantity(workOrderLine, workOrderProduceTransaction, totalProducedQuantity);
        }
        else {
            // USER specify the quantity to be consumed
            logger.debug("user specify the quantity to be consumed, ignore the BOM");
            workOrderProduceTransaction.getWorkOrderLineConsumeTransactions().forEach(workOrderLineConsumeTransaction -> {
                if (workOrderLine.equals(workOrderLineConsumeTransaction.getWorkOrderLine())) {

                    workOrderLineService.consume(workOrderLine,
                            workOrderLineConsumeTransaction,
                            workOrderProduceTransaction.getProductionLine());
                }
            });
        }
    }

    private void consumeQuantityByBomQuantity(WorkOrderLine workOrderLine,
                                              WorkOrderProduceTransaction workOrderProduceTransaction,
                                              Long totalProducedQuantity) {

        logger.debug("start to consume work order line {} / {}",
                workOrderLine.getNumber(),
                workOrderLine.getItem().getName());
        BillOfMaterial billOfMaterial = workOrderProduceTransaction.getConsumeByBom();
        if (Objects.isNull(billOfMaterial) || Strings.isBlank(billOfMaterial.getNumber())) {
            // if the user didn't specify a bom
            // then find most suitable bom
            logger.debug("bill of material is not setup in the transaction, let's find one ");
            billOfMaterial = billOfMaterialService.getMatchedBillOfMaterial(workOrderProduceTransaction.getWorkOrder());
        }

        if (Objects.isNull(billOfMaterial)) {
            throw WorkOrderException.raiseException("Can't consume the work order by bill of material as none is defined for this work order");
        }

        BillOfMaterial finalBillOfMaterial = billOfMaterial;
        billOfMaterial.getBillOfMaterialLines().forEach(billOfMaterialLine -> {
            if(billOfMaterialLineService.match(billOfMaterialLine, workOrderLine)) {
                Double billOfMaterialLineConsumeQuantity = billOfMaterialLine.getExpectedQuantity();
                Long consumedQuantity = (long)
                        Math.ceil(billOfMaterialLineConsumeQuantity * totalProducedQuantity * 1.0 / finalBillOfMaterial.getExpectedQuantity());

                workOrderLineService.consume(workOrderLine, consumedQuantity, workOrderProduceTransaction.getProductionLine());
            }
        });

    }

    private Inventory receiveInventoryFromWorkOrder(WorkOrder workOrder,
                                                    WorkOrderProducedInventory workOrderProducedInventory,
                                                    WorkOrderProduceTransaction workOrderProduceTransaction)   {
        logger.debug("Start to receive inventory from work order: \n{}", workOrder.getNumber());
        logger.debug("Inventory's item package typ is setup to \n{}",
                workOrderProducedInventory.getItemPackageType());

        // see if the LPN is a new LPN, if so, we will print the label for the new LPN(depends on the configuration)
        boolean newLPN = inventoryServiceRestemplateClient.findInventoryByLPN(
                                workOrder.getWarehouseId(),
                                workOrderProducedInventory.getLpn()).size() == 0;

        Inventory inventory = workOrderProducedInventory.createInventory(workOrder, workOrderProduceTransaction);


        inventory = inventoryServiceRestemplateClient.receiveInventoryFromWorkOrder(workOrder,inventory);

        if (newLPN) {
            logger.debug("We are producing a new LPN, let's see if we will need to print a LPN label for it");
            try {
                printNEWLPNLabel(inventory, workOrder, workOrderProduceTransaction.getProductionLine());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return inventory;
    }

    private void printNEWLPNLabel(Inventory inventory,
                                  WorkOrder workOrder,
                                  ProductionLine productionLine) throws JsonProcessingException {
        WarehouseConfiguration warehouseConfiguration
                = warehouseLayoutServiceRestemplateClient.getWarehouseConfiguration(workOrder.getWarehouseId());
        if (Objects.isNull(warehouseConfiguration) || !Boolean.TRUE.equals(warehouseConfiguration.getNewLPNPrintLabelAtProducingFlag())) {
            logger.debug("The warehouse is configured to not print LPN label for new LPN from work order");
            return;
        }
        String printerName = getPrinterName(productionLine);
        logger.debug("We will print LPN label for new LPN ");

        // warehouse is configured to print new lpn label when producing
        workOrderService.generatePrePrintLPNLabel(workOrder, inventory.getLpn(), inventory.getQuantity(),
                productionLine.getName(), "", printerName);
    }

    private String getPrinterName(ProductionLine productionLine) {
        return "";
    }




}
