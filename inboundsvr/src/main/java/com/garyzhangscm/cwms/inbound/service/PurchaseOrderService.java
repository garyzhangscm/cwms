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

package com.garyzhangscm.cwms.inbound.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.inbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.exception.ReceiptOperationException;
import com.garyzhangscm.cwms.inbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.repository.PurchaseOrderLineRepository;
import com.garyzhangscm.cwms.inbound.repository.PurchaseOrderRepository;
import com.garyzhangscm.cwms.inbound.repository.ReceiptRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

@Service
public class PurchaseOrderService {
    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderService.class);

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PurchaseOrderLineRepository purchaseOrderLineRepository;
    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    public PurchaseOrder findById(Long id, boolean loadDetails) {
        PurchaseOrder purchaseOrder =  purchaseOrderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("purchase order not found by id: " + id));
        if (loadDetails) {
            loadReceiptAttribute(purchaseOrder);
        }
        return purchaseOrder;
    }

    public PurchaseOrder findById(Long id) {
        return findById(id, true);
    }


    public List<PurchaseOrder> findAll(Long warehouseId, String number,
                                       String purchasOrderStatusList,
                                 Long supplierId, String supplierName) {
        return findAll(warehouseId, number, purchasOrderStatusList, supplierId, supplierName, true);
    }

    public List<PurchaseOrder> findAll(Long warehouseId, String number, String purchasOrderStatusList,
                                       Long supplierId, String supplierName,
                                 boolean loadDetails) {

        List<PurchaseOrder> purchaseOrders =  purchaseOrderRepository.findAll(
                (Root<PurchaseOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(number)) {

                        if (number.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("number"), number));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("number"), number));
                        }
                    }

                    if (Objects.nonNull(supplierId)) {
                        predicates.add(criteriaBuilder.equal(root.get("supplierId"), supplierId));

                    }
                    if (StringUtils.isNotBlank(supplierName)) {

                        Supplier supplier = commonServiceRestemplateClient.getSupplierByName(warehouseId, supplierName);
                        if (Objects.nonNull(supplier)) {
                            predicates.add(criteriaBuilder.equal(root.get("supplierId"), supplier.getId()));

                        }
                        else {

                            // we can't find the supplier by name,
                            predicates.add(criteriaBuilder.equal(root.get("supplierId"), -1));
                        }
                    }

                    if (StringUtils.isNotBlank(purchasOrderStatusList)) {
                        CriteriaBuilder.In<PurchaseOrderStatus> inPurchaseOrderStatus
                                = criteriaBuilder.in(root.get("purchaseOrderStatus"));
                        for(String purchaseOrderStatus : purchasOrderStatusList.split(",")) {
                            inPurchaseOrderStatus.value(PurchaseOrderStatus.valueOf(purchaseOrderStatus));
                        }
                        predicates.add(criteriaBuilder.and(inPurchaseOrderStatus));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
        if (purchaseOrders.size() > 0 && loadDetails) {
            loadReceiptAttribute(purchaseOrders);
        }
        return purchaseOrders;
    }

    public PurchaseOrder findByNumber(Long warehouseId, String number, boolean loadDetails) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByNumber(warehouseId, number);
        if (purchaseOrder != null && loadDetails) {
            loadReceiptAttribute(purchaseOrder);
        }
        return purchaseOrder;
    }

    public PurchaseOrder findByNumber(Long warehouseId, String number) {
        return findByNumber(warehouseId, number, true);
    }



    public void loadReceiptAttribute(List<PurchaseOrder> purchaseOrders) {
        for(PurchaseOrder purchaseOrder : purchaseOrders) {
            loadReceiptAttribute(purchaseOrder);
        }
    }

    public void loadReceiptAttribute(PurchaseOrder purchaseOrder) {
        // Load the details for client and supplier informaiton
        if (purchaseOrder.getClientId() != null && purchaseOrder.getClient() == null) {
            purchaseOrder.setClient(commonServiceRestemplateClient.getClientById(purchaseOrder.getClientId()));
        }
        if (purchaseOrder.getSupplierId() != null && purchaseOrder.getSupplier() == null) {
            purchaseOrder.setSupplier(commonServiceRestemplateClient.getSupplierById(purchaseOrder.getSupplierId()));
        }

        // load the details for receipt lines
        if (purchaseOrder.getPurchaseOrderLines().size() > 0) {
            loadPurchaseOrderLineAttribute(purchaseOrder.getPurchaseOrderLines());
        }

    }

    private void loadPurchaseOrderLineAttribute(List<PurchaseOrderLine> purchaseOrderLines) {

        for(PurchaseOrderLine purchaseOrderLine : purchaseOrderLines) {
            loadPurchaseOrderLineAttribute(purchaseOrderLine);
        }
    }

    private void loadPurchaseOrderLineAttribute(PurchaseOrderLine purchaseOrderLine) {

        // Load Item information
        if (purchaseOrderLine.getItemId() != null && purchaseOrderLine.getItem() == null) {
            purchaseOrderLine.setItem(inventoryServiceRestemplateClient.getItemById(purchaseOrderLine.getItemId()));

        }
    }


    @Transactional
    public PurchaseOrder save(PurchaseOrder purchaseOrder, boolean loadAttribute) {
        PurchaseOrder newPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);
        if (loadAttribute) {
            loadReceiptAttribute(newPurchaseOrder);
        }
        return newPurchaseOrder;
    }

    @Transactional
    public PurchaseOrder save(PurchaseOrder purchaseOrder) {
        return save(purchaseOrder, true);
    }

    @Transactional
    public PurchaseOrder saveOrUpdate(PurchaseOrder purchaseOrder) {
        return saveOrUpdate(purchaseOrder, true);
    }

    @Transactional
    public PurchaseOrder saveOrUpdate(PurchaseOrder purchaseOrder, boolean loadAttribute) {
        if (purchaseOrder.getId() == null && findByNumber(purchaseOrder.getWarehouseId(),purchaseOrder.getNumber(), false) != null) {
            purchaseOrder.setId(findByNumber(purchaseOrder.getWarehouseId(),purchaseOrder.getNumber(), false).getId());
        }
        return save(purchaseOrder, loadAttribute);
    }


    /**
     * Create receipt from a purchase order
     * @param id
     * @param receiptNumber
     * @param allowUnexpectedItem
     * @param receiptQuantityMap
     * @return
     */
    public Receipt createReceiptFromPurchaseOrder(Long id, String receiptNumber, Boolean allowUnexpectedItem, Map<Long, Long> receiptQuantityMap) {
        PurchaseOrder purchaseOrder = findById(id);



        // make sure the parameters pass in is valid
        // key: PO line id
        // value: PO line open quantity
        Map<Long, PurchaseOrderLine> matchedPurchaseOrderLineMap = new HashMap<>();
        purchaseOrder.getPurchaseOrderLines().forEach(
                purchaseOrderLine -> {
                    // save the open quantity to the map

                    if (receiptQuantityMap.containsKey(purchaseOrderLine.getId())) {

                        matchedPurchaseOrderLineMap.put(purchaseOrderLine.getId(),
                                purchaseOrderLine);
                    }
                }
        );
        // make sure the receipt quantity map passed in are all valid in the purchase order and the
        // quantity doesn't exceed the open quantity

        validateCreateReceiptFromPurchaseOrder(purchaseOrder, receiptQuantityMap, matchedPurchaseOrderLineMap);


        // start to create receipt

        return receiptService.createReceiptFromPurchaseOrder(purchaseOrder,
                receiptNumber, allowUnexpectedItem, receiptQuantityMap, matchedPurchaseOrderLineMap);


    }

    private void validateCreateReceiptFromPurchaseOrder(PurchaseOrder purchaseOrder,
                                                        Map<Long, Long> receiptQuantityMap,
                                                        Map<Long, PurchaseOrderLine> matchedPurchaseOrderLineMap) {

        if (purchaseOrder.getStatus().equals(PurchaseOrderStatus.CLOSED)) {
            throw ReceiptOperationException.raiseException("Can't create from the purchase order " +
                    purchaseOrder.getNumber() + " as it is already closed");
        }

        for(Map.Entry<Long, Long> receiptQuantityMapEntry : receiptQuantityMap.entrySet()) {
            Long purchaseOrderLineId = receiptQuantityMapEntry.getKey();
            Long receiptQuantity = receiptQuantityMapEntry.getValue();
            if (!matchedPurchaseOrderLineMap.containsKey(purchaseOrderLineId)) {
                throw ReceiptOperationException.raiseException("Can't create from the purchase order " +
                        purchaseOrder.getNumber() + " as the line passed in doesn't exists in this purchase order");
            }
            Long openQuantity = matchedPurchaseOrderLineMap.get(purchaseOrderLineId).getExpectedQuantity() -
                    matchedPurchaseOrderLineMap.get(purchaseOrderLineId).getReceiptQuantity();
            if (openQuantity <= 0){
                throw ReceiptOperationException.raiseException("Can't create from the purchase order " +
                        purchaseOrder.getNumber() + " as the line " + purchaseOrderLineId
                        + " has no open quantity");
            }
            else if (openQuantity <= receiptQuantity){
                throw ReceiptOperationException.raiseException("Can't create from the purchase order " +
                        purchaseOrder.getNumber() + " as the line " + purchaseOrderLineId
                        + " 's open quantity " + openQuantity + " is less than passed in quantity " + receiptQuantity);
            }
        }
    }

    /**
     * When we create a new receipt line from the purchase order line, we will add the quantity of the
     * receipt line back to the purchase order
     * @param purchaseOrderLine
     * @param receiptQuantity
     */
    public void addReceiptQuantity(PurchaseOrderLine purchaseOrderLine, Long receiptQuantity) {
        purchaseOrderLine.setReceiptQuantity(
                purchaseOrderLine.getReceiptQuantity() + receiptQuantity
        );
        purchaseOrderLineRepository.save(purchaseOrderLine);

    }

    /**
     * When we close a receipt, if the receipt is created from the purcahse order, then we will
     * add the received quantity back to the purchase order
     * @param purchaseOrderLine
     * @param receivedQuantity
     */
    public void addReceivedQuantity(PurchaseOrderLine purchaseOrderLine, Long receivedQuantity) {
        purchaseOrderLine.setReceivedQuantity(
                purchaseOrderLine.getReceivedQuantity() + receivedQuantity
        );
        purchaseOrderLineRepository.save(purchaseOrderLine);

    }

    public void refreshReceivedQuantity(Receipt receipt) {
        PurchaseOrder purchaseOrder = receipt.getPurchaseOrder();
        if (Objects.isNull(purchaseOrder)) {
            return;
        }

        //

        for (ReceiptLine receiptLine : receipt.getReceiptLines()) {

            if (Objects.nonNull(receiptLine.getPurchaseOrderLine())) {
                addReceivedQuantity(receiptLine.getPurchaseOrderLine(),
                        receiptLine.getReceivedQuantity());
            }
        }
    }

    public void processIntegration(PurchaseOrder purchaseOrder) {

        // if the purchase order already exists, make sure its status is
        // still open
        logger.debug("start to process integration for purchase order {}",
                purchaseOrder.getNumber());
        PurchaseOrder existingPurchaseOrder = findByNumber(purchaseOrder.getWarehouseId(),
                purchaseOrder.getNumber(), false);
        logger.debug("purchase order already exists? {}",
                Objects.nonNull(existingPurchaseOrder));
        if (Objects.nonNull(existingPurchaseOrder) &&
                !existingPurchaseOrder.getStatus().equals(PurchaseOrderStatus.OPEN)) {
            throw ReceiptOperationException.raiseException("Purchase Order " + existingPurchaseOrder.getNumber() +
                    " already exists and not in OPEN status");
        }

        saveOrUpdate(purchaseOrder, false);
        logger.debug("purchase order integration processed!");
    }

    /**
     * Remove receipt quantity from the purchase order line. This is normally happens when we cancel a receipt line
     * @param purchaseOrderLine
     * @param receiptQuantity
     */
    public void removeReceiptQuantity(PurchaseOrderLine purchaseOrderLine, Long receiptQuantity) {

        purchaseOrderLine.setReceiptQuantity(
                purchaseOrderLine.getReceiptQuantity() - receiptQuantity
        );
        purchaseOrderLineRepository.save(purchaseOrderLine);

    }

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {

        purchaseOrderLineRepository.processItemOverride(
                oldItemId, newItemId, warehouseId
        );
    }
}
