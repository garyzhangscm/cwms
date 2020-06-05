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

import com.garyzhangscm.cwms.inbound.exception.ReceiptOperationException;
import com.garyzhangscm.cwms.inbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.repository.ReceiptRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class ReceiptService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(ReceiptService.class);

    @Autowired
    private ReceiptRepository receiptRepository;
    @Autowired
    private ReceiptLineService receiptLineService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.receipts:receipts}")
    String testDataFile;

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


    public List<Receipt> findAll(Long warehouseId, String number) {
        return findAll(warehouseId, number, true);
    }

    public List<Receipt> findAll(Long warehouseId, String number, boolean loadDetails) {
        List<Receipt> receipts;

        if (StringUtils.isBlank(number)) {
            receipts = receiptRepository.findAll(warehouseId);
        }
        else {
            Receipt receipt = receiptRepository.findByNumber(warehouseId, number);
            if (receipt != null) {
                receipts = Arrays.asList(new Receipt[]{receipt});
            }
            else {
                receipts = new ArrayList<>();
            }
        }
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

    }


    @Transactional
    public Receipt save(Receipt receipt) {
        Receipt newReceipt = receiptRepository.save(receipt);
        loadReceiptAttribute(newReceipt);
        return newReceipt;
    }

    @Transactional
    public Receipt saveOrUpdate(Receipt receipt) {
        if (receipt.getId() == null && findByNumber(receipt.getWarehouseId(),receipt.getNumber()) != null) {
            receipt.setId(findByNumber(receipt.getWarehouseId(),receipt.getNumber()).getId());
        }
        return save(receipt);
    }


    @Transactional
    public Receipt checkInReceipt(Long receiptId) {
        Receipt receipt = findById(receiptId);
        logger.debug("receipt ID: {}, status: {}", receiptId, receipt.getReceiptStatus());
        logger.debug("receipt.getReceiptStatus().equals(ReceiptStatus.OPEN)? {}", receipt.getReceiptStatus().equals(ReceiptStatus.OPEN));
        // only allow check in when the receipt is in OPEN status
        if (receipt != null && receipt.getReceiptStatus().equals(ReceiptStatus.OPEN)) {

            receipt.setReceiptStatus(ReceiptStatus.CHECK_IN);
            logger.debug("update the receipt to check in");
            Receipt newReceipt = save(receipt);

            // After we check in the receipt, we will create a location
            // with the same name of the receipt so that we can receive inventory
            // on this receipt

            logger.debug("create the location for the receipt");
            warehouseLayoutServiceRestemplateClient.createLocationForReceipt(newReceipt);
            return newReceipt;
        }
        throw ReceiptOperationException.raiseException("Can't check in the receipt due to not correct status. Current Status: " + receipt.getReceiptStatus());
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

    public List<ReceiptCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("number").
                addColumn("client").
                addColumn("supplier").
                addColumn("allowUnexpectedItem").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ReceiptCSVWrapper.class);
    }

    @Transactional
    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ReceiptCSVWrapper> receiptCSVWrappers = loadData(inputStream);
            receiptCSVWrappers.stream().forEach(receiptCSVWrapper -> saveOrUpdate(convertFromWrapper(receiptCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private Receipt convertFromWrapper(ReceiptCSVWrapper receiptCSVWrapper) {

        Receipt receipt = new Receipt();
        receipt.setNumber(receiptCSVWrapper.getNumber());
        receipt.setReceiptStatus(ReceiptStatus.OPEN);
        receipt.setAllowUnexpectedItem(receiptCSVWrapper.getAllowUnexpectedItem());

        if (!StringUtils.isBlank(receiptCSVWrapper.getWarehouse())) {
            Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(receiptCSVWrapper.getWarehouse());
            if (warehouse != null) {
                receipt.setWarehouseId(warehouse.getId());
            }
        }

        if (!StringUtils.isBlank(receiptCSVWrapper.getClient())) {
            Client client = commonServiceRestemplateClient.getClientByName(receiptCSVWrapper.getClient());
            receipt.setClientId(client.getId());
        }
        if (!StringUtils.isBlank(receiptCSVWrapper.getSupplier())) {
            Supplier supplier = commonServiceRestemplateClient.getSupplierByName(receiptCSVWrapper.getSupplier());
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

        return inventoryServiceRestemplateClient.findInventoryByReceipt(receipt.getWarehouseId(), receiptId);
    }


}
