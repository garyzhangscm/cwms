package com.garyzhangscm.cwms.adminserver.model.tester;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.exception.TestFailException;
import com.garyzhangscm.cwms.adminserver.model.wms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TestCreatingInventoryAdjustmentThreshold extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestCreatingInventoryAdjustmentThreshold.class);

    // Will create inventory adjustment
    // quantity: 1000
    // cost: $500
    String[] itemNames = {"TEST-ITEM-HV-001", "TEST-ITEM-HV-002", "TEST-ITEM-HV-003"};
    String itemFamilyName = "TEST_HIGH_VALUE";

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;


    public TestCreatingInventoryAdjustmentThreshold() {
        super(TestScenarioType.CREATE_INVENTORY_ADJUSTMENT_THRESHOLD, 30100);


    }
    @Override
    public void runTest(Warehouse warehouse) {
            createInventoryAdjustmentThresholdsByItem(warehouse);
            createInventoryAdjustmentThresholdByItemFamily(warehouse, itemFamilyName);

            assertResult(warehouse);

    }

    private void createInventoryAdjustmentThresholdsByItem(Warehouse warehouse) {

        Arrays.stream(itemNames).forEach(itemName ->
                {
                    try {
                        createInventoryAdjustmentThreshold(warehouse, itemName);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        throw TestFailException.raiseException("Error while creating inventory adjustment threshold by items: " + e.getMessage());
                    }
                }
        );
    }

    private void createInventoryAdjustmentThreshold(Warehouse warehouse, String itemName) throws JsonProcessingException {
        InventoryAdjustmentThreshold inventoryAdjustmentThreshold =
                new InventoryAdjustmentThreshold();
        Item item = assertItem(warehouse, itemName);
        inventoryAdjustmentThreshold.setItem(item);
        inventoryAdjustmentThreshold.setCostThreshold(500.0);
        inventoryAdjustmentThreshold.setQuantityThreshold(1000L);
        inventoryAdjustmentThreshold.setEnabled(true);
        inventoryAdjustmentThreshold.setWarehouse(warehouse);
        inventoryAdjustmentThreshold.setWarehouseId(warehouse.getId());

        inventoryServiceRestemplateClient.createInventoryAdjustmentThreshold(
                inventoryAdjustmentThreshold
        );

    }

    private void createInventoryAdjustmentThresholdByItemFamily(Warehouse warehouse, String itemFamilyName)  {

        InventoryAdjustmentThreshold inventoryAdjustmentThreshold =
                new InventoryAdjustmentThreshold();

        ItemFamily itemFamily =
                inventoryServiceRestemplateClient.getItemFamilyByName(warehouse.getId(), itemFamilyName);

        inventoryAdjustmentThreshold.setItemFamily(itemFamily);
        inventoryAdjustmentThreshold.setCostThreshold(500.0);
        inventoryAdjustmentThreshold.setQuantityThreshold(1000L);
        inventoryAdjustmentThreshold.setEnabled(true);
        inventoryAdjustmentThreshold.setWarehouse(warehouse);
        inventoryAdjustmentThreshold.setWarehouseId(warehouse.getId());

        try {
            inventoryServiceRestemplateClient.createInventoryAdjustmentThreshold(
                    inventoryAdjustmentThreshold
            );
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw TestFailException.raiseException("Error while creating inventory adjustment threshold for item family: " + e.getMessage());
        }
    }

    private Item assertItem(Warehouse warehouse, String itemName) {
        Item item = inventoryServiceRestemplateClient.getItemByName(warehouse.getId(), null, itemName);

        if (Objects.isNull(item)) {
            throw TestFailException.raiseException("Can't find item by name: " + itemName);
        }
        if (item.getItemPackageTypes().size() == 0) {
            throw TestFailException.raiseException("The item doesn't have any package type: " + itemName);
        }
        return item;
    }

    private void assertResult(Warehouse warehouse) {


        assertInventoryAdjustmentThresholdByItem(warehouse);
        assertInventoryAdjustmentThresholdByItemFamily(warehouse);


    }
    private void assertInventoryAdjustmentThresholdByItem(Warehouse warehouse) {

        Arrays.stream(itemNames).forEach(itemName ->
                {

                    List<InventoryAdjustmentThreshold> inventoryAdjustmentThreshold =
                            inventoryServiceRestemplateClient.getInventoryAdjustmentThresholdByItem(
                                    warehouse.getId(), itemName);

                    if (inventoryAdjustmentThreshold.size() == 0) {
                        throw TestFailException.raiseException("The item doesn't have any adjustment threashold: " + itemName);
                    }
                }
        );


    }
    private void assertInventoryAdjustmentThresholdByItemFamily(Warehouse warehouse) {

        List<InventoryAdjustmentThreshold> inventoryAdjustmentThreshold =
                            inventoryServiceRestemplateClient.getInventoryAdjustmentThresholdByItemFamily(
                                    warehouse.getId(), itemFamilyName);


        if (inventoryAdjustmentThreshold.size() == 0) {
            throw TestFailException.raiseException("The item family doesn't have any adjustment threshold: " + itemFamilyName);
        }


    }


}
