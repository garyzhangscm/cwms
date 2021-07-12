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
import com.garyzhangscm.cwms.workorder.repository.WorkOrderKPIRepository;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.Transient;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class WorkOrderKPIService{
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderKPIService.class);

    @Autowired
    private WorkOrderKPIRepository workOrderKPIRepository;
    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private WorkOrderLineService workOrderLineService;
    @Autowired
    private BillOfMaterialService billOfMaterialService;
    @Autowired
    private ProductionLineService productionLineService;
    @Autowired
    private WorkOrderInstructionService workOrderInstructionService;

    @Autowired
    private OutboundServiceRestemplateClient outboundServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    public WorkOrderKPI findById(Long id, boolean loadDetails) {
        WorkOrderKPI workOrderKPI = workOrderKPIRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order not found by id: " + id));
        if (loadDetails) {
            loadAttribute(workOrderKPI);
        }
        return workOrderKPI;
    }

    public WorkOrderKPI findById(Long id) {
        return findById(id, true);
    }


    public List<WorkOrderKPI> findAll(Long warehouseId, WorkOrder workOrder,
                                      String username, String workingTeamName,
                                      boolean genericQuery, boolean loadDetails) {

        List<WorkOrderKPI> workOrderKPIs =  workOrderKPIRepository.findAll(
                (Root<WorkOrderKPI> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(warehouseId)) {

                        Join<WorkOrderKPI, WorkOrder> joinWorkOrder = root.join("workOrder", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinWorkOrder.get("warehouseId"), warehouseId));
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

        if (workOrderKPIs.size() > 0 && loadDetails) {
            loadAttribute(workOrderKPIs);
        }
        return workOrderKPIs;
    }

    public List<WorkOrderKPI> findAll(Long warehouseId, WorkOrder workOrder, String username,
                                      String workingTeamName, boolean genericQuery) {
        return findAll(warehouseId, workOrder, username, workingTeamName, genericQuery,true);
    }

    public List<WorkOrderKPI> findAll(Long warehouseId, String workOrderNumber,
                                      String username, String workingTeamName,
                                      boolean genericQuery) {
        WorkOrder workOrder = null;
        if (StringUtils.isNotBlank(workOrderNumber)) {
            workOrder = workOrderService.findByNumber(warehouseId, workOrderNumber);
        }
        return findAll(warehouseId, workOrder, username, workingTeamName, genericQuery);
    }




    public void loadAttribute(List<WorkOrderKPI> workOrderKPIs) {
        for (WorkOrderKPI workOrderKPI : workOrderKPIs) {
            loadAttribute(workOrderKPI);
        }
    }

    public void loadAttribute(WorkOrderKPI workOrderKPI) {

        workOrderService.loadAttribute(workOrderKPI.getWorkOrder());

    }

    public WorkOrderKPI findByWorkOrderAndUsername(WorkOrder workOrder, String username, boolean loadDetails) {
        Long warehouseId = Objects.isNull(workOrder) ? null : workOrder.getWarehouseId();
        return findAll(warehouseId, workOrder, username, null, loadDetails).stream().findFirst().orElse(null);
    }
    public List<WorkOrderKPI> findByWorkOrder(WorkOrder workOrder, boolean loadDetails) {
        Long warehouseId = Objects.isNull(workOrder) ? null : workOrder.getWarehouseId();
        return findAll(warehouseId, workOrder, null, null,  loadDetails);
    }
    public List<WorkOrderKPI> findByWorkOrder(WorkOrder workOrder) {
        return findByWorkOrder(workOrder, true);
    }


    public WorkOrderKPI findByWorkOrderAndUsername(WorkOrder workOrder, String username) {
        return findByWorkOrderAndUsername(workOrder, username, true);
    }

    public WorkOrderKPI findByWorkOrderAndWorkingTeamName(WorkOrder workOrder, String workingTeamName) {
        return findByWorkOrderAndWorkingTeamName(workOrder, workingTeamName, true);
    }

    public WorkOrderKPI findByWorkOrderAndWorkingTeamName(WorkOrder workOrder, String workingTeamName, boolean loadDetails) {
        Long warehouseId = Objects.isNull(workOrder) ? null : workOrder.getWarehouseId();
        return findAll(warehouseId, workOrder, null, workingTeamName, loadDetails).stream().findFirst().orElse(null);
    }



    public WorkOrderKPI save(WorkOrderKPI workOrderKPI) {
        WorkOrderKPI newWorkOrderKPI = workOrderKPIRepository.save(workOrderKPI);
        loadAttribute(newWorkOrderKPI);
        return newWorkOrderKPI;
    }

    public WorkOrderKPI saveOrUpdate(WorkOrderKPI workOrder) {
        if (workOrder.getId() == null && findByWorkOrderAndUsername(workOrder.getWorkOrder(), workOrder.getUsername(), false) != null) {
            workOrder.setId(
                    findByWorkOrderAndUsername(workOrder.getWorkOrder(), workOrder.getUsername(), false).getId());
        }
        return save(workOrder);
    }


    public void delete(WorkOrderKPI workOrderKPI) {
        workOrderKPIRepository.delete(workOrderKPI);
    }

    public void delete(Long id) {
        workOrderKPIRepository.deleteById(id);
    }


    public void processWorkOrderKPIWhenCompletingWorkOrder(WorkOrderKPITransaction workOrderKPITransaction) {
        switch (workOrderKPITransaction.getType()) {
            case ADD: OVERRIDE:
                saveOrUpdate(getWorkOrderKPIFromTransaction(workOrderKPITransaction));
                break;
            case REMOVED:
                delete(workOrderKPITransaction.getWorkOrderKPI());
                break;
            // by default, the KPI is unchagned
        }

    }
    private WorkOrderKPI getWorkOrderKPIFromTransaction(WorkOrderKPITransaction workOrderKPITransaction) {
        WorkOrderKPI workOrderKPI =  new WorkOrderKPI();
        if (Objects.nonNull(workOrderKPITransaction.getWorkOrderKPI())) {
            workOrderKPI = workOrderKPITransaction.getWorkOrderKPI();

        }

        workOrderKPI.setWorkingTeamName(workOrderKPITransaction.getWorkingTeamName());
        workOrderKPI.setUsername(workOrderKPITransaction.getUsername());
        workOrderKPI.setAmount(workOrderKPITransaction.getAmount());
        workOrderKPI.setKpiMeasurement(workOrderKPITransaction.getKpiMeasurement());
        return workOrderKPI;
    }
    /**
     * Process work order KPI when completing the work order. We will allow the user to change
     * the existing work order KPIs
     * When the user save KPI during producing finish goods, we will only allow the user to input
     * new KPI
     * @param workOrder
     * @param updatedWorkOrderKPIs
     */
    @Transactional
    public void processWorkOrderKPIWhenCompletingWorkOrder(WorkOrder workOrder, List<WorkOrderKPI> updatedWorkOrderKPIs) {
        List<WorkOrderKPI> existingWorkOrderKPIs = findByWorkOrder(workOrder);

        if (existingWorkOrderKPIs.size() == 0 && updatedWorkOrderKPIs.size() == 0) {
            // It looks like we don't capture the KPI for this work order
            return;
        }

        // Let's loop through both list and do the update
        // Change the list to map for each iterating
        Map<Long, WorkOrderKPI> updatedWorkOrderKPIMap = new HashMap<>();

        updatedWorkOrderKPIs.forEach(updatedWorkOrderKPI -> {
            updatedWorkOrderKPI.setWorkOrder(workOrder);
            if(Objects.nonNull(updatedWorkOrderKPI.getId())) {
                updatedWorkOrderKPIMap.put(updatedWorkOrderKPI.getId(), updatedWorkOrderKPI);
            }
            else if (Objects.nonNull(findByWorkOrderAndUsername(
                    updatedWorkOrderKPI.getWorkOrder(), updatedWorkOrderKPI.getUsername()))) {
                // the KPI record doesn't have an ID, let's see if the key(work order + username)
                // already exists. If so, we will only update the record since we only allow
                // one KPI information for one work order + username combination
                updatedWorkOrderKPIMap.put(findByWorkOrderAndUsername(
                        updatedWorkOrderKPI.getWorkOrder(), updatedWorkOrderKPI.getUsername()).getId()
                        , updatedWorkOrderKPI);
            }
            else {
                // This is a new user for this work order, let's just add it
                save(updatedWorkOrderKPI);
            }
        });


        Iterator<WorkOrderKPI> existingWorkOrderKPIIterator = existingWorkOrderKPIs.iterator();
        while(existingWorkOrderKPIIterator.hasNext()) {
            WorkOrderKPI existingWorkOrderKPI = existingWorkOrderKPIIterator.next();

            WorkOrderKPI updatedWorkOrderKPI = updatedWorkOrderKPIMap.getOrDefault(
                    existingWorkOrderKPI.getId(), null
            );
            if (Objects.isNull(updatedWorkOrderKPI)) {
                // OK, the existing KPI doesn't exists any more
                // Let's just remove it
                delete(existingWorkOrderKPI);
            }
            else if (existingWorkOrderKPI.getAmount() != updatedWorkOrderKPI.getAmount() ||
                    !existingWorkOrderKPI.getKpiMeasurement().equals(updatedWorkOrderKPI.getKpiMeasurement())) {
                // either measurement changed, or amount changed, let's change and save it
                existingWorkOrderKPI.setKpiMeasurement(updatedWorkOrderKPI.getKpiMeasurement());
                existingWorkOrderKPI.setAmount(updatedWorkOrderKPI.getAmount());
                save(existingWorkOrderKPI);
            }
        }
    }


    /**
     * Process work order KPI when producing finish goods. We will only save the KPI transaction and then add the KPI to the
     * existing KPI records
     *
     * @param workOrderKPI
     */
    @Transactional
    public void processWorkOrderKPIWhenProducingFinishGood( WorkOrderKPI workOrderKPI) {
        WorkOrderKPI existingWorkOrderKPI = null;
        if (Objects.nonNull(workOrderKPI.getUsername())) {
            existingWorkOrderKPI = findByWorkOrderAndUsername(
                    workOrderKPI.getWorkOrder(), workOrderKPI.getUsername());
        }
        else if (Objects.nonNull(workOrderKPI.getWorkingTeamName())) {
            existingWorkOrderKPI = findByWorkOrderAndWorkingTeamName(
                    workOrderKPI.getWorkOrder(), workOrderKPI.getWorkingTeamName());
        }

        if (Objects.isNull(existingWorkOrderKPI)) {

            // OK we don't have existing work order KPI for this
            // user or team, let's just save it

            save(workOrderKPI);
        }
        else if (!existingWorkOrderKPI.getKpiMeasurement().equals(workOrderKPI.getKpiMeasurement())){
            // OK we already have the existing work order KPI information for
            // this user or team, then we will
            // 1. if the original one have a different measurement, then we
            //    we will override the original one
            // 2. if the original one have the same measurement, we will sum up the amount
            existingWorkOrderKPI.setAmount(workOrderKPI.getAmount());
            existingWorkOrderKPI.setKpiMeasurement(workOrderKPI.getKpiMeasurement());
            save(existingWorkOrderKPI);
        }
        else {
            // if we are here, we know that we have existing work order kpi
            // belong to the same username or working team and
            // it has the same measure, we will sum up the amount
            existingWorkOrderKPI.setAmount(existingWorkOrderKPI.getAmount() + workOrderKPI.getAmount());
            save(existingWorkOrderKPI);

        }
    }


    /**
     * Process work order KPI when complete teh work order.
     *
     *
     * @param workOrderKPI
     */
    @Transactional
    public void processWorkOrderKPIWhenCompletingWorkOrder( WorkOrderKPI workOrderKPI) {
        WorkOrderKPI existingWorkOrderKPI = null;
        if (Objects.nonNull(workOrderKPI.getUsername())) {
            existingWorkOrderKPI = findByWorkOrderAndUsername(
                    workOrderKPI.getWorkOrder(), workOrderKPI.getUsername());
        }
        else if (Objects.nonNull(workOrderKPI.getWorkingTeamName())) {
            existingWorkOrderKPI = findByWorkOrderAndWorkingTeamName(
                    workOrderKPI.getWorkOrder(), workOrderKPI.getWorkingTeamName());
        }

        // OK we don't have existing work order KPI for this
        // user or team, let's just save it
        if (Objects.isNull(existingWorkOrderKPI)) {
            save(workOrderKPI);
        }
        else {
            // OK we already have the existing work order KPI information for
            // this user or team, then we will
            // 1. override if we are completing work order
            // 2. add the quantity if we are producing finish good
            existingWorkOrderKPI.setKpiMeasurement(workOrderKPI.getKpiMeasurement());
            existingWorkOrderKPI.setAmount(workOrderKPI.getAmount());
            save(existingWorkOrderKPI);
        }
    }


    @Transactional
    public WorkOrderKPI recordWorkOrderKPIForCheckedInUser(WorkOrder workOrder,
                                                           ProductionLine productionLine,
                                                           String username,
                                                           Integer workingTeamMemberCount,
                                                           Long quantity) {

        WorkOrderKPI workOrderKPI = new WorkOrderKPI();
        workOrderKPI.setWorkOrder(workOrder);
        workOrderKPI.setProductionLine(productionLine);
        workOrderKPI.setKpiMeasurement(KPIMeasurement.BY_QUANTITY);
        workOrderKPI.setAmount(quantity);
        workOrderKPI.setWorkingTeamMemberCount(workingTeamMemberCount);
        workOrderKPI.setUsername(username);
        workOrderKPI.setWorkingTeamName("");
        return  save(workOrderKPI);

    }
}
