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

import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderReverseProductionInventoryRepository;
import jakarta.persistence.criteria.*;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class WorkOrderReverseProductionInventoryService  {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderReverseProductionInventoryService.class);

    @Autowired
    private WorkOrderReverseProductionInventoryRepository workOrderReverseProductionInventoryRepository;

    public WorkOrderReverseProductionInventory findById(Long id) {
        return workOrderReverseProductionInventoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order reverse production not found by id: " + id));
    }



    public List<WorkOrderReverseProductionInventory> findAll(Long workOrderProduceTransactionId,
                                                             String workOrderProduceTransactionIds,
                                                             String lpn) {
        return workOrderReverseProductionInventoryRepository.findAll(
                (Root<WorkOrderReverseProductionInventory> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (StringUtils.isNotBlank(lpn)) {
                            predicates.add(criteriaBuilder.equal(root.get("lpn"), lpn));

                    }
                    if (Objects.nonNull(workOrderProduceTransactionId)) {
                        Join<WorkOrderReverseProductionInventory, WorkOrderProduceTransaction> joinWorkOrderProduceTransaction
                                = root.join("workOrderProduceTransaction", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinWorkOrderProduceTransaction.get("id"), workOrderProduceTransactionId));
                    }
                    if (Strings.isNotBlank(workOrderProduceTransactionIds)) {
                        Join<WorkOrderReverseProductionInventory, WorkOrderProduceTransaction> joinWorkOrderProduceTransaction
                                = root.join("workOrderProduceTransaction", JoinType.INNER);

                        CriteriaBuilder.In<Long> inWorkOrderProduceTransactionIds = criteriaBuilder.in(joinWorkOrderProduceTransaction.get("id"));
                        for(String id : workOrderProduceTransactionIds.split(",")) {
                            inWorkOrderProduceTransactionIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inWorkOrderProduceTransactionIds));


                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

    }



    public WorkOrderReverseProductionInventory save(WorkOrderReverseProductionInventory workOrderReverseProductionInventory) {
        return workOrderReverseProductionInventoryRepository.save(workOrderReverseProductionInventory);
    }


    public void delete(WorkOrderReverseProductionInventory workOrderReverseProductionInventory) {
        workOrderReverseProductionInventoryRepository.delete(workOrderReverseProductionInventory);
    }

    public void delete(Long id) {
        workOrderReverseProductionInventoryRepository.deleteById(id);
    }



}
