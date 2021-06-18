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
    private WorkOrderLineService workOrderLineService;
    @Autowired
    private WorkOrderKPITransactionService workOrderKPITransactionService;
    @Autowired
    private ProductionLineService productionLineService;

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


    public List<WorkOrderProduceTransaction> findAll(Long warehouseId, String workOrderNumber, boolean genericQuery) {
        return findAll(warehouseId, workOrderNumber, genericQuery, true);
    }
    public List<WorkOrderProduceTransaction> findAll(Long warehouseId, String workOrderNumber,
                                                     boolean genericQuery, boolean loadDetails) {

        List<WorkOrderProduceTransaction> workOrderProduceTransactions
                =  workOrderProduceTransactionRepository.findAll(
                (Root<WorkOrderProduceTransaction> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (!StringUtils.isBlank(workOrderNumber)) {


                        Join<WorkOrderProduceTransaction, WorkOrder> joinWorkOrder = root.join("workOrder", JoinType.INNER);
                        if (genericQuery) {

                            predicates.add(criteriaBuilder.like(joinWorkOrder.get("number"), workOrderNumber));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinWorkOrder.get("number"), workOrderNumber));

                        }

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

        logger.debug("=====>   save workOrderProduceTransaction: \n{}", workOrderProduceTransaction);
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
    public WorkOrderProduceTransaction startNewTransaction(WorkOrderProduceTransaction workOrderProduceTransaction) {

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

        WorkOrder workOrder = workOrderProduceTransaction.getWorkOrder();
        // will load the work order's attribute. Some actions
        // are based on the work order's detail attribute, like the
        // location that for production line
        workOrderService.loadAttribute(workOrder);

        // make sure
        // 1. we are not over produce
        // 2. we are not over consume
        if (!validateWorkOrderProduceTransaction(workOrderProduceTransaction, workOrder)) {
             logger.debug("Current work order production transaction is not valid ");
             throw WorkOrderException.raiseException("Work work produce transaction is not valid");
        }
        // total work order produced quantity
        Long totalProducedQuantity = 0L;
        for(WorkOrderProducedInventory workOrderProducedInventory :
                workOrderProduceTransaction.getWorkOrderProducedInventories()) {
            // skip the record with incorrect value
            if (StringUtils.isBlank(workOrderProducedInventory.getLpn()) ||
                    Objects.isNull(workOrderProducedInventory.getInventoryStatus()) ||
                    Objects.isNull(workOrderProducedInventory.getItemPackageType()) ||
                    Objects.isNull(workOrderProducedInventory.getQuantity())  ) {
                continue;
            }
            totalProducedQuantity += workOrderProducedInventory.getQuantity();
            // Let's create the inventory
            receiveInventoryFromWorkOrder(workOrder, workOrderProducedInventory, workOrderProduceTransaction);

        }
        // Change the produced quantity of the work order
        workOrderService.produce(workOrder, totalProducedQuantity);

        // change each work order line's consumed quantity
        for (WorkOrderLine workOrderLine : workOrder.getWorkOrderLines()) {
            consumeQuantity(workOrderLine, workOrderProduceTransaction, totalProducedQuantity);
        }
        workOrderProduceTransaction.getWorkOrderByProductProduceTransactions().forEach(
                workOrderByProductProduceTransaction ->
                        workOrderByProductService.processWorkOrderByProductProduceTransaction(
                                workOrder, workOrderByProductProduceTransaction,
                                workOrderProduceTransaction.getProductionLine().getOutboundStageLocation()
                        )
        );

        WorkOrderProduceTransaction newWorkOrderProduceTransaction = save(workOrderProduceTransaction);

        workOrderKPITransactionService.processWorkOrderKIPTransaction(newWorkOrderProduceTransaction);

        return newWorkOrderProduceTransaction;

    }

    private boolean validateWorkOrderProduceTransaction(WorkOrderProduceTransaction workOrderProduceTransaction, WorkOrder workOrder) {
        // make sure we won't over consume

        boolean result;

        List<Inventory> inventories = workOrderService.getDeliveredInventory(workOrder.getId(),
                workOrderProduceTransaction.getProductionLine());
        logger.debug("start to validate work order product transaction against production line: {}",
                workOrderProduceTransaction.getProductionLine().getName());
        logger.debug("We have following invenotry delivered in this production");
        inventories.forEach(inventory ->
                logger.debug(">> inventory {}, location: {}, quantity: {}",
                        inventory.getLpn(), inventory.getLocation().getName(), inventory.getQuantity() ));
        // check if we have enough inventory delivered for the work order line
        result = !workOrderProduceTransaction.getWorkOrderLineConsumeTransactions()
                .stream().anyMatch(
                        workOrderLineConsumeTransaction -> {
                            List<Inventory> deliveredInventories = inventories.stream().filter(
                                    inventory -> inventory.getItem().getId().equals(
                                            workOrderLineConsumeTransaction.getWorkOrderLine().getItemId()
                                            )
                                    ).collect(Collectors.toList());

                            boolean lineValid = validateWorkOrderLineConsumeTransaction(
                                        workOrderLineConsumeTransaction,deliveredInventories
                                   );

                            logger.debug("For work order line {} / {} 's consume transaction is valid? {}",
                                    workOrder.getNumber(),
                                    workOrderLineConsumeTransaction.getWorkOrderLine().getNumber(),
                                    lineValid);

                            return !lineValid;
                        });
        return result;
    }

    private boolean validateWorkOrderLineConsumeTransaction(
            WorkOrderLineConsumeTransaction workOrderLineConsumeTransaction, List<Inventory> deliveredInventories) {
        Long totalDeliveredQuantity = deliveredInventories.stream().mapToLong(Inventory::getQuantity).sum();

        logger.debug("For work order line  {} 's consume transaction, we have total deliver quantity {}, consumed quantity {}",

                workOrderLineConsumeTransaction.getWorkOrderLine().getNumber(),
                totalDeliveredQuantity,
                workOrderLineConsumeTransaction.getConsumedQuantity());
        return totalDeliveredQuantity >= workOrderLineConsumeTransaction.getConsumedQuantity();
    }

    @Transactional
    public void consumeQuantity(WorkOrderLine workOrderLine, WorkOrderProduceTransaction workOrderProduceTransaction,
                                 Long totalProducedQuantity) {
        if (workOrderProduceTransaction.getConsumeByBomQuantity() == true) {
            consumeQuantityByBomQuantity(workOrderLine, workOrderProduceTransaction, totalProducedQuantity);
        }
        else {
            workOrderProduceTransaction.getWorkOrderLineConsumeTransactions().forEach(workOrderLineConsumeTransaction -> {
                if (workOrderLine.equals(workOrderLineConsumeTransaction.getWorkOrderLine())) {

                    workOrderLineService.consume(workOrderLine,
                            workOrderLineConsumeTransaction.getConsumedQuantity(),
                            workOrderProduceTransaction.getProductionLine());
                }
            });
        }
    }

    private void consumeQuantityByBomQuantity(WorkOrderLine workOrderLine,
                                              WorkOrderProduceTransaction workOrderProduceTransaction,
                                              Long totalProducedQuantity) {

        BillOfMaterial billOfMaterial = billOfMaterialService.getMatchedBillOfMaterial(workOrderProduceTransaction.getWorkOrder());
        if (Objects.isNull(billOfMaterial)) {
            throw WorkOrderException.raiseException("Can't consume the work order by bill of material as none is defined for this work order");
        }

        billOfMaterial.getBillOfMaterialLines().forEach(billOfMaterialLine -> {
            if(billOfMaterialLineService.match(billOfMaterialLine, workOrderLine)) {
                Long billOfMaterialLineConsumeQuantity = billOfMaterialLine.getExpectedQuantity();
                Long consumedQuantity = billOfMaterialLineConsumeQuantity * totalProducedQuantity /billOfMaterial.getExpectedQuantity();

                workOrderLineService.consume(workOrderLine, consumedQuantity, workOrderProduceTransaction.getProductionLine());
            }
        });

    }

    private Inventory receiveInventoryFromWorkOrder(WorkOrder workOrder,
                                                    WorkOrderProducedInventory workOrderProducedInventory,
                                                    WorkOrderProduceTransaction workOrderProduceTransaction) {
        logger.debug("Start to receive inventory from work order: \n{}", workOrder.getNumber());
        Inventory inventory = workOrderProducedInventory.createInventory(workOrder, workOrderProduceTransaction);

        return inventoryServiceRestemplateClient.receiveInventoryFromWorkOrder(inventory);
    }




}
