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
import com.garyzhangscm.cwms.workorder.clients.OutboundServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderKPIRepository;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderKPITransactionRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.util.*;


@Service
public class WorkOrderKPITransactionService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderKPITransactionService.class);

    @Autowired
    private WorkOrderKPITransactionRepository workOrderKPITransactionRepository;
    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private WorkOrderKPIService workOrderKPIService;


    public WorkOrderKPITransaction findById(Long id) {
        return workOrderKPITransactionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order KPI transaction not found by id: " + id));
    }



    public List<WorkOrderKPITransaction> findAll(Long warehouseId, WorkOrder workOrder,
                                                 String username, String workingTeamName,
                                                 String workOrderNumber) {
        return workOrderKPITransactionRepository.findAll(
                (Root<WorkOrderKPITransaction> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(warehouseId) || Strings.isNotBlank(workOrderNumber)) {

                        Join<WorkOrderKPI, WorkOrder> joinWorkOrder = root.join("workOrder", JoinType.INNER);
                        if (Objects.nonNull(warehouseId) ) {

                            predicates.add(criteriaBuilder.equal(joinWorkOrder.get("warehouseId"), warehouseId));
                        }
                        if (Strings.isNotBlank(workOrderNumber)) {
                            predicates.add(criteriaBuilder.equal(joinWorkOrder.get("number"), workOrderNumber));
                        }
                    }


                    if (Objects.nonNull(workOrder)) {
                        predicates.add(criteriaBuilder.equal(root.get("workOrder"), workOrder));

                    }
                    if (StringUtils.isNotBlank(username)) {
                        predicates.add(criteriaBuilder.equal(root.get("username"), username));

                    }
                    if (StringUtils.isNotBlank(workingTeamName)) {
                        predicates.add(criteriaBuilder.equal(root.get("workingTeamName"), workingTeamName));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }



/**
    public void loadAttribute(List<WorkOrderKPITransaction> workOrderKPITransactions) {
        for (WorkOrderKPITransaction workOrderKPITransaction : workOrderKPITransactions) {
            loadAttribute(workOrderKPITransaction);
        }
    }

    public void loadAttribute(WorkOrderKPITransaction workOrderKPITransaction) {

        workOrderService.loadAttribute(workOrderKPITransaction.getWorkOrder());


    }
 **/

    public List<WorkOrderKPITransaction> findByWorkOrder(WorkOrder workOrder) {
        Long warehouseId = Objects.isNull(workOrder) ? null : workOrder.getWarehouseId();
        return findAll(warehouseId, workOrder, null, null, null);
    }




    public WorkOrderKPITransaction save(WorkOrderKPITransaction workOrderKPITransaction) {
        return workOrderKPITransactionRepository.save(workOrderKPITransaction);
    }

    public void delete(WorkOrderKPITransaction workOrderKPITransaction) {
        workOrderKPITransactionRepository.delete(workOrderKPITransaction);
    }

    public void delete(Long id) {
        workOrderKPITransactionRepository.deleteById(id);
    }


    /**
     * @param workOrderProduceTransaction
     */
    @Transactional
    public void processWorkOrderKIPTransaction(WorkOrderProduceTransaction workOrderProduceTransaction) {

        workOrderProduceTransaction.getWorkOrderKPITransactions().forEach(workOrderKPITransaction -> {
            if (Objects.isNull(workOrderKPITransaction.getWorkOrderProduceTransaction())) {
                workOrderKPITransaction.setWorkOrderProduceTransaction(
                        workOrderProduceTransaction
                );
            }
            if (Objects.isNull(workOrderKPITransaction.getWorkOrder())) {
                workOrderKPITransaction.setWorkOrder(
                        workOrderProduceTransaction.getWorkOrder()
                );
            }

            workOrderKPITransaction = save(workOrderKPITransaction);

            WorkOrderKPI workOrderKPI = workOrderKPITransaction.getWorkOrderKPI();
            workOrderKPIService.processWorkOrderKPIWhenProducingFinishGood(workOrderKPI);

        });

    }

    /**
     * @param workOrderCompleteTransaction
     */
    @Transactional
    public void processWorkOrderKIPTransaction(WorkOrderCompleteTransaction workOrderCompleteTransaction) {


        workOrderCompleteTransaction.getWorkOrderKPITransactions().forEach(workOrderKPITransaction -> {
            if (Objects.isNull(workOrderKPITransaction.getWorkOrderCompleteTransaction())) {
                workOrderKPITransaction.setWorkOrderCompleteTransaction(
                        workOrderCompleteTransaction
                );
            }
            if (Objects.isNull(workOrderKPITransaction.getWorkOrder())) {
                workOrderKPITransaction.setWorkOrder(
                        workOrderCompleteTransaction.getWorkOrder()
                );
            }

            workOrderKPITransaction = save(workOrderKPITransaction);

            workOrderKPIService.processWorkOrderKPIWhenCompletingWorkOrder(
                    workOrderKPITransaction
            );


        });



    }



}
