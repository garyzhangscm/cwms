package com.garyzhangscm.cwms.adminserver.model.tester;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.model.wms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TestIntegrationAdjustInventory extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestIntegrationAdjustInventory.class);

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;


    public TestIntegrationAdjustInventory() {
        super("Test Integration - Adjust Inventory Up");
    }
    public boolean run(Warehouse warehouse) {

        logger.debug("Start to run test scenario: {} - {}", warehouse.getName(), getName());
        try {
            List<IntegrationData> integrationDataList = createItem(warehouse);
            integrationDataList.forEach(integrationData -> {
                logger.debug("Created item integration data: {}", integrationData.getId());
            });
        } catch (Exception e) {
            e.printStackTrace();
            setErrorMessage(e.getMessage());
            return false;
        }

        return true;

    }

    private List<IntegrationData> createItem(Warehouse warehouse) throws JsonProcessingException {

        List<IntegrationData> integrationDataList = new ArrayList<>();
        for(int i = 1; i <= 5; i++) {
            String itemName = "ITEM-HV-00" + i;
            integrationDataList.add(createItem(warehouse, itemName, "HIGH_VALUE"));
            itemName = "ITEM-FZ-00" + i;
            integrationDataList.add(createItem(warehouse, itemName, "FROZEN"));
        }
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
