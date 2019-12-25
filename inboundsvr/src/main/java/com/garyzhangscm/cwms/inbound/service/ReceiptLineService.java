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
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.receipt_lines:receipt_lines.csv}")
    String testDataFile;

    public ReceiptLine findById(Long id) {
        return findById(id, true);
    }

    public ReceiptLine findById(Long id, boolean includeDetails) {
        ReceiptLine receiptLine = receiptLineRepository.findById(id).orElse(null);
        if (receiptLine != null && includeDetails) {
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

    public ReceiptLine findByNaturalKey(Long receiptId, String number) {
        return receiptLineRepository.findByNaturalKey(receiptId, number);

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

    public ReceiptLine save(ReceiptLine receiptLine) {
        return receiptLineRepository.save(receiptLine);
    }

    public ReceiptLine saveOrUpdate(ReceiptLine receiptLine) {

        if (receiptLine.getId() == null && findByNaturalKey(receiptLine.getReceipt().getId(), receiptLine.getNumber()) != null) {
            receiptLine.setId(findByNaturalKey(receiptLine.getReceipt().getId(), receiptLine.getNumber()).getId());
        }
        return save(receiptLine);
    }

    public void delete(ReceiptLine receiptLine) {
        receiptLineRepository.delete(receiptLine);
    }
    public void delete(Long id) {
        receiptLineRepository.deleteById(id);
    }
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
                addColumn("receipt").
                addColumn("number").
                addColumn("item").
                addColumn("expectedQuantity").
                addColumn("receivedQuantity").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ReceiptLineCSVWrapper.class);
    }

    public void initTestData() {
        try {
            InputStream inputStream = new ClassPathResource(testDataFile).getInputStream();
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

        if (!StringUtils.isBlank(receiptLineCSVWrapper.getReceipt())) {
            Receipt receipt = receiptService.findByNumber(receiptLineCSVWrapper.getReceipt());
            receiptLine.setReceipt(receipt);
        }
        if (!StringUtils.isBlank(receiptLineCSVWrapper.getItem())) {
            Item item = inventoryServiceRestemplateClient.getItemByName(receiptLineCSVWrapper.getItem());
            receiptLine.setItemId(item.getId());
        }
        return receiptLine;
    }

    public ReceiptLine addReceiptLine(Long receiptId, ReceiptLine receiptLine) {
        receiptLine.setReceipt(receiptService.findById(receiptId));
        if (receiptLine.getItemId() == null && receiptLine.getItem() != null) {
            receiptLine.setItemId(receiptLine.getItem().getId());
        }
        return save(receiptLine);
    }

    @Transactional
    public Inventory receive(Long receiptId, Long receiptLineId,
                             Inventory inventory) throws IOException{
        // Receive inventory and save it on the receipt

        Receipt receipt = receiptService.findById(receiptId);
        // Everytime when we check in a receipt, we will create a location with the same name so that
        // we can receive inventory on this receipt
        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(receipt.getNumber());
        inventory.setLocationId(location.getId());
        inventory.setLocation(location);
        inventory.setVirtual(false);
        inventory.setReceiptId(receiptId);

        Inventory newInventory = inventoryServiceRestemplateClient.addInventory(inventory);
        ReceiptLine receiptLine = findById(receiptLineId);
        receiptLine.setReceivedQuantity(receiptLine.getReceivedQuantity() + newInventory.getQuantity());
        save(receiptLine);
        return newInventory;
    }


}
