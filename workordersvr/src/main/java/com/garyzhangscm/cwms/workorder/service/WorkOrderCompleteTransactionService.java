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

import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderCompleteTransactionRepository;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderProduceTransactionRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.*;


@Service
public class WorkOrderCompleteTransactionService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderCompleteTransactionService.class);

    @Autowired
    private WorkOrderCompleteTransactionRepository workOrderCompleteTransactionRepository;
    @Autowired
    private WorkOrderKPITransactionService workOrderKPITransactionService;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private WorkOrderByProductService workOrderByProductService;
    @Autowired
    private BillOfMaterialService billOfMaterialService;
    @Autowired
    private BillOfMaterialLineService billOfMaterialLineService;
    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private WorkOrderLineService workOrderLineService;
    @Autowired
    private ProductionLineAssignmentService productionLineAssignmentService;

    public WorkOrderCompleteTransaction findById(Long id, boolean loadDetails) {
        WorkOrderCompleteTransaction workOrderCompleteTransaction
                = workOrderCompleteTransactionRepository.findById(id)
                    .orElseThrow(() -> ResourceNotFoundException.raiseException("work order complete transaction not found by id: " + id));
        if (loadDetails) {
            loadAttribute(workOrderCompleteTransaction);
        }
        return workOrderCompleteTransaction;
    }

    public WorkOrderCompleteTransaction findById(Long id) {
        return findById(id, true);
    }


    public List<WorkOrderCompleteTransaction> findAll(Long warehouseId, String workOrderNumber) {
        return findAll(warehouseId, workOrderNumber, true);
    }
    public List<WorkOrderCompleteTransaction> findAll(Long warehouseId, String workOrderNumber, boolean loadDetails) {

        List<WorkOrderCompleteTransaction> workOrderCompleteTransactions
                =  workOrderCompleteTransactionRepository.findAll(
                (Root<WorkOrderCompleteTransaction> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (!StringUtils.isBlank(workOrderNumber)) {

                        Join<WorkOrderProduceTransaction, WorkOrder> joinWorkOrder = root.join("workOrder", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinWorkOrder.get("number"), workOrderNumber));

                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (workOrderCompleteTransactions.size() > 0 && loadDetails) {
            loadAttribute(workOrderCompleteTransactions);
        }
        return workOrderCompleteTransactions;
    }


    public void loadAttribute(List<WorkOrderCompleteTransaction> workOrderCompleteTransactions) {
        for (WorkOrderCompleteTransaction workOrderCompleteTransaction : workOrderCompleteTransactions) {
            loadAttribute(workOrderCompleteTransaction);
        }
    }

    public void loadAttribute(WorkOrderCompleteTransaction workOrderCompleteTransaction) {

        if (workOrderCompleteTransaction.getWorkOrder().getItemId() != null &&
                workOrderCompleteTransaction.getWorkOrder().getItem() == null) {
            workOrderCompleteTransaction.getWorkOrder().setItem(
                    inventoryServiceRestemplateClient.getItemById(
                            workOrderCompleteTransaction.getWorkOrder().getItemId()));
        }
        if (workOrderCompleteTransaction.getWorkOrder().getWarehouseId() != null &&
                workOrderCompleteTransaction.getWorkOrder().getWarehouse() == null) {
            workOrderCompleteTransaction.getWorkOrder().setWarehouse(
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(
                            workOrderCompleteTransaction.getWorkOrder().getWarehouseId()));
        }



    }



    public WorkOrderCompleteTransaction save(WorkOrderCompleteTransaction workOrderCompleteTransaction) {
        // setup the work order produce transaction reference for
        // WorkOrderLineConsumeTransaction and WorkOrderProducedInventory array so that
        // the change can be cascaded
        workOrderCompleteTransaction.getWorkOrderLineCompleteTransactions().forEach(
                workOrderLineCompleteTransaction -> {
                    workOrderLineCompleteTransaction.setWorkOrderCompleteTransaction(workOrderCompleteTransaction);
                    workOrderLineCompleteTransaction.getReturnMaterialRequests().forEach(
                            returnMaterialRequest ->
                                    returnMaterialRequest.setWorkOrderLineCompleteTransaction(
                                            workOrderLineCompleteTransaction
                                    )
                    );
                });

        logger.debug("=====>   save workOrderCompleteTransaction: \n{}", workOrderCompleteTransaction);
        WorkOrderCompleteTransaction newWorkOrderCompleteTransaction
                = workOrderCompleteTransactionRepository.save(workOrderCompleteTransaction);
        loadAttribute(newWorkOrderCompleteTransaction);
        return newWorkOrderCompleteTransaction;
    }



    public void delete(WorkOrderCompleteTransaction workOrderProduceTransaction) {
        workOrderCompleteTransactionRepository.delete(workOrderProduceTransaction);
    }

    public void delete(Long id) {
        workOrderCompleteTransactionRepository.deleteById(id);
    }

    /**
     * Start a new transaction.
     * 1. Save the transaction
     * 2. Create the inventory for returned material, if any
     * 3. update the consumed quantity and scrapped quantity
     * @param workOrderCompleteTransaction
     * @return
     */

    public WorkOrderCompleteTransaction startNewTransaction(WorkOrderCompleteTransaction workOrderCompleteTransaction, Long locationId) {
        if (Objects.isNull(locationId)) {
            // the user didn't specify any location, choose any production
            List<ProductionLineAssignment> productionLineAssignments =
                    productionLineAssignmentService.findAll(null, null, workOrderCompleteTransaction.getWorkOrder().getId(), null);
            logger.debug("We get {} production line assignment for work order {} when closing this work order",
                    productionLineAssignments.size(), workOrderCompleteTransaction.getWorkOrder().getNumber());
            logger.debug("We will choose the first production line to complete the work order and receive returned material");
            if (productionLineAssignments.size() > 0) {
                locationId = productionLineAssignments.get(0).getProductionLine().getOutboundStageLocationId();
            }
            else {
                throw WorkOrderException.raiseException("Can't close work order. We are not able to find a good location to return material");
            }
        }

        logger.debug("Will close the work order from location {}",
                warehouseLayoutServiceRestemplateClient.getLocationById(locationId).getName());
        return startNewTransaction(
                workOrderCompleteTransaction,
                warehouseLayoutServiceRestemplateClient.getLocationById(locationId)

        );
    }
    public WorkOrderCompleteTransaction startNewTransaction(WorkOrderCompleteTransaction workOrderCompleteTransaction, Location location) {

        // Let's make sure the complete transaction is the right transaction
        WorkOrder workOrder = workOrderCompleteTransaction.getWorkOrder();


        // OK, let's create the inventory for returned material first

        for (WorkOrderLineCompleteTransaction workOrderLineCompleteTransaction : workOrderCompleteTransaction.getWorkOrderLineCompleteTransactions()) {
            processWorkOrderLineCompleteTransaction(workOrderCompleteTransaction,
                    workOrderLineCompleteTransaction, location);

        }
        for (WorkOrderByProductProduceTransaction workOrderByProductProduceTransaction : workOrderCompleteTransaction.getWorkOrderByProductProduceTransactions()) {
            processWorkOrderByProductProduceTransaction(workOrderCompleteTransaction,
                    workOrderByProductProduceTransaction,
                    location);
        }


        WorkOrderCompleteTransaction newWorkOrderCompleteTransaction = save(workOrderCompleteTransaction);

        workOrderKPITransactionService.processWorkOrderKIPTransaction(newWorkOrderCompleteTransaction);

        // let's change the status of the work order
        workOrderService.completeWorkOrder(workOrder);

        deassignProductLine(workOrder);
        // save the transaction
        return save(workOrderCompleteTransaction);

    }

    private void deassignProductLine(WorkOrder workOrder) {
        logger.debug("Remove production line assignment for work order {} as it is closed",
                workOrder.getNumber());
        productionLineAssignmentService.removeProductionLineAssignmentForWorkOrder(workOrder.getId());

    }

    private void processWorkOrderByProductProduceTransaction(WorkOrderCompleteTransaction workOrderCompleteTransaction,
                                                             WorkOrderByProductProduceTransaction workOrderByProductProduceTransaction,
                                                             Location location) {

        workOrderByProductService.processWorkOrderByProductProduceTransaction(workOrderCompleteTransaction,
                workOrderByProductProduceTransaction,
                location);
    }



    private void processWorkOrderLineCompleteTransaction(WorkOrderCompleteTransaction workOrderCompleteTransaction,
                                                         WorkOrderLineCompleteTransaction workOrderLineCompleteTransaction,
                                                         Location location) {
        WorkOrderLine workOrderLine = workOrderLineCompleteTransaction.getWorkOrderLine();
        workOrderLine.setWorkOrder(workOrderCompleteTransaction.getWorkOrder());

        Long deliveredQuantity = workOrderLineCompleteTransaction.getWorkOrderLine().getDeliveredQuantity();
        Long consumedQuantity = Objects.isNull(workOrderLineCompleteTransaction.getAdjustedConsumedQuantity()) ?
                workOrderLineCompleteTransaction.getWorkOrderLine().getConsumedQuantity() :
                workOrderLineCompleteTransaction.getAdjustedConsumedQuantity();
        Long scrappedQuantity = Objects.isNull(workOrderLineCompleteTransaction.getScrappedQuantity()) ?
                0L : workOrderLineCompleteTransaction.getScrappedQuantity();

        Long returnedMaterialsQuantity = workOrderLineCompleteTransaction.getReturnMaterialRequests().size() > 0 ?
                workOrderLineCompleteTransaction.getReturnMaterialRequests().stream()
                        .map(ReturnMaterialRequest::getQuantity).mapToLong(Long::longValue).sum()
                : 0L;

        if (!deliveredQuantity.equals(consumedQuantity + scrappedQuantity + returnedMaterialsQuantity)) {
            throw WorkOrderException.raiseException("work order " + workOrderCompleteTransaction.getWorkOrder().getNumber()
                    +",  line " + workOrderLineCompleteTransaction.getWorkOrderLine().getNumber() +
                    ",  quantity doesn't match! \n " +
                    "deliveredQuantity: " + deliveredQuantity +
                    "consumedQuantity: " + consumedQuantity +
                    "scrappedQuantity: " + scrappedQuantity +
                    "returnedMaterialsQuantity: " + returnedMaterialsQuantity);
        }

        // Receive the returned material
        workOrderLineCompleteTransaction.getReturnMaterialRequests().stream().forEach(
                returnMaterialRequest -> returnMaterial(workOrderCompleteTransaction.getWorkOrder(),
                        workOrderLine, returnMaterialRequest, location)
        );

        // Update the work order line
        workOrderLineService.completeWorkOrderLine(workOrderLine,
                consumedQuantity, scrappedQuantity, returnedMaterialsQuantity);

    }

    private Inventory returnMaterial(WorkOrder workOrder,
                                WorkOrderLine workOrderLine,
                                ReturnMaterialRequest returnMaterialRequest,
                                     Location location) {

        logger.debug("Start to return material from work order: {}, line {}",
                workOrder.getNumber(), workOrderLine.getNumber());
        Inventory inventory = returnMaterialRequest.createInventory(workOrder, workOrderLine, location);

        return inventoryServiceRestemplateClient.receiveInventoryFromWorkOrder(workOrder, inventory);
    }


}
