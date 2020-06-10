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
public class TestIntegrationDownloadItem extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestIntegrationDownloadItem.class);

    // key: item name
    // value: item family name
    Map<String, String> testingItems = new HashMap<>();

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;


    public TestIntegrationDownloadItem() {
        super(TestScenarioType.INTEGRATION_DOWNLOAD_ITEM, 10000);

        for(int i = 1; i <= 5; i++) {
            String itemName = "ITEM-HV-00" + i;
            testingItems.put(itemName, "HIGH_VALUE");
            itemName = "ITEM-FZ-00" + i;
            testingItems.put(itemName, "FROZEN");
        }

    }
    public boolean run(Warehouse warehouse) {

        logger.debug("Start to run test scenario: {} - {}", warehouse.getName(), getName());
        try {
            List<IntegrationData> integrationDataList = createItem(warehouse);
            integrationDataList.forEach(integrationData -> {
                logger.debug("Created item integration data: {}", integrationData.getId());
            });

            assertResult(integrationDataList, warehouse);
        } catch (Exception e) {
            e.printStackTrace();
            setErrorMessage(e.getMessage());
            return false;
        }

        return true;

    }

    private void assertResult(List<IntegrationData> integrationDataList, Warehouse warehouse) {

        // we will need to make sure
        // 1. all the integration data has been saved, immediately
        // 2. all the integration data has been processed, within certain time period
        // 3. data has been saved in certain table, within certain time period
        assertResultSaved(integrationDataList);
        assertResultProcessed(integrationDataList);
        assertResultConfirmed(warehouse);



    }
    private void assertResultSaved(List<IntegrationData> integrationDataList) {
        integrationDataList.forEach(integrationData -> {
            // make sure we at least have the integration with the right id

            IntegrationData savedIntegrationData
                    = integrationServiceRestemplateClient.getItemData(integrationData.getId());
            if (Objects.isNull(savedIntegrationData)) {
                throw TestFailException.raiseException("Can't find integration data by id " + integrationData.getId());
            }
        });
    }
    private void assertResultProcessed(List<IntegrationData> integrationDataList)  {
        // We will allow about 2 minutes for the system to process the integration data
        int timeoutInTotal = 120000;
        int timeout = 2000;
        int i = 0;
        boolean allProcessed;
        while(i * timeout < timeoutInTotal) {
            i++;
            try {
                allProcessed = true;
                for (IntegrationData integrationData : integrationDataList) {

                    IntegrationData savedIntegrationData
                            = integrationServiceRestemplateClient.getItemData(integrationData.getId());
                    if (Objects.isNull(savedIntegrationData)) {
                        throw TestFailException.raiseException("Can't find integration data by id " + integrationData.getId());
                    }
                    if (!savedIntegrationData.getStatus().equals(IntegrationStatus.COMPLETED) &&
                        !savedIntegrationData.getStatus().equals(IntegrationStatus.ERROR)) {
                        allProcessed = false;
                        break;
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

        throw TestFailException.raiseException("Some integration has not been processed after  " +
                (timeoutInTotal / (timeout * 1000)) + " seconds");
    }
    private void assertResultConfirmed(Warehouse warehouse) {

        // Let's check from the inventory service to see if our item has been saved correctly
        // if we are here, we will assume we already passed assetResultProcessed()
        // which means the integration data has been processed correctly

        // We will allow about 2 minutes for the system to send and process the integration data
        int timeoutInTotal = 120000;
        int timeout = 2000;
        int i = 0;
        boolean allProcessed;

        while(i * timeout < timeoutInTotal) {
            i++;
            allProcessed = true;
            try {

                for (Map.Entry<String, String> entry : testingItems.entrySet()) {
                    String itemName = entry.getKey();
                    String itemFamilyName = entry.getValue();
                    // Get the item information
                    Item item = inventoryServiceRestemplateClient.getItemByName(warehouse.getId(), itemName);
                    if (Objects.isNull(item)) {
                        allProcessed = false;
                        break;
                    }

                    // OK, we got the item saved, let's check if
                    // if it has the right
                    // 1. item family
                    // 2. item package type
                    // 3. item unit of measure
                    if (Objects.isNull(item.getItemFamily()) || !item.getItemFamily().getName().equals(itemFamilyName)){
                        allProcessed = false;
                        break;
                    }
                    if (item.getItemPackageTypes().size() == 0){
                        allProcessed = false;
                        break;
                    }
                    for (ItemPackageType itemPackageType : item.getItemPackageTypes()) {
                        if (itemPackageType.getItemUnitOfMeasures().size() == 0) {
                            allProcessed = false;
                            break;
                        }
                    }
                    if (!allProcessed) {
                        // if we are here, it means the item has package type(s)
                        // but some of the package type has no unit of measure
                        break;
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

        throw TestFailException.raiseException("All integration has been processed but we can't find the item after  " +
                (timeoutInTotal / (timeout * 1000)) + " seconds");

    }

    private List<IntegrationData> createItem(Warehouse warehouse) {

        List<IntegrationData> integrationDataList = new ArrayList<>();
        testingItems.entrySet().forEach(entry ->
        {
            try {
                integrationDataList.add(createItem(warehouse, entry.getKey(), entry.getValue()));
            } catch (JsonProcessingException e) {
                throw TestFailException.raiseException("JsonProcessingException: " + e.getMessage());
            }
        });

        return integrationDataList;

    }

    private IntegrationData createItem(Warehouse warehouse, String name, String itemFamilyName) throws JsonProcessingException {
        logger.debug("Start to create item {}", name);

        // Get the item family first

        ItemFamily itemFamily =
                inventoryServiceRestemplateClient.getItemFamilyByName(warehouse.getId(), itemFamilyName);

        Item item = new Item();
        item.setName(name);
        item.setDescription("Test Integration download item - " + name);
        item.setUnitCost(5.0);
        // item.setWarehouseId(warehouse.getId());
        item.setWarehouseName(warehouse.getName());
        item.setItemFamily(itemFamily);
        item.setItemPackageTypes(createItemPackageTypes(item));

        return integrationServiceRestemplateClient.sendItemData(item);

    }
    private List<ItemPackageType> createItemPackageTypes(Item item){
        List<ItemPackageType> itemPackageTypes = new ArrayList<>();
        itemPackageTypes.add(createItemPackageType(item, item.getName() + "-PACKAGE"));
        return itemPackageTypes;

    }

    private ItemPackageType createItemPackageType(Item item, String name){
        ItemPackageType itemPackageType = new ItemPackageType();
        itemPackageType.setName(name);
        itemPackageType.setDescription(name);
        // itemPackageType.setWarehouseId(item.getWarehouseId());
        itemPackageType.setWarehouseName(item.getWarehouseName());
        itemPackageType.setItemUnitOfMeasures(createItemUnitOfMeasures(itemPackageType));
        return itemPackageType;
    }

    private List<ItemUnitOfMeasure> createItemUnitOfMeasures(ItemPackageType itemPackageType){
        List<ItemUnitOfMeasure> itemUnitOfMeasures = new ArrayList<>();

        // Will create Item Unit Of Measure with EA / CS / PL
        UnitOfMeasure unitOfMeasureEA = commonServiceRestemplateClient.getUnitOfMeasureByName("EA");
        UnitOfMeasure unitOfMeasureCS = commonServiceRestemplateClient.getUnitOfMeasureByName("CS");
        UnitOfMeasure unitOfMeasurePL = commonServiceRestemplateClient.getUnitOfMeasureByName("PL");

        itemUnitOfMeasures.add(createItemUnitOfMeasure(

                unitOfMeasureEA, 1.0, 1.0, 1.0,
                1, 1.0, itemPackageType.getWarehouseName()
        ));

        itemUnitOfMeasures.add(createItemUnitOfMeasure(

                unitOfMeasureCS, 5.0, 4.0, 5.0,
                100, 100.0, itemPackageType.getWarehouseName()
        ));


        itemUnitOfMeasures.add(createItemUnitOfMeasure(

                unitOfMeasurePL, 10.0, 20.0, 10.0,
                2000, 2000.0, itemPackageType.getWarehouseName()
        ));

        return itemUnitOfMeasures;
    }

    private ItemUnitOfMeasure createItemUnitOfMeasure(UnitOfMeasure unitOfMeasure,
                                                      Double length, Double width, Double height,
                                                      Integer quantity, Double weight, String warehouseName){
        ItemUnitOfMeasure itemUnitOfMeasure = new ItemUnitOfMeasure();
        itemUnitOfMeasure.setUnitOfMeasureId(unitOfMeasure.getId());
        // itemUnitOfMeasure.setWarehouseId(warehouseId);
        itemUnitOfMeasure.setWarehouseName(warehouseName);
        itemUnitOfMeasure.setLength(length);
        itemUnitOfMeasure.setWidth(width);
        itemUnitOfMeasure.setHeight(height);
        itemUnitOfMeasure.setQuantity(quantity);
        itemUnitOfMeasure.setWeight(weight);
        return itemUnitOfMeasure;

    }
}
