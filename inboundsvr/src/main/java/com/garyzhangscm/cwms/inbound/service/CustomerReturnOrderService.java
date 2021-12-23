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
import com.garyzhangscm.cwms.inbound.repository.CustomerReturnOrderRepository;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.util.*;

import java.util.stream.Collectors;

@Service
public class CustomerReturnOrderService{
    private static final Logger logger = LoggerFactory.getLogger(CustomerReturnOrderService.class);

    @Autowired
    private CustomerReturnOrderRepository customerReturnOrderRepository;
    @Autowired
    private CustomerReturnOrderLineService customerReturnOrderLineService;
    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;


    

    public CustomerReturnOrder findById(Long id, boolean loadDetails) {
        CustomerReturnOrder customerReturnOrder =  customerReturnOrderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("customer return order not found by id: " + id));
        if (loadDetails) {
            loadAttribute(customerReturnOrder);
        }
        return customerReturnOrder;
    }

    public CustomerReturnOrder findById(Long id) {
        return findById(id, true);
    }


    public List<CustomerReturnOrder> findAll(Long warehouseId, String number, String statusList) {
        return findAll(warehouseId, number, statusList, true);
    }

    public List<CustomerReturnOrder> findAll(Long warehouseId, String number, String statusList, boolean loadDetails) {



        List<CustomerReturnOrder> customerReturnOrders =  customerReturnOrderRepository.findAll(
                (Root<CustomerReturnOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

                    if (StringUtils.isNotBlank(statusList)) {
                        CriteriaBuilder.In<ReceiptStatus> inReceiptStatuses = criteriaBuilder.in(root.get("status"));
                        for(String receiptStatus : statusList.split(",")) {
                            inReceiptStatuses.value(ReceiptStatus.valueOf(receiptStatus));
                        }
                        predicates.add(criteriaBuilder.and(inReceiptStatuses));
                    }



                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
        if (customerReturnOrders.size() > 0 && loadDetails) {
            loadAttribute(customerReturnOrders);
        }
        return customerReturnOrders;
    }

    public CustomerReturnOrder findByNumber(Long warehouseId, String number, boolean loadDetails) {
        CustomerReturnOrder customerReturnOrder = customerReturnOrderRepository.findByWarehouseIdAndNumber(warehouseId, number);
        if (customerReturnOrder != null && loadDetails) {
            loadAttribute(customerReturnOrder);
        }
        return customerReturnOrder;
    }

    public CustomerReturnOrder findByNumber(Long warehouseId, String number) {
        return findByNumber(warehouseId, number, true);
    }



    public void loadAttribute(List<CustomerReturnOrder> customerReturnOrders) {
        for(CustomerReturnOrder customerReturnOrder : customerReturnOrders) {
            loadAttribute(customerReturnOrder);
        }
    }

    public void loadAttribute(CustomerReturnOrder customerReturnOrder) {

        // Load the details for client and supplier informaiton
        if (customerReturnOrder.getClientId() != null && customerReturnOrder.getClient() == null) {
            customerReturnOrder.setClient(commonServiceRestemplateClient.getClientById(customerReturnOrder.getClientId()));
        }
        if (customerReturnOrder.getCustomerId() != null && customerReturnOrder.getCustomer() == null) {
            customerReturnOrder.setCustomer(commonServiceRestemplateClient.getCustomerById(
                    customerReturnOrder.getCustomerId()));
        }

        // load the details for receipt lines
        if (customerReturnOrder.getCustomerReturnOrderLines().size() > 0) {
            customerReturnOrderLineService.loadAttribute(customerReturnOrder.getCustomerReturnOrderLines());
        }

    }


    @Transactional
    public CustomerReturnOrder save(CustomerReturnOrder receipt, boolean loadAttribute) {
        CustomerReturnOrder newCustomerReturnOrder = customerReturnOrderRepository.save(receipt);
        if (loadAttribute) {
            loadAttribute(newCustomerReturnOrder);
        }
        return newCustomerReturnOrder;
    }

    @Transactional
    public CustomerReturnOrder save(CustomerReturnOrder customerReturnOrder) {
        return save(customerReturnOrder, true);
    }

    @Transactional
    public CustomerReturnOrder saveOrUpdate(CustomerReturnOrder customerReturnOrder) {
        return saveOrUpdate(customerReturnOrder, true);
    }

    @Transactional
    public CustomerReturnOrder saveOrUpdate(CustomerReturnOrder customerReturnOrder, boolean loadAttribute) {
        if (customerReturnOrder.getId() == null &&
                findByNumber(customerReturnOrder.getWarehouseId(),customerReturnOrder.getNumber(), false) != null) {
            customerReturnOrder.setId(findByNumber(customerReturnOrder.getWarehouseId(),customerReturnOrder.getNumber(), false).getId());
        }
        if (Objects.isNull(customerReturnOrder.getId())) {
            // we are creating a new customer return, let's setup the QC quantity
            // for each line
            customerReturnOrder.getCustomerReturnOrderLines().forEach(
                    customerReturnOrderLine -> {
                        customerReturnOrderLineService.setupQCQuantity(customerReturnOrder, customerReturnOrderLine);
                        customerReturnOrderLine.setQcQuantityRequested(0l);
                    }
            );
        }
        return save(customerReturnOrder, loadAttribute);
    }


    /**
     * Recalculate the qc quantity for all the lines in the customer return order
     *
     */
    private void recalculateQCQuantity(CustomerReturnOrder customerReturnOrder) {
        customerReturnOrder.getCustomerReturnOrderLines().forEach(
                customerReturnOrderLine -> customerReturnOrderLineService.recalculateQCQuantity(customerReturnOrderLine.getId(), null, null)
        );
    }

    @Transactional
    public void delete(CustomerReturnOrder customerReturnOrder) {
        customerReturnOrderRepository.delete(customerReturnOrder);
    }
    @Transactional
    public void delete(Long id) {
        customerReturnOrderRepository.deleteById(id);
    }


    public String getNextReceiptLineNumber(Long id) {
        CustomerReturnOrder customerReturnOrder = findById(id, false);
        if (Objects.isNull(customerReturnOrder)) {
            return "";
        }


        else if (customerReturnOrder.getCustomerReturnOrderLines().isEmpty()) {
            return "0";
        }
        else {
            // Suppose the line number is all numeric
            int max = 0;
            for(CustomerReturnOrderLine customerReturnOrderLine : customerReturnOrder.getCustomerReturnOrderLines()) {
                try {
                    if (Integer.parseInt(customerReturnOrderLine.getNumber()) > max) {
                        max = Integer.parseInt(customerReturnOrderLine.getNumber());
                    }
                }
                catch (Exception e) {
                    continue;
                }
            }
            return String.valueOf(max+1);
        }
    }

    public List<Inventory> findInventoryByCustomerReturnOrder(Long customerReturnOrderId) {

        CustomerReturnOrder customerReturnOrder = findById(customerReturnOrderId);

        List<Inventory> receivedInventory =  inventoryServiceRestemplateClient
                .findInventoryByCustomerReturnOrder(customerReturnOrder.getWarehouseId(), customerReturnOrderId,
                        null, null);

        receivedInventory.sort((inventory1, inventory2) ->
                inventory1.getLpn().compareToIgnoreCase(inventory2.getLpn())
         );
        return receivedInventory;
    }

    public List<Inventory> findInventoryByCustomerReturnOrder(Long customerReturnOrderId,
                                                  String inventoryIds,
                                                  Boolean notPutawayInventoryOnly) {

        CustomerReturnOrder customerReturnOrder = findById(customerReturnOrderId);

        return inventoryServiceRestemplateClient.
                findInventoryByCustomerReturnOrder(customerReturnOrder.getWarehouseId(), customerReturnOrderId,
                        inventoryIds,
                        notPutawayInventoryOnly);
    }

    public CustomerReturnOrder completeCustomerReturnOrder(CustomerReturnOrder customerReturnOrder) {

        if (customerReturnOrder.getStatus().equals(ReceiptStatus.CLOSED)) {
            throw ReceiptOperationException.raiseException("Can't complete the customer return order as it is already closed!");
        }
        customerReturnOrder.setStatus(ReceiptStatus.CLOSED);
        // Raise integration for receipt closing
        customerReturnOrder = saveOrUpdate(customerReturnOrder);

        integrationService.sendCustomerReturnOrderCompleteData(customerReturnOrder);

        return customerReturnOrder;
    }

    public CustomerReturnOrder completeCustomerReturnOrder(Long customerReturnOrderId) {
        return completeCustomerReturnOrder(findById(customerReturnOrderId));
    }



    public ReportHistory generateReceivingDocument(Long customerReturnOrderId, String locale)
            throws JsonProcessingException {


        return generateReceivingDocument(findById(customerReturnOrderId), locale);
    }
    public ReportHistory generateReceivingDocument(CustomerReturnOrder customerReturnOrder, String locale)
            throws JsonProcessingException {


        Long warehouseId = customerReturnOrder.getWarehouseId();


        Report reportData = new Report();
        setupReceivingDocumentData(
                reportData, customerReturnOrder
        );
        setupReceivingDocumentParameters(
                reportData, customerReturnOrder
        );

        logger.debug("will call resource service to print the receiving report with locale: {}",
                locale);
        // logger.debug("####   Report   Data  ######");
        // logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.RECEIVING_DOCUMENT, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;

    }

    private void setupReceivingDocumentParameters(
            Report report, CustomerReturnOrder customerReturnOrder) {

        // set the parameters to be the meta data of
        // the order

        logger.debug("Start to setup receiving document's paramters: {}",
                customerReturnOrder.getNumber());
        report.addParameter("receipt_number", customerReturnOrder.getNumber());

        if (Objects.nonNull(customerReturnOrder.getCustomer())) {

            report.addParameter("customer_name", customerReturnOrder.getCustomer().getName());

            report.addParameter("customer_contact_name",
                    customerReturnOrder.getCustomer().getContactorLastname() + " "
                            + customerReturnOrder.getCustomer().getContactorFirstname());

            report.addParameter("customer_address",
                    customerReturnOrder.getCustomer().getAddressLine1() + ",  "
                            + customerReturnOrder.getCustomer().getAddressCity() +
                            ", " + customerReturnOrder.getCustomer().getAddressCounty() + ", "
                            + customerReturnOrder.getCustomer().getAddressPostcode());
            report.addParameter("customer_phone", "");
        }
        else {
            report.addParameter("customer_name", "");

            report.addParameter("customer_contact_name",
                    "");

            report.addParameter("customer_address",
                    "");
            report.addParameter("customer_phone", "");

        }


        report.addParameter("totalLineCount", customerReturnOrder.getCustomerReturnOrderLines().size());

        Set<String> itemNumbers = customerReturnOrder.getCustomerReturnOrderLines().stream()
                .map(CustomerReturnOrderLine::getItem).map(Item::getName).collect(Collectors.toSet());
        report.addParameter("totalItemCount", itemNumbers.size());

        report.addParameter("totalQuantity",
                customerReturnOrder.getCustomerReturnOrderLines().stream().mapToLong(CustomerReturnOrderLine::getExpectedQuantity).sum());



    }

    private void setupReceivingDocumentData(Report report, CustomerReturnOrder customerReturnOrder) {

        // set data to be all picks
        logger.debug("Start to setup receiving document's data: {}",
                customerReturnOrder.getNumber());

        List<CustomerReturnOrderLine> customerReturnOrderLines = customerReturnOrder.getCustomerReturnOrderLines();

        report.setData(customerReturnOrderLines);

    }



    public ReportHistory generatePutawayDocument(Long customerReturnOrderId, String locale,
                                                 String inventoryIds,
                                                 Boolean notPutawayInventoryOnly)
            throws JsonProcessingException {
        logger.debug("Start to generate putaway document for customer return order id: {}",
                customerReturnOrderId);

        return generatePutawayDocument(findById(customerReturnOrderId), locale,
                inventoryIds, notPutawayInventoryOnly);
    }
    public ReportHistory generatePutawayDocument(CustomerReturnOrder customerReturnOrder, String locale,
                                                 String inventoryIds,
                                                 Boolean notPutawayInventoryOnly)
            throws JsonProcessingException {

        Long warehouseId = customerReturnOrder.getWarehouseId();

        Report reportData = new Report();
        List<Inventory> receivedInventories =
                findInventoryByCustomerReturnOrder(customerReturnOrder.getId(),
                    inventoryIds, notPutawayInventoryOnly);
        setupPutawayDocumentParameters(
                reportData, customerReturnOrder, receivedInventories
        );
        setupPutawayDocumentData(
                reportData, receivedInventories
        );

        logger.debug("will call resource service to print the Putaway report with locale: {}",
                locale);
        // logger.debug("####   Report   Data  ######");
        // logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.PUTAWAY_DOCUMENT, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;

    }

    private void setupPutawayDocumentParameters(
            Report report, CustomerReturnOrder customerReturnOrder, List<Inventory> receivedInventories) {


        logger.debug("Start to setup putaway document's paramters: {}",
                customerReturnOrder.getNumber());

        report.addParameter("receipt_number", customerReturnOrder.getNumber());


        Set<String> lpnNumber = receivedInventories.stream()
                .map(Inventory::getLpn).collect(Collectors.toSet());
        report.addParameter("totalLPNCount", lpnNumber.size());

        Set<String> itemNumbers = receivedInventories.stream()
                .map(Inventory::getItem).map(Item::getName).collect(Collectors.toSet());
        report.addParameter("totalItemCount", itemNumbers.size());


        report.addParameter("totalQuantity", receivedInventories.stream()
                .mapToLong(Inventory::getQuantity).sum());

    }

    private void setupPutawayDocumentData(Report report, List<Inventory> receivedInventories) {

        // set data to be all picks
        logger.debug("Start to setup putaway document's data. Inventory count: {}",
                receivedInventories.size());

        report.setData(receivedInventories);
    }

    public String getNextReceiptNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "customer-return-order-number");
    }



    public void processIntegration(CustomerReturnOrder customerReturnOrder) {

        // if the receipt already exists, make sure its status is
        // still open
        CustomerReturnOrder existingCustomerReturnOrder = findByNumber(customerReturnOrder.getWarehouseId(),
                customerReturnOrder.getNumber(), false);
        if (Objects.nonNull(existingCustomerReturnOrder) &&
                !existingCustomerReturnOrder.getStatus().equals(ReceiptStatus.OPEN)) {
            throw ReceiptOperationException.raiseException("Customer return order " + existingCustomerReturnOrder.getNumber() +
                    " already exists and not in OPEN status");
        }

        saveOrUpdate(existingCustomerReturnOrder, false);
    }
}
