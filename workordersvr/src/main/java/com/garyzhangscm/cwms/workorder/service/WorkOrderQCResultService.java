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

import com.garyzhangscm.cwms.workorder.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderQCResultRepository;
import jakarta.persistence.criteria.*;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class WorkOrderQCResultService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderQCResultService.class);

    @Autowired
    private WorkOrderQCResultRepository workOrderQCResultRepository;
    @Autowired
    private UserService userService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;


    public WorkOrderQCResult findById(Long id) {
        return workOrderQCResultRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order qc result not found by id: " + id));
    }



    public List<WorkOrderQCResult> findAll(Long warehouseId, String number,
                                           String workOrderSampleNumber,
                                           Long productionLineAssignmentId,
                                           Long productionLineId,
                                           Long workOrderId,
                                           String workOrderNumber) {
        return workOrderQCResultRepository.findAll(
                (Root<WorkOrderQCResult> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(number)) {
                            predicates.add(criteriaBuilder.equal(root.get("number"), number));
                    }

                    if (StringUtils.isNotBlank(workOrderSampleNumber)) {

                        Join<WorkOrderQCResult, WorkOrderQCSample> joinWorkOrderQCSample
                                = root.join("workOrderQCSample", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinWorkOrderQCSample.get("number"), workOrderSampleNumber));
                    }


                    if (Objects.nonNull(productionLineAssignmentId)) {

                        Join<WorkOrderQCResult, WorkOrderQCSample> joinWorkOrderQCSample
                                = root.join("workOrderQCSample", JoinType.INNER);
                        Join<WorkOrderQCSample, ProductionLineAssignment> joinProductionLineAssignment
                                = joinWorkOrderQCSample.join("productionLineAssignment", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinProductionLineAssignment.get("id"), productionLineAssignmentId));
                    }

                    if (Objects.nonNull(productionLineId)) {

                        Join<WorkOrderQCResult, WorkOrderQCSample> joinWorkOrderQCSample
                                = root.join("workOrderQCSample", JoinType.INNER);
                        Join<WorkOrderQCSample, ProductionLineAssignment> joinProductionLineAssignment
                                = joinWorkOrderQCSample.join("productionLineAssignment", JoinType.INNER);
                        Join<ProductionLineAssignment, ProductionLine> joinProductionLine
                                = joinProductionLineAssignment.join("productionLine", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinProductionLine.get("id"), productionLineId));
                    }
                    if (Objects.nonNull(workOrderId)) {

                        Join<WorkOrderQCResult, WorkOrderQCSample> joinWorkOrderQCSample
                                = root.join("workOrderQCSample", JoinType.INNER);
                        Join<WorkOrderQCSample, ProductionLineAssignment> joinProductionLineAssignment
                                = joinWorkOrderQCSample.join("productionLineAssignment", JoinType.INNER);
                        Join<ProductionLineAssignment, WorkOrder> joinWorkOrder
                                = joinProductionLineAssignment.join("workOrder", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinWorkOrder.get("id"), workOrderId));
                    }
                    if (Strings.isNotBlank(workOrderNumber)) {

                        Join<WorkOrderQCResult, WorkOrderQCSample> joinWorkOrderQCSample
                                = root.join("workOrderQCSample", JoinType.INNER);
                        Join<WorkOrderQCSample, ProductionLineAssignment> joinProductionLineAssignment
                                = joinWorkOrderQCSample.join("productionLineAssignment", JoinType.INNER);
                        Join<ProductionLineAssignment, WorkOrder> joinWorkOrder
                                = joinProductionLineAssignment.join("workOrder", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinWorkOrder.get("number"), workOrderNumber));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.DESC, "number")
        );

    }

    
    public WorkOrderQCResult save(WorkOrderQCResult workOrderQCResult) {


        if (Objects.isNull(workOrderQCResult.getNumber())) {
            workOrderQCResult.setNumber(
                    getNextNumber(workOrderQCResult.getWarehouseId())
            );
        }
        if (Strings.isBlank(workOrderQCResult.getQcUsername())) {
            workOrderQCResult.setQcUsername(
                    userService.getCurrentUserName()
            );
        }
        if (Objects.isNull(workOrderQCResult.getQcTime())) {
            workOrderQCResult.setQcTime(LocalDateTime.now());
        }
        return workOrderQCResultRepository.save(workOrderQCResult);
    }

    private String getNextNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "work-order-qc-result-number");
    }

    public WorkOrderQCResult saveOrUpdate(WorkOrderQCResult workOrderQCResult) {

        if (workOrderQCResult.getId() == null &&
                findByNumber(workOrderQCResult.getWarehouseId(), workOrderQCResult.getNumber()) != null) {
            workOrderQCResult.setId(
                    findByNumber(workOrderQCResult.getWarehouseId(), workOrderQCResult.getNumber()).getId());
        }
        return save(workOrderQCResult);
    }

    private WorkOrderQCResult findByNumber(Long warehouseId, String number) {
        return workOrderQCResultRepository.findByWarehouseIdAndNumber(
                warehouseId, number
        );
    }


    public void delete(WorkOrderQCResult workOrderQCSample) {
        workOrderQCResultRepository.delete(workOrderQCSample);
    }

    public void delete(Long id) {
        workOrderQCResultRepository.deleteById(id);
    }




    public WorkOrderQCResult addWorkOrderQCResult(WorkOrderQCResult workOrderQCSample) {
        return saveOrUpdate(workOrderQCSample);
    }

}
