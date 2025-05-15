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
import com.garyzhangscm.cwms.workorder.model.WorkOrder;
import com.garyzhangscm.cwms.workorder.model.WorkOrderFlow;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderFlowRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class WorkOrderFlowService  {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderFlowService.class);

    @Autowired
    private WorkOrderFlowRepository workOrderFlowRepository;

    @Autowired
    private WorkOrderService workOrderService;


    public WorkOrderFlow findById(Long id) {
        return workOrderFlowRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order flow not found by id: " + id));
    }



    public List<WorkOrderFlow> findAll(Long warehouseId, String name, String description) {
        return workOrderFlowRepository.findAll(
                (Root<WorkOrderFlow> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(name)) {

                        if (name.contains("*")) {

                            predicates.add(criteriaBuilder.like(root.get("name"), name.replaceAll("\\*", "%")));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("name"), name));
                        }

                    }

                    if (StringUtils.isNotBlank(description)) {
                        if (description.contains("*")) {

                            predicates.add(criteriaBuilder.like(root.get("description"), description.replaceAll("\\*", "%")));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("description"), description));
                        }

                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "name")
        );

    }

    public WorkOrderFlow findByName(Long warehouseId, String name) {

        return workOrderFlowRepository.findByWarehouseIdAndName(warehouseId, name);
    }

    public WorkOrderFlow save(WorkOrderFlow workOrderFlow) {
        return workOrderFlowRepository.save(workOrderFlow);
    }

    public WorkOrderFlow saveOrUpdate(WorkOrderFlow workOrderFlow) {
        if (workOrderFlow.getId() == null &&
                findByName(workOrderFlow.getWarehouseId(), workOrderFlow.getName()) != null) {
            workOrderFlow.setId(
                    findByName(workOrderFlow.getWarehouseId(), workOrderFlow.getName()).getId());
        }
        return save(workOrderFlow);
    }


    public void delete(WorkOrderFlow workOrderFlow) {
        workOrderFlowRepository.delete(workOrderFlow);
    }

    public void delete(Long id) {
        workOrderFlowRepository.deleteById(id);
    }




    public WorkOrderFlow addWorkOrderFlow(WorkOrderFlow workOrderFlow) {
        return saveOrUpdate(workOrderFlow);
    }

    public WorkOrderFlow changeWorkOrderFlow(Long id, WorkOrderFlow workOrderFlow) {
        return saveOrUpdate(workOrderFlow);
    }

    /**
     * Get a list of work order numbers that can be group into work order flow
     * @param warehouseId
     * @return
     */
    public List<String> getWorkOrderFlowCandidate(Long warehouseId) {
        String statusList = "PENDING,INPROCESS,STAGED,WORK_IN_PROCESS";

        List<WorkOrder> workOrders = workOrderService.findAll(warehouseId,
                null, null, statusList,
        null);

        return workOrders.stream().map(workOrder -> workOrder.getNumber()).collect(Collectors.toList());
    }
}
