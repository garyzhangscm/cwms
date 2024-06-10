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

import com.garyzhangscm.cwms.inbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.exception.MissingInformationException;
import com.garyzhangscm.cwms.inbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inbound.model.*;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EulogiaService {
    private static final Logger logger = LoggerFactory.getLogger(EulogiaService.class);


    @Autowired
    private FileService fileService;
    @Autowired
    private UserService userService;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private ReceiptLineService receiptLineService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private ReceiptService receiptService;

    private final static int CUSTOMER_PACKING_SLIP_FILE_UPLOAD_MAP_SIZE_THRESHOLD = 20;
    private Map<String, Double> customerPackingSlipFileUploadProgress = new ConcurrentHashMap<>();
    private Map<String, List<FileUploadResult>> customerPackingSlipFileUploadResult = new ConcurrentHashMap<>();

    private List<EulogiaCustomerPackingSlipCSVWrapper> loadDataWithLine(File file)  {
        return fileService.loadData(file, EulogiaCustomerPackingSlipCSVWrapper.class);
    }

    public double getCustomerPackingSlipFileUploadProgress(String key) {
        return customerPackingSlipFileUploadProgress.getOrDefault(key, 100.0);
    }

    public List<FileUploadResult> getCustomerPackingSlipFileUploadResult(Long warehouseId, String key) {
        return customerPackingSlipFileUploadResult.getOrDefault(key, new ArrayList<>());
    }

    private void clearCustomerPackingSlipFileUploadMap() {

        if (customerPackingSlipFileUploadProgress.size() > CUSTOMER_PACKING_SLIP_FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = customerPackingSlipFileUploadProgress.keySet().iterator();
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

        if (customerPackingSlipFileUploadResult.size() > CUSTOMER_PACKING_SLIP_FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = customerPackingSlipFileUploadResult.keySet().iterator();
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

    public String saveCustomerPackingSlipData(Long warehouseId, File localFile) throws IOException {

        String username = userService.getCurrentUserName();

        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();

        clearCustomerPackingSlipFileUploadMap();
        customerPackingSlipFileUploadProgress.put(fileUploadProgressKey, 0.0);
        customerPackingSlipFileUploadResult.put(fileUploadProgressKey, new ArrayList<>());

        List<EulogiaCustomerPackingSlipCSVWrapper> eulogiaCustomerPackingSlipCSVWrappers = loadDataWithLine(localFile);
        eulogiaCustomerPackingSlipCSVWrappers.forEach(
                eulogiaCustomerPackingSlipCSVWrapper ->
                        eulogiaCustomerPackingSlipCSVWrapper.trim()
        );
        logger.debug("start to save {} eulogiaCustomerPackingSlipCSVWrapper ", eulogiaCustomerPackingSlipCSVWrappers.size());

        customerPackingSlipFileUploadProgress.put(fileUploadProgressKey, 10.0);
        Long companyId = warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId).getCompanyId();

        new Thread(() -> {

            int totalCustomerPackingSlipLineCount = eulogiaCustomerPackingSlipCSVWrappers.size();
            int index = 0;
            // see if we need to create order
            for (EulogiaCustomerPackingSlipCSVWrapper eulogiaCustomerPackingSlipCSVWrapper : eulogiaCustomerPackingSlipCSVWrappers) {
                try {

                    customerPackingSlipFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCustomerPackingSlipLineCount) * (index));
                    Client client = null;
                    if (Strings.isNotBlank(eulogiaCustomerPackingSlipCSVWrapper.getClient())) {
                        client = commonServiceRestemplateClient.getClientByName(warehouseId,
                                eulogiaCustomerPackingSlipCSVWrapper.getClient());
                    }
                    setupMissingValue(eulogiaCustomerPackingSlipCSVWrapper);
                    
                    // create item and its unit of measure on the fly
                    Item item = getItemForUploadingCustomerPackingSlip(
                            companyId, warehouseId,
                            Objects.isNull(client)? null :  client.getId(),
                            eulogiaCustomerPackingSlipCSVWrapper);

                    // after we create / get the item, let's setup the receipt line
                    customerPackingSlipFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCustomerPackingSlipLineCount) * (index + 0.25));


                    Receipt receipt = receiptService.findByNumber(warehouseId,
                            Objects.isNull(client) ? null : client.getId(),
                            eulogiaCustomerPackingSlipCSVWrapper.getReceipt());

                    if (Objects.isNull(receipt)) {
                        logger.debug("receipt {} is not created yet, let's create the order on the fly ", eulogiaCustomerPackingSlipCSVWrapper.getReceipt());
                        // save the username for the receipt
                        // we have to do it manually since the user name is only available in the main http session
                        // but we will create the receipt / receipt line in a separate transaction
                        receipt = getReceiptFromCustomerPackingSlipCSVWrapper(warehouseId,
                                Objects.isNull(client)? null :  client.getId(),eulogiaCustomerPackingSlipCSVWrapper);
                        receipt.setCreatedBy(username);
                        receipt = receiptService.saveOrUpdate(receipt);
                    }
                    customerPackingSlipFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCustomerPackingSlipLineCount) * (index + 0.5));

                    logger.debug("start to create receipt line for item {}, carton quantity {}, for receipt {}",
                            item.getName(),
                            eulogiaCustomerPackingSlipCSVWrapper.getCartonQuantity(),
                            eulogiaCustomerPackingSlipCSVWrapper.getReceipt());

                    receiptLineService.saveOrUpdate(
                            getReceiptLineFromCustomerPackingSlipCSVWrapper(warehouseId,
                                receipt, item, eulogiaCustomerPackingSlipCSVWrapper));



                    List<FileUploadResult> fileUploadResults = customerPackingSlipFileUploadResult.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            eulogiaCustomerPackingSlipCSVWrapper.toString(),
                            "success", ""
                    ));
                    customerPackingSlipFileUploadResult.put(fileUploadProgressKey, fileUploadResults);

                    customerPackingSlipFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCustomerPackingSlipLineCount) * (index + 1));
                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process eulogia customer package slip line upload file record: {}, \n error message: {}",
                            eulogiaCustomerPackingSlipCSVWrapper,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = customerPackingSlipFileUploadResult.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            eulogiaCustomerPackingSlipCSVWrapper.toString(),
                            "fail", ex.getMessage()
                    ));
                    customerPackingSlipFileUploadResult.put(fileUploadProgressKey, fileUploadResults);
                }
                finally {

                    index++;
                }
            }
            // after we process all lines, mark the progress to 100%
            customerPackingSlipFileUploadProgress.put(fileUploadProgressKey, 100.0);

        }).start();
        return fileUploadProgressKey;

    }

    private Receipt getReceiptFromCustomerPackingSlipCSVWrapper(Long warehouseId,
                                                                Long clientId,
                                                                EulogiaCustomerPackingSlipCSVWrapper eulogiaCustomerPackingSlipCSVWrapper) {
        Receipt receipt = new Receipt();
        receipt.setNumber(eulogiaCustomerPackingSlipCSVWrapper.getReceipt());
        receipt.setReceiptStatus(ReceiptStatus.OPEN);


        receipt.setAllowUnexpectedItem(false);

        receipt.setWarehouseId(warehouseId);
        receipt.setClientId(clientId);

        if (!StringUtils.isBlank(eulogiaCustomerPackingSlipCSVWrapper.getSupplier())) {
            Supplier supplier = commonServiceRestemplateClient.getSupplierByName(
                    warehouseId, eulogiaCustomerPackingSlipCSVWrapper.getSupplier());
            receipt.setSupplierId(supplier.getId());
        }
        return receipt;
    }


    private ReceiptLine getReceiptLineFromCustomerPackingSlipCSVWrapper(Long warehouseId,
                                                                        Receipt receipt,
                                                                        Item item,
                                                                        EulogiaCustomerPackingSlipCSVWrapper eulogiaCustomerPackingSlipCSVWrapper) {

        ReceiptLine receiptLine = new ReceiptLine();
        // get the next line number
        Long maxLineNumber = 0l;
        for (ReceiptLine existingReceiptLine : receipt.getReceiptLines()) {
            try{
                Long lineNumber = Long.parseLong(existingReceiptLine.getNumber());
                maxLineNumber = Math.max(lineNumber, maxLineNumber);
            }
            catch (Exception ex) {
                // do nothing
            }
        }
        receiptLine.setNumber(String.valueOf(maxLineNumber + 1));
        receiptLine.setItemId(item.getId());
        receiptLine.setItem(item);

        for (ItemPackageType itemPackageType : item.getItemPackageTypes()) {
            if (itemPackageType.getName().equalsIgnoreCase(eulogiaCustomerPackingSlipCSVWrapper.getItemPackageType())) {
                logger.debug("the receipt's item package type is setup to {} / {}",
                        item.getName(), itemPackageType.getName());
                receiptLine.setItemPackageTypeId(itemPackageType.getId());
            }
        }

        logger.debug("created receipt line with number {}, for eulogiaCustomerPackingSlipCSVWrapper: {}",
                receiptLine.getNumber(), eulogiaCustomerPackingSlipCSVWrapper);

        receiptLine.setReceivedQuantity(0L);


        receiptLine.setOverReceivingQuantity(0l);
        receiptLine.setOverReceivingPercent(0.0d);

        // Warehouse is mandate
        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId);

        receiptLine.setWarehouseId(warehouse.getId());

        logger.debug("Start to create receipt line {} with item {}, in receipt {} / warehouse id: {}",
                receiptLine.getNumber(),
                eulogiaCustomerPackingSlipCSVWrapper.getItem(),
                eulogiaCustomerPackingSlipCSVWrapper.getReceipt(),
                warehouse.getId());


        receiptLine.setReceipt(receipt);

        ItemPackageType itemPackageType = item.getItemPackageTypes().stream().filter(
                existingItemPackageType -> existingItemPackageType.getName().equalsIgnoreCase(eulogiaCustomerPackingSlipCSVWrapper.getItemPackageType())
        ).findFirst().orElse(null);

        if (Objects.isNull(itemPackageType)) {
            throw ResourceNotFoundException.raiseException("Can't find item package type " + eulogiaCustomerPackingSlipCSVWrapper.getItemPackageType() +
                    " for item " + item.getName());
        }

        ItemUnitOfMeasure caseItemUnitOfMeasure = itemPackageType.getItemUnitOfMeasures().stream().filter(
                existingItemUnitOfMeasure -> Boolean.TRUE.equals(existingItemUnitOfMeasure.getCaseFlag())
        ).findFirst().orElse(null);

        if (Objects.isNull(caseItemUnitOfMeasure)) {
            throw ResourceNotFoundException.raiseException("Can't find case UOM for item package type "
                    + eulogiaCustomerPackingSlipCSVWrapper.getItemPackageType() +
                    " of item " + item.getName());
        }

        receiptLine.setExpectedQuantity(eulogiaCustomerPackingSlipCSVWrapper.getCartonQuantity() * caseItemUnitOfMeasure.getQuantity());

        InventoryStatus inventoryStatus = inventoryServiceRestemplateClient.getAvailableInventoryStatus(
                warehouseId
        );
        receiptLine.setInventoryStatusId(inventoryStatus.getId());

        receiptLine.setColor(eulogiaCustomerPackingSlipCSVWrapper.getColor());
        receiptLine.setProductSize(eulogiaCustomerPackingSlipCSVWrapper.getProductSize());
        receiptLine.setStyle(eulogiaCustomerPackingSlipCSVWrapper.getStyle());

        receiptLine.setInventoryAttribute1(eulogiaCustomerPackingSlipCSVWrapper.getInventoryAttribute1());
        receiptLine.setInventoryAttribute2(eulogiaCustomerPackingSlipCSVWrapper.getInventoryAttribute2());
        receiptLine.setInventoryAttribute3(eulogiaCustomerPackingSlipCSVWrapper.getInventoryAttribute3());
        receiptLine.setInventoryAttribute4(eulogiaCustomerPackingSlipCSVWrapper.getInventoryAttribute4());
        receiptLine.setInventoryAttribute5(eulogiaCustomerPackingSlipCSVWrapper.getInventoryAttribute5());

        receiptLine.setCubicMeter(eulogiaCustomerPackingSlipCSVWrapper.getCubicMeter());
        return receiptLine;
    }

    /**
     * When upload the customer packing slip, we will create the item on the fly, if the item doesn't exist yet
     * @param eulogiaCustomerPackingSlipCSVWrapper
     * @return
     */
    private Item getItemForUploadingCustomerPackingSlip(Long companyId,
                                                        Long warehouseId,
                                                        Long clientId,
                                                        EulogiaCustomerPackingSlipCSVWrapper eulogiaCustomerPackingSlipCSVWrapper) {
        logger.debug("see if we already have the item in the system, company id: {}, warehouse id: {}, client id: {}, item: {}",
                companyId, warehouseId, clientId, eulogiaCustomerPackingSlipCSVWrapper.getItem());

        Item item = inventoryServiceRestemplateClient.getItemByName(
                warehouseId, clientId, eulogiaCustomerPackingSlipCSVWrapper.getItem()
        );

        if (Objects.isNull(item)) {
            logger.debug("> item doesn't exists yet, let's create on the fly");
            return createItemForUploadingCustomerPackingSlip(companyId, warehouseId, clientId, eulogiaCustomerPackingSlipCSVWrapper);
        }
        // we get the item, see if the package type already exists in the item
        boolean itemPackageTypeExists = item.getItemPackageTypes().stream().anyMatch(
                itemPackageType -> itemPackageType.getName().equalsIgnoreCase(eulogiaCustomerPackingSlipCSVWrapper.getItemPackageType())
        );

        logger.debug("> item {} exists, does item package type exists? {}",
                eulogiaCustomerPackingSlipCSVWrapper.getItem(), itemPackageTypeExists);
        // the item and item package type exists, let's just return it
        if (itemPackageTypeExists) {
            logger.debug(">> item package type {} exists, we won't need to do anything",
                    eulogiaCustomerPackingSlipCSVWrapper.getItemPackageType());
            return item;
        }
        // the item exists but item package type doesn't exists, let's create the item package type
        logger.debug(">> item package type {} doesn't exists, we will need to create the item package type",
                eulogiaCustomerPackingSlipCSVWrapper.getItemPackageType());
        return changeItemForUploadingCustomerPackingSlip(companyId, warehouseId, clientId, item, eulogiaCustomerPackingSlipCSVWrapper);
    }

    private Item changeItemForUploadingCustomerPackingSlip(Long companyId,
                                                           Long warehouseId,
                                                           Long clientId,
                                                           Item item,
                                                           EulogiaCustomerPackingSlipCSVWrapper eulogiaCustomerPackingSlipCSVWrapper) {
        // add the new item package type to the existing item
        logger.debug("start to create item package type {} for an exists item {}",
                eulogiaCustomerPackingSlipCSVWrapper.getItemPackageType(), item.getName());

        item.getItemPackageTypes().add(
                createItemPackageTypeForUploadingCustomerPackingSlip(
                        companyId, warehouseId, clientId, false,
                        eulogiaCustomerPackingSlipCSVWrapper
                )
        );

        return inventoryServiceRestemplateClient.changeItem(item);
    }
    private Item createItemForUploadingCustomerPackingSlip(Long companyId,
                                                           Long warehouseId,
                                                           Long clientId,
                                                           EulogiaCustomerPackingSlipCSVWrapper eulogiaCustomerPackingSlipCSVWrapper) {


        logger.debug("start to create new item {}", eulogiaCustomerPackingSlipCSVWrapper.getItem());

        Item item = new Item();
        item.setName(eulogiaCustomerPackingSlipCSVWrapper.getItem());
        item.setDescription(eulogiaCustomerPackingSlipCSVWrapper.getItem());
        item.setClientId(clientId);

        item.setItemPackageTypes(List.of(createItemPackageTypeForUploadingCustomerPackingSlip(
                companyId, warehouseId, clientId, true,
                eulogiaCustomerPackingSlipCSVWrapper
        )));

        item.setTrackingColorFlag(true);
        item.setTrackingProductSizeFlag(false);
        item.setTrackingStyleFlag(true);

        item.setTrackingInventoryAttribute1Flag(true);

        item.setCompanyId(companyId);
        item.setWarehouseId(warehouseId);

        return inventoryServiceRestemplateClient.createItem(item);
    }

    public ItemPackageType createItemPackageTypeForUploadingCustomerPackingSlip(Long companyId,
                                                                                Long warehouseId,
                                                                                Long clientId,
                                                                                Boolean defaultItemPackageTypeFlag,
                                                                                EulogiaCustomerPackingSlipCSVWrapper eulogiaCustomerPackingSlipCSVWrapper) {

        logger.debug("start to create new item package type {}", eulogiaCustomerPackingSlipCSVWrapper.getItemPackageType());

        ItemPackageType itemPackageType = new ItemPackageType();
        itemPackageType.setName(eulogiaCustomerPackingSlipCSVWrapper.getItemPackageType());
        itemPackageType.setDescription(eulogiaCustomerPackingSlipCSVWrapper.getItemPackageType());

        itemPackageType.setCompanyId(companyId);
        itemPackageType.setWarehouseId(warehouseId);
        itemPackageType.setClientId(clientId);

        itemPackageType.setItemUnitOfMeasures(
                createUnitOfMeasuresForUploadingCustomerPackingSlip(companyId, warehouseId, eulogiaCustomerPackingSlipCSVWrapper)
        );

        itemPackageType.setDefaultFlag(defaultItemPackageTypeFlag);

        return itemPackageType;

    }

    private List<ItemUnitOfMeasure> createUnitOfMeasuresForUploadingCustomerPackingSlip(Long companyId,
                                                                                        Long warehouseId,
                                                                                        EulogiaCustomerPackingSlipCSVWrapper eulogiaCustomerPackingSlipCSVWrapper) {

        List<ItemUnitOfMeasure> itemUnitOfMeasures = new ArrayList<>();
        // piece
        UnitOfMeasure unitOfMeasure = commonServiceRestemplateClient.getUnitOfMeasureByName(companyId, warehouseId, "PCS");
        if (Objects.isNull(unitOfMeasure)) {
            throw ResourceNotFoundException.raiseException("UOM piece not found");
        }
        itemUnitOfMeasures.add(createUnitOfMeasureForUploadingCustomerPackingSlip(
                companyId, warehouseId, unitOfMeasure.getId(),
                1, 1.0, 1.0, 1.0, 1.0,
                false, false, false, false, false
        ));

        // pack
        unitOfMeasure = commonServiceRestemplateClient.getUnitOfMeasureByName(companyId, warehouseId, "PK");
        if (Objects.isNull(unitOfMeasure)) {
            throw ResourceNotFoundException.raiseException("UOM Pack not found");
        }
        itemUnitOfMeasures.add(createUnitOfMeasureForUploadingCustomerPackingSlip(
                companyId, warehouseId, unitOfMeasure.getId(),
                eulogiaCustomerPackingSlipCSVWrapper.getUnitPerPack(), 1.0, 1.0, 1.0, 1.0,
                false, false, false, false, false
        ));

        // carton
        unitOfMeasure = commonServiceRestemplateClient.getUnitOfMeasureByName(companyId, warehouseId, "CTN");
        if (Objects.isNull(unitOfMeasure)) {
            throw ResourceNotFoundException.raiseException("UOM Carton not found");
        }
        itemUnitOfMeasures.add(createUnitOfMeasureForUploadingCustomerPackingSlip(
                companyId, warehouseId, unitOfMeasure.getId(),
                eulogiaCustomerPackingSlipCSVWrapper.getUnitPerCarton(),
                eulogiaCustomerPackingSlipCSVWrapper.getCartonWeight(),
                eulogiaCustomerPackingSlipCSVWrapper.getCartonLength(),
                eulogiaCustomerPackingSlipCSVWrapper.getCartonWidth(),
                eulogiaCustomerPackingSlipCSVWrapper.getCartonHeight(),
                true, true, false, true, true
        ));

        // Pallet
        unitOfMeasure = commonServiceRestemplateClient.getUnitOfMeasureByName(companyId, warehouseId, "PL");
        if (Objects.isNull(unitOfMeasure)) {
            throw ResourceNotFoundException.raiseException("UOM Pallet not found");
        }
        itemUnitOfMeasures.add(createUnitOfMeasureForUploadingCustomerPackingSlip(
                companyId, warehouseId, unitOfMeasure.getId(),
                eulogiaCustomerPackingSlipCSVWrapper.getUnitPerCarton() * 30,
                eulogiaCustomerPackingSlipCSVWrapper.getCartonWeight() * 30,
                eulogiaCustomerPackingSlipCSVWrapper.getCartonLength() * 5,
                eulogiaCustomerPackingSlipCSVWrapper.getCartonWidth() * 2,
                eulogiaCustomerPackingSlipCSVWrapper.getCartonHeight() * 3,
                false, false, true, false, false
        ));

        return itemUnitOfMeasures;
    }
    private ItemUnitOfMeasure createUnitOfMeasureForUploadingCustomerPackingSlip(Long companyId,
                                                                                 Long warehouseId,
                                                                                 Long unitOfMeasureId,
                                                                                 Integer quantity,
                                                                                 Double weight,
                                                                                 Double length,
                                                                                 Double width,
                                                                                 Double height,
                                                                                 Boolean defaultForInboundReceiving,
                                                                                 Boolean defaultForWorkOrderReceiving,
                                                                                 Boolean trackingLpn,
                                                                                 Boolean defaultForDisplay,
                                                                                 Boolean caseFlag) {
        logger.debug("start to create new item unit of measure with");
        logger.debug("# unitOfMeasureId : {}", unitOfMeasureId);
        logger.debug("# quantity : {}", quantity);
        logger.debug("# weight : {}", weight);
        logger.debug("# length : {}", length);
        logger.debug("# width : {}", width);
        logger.debug("# height : {}", height);
        logger.debug("# defaultForInboundReceiving : {}", defaultForInboundReceiving);
        logger.debug("# defaultForWorkOrderReceiving : {}", defaultForWorkOrderReceiving);
        logger.debug("# trackingLpn : {}", trackingLpn);
        logger.debug("# defaultForDisplay : {}", defaultForDisplay);
        logger.debug("# caseFlag : {}", caseFlag);

        ItemUnitOfMeasure itemUnitOfMeasure = new ItemUnitOfMeasure();

        itemUnitOfMeasure.setCompanyId(companyId);
        itemUnitOfMeasure.setWarehouseId(warehouseId);

        itemUnitOfMeasure.setUnitOfMeasureId(unitOfMeasureId);
        itemUnitOfMeasure.setQuantity(quantity);

        itemUnitOfMeasure.setWeight(weight);
        itemUnitOfMeasure.setLength(length);
        itemUnitOfMeasure.setWidth(width);
        itemUnitOfMeasure.setHeight(height);

        itemUnitOfMeasure.setDefaultForInboundReceiving(defaultForInboundReceiving);
        itemUnitOfMeasure.setDefaultForWorkOrderReceiving(defaultForWorkOrderReceiving);
        itemUnitOfMeasure.setTrackingLpn(trackingLpn);

        itemUnitOfMeasure.setDefaultForDisplay(defaultForDisplay);
        itemUnitOfMeasure.setCaseFlag(caseFlag);

        return itemUnitOfMeasure;


    }

    /**
     * Setup the missing field for the uploaded files
     * # if item is missing, then use style value as item
     * # if item package type is missing, then use unitPerPack x packPerCarton as the item package type
     * @param eulogiaCustomerPackingSlipCSVWrapper
     */
    private void setupMissingValue(EulogiaCustomerPackingSlipCSVWrapper eulogiaCustomerPackingSlipCSVWrapper) {
        if (Strings.isBlank(eulogiaCustomerPackingSlipCSVWrapper.getItem())) {
            eulogiaCustomerPackingSlipCSVWrapper.setItem(
                    eulogiaCustomerPackingSlipCSVWrapper.getStyle()
            );
        }

        if (Objects.isNull(eulogiaCustomerPackingSlipCSVWrapper.getUnitPerPack())) {
            eulogiaCustomerPackingSlipCSVWrapper.setUnitPerPack(1);
        }
        if (Objects.isNull(eulogiaCustomerPackingSlipCSVWrapper.getPackPerCarton())) {
            eulogiaCustomerPackingSlipCSVWrapper.setPackPerCarton(1);
        }
        if (Objects.isNull(eulogiaCustomerPackingSlipCSVWrapper.getUnitPerCarton())) {
            eulogiaCustomerPackingSlipCSVWrapper.setUnitPerCarton(
                    eulogiaCustomerPackingSlipCSVWrapper.getUnitPerPack() *
                            eulogiaCustomerPackingSlipCSVWrapper.getPackPerCarton()
            );
        }

        /**
         * If the quantity doesn't match, then we may need to recalculate the quantities
         */
        if (eulogiaCustomerPackingSlipCSVWrapper.getUnitPerPack() * eulogiaCustomerPackingSlipCSVWrapper.getPackPerCarton() !=
            eulogiaCustomerPackingSlipCSVWrapper.getUnitPerCarton()) {
            if (eulogiaCustomerPackingSlipCSVWrapper.getUnitPerCarton() % eulogiaCustomerPackingSlipCSVWrapper.getUnitPerPack() != 0) {
                throw MissingInformationException.raiseException("unit per carton can't be divided by unit per pack");
            }
            eulogiaCustomerPackingSlipCSVWrapper.setPackPerCarton(
                    eulogiaCustomerPackingSlipCSVWrapper.getUnitPerCarton() / eulogiaCustomerPackingSlipCSVWrapper.getUnitPerPack()
            );
        }

        if (Objects.isNull(eulogiaCustomerPackingSlipCSVWrapper.getCartonWeight())) {
            eulogiaCustomerPackingSlipCSVWrapper.setCartonWeight(1.0);
        }
        if (Objects.isNull(eulogiaCustomerPackingSlipCSVWrapper.getCartonLength())) {
            eulogiaCustomerPackingSlipCSVWrapper.setCartonLength(1.0);
        }
        if (Objects.isNull(eulogiaCustomerPackingSlipCSVWrapper.getCartonWidth())) {
            eulogiaCustomerPackingSlipCSVWrapper.setCartonWidth(1.0);
        }
        if (Objects.isNull(eulogiaCustomerPackingSlipCSVWrapper.getCartonHeight())) {
            eulogiaCustomerPackingSlipCSVWrapper.setCartonHeight(1.0);
        }

        if (Strings.isBlank(eulogiaCustomerPackingSlipCSVWrapper.getItemPackageType())) {
            String itemPackageType =
                    (Objects.isNull(eulogiaCustomerPackingSlipCSVWrapper.getUnitPerPack()) ? "1" :  eulogiaCustomerPackingSlipCSVWrapper.getUnitPerPack().toString())
                    + "x"
                    + (Objects.isNull(eulogiaCustomerPackingSlipCSVWrapper.getPackPerCarton()) ? "1" :  eulogiaCustomerPackingSlipCSVWrapper.getPackPerCarton());

            eulogiaCustomerPackingSlipCSVWrapper.setItemPackageType(
                    itemPackageType
            );
        }

    }


}
