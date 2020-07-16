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
 * Test basic Sales order
 * 1. Customization from each picking area
 * 2. Case picking
 * 3. Pallet picking
 * 4. Complete
 */
@Service
public class TestSalesOrderWithShortAllocation extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestSalesOrderWithShortAllocation.class);

    // Order number
    private final String orderNumber = "SO000002";


    // Item                  replenish from
    // TEST-ITEM-HV-023       TEST-CS-021
    // TEST-ITEM-HV-024       TEST-PL-022
    private final String[] itemNames = {"TEST-ITEM-HV-023", "TEST-ITEM-HV-024"};
    private final String csLocationName = "TEST-CS-021";
    private final String plLocationName = "TEST-PL-022";
    private final String[] locationNames = {csLocationName, plLocationName};
    private final Long[] inventoryQuantities = {300L, 2000L};
    private final Long[] orderQuantities = {3L, 5L};

    private final String customerName = "TEST-CUSTOMER-X";

    private final String inventoryStatusName = "AVAL";

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private InboundServiceRestemplateClient inboundServiceRestemplateClient;
    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private IntegrationUtil integrationUtil;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public TestSalesOrderWithShortAllocation() {

        super(TestScenarioType.SALES_ORDER_WITH_SHORT_ALLOCATION, 51010);

    }
    @Override
    public void runTest(Warehouse warehouse) {
        // create all receipts by integration downloading
        Customer customer = commonServiceRestemplateClient.getCustomerByName(customerName);
        InventoryStatus inventoryStatus =
                inventoryServiceRestemplateClient.getInventoryStatusByName(
                        warehouse.getId(), inventoryStatusName
                );
        testCreateInventory(warehouse, inventoryStatus);

        Order order = testDownloadOrder(warehouse, customer, inventoryStatus);

        testOutboundProcess(warehouse, inventoryStatus, order);


    }

    private void testOutboundProcess(Warehouse warehouse, InventoryStatus inventoryStatus, Order order) {
        List<ShortAllocation> shortAllocations  = testAllocation(warehouse, order);

        testEmergencyReplenishment(warehouse, shortAllocations);

        // testShipping(warehouse, order);
    }

    private void testEmergencyReplenishment(Warehouse warehouse, List<ShortAllocation> shortAllocations) {
        // Let's get all the picks that related to the short allocation
        logger.debug("Will try to finish all the picks for short allocation");

        shortAllocations.forEach(shortAllocation -> confirmPicks(warehouse, shortAllocation));
    }

    private void confirmPicks(Warehouse warehouse,ShortAllocation shortAllocation) {
        logger.debug("Start to confirm short allocation id: {}", shortAllocation.getId());
        List<Pick> picks = getPicksByShortAllocation(warehouse, shortAllocation);
        logger.debug("Get {} picks by this short allocation", picks.size());


        picks.forEach(pick -> {
            logger.debug("Start to confirm pick with id {}", pick.getId());
            outbuondServiceRestemplateClient.confirmPick(pick);
            logger.debug("Confirmed! pick with id {}", pick.getId());
        });

    }

    private List<Pick> getPicksByShortAllocation(Warehouse warehouse, ShortAllocation shortAllocation) {
        int timeoutInTotal = 180000;
        int timeout = 2000;
        int i = 0;

        while(i * timeout < timeoutInTotal) {
            i++;
            try {
                List<Pick> picks = outbuondServiceRestemplateClient.getPicksByShortAllocation(warehouse.getId(), shortAllocation);

                if (picks.size() == 0) {

                    // Order has not been saved yet
                    Thread.sleep(timeout);
                    continue;
                }
                else {
                    return picks;
                }

            } catch (InterruptedException e) {
                throw TestFailException.raiseException("Thread interrupted: " + e.getMessage());
            }
        }

        throw TestFailException.raiseException("Can't get any picks for the short allocation  " + shortAllocation +
                " after " + (timeoutInTotal / 1000 ) + " seconds");
    }

    private List<ShortAllocation> testAllocation(Warehouse warehouse, Order order) {

        // Order order = outbuondServiceRestemplateClient.getOrderByNumber(warehouse.getId(), orderNumber);
        order = outbuondServiceRestemplateClient.allocateOrder(order);


        // Let's get all the picks and confirm we picks
        // the right quantity from the locations
        List<ShortAllocation> shortAllocations  = assertAllocationResult(warehouse, order);

        return shortAllocations;

    }


    private List<ShortAllocation> assertAllocationResult(Warehouse warehouse, Order order) {
        List<ShortAllocation> shortAllocations = outbuondServiceRestemplateClient.getShortAllocationsByOrder(warehouse.getId(), order);
        if (shortAllocations.size() != itemNames.length) {
            throw TestFailException.raiseException("Allocation error. Number doesn't match.\n" +
                    "Number of ShortAllocation: " + shortAllocations.size() + "\n" +
                    "Number of item: " + itemNames.length);
        }
        for(ShortAllocation shortAllocation : shortAllocations) {
            boolean itemExists = Arrays.stream(itemNames)
                    .filter(itemName -> shortAllocation.getItem().getName().equals(itemName)).count() > 0;
            if (!itemExists) {

                throw TestFailException.raiseException("Allocation error. Item doesn't match.\n" +
                        "Current shortAllocation's item: " + shortAllocation.getItem().getName() + "\n" +
                        "expected item: " + itemNames);
            }
        }
        return shortAllocations;
    }


    private void testCreateInventory(Warehouse warehouse, InventoryStatus inventoryStatus) {
        for(int i = 0; i < itemNames.length; i++) {

            String itemName = itemNames[i];
            Long quantity = inventoryQuantities[i];
            String locationName = locationNames[i];

            Item item = inventoryServiceRestemplateClient.getItemByName(warehouse.getId(), itemName);
            Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                    warehouse.getId(), locationName
            );

            createInventory(warehouse, location,
                    item, quantity, inventoryStatus);
        }
    }


    public Order testDownloadOrder(Warehouse warehouse, Customer customer, InventoryStatus inventoryStatus) {


        try {
            IntegrationData integrationData = createOrder(warehouse, orderNumber, customer, inventoryStatus);
            logger.debug("Created order integration data: {}", integrationData.getId());

            return assertOrderIntegrationResult(integrationData, warehouse);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw TestFailException.raiseException("Error download order " + orderNumber + " by integration");
        }



    }


    private Order assertOrderIntegrationResult(IntegrationData integrationData, Warehouse warehouse) {

        assertResultSaved(integrationData);
        assertResultProcessed(integrationData);
        return assertResultConfirmed(warehouse);
    }


    private void assertResultSaved(IntegrationData integrationData) {
        integrationUtil.assertResultSaved(Collections.singletonList(integrationData), Order.class);
    }
    private void assertResultProcessed(IntegrationData integrationData)  {
        integrationUtil.assertResultProcessed(Collections.singletonList(integrationData), Order.class);
    }

    private Order assertResultConfirmed(Warehouse warehouse) {

        // Let's check from the inventory service to see if our item has been saved correctly
        // if we are here, we will assume we already passed assetResultProcessed()
        // which means the integration data has been processed correctly

        // We will allow about 2 minutes for the system to send and process the integration data
        int timeoutInTotal = 180000;
        int timeout = 2000;
        int i = 0;

        while(i * timeout < timeoutInTotal) {
            i++;
            try {
                Order order = outbuondServiceRestemplateClient.getOrderByNumber(warehouse.getId(), orderNumber);

                if (Objects.isNull(order) || !order.getNumber().equals(orderNumber)) {

                    // Order has not been saved yet
                    Thread.sleep(timeout);
                    continue;
                }

                // Order has been saved
                // make sure it has the right lines
                if (itemNames.length != order.getOrderLines().size()) {
                    throw TestFailException.raiseException("order " + order.getNumber() +
                                " has different number of lines.\n" +
                                " expected: " + itemNames.length + "\n " +
                                " actual: " + order.getOrderLines().size());
                }

                for (int j = 0; j < itemNames.length; j++) {
                    String itemName = itemNames[i];
                    Long quantity = orderQuantities[i];

                    // Loop through all receipt lines
                    // and make sure we have at least one
                    // receipt line matches with the
                    // expectation
                    boolean matchedReceiptLine = false;
                    for(OrderLine orderLine : order.getOrderLines()) {
                        if (orderLine.getItem().getName().equals(itemName) &&
                                orderLine.getExpectedQuantity().equals(quantity)) {
                            matchedReceiptLine = true;
                            break;
                        }
                    }
                    if (!matchedReceiptLine) {
                        throw TestFailException.raiseException("order " + order.getNumber() +
                                    " doesn't have item : " + itemName);
                    }

                }

                // If we are here, we know we have the order saved
                // with the right quantity
                return order;
            } catch (InterruptedException e) {
                throw TestFailException.raiseException("Thread interrupted: " + e.getMessage());
            }
        }

        throw TestFailException.raiseException("All integration has been processed but we can't find the order after  " +
                (timeoutInTotal / 1000 ) + " seconds");

    }

    private IntegrationData createOrder(Warehouse warehouse, String orderNumber,
                                        Customer customer , InventoryStatus inventoryStatus) throws JsonProcessingException {

        logger.debug("Start to create order {}", orderNumber);


        Order order  = new Order(warehouse, orderNumber, customer, customer);

        for (int i = 0; i < itemNames.length; i++) {
            String itemName = itemNames[i];
            Long quantity = orderQuantities[i];

            Item item = inventoryServiceRestemplateClient.getItemByName(warehouse.getId(), itemName);
            OrderLine orderLine = new OrderLine(warehouse, order, String.valueOf(i),
                    item, quantity, inventoryStatus);
            order.addOrderLine(orderLine);
        }


        return integrationUtil.sendData(Order.class, order);
    }

    private Inventory createInventory(Warehouse warehouse, Location location,
                                      Item item, Long quantity,
                                      InventoryStatus inventoryStatus) {

        String lpn = commonServiceRestemplateClient.getNextLpn();

        ItemPackageType itemPackageType = item.getItemPackageTypes().get(0);

        Inventory inventory = new Inventory(lpn,  warehouse, location,
                item, itemPackageType,  inventoryStatus,
                quantity);
        try {
            inventory = inventoryServiceRestemplateClient.createInventory(inventory);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw TestFailException.raiseException("Error while creating create inventory. " + e.getMessage());
        }
        return inventory;
    }



}
