package com.garyzhangscm.cwms.adminserver.model.tester;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.exception.TestFailException;
import com.garyzhangscm.cwms.adminserver.model.wms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Objects;

@Service
public class TestAdjustInventory extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestAdjustInventory.class);

    private final String itemName = "TEST-ITEM-HV-001";
    private final String locationName = "TEST-EA-001";
    private final String inventoryStatusName = "AVAL";

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public TestAdjustInventory() {

        super(TestScenarioType.INVENTORY_ADJUST, 50000);
    }
    @Override
    public void runTest(Warehouse warehouse) {

            Location location = assertLocationEmpty(warehouse, locationName);
            Item item = assertItem(warehouse, itemName);
            logger.debug("Will start to adjust inventory with item \n{}", item);
            InventoryStatus inventoryStatus = assertInventoryStatus(warehouse, inventoryStatusName);

            // Test inventory adjust with immediate consequence
            logger.debug("Start to test adding new inventory");
            Long quantity = 100L;
            Inventory inventory = createInventory(warehouse, location, item, quantity, inventoryStatus);
            // current inventory quantity: 100
            assertInventory(inventory.getId(), warehouse, location, item, quantity, inventoryStatus);
            logger.debug("New inventory added");

            logger.debug("Start to adjust inventory up by 200");
            Long adjustedQuantityUp = 200L;
            Long resultQuantity = quantity + adjustedQuantityUp;
            inventory = adjustInventory(inventory, adjustedQuantityUp);
            // current inventory quantity: 100 + 200 = 300
            assertInventoryQuantity(inventory, resultQuantity);
            logger.debug("Inventory adjust up by 200");

            logger.debug("Start to adjust inventory down by 250");
            Long adjustedQuantityDown = -250L;
            resultQuantity = resultQuantity + adjustedQuantityDown;
            inventory = adjustInventory(inventory, adjustedQuantityDown);
            // current inventory quantity: 300 - 250 = 50
            assertInventoryQuantity(inventory, resultQuantity);
            logger.debug("Inventory adjust down by 250");

            logger.debug("Start to adjust inventory up by 2500, approval needed");
            // Test inventory adjust with approval

            adjustedQuantityUp = 2500L;
            // This is a adjustment that need approval
            // let's make sure the quantity doesn't change
            // and we get a adjustment request
            // resultQuantity = resultQuantity + adjustedQuantityUp;
            inventory = adjustInventory(inventory, adjustedQuantityUp);
            // Quantity should be unchanged
            // current inventory quantity: 50 / unchanged
            assertInventoryQuantity(inventory, resultQuantity);
            InventoryAdjustmentRequest inventoryAdjustmentRequest =
                    assertInventoryAdjustmentRequest(warehouse, inventory, (resultQuantity + adjustedQuantityUp));

            // Let's approve this request
            logger.debug("Start to approve the inventory up by 2500");
            approveInventoryAdjustmentRequest(inventoryAdjustmentRequest);
            // refresh the inventory after the adjust request is approved
            inventory = inventoryServiceRestemplateClient.getInventoryById(inventory.getId());
            resultQuantity = resultQuantity + adjustedQuantityUp;
            // current inventory quantity: 50 + 2500 = 2550
            assertInventoryQuantity(inventory, resultQuantity);
            logger.debug("  inventory up by 2500 approved");


            logger.debug("Start to adjust inventory down by 2500, approval needed");
            adjustedQuantityDown = -2500L;
            // This is a adjustment that need approval
            // let's make sure the quantity doesn't change
            // and we get a adjustment request
            // resultQuantity = resultQuantity + adjustedQuantityUp;
            inventory = adjustInventory(inventory, adjustedQuantityDown);
            // Quantity should be unchanged
            // current inventory quantity: 2550 / unchanged
            assertInventoryQuantity(inventory, resultQuantity);
            inventoryAdjustmentRequest =
                    assertInventoryAdjustmentRequest(warehouse, inventory, (resultQuantity + adjustedQuantityDown));

            // Let's approve this request
            logger.debug("Start to reject the inventory down by 2500");
            disapproveInventoryAdjustmentRequest(inventoryAdjustmentRequest);
            // current inventory quantity: 2550 / unchanged
            assertInventoryQuantity(inventory, resultQuantity);
            logger.debug("  inventory down by 2500 reject");


    }

    private void approveInventoryAdjustmentRequest(InventoryAdjustmentRequest inventoryAdjustmentRequest) {

        inventoryServiceRestemplateClient.approveInventoryAdjustmentRequest(inventoryAdjustmentRequest.getId());
    }
    private void disapproveInventoryAdjustmentRequest(InventoryAdjustmentRequest inventoryAdjustmentRequest) {

        inventoryServiceRestemplateClient.disapproveInventoryAdjustmentReuqest(inventoryAdjustmentRequest.getId());
    }


    private InventoryAdjustmentRequest assertInventoryAdjustmentRequest(Warehouse warehouse, Inventory inventory, Long requestQuantity) {
        List<InventoryAdjustmentRequest> inventoryAdjustmentRequests =
                inventoryServiceRestemplateClient.getPendingInventoryAdjustmentRequestByInventoryId(
                        warehouse.getId(), inventory.getId()
                );

        if (inventoryAdjustmentRequests.size() == 0) {

            throw TestFailException.raiseException("Can't find inventory adjustment request by inventory: " + inventory);
        }
        else if (inventoryAdjustmentRequests.size() > 1) {

            throw TestFailException.raiseException("Find multiple request by inventory: " + inventory);
        }
        else {
            InventoryAdjustmentRequest inventoryAdjustmentRequest = inventoryAdjustmentRequests.get(0);
            if (!inventoryAdjustmentRequest.getNewQuantity().equals(requestQuantity)) {

                throw TestFailException.raiseException("Expected quantity:  " + requestQuantity +
                        ", actual quantity: " + inventoryAdjustmentRequest.getNewQuantity());
            }
            return inventoryAdjustmentRequest;

        }

    }


    private InventoryStatus assertInventoryStatus(Warehouse warehouse, String inventoryStatusName) {
        InventoryStatus inventoryStatus =
                inventoryServiceRestemplateClient.getInventoryStatusByName(
                        warehouse.getId(), inventoryStatusName
                );

        if (Objects.isNull(inventoryStatus)) {
            throw TestFailException.raiseException("Can't find inventory status by name: " + inventoryStatusName);

        }
        return inventoryStatus;
    }

    private Item assertItem(Warehouse warehouse, String itemName) {
        Item item = inventoryServiceRestemplateClient.getItemByName(warehouse.getId(), itemName);

        if (Objects.isNull(item)) {
            throw TestFailException.raiseException("Can't find item by name: " + itemName);
        }
        if (item.getItemPackageTypes().size() == 0) {
            throw TestFailException.raiseException("The item doesn't have any package type: " + itemName);
        }
        return item;
    }

    private Location assertLocationEmpty(Warehouse warehouse, String locationName) {

        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                warehouse.getId(), locationName
        );
        if (Objects.isNull(location)) {
            throw TestFailException.raiseException("Can't find location by name: " + locationName);
        }
        if (location.getCurrentVolume() > 0.0) {
            throw TestFailException.raiseException("Test fail as location  " + locationName + " is not empty" );

        }
        return location;

    }

    private Inventory createInventory(Warehouse warehouse, Location location,
                                 Item item, Long quantity,
                                 InventoryStatus inventoryStatus) {

        String lpn = commonServiceRestemplateClient.getNextLpn(warehouse.getId());

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

    private void assertInventory(Long inventoryId, Warehouse warehouse,
                                       Location location, Item item,
                                       Long quantity, InventoryStatus inventoryStatus) {
        // Let's get inventory
        Inventory existingInventory =
                inventoryServiceRestemplateClient.getInventoryById(inventoryId);
        if (Objects.isNull(existingInventory)) {

            throw TestFailException.raiseException("Error while get inventory we just created. ");
        }

        if (!existingInventory.getWarehouseId().equals(warehouse.getId())) {
            throw TestFailException.raiseException("Error creating inventory, warehouse ID doesn't match!" +
                    "expected warehouse id: " + warehouse.getId() + ", " +
                    "actual warehouse id: " + existingInventory.getWarehouseId());
        }

        if (!existingInventory.getLocationId().equals(location.getId())) {
            throw TestFailException.raiseException("Error creating inventory, location ID doesn't match!" +
                    "expected location id: " + location.getId() + ", " +
                    "actual location id: " + existingInventory.getLocationId());
        }

        if (!existingInventory.getItem().getId().equals(item.getId())) {
            throw TestFailException.raiseException("Error creating inventory, item ID doesn't match!" +
                    "expected item id: " + item.getId() + ", " +
                    "actual item id: " + existingInventory.getItem().getId());
        }

        if (!existingInventory.getQuantity().equals(quantity)) {
            throw TestFailException.raiseException("Error creating inventory, quantity doesn't match!" +
                    "expected quantity: " + quantity + ", " +
                    "actual quantity: " + existingInventory.getQuantity());
        }

        if (!existingInventory.getInventoryStatus().getId().equals(inventoryStatus.getId())) {
            throw TestFailException.raiseException("Error creating inventory, inventory status ID doesn't match!" +
                    "expected inventory status id: " + inventoryStatus.getId() + ", " +
                    "actual inventory status id: " + existingInventory.getInventoryStatus().getId());
        }
    }
    private Inventory adjustInventory(Inventory inventory, Long adjustedQuantity) {

        Long newQuantity = inventory.getQuantity() + adjustedQuantity;
        logger.debug("Start to adjust inventory from quantity {} to quantity {}",
                inventory.getQuantity(), newQuantity);
        return inventoryServiceRestemplateClient.adjustInventoryQuantity(inventory, newQuantity);
    }
    private void assertInventoryQuantity(Inventory inventory, long quantity) {
        Inventory existingInventory =
                inventoryServiceRestemplateClient.getInventoryById(inventory.getId());
        if (Objects.isNull(existingInventory)) {

            throw TestFailException.raiseException("Error while get inventory we just created. ");
        }
        if (!existingInventory.getQuantity().equals(quantity)) {
            throw TestFailException.raiseException("Error while verify quantity." +
                    "Expected: " + quantity + ", got: " + existingInventory.getQuantity());

        }
    }



}
