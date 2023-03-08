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
import com.garyzhangscm.cwms.inbound.clients.*;

import com.garyzhangscm.cwms.inbound.exception.ReceiptOperationException;
import com.garyzhangscm.cwms.inbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inbound.model.*;
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

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ReceiptService {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptService.class);

    @Autowired
    private ReceiptRepository receiptRepository;
    @Autowired
    private ReceiptLineService receiptLineService;
    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;
    @Autowired
    private FileService fileService;
    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private UserService userService;
    @Autowired
    private AdminServiceRestemplateClient adminServiceRestemplateClient;


    private final static int INVENTORY_FILE_UPLOAD_MAP_SIZE_THRESHOLD = 20;
    private Map<String, Double> receiptFileUploadProgress = new ConcurrentHashMap<>();
    private Map<String, List<FileUploadResult>> receiptFileUploadResult = new ConcurrentHashMap<>();

    private Map<String, Double> receivingInventoryFileUploadProgress = new ConcurrentHashMap<>();
    private Map<String, List<FileUploadResult>> receivingInventoryFileUploadResult = new ConcurrentHashMap<>();



    public Receipt findById(Long id, boolean loadDetails) {
        Receipt receipt =  receiptRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("receipt not found by id: " + id));
        if (loadDetails) {
            loadReceiptAttribute(receipt);
        }
        return receipt;
    }

    public Receipt findById(Long id) {
        return findById(id, true);
    }


    public List<Receipt> findAll(Long warehouseId, String number, String receiptStatusList,
                                 Long supplierId, String supplierName,
                                 Long clientId, String clientName,
                                 ZonedDateTime checkInStartTime,
                                 ZonedDateTime checkInEndTime,
                                 LocalDate checkInDate, Long purchaseOrderId,
                                 ClientRestriction clientRestriction) {
        return findAll(warehouseId, number, receiptStatusList, supplierId, supplierName,
                clientId, clientName,
                checkInStartTime, checkInEndTime, checkInDate, purchaseOrderId, true, clientRestriction);
    }

    public List<Receipt> findAll(Long warehouseId, String number, String receiptStatusList,
                                 Long supplierId, String supplierName,
                                 Long clientId, String clientName,
                                 ZonedDateTime checkInStartTime,
                                 ZonedDateTime checkInEndTime,
                                 LocalDate checkInDate,
                                 Long purchaseOrderId,
                                 boolean loadDetails, ClientRestriction clientRestriction) {



        List<Receipt> receipts =  receiptRepository.findAll(
                (Root<Receipt> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(number)) {

                        if (number.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("number"), number.replaceAll("\\*", "%")));
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

                    if (Objects.nonNull(clientId)) {
                        predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));

                    }
                    if (StringUtils.isNotBlank(clientName)) {

                        Client client = commonServiceRestemplateClient.getClientByName(warehouseId, clientName);
                        if (Objects.nonNull(client)) {
                            predicates.add(criteriaBuilder.equal(root.get("clientId"), client.getId()));

                        }
                        else {

                            // we can't find the client by name,
                            predicates.add(criteriaBuilder.equal(root.get("clientId"), -1));
                        }
                    }

                    if (StringUtils.isNotBlank(receiptStatusList)) {
                        CriteriaBuilder.In<ReceiptStatus> inReceiptStatuses = criteriaBuilder.in(root.get("receiptStatus"));
                        for(String receiptStatus : receiptStatusList.split(",")) {
                            inReceiptStatuses.value(ReceiptStatus.valueOf(receiptStatus));
                        }
                        predicates.add(criteriaBuilder.and(inReceiptStatuses));
                    }

                    if (Objects.nonNull(checkInStartTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("checkInTime"), checkInStartTime));

                    }

                    if (Objects.nonNull(checkInEndTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("checkInTime"), checkInEndTime));

                    }
                    logger.debug(">> Check In Date is passed in {}", checkInDate);
                    if (Objects.nonNull(checkInDate)) {
                        LocalDateTime dateStartTime = checkInDate.atStartOfDay();
                        LocalDateTime dateEndTime = checkInDate.atStartOfDay().plusDays(1).minusSeconds(1);
                        predicates.add(criteriaBuilder.between(
                                root.get("checkInTime"), dateStartTime.atZone(ZoneOffset.UTC), dateEndTime.atZone(ZoneOffset.UTC)));

                    }
                    if (Objects.nonNull(purchaseOrderId)) {
                        Join<Receipt, PurchaseOrder> joinPurchaseOrder = root.join("purchaseOrder", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinPurchaseOrder.get("id"), purchaseOrderId));

                    }


                    Predicate[] p = new Predicate[predicates.size()];

                    // special handling for 3pl
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));

                    if (Objects.isNull(clientRestriction) ||
                            !Boolean.TRUE.equals(clientRestriction.getThreePartyLogisticsFlag()) ||
                            Boolean.TRUE.equals(clientRestriction.getAllClientAccess())) {
                        // not a 3pl warehouse, let's not put any restriction on the client
                        // (unless the client restriction is from the web request, which we already
                        // handled previously
                        return predicate;
                    }


                    // build the accessible client list predicated based on the
                    // client ID that the user has access
                    Predicate accessibleClientListPredicate;
                    if (clientRestriction.getClientAccesses().trim().isEmpty()) {
                        // the user can't access any client, then the user
                        // can only access the non 3pl data
                        accessibleClientListPredicate = criteriaBuilder.isNull(root.get("clientId"));
                    }
                    else {
                        CriteriaBuilder.In<Long> inClientIds = criteriaBuilder.in(root.get("clientId"));
                        for(String id : clientRestriction.getClientAccesses().trim().split(",")) {
                            inClientIds.value(Long.parseLong(id));
                        }
                        accessibleClientListPredicate = criteriaBuilder.and(inClientIds);
                    }

                    if (Boolean.TRUE.equals(clientRestriction.getNonClientDataAccessible())) {
                        // the user can access the non 3pl data
                        return criteriaBuilder.and(predicate,
                                criteriaBuilder.or(
                                        criteriaBuilder.isNull(root.get("clientId")),
                                        accessibleClientListPredicate));
                    }
                    else {

                        // the user can NOT access the non 3pl data
                        return criteriaBuilder.and(predicate,
                                criteriaBuilder.and(
                                        criteriaBuilder.isNotNull(root.get("clientId")),
                                        accessibleClientListPredicate));
                    }

                }
        );
        if (receipts.size() > 0 && loadDetails) {
            loadReceiptAttribute(receipts);
        }
        return receipts;
    }

    public Receipt findByNumber(Long warehouseId, String number, boolean loadDetails) {
        Receipt receipt = receiptRepository.findByNumber(warehouseId, number);
        if (receipt != null && loadDetails) {
            loadReceiptAttribute(receipt);
        }
        return receipt;
    }

    public Receipt findByNumber(Long warehouseId, String number) {
        return findByNumber(warehouseId, number, true);
    }



    public void loadReceiptAttribute(List<Receipt> receipts) {
        for(Receipt receipt : receipts) {
            loadReceiptAttribute(receipt);
        }
    }

    public void loadReceiptAttribute(Receipt receipt) {
        if (receipt.getWarehouseId() != null && receipt.getWarehouse() == null) {
            receipt.setWarehouse(warehouseLayoutServiceRestemplateClient.getWarehouseById(receipt.getWarehouseId()));
        }
        // Load the details for client and supplier informaiton
        if (receipt.getClientId() != null && receipt.getClient() == null) {
            receipt.setClient(commonServiceRestemplateClient.getClientById(receipt.getClientId()));
        }
        if (receipt.getSupplierId() != null && receipt.getSupplier() == null) {
            receipt.setSupplier(commonServiceRestemplateClient.getSupplierById(receipt.getSupplierId()));
        }

        // load the details for receipt lines
        if (receipt.getReceiptLines().size() > 0) {
            receiptLineService.loadReceiptLineAttribute(receipt.getReceiptLines());
        }

        receipt.getReceiptBillableActivities().forEach(
                receiptBillableActivity -> {
                    if (Objects.nonNull(receiptBillableActivity.getBillableActivityTypeId()) &&
                        Objects.isNull(receiptBillableActivity.getBillableActivityType())) {

                        receiptBillableActivity.setBillableActivityType(
                                adminServiceRestemplateClient.getBillableActivityTypeById(
                                        receiptBillableActivity.getBillableActivityTypeId()
                                )
                        );
                    }

                }
        );

    }


    @Transactional
    public Receipt save(Receipt receipt, boolean loadAttribute) {
        Receipt newReceipt = receiptRepository.save(receipt);
        if (loadAttribute) {
            loadReceiptAttribute(newReceipt);
        }
        return newReceipt;
    }

    @Transactional
    public Receipt save(Receipt receipt) {
        return save(receipt, true);
    }

    @Transactional
    public Receipt saveOrUpdate(Receipt receipt) {
        return saveOrUpdate(receipt, true);
    }

    @Transactional
    public Receipt saveOrUpdate(Receipt receipt, boolean loadAttribute) {
        if (receipt.getId() == null && findByNumber(receipt.getWarehouseId(),receipt.getNumber(), false) != null) {
            receipt.setId(findByNumber(receipt.getWarehouseId(),receipt.getNumber(), false).getId());
        }
        if (Objects.isNull(receipt.getId())) {
            // we are creating a new receipt, let's setup the QC quantity
            // for each line
            receipt.getReceiptLines().forEach(
                    receiptLine -> {
                        receiptLineService.setupQCQuantity(receipt, receiptLine);
                        receiptLine.setQcQuantityRequested(0l);
                    }
            );
        }
        return save(receipt, loadAttribute);
    }



    @Transactional
    public Receipt checkInReceipt(Long receiptId) {
        Receipt receipt = findById(receiptId, false);
        logger.debug("receipt ID: {}, status: {}", receiptId, receipt.getReceiptStatus());
        logger.debug("receipt.getReceiptStatus().equals(ReceiptStatus.OPEN)? {}", receipt.getReceiptStatus().equals(ReceiptStatus.OPEN));
        // only allow check in when the receipt is in OPEN status
        if (receipt != null && receipt.getReceiptStatus().equals(ReceiptStatus.OPEN)) {

            receipt.setReceiptStatus(ReceiptStatus.CHECK_IN);
            receipt.setCheckInTime(ZonedDateTime.now(ZoneOffset.UTC));
            logger.debug("update the receipt to check in @ {}",
                    receipt.getCheckInTime());
            if (recalculateQCDuringCheckin()) {
                recalculateQCQuantity(receipt);
            }
            Receipt newReceipt = saveOrUpdate(receipt, false);

            // After we check in the receipt, we will create a location
            // with the same name of the receipt so that we can receive inventory
            // on this receipt

            logger.debug("create the location for the receipt");
            warehouseLayoutServiceRestemplateClient.createLocationForReceipt(newReceipt);
            return newReceipt;
        }
        throw ReceiptOperationException.raiseException("Can't check in the receipt due to not correct status. Current Status: " + receipt.getReceiptStatus());
    }

    /**
     * Recalculate the qc quantity for all the lines in the receipt
     * @param receipt
     */
    private void recalculateQCQuantity(Receipt receipt) {
        receipt.getReceiptLines().forEach(
                receiptLine -> receiptLineService.recalculateQCQuantity(receiptLine.getId(), null, null)
        );
    }

    private boolean recalculateQCDuringCheckin() {
        return true;
    }

    @Transactional
    public Receipt addReceipt(Long warehouseId, String number, String clientId, String supplierId) {
        Receipt receipt = new Receipt();
        receipt.setNumber(number);
        receipt.setWarehouseId(warehouseId);
        receipt.setReceiptStatus(ReceiptStatus.OPEN);
        if (!StringUtils.isBlank(clientId)) {
            logger.debug("start to get client by ID: {}", clientId);
            Client client = commonServiceRestemplateClient.getClientById(Long.parseLong(clientId));
            if (client != null) {
                logger.debug("get client, ID: {}, client: {}", clientId, client);
                receipt.setClientId(client.getId());
            }
        }

        if (!StringUtils.isBlank(supplierId)) {
            logger.debug("start to get supplier by ID: {}", supplierId);
            Supplier supplier = commonServiceRestemplateClient.getSupplierById(Long.parseLong(supplierId));
            if (supplier != null) {
                logger.debug("get supplier, ID: {}, supplier: {}", supplierId, supplier);
                receipt.setSupplierId(supplier.getId());
            }
        }

        return save(receipt);

    }
    @Transactional
    public void delete(Receipt receipt) {
        receiptRepository.delete(receipt);
    }
    @Transactional
    public void delete(Long id) {
        receiptRepository.deleteById(id);
    }
    @Transactional
    public void delete(String receiptIds) {
        if (!receiptIds.isEmpty()) {
            long[] receiptIdArray = Arrays.asList(receiptIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : receiptIdArray) {
                delete(id);
            }
        }
    }

    /**
    public List<ReceiptCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("number").
                addColumn("client").
                addColumn("supplier").
                addColumn("allowUnexpectedItem").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ReceiptCSVWrapper.class);
    }
*/
    /**
    @Transactional
    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ReceiptCSVWrapper> receiptCSVWrappers = loadData(inputStream);
            receiptCSVWrappers.stream().forEach(receiptCSVWrapper -> saveOrUpdate(convertFromWrapper(receiptCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
**/

    private Receipt convertFromWrapper(Long warehouseId,
                                       ReceiptLineCSVWrapper receiptLineCSVWrapper) {

        Receipt receipt = new Receipt();
        receipt.setNumber(receiptLineCSVWrapper.getReceipt());
        receipt.setReceiptStatus(ReceiptStatus.OPEN);

        boolean allowUnexpectedItem =
                Strings.isNotBlank(receiptLineCSVWrapper.getAllowUnexpectedItem()) &&
                        (
                                receiptLineCSVWrapper.getAllowUnexpectedItem().equalsIgnoreCase("1") ||
                                        receiptLineCSVWrapper.getAllowUnexpectedItem().equalsIgnoreCase("true")
                                );

        receipt.setAllowUnexpectedItem(allowUnexpectedItem);

        receipt.setWarehouseId(warehouseId);

        if (!StringUtils.isBlank(receiptLineCSVWrapper.getClient())) {
            Client client = commonServiceRestemplateClient.getClientByName(
                    warehouseId, receiptLineCSVWrapper.getClient());
            receipt.setClientId(client.getId());
        }
        if (!StringUtils.isBlank(receiptLineCSVWrapper.getSupplier())) {
            Supplier supplier = commonServiceRestemplateClient.getSupplierByName(
                    warehouseId, receiptLineCSVWrapper.getSupplier());
            receipt.setSupplierId(supplier.getId());
        }
        return receipt;
    }

    public String getNextReceiptLineNumber(Long id) {
        Receipt receipt = findById(id);
        if (Objects.isNull(receipt)) {
            return "";
        }


        else if (receipt.getReceiptLines().isEmpty()) {
            return "0";
        }
        else {
            // Suppose the line number is all numeric
            int max = 0;
            for(ReceiptLine receiptLine : receipt.getReceiptLines()) {
                try {
                    if (Integer.parseInt(receiptLine.getNumber()) > max) {
                        max = Integer.parseInt(receiptLine.getNumber());
                    }
                }
                catch (Exception e) {
                    continue;
                }
            }
            return String.valueOf(max+1);
        }
    }

    public List<Inventory> findInventoryByReceipt(Long receiptId) {

        Receipt receipt = findById(receiptId);

        List<Inventory> receivedInventory =  inventoryServiceRestemplateClient
                .findInventoryByReceipt(receipt.getWarehouseId(), receiptId,
                        null, null);

        receivedInventory.sort((inventory1, inventory2) ->
                inventory1.getLpn().compareToIgnoreCase(inventory2.getLpn())
         );
        return receivedInventory;
    }

    public List<Inventory> findInventoryByReceipt(Long receiptId,
                                                  String inventoryIds,
                                                  Boolean notPutawayInventoryOnly) {

        Receipt receipt = findById(receiptId);

        return inventoryServiceRestemplateClient.
                findInventoryByReceipt(receipt.getWarehouseId(), receiptId,
                        inventoryIds,
                        notPutawayInventoryOnly);
    }

    public Receipt completeReceipt(Receipt receipt) {

        if (receipt.getReceiptStatus().equals(ReceiptStatus.CLOSED)) {
            throw ReceiptOperationException.raiseException("Can't complete the receipt as it is already closed!");
        }
        receipt.setReceiptStatus(ReceiptStatus.CLOSED);
        // Raise integration for receipt closing
        receipt = saveOrUpdate(receipt);

        integrationService.sendReceiptCompleteData(receipt);
        if (Objects.nonNull(receipt.getPurchaseOrder())) {
            refreshReceivedQuantity(receipt);
        }

        return receipt;
    }

    private void refreshReceivedQuantity(Receipt receipt) {
        purchaseOrderService.refreshReceivedQuantity(receipt);
    }

    public Receipt completeReceipt(Long receiptId) {
        return completeReceipt(findById(receiptId));
    }


    /**
     * Wo will only change the receipt's attribute here. We will not process
     * receipt line
     * @param receipt
     * @return
     */
    public Receipt changeReceipt(Long id, Receipt receipt) {

        Receipt existingReceipt = findById(id);

        existingReceipt.setClientId(receipt.getClientId());
        existingReceipt.setSupplierId(receipt.getSupplierId());
        existingReceipt.setAllowUnexpectedItem(receipt.getAllowUnexpectedItem());

        return saveOrUpdate(existingReceipt);
    }


    public ReportHistory generateReceivingDocument(Long receiptId, String locale, String printerName)
            throws JsonProcessingException {


        return generateReceivingDocument(findById(receiptId), locale, printerName);
    }
    public ReportHistory generateReceivingDocument(Receipt receipt, String locale,
                                                   String printerName)
            throws JsonProcessingException {


        Long warehouseId = receipt.getWarehouseId();


        Report reportData = new Report();
        setupReceivingDocumentData(
                reportData, receipt
        );
        setupReceivingDocumentParameters(
                reportData, receipt
        );

        logger.debug("will call resource service to print the receiving report with locale: {}",
                locale);
        // logger.debug("####   Report   Data  ######");
        // logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.RECEIVING_DOCUMENT, reportData, locale,
                        printerName
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;

    }

    private void setupReceivingDocumentParameters(
            Report report, Receipt receipt) {

        // set the parameters to be the meta data of
        // the order

        logger.debug("Start to setup receiving document's paramters: {}",
                receipt.getNumber());
        report.addParameter("receipt_number", receipt.getNumber());
        report.addParameter("check_in_time",
                Objects.nonNull(receipt.getCheckInTime()) ? receipt.getCheckInTime() : "");

        if (Objects.nonNull(receipt.getSupplier())) {

            report.addParameter("supplier_name", receipt.getSupplier().getName());
            report.addParameter("supplier_description", receipt.getSupplier().getDescription());

            report.addParameter("supplier_contact_name",
                    receipt.getSupplier().getContactorLastname() + " " + receipt.getSupplier().getContactorFirstname());

            report.addParameter("supplier_address",
                    receipt.getSupplier().getAddressLine1() + ",  " + receipt.getSupplier().getAddressCity() +
                            ", " + receipt.getSupplier().getAddressCounty() + ", " + receipt.getSupplier().getAddressPostcode());
            report.addParameter("supplier_phone", "");
        }
        else {
            report.addParameter("supplier_name", "");
            report.addParameter("supplier_description", "");

            report.addParameter("supplier_contact_name",
                    "");

            report.addParameter("supplier_address",
                    "");
            report.addParameter("supplier_phone", "");

        }


        report.addParameter("totalLineCount", receipt.getReceiptLines().size());

        Set<String> itemNumbers = receipt.getReceiptLines().stream()
                .map(ReceiptLine::getItem).map(Item::getName).collect(Collectors.toSet());
        report.addParameter("totalItemCount", itemNumbers.size());

        report.addParameter("totalQuantity",
                receipt.getReceiptLines().stream().mapToLong(ReceiptLine::getExpectedQuantity).sum());



    }

    private void setupReceivingDocumentData(Report report, Receipt receipt) {

        // set data to be all picks
        logger.debug("Start to setup receiving document's data: {}",
                receipt.getNumber());

        List<ReceiptLine> receiptLines = receipt.getReceiptLines();

        report.setData(receiptLines);

    }



    public ReportHistory generatePutawayDocument(Long receiptId, String locale,
                                                 String inventoryIds,
                                                 Boolean notPutawayInventoryOnly,
                                                 String printerName)
            throws JsonProcessingException {
        logger.debug("Start to generate putaway document for receipt id: {}",
                receiptId);

        return generatePutawayDocument(findById(receiptId), locale,
                inventoryIds, notPutawayInventoryOnly, printerName);
    }
    public ReportHistory generatePutawayDocument(Receipt receipt, String locale,
                                                 String inventoryIds,
                                                 Boolean notPutawayInventoryOnly,
                                                 String printerName)
            throws JsonProcessingException {

        Long warehouseId = receipt.getWarehouseId();

        Report reportData = new Report();
        List<Inventory> receivedInventories =
                findInventoryByReceipt(receipt.getId(),
                    inventoryIds, notPutawayInventoryOnly);
        setupPutawayDocumentParameters(
                reportData, receipt, receivedInventories
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
                        warehouseId, ReportType.PUTAWAY_DOCUMENT, reportData, locale,
                        printerName
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;

    }

    private void setupPutawayDocumentParameters(
            Report report, Receipt receipt, List<Inventory> receivedInventories) {


        logger.debug("Start to setup putaway document's paramters: {}",
                receipt.getNumber());

        report.addParameter("receipt_number", receipt.getNumber());


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

    /**
     * Create receipt from the warehouser transfer receipt request
     * @param warehouseTransferReceipt
     */
    public void processWarehouseTransferReceiptRequest(WarehouseTransferReceipt warehouseTransferReceipt) {
        Receipt receipt = new Receipt();
        if (Strings.isNotBlank(warehouseTransferReceipt.getReceiptNumber())) {
            receipt.setNumber(warehouseTransferReceipt.getReceiptNumber());
        }
        else {
            receipt.setNumber( getNextReceiptNumber(warehouseTransferReceipt.getDestinationWarehouseId()));
        }

        receipt.setWarehouseId(warehouseTransferReceipt.getDestinationWarehouseId());
        receipt.setTransferOrderWarehouseId(warehouseTransferReceipt.getSourceWarehouseId());
        receipt.setTransferOrderNumber(warehouseTransferReceipt.getOrderNumber());
        receipt.setCategory(ReceiptCategory.WAREHOUSE_TRANSFER_ORDER);

        // create receipt lines according to the items being shipped
        AtomicInteger receiptLineSequence = new AtomicInteger();
        warehouseTransferReceipt.getShippedItem().forEach(
                (itemId, quantity) -> {
                    ReceiptLine receiptLine = new ReceiptLine();

                    if (Objects.nonNull(itemId) && quantity > 0) {
                        receiptLine.setWarehouseId(
                                receipt.getWarehouseId()
                        );
                        receiptLine.setReceipt(receipt);
                        receiptLine.setItemId(itemId);

                        receiptLine.setNumber(receiptLineSequence.toString());
                        receiptLineSequence.getAndIncrement();
                        receiptLine.setExpectedQuantity(quantity);
                        // over receive is normally disallowed for warehouse transfer order
                        receiptLine.setOverReceivingQuantity(0L);
                        receiptLine.setOverReceivingPercent(0.0);
                        receiptLine.setReceivedQuantity(0L);
                        receipt.getReceiptLines().add(receiptLine);

                    }
                }
        );

        save(receipt, false);


    }

    public String getNextReceiptNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "receipt-number");
    }


    public ReportHistory generatePrePrintLPNReport(Long id, String lpnNumber, Long lpnQuantity, String locale, String printerName)
            throws JsonProcessingException {
        return generatePrePrintLPNDocument(ReportType.RECEIVING_LPN_REPORT, receiptLineService.findById(id), lpnNumber, lpnQuantity, locale, printerName);
    }
    public ReportHistory generatePrePrintLPNLabel(Long id, String lpnNumber, Long lpnQuantity, String locale, String printerName)
            throws JsonProcessingException {
        return generatePrePrintLPNDocument(ReportType.RECEIVING_LPN_LABEL, receiptLineService.findById(id), lpnNumber, lpnQuantity, locale, printerName);
    }
    /**
     * Print LPN label or document
     * @param id
     * @param lpnNumber
     * @param lpnQuantity
     * @param locale
     * @param printerName
     * @return
     * @throws JsonProcessingException
     */
    public ReportHistory generatePrePrintLPNDocument(ReportType reportType, Long id, String lpnNumber, Long lpnQuantity, String locale, String printerName)
            throws JsonProcessingException {
        return generatePrePrintLPNDocument(reportType, receiptLineService.findById(id), lpnNumber, lpnQuantity, locale, printerName);
    }

    public ReportHistory generatePrePrintLPNDocument(ReportType reportType,
                                                     ReceiptLine receiptLine,
                                                     String lpnNumber,
                                                     Long lpnQuantity, String locale,
                                                  String printerName)
            throws JsonProcessingException {
        Long warehouseId = receiptLine.getWarehouseId();

        Report reportData = new Report();
        // setup the parameters for the label;
        // for label, we don't need the actual data.
        setupPrePrintLPNLabelParameters(
                reportData, receiptLine, lpnNumber, lpnQuantity
        );
        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        logger.debug("####   Report   Data  ######");
        logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, reportType, reportData, locale,
                        printerName
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;
    }




    private void setupPrePrintLPNLabelParameters(
            Report report, ReceiptLine receiptLine, String lpnNumber,
            Long lpnQuantity) {

        Map<String, Object> lpnLabelContent =   getLPNDocumentContent(
                receiptLine, lpnNumber, lpnQuantity
        );
        for(Map.Entry<String, Object> entry : lpnLabelContent.entrySet()) {

            report.addParameter(entry.getKey(), entry.getValue());
        }



    }

    private Map<String, Object> getLPNDocumentContent(ReceiptLine receiptLine, String lpnNumber,
                                                   Long lpnQuantity) {

        Map<String, Object> lpnLabelContent = new HashMap<>();
        lpnLabelContent.put("lpn", lpnNumber);
        lpnLabelContent.put("item_family", Objects.nonNull(receiptLine.getItem().getItemFamily()) ?
                receiptLine.getItem().getItemFamily().getDescription() : "");
        lpnLabelContent.put("item_name", receiptLine.getItem().getName());
        lpnLabelContent.put("receipt_number", receiptLine.getReceipt().getNumber());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy");
        lpnLabelContent.put("check_in_date", LocalDateTime.now().format(formatter));


        if (Objects.nonNull(receiptLine.getReceipt())) {
            // if we already have supplier setup, then use the value. otherwise,
            // get the supplier information from the id
            Supplier supplier = receiptLine.getReceipt().getSupplier();
            if (Objects.isNull(supplier) &&
                    Objects.nonNull(receiptLine.getReceipt().getSupplierId())) {

                supplier = commonServiceRestemplateClient.getSupplierById(
                    receiptLine.getReceipt().getSupplierId()
                );
            }

            lpnLabelContent.put("supplier",
                    Objects.nonNull(supplier) ? supplier.getDescription() : "");
        }
        else {

            lpnLabelContent.put("supplier",  "");
        }

        if (Objects.nonNull(lpnQuantity)) {
            logger.debug("LPN Quantity is passed in: {}", lpnQuantity);
            lpnLabelContent.put("quantity", lpnQuantity);

        }
        else if (receiptLine.getItem().getItemPackageTypes().size() > 0){
            logger.debug("LPN Quantity is not passed in, let's get from the UOM");
            // the user doesn't specify hte lpn quantity, let's get from the item's default package type
            ItemPackageType itemPackageType = receiptLine.getItem().getItemPackageTypes().get(0);
            // get the biggest item uom
            if (itemPackageType.getItemUnitOfMeasures().size() > 0) {

                Long lpnQuantityFromItemUOM
                        = itemPackageType.getItemUnitOfMeasures().stream().mapToLong(ItemUnitOfMeasure::getQuantity).max().orElse(0l);

                logger.debug("LPN Quantity is setup to {}, according to item {}, package type {}",
                        lpnQuantityFromItemUOM, receiptLine.getItem().getName(),
                        itemPackageType.getName());
                lpnLabelContent.put("quantity", lpnQuantityFromItemUOM);
            }
            else  {

                logger.debug("item {} , package type {} have no UOM defined yet",
                        receiptLine.getItem().getName(), itemPackageType.getName());
                lpnLabelContent.put("quantity", 0);
            }
        }
        else {

            logger.debug("item {} have no item package type defined yet", receiptLine.getItem().getName());
            lpnLabelContent.put("quantity", 0);
        }
        return lpnLabelContent;

    }

    public ReportHistory generatePrePrintLPNLabelInBatch(Long id, String lpn, Long lpnQuantity, Integer count,
                                                         Integer copies, String locale,
                                                         String printerName) throws JsonProcessingException {
        return generatePrePrintLPNDocumentInBatch(
                ReportType.RECEIVING_LPN_LABEL, id,
                lpn, lpnQuantity, count, copies, locale, printerName
        );
    }


    public ReportHistory generatePrePrintLPNReportInBatch(Long id, String lpn, Long lpnQuantity, Integer count,
                                                         Integer copies, String locale,
                                                         String printerName) throws JsonProcessingException {
        return generatePrePrintLPNDocumentInBatch(
                ReportType.RECEIVING_LPN_REPORT, id,
                lpn, lpnQuantity, count, copies, locale, printerName
        );
    }

    /**
     * Generate multiple labels in a batch, one for each lpn
     * @param id
     * @param lpn
     * @param lpnQuantity
     * @param count
     * @param locale
     * @return
     */
    public ReportHistory generatePrePrintLPNDocumentInBatch(ReportType reportType,
                                                         Long id, String lpn, Long lpnQuantity, Integer count,
                                                         Integer copies, String locale,
                                                         String printerName) throws JsonProcessingException {
        return generatePrePrintLPNDocumentInBatch(
                reportType, receiptLineService.findById(id),
                lpn, lpnQuantity, count, copies, locale, printerName
        );
    }

    public ReportHistory generatePrePrintLPNDocumentInBatch(ReportType reportType,
                                                         ReceiptLine receiptLine, String lpn, Long lpnQuantity, Integer count,
                                                         Integer copies, String locale,
                                                         String printerName) throws JsonProcessingException {

        Long warehouseId = receiptLine.getWarehouseId();
        List<String> lpnNumbers;
        if (Strings.isNotBlank(lpn)) {
            // if the user specify the start lpn, then generate lpns based on this
            lpnNumbers = getNextLPNNumbers(lpn, count);
        }
        else {
            lpnNumbers = commonServiceRestemplateClient.getNextNumberInBatch(warehouseId, "receiving-lpn-number", count);
        }
        logger.debug("we will print document for lpn : {}, by document type {}",
                lpnNumbers, reportType);
        if (lpnNumbers.size() > 0) {


            Report reportData = new Report();
            // setup the parameters for the label;
            // for label, we don't need the actual data.
            if (reportType.isLabel()) {

                setupPrePrintLPNLabelData(
                        reportData, receiptLine, lpnNumbers, lpnQuantity, copies
                );
            }
            else {

                setupPrePrintLPNDocumentData(
                        reportData, receiptLine, lpnNumbers, lpnQuantity, copies
                );
            }
            logger.debug("will call resource service to print the report with locale: {}",
                    locale);
            logger.debug("####   Report   Data  ######");
            logger.debug(reportData.toString());
            ReportHistory reportHistory =
                    resourceServiceRestemplateClient.generateReport(
                            warehouseId, reportType, reportData, locale,
                            printerName
                    );


            logger.debug("####   Report   printed: {}", reportHistory.getFileName());
            return reportHistory;
        }
        throw ReceiptOperationException.raiseException("Can't get lpn numbers");
    }

    private void setupPrePrintLPNLabelData(Report reportData, ReceiptLine receiptLine, List<String> lpnNumbers,
                                           Long lpnQuantity, Integer copies) {

        List<Map<String, Object>> lpnLabelContents = new ArrayList<>();
        lpnNumbers.forEach(
                lpnNumber -> {

                    Map<String, Object> lpnLabelContent =   getLPNDocumentContent(
                            receiptLine, lpnNumber, lpnQuantity
                    );
                    for (int i = 0; i < copies; i++) {

                        lpnLabelContents.add(lpnLabelContent);
                    }
                }
        );
        reportData.setData(lpnLabelContents);

    }

    private void setupPrePrintLPNDocumentData(Report reportData, ReceiptLine receiptLine, List<String> lpnNumbers,
                                           Long lpnQuantity, Integer copies) {

        List<ReceivingLPNReportData> receivingLPNReportData = new ArrayList<>();
        lpnNumbers.forEach(
                lpnNumber -> {

                    Map<String, Object> lpnLabelContent =   getLPNDocumentContent(
                            receiptLine, lpnNumber, lpnQuantity
                    );
                    for (int i = 0; i < copies; i++) {

                        receivingLPNReportData.add(
                                new ReceivingLPNReportData(
                                        lpnLabelContent.get("item_family").toString(),
                                        lpnLabelContent.get("item_name").toString(),
                                        lpnLabelContent.get("receipt_number").toString(),
                                        lpnLabelContent.get("supplier").toString(),
                                        lpnLabelContent.get("check_in_date").toString(),
                                        lpnLabelContent.get("quantity").toString(),
                                        lpnLabelContent.get("lpn").toString()));

                    }
                }
        );
        reportData.setData(receivingLPNReportData);

    }
    private List<String> getNextLPNNumbers(String lpn, Integer count) {


        // num[0] will be 21

        List<String> lpnNumbers = new ArrayList<>();
        logger.debug("start to get next batch of lpn number from user input lpn {}", lpn);
        Pattern prefixLetterPattern =  Pattern.compile("[a-zA-Z]+");
        Matcher matcher = prefixLetterPattern.matcher(lpn);
        if (matcher.find()) {
            logger.debug("we found the prefix letters");
            String prefixLetters = matcher.group();
            logger.debug("> {}", prefixLetters);
            Long startNumber = Long.parseLong(lpn.replace(prefixLetters, ""));
            logger.debug("> and the startNumber is {}", startNumber);

            for(int i = 0; i<count ; i++) {
                // padding leading 0 to the number
                String numberPattern = "%0" + (lpn.length() - prefixLetters.length())+ "d";
                lpnNumbers.add(
                        prefixLetters + String.format(numberPattern, (i + startNumber))
                );
            }
        }
        return lpnNumbers;
    }

    public void processIntegration(Receipt receipt) {

        // if the receipt already exists, make sure its status is
        // still open
        Receipt existingReceipt = findByNumber(receipt.getWarehouseId(),
                receipt.getNumber(), false);
        if (Objects.nonNull(existingReceipt) &&
                !existingReceipt.getReceiptStatus().equals(ReceiptStatus.OPEN)) {
            throw ReceiptOperationException.raiseException("Receipt " + existingReceipt.getNumber() +
                    " already exists and not in OPEN status");
        }

        saveOrUpdate(receipt, false);
    }

    public Integer getReceiptCountBySupplier(Long warehouseId, Long supplierId, String supplierName) {
        if (Objects.isNull(supplierId) && Strings.isBlank(supplierName)) {
            throw ReceiptOperationException.raiseException(
                    "Either supplier id or supplier name must be passed in " +
                            " to get the receipt count for the supplier");
        }
        return findAll(warehouseId, null, null, supplierId,
                supplierName, null, null, null,
                null, null, null, false, null).size();
    }

    /**
     * Create a new receipt from purchase order
     * @param purchaseOrder
     * @param receiptNumber
     * @param allowUnexpectedItem
     * @param receiptQuantityMap
     * @return
     */
    public Receipt createReceiptFromPurchaseOrder(PurchaseOrder purchaseOrder,
                                                  String receiptNumber, Boolean allowUnexpectedItem,
                                                  Map<Long, Long> receiptQuantityMap,
                                                  Map<Long, PurchaseOrderLine> matchedPurchaseOrderLineMap) {
        Receipt receipt = new Receipt();
        receipt.setNumber(receiptNumber);
        receipt.setPurchaseOrder(purchaseOrder);
        receipt.setReceiptStatus(ReceiptStatus.OPEN);
        receipt.setCategory(ReceiptCategory.PURCHASE_ORDER);
        receipt.setWarehouseId(purchaseOrder.getWarehouseId());
        receipt.setAllowUnexpectedItem(Objects.isNull(allowUnexpectedItem) ? false : allowUnexpectedItem);
        receipt.setClientId(purchaseOrder.getClientId());
        receipt.setSupplierId(purchaseOrder.getSupplierId());


        for(Map.Entry<Long, Long> receiptQuantityMapEntry : receiptQuantityMap.entrySet()) {
            Long purchaseOrderLineId = receiptQuantityMapEntry.getKey();
            Long receiptQuantity = receiptQuantityMapEntry.getValue();
            PurchaseOrderLine purchaseOrderLine
                    = matchedPurchaseOrderLineMap.get( purchaseOrderLineId );

            ReceiptLine receiptLine = new ReceiptLine();
            receiptLine.setReceipt(receipt);
            receiptLine.setNumber(purchaseOrderLine.getNumber());
            receiptLine.setWarehouseId(purchaseOrderLine.getWarehouseId());
            receiptLine.setItemId(purchaseOrderLine.getItemId());
            receiptLine.setExpectedQuantity(receiptQuantity);
            receiptLine.setPurchaseOrderLine(purchaseOrderLine);
            receipt.addReceiptLines(receiptLine);

        }
        Receipt newReceipt =  saveOrUpdate(receipt);

        for (ReceiptLine receiptLine : newReceipt.getReceiptLines()) {

            if (Objects.nonNull(receiptLine.getPurchaseOrderLine())) {

                purchaseOrderService.addReceiptQuantity(
                        receiptLine.getPurchaseOrderLine(), receiptLine.getExpectedQuantity());
            }
        }
        return newReceipt;
    }

    public void removeReceipts(String receiptIds) {
        // get all the receipts we are try to remove. We may need to
        // return the quantity back to the purchase order if
        // the receipt is created from the purchase order
        Arrays.stream(receiptIds.split(",")).forEach(
                receiptId -> removeReceipt(Long.parseLong(receiptId))
        );
    }

    /**
     * Remove receipt by id
     * @param receiptId
     */
    public void removeReceipt(Long receiptId) {
        Receipt receipt  = findById(receiptId);
        // see if any of the receipt is created from a purchase order line
        receipt.getReceiptLines().stream().filter(
                receiptLine -> Objects.nonNull(receiptLine.getPurchaseOrderLine())
        ).forEach(
                receiptLine -> purchaseOrderService.removeReceiptQuantity(
                        receiptLine.getPurchaseOrderLine(), receiptLine.getExpectedQuantity()
                )
        );

        delete(receiptId);

    }

    public Receipt addReceipts(Receipt receipt) {
        // make sure all the necessary field is filled in
        for (ReceiptLine receiptLine : receipt.getReceiptLines()) {
            if (Objects.isNull(receiptLine)) {
                throw ReceiptOperationException.raiseException("Can't create the receipt as the item for line " +
                        receiptLine.getNumber() + " is null");
            }
        }
        return saveOrUpdate(receipt);
    }

    public String saveReceiptData(Long warehouseId, File localFile) throws IOException {

        String username = userService.getCurrentUserName();

        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();

        clearReceiptFileUploadMap();
        receiptFileUploadProgress.put(fileUploadProgressKey, 0.0);
        receiptFileUploadResult.put(fileUploadProgressKey, new ArrayList<>());

        List<ReceiptLineCSVWrapper> receiptLineCSVWrappers = loadDataWithLine(localFile);
        logger.debug("start to save {} receipt lines ", receiptLineCSVWrappers.size());

        receiptFileUploadProgress.put(fileUploadProgressKey, 10.0);

        new Thread(() -> {

            int totalReceiptLineCount = receiptLineCSVWrappers.size();
            int index = 0;
            // see if we need to create order
            for (ReceiptLineCSVWrapper receiptLineCSVWrapper : receiptLineCSVWrappers) {
                try {

                    receiptFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalReceiptLineCount) * (index));
                    Receipt receipt = findByNumber(warehouseId, receiptLineCSVWrapper.getReceipt());
                    receiptFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalReceiptLineCount) * (index + 0.25));
                    if (Objects.isNull(receipt)) {
                        logger.debug("receipt {} is not created yet, let's create the order on the fly ", receiptLineCSVWrapper.getReceipt());
                        receipt = saveOrUpdate(
                                convertFromWrapper(warehouseId, receiptLineCSVWrapper)
                        );
                    }
                    receiptFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalReceiptLineCount) * (index + 0.5));
                    logger.debug("start to create receipt line {} for item {}, quantity {}, for receipt {}",
                            receiptLineCSVWrapper.getLine(),
                            receiptLineCSVWrapper.getItem(),
                            receiptLineCSVWrapper.getExpectedQuantity(),
                            receiptLineCSVWrapper.getReceipt());
                    receiptLineService.saveReceiptLineData(warehouseId, receipt, receiptLineCSVWrapper);

                    receiptFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalReceiptLineCount) * (index + 1));


                    List<FileUploadResult> fileUploadResults = receiptFileUploadResult.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            receiptLineCSVWrappers.toString(),
                            "success", ""
                    ));
                    receiptFileUploadResult.put(fileUploadProgressKey, fileUploadResults);
                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process receipt line upload file record: {}, \n error message: {}",
                            receiptLineCSVWrappers,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = receiptFileUploadResult.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            receiptLineCSVWrappers.toString(),
                            "fail", ex.getMessage()
                    ));
                    receiptFileUploadResult.put(fileUploadProgressKey, fileUploadResults);
                }
                finally {

                    index++;
                }
                // after we process all inventory, mark the progress to 100%
                receiptFileUploadProgress.put(fileUploadProgressKey, 100.0);
            }

        }).start();
        return fileUploadProgressKey;

    }

    private List<ReceiptLineCSVWrapper> loadDataWithLine(File file) throws IOException {


        // return fileService.loadData(file, getCsvSchemaWithLine(), ReceiptLineCSVWrapper.class);
        return fileService.loadData(file, ReceiptLineCSVWrapper.class);
    }

    public String saveReceivingInventoryData(Long warehouseId, File file) throws IOException {

        String username = userService.getCurrentUserName();
        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();

        clearRecevingInventoryFileUploadMap();
        receivingInventoryFileUploadResult.put(fileUploadProgressKey, new ArrayList<>());

        receivingInventoryFileUploadProgress.put(fileUploadProgressKey, 0.0);

        List<InventoryCSVWrapper> inventoryCSVWrappers =
                fileService.loadData(file, InventoryCSVWrapper.class).stream().filter(
                        inventoryCSVWrapper -> validateInventoryCSVWrapperForReceiving(inventoryCSVWrapper)
                ).collect(Collectors.toList());

        receivingInventoryFileUploadProgress.put(fileUploadProgressKey, 5.0);


        new Thread(() -> {
            int totalInventoryCount = inventoryCSVWrappers.size();
            int index = 0;
            for (InventoryCSVWrapper inventoryCSVWrapper : inventoryCSVWrappers) {
                receivingInventoryFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalInventoryCount) * (index));
                try {
                    // let's get the receipt and reciept line
                    // make sure all the necessary data exists
                    logger.debug("start to process inventory {}", inventoryCSVWrapper);
                    Item item = inventoryServiceRestemplateClient.getItemByName(warehouseId,
                            inventoryCSVWrapper.getItem());
                    receivingInventoryFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalInventoryCount) * (index + 0.2));
                    if (Objects.isNull(item)) {
                        // skip the item if the name is wrong
                        logger.debug("can't find item by name {}, skip current line",
                                inventoryCSVWrapper.getItem());
                        throw ReceiptOperationException.raiseException(
                                "can't find item by name " + inventoryCSVWrapper.getItem()
                                        + ", skip current line");
                    }
                    logger.debug("got item {} by name {}", item.getId(), item.getName());
                    Receipt receipt = findByNumber(warehouseId, inventoryCSVWrapper.getReceipt());
                    receivingInventoryFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalInventoryCount) * (index + 0.4));

                    logger.debug("got receipt by number {}", receipt.getNumber());
                    // get the first matched line that has enough open quantity
                    Optional<ReceiptLine> matchedReceiptLineOptional = receipt.getReceiptLines().stream().filter(
                            receiptLine ->
                                    receiptLine.getItem().getName().equalsIgnoreCase(inventoryCSVWrapper.getItem()) &&
                                            item.getId().equals(receiptLine.getItemId()) &&
                                            receiptLineService.getOpenQuantity(receiptLine) > 0
                    ).sorted(Comparator.comparing(a -> receiptLineService.getOpenQuantity(a)))
                            .findFirst();
                    if (matchedReceiptLineOptional.isEmpty()) {
                        logger.debug("can't find an open line from receipt {} for item {} with quantity",
                                receipt.getNumber(),
                                inventoryCSVWrapper.getItem(),
                                inventoryCSVWrapper.getQuantity());
                        throw ReceiptOperationException.raiseException(
                                "can't find an open line from receipt " + receipt.getNumber()
                                        + " for item " + inventoryCSVWrapper.getItem()
                                        + " with quantity " + inventoryCSVWrapper.getQuantity());
                    }

                    receivingInventoryFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalInventoryCount) * (index + 0.6));

                    ReceiptLine matchedReceiptLine = matchedReceiptLineOptional.get();
                    logger.debug("we found the matched line, number is {}",
                            matchedReceiptLine.getNumber());

                    Inventory inventory = convertFromWrapper(warehouseId, inventoryCSVWrapper,
                            receipt, matchedReceiptLine);

                    if (Objects.isNull(inventory.getLocation())) {
                        throw ReceiptOperationException.raiseException("Can't get the location for the inventory. " +
                                "if receive into the receipt, make sure that the receipt is already check in");
                    }
                    receivingInventoryFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalInventoryCount) * (index + 0.75));
                    logger.debug("created inventory from the csv line, will start to receive against this inventory" +
                                    " ================           Inventory ================\nlpn： {} , qty: {}, location: {}",
                            inventory.getLpn(),
                            inventory.getQuantity(),
                            inventory.getLocation().getName());
                    receiptLineService.receive(receipt.getId(), matchedReceiptLine.getId(), inventory);
                    // we complete this inventory
                    logger.debug("Inventory received, continue with next line");
                    receivingInventoryFileUploadProgress.put(fileUploadProgressKey, 10.0 + (90.0 / totalInventoryCount) * (index + 1));

                    List<FileUploadResult> fileUploadResults = receivingInventoryFileUploadResult.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            inventoryCSVWrapper.toString(),
                            "success", ""
                    ));
                    receivingInventoryFileUploadResult.put(fileUploadProgressKey, fileUploadResults);

                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process receiving inventory upload file record: {}, \n error message: {}",
                            inventoryCSVWrapper,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = receivingInventoryFileUploadResult.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            inventoryCSVWrapper.toString(),
                            "fail", ex.getMessage()
                    ));
                    receivingInventoryFileUploadResult.put(fileUploadProgressKey, fileUploadResults);
                }
                finally {

                    index++;
                }

            }

            logger.debug("All lines are processed");
            receivingInventoryFileUploadProgress.put(fileUploadProgressKey, 100.0);
        }).start();

        return fileUploadProgressKey;


    }

    private boolean validateInventoryCSVWrapperForReceiving(InventoryCSVWrapper inventoryCSVWrapper) {
        if (Strings.isBlank(inventoryCSVWrapper.getReceipt())) {
            return false;
        }
        if (Strings.isBlank(inventoryCSVWrapper.getItem())) {
            return false;
        }
        if (Objects.isNull(inventoryCSVWrapper.getQuantity())) {
            return false;
        }

        // optional field
        // 1. lpn: get the next available number if it is not passed in
        // 2. location: receive into receipt if it is not passed in
        // 3. itemPackageType: get the default item package type if it is not passed in
        // 4. inventoryStatus: get the available inventory status
        // 5. color
        // 6. productSize
        // 7. style
        return true;
    }

    private Inventory convertFromWrapper(Long warehouseId,
                                         InventoryCSVWrapper inventoryCSVWrapper,
                                         Receipt receipt,
                                         ReceiptLine receiptLine) {
        Inventory inventory = new Inventory();
        if (Strings.isNotBlank(inventoryCSVWrapper.getLpn())) {
            inventory.setLpn(inventoryCSVWrapper.getLpn());
        }
        else {
            // if inventory is not passed in, let's get
            // the automatically generated one
            inventory.setLpn(
                    commonServiceRestemplateClient.getNextNumber(warehouseId,
                            "lpn")
            );

        }

        inventory.setVirtual(false);

        inventory.setColor(inventoryCSVWrapper.getColor());
        inventory.setProductSize(inventoryCSVWrapper.getProductSize());
        inventory.setStyle(inventoryCSVWrapper.getStyle());

        inventory.setWarehouseId(warehouseId);

        // client
        if (Objects.nonNull(receipt.getClientId())) {
            inventory.setClientId(receipt.getClientId());
        }

        // item
        inventory.setItem(receiptLine.getItem());

        // itemPackageType
        // if the item package type is passed in, use it
        // otherwise, use the default item package type
        ItemPackageType itemPackageType = null;
        if (Strings.isNotBlank(inventoryCSVWrapper.getItemPackageType())) {
            itemPackageType = receiptLine.getItem().getItemPackageTypes()
                    .stream().filter(
                            existingItemPackageType -> existingItemPackageType.getName().equalsIgnoreCase(
                                    inventoryCSVWrapper.getItemPackageType()
                            )
                    ).findFirst().orElse(null);

        }
        else {

            // item package type is not passed in, let's
            // get the default item package type from the item
            itemPackageType = receiptLine.getItem().getDefaultItemPackageType();
        }
        if (Objects.nonNull(itemPackageType)) {
            inventory.setItemPackageType(itemPackageType);
        }

        int unitOfMeasureQuantity = 1;
        if (Strings.isNotBlank(inventoryCSVWrapper.getUnitOfMeasure())) {
            unitOfMeasureQuantity = inventory.getItemPackageType().getItemUnitOfMeasures()
                    .stream().filter(itemUnitOfMeasure ->
                            itemUnitOfMeasure.getUnitOfMeasure().getName().equalsIgnoreCase(
                                    inventoryCSVWrapper.getUnitOfMeasure()
                            ))
                    .map(itemUnitOfMeasure -> itemUnitOfMeasure.getQuantity())
                    .findFirst().orElse(1);
        }
        inventory.setQuantity(inventoryCSVWrapper.getQuantity() * unitOfMeasureQuantity);


        // inventoryStatus
        InventoryStatus inventoryStatus = null;
        if (Strings.isNotBlank(inventoryCSVWrapper.getInventoryStatus())) {
            inventoryStatus = inventoryServiceRestemplateClient.getInventoryStatusByName(
                    warehouseId, inventoryCSVWrapper.getInventoryStatus()
            );
        }
        else {
            logger.debug("will set inventory status: {} / {}",
                    warehouseId,
                    inventoryCSVWrapper.getInventoryStatus());
            inventoryStatus =
                    inventoryServiceRestemplateClient.getAvailableInventoryStatus(
                    warehouseId );
        }
        if (Objects.nonNull(inventoryStatus)) {
            inventory.setInventoryStatus(inventoryStatus);
        }

        // location
        Location location = null;
        if (Strings.isNotBlank(inventoryCSVWrapper.getLocation())) {

            location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                    warehouseId, inventoryCSVWrapper.getLocation()
            );
        }
        else {
            location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                    warehouseId, receipt.getNumber()
            );
        }
        if (Objects.nonNull(location)) {

            inventory.setLocationId(location.getId());
            inventory.setLocation(location);
        }

        return inventory;

    }

    public double getReceiptFileUploadProgress(String key) {
        return receiptFileUploadProgress.getOrDefault(key, 100.0);
    }

    public List<FileUploadResult> getReceiptFileUploadResult(Long warehouseId, String key) {
        return receiptFileUploadResult.getOrDefault(key, new ArrayList<>());
    }


    public double getReceivingInventoryFileUploadProgress(String key) {
        return receivingInventoryFileUploadProgress.getOrDefault(key, 100.0);
    }

    public List<FileUploadResult> getReceivingInventoryFileUploadResult(Long warehouseId, String key) {
        return receivingInventoryFileUploadResult.getOrDefault(key, new ArrayList<>());
    }


    private void clearReceiptFileUploadMap() {

        if (receiptFileUploadProgress.size() > INVENTORY_FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = receiptFileUploadProgress.keySet().iterator();
            while(iterator.hasNext()) {
                String key = iterator.next();
                // key should be in the format of
                // warehouseId + "-" + username + "-" + System.currentTimeMillis()
                long lastTimeMillis = Long.parseLong(key.substring(key.lastIndexOf("-")));
                // check the different between current time stamp and the time stamp of when
                // the record is generated
                if (System.currentTimeMillis() - lastTimeMillis > 60 * 60 * 1000) {
                    iterator.remove();
                }
            }
        }

        if (receiptFileUploadResult.size() > INVENTORY_FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = receiptFileUploadResult.keySet().iterator();
            while(iterator.hasNext()) {
                String key = iterator.next();
                // key should be in the format of
                // warehouseId + "-" + username + "-" + System.currentTimeMillis()
                long lastTimeMillis = Long.parseLong(key.substring(key.lastIndexOf("-")));
                // check the different between current time stamp and the time stamp of when
                // the record is generated
                if (System.currentTimeMillis() - lastTimeMillis > 60 * 60 * 1000) {
                    iterator.remove();
                }
            }
        }
    }

    private void clearRecevingInventoryFileUploadMap() {

        if (receivingInventoryFileUploadProgress.size() > INVENTORY_FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = receivingInventoryFileUploadProgress.keySet().iterator();
            while(iterator.hasNext()) {
                String key = iterator.next();
                // key should be in the format of
                // warehouseId + "-" + username + "-" + System.currentTimeMillis()
                long lastTimeMillis = Long.parseLong(key.substring(key.lastIndexOf("-")));
                // check the different between current time stamp and the time stamp of when
                // the record is generated
                if (System.currentTimeMillis() - lastTimeMillis > 60 * 60 * 1000) {
                    iterator.remove();
                }
            }
        }

        if (receivingInventoryFileUploadResult.size() > INVENTORY_FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = receivingInventoryFileUploadResult.keySet().iterator();
            while(iterator.hasNext()) {
                String key = iterator.next();
                // key should be in the format of
                // warehouseId + "-" + username + "-" + System.currentTimeMillis()
                long lastTimeMillis = Long.parseLong(key.substring(key.lastIndexOf("-")));
                // check the different between current time stamp and the time stamp of when
                // the record is generated
                if (System.currentTimeMillis() - lastTimeMillis > 60 * 60 * 1000) {
                    iterator.remove();
                }
            }
        }
    }
}
