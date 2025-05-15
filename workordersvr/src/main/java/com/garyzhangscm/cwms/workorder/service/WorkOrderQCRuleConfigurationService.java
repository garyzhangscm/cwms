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
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderQCRuleConfigurationRepository;
import jakarta.persistence.criteria.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

                        try {
                            workOrderQCRuleConfigurationRule.setQcRule(
                                    inventoryServiceRestemplateClient.getQCRuleById(
                                            workOrderQCRuleConfigurationRule.getQcRuleId()
                                    )
                            );
                        }
                        catch (Exception ex) {}
                    }
                }
        );
    }


    public List<WorkOrderQCRuleConfiguration> findAll(Long warehouseId, Long workOrderId,
                                                      String workOrderNumber,
                                                      Long productionLineId,
                                                      Long btoOutboundOrderId,
                                                      Long btoCustomerId,
                                                      Long itemFamilyId,
                                                      Long itemId,
                                                      Long fromInventoryStatusId,
                                                      Long companyId) {

        return findAll(warehouseId, workOrderId, workOrderNumber, productionLineId,
                btoOutboundOrderId, btoCustomerId,
                itemFamilyId, itemId, fromInventoryStatusId, companyId,
                true);
    }

    public List<WorkOrderQCRuleConfiguration> findAll(Long warehouseId, Long workOrderId,
                                                      String workOrderNumber,
                                                      Long productionLineId,
                                                      Long btoOutboundOrderId,
                                                      Long btoCustomerId,
                                                      Long itemFamilyId,
                                                      Long itemId,
                                                      Long fromInventoryStatusId,
                                                      Long companyId,
                                           boolean loadDetail) {
        List<WorkOrderQCRuleConfiguration> workOrderQCRuleConfigurations =
                workOrderQCRuleConfigurationRepository.findAll(
                (Root<WorkOrderQCRuleConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(warehouseId)) {

                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }

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

                    if (Objects.nonNull(btoOutboundOrderId)) {

                        predicates.add(criteriaBuilder.equal(root.get("btoOutboundOrderId"), btoOutboundOrderId));
                    }

                    if (Objects.nonNull(btoCustomerId)) {

                        predicates.add(criteriaBuilder.equal(root.get("btoCustomerId"), btoCustomerId));
                    }

                    if (Objects.nonNull(itemFamilyId)) {

                        predicates.add(criteriaBuilder.equal(root.get("itemFamilyId"), itemFamilyId));
                    }
                    if (Objects.nonNull(itemId)) {

                        predicates.add(criteriaBuilder.equal(root.get("itemId"), itemId));
                    }
                    if (Objects.nonNull(fromInventoryStatusId)) {

                        predicates.add(criteriaBuilder.equal(root.get("fromInventoryStatusId"), fromInventoryStatusId));
                    }
                    if (Objects.nonNull(companyId)) {

                        predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
        if (workOrderQCRuleConfigurations.size() > 0 && Boolean.TRUE.equals(loadDetail)) {
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
                findAll(workOrderQCSample.getWarehouseId(), null, null, null,
                        null, null, null, null, null, null);
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

    public WorkOrderQCRuleConfiguration getBestMatchedWorkOrderQCRuleConfiguration(
            Long btoOutboundOrderId, Long btoCustomerId, Long itemFamilyId, Long itemId, Long warehouseId, Long companyId) {

        logger.debug("start to get best matched work order qc configuration with " +
                        "\n bto outbound order id: {}, bto customer id: {}" +
                " item family id: {}, item id: {}, warehouse id: {}, company id: {}",
                btoOutboundOrderId, btoCustomerId, itemFamilyId,  itemId, warehouseId, companyId);
        return getBestMatchedWorkOrderQCRuleConfiguration(
                btoOutboundOrderId, btoCustomerId, itemFamilyId, itemId,  null, warehouseId, companyId
        );
    }

    public WorkOrderQCRuleConfiguration getBestMatchedWorkOrderQCRuleConfiguration(
            Long btoOutboundOrderId, Long btoCustomerId,
            Long itemFamilyId, Long itemId,
            Long fromInventoryStatusId,
            Long warehouseId, Long companyId) {

        logger.debug("start to get best matched work order qc configuration with " +
                        "\n bto outbound order id: {}, bto customer id: {}" +
                        " item family id: {}, item id: {}, " +
                " from inventory status id: {}, warehouse id: {}, company id: {}",
                btoOutboundOrderId, btoCustomerId,
                itemFamilyId, itemId, fromInventoryStatusId, warehouseId, companyId);
        List<WorkOrderQCRuleConfiguration> allWorkOrderQCRuleConfiguration =
                findAll(null, null, null, null,
                        null, null, null, null,
                        null, companyId, false);

        if (allWorkOrderQCRuleConfiguration.size() == 0) {
            logger.debug("Can't find any work order QC configuration. Suppose we don't need QC");
            return null;
        }
        // we will get the best matched qc configuration based on the priority
        // 1. supplier + item
        // 2. supplier
        // 3. item
        // 4. warehouse id
        // 5. company id
        List<WorkOrderQCRuleConfiguration> matchedWorkOrderQCRuleConfiguration =
                allWorkOrderQCRuleConfiguration.stream().filter(
                        workOrderQCRuleConfiguration -> isMatch(
                                workOrderQCRuleConfiguration,
                                btoOutboundOrderId, btoCustomerId,
                                itemFamilyId, itemId, fromInventoryStatusId,
                                warehouseId, companyId
                        )
                ).collect(Collectors.toList());

        if (matchedWorkOrderQCRuleConfiguration.size() == 0) {
            logger.debug("Can't find any work order qc configuration matched with " +
                            "btoOutboundOrderId: {}, btoCustomerId: {}, itemFamilyId: {},itemId: {}, fromInventoryStatusId: {}, " +
                            "warehouseId: {}, companyId: {}",
                    btoOutboundOrderId, btoCustomerId,
                    itemFamilyId,  itemId, fromInventoryStatusId,
                    warehouseId, companyId);
            return null;
        }
        WorkOrderQCRuleConfiguration bestWorkOrderQCRuleConfiguration = matchedWorkOrderQCRuleConfiguration.get(0);
        for (WorkOrderQCRuleConfiguration workOrderQCRuleConfiguration : matchedWorkOrderQCRuleConfiguration) {
            logger.debug("workOrderQCRuleConfiguration {}'s priority: {}",
                    workOrderQCRuleConfiguration.getId(),
                    getPriority(workOrderQCRuleConfiguration));
            logger.debug("current bestWorkOrderQCRuleConfiguration {}'s priority: {}",
                    bestWorkOrderQCRuleConfiguration.getId(),
                    getPriority(bestWorkOrderQCRuleConfiguration));
            logger.debug("comparePriority(workOrderQCRuleConfiguration, bestWorkOrderQCRuleConfiguration): {}",
                    comparePriority(workOrderQCRuleConfiguration, bestWorkOrderQCRuleConfiguration) );
            if (comparePriority(workOrderQCRuleConfiguration, bestWorkOrderQCRuleConfiguration) > 0) {
                bestWorkOrderQCRuleConfiguration = workOrderQCRuleConfiguration;
            }
        }
        logger.debug("bestWorkOrderQCRuleConfiguration: {} for btoOutboundOrderId: {}, btoCustomerId: {}， item {}, warehouse {}",
                bestWorkOrderQCRuleConfiguration.getId(),
                btoOutboundOrderId,
                btoCustomerId,
                itemId,
                warehouseId);
        return bestWorkOrderQCRuleConfiguration;
    }


    private boolean isMatch(WorkOrderQCRuleConfiguration workOrderQCRuleConfiguration,
                            Long btoOutboundOrderId,
                            Long btoCustomerId,
                            Long itemFamilyId,
                            Long itemId,
                            Long fromInventoryStatusId,
                            Long warehouseId,
                            Long companyId) {
        if (Objects.nonNull(workOrderQCRuleConfiguration.getCompanyId()) &&
                !workOrderQCRuleConfiguration.getCompanyId().equals(companyId)) {
            logger.debug("company id in the qc configuration is {}, company id being compared is {}, mis match",
                    workOrderQCRuleConfiguration.getCompanyId(),
                    companyId);
            return false;
        }


        if (Objects.nonNull(workOrderQCRuleConfiguration.getWarehouseId()) &&
                !workOrderQCRuleConfiguration.getWarehouseId().equals(warehouseId)) {
            logger.debug("warehouse id in the qc configuration is {}, warehouse id being compared is {}, mis match",
                    workOrderQCRuleConfiguration.getWarehouseId(),
                    warehouseId);
            return false;
        }

        if (Objects.nonNull(workOrderQCRuleConfiguration.getItemFamilyId()) &&
                !workOrderQCRuleConfiguration.getItemFamilyId().equals(itemFamilyId)) {
            logger.debug("item family id in the qc configuration is {}, item family id being compared is {}, mis match",
                    workOrderQCRuleConfiguration.getItemFamilyId(),
                    itemFamilyId);
            return false;
        }
        if (Objects.nonNull(workOrderQCRuleConfiguration.getItemId()) &&
                !workOrderQCRuleConfiguration.getItemId().equals(itemId)) {
            logger.debug("item id in the qc configuration is {}, item id being compared is {}, mis match",
                    workOrderQCRuleConfiguration.getItemId(),
                    itemId);
            return false;
        }

        if (Objects.nonNull(workOrderQCRuleConfiguration.getOutboundOrderId()) &&
                !workOrderQCRuleConfiguration.getOutboundOrderId().equals(btoOutboundOrderId)
        ) {
            logger.debug("outbound order id in the qc configuration is {}, bto order id being compared is {}, mis match",
                    workOrderQCRuleConfiguration.getOutboundOrderId(),
                    btoOutboundOrderId);
            return false;
        }

        if (Objects.nonNull(workOrderQCRuleConfiguration.getCustomerId()) &&
                !workOrderQCRuleConfiguration.getCustomerId().equals(btoCustomerId)
        ) {
            logger.debug("outbound customer id in the qc configuration is {}, bto customer id being compared is {}, mis match",
                    workOrderQCRuleConfiguration.getCustomerId(),
                    btoCustomerId);
            return false;
        }
        // from inventory status is optional
        if (Objects.nonNull(fromInventoryStatusId) &&
                Objects.nonNull(workOrderQCRuleConfiguration.getFromInventoryStatusId()) &&
                !workOrderQCRuleConfiguration.getFromInventoryStatusId().equals(fromInventoryStatusId)
        ) {

            logger.debug("from inventory id in the qc configuration is {}, from inventory id being compared is {}, mis match",
                    workOrderQCRuleConfiguration.getFromInventoryStatusId(),
                    fromInventoryStatusId);
            return false;
        }

        logger.debug("qc configuration id {} matches with " +
                        "btoOutboundOrderId: {}, btoCustomerId: {},itemFamilyId: {},itemId: {}, fromInventoryStatusId: {}, " +
                        "warehouseId: {}, companyId: {}",
                workOrderQCRuleConfiguration.getId(),
                btoOutboundOrderId, btoCustomerId, itemFamilyId,  itemId, fromInventoryStatusId,
                warehouseId, companyId);
        return true;
    }


    /**
     * Get the priority of the configuration based off, lower the number, higher the priority
     * 1. order + item
     * 2. order + item family
     * 3. order
     * 4. customer + item
     * 5. customer + item family
     * 6. customer
     * 7. item
     * 8. item family
     * 9. warehouse id
     * 10. company id
     * @param workOrderQCRuleConfiguration
     * @return
     */
    private Integer getPriority(WorkOrderQCRuleConfiguration workOrderQCRuleConfiguration) {
        if (Objects.nonNull(workOrderQCRuleConfiguration.getOutboundOrderId())) {
            if (Objects.nonNull(workOrderQCRuleConfiguration.getItemId())) {
                return 1;
            }
            if (Objects.nonNull(workOrderQCRuleConfiguration.getItemFamilyId())) {
                return 2;
            }
            return 3;
        }
        if (Objects.nonNull(workOrderQCRuleConfiguration.getCustomerId())) {
            if (Objects.nonNull(workOrderQCRuleConfiguration.getItemId())) {
                return 4;
            }
            if (Objects.nonNull(workOrderQCRuleConfiguration.getItemFamilyId())) {
                return 5;
            }
            return 6;
        }
        if (Objects.nonNull(workOrderQCRuleConfiguration.getItemId())) {
            return 7;

        }
        if (Objects.nonNull(workOrderQCRuleConfiguration.getItemFamilyId())) {
            return 8;

        }
        if (Objects.nonNull(workOrderQCRuleConfiguration.getWarehouseId())) {
            return 9;
        }
        return 10;
    }

    /**
     * Return 1 if the first one has high priority(lower number). Return 0 if both have the same priority
     * return -1 if the last one has high priority(lower number)
     * Priority number is based off
     * 1. order + item
     * 2. order + item family
     * 3. order
     * 4. customer + item
     * 5. customer + item family
     * 6. customer
     * 7. item
     * 8. item family
     * 9. warehouse id
     * 10. company id
     * low number means high priority
     * @param firstWorkOrderQCRuleConfiguration
     * @param secondWorkOrderQCRuleConfiguration
     * @return
     */
    private int comparePriority(WorkOrderQCRuleConfiguration firstWorkOrderQCRuleConfiguration,
                                WorkOrderQCRuleConfiguration secondWorkOrderQCRuleConfiguration) {
        return getPriority(secondWorkOrderQCRuleConfiguration).compareTo(
                getPriority(firstWorkOrderQCRuleConfiguration)
        );

    }

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        workOrderQCRuleConfigurationRepository.processItemOverride(
                warehouseId, oldItemId, newItemId
        );
    }
}
