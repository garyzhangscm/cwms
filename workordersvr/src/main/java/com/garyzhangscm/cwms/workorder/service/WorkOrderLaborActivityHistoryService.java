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

import com.garyzhangscm.cwms.workorder.exception.ProductionLineException;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderLaborActivityHistoryRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class WorkOrderLaborActivityHistoryService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderLaborActivityHistoryService.class);

    @Autowired
    private WorkOrderLaborActivityHistoryRepository workOrderLaborHistoryActivityRepository;

    @Autowired
    private ProductionLineService productionLineService;

    public WorkOrderLaborActivityHistory findById(Long id) {
        return workOrderLaborHistoryActivityRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order labor activity history record not found by id: " + id));
    }



    public List<WorkOrderLaborActivityHistory> findAll(Long warehouseId,
                                                        Long productionLineId,
                                                        String username) {
        return workOrderLaborHistoryActivityRepository.findAll(
                (Root<WorkOrderLaborActivityHistory> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(productionLineId) || Strings.isNotBlank(username)) {
                        Join<WorkOrderLaborActivityHistory, WorkOrderLabor> joinWorkOrderLabor = root.join("workOrderLabor", JoinType.INNER);

                        if (Objects.nonNull(productionLineId)) {
                            Join<WorkOrderLabor, ProductionLine> joinProductionLine = joinWorkOrderLabor.join("productionLine", JoinType.INNER);

                            predicates.add(criteriaBuilder.equal(joinProductionLine.get("id"), productionLineId));
                        }
                        if (Strings.isNotBlank(username)) {

                            predicates.add(criteriaBuilder.equal(joinWorkOrderLabor.get("username"), username));
                        }
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "workOrderLabor.productionLine")
        );

    }


    public WorkOrderLaborActivityHistory save(WorkOrderLaborActivityHistory workOrderLaborActivityHistory) {
        return workOrderLaborHistoryActivityRepository.save(workOrderLaborActivityHistory);
    }


    public void delete(WorkOrderLaborActivityHistory workOrderLaborActivityHistory) {
        workOrderLaborHistoryActivityRepository.delete(workOrderLaborActivityHistory);
    }

    public void delete(Long id) {
        workOrderLaborHistoryActivityRepository.deleteById(id);
    }


    public WorkOrderLaborActivityHistory checkInUser(Long warehouseId, WorkOrderLabor workOrderLabor,
                                                     String originalValue, String username) {
        WorkOrderLaborActivityHistory workOrderLaborActivityHistory = new WorkOrderLaborActivityHistory(
                warehouseId, workOrderLabor, WorkOrderLaborActivityType.CHECK_IN,
                originalValue, workOrderLabor.getLastCheckInTime().toString(),
                LocalDateTime.now(), username
        );
        return save(workOrderLaborActivityHistory);
    }


    public WorkOrderLaborActivityHistory checkOutUser(Long warehouseId, WorkOrderLabor workOrderLabor,
                                                      String originalValue, String username) {
        WorkOrderLaborActivityHistory workOrderLaborActivityHistory = new WorkOrderLaborActivityHistory(
                warehouseId, workOrderLabor, WorkOrderLaborActivityType.CHECK_OUT,
                originalValue, workOrderLabor.getLastCheckOutTime().toString(),
                LocalDateTime.now(), username
        );
        return save(workOrderLaborActivityHistory);
    }

    public WorkOrderLaborActivityHistory addLabor(Long warehouseId, WorkOrderLabor workOrderLabor,
                          String originalValue, String username) {
        WorkOrderLaborActivityHistory workOrderLaborActivityHistory = new WorkOrderLaborActivityHistory(
                warehouseId, workOrderLabor, WorkOrderLaborActivityType.MANUAL_MODIFY,
                originalValue,
                workOrderLabor.getLastCheckInTime().toString() + "|" + workOrderLabor.getLastCheckOutTime().toString(),
                LocalDateTime.now(), username
        );
        return save(workOrderLaborActivityHistory);
    }

    public WorkOrderLaborActivityHistory changeLabor(Long warehouseId, WorkOrderLabor workOrderLabor, String originalValue, String username) {

        WorkOrderLaborActivityHistory workOrderLaborActivityHistory = new WorkOrderLaborActivityHistory(
                warehouseId, workOrderLabor, WorkOrderLaborActivityType.MANUAL_MODIFY,
                originalValue,
                workOrderLabor.getLastCheckInTime().toString() + "|" + workOrderLabor.getLastCheckOutTime().toString(),
                LocalDateTime.now(), username
        );
        return save(workOrderLaborActivityHistory);
    }
}
