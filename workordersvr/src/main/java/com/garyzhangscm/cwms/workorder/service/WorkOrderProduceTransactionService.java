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


    public List<WorkOrderProduceTransaction> findAll(Long warehouseId, String workOrderNumber) {
        return findAll(warehouseId, workOrderNumber, true);
    }
    public List<WorkOrderProduceTransaction> findAll(Long warehouseId, String workOrderNumber, boolean loadDetails) {

        List<WorkOrderProduceTransaction> workOrderProduceTransactions
                =  workOrderProduceTransactionRepository.findAll(
                (Root<WorkOrderProduceTransaction> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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


        WorkOrder workOrder = workOrderProduceTransaction.getWorkOrder();
        // will load the work order's attribute. Some actions
        // are based on the work order's detail attribute, like the
        // location that for production line
        workOrderService.loadAttribute(workOrder);
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
                                workOrder, workOrderByProductProduceTransaction
                        )
        );

        WorkOrderProduceTransaction newWorkOrderProduceTransaction = save(workOrderProduceTransaction);

        workOrderKPITransactionService.processWorkOrderKIPTransaction(newWorkOrderProduceTransaction);

        return newWorkOrderProduceTransaction;

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

                    workOrderLineService.consume(workOrderLine, workOrderLineConsumeTransaction.getConsumedQuantity());
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

                workOrderLineService.consume(workOrderLine, consumedQuantity);
            }
        });

    }

    private Inventory receiveInventoryFromWorkOrder(WorkOrder workOrder,
                                                    WorkOrderProducedInventory workOrderProducedInventory,
                                                    WorkOrderProduceTransaction workOrderProduceTransaction) {
        logger.debug("Start to receive inventory from work order: \n{}", workOrder);
        Inventory inventory = workOrderProducedInventory.createInventory(workOrder, workOrderProduceTransaction);

        return inventoryServiceRestemplateClient.receiveInventoryFromWorkOrder(inventory);
    }




}
