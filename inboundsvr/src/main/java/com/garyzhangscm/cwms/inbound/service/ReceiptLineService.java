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
import com.garyzhangscm.cwms.inbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.exception.GenericException;
import com.garyzhangscm.cwms.inbound.exception.ReceiptOperationException;
import com.garyzhangscm.cwms.inbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.repository.ReceiptLineRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class ReceiptLineService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(ReceiptLineService.class);

    @Autowired
    private ReceiptLineRepository receiptLineRepository;

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private FileService fileService;

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

        // Load Item information
        if (receiptLine.getItemId() != null && receiptLine.getItem() == null) {
            receiptLine.setItem(inventoryServiceRestemplateClient.getItemById(receiptLine.getItemId()));

        }

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

    @Transactional
    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ReceiptLineCSVWrapper> receiptLineCSVWrappers = loadData(inputStream);
            receiptLineCSVWrappers.stream().forEach(receiptLineCSVWrapper -> saveOrUpdate(convertFromWrapper(receiptLineCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private ReceiptLine convertFromWrapper(ReceiptLineCSVWrapper receiptLineCSVWrapper) {

        ReceiptLine receiptLine = new ReceiptLine();
        receiptLine.setNumber(receiptLineCSVWrapper.getNumber());
        receiptLine.setExpectedQuantity(receiptLineCSVWrapper.getExpectedQuantity());
        receiptLine.setReceivedQuantity(receiptLineCSVWrapper.getReceivedQuantity());


        receiptLine.setOverReceivingQuantity(receiptLineCSVWrapper.getOverReceivingQuantity());
        receiptLine.setOverReceivingPercent(receiptLineCSVWrapper.getOverReceivingPercent());

        // Warehouse is mandate
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(receiptLineCSVWrapper.getWarehouse());
        receiptLine.setWarehouseId(warehouse.getId());

        logger.debug("Start to create receipt line {} with item {}, in receipt {} / warehouse id: {}",
                receiptLineCSVWrapper.getNumber(),
                receiptLineCSVWrapper.getItem(), receiptLineCSVWrapper.getReceipt(),
                warehouse.getId());
        if (!StringUtils.isBlank(receiptLineCSVWrapper.getReceipt())) {
            Receipt receipt = receiptService.findByNumber(warehouse.getId(), receiptLineCSVWrapper.getReceipt());
            receiptLine.setReceipt(receipt);
        }
        if (!StringUtils.isBlank(receiptLineCSVWrapper.getItem())) {
            Item item =
                    inventoryServiceRestemplateClient.getItemByName(warehouse.getId(), receiptLineCSVWrapper.getItem());
            receiptLine.setItemId(item.getId());
        }
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
        return save(receiptLine);
    }

    @Transactional
    public Inventory receive(Long receiptId, Long receiptLineId,
                             Inventory inventory){
        // Receive inventory and save it on the receipt

        Receipt receipt = receiptService.findById(receiptId);
        ReceiptLine receiptLine = findById(receiptLineId);
        // If the inventory has location passed in, we will directly receive the inventory into
        // the location.
        // Otherwise, we will receive the location on the receipt and let the putaway logic
        // to decide where to put this LPN away

        // Validate if we can receive the inventory
        // 1. over receiving?
        // 3. unexpected item number?
        validateReceiving(receipt, receiptLine, inventory);

        logger.debug("Will receive inventory\n {}", inventory);
        if (inventory.getLocation() == null) {
            Location location =
                    warehouseLayoutServiceRestemplateClient.getLocationByName(
                            receipt.getWarehouseId(), receipt.getNumber());
            inventory.setLocationId(location.getId());
            inventory.setLocation(location);
            inventory.setVirtual(false);
        }
        else {
            inventory.setVirtual(inventory.getLocation().getLocationGroup().getLocationGroupType().getVirtual());
        }
        // Everytime when we check in a receipt, we will create a location with the same name so that
        // we can receive inventory on this receipt
        inventory.setReceiptId(receiptId);
        inventory.setWarehouseId(receipt.getWarehouseId());

        Inventory newInventory = inventoryServiceRestemplateClient.receiveInventory(inventory);
        // Note here when we receive, the inventory may already consolidate with existing inventory
        // in the location and the newInventory may represent the inventory after consolidated.
        // so we need to calculate the received quantity on the line based off the original
        // inventory structure
        receiptLine.setReceivedQuantity(receiptLine.getReceivedQuantity() + inventory.getQuantity());
        save(receiptLine);
        return newInventory;
    }

    // validate whether we can receive inventory against this receipt line
    // 1. over receiving?
    // 3. unexpected item number?
    private void validateReceiving(Receipt receipt, ReceiptLine receiptLine, Inventory inventory) {
        // unexpected item number?
        if (!receipt.getAllowUnexpectedItem() &&
             !receiptLine.getItemId().equals(inventory.getItem().getId())) {
            throw ReceiptOperationException.raiseException("Unexpected item not allowed for this receipt");
        }

        // check how many more we can receive against this receipt line
        Long maxOverReceivingQuantityAllowedByQuantity = 0L;
        Long maxOverReceivingQuantityAllowedByPercentage = 0L;

        if (receiptLine.getOverReceivingQuantity() > 0) {
            maxOverReceivingQuantityAllowedByQuantity = receiptLine.getOverReceivingQuantity();
        }

        if (receiptLine.getOverReceivingPercent() > 0) {
            maxOverReceivingQuantityAllowedByPercentage =
                    (long) (receiptLine.getExpectedQuantity() * receiptLine.getOverReceivingPercent() / 100);
        }
        Long maxOverReceivingQuantityAllowed = Math.max(
                maxOverReceivingQuantityAllowedByQuantity, maxOverReceivingQuantityAllowedByPercentage);

        // See should we receive this inventory, will the quantity maximum the total quantity allowed
        if (receiptLine.getReceivedQuantity() + inventory.getQuantity() >
                receiptLine.getExpectedQuantity() + maxOverReceivingQuantityAllowed) {
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

    public Long getWarehouseId(String warehouseName){
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(warehouseName);
        if (warehouse == null) {
            return null;
        }
        else {
            return warehouse.getId();
        }
    }

}
