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

package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.InboundServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WorkOrderServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.QCInspectionRequestRepository;
import com.garyzhangscm.cwms.inventory.repository.QCRuleConfigurationRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QCInspectionRequestService {
    private static final Logger logger = LoggerFactory.getLogger(QCInspectionRequestService.class);

    @Autowired
    private QCInspectionRequestRepository qcInspectionRequestRepository;
    @Autowired
    private QCRuleService qcRuleService;
    @Autowired
    private QCRuleConfigurationService qcRuleConfigurationService;
    @Autowired
    private QCConfigurationService qcConfigurationService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService userService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InboundServiceRestemplateClient inboundServiceRestemplateClient;
    @Autowired
    private WorkOrderServiceRestemplateClient workOrderServiceRestemplateClient;


    public QCInspectionRequest findById(Long id, boolean loadDetails) {
        QCInspectionRequest qcInspectionRequest =
                qcInspectionRequestRepository.findById(id)
                    .orElseThrow(() -> ResourceNotFoundException.raiseException("QC inspection request not found by id: " + id));
        if (loadDetails) {
            loadAttributes(qcInspectionRequest);
        }
        return  qcInspectionRequest;

    }
    public QCInspectionRequest findById(Long id ) {
        return findById(id, true);

    }

    public List<QCInspectionRequest> findAll(Long warehouseId,
                                             Long inventoryId,
                                             String inventoryIds,
                                             String lpn,
                                             String workOrderQCSampleNumber,
                                             QCInspectionResult qcInspectionResult){
        return findAll(warehouseId, inventoryId, inventoryIds,
                lpn, workOrderQCSampleNumber, qcInspectionResult, true);
    }
    public List<QCInspectionRequest> findAll(Long warehouseId,
                                             Long inventoryId,
                                             String inventoryIds,
                                             String lpn,
                                             String workOrderQCSampleNumber,
                                             QCInspectionResult qcInspectionResult,
                                             boolean loadDetails) {

        List<QCInspectionRequest> qcInspectionRequests =
                qcInspectionRequestRepository.findAll(
            (Root<QCInspectionRequest> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();

                predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                if (Objects.nonNull(inventoryId)) {

                    Join<QCInspectionRequest, Inventory> joinInventory = root.join("inventory", JoinType.INNER);
                    predicates.add(criteriaBuilder.equal(joinInventory.get("id"), inventoryId));
                }

                if (Strings.isNotBlank(inventoryIds)) {

                    Join<QCInspectionRequest, Inventory> joinInventory = root.join("inventory", JoinType.INNER);

                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(joinInventory.get("id"));
                    for(String id : inventoryIds.split(",")) {
                        in.value(Long.parseLong(id));
                    }
                    predicates.add(criteriaBuilder.and(in));
                }
                if (Objects.nonNull(qcInspectionResult)) {

                    predicates.add(criteriaBuilder.equal(root.get("qcInspectionResult"), qcInspectionResult));
                }

                if (Strings.isNotBlank(lpn)) {

                    Join<QCInspectionRequest, Inventory> joinInventory = root.join("inventory", JoinType.INNER);

                    predicates.add(criteriaBuilder.equal(joinInventory.get("lpn"), lpn));
                }

                if (Strings.isNotBlank(workOrderQCSampleNumber)) {
                    WorkOrderQCSample workOrderQCSample = workOrderServiceRestemplateClient.getWorkOrderQCSampleByNumber(
                            warehouseId, workOrderQCSampleNumber
                    );
                    if (Objects.nonNull(workOrderQCSample)) {

                        predicates.add(criteriaBuilder.equal(root.get("workOrderQCSampleId"), workOrderQCSample.getId()));
                    }
                    else  {

                        // we can't find the work order sample by the number, let's return nothing
                        predicates.add(criteriaBuilder.equal(root.get("workOrderQCSampleId"), -1l));
                    }
                }
                Predicate[] p = new Predicate[predicates.size()];
                return criteriaBuilder.and(predicates.toArray(p));
            }
        );

        if (qcInspectionRequests.size() > 0 && loadDetails) {
            loadAttributes(qcInspectionRequests);
        }

        return qcInspectionRequests;



    }

    private void loadAttributes(List<QCInspectionRequest> qcInspectionRequests) {
        qcInspectionRequests.forEach(
                qcInspectionRequest -> loadAttributes(qcInspectionRequest)
        );

    }
    private void loadAttributes(QCInspectionRequest qcInspectionRequest) {

        if (Objects.nonNull(qcInspectionRequest.getWorkOrderQCSampleId()) &&
                Objects.isNull(qcInspectionRequest.getWorkOrderQCSample())) {
            qcInspectionRequest.setWorkOrderQCSample(
                    workOrderServiceRestemplateClient.getWorkOrderQCSampleById(
                            qcInspectionRequest.getWorkOrderQCSampleId()
                    )
            );
        }
    }


    public QCInspectionRequest save(QCInspectionRequest qcInspectionRequest) {
        return qcInspectionRequestRepository.save(qcInspectionRequest);
    }


    public void delete(QCInspectionRequest qcInspectionRequest) {
        qcInspectionRequestRepository.delete(qcInspectionRequest);
    }
    public void delete(Long id) {
        qcInspectionRequestRepository.deleteById(id);
    }


    public void generateInboundQCInspectionRequest(Inventory inventory) {
        // setup all the necessary fields in case we will need to get
        // the matched qc rule configuration from the inventory attribute
        inventory.setItem(
                itemService.findById(
                        inventory.getItem().getId()
                )
        );
        if (inventory.getInboundQCRequired() == false) {
            logger.debug("inventory {} / {} doens't need QC",
                    inventory.getId(), inventory.getLpn());
            return;
        }
        // get the matched qc rules for this inventory
        if (Objects.isNull(inventory.getReceiptId())) {
            logger.debug("can't find receipt for inventory {} / {}, we won't be able to generate the inbound QC request",
                    inventory.getId(), inventory.getLpn());
            return;
        }
        Receipt receipt = inboundServiceRestemplateClient.getReceiptById(inventory.getReceiptId());
        QCRuleConfiguration qcRuleConfiguration =
                qcRuleConfigurationService.findBestMatchedQCRuleConfiguration(
                        receipt.getSupplier(), inventory
                );
        if (Objects.nonNull(qcRuleConfiguration)) {
            logger.debug("We found a qc rule configuration for this inventory {} / {}",
                    inventory.getId(), inventory.getLpn());
            // assign each rule into this inventory
             save(setupInboundQCInspectionRequest(inventory, qcRuleConfiguration));
        }
        else {

            logger.debug("We can't find any qc rule configuration for this inventory {} / {}",
                    inventory.getId(), inventory.getLpn());
        }
    }

    private QCInspectionRequest setupInboundQCInspectionRequest(Inventory inventory, QCRuleConfiguration qcRuleConfiguration) {
        QCInspectionRequest qcInspectionRequest = new QCInspectionRequest();
        qcInspectionRequest.setInventory(inventory);
        qcInspectionRequest.setWarehouseId(inventory.getWarehouseId());
        qcInspectionRequest.setQcInspectionResult(QCInspectionResult.PENDING);
        qcInspectionRequest.setNumber(getNextQCInspectionRequest(inventory.getWarehouseId()));
        qcRuleConfiguration.getQcRules().forEach(
                qcRule -> {
                    QCInspectionRequestItem qcInspectionRequestItem = new QCInspectionRequestItem();
                    qcInspectionRequestItem.setQcInspectionRequest(qcInspectionRequest);
                    qcInspectionRequestItem.setQcInspectionResult(QCInspectionResult.PENDING);
                    qcInspectionRequestItem.setQcRule(qcRule);
                    qcRule.getQcRuleItems().forEach(
                            qcRuleItem -> {
                                QCInspectionRequestItemOption qcInspectionRequestItemOption = new QCInspectionRequestItemOption();
                                qcInspectionRequestItemOption.setQcRuleItem(qcRuleItem);
                                qcInspectionRequestItemOption.setQcInspectionRequestItem(qcInspectionRequestItem);
                                qcInspectionRequestItemOption.setQcInspectionResult(QCInspectionResult.PENDING);
                                qcInspectionRequestItem.addQcInspectionRequestItemOption(qcInspectionRequestItemOption);
                            }
                    );
                    qcInspectionRequest.addQcInspectionRequestItem(qcInspectionRequestItem);
                }
        );
        return qcInspectionRequest;


    }

    private String getNextQCInspectionRequest(Long warehouseId) {
        return commonServiceRestemplateClient.getNextQCInspectionRequest(warehouseId);
    }


    public List<QCInspectionRequest> findPendingQCInspectionRequests(
            Long warehouseId, Long inventoryId, String inventoryIds) {
        return findAll(
                warehouseId,
                inventoryId,
                inventoryIds,
                null,
                null,
                QCInspectionResult.PENDING
        );
    }

    public List<QCInspectionRequest> savePendingQCInspectionRequest(Long warehouseId,
                                                                    List<QCInspectionRequest> qcInspectionRequests,
                                                                    String rfCode) {
        List<QCInspectionRequest> newQCInspectionRequests = new ArrayList<>();
        qcInspectionRequests.forEach(
                qcInspectionRequest -> {
                    qcInspectionRequest.setQcTime(LocalDateTime.now());
                    qcInspectionRequest.setQcUsername(userService.getCurrentUserName());
                    qcInspectionRequest.setRfCode(rfCode);

                    // setup the connection for the parent / child relationship
                    // when we get the data from the client with json format,
                    // we will only have one direction relationship setup
                    // in order to have JPA persist the data all in once,
                    // we will have to setup bi-direction relationship
                    qcInspectionRequest.getQcInspectionRequestItems().forEach(
                            qcInspectionRequestItem -> {
                                qcInspectionRequestItem.setQcInspectionRequest(
                                        qcInspectionRequest
                                );
                                qcInspectionRequestItem.getQcInspectionRequestItemOptions().forEach(
                                        qcInspectionRequestItemOption ->
                                                qcInspectionRequestItemOption.setQcInspectionRequestItem(
                                                        qcInspectionRequestItem
                                                )
                                );
                            }
                    );
                    newQCInspectionRequests.add(
                            save(qcInspectionRequest)
                    );
                }
        );
        // change the result according
        changeInventoryStatus(newQCInspectionRequests);
        return newQCInspectionRequests;
    }

    private void changeInventoryStatus(List<QCInspectionRequest> qcInspectionRequests) {
        qcInspectionRequests.forEach(
                qcInspectionRequest -> {

                    changeInventoryStatus(qcInspectionRequest);
                }
        );
    }

    private void changeInventoryStatus(QCInspectionRequest qcInspectionRequest) {
        Inventory inventory = qcInspectionRequest.getInventory();
        if (Objects.isNull(inventory)) {
            // the result is not for inventory, it may be for work order
            return;
        }
        if (qcInspectionRequest.getQcInspectionResult().equals(QCInspectionResult.FAIL) &&
                Objects.nonNull(getInventoryStatusForQCFail(inventory.getWarehouseId()))) {
            inventory.setInventoryStatus(
                    getInventoryStatusForQCFail(inventory.getWarehouseId())
            );
            inventory.setInboundQCRequired(false);
            logger.debug("The qc result for inventory {} / {} is Fail, let's set the status to {}",
                    inventory.getId(), inventory.getLpn(),
                    inventory.getInventoryStatus().getName());
            inventoryService.saveOrUpdate(inventory);
        }
        else if (qcInspectionRequest.getQcInspectionResult().equals(QCInspectionResult.PASS) &&
                Objects.nonNull(getInventoryStatusForQCPass(inventory.getWarehouseId()))) {
            inventory.setInventoryStatus(
                    getInventoryStatusForQCPass(inventory.getWarehouseId())
            );
            inventory.setInboundQCRequired(false);
            logger.debug("The qc result for inventory {} / {} is Pass, let's set the status to {}",
                    inventory.getId(), inventory.getLpn(),
                    inventory.getInventoryStatus().getName());
            inventoryService.saveOrUpdate(inventory);
        }

    }

    private InventoryStatus getInventoryStatusForQCPass(Long warehouseId) {

        return qcConfigurationService.getQCConfiguration(warehouseId).getQcPassInventoryStatus();
    }
    private InventoryStatus getInventoryStatusForQCFail(Long warehouseId) {
        return qcConfigurationService.getQCConfiguration(warehouseId).getQcFailInventoryStatus();

    }

    public List<QCInspectionRequest> findAllQCInspectionRequestResults(Long warehouseId,
                                                                       String lpn,
                                                                       String workOrderQCSampleNumber) {
        List<QCInspectionRequest> passedQCInspectionRequest =
                findAll(warehouseId, null, null, lpn, workOrderQCSampleNumber, QCInspectionResult.PASS);
        List<QCInspectionRequest> failedQCInspectionRequest =
                findAll(warehouseId, null, null, lpn, workOrderQCSampleNumber, QCInspectionResult.FAIL);

        // return both passed qc inspection and failed qc inspection
        passedQCInspectionRequest.addAll(failedQCInspectionRequest);

        return passedQCInspectionRequest;

    }

    /**
     * Generate qc inspection request for work order
     * @param warehouseId
     * @param ruleIds
     * @return
     */
    public QCInspectionRequest generateWorkOrderQCInspectionRequest(Long warehouseId, Long workOrderQCSampleId,
                                                                   String ruleIds, Long qcQuantity) {
        QCInspectionRequest qcInspectionRequest = new QCInspectionRequest();
        qcInspectionRequest.setWorkOrderQCSampleId(workOrderQCSampleId);
        qcInspectionRequest.setWarehouseId(warehouseId);
        qcInspectionRequest.setQcQuantity(qcQuantity);
        qcInspectionRequest.setQcInspectionResult(QCInspectionResult.PENDING);
        qcInspectionRequest.setNumber(getNextQCInspectionRequest(warehouseId));


        qcRuleService.findAll(warehouseId, null, ruleIds).forEach(
                qcRule -> {
                    QCInspectionRequestItem qcInspectionRequestItem = new QCInspectionRequestItem();
                    qcInspectionRequestItem.setQcInspectionRequest(qcInspectionRequest);
                    qcInspectionRequestItem.setQcInspectionResult(QCInspectionResult.PENDING);
                    qcInspectionRequestItem.setQcRule(qcRule);
                    qcRule.getQcRuleItems().forEach(
                            qcRuleItem -> {
                                QCInspectionRequestItemOption qcInspectionRequestItemOption = new QCInspectionRequestItemOption();
                                qcInspectionRequestItemOption.setQcRuleItem(qcRuleItem);
                                qcInspectionRequestItemOption.setQcInspectionRequestItem(qcInspectionRequestItem);
                                qcInspectionRequestItemOption.setQcInspectionResult(QCInspectionResult.PENDING);
                                qcInspectionRequestItem.addQcInspectionRequestItemOption(qcInspectionRequestItemOption);
                            }
                    );
                    qcInspectionRequest.addQcInspectionRequestItem(qcInspectionRequestItem);
                }
        );
        return save(qcInspectionRequest);
    }
}
