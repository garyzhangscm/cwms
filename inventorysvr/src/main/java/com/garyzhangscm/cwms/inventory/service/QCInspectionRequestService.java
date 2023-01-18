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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.inventory.clients.*;
import com.garyzhangscm.cwms.inventory.exception.QCException;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.QCInspectionRequestRepository;
import com.garyzhangscm.cwms.inventory.repository.QCRuleConfigurationRepository;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private FileService fileService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InboundServiceRestemplateClient inboundServiceRestemplateClient;
    @Autowired
    private WorkOrderServiceRestemplateClient workOrderServiceRestemplateClient;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;


    @Value("${inventory.qc.documentFolder}")
    private String qcDocumentFolder;


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
                                             QCInspectionResult qcInspectionResult,
                                             String type, String number){
        return findAll(warehouseId, inventoryId, inventoryIds,
                lpn,
                workOrderQCSampleNumber, qcInspectionResult, type, number, true);
    }


    public List<QCInspectionRequest> findAll(Long warehouseId,
                                             Long inventoryId,
                                             String inventoryIds,
                                             String lpn,
                                             String workOrderQCSampleNumber,
                                             QCInspectionResult qcInspectionResult,
                                             String type, String number,
                                             boolean loadDetails) {

        List<QCInspectionRequest> qcInspectionRequests =
                qcInspectionRequestRepository.findAll(
            (Root<QCInspectionRequest> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();

                predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                if (Strings.isNotBlank(number)) {

                    if (number.contains("%")) {
                        predicates.add(criteriaBuilder.like(root.get("number"), number));
                    }
                    else {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));
                    }


                }


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

                    if (lpn.contains("%")) {
                        predicates.add(criteriaBuilder.like(joinInventory.get("lpn"), lpn));
                    }
                    else {
                        predicates.add(criteriaBuilder.equal(joinInventory.get("lpn"), lpn));
                    }

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
                if (Strings.isNotBlank(type)) {

                    predicates.add(criteriaBuilder.equal(root.get("type"), QCInspectionRequestType.valueOf(type)));
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

    private QCInspectionRequest findByNumber(Long warehouseId, String number) {
        return qcInspectionRequestRepository.findByWarehouseIdAndNumber(warehouseId, number);
    }

    public QCInspectionRequest saveOrUpdate(QCInspectionRequest qcInspectionRequest) {
        if (Objects.isNull(qcInspectionRequest.getId()) &&
                Objects.nonNull(findByNumber(qcInspectionRequest.getWarehouseId(), qcInspectionRequest.getNumber()))) {
            qcInspectionRequest.setId(
                    findByNumber(qcInspectionRequest.getWarehouseId(), qcInspectionRequest.getNumber())
                    .getId()
            );
        }
        return save(qcInspectionRequest);
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

        if (Objects.nonNull(qcInspectionRequest.getWorkOrderId()) &&
                Objects.isNull(qcInspectionRequest.getWorkOrder())) {
            qcInspectionRequest.setWorkOrder(
                    workOrderServiceRestemplateClient.getWorkOrderById(
                            qcInspectionRequest.getWorkOrderId()
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
             save(setupInboundQCInspectionRequest(inventory, qcRuleConfiguration, QCInspectionRequestType.BY_INVENTORY));
        }
        else {

            save(setupInboundQCInspectionRequest(inventory, null, QCInspectionRequestType.BY_INVENTORY));
            logger.debug("We can't find any qc rule configuration for this inventory {} / {}",
                    inventory.getId(), inventory.getLpn());
        }
    }

    private QCInspectionRequest setupInboundQCInspectionRequest(Inventory inventory,
                                                                QCRuleConfiguration qcRuleConfiguration,
                                                                QCInspectionRequestType type) {
        QCInspectionRequest qcInspectionRequest = new QCInspectionRequest();
        qcInspectionRequest.setInventory(inventory);
        qcInspectionRequest.setWarehouseId(inventory.getWarehouseId());
        qcInspectionRequest.setQcInspectionResult(QCInspectionResult.PENDING);
        qcInspectionRequest.setType(
                Objects.isNull(type) ? QCInspectionRequestType.BY_INVENTORY : type);
        qcInspectionRequest.setNumber(getNextQCInspectionRequest(inventory.getWarehouseId()));
        if (Objects.nonNull(qcRuleConfiguration)) {
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
        }
        return qcInspectionRequest;


    }

    public void removeInboundQCInspectionRequest(Inventory inventory) {

        List<QCInspectionRequest> qcInspectionRequests = findAll(inventory.getWarehouseId(),
                inventory.getId(), null, null, null, null, null, null, false);

        qcInspectionRequests.forEach(
                qcInspectionRequest -> delete(qcInspectionRequest)
        );
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
                QCInspectionResult.PENDING, null, null
        );
    }

    public List<QCInspectionRequest> savePendingQCInspectionRequest(Long warehouseId,
                                                                    List<QCInspectionRequest> qcInspectionRequests,
                                                                    String rfCode) {
        List<QCInspectionRequest> newQCInspectionRequests = new ArrayList<>();
        qcInspectionRequests.forEach(
                qcInspectionRequest -> {
                    qcInspectionRequest.setQcTime(LocalDateTime.now().atZone(ZoneOffset.UTC));
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
                    QCInspectionRequest newQCInspectionRequest = save(qcInspectionRequest);

                    // if this is for certain work order, we will update the work order's qc quantity
                    if (Objects.nonNull(newQCInspectionRequest.getWorkOrderId())) {
                        // logger.debug("start to process qc quantity by inventory: {}", newQCInspectionRequest.getInventories());
                        Long qcQuantity = newQCInspectionRequest.getInventories().stream().mapToLong(Inventory::getQuantity).sum();
                        workOrderServiceRestemplateClient.addQCQuantity(
                                qcInspectionRequest.getWorkOrderId(), qcQuantity
                        );
                    }
                    newQCInspectionRequests.add(newQCInspectionRequest);
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

        QCConfiguration qcConfiguration  = qcConfigurationService.getQCConfiguration(warehouseId);
        if (Objects.isNull(qcConfiguration)) {
            throw ResourceNotFoundException.raiseException("QC configuration is not found " +
                    " for pass inventory status of warehouse " + warehouseId);
        }
        return qcConfiguration.getQcPassInventoryStatus();
    }
    private InventoryStatus getInventoryStatusForQCFail(Long warehouseId) {
        QCConfiguration qcConfiguration  = qcConfigurationService.getQCConfiguration(warehouseId);
        if (Objects.isNull(qcConfiguration)) {
            throw ResourceNotFoundException.raiseException("QC configuration is not found " +
                    " for fail inventory status of warehouse " + warehouseId);
        }
        return qcConfiguration.getQcFailInventoryStatus();

    }

    public List<QCInspectionRequest> findAllQCInspectionRequestResults(Long warehouseId,
                                                                       String lpn,
                                                                       String workOrderQCSampleNumber,
                                                                       String number) {
        List<QCInspectionRequest> passedQCInspectionRequest =
                findAll(warehouseId, null, null, lpn, workOrderQCSampleNumber, QCInspectionResult.PASS, null, number);
        List<QCInspectionRequest> failedQCInspectionRequest =
                findAll(warehouseId, null, null, lpn, workOrderQCSampleNumber, QCInspectionResult.FAIL, null, number);

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
        qcInspectionRequest.setType(QCInspectionRequestType.BY_WORK_ORDER_SAMPLING);
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

    public QCInspectionRequest addQCInspectionRequest(Long warehouseId, QCInspectionRequest qcInspectionRequest) {
        // verify the qc inspection
        verifyQCInspectionRequest(qcInspectionRequest);
        qcInspectionRequest.getQcInspectionRequestItems().forEach(
                qcInspectionRequestItem -> {
                    qcInspectionRequestItem.setQcInspectionRequest(qcInspectionRequest);
                    qcInspectionRequestItem.setQcInspectionResult(QCInspectionResult.PENDING);
                    qcInspectionRequestItem.getQcInspectionRequestItemOptions().forEach(
                            qcInspectionRequestItemOption -> {
                                qcInspectionRequestItemOption.setQcInspectionRequestItem(
                                        qcInspectionRequestItem
                                );
                                qcInspectionRequestItemOption.setQcInspectionResult(QCInspectionResult.PENDING);
                            }
                    );
                }
        );

        QCInspectionRequest newQCInspectionRequest = saveOrUpdate(qcInspectionRequest);

        return newQCInspectionRequest;
    }

    private void verifyQCInspectionRequest(QCInspectionRequest qcInspectionRequest) {
        switch (qcInspectionRequest.getType()) {
            case BY_ITEM:
                verifyItemQCInspectionRequest(qcInspectionRequest);
                break;

        }
    }

    /**
     * Make sure if the user pass in the work order, the work order matches with the item
     * @param qcInspectionRequest
     */
    private void verifyItemQCInspectionRequest(QCInspectionRequest qcInspectionRequest) {
        if (Objects.isNull(qcInspectionRequest.getItem())) {
            throw QCException.raiseException("item is needed for the qc inspection request");
        }
        if (Objects.nonNull(qcInspectionRequest.getWorkOrder()) &&
                !Objects.equals(qcInspectionRequest.getWorkOrder().getItem().getId(),
                        qcInspectionRequest.getItem().getId())) {
            throw QCException.raiseException("the item " + qcInspectionRequest.getItem().getName() +
                    " doesn't match with work order " + qcInspectionRequest.getWorkOrder().getNumber() + "'s " +
                    " item " + qcInspectionRequest.getWorkOrder().getItem().getId());

        }
        if (Objects.nonNull(qcInspectionRequest.getWorkOrderId())) {
            WorkOrder workOrder = workOrderServiceRestemplateClient.getWorkOrderById(
                    qcInspectionRequest.getWorkOrderId()
            );
            if (!Objects.equals(workOrder.getItem().getId(), qcInspectionRequest.getItem().getId())) {

                throw QCException.raiseException("the item " + qcInspectionRequest.getItem().getName() +
                        " doesn't match with work order " + workOrder.getNumber() + "'s " +
                        " item " + workOrder.getItem().getId());
            }
        }
    }

    /**
     * make sure we can use the LPN for the qc and return the inventories of this LPN
     * @param id
     * @param warehouseId
     * @param lpn
     * @return
     */
    public List<Inventory> validateLPNForInspectionByQCRequest(Long id, Long warehouseId, String lpn,
                                                               Boolean reQC) {
        QCInspectionRequest qcInspectionRequest = findById(id);

        List<Inventory> inventories = inventoryService.findByLpn(warehouseId, lpn);

        if (inventories.isEmpty()) {
            throw QCException.raiseException("Invalid LPN");
        }
        // make sure all the items on the LPN match with the qc request's item
        if (inventories.stream().anyMatch(inventory -> !Objects.equals(inventory.getItem(), qcInspectionRequest.getItem()))) {

            throw QCException.raiseException("LPN " + lpn + " contains item that doesn't match with " +
                    " the qc request " + qcInspectionRequest.getNumber() + "'s item ");
        }
        // if we are not in the ReQC mode, then we will not allow the user to re-enter the same LPN again
        if (!Boolean.TRUE.equals(reQC)) {
            List<QCInspectionRequest> qcInspectionRequests = qcInspectionRequestRepository.findByQCCompletedInventory(lpn);

            if (!qcInspectionRequests.isEmpty()) {

                // OK, we are not in Reqc mode but we found there's some qc completed on this lpn,
                // let's raise an error
                throw QCException.raiseException("LPN " + lpn + " has been QC by request: [" +
                                qcInspectionRequests.stream().map(QCInspectionRequest::getNumber).collect(Collectors.joining(","))  +
                        "], please specify that you are Re-QC the same LPN explicitly");
            }

        }

        return inventories;
    }

    public ReportHistory generateQCInspectionRequestReport(Long id, String locale)
            throws JsonProcessingException {

        return generateQCInspectionRequestReport(findById(id), locale);
    }
    public ReportHistory generateQCInspectionRequestReport(QCInspectionRequest qcInspectionRequest, String locale)
            throws JsonProcessingException {

        Long warehouseId = qcInspectionRequest.getWarehouseId();


        Report reportData = new Report();
        setupOrderPickReportParameters(
                reportData, qcInspectionRequest
        );
        setupOrderPickReportData(
                reportData, qcInspectionRequest
        );

        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        // logger.debug("####   Report   Data  ######");
        // logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.QC_INSPECTION_REQUEST_REPORT, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;

    }

    private void setupOrderPickReportParameters(
            Report report, QCInspectionRequest qcInspectionRequest) {

        // set the parameters to be the meta data of
        // the qcInspectionRequest

        report.addParameter("number", qcInspectionRequest.getNumber());

        report.addParameter("itemName",
                Objects.nonNull(qcInspectionRequest.getItem()) ?
                    qcInspectionRequest.getItem().getName() : "");
        report.addParameter("itemDescription",
                Objects.nonNull(qcInspectionRequest.getItem()) ?
                        qcInspectionRequest.getItem().getDescription() : "");
        report.addParameter("quantity",
                 qcInspectionRequest.getQcQuantity());

        report.addParameter("workOrder",
                Objects.nonNull(qcInspectionRequest.getWorkOrder()) ?
                        qcInspectionRequest.getWorkOrder().getNumber() : "");
        report.addParameter("receipt",
                Objects.nonNull(qcInspectionRequest.getReceipt()) ?
                        qcInspectionRequest.getReceipt().getNumber() : "");



    }

    private void setupOrderPickReportData(Report report, QCInspectionRequest qcInspectionRequest) {

        // set data to be all picks
        List<QCInspectionRequestItem> qcInspectionRequestItems = qcInspectionRequest.getQcInspectionRequestItems();
        List<QCInspectionRequestItemOption> qcInspectionRequestItemOptions =
                qcInspectionRequestItems.stream().map(
                        qcInspectionRequestItem -> qcInspectionRequestItem.getQcInspectionRequestItemOptions()
                ).flatMap(List::stream)
                        .collect(Collectors.toList());
        report.setData(qcInspectionRequestItemOptions);
    }

    public String uploadQCInspectionDocument(Long id, MultipartFile file) throws IOException {


        String filePath = getQCInspectionDocumentFolder(id).toString();
        logger.debug("Save file to {}{}",
                filePath, file.getOriginalFilename());

        File savedFile =
                fileService.saveFile(
                        file, filePath, file.getOriginalFilename());

        logger.debug("File saved, path: {}",
                savedFile.getAbsolutePath());
        return file.getOriginalFilename();
    }

    private Path getQCInspectionDocumentFolder(Long id) {
        return Paths.get(qcDocumentFolder, String.valueOf(id));
        // return qcImageFolder + "/" + id + "/";
    }

    private Path getQCInspectionDocumentPath(Long id, String fileName) {
        return Paths.get(qcDocumentFolder, String.valueOf(id), fileName);
        // return qcImageFolder + "/" + id + "/";
    }

    public File getQCInspectionDocument(Long warehouseId,
                                          Long id, String fileName) {



        String fileUrl = getQCInspectionDocumentPath(id, fileName).toString();

        logger.debug("Will return {} to the client",
                fileUrl);
        return new File(fileUrl);
    }

    public QCInspectionRequest changeQCInspectionDocumentUrls(Long id, String documentUrls) {
        QCInspectionRequest qcInspectionRequest = findById(id);

        qcInspectionRequest.setDocumentUrls(documentUrls);
        return saveOrUpdate(qcInspectionRequest);
    }

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        qcInspectionRequestRepository.processItemOverride(warehouseId,
                oldItemId, newItemId);
    }
}
