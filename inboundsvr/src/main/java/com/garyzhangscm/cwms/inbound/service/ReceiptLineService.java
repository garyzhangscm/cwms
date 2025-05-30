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

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.inbound.clients.AdminServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.exception.GenericException;
import com.garyzhangscm.cwms.inbound.exception.MissingInformationException;
import com.garyzhangscm.cwms.inbound.exception.ReceiptOperationException;
import com.garyzhangscm.cwms.inbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.repository.ReceiptLineRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReceiptLineService {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptLineService.class);

    @Autowired
    private ReceiptLineRepository receiptLineRepository;
    @Autowired
    private ReceivingTransactionService receivingTransactionService;
    @Autowired
    private UserService userService;

    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private InboundQCConfigurationService inboundQCConfigurationService;
    @Autowired
    private InboundReceivingConfigurationService inboundReceivingConfigurationService;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Autowired
    private  BillableActivityService billableActivityService;
    @Autowired
    private AdminServiceRestemplateClient adminServiceRestemplateClient;

    @Value("${fileupload.test-data.receipt_lines:receipt_lines}")
    String testDataFile;

    public ReceiptLine findById(Long id) {
        return findById(id, true);
    }

    public ReceiptLine findById(Long id, boolean includeDetails) {
        ReceiptLine receiptLine = receiptLineRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("receipt line not found by id: " + id));
        if (includeDetails) {
            loadReceiptLineAttribute(receiptLine);
        }
        return receiptLine;
    }
    public List<ReceiptLine> findAll() {
        return findAll(true);
    }
    public List<ReceiptLine> findAll(boolean includeDetails) {


        List<ReceiptLine> receiptLines = receiptLineRepository.findAll();
        if (receiptLines.size() > 0 && includeDetails) {
            loadReceiptLineAttribute(receiptLines);
        }
        return receiptLines;
    }

    public ReceiptLine findByNaturalKey(Long warehouseId, Long receiptId, String number) {
        return receiptLineRepository.findByNaturalKey(warehouseId, receiptId, number);

    }


    public void loadReceiptLineAttribute(List<ReceiptLine> receiptLines) {
        for(ReceiptLine receiptLine : receiptLines) {
            loadReceiptLineAttribute(receiptLine);
        }
    }

    public void loadReceiptLineAttribute(ReceiptLine receiptLine) {

        if (receiptLine.getWarehouseId() != null && receiptLine.getWarehouse() == null) {
            receiptLine.setWarehouse(warehouseLayoutServiceRestemplateClient.getWarehouseById(receiptLine.getWarehouseId()));

        }
        // Load Item information
        if (receiptLine.getItemId() != null && receiptLine.getItem() == null) {
            receiptLine.setItem(inventoryServiceRestemplateClient.getItemById(receiptLine.getItemId()));

        }

        // Load Item information
        if (receiptLine.getItemPackageTypeId() != null && receiptLine.getItemPackageType() == null) {
            ItemPackageType itemPackageType = receiptLine.getItem().getItemPackageTypes().stream().filter(
                    existingItemPackageType -> existingItemPackageType.getId().equals(
                            receiptLine.getItemPackageTypeId()
                    )
            ).findFirst().orElse(null);
            receiptLine.setItemPackageType(itemPackageType);

        }

        // load the receipt number
        if (Objects.nonNull(receiptLine.getReceipt())) {
            receiptLine.setReceiptNumber(receiptLine.getReceipt().getNumber());
            receiptLine.setReceiptId(receiptLine.getReceipt().getId());
        }

        receiptLine.getReceiptLineBillableActivities().forEach(
                receiptLineBillableActivity -> {
                    if (Objects.nonNull(receiptLineBillableActivity.getBillableActivityTypeId()) &&
                            Objects.isNull(receiptLineBillableActivity.getBillableActivityType())) {

                        receiptLineBillableActivity.setBillableActivityType(
                                adminServiceRestemplateClient.getBillableActivityTypeById(
                                        receiptLineBillableActivity.getBillableActivityTypeId()
                                )
                        );
                    }

                }
        );

    }

    @Transactional
    public ReceiptLine save(ReceiptLine receiptLine) {
        return receiptLineRepository.save(receiptLine);
    }

    @Transactional
    public ReceiptLine saveOrUpdate(ReceiptLine receiptLine) {

        if (receiptLine.getId() == null &&
                findByNaturalKey(receiptLine.getWarehouseId(), receiptLine.getReceipt().getId(), receiptLine.getNumber()) != null) {
            receiptLine.setId(
                    findByNaturalKey(receiptLine.getWarehouseId(),receiptLine.getReceipt().getId(), receiptLine.getNumber()).getId());
        }
        if (Objects.isNull(receiptLine.getId())) {
            // we are creating a new receipt line,
            // let's setup the qc quantity
            setupQCQuantity(
                    receiptLine.getReceipt(),
                    receiptLine
            );
        }
        return save(receiptLine);
    }

    @Transactional
    public void delete(ReceiptLine receiptLine) {
        receiptLineRepository.delete(receiptLine);
    }
    @Transactional
    public void delete(Long id) {
        receiptLineRepository.deleteById(id);
    }
    @Transactional
    public void delete(String receiptLineIds) {
        if (!receiptLineIds.isEmpty()) {
            long[] receiptLineIdArray = Arrays.asList(receiptLineIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : receiptLineIdArray) {
                delete(id);
            }
        }
    }

    public List<ReceiptLineCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("receipt").
                addColumn("number").
                addColumn("item").
                addColumn("expectedQuantity").
                addColumn("receivedQuantity").
                addColumn("overReceivingQuantity").
                addColumn("overReceivingPercent").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ReceiptLineCSVWrapper.class);
    }

    /**
    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ReceiptLineCSVWrapper> receiptLineCSVWrappers = loadData(inputStream);
            receiptLineCSVWrappers.stream().forEach(receiptLineCSVWrapper -> saveOrUpdate(convertFromWrapper(receiptLineCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
     **/

    private ReceiptLine convertFromWrapper(Long warehouseId,
                                           Receipt receipt,
                                           ReceiptLineCSVWrapper receiptLineCSVWrapper) {

        ReceiptLine receiptLine = new ReceiptLine();
        receiptLine.setNumber(receiptLineCSVWrapper.getLine());
        receiptLine.setReceivedQuantity(0L);


        receiptLine.setOverReceivingQuantity(
                Objects.isNull(receiptLineCSVWrapper.getOverReceivingQuantity()) ?
                0l :
                receiptLineCSVWrapper.getOverReceivingQuantity());
        receiptLine.setOverReceivingPercent(
                Objects.isNull(receiptLineCSVWrapper.getOverReceivingPercent())?
                0.0d :
                receiptLineCSVWrapper.getOverReceivingPercent());


        // Warehouse is mandate
        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId);

        receiptLine.setWarehouseId(warehouse.getId());

        logger.debug("Start to create receipt line {} with item {}, in receipt {} / warehouse id: {}",
                receiptLineCSVWrapper.getLine(),
                receiptLineCSVWrapper.getItem(), receiptLineCSVWrapper.getReceipt(),
                warehouse.getId());
        if (Objects.isNull(receipt) && Strings.isNotBlank(receiptLineCSVWrapper.getReceipt())) {
            receipt = receiptService.findByNumber(warehouse.getId(),
                    receipt.getClientId(), receiptLineCSVWrapper.getReceipt());
        }
        if (Objects.nonNull(receipt)) {
            receiptLine.setReceipt(receipt);
        }

        if (!StringUtils.isBlank(receiptLineCSVWrapper.getItem())) {
            Item item =
                    inventoryServiceRestemplateClient.getItemByName(warehouse.getId(), receipt.getClientId(),  receiptLineCSVWrapper.getItem());
            receiptLine.setItemId(item.getId());


            int unitOfMeasureQuantity = 1;
            if (Strings.isNotBlank(receiptLineCSVWrapper.getUnitOfMeasure()) &&
                Objects.nonNull(item)) {
                unitOfMeasureQuantity = item.getDefaultItemPackageType().getItemUnitOfMeasures()
                        .stream().filter(itemUnitOfMeasure ->
                                itemUnitOfMeasure.getUnitOfMeasure().getName().equalsIgnoreCase(
                                        receiptLineCSVWrapper.getUnitOfMeasure()
                                ))
                        .map(itemUnitOfMeasure -> itemUnitOfMeasure.getQuantity())
                        .findFirst().orElse(1);
            }
            receiptLine.setExpectedQuantity(receiptLineCSVWrapper.getExpectedQuantity() * unitOfMeasureQuantity);
            receiptLine.setArrivedQuantity(receiptLine.getExpectedQuantity());
        }

        InventoryStatus inventoryStatus;
        if (Strings.isBlank(receiptLineCSVWrapper.getInventoryStatus())) {
            inventoryStatus = inventoryServiceRestemplateClient.getAvailableInventoryStatus(
                    warehouseId
            );
            if (Objects.isNull(inventoryStatus)) {
                throw MissingInformationException.raiseException("Can't find the default available inventory." +
                        "please specify the inventory status, or configure at the warehouse");
            }
        }
        else {
            inventoryStatus = inventoryServiceRestemplateClient.getInventoryStatusByName(
                    warehouseId, receiptLineCSVWrapper.getInventoryStatus()
            );
            if (Objects.isNull(inventoryStatus)) {
                throw MissingInformationException.raiseException("Can't find inventory status by name " +
                        receiptLineCSVWrapper.getInventoryStatus());
            }
        }
        receiptLine.setInventoryStatusId(inventoryStatus.getId());

        receiptLine.setColor(receiptLineCSVWrapper.getColor());
        receiptLine.setProductSize(receiptLineCSVWrapper.getProductSize());
        receiptLine.setStyle(receiptLineCSVWrapper.getStyle());

        receiptLine.setInventoryAttribute1(receiptLineCSVWrapper.getInventoryAttribute1());
        receiptLine.setInventoryAttribute2(receiptLineCSVWrapper.getInventoryAttribute2());
        receiptLine.setInventoryAttribute3(receiptLineCSVWrapper.getInventoryAttribute3());
        receiptLine.setInventoryAttribute4(receiptLineCSVWrapper.getInventoryAttribute4());
        receiptLine.setInventoryAttribute5(receiptLineCSVWrapper.getInventoryAttribute5());
        return receiptLine;
    }

    @Transactional
    public ReceiptLine addReceiptLine(Long receiptId, ReceiptLine receiptLine) {
        Receipt receipt = receiptService.findById(receiptId);
        receiptLine.setReceipt(receipt);
        receiptLine.setWarehouseId(receipt.getWarehouseId());
        if (receiptLine.getItemId() == null && receiptLine.getItem() != null) {
            receiptLine.setItemId(receiptLine.getItem().getId());
        }
        // default the arrived quantity to the expected quantity
        if (Objects.isNull(receiptLine.getArrivedQuantity())) {
            receiptLine.setArrivedQuantity(receiptLine.getExpectedQuantity());
        }
        return saveOrUpdate(receiptLine);
    }

    @Transactional
    public List<Inventory> receive(Long receiptId, Long receiptLineId,
                             List<Inventory> inventoryList, String rfCode){


        return inventoryList.stream().map(
                inventory -> receive(receiptId, receiptLineId, inventory, rfCode)).collect(Collectors.toList());
    }
    @Transactional
    public Inventory receive(Long receiptId, Long receiptLineId,
                             Inventory inventory, String rfCode) {
        return receive(receiptId, receiptLineId, inventory, false, "", rfCode);
    }
    @Transactional
    public Inventory receive(Long receiptId, Long receiptLineId,
                             Inventory inventory,
                             Boolean receiveToStage,
                             String stageLocation,
                             String rfCode) {
        // Receive inventory and save it on the receipt

        Receipt receipt = receiptService.findById(receiptId);
        ReceiptLine receiptLine = findById(receiptLineId);

        InboundReceivingConfiguration inboundReceivingConfiguration =
                inboundReceivingConfigurationService.getBestMatchedInboundReceivingConfiguration(receipt);

        Boolean validateOverReceivingAgainstArrivedQuantity = Objects.isNull(inboundReceivingConfiguration) ?
                false : inboundReceivingConfiguration.getValidateOverReceivingAgainstArrivedQuantity();
        // setup the in warehouse date for the inventory, if not setup yet
        // we will use the check in time as the inventory's in warehouse date
        // if setup by policy. Otherwise, we will leave it blank and let the
        // inventory service handle it
        if (Objects.isNull(inventory.getInWarehouseDatetime())) {

            if (Objects.nonNull(inboundReceivingConfiguration) &&
                    Boolean.TRUE.equals(inboundReceivingConfiguration.getUseReceiptCheckInTimeAsInWarehouseDateTime()))
            inventory.setInWarehouseDatetime(receipt.getCheckInTime());
        }

        return receive(receipt, receiptLine, inventory, receiveToStage, stageLocation,
                validateOverReceivingAgainstArrivedQuantity, rfCode);

    }

    @Transactional
    public Inventory receive(Receipt receipt,
                             ReceiptLine receiptLine,
                             Inventory inventory,
                             Boolean receiveToStage,
                             String stageLocationName,
                             Boolean validateOverReceivingAgainstArrivedQuantity,
                             String rfCode){
        // Receive inventory and save it on the receipt

        // If the inventory has location passed in, we will directly receive the inventory into
        // the location.
        // Otherwise, we will receive the location on the receipt and let the putaway logic
        // to decide where to put this LPN away

        // Validate if we can receive the inventory
        // 1. over receiving?
        // 3. unexpected item number?
        validateReceiving(receipt, receiptLine, inventory, validateOverReceivingAgainstArrivedQuantity);
        boolean qcRequired = checkQCRequired(receipt, receiptLine, inventory);
        logger.debug("inventory {} received from receipt line {} / {} needs QC? {}",
                inventory.getLpn(), receipt.getNumber(), receiptLine.getNumber(),
                qcRequired);
        if (qcRequired == true) {

            // qc is required, let's get the destination inventory status for QC
            // anc change the inventory automatically
            InboundQCConfiguration bestMatchedInboundQCConfiguration
                    = getBestMatchedInboundQCConfiguration(
                            receipt, receiptLine, inventory
                    );
            InventoryStatus toInventoryStatus =
                    bestMatchedInboundQCConfiguration.getToInventoryStatus();
            if (Objects.isNull(toInventoryStatus)) {
                toInventoryStatus = inventoryServiceRestemplateClient.getInventoryStatusById(
                        bestMatchedInboundQCConfiguration.getToInventoryStatusId()
                );
            }
            inventory.setInventoryStatus(toInventoryStatus);
            inventory.setInboundQCRequired(true);

        }
        else {

            inventory.setInboundQCRequired(false);
        }

        if (inventory.getLocation() == null) {
            Location location =
                    warehouseLayoutServiceRestemplateClient.getLocationByName(
                            receipt.getWarehouseId(), receipt.getNumber());
            inventory.setLocationId(location.getId());
            inventory.setLocation(location);
        }
        /**
        else {
            inventory.setVirtual(inventory.getLocation().getLocationGroup().getLocationGroupType().getVirtual());
        }
         **/

        // inventory should be always actual after it is received
        inventory.setVirtual(false);
        // Everytime when we check in a receipt, we will create a location with the same name so that
        // we can receive inventory on this receipt
        inventory.setReceiptId(receipt.getId());
        inventory.setReceiptLineId(receiptLine.getId());
        inventory.setWarehouseId(receipt.getWarehouseId());

        logger.debug("==========      Will receive inventory===============" +
                        "\n lpn： {} , qty: {}, location: {}",
                inventory.getLpn(), inventory.getQuantity(), inventory.getLocation().getName());

        Inventory newInventory =
                inventoryServiceRestemplateClient.receiveInventory(inventory, receipt.getNumber());
        // Note here when we receive, the inventory may already consolidate with existing inventory
        // in the location and the newInventory may represent the inventory after consolidated.
        // so we need to calculate the received quantity on the line based off the original
        // inventory structure
        receiptLine.setReceivedQuantity(receiptLine.getReceivedQuantity() + inventory.getQuantity());
        if (qcRequired) {
            receiptLine.setQcQuantityRequested(
                    receiptLine.getQcQuantityRequested() +  inventory.getQuantity()
            );
        }
        receiptLine = save(receiptLine);
        receipt.setReceiptStatus(ReceiptStatus.RECEIVING);
        receiptService.saveOrUpdate(receipt);

        newInventory.setInboundQCRequired(qcRequired);


        billableActivityService.sendBillableActivity(
                Objects.nonNull(receipt.getWarehouse()) ?
                    receipt.getWarehouse().getCompanyId() : null,
                receipt.getWarehouseId(),
                receipt.getClientId(),
                inventory.getQuantity(),
                receipt.getNumber(),
                Objects.nonNull(inventory.getItem()) ?
                    inventory.getItem().getName() : null,
                BillableCategory.RECEIVING_CHARGE_BY_QUANTITY);

        if (Boolean.TRUE.equals(receiveToStage)) {
            newInventory = moveReceivedInventoryToStage(newInventory, stageLocationName);
        }
        receivingTransactionService.createReceivingTransaction(receiptLine, newInventory,
                userService.getCurrentUserName(), rfCode);
        return newInventory;
    }

    /**
     * Move newly received inventory into a receiving stage location. If the location name is specified, use the
     * location, otherwise, pick one from the list, with less volume or empty
     * @param newInventory
     * @param stageLocationName
     * @return
     */
    private Inventory moveReceivedInventoryToStage(Inventory newInventory, String stageLocationName) {
        logger.debug("We will need to move the newly received LPN {} automatically to the stage right after receiving",
                newInventory);
        // let's automaticaly move the inventory to the stage location
        Location stageLocation = null;
        if (Strings.isNotBlank(stageLocationName)) {
            stageLocation = warehouseLayoutServiceRestemplateClient.getLocationByName(newInventory.getWarehouseId(),
                    stageLocationName);
        }
        if (Objects.isNull(stageLocation)) {
            List<Location> locations = warehouseLayoutServiceRestemplateClient.getReceivingStageLocations(newInventory.getWarehouseId());
            if (Objects.isNull(locations) || locations.isEmpty()) {
                throw ReceiptOperationException.raiseException("Can't receive into receiving stage as there's none receiving stage location defined in the system");
            }
            stageLocation = locations.get(0);
        }
        try {
            return inventoryServiceRestemplateClient.moveInventory(newInventory, stageLocation);
        } catch (IOException e) {
            e.printStackTrace();
            throw ReceiptOperationException.raiseException("Can't receive into receiving stage location " +
                            stageLocation.getName() + " for inventory " + newInventory.getLpn());
        }

    }

    private boolean checkQCRequired(Receipt receipt, ReceiptLine receiptLine, Inventory inventory) {

        Long qcQuantityNeeded =
                receiptLine.getQcQuantity() > 0 ?
                        receiptLine.getQcQuantity() :
                        (long)(receiptLine.getExpectedQuantity() * receiptLine.getQcPercentage() / 100);
        logger.debug("Receipt line {} / {}, receipt line qc: {} / {}, qc quantity needed? {}, qc quantity requested: {}",
                receipt.getNumber(), receiptLine.getNumber(),
                receiptLine.getQcQuantity(), receiptLine.getQcPercentage(),
                qcQuantityNeeded, receiptLine.getQcQuantityRequested());
        if (receiptLine.getQcQuantityRequested() >= qcQuantityNeeded) {
            return false;
        }

        return Objects.nonNull(
                getBestMatchedInboundQCConfiguration(
                        receipt, receiptLine, inventory
                )
        );

    }

    private InboundQCConfiguration getBestMatchedInboundQCConfiguration(
            Receipt receipt, ReceiptLine receiptLine, Inventory inventory
    ) {

        Warehouse warehouse = receipt.getWarehouse();
        if (Objects.isNull(warehouse)) {
            warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(
                    receipt.getWarehouseId()
            );
        }
        if (Objects.isNull(warehouse)) {
            // we should not arrive here！
            logger.debug("Can't get the QC configuration as we can't get the warehouse" +
                    "information from the receipt");
            logger.debug("=======   Receipt ======= \n {}",
                    receipt);
        }
        return
                inboundQCConfigurationService.getBestMatchedInboundQCConfiguration(
                        receipt.getSupplierId(),
                        Objects.isNull(receiptLine.getItem().getItemFamily()) ? null :
                                receiptLine.getItem().getItemFamily().getId(),
                        receiptLine.getItemId(),
                        inventory.getInventoryStatus().getId(),
                        receipt.getWarehouseId(),
                        warehouse.getCompany().getId()
                );

    }

    // validate whether we can receive inventory against this receipt line
    // 1. over receiving?
    // 3. unexpected item number?
    private void validateReceiving(Receipt receipt, ReceiptLine receiptLine,
                                   Inventory inventory, Boolean validateOverReceivingAgainstArrivedQuantity) {
        // make sure the receipt is already checked in
        if (receipt.getReceiptStatus().equals(ReceiptStatus.OPEN)) {
            throw ReceiptOperationException.raiseException("Please check in the receipt before you can receive");
        }

        // unexpected item number?

        if (!receipt.getAllowUnexpectedItem() &&
             !receiptLine.getItemId().equals(
                     inventory.getItem().getId())) {
            throw ReceiptOperationException.raiseException("Unexpected item not allowed for this receipt");
        }

        // check how many more we can receive against this receipt line
        Long maxOverReceivingQuantityAllowedByQuantity = 0L;
        Long maxOverReceivingQuantityAllowedByPercentage = 0L;
        Long expectedReceivingQuantity =
                Boolean.TRUE.equals(validateOverReceivingAgainstArrivedQuantity) ?
                        receiptLine.getArrivedQuantity() : receiptLine.getExpectedQuantity();

        if (receiptLine.getOverReceivingQuantity() > 0) {
            maxOverReceivingQuantityAllowedByQuantity = receiptLine.getOverReceivingQuantity();
        }



        if (receiptLine.getOverReceivingPercent() > 0) {
            maxOverReceivingQuantityAllowedByPercentage =
                    (long) (expectedReceivingQuantity * receiptLine.getOverReceivingPercent() / 100);
        }
        Long maxOverReceivingQuantityAllowed = Math.max(
                maxOverReceivingQuantityAllowedByQuantity, maxOverReceivingQuantityAllowedByPercentage);

        // See should we receive this inventory, will the quantity maximum the total quantity allowed
        if (receiptLine.getReceivedQuantity() + inventory.getQuantity() >
                expectedReceivingQuantity + maxOverReceivingQuantityAllowed) {
            if (maxOverReceivingQuantityAllowed == 0) {
                // over receiving is not allowed in this receipt line
                throw ReceiptOperationException.raiseException("Over receiving is not allowed");
            }
            if (maxOverReceivingQuantityAllowed > 0) {
                // over receiving is not allowed in this receipt line
                throw ReceiptOperationException.raiseException("Over receiving. The maximum you can receive for this line is " +
                        (receiptLine.getExpectedQuantity() + maxOverReceivingQuantityAllowed - receiptLine.getReceivedQuantity()));
            }
        }


    }

    public ReceiptLine reverseReceivedInventory(Long receiptId, Long receiptLineId, Long quantity,
                                                Boolean inboundQCRequired, Boolean reverseQCQuantity) {
        ReceiptLine receiptLine = findById(receiptLineId);

        receiptLine.setReceivedQuantity(
                receiptLine.getReceivedQuantity() - quantity > 0 ?
                        receiptLine.getReceivedQuantity() - quantity : 0);
        if (Boolean.TRUE.equals(inboundQCRequired) &&
            Boolean.TRUE.equals(reverseQCQuantity)) {
            // we just reserved an inventory that needs QC, let's return
            // the qc quantity back
            receiptLine.setQcQuantityRequested(
                    receiptLine.getQcQuantityRequested() - quantity > 0 ?
                            receiptLine.getQcQuantityRequested() - quantity  : 0);
        }

        return saveOrUpdate(receiptLine);
    }

    /**
     * Setup qc quantity based on
     * 1. supplier
     * 2. item
     * 3. warehouse
     * 4. company
     * from most specific to most generic
     * @param receiptLine
     */
    public void setupQCQuantity(Receipt receipt, ReceiptLine receiptLine) {

        logger.debug("Start to setup qc quantity for receipt line {} / {}",
                receipt.getNumber(), receiptLine.getNumber());
        // default to the qc quantity to 0
        receiptLine.setQcQuantity(0l);
        receiptLine.setQcPercentage(0d);

        Warehouse warehouse = receipt.getWarehouse();
        if (Objects.isNull(warehouse)) {
            warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(
                    receipt.getWarehouseId()
            );
        }
        if (Objects.isNull(warehouse)) {
            // we should not arrive here！
            logger.debug("Can't get the QC configuration as we can't get the warehouse" +
                    "information from the receipt");
            logger.debug("=======   Receipt ======= \n {}",
                    receipt);
            return;
        }
        Item item =
                Objects.nonNull(receiptLine.getItem()) ? receiptLine.getItem() :
                        inventoryServiceRestemplateClient.getItemById(receiptLine.getItemId());

        InboundQCConfiguration inboundQCConfiguration =
                inboundQCConfigurationService.getBestMatchedInboundQCConfiguration(
                        receipt.getSupplierId(),
                        Objects.isNull(item.getItemFamily()) ? null : item.getItemFamily().getId(),
                        receiptLine.getItemId(),
                        receipt.getWarehouseId(),
                        warehouse.getCompany().getId()
                );
        if (Objects.isNull(inboundQCConfiguration)) {
            logger.debug("No inbound qc configuration is defined for the receipt line {} / {}",
                    receipt.getNumber(), receiptLine.getNumber());
            logger.debug("supplier: {} / {}, item {} / {}, warehouse {} / {}, company {} / {}",
                    receipt.getSupplierId(),
                    Objects.isNull(receipt.getSupplier()) ? "" : receipt.getSupplier().getName(),
                    receiptLine.getItemId(),
                    Objects.isNull(receiptLine.getItem()) ? "" : receiptLine.getItem().getName(),
                    warehouse.getId(),
                    warehouse.getName(),
                    Objects.isNull(warehouse.getCompany()) ? "" : warehouse.getCompany().getId(),
                    Objects.isNull(warehouse.getCompany()) ? "" : warehouse.getCompany().getName());
            return;
        }
        // setup the qc quantity and percentage based on the configuration
        if (Objects.nonNull(inboundQCConfiguration.getQcQuantityPerReceipt())) {

            receiptLine.setQcQuantity(inboundQCConfiguration.getQcQuantityPerReceipt());
        }
        if (Objects.nonNull(inboundQCConfiguration.getQcPercentage())) {

            receiptLine.setQcPercentage(inboundQCConfiguration.getQcPercentage());
        }

    }

    /**
     *
     * Recalculate the qc quantity for the receipt line. We can specify the qc quantity and percentage, or let
     * the system run the configuration again to refresh the qc quantity required
     * @param receiptLineId
     * @param receiptLineId
     * @param qcQuantity
     * @param qcPercentage
     * @return
     */
    public ReceiptLine recalculateQCQuantity(Long receiptLineId, Long qcQuantity, Double qcPercentage) {
        ReceiptLine receiptLine = findById(receiptLineId);
        if (Objects.isNull(qcQuantity) && Objects.isNull(qcPercentage)) {
            // the user doesn't specify any field, let's re-run the configuration to get the
            // quantity or percentage

            setupQCQuantity(receiptLine.getReceipt(), receiptLine);
        }
        // if the user specify at least quantity of percentage, then update the field
        // based on the user's input
        else{
            if (Objects.nonNull(qcQuantity)){
                receiptLine.setQcQuantity(qcQuantity);
            }
            if (Objects.nonNull(qcPercentage)){
                receiptLine.setQcPercentage(qcPercentage);
            }
        }

        return saveOrUpdate(receiptLine);
    }

    public List<ReceiptLine> getAvailableReceiptLinesForMPS(Long warehouseId, Long itemId) {
        return receiptLineRepository.findOpenReceiptLinesByItem(itemId);
    }

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {

        logger.debug("start to process item override for receipt line, current warehouse {}, from item id {} to item id {}",
                warehouseId, oldItemId, newItemId);
        receiptLineRepository.processItemOverride(oldItemId, newItemId, warehouseId);
    }

    public void saveReceiptLineData(Long warehouseId, Receipt receipt, ReceiptLineCSVWrapper receiptLineCSVWrapper) {

        saveOrUpdate(
                convertFromWrapper(warehouseId, receipt, receiptLineCSVWrapper)
        );
    }

    public Long getOpenQuantity(ReceiptLine receiptLine) {
        Long overReceivingQuantityAllowed =
                Objects.isNull(receiptLine.getOverReceivingQuantity()) ?
                        0l : receiptLine.getOverReceivingQuantity();
        if (Objects.nonNull(receiptLine.getOverReceivingPercent()) &&
            receiptLine.getExpectedQuantity() * receiptLine.getOverReceivingPercent() * 1.0/ 100 > overReceivingQuantityAllowed) {
            overReceivingQuantityAllowed = (long)(receiptLine.getExpectedQuantity() * receiptLine.getOverReceivingPercent() * 1.0/ 100);
        }
        return (receiptLine.getExpectedQuantity() + overReceivingQuantityAllowed - receiptLine.getReceivedQuantity()) > 0 ?
                receiptLine.getExpectedQuantity() + overReceivingQuantityAllowed - receiptLine.getReceivedQuantity() : 0;
    }

    public ReceiptLine changeReceiptLine(Long receiptId, Long receiptLineId, ReceiptLine receiptLine) {
        ReceiptLine exisitingReceiptLine = findById(receiptLineId);
        // we will only allow the user to change the
        if (Objects.nonNull(receiptLine.getCubicMeter())) {
            exisitingReceiptLine.setCubicMeter(receiptLine.getCubicMeter());
        }
        if (Objects.nonNull(receiptLine.getExpectedQuantity())) {
            exisitingReceiptLine.setExpectedQuantity(receiptLine.getExpectedQuantity());
        }
        if (Objects.nonNull(receiptLine.getArrivedQuantity())) {
            exisitingReceiptLine.setArrivedQuantity(receiptLine.getArrivedQuantity());
        }
        if (Objects.nonNull(receiptLine.getOverReceivingPercent())) {
            exisitingReceiptLine.setOverReceivingPercent(receiptLine.getOverReceivingPercent());
        }
        if (Objects.nonNull(receiptLine.getOverReceivingQuantity())) {
            exisitingReceiptLine.setOverReceivingQuantity(receiptLine.getOverReceivingQuantity());
        }
        if (Objects.nonNull(receiptLine.getQcQuantity())) {
            exisitingReceiptLine.setQcQuantity(receiptLine.getQcQuantity());
        }
        if (Objects.nonNull(receiptLine.getQcPercentage())) {
            exisitingReceiptLine.setQcPercentage(receiptLine.getQcPercentage());
        }
        if (Objects.nonNull(receiptLine.getColor())) {
            exisitingReceiptLine.setColor(receiptLine.getColor());
        }
        if (Objects.nonNull(receiptLine.getStyle())) {
            exisitingReceiptLine.setStyle(receiptLine.getStyle());
        }
        if (Objects.nonNull(receiptLine.getProductSize())) {
            exisitingReceiptLine.setProductSize(receiptLine.getProductSize());
        }
        if (Objects.nonNull(receiptLine.getInventoryAttribute1())) {
            exisitingReceiptLine.setInventoryAttribute1(receiptLine.getInventoryAttribute1());
        }
        if (Objects.nonNull(receiptLine.getInventoryAttribute2())) {
            exisitingReceiptLine.setInventoryAttribute2(receiptLine.getInventoryAttribute2());
        }
        if (Objects.nonNull(receiptLine.getInventoryAttribute3())) {
            exisitingReceiptLine.setInventoryAttribute3(receiptLine.getInventoryAttribute3());
        }
        if (Objects.nonNull(receiptLine.getInventoryAttribute4())) {
            exisitingReceiptLine.setInventoryAttribute4(receiptLine.getInventoryAttribute4());
        }
        if (Objects.nonNull(receiptLine.getInventoryAttribute5())) {
            exisitingReceiptLine.setInventoryAttribute5(receiptLine.getInventoryAttribute5());
        }

        if (Objects.nonNull(receiptLine.getInventoryStatusId())) {
            exisitingReceiptLine.setInventoryStatusId(receiptLine.getInventoryStatusId());
        }

        return saveOrUpdate(exisitingReceiptLine);
    }
}
