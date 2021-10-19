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
import com.garyzhangscm.cwms.workorder.repository.WorkOrderQCRuleConfigurationRepository;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderQCSampleRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class WorkOrderQCRuleConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderQCRuleConfigurationService.class);

    @Autowired
    private WorkOrderQCRuleConfigurationRepository workOrderQCRuleConfigurationRepository;
    @Autowired
    private ProductionLineAssignmentService productionLineAssignmentService;
    @Autowired
    private WorkOrderQCSampleService workOrderQCSampleService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public WorkOrderQCRuleConfiguration findById(Long id) {
        return findById(id, true);
    }

    public WorkOrderQCRuleConfiguration findById(Long id, boolean loadDetail) {
        WorkOrderQCRuleConfiguration workOrderQCRuleConfiguration =
                workOrderQCRuleConfigurationRepository.findById(id)
                    .orElseThrow(() -> ResourceNotFoundException.raiseException("work order qc sample not found by id: " + id));
        if (loadDetail) {
            loadAttribute(workOrderQCRuleConfiguration);
        }
        return workOrderQCRuleConfiguration;
    }


    private void loadAttribute(List<WorkOrderQCRuleConfiguration> workOrderQCRuleConfigurations) {

        workOrderQCRuleConfigurations.forEach(
                workOrderQCRuleConfiguration -> loadAttribute(workOrderQCRuleConfiguration)
        );
    }

    private void loadAttribute(WorkOrderQCRuleConfiguration workOrderQCRuleConfiguration) {
        workOrderQCRuleConfiguration.getWorkOrderQCRuleConfigurationRules().forEach(
                workOrderQCRuleConfigurationRule -> {

                    if (Objects.nonNull(workOrderQCRuleConfigurationRule.getQcRuleId()) &&
                            Objects.isNull(workOrderQCRuleConfigurationRule.getQcRule())) {

                        workOrderQCRuleConfigurationRule.setQcRule(
                                inventoryServiceRestemplateClient.getQCRuleById(
                                        workOrderQCRuleConfigurationRule.getQcRuleId()
                                )
                        );
                    }
                }
        );
    }


    public List<WorkOrderQCRuleConfiguration> findAll(Long warehouseId, Long workOrderId,
                                                      String workOrderNumber,
                                                      Long productionLineId ) {

        return findAll(warehouseId, workOrderId, workOrderNumber, productionLineId, true);
    }

    public List<WorkOrderQCRuleConfiguration> findAll(Long warehouseId, Long workOrderId,
                                                      String workOrderNumber,
                                                      Long productionLineId,
                                           boolean loadDetail) {
        List<WorkOrderQCRuleConfiguration> workOrderQCRuleConfigurations =
                workOrderQCRuleConfigurationRepository.findAll(
                (Root<WorkOrderQCRuleConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(workOrderId)) {

                        Join<WorkOrderQCRuleConfiguration, WorkOrder> joinWorkOrder
                                = root.join("workOrder", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinWorkOrder.get("id"), workOrderId));
                    }
                    if (Strings.isNotBlank(workOrderNumber)) {

                        Join<WorkOrderQCRuleConfiguration, WorkOrder> joinWorkOrder
                                = root.join("workOrder", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinWorkOrder.get("number"), workOrderNumber));
                    }

                    if (Objects.nonNull(productionLineId)) {

                        Join<WorkOrderQCRuleConfiguration, ProductionLine> joinProductionLine
                                = root.join("productionLine", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinProductionLine.get("id"), productionLineId));
                    }




                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
        if (workOrderQCRuleConfigurations.size() > 0 && loadDetail) {
            loadAttribute(workOrderQCRuleConfigurations);
        }
        return  workOrderQCRuleConfigurations;

    }

    
    public WorkOrderQCRuleConfiguration save(WorkOrderQCRuleConfiguration workOrderQCRuleConfiguration) {
        return workOrderQCRuleConfigurationRepository.save(workOrderQCRuleConfiguration);
    }



    public void delete(WorkOrderQCRuleConfiguration workOrderQCRuleConfiguration) {
        workOrderQCRuleConfigurationRepository.delete(workOrderQCRuleConfiguration);
    }

    public void delete(Long id) {
        workOrderQCRuleConfigurationRepository.deleteById(id);
    }


    public WorkOrderQCRuleConfiguration addQCRuleConfiguration(WorkOrderQCRuleConfiguration qcRuleConfiguration) {
        qcRuleConfiguration.getWorkOrderQCRuleConfigurationRules().forEach(
                workOrderQCRuleConfigurationRule -> workOrderQCRuleConfigurationRule.setWorkOrderQCRuleConfiguration(
                        qcRuleConfiguration
                )
        );
        return save(qcRuleConfiguration);
    }

    public WorkOrderQCRuleConfiguration changeQCRuleConfiguration(Long id, WorkOrderQCRuleConfiguration qcRuleConfiguration) {
        qcRuleConfiguration.getWorkOrderQCRuleConfigurationRules().forEach(
                workOrderQCRuleConfigurationRule -> workOrderQCRuleConfigurationRule.setWorkOrderQCRuleConfiguration(
                        qcRuleConfiguration
                )
        );
        qcRuleConfiguration.setId(id);
        return save(qcRuleConfiguration);
    }

    /**
     * Find all configuration that matches with the qc sample
     * 1. any configuration setup for the warehouse
     * 2. any configuration setup for the production line of the sample, work order is empty
     * 3. any configuration setup for the work order of the sample, production line is empty
     * 4. any configuration setup for the production line and work order of the sample
     * @param qcSampleId
     * @return
     */
    public List<WorkOrderQCRuleConfiguration> findMatchedConfigurationForQCSample(Long qcSampleId) {
        WorkOrderQCSample workOrderQCSample = workOrderQCSampleService.findById(qcSampleId);
        logger.debug("start to find matched work order qc configuration for sample: {} / {}",
                workOrderQCSample.getId(), workOrderQCSample.getNumber());
        List<WorkOrderQCRuleConfiguration> allConfiguraiton =
                findAll(workOrderQCSample.getWarehouseId(), null, null, null);
        logger.debug("We have {} work order qc configuration defined ",
                allConfiguraiton.size());
        return allConfiguraiton.stream().filter(
                        workOrderQCRuleConfiguration -> isMatch(workOrderQCRuleConfiguration, workOrderQCSample)
                ).collect(Collectors.toList());
    }

    private boolean isMatch(WorkOrderQCRuleConfiguration workOrderQCRuleConfiguration,
                            WorkOrderQCSample workOrderQCSample) {
        logger.debug("Start to compare qc rule configuration {} / {} / {} / {}",
                workOrderQCRuleConfiguration.getId(),
                workOrderQCRuleConfiguration.getWarehouseId(),
                Objects.isNull(workOrderQCRuleConfiguration.getWorkOrder()) ?
                        "N/A" : workOrderQCRuleConfiguration.getWorkOrder().getNumber(),
                Objects.isNull(workOrderQCRuleConfiguration.getProductionLine()) ?
                        "N/A" : workOrderQCRuleConfiguration.getProductionLine().getName());

        logger.debug("> with qc sample {} / {} / {} / {}",
                workOrderQCSample.getId(),
                workOrderQCSample.getWarehouseId(),
                Objects.isNull(workOrderQCSample.getProductionLineAssignment().getWorkOrder()) ?
                        "N/A" : workOrderQCSample.getProductionLineAssignment().getWorkOrder().getNumber(),
                Objects.isNull(workOrderQCSample.getProductionLineAssignment().getProductionLine()) ?
                        "N/A" : workOrderQCSample.getProductionLineAssignment().getProductionLine().getName());

        boolean isMatch = false;
        if (Objects.isNull(workOrderQCRuleConfiguration.getWorkOrder()) &&
                Objects.isNull(workOrderQCRuleConfiguration.getProductionLine())) {
            isMatch = workOrderQCRuleConfiguration.getWarehouseId().equals(
                    workOrderQCSample.getWarehouseId()
            );
        }
        else if (Objects.isNull(workOrderQCRuleConfiguration.getWorkOrder()) &&
                Objects.nonNull(workOrderQCRuleConfiguration.getProductionLine())) {
            isMatch =  workOrderQCRuleConfiguration.getWarehouseId().equals(
                    workOrderQCSample.getWarehouseId()) &&
                    workOrderQCRuleConfiguration.getProductionLine().getId().equals(
                            workOrderQCSample.getProductionLineAssignment().getProductionLine().getId());

        }
        else if (Objects.nonNull(workOrderQCRuleConfiguration.getWorkOrder()) &&
                Objects.isNull(workOrderQCRuleConfiguration.getProductionLine())) {
            isMatch =  workOrderQCRuleConfiguration.getWarehouseId().equals(
                    workOrderQCSample.getWarehouseId()) &&
                    workOrderQCRuleConfiguration.getWorkOrder().getId().equals(
                            workOrderQCSample.getProductionLineAssignment().getWorkOrder().getId());

        }
        else {

            isMatch =  workOrderQCRuleConfiguration.getWarehouseId().equals(
                        workOrderQCSample.getWarehouseId()) &&
                    workOrderQCRuleConfiguration.getWorkOrder().getId().equals(
                            workOrderQCSample.getProductionLineAssignment().getWorkOrder().getId()) &&
                    workOrderQCRuleConfiguration.getProductionLine().getId().equals(
                            workOrderQCSample.getProductionLineAssignment().getProductionLine().getId());
        }
        logger.debug(">> match? {}", isMatch);
        return isMatch;
    }
}
