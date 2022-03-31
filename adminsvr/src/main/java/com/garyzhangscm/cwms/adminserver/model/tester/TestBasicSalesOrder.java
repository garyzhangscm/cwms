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
public class TestBasicSalesOrder extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestBasicSalesOrder.class);

    // Order number
    private final String orderNumber = "SO000001";


    private final String[] itemNames = {"TEST-ITEM-HV-020", "TEST-ITEM-HV-021", "TEST-ITEM-HV-022"};
    private final String eaLocationName = "TEST-EA-020";
    private final String csLocationName = "TEST-CS-020";
    private final String plLocationName = "TEST-PL-020";
    private final String[] locationNames = {eaLocationName, csLocationName, plLocationName};
    private final Long[] inventoryQuantities = {200L, 300L, 2000L};
    private final Long[] orderQuantities = {200L, 300L, 2000L};

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


    public TestBasicSalesOrder() {

        super(TestScenarioType.BASIC_SALES_ORDER, 51000);

    }
    @Override
    public void runTest(Warehouse warehouse) {
        // create all receipts by integration downloading
        Customer customer = commonServiceRestemplateClient.getCustomerByName(null, warehouse.getId(), customerName);
        InventoryStatus inventoryStatus =
                inventoryServiceRestemplateClient.getInventoryStatusByName(
                        warehouse.getId(), inventoryStatusName
                );
        testCreateInventory(warehouse, inventoryStatus);

        Order order = testDownloadOrder(warehouse, customer, inventoryStatus);

        testOutboundProcess(warehouse, inventoryStatus, order);


    }

    private void testOutboundProcess(Warehouse warehouse, InventoryStatus inventoryStatus, Order order) {
        List<Pick> picks  = testAllocation(warehouse, order);

        testPicking(warehouse, order, picks);

        testShipping(warehouse, order);
    }

    private List<Pick> testAllocation(Warehouse warehouse, Order order) {

        // Order order = outbuondServiceRestemplateClient.getOrderByNumber(warehouse.getId(), orderNumber);
        order = outbuondServiceRestemplateClient.allocateOrder(order);


        // Let's get all the picks and confirm we picks
        // the right quantity from the locations
        List<Pick> picks  = assertAllocationResult(warehouse, order);
        return picks;

    }

    private List<Pick> assertAllocationResult(Warehouse warehouse, Order order) {
        List<Pick> picks = outbuondServiceRestemplateClient.getPicksByOrder(warehouse.getId(), order);
        if (picks.size() != itemNames.length) {
            throw TestFailException.raiseException("Allocation error. Number doesn't match.\n" +
                    "Number of picks: " + picks.size() + "\n" +
                    "Number of item: " + itemNames.length);
        }
        for(Pick pick : picks) {
            boolean itemExists = Arrays.stream(itemNames)
                    .filter(itemName -> pick.getItem().getName().equals(itemName)).count() > 0;
            if (!itemExists) {

                throw TestFailException.raiseException("Allocation error. Item doesn't match.\n" +
                        "Current pick's item: " + pick.getItem().getName() + "\n" +
                        "expected item: " + itemNames);
            }
        }
        return picks;
    }

    private void testPicking(Warehouse warehouse, Order order, List<Pick> picks) {
        // Let's get all the picks by the order
        // and then confirm all of them in full quantity
        picks.forEach(pick -> {
            outbuondServiceRestemplateClient.confirmPick(pick);
        });

        assertOrderFullyPicked(warehouse, order);
    }

    private void assertOrderFullyPicked(Warehouse warehouse, Order order) {

        // Get the latest
        List<Pick> picks = outbuondServiceRestemplateClient.getPicksByOrder(warehouse.getId(), order);
        for(Pick pick: picks) {
            if (!pick.getPickedQuantity().equals(pick.getQuantity())) {

                throw TestFailException.raiseException("Pick confirm error: \n" +
                        "Expected pick " + pick.getQuantity() + " of item " + pick.getItem().getName() +
                        " from location " + pick.getSourceLocation().getName() + "\n" +
                        "actual picked quantity: " + pick.getPickedQuantity());
            }

        }
    }

    /**
     * Complete the order and
     * @param warehouse
     */
    private void testShipping(Warehouse warehouse, Order order) {

        outbuondServiceRestemplateClient.completeOrder(order);

        assertOrderCompleted(warehouse);

        assertOrderConfirmationIntegration(order);
    }

    private void assertOrderConfirmationIntegration(Order order) {

        Map<String, String> params = new HashMap<>();
        params.put("number", order.getNumber());


        int timeoutInTotal = 180000;
        int timeout = 2000;
        int i = 0;
        while(i * timeout < timeoutInTotal) {
            i++;
            try {

                List<IntegrationData> integrationDataList =
                        integrationUtil.getDataByParams(OrderConfirmation.class, params);

                // We already get the integration data
                if (integrationDataList.size() > 0) {
                    return;
                }
                Thread.sleep(timeout);

            } catch (InterruptedException e) {
                throw TestFailException.raiseException("Thread interrupted: " + e.getMessage());
            }
        }

        throw TestFailException.raiseException("Order: " + order.getNumber() +
                "'s confirmation integration has not been saved after  " + (timeoutInTotal / 1000 ) + " seconds");
    }

    /**
     * Make sure the order is completed and
     * inventory are in the order's location now
     *
     * @param warehouse
     */
    private void assertOrderCompleted(Warehouse warehouse) {

        Order order = outbuondServiceRestemplateClient.getOrderByNumber(warehouse.getId(), orderNumber);

        if (!order.getStatus().equals(OrderStatus.COMPLETE)) {
            throw TestFailException.raiseException("Order " + orderNumber + " is " + order.getStatus() + ", expected: " + OrderStatus.COMPLETE);
        }

        // Make sure all inventory are on the order now

        List<Inventory> inventories = outbuondServiceRestemplateClient.getPickedInventoryByOrder(warehouse.getId(), order);

        for(Inventory inventory : inventories) {
            if (!inventory.getLocation().getName().equals(orderNumber)) {

                throw TestFailException.raiseException("inventory " + inventory.getLpn() + " from Order " + orderNumber +
                        " is not in the right location. Expected on the order. Actual location: " + inventory.getLocation().getName());
            }
        }
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
