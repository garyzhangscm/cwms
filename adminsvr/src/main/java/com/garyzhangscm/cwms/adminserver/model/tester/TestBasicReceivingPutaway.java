package com.garyzhangscm.cwms.adminserver.model.tester;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.clients.*;
import com.garyzhangscm.cwms.adminserver.exception.TestFailException;
import com.garyzhangscm.cwms.adminserver.model.wms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Test basic receiving process
 * 1. Fully received and close
 * 1.1 Over receiving is not allow
 * 2. Short receiving and close
 * 3. Over receiving with quantity and percentage
 * 4. putaway
 * 4.1 to empty location
 * 4.2 to partial location
 */
@Service
public class TestBasicReceivingPutaway extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestBasicReceivingPutaway.class);

    // Receipt numbers
    private final String fullyReceivedReceiptNumber = "RTEST000001";
    private final String shortReceivedReceiptNumber = "RTEST000002";
    private final String overReceivedReceiptNumber = "RTEST000003";
    private final String[] recieptNumbers = {fullyReceivedReceiptNumber, shortReceivedReceiptNumber, overReceivedReceiptNumber};
    private final String supplierName = "TEST-SUPPLIER-A";

    private final String[] itemNames = {"TEST-ITEM-HV-001", "TEST-ITEM-HV-002", "TEST-ITEM-HV-003"};
    private final Long[] expectedQuantities = {200L, 300L, 500L};
    private final String inventoryStatusName = "AVAL";

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private InboundServiceRestemplateClient inboundServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private IntegrationUtil integrationUtil;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public TestBasicReceivingPutaway() {

        super(TestScenarioType.BASIC_RECEIVING_PUTAWAY, 50300);

    }
    @Override
    public void runTest(Warehouse warehouse) {
        // create all receipts by integration downloading
        Supplier supplier = commonServiceRestemplateClient.getSupplierByName(supplierName);
        InventoryStatus inventoryStatus =
                inventoryServiceRestemplateClient.getInventoryStatusByName(
                        warehouse.getId(), inventoryStatusName
                );
        testDownloadReceipts(warehouse, supplier);

        testReceiving(warehouse, inventoryStatus);

        /***
        testReceiving(fullyReceivedReceipt);
        testPutaway(fullyReceivedReceipt);
        testCloseReceipt(fullyReceivedReceipt);
***/

    }

    /**
     * Test receiving inventory with the receipt we just created
     * @param warehouse
     */
    private void testReceiving(Warehouse warehouse, InventoryStatus inventoryStatus) {
        testFullyReceiving(warehouse, inventoryStatus);

    }

    /**
     * Test fully receiving all the lines
     * @param warehouse
     */
    private void testFullyReceiving(Warehouse warehouse, InventoryStatus inventoryStatus)  {
        Receipt receipt = inboundServiceRestemplateClient.getReceiptByNumber(
                warehouse.getId(), fullyReceivedReceiptNumber
        );

        // we will need to check in first
        inboundServiceRestemplateClient.checkInReceipt(receipt.getId());


        // fully receiving all the lines
        receipt.getReceiptLines().forEach(receiptLine -> {
            Item item = receiptLine.getItem();
            if (Objects.isNull(item)) {
                item = inventoryServiceRestemplateClient.getItemById(
                        receiptLine.getItemId()
                );
            }

            Inventory inventory = createInventory(
                    warehouse, null, item,
                    receiptLine.getExpectedQuantity() - receiptLine.getReceivedQuantity(),
                    inventoryStatus
            );
            try {
                inboundServiceRestemplateClient.receive(
                        receipt.getId(), receiptLine.getId(),
                        inventory
                );
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw TestFailException.raiseException("Can't receive inventory\n " + inventory +
                        " \n from receipt line: \n" + receiptLine);
            }
        });

        // let's complete the receipt
        try {
            inboundServiceRestemplateClient.completeReceipt(receipt.getId());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw TestFailException.raiseException("Can't complete receipt\n " +  receipt);
        }

        // make sure the integration data has been generated
        assertReceiptConfirmationIntegration(receipt.getNumber());
    }

    private void assertReceiptConfirmationIntegration(String receiptNumber) {
        Map<String, String> params = new HashMap<>();
        params.put("number", receiptNumber);


        int timeoutInTotal = 180000;
        int timeout = 2000;
        int i = 0;
        while(i * timeout < timeoutInTotal) {
            i++;
            try {

                List<IntegrationData> integrationDataList =
                        integrationUtil.getDataByParams(ReceiptConfirmation.class, params);

                // We already get the integration data
                if (integrationDataList.size() > 0) {
                    return;
                }
                Thread.sleep(timeout);

            } catch (InterruptedException e) {
                throw TestFailException.raiseException("Thread interrupted: " + e.getMessage());
            }
        }

        throw TestFailException.raiseException("Receipt: " + receiptNumber +
                "'s confirmation integration has not been saved after  " + (timeoutInTotal / 1000 ) + " seconds");

    }

    public void testDownloadReceipts(Warehouse warehouse, Supplier supplier) {

        List<IntegrationData> integrationDataList = createReceipts(warehouse, supplier);
        integrationDataList.forEach(integrationData -> {
            logger.debug("Created item Family integration data: {}", integrationData.getId());
        });

        assertReceiptIntegrationResult(integrationDataList, warehouse);


    }

    private List<IntegrationData> createReceipts(Warehouse warehouse, Supplier supplier) {

        List<IntegrationData> integrationDataList = new ArrayList<>();
        Arrays.stream(recieptNumbers).forEach(receiptNumber ->
        {
            try {
                integrationDataList.add(createReceipt(warehouse, receiptNumber, supplier));
            } catch (JsonProcessingException e) {
                throw TestFailException.raiseException("JsonProcessingException: " + e.getMessage());
            }
        });

        return integrationDataList;
    }

    private IntegrationData createReceipt(Warehouse warehouse, String receiptNumber, Supplier supplier) throws JsonProcessingException {

        logger.debug("Start to create receipt {}", receiptNumber);

        Receipt receipt = new Receipt(warehouse, receiptNumber, supplier);
        for (int i = 0; i < itemNames.length; i++) {
            String itemName = itemNames[i];
            Long quantity = expectedQuantities[i];

            Item item = inventoryServiceRestemplateClient.getItemByName(warehouse.getId(), itemName);
            ReceiptLine receiptLine = new ReceiptLine(warehouse, receipt, String.valueOf(i),
                    item, quantity);
            receipt.addReceiptLine(receiptLine);
        }


        return integrationUtil.sendData(Receipt.class, receipt);
    }


    private void assertReceiptIntegrationResult(List<IntegrationData> integrationDataList, Warehouse warehouse) {

        assertResultSaved(integrationDataList);
        assertResultProcessed(integrationDataList);
        assertResultConfirmed(warehouse);
    }


    private void assertResultSaved(List<IntegrationData> integrationDataList) {
        integrationUtil.assertResultSaved(integrationDataList, Receipt.class);
    }
    private void assertResultProcessed(List<IntegrationData> integrationDataList)  {
        integrationUtil.assertResultProcessed(integrationDataList, Receipt.class);
    }

    private void assertResultConfirmed(Warehouse warehouse) {

        // Let's check from the inventory service to see if our item has been saved correctly
        // if we are here, we will assume we already passed assetResultProcessed()
        // which means the integration data has been processed correctly

        // We will allow about 2 minutes for the system to send and process the integration data
        int timeoutInTotal = 180000;
        int timeout = 2000;
        int i = 0;
        boolean allProcessed;

        while(i * timeout < timeoutInTotal) {
            i++;
            allProcessed = true;
            try {

                for (String receiptNumber : recieptNumbers) {

                    // Get the item family information
                    Receipt receipt = inboundServiceRestemplateClient.getReceiptByNumber(warehouse.getId(), receiptNumber);
                    if (Objects.isNull(receipt) || !receipt.getNumber().equals(receiptNumber)) {
                        allProcessed = false;
                        break;
                    }

                    // make sure it has the right lines
                    if (itemNames.length != receipt.getReceiptLines().size()) {

                        throw TestFailException.raiseException("receipt " + receipt.getNumber() +
                                " has different number of lines.\n" +
                                " expected: " + itemNames.length + "\n " +
                                " actual: " + receipt.getReceiptLines().size());
                    }

                    for (int j = 0; j < itemNames.length; j++) {

                        String itemName = itemNames[i];
                        Long quantity = expectedQuantities[i];

                        // Loop through all receipt lines
                        // and make sure we have at least one
                        // receipt line matches with the
                        // expectation
                        boolean matchedReceiptLine = false;
                        for(ReceiptLine receiptLine : receipt.getReceiptLines()) {
                            if (receiptLine.getItem().getName().equals(itemName) &&
                                receiptLine.getExpectedQuantity().equals(quantity)) {
                                matchedReceiptLine = true;
                                break;
                            }
                        }
                        if (!matchedReceiptLine) {
                            throw TestFailException.raiseException("receipt " + receipt.getNumber() +
                                    " doesn't have item : " + itemName);
                        }

                    }


                }
                if (allProcessed) {
                    // OK, all the integration has been process, sounds good
                    return;
                }
                else {

                    //
                    Thread.sleep(timeout);
                }

            } catch (InterruptedException e) {
                throw TestFailException.raiseException("Thread interrupted: " + e.getMessage());
            }
        }

        throw TestFailException.raiseException("All integration has been processed but we can't find the receipt after  " +
                (timeoutInTotal / 1000 ) + " seconds");

    }

    private Inventory createInventory(Warehouse warehouse, Location location,
                                      Item item, Long quantity,
                                      InventoryStatus inventoryStatus) {

        String lpn = commonServiceRestemplateClient.getNextLpn();

        ItemPackageType itemPackageType = item.getItemPackageTypes().get(0);

        return new Inventory(lpn,  warehouse, location,
                item, itemPackageType,  inventoryStatus,
                quantity);
    }




}
