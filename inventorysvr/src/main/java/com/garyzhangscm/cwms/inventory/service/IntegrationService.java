package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.clients.KafkaSender;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class IntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationService.class);
    @Autowired
    ItemUnitOfMeasureService itemUnitOfMeasureService;

    @Autowired
    ItemPackageTypeService itemPackageTypeService;

    @Autowired
    private KafkaSender kafkaSender;

    @Autowired
    ItemService itemService;
    @Autowired
    ItemFamilyService itemFamilyService;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    // Add/ change item
    public void process(Item item) {
        // integration data may have
        // -- item
        // -- item family
        // -- item package type
        // -- item unit of measure

        if (Objects.nonNull(item.getItemFamily())) {
            ItemFamily savedItemFamily =
                    itemFamilyService.saveOrUpdate(item.getItemFamily());
            item.setItemFamily(savedItemFamily);
        }
        // Setup all the data so we can save the
        // one to many relationship all at once
        item.getItemPackageTypes().forEach(itemPackageType -> {
            itemPackageType.getItemUnitOfMeasures().forEach(itemUnitOfMeasure ->
                    itemUnitOfMeasure.setItemPackageType(itemPackageType));
            itemPackageType.setItem(item);
        });
        Item savedItem = itemService.saveOrUpdate(item);
        logger.debug(">> item information saved!");



    }

    // Add/ change item family
    public void process(ItemFamily itemFamily) {

        ItemFamily savedItemFamily =
                    itemFamilyService.saveOrUpdate(itemFamily);

        logger.debug(">> item family information saved!");
    }

    // Add/ change item unit of measure
    public void process(Item item, ItemUnitOfMeasure itemUnitOfMeasure) {
        if (itemUnitOfMeasure.getItemPackageType() == null) {
            logger.debug("Start to find item package type with key, {} / {}, {}",
                    item.getWarehouseId(), item.getItemPackageTypes().get(0).getName(), item.getName());
            ItemPackageType itemPackageType =
                    itemPackageTypeService.findByNaturalKeys(
                            item.getWarehouseId(), item.getItemPackageTypes().get(0).getName(), item.getName());
            logger.debug("get itemPackageType: {}",itemPackageType);
            itemUnitOfMeasure.setItemPackageType(itemPackageType);
        }

        itemUnitOfMeasureService.saveOrUpdate(itemUnitOfMeasure);

    }

    public void processInventoryAdjustment(Inventory inventory, Long originalQuantity, Long newQuantity) {
        Warehouse warehouse = inventory.getWarehouse();
        if (Objects.isNull(warehouse)) {
            warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(inventory.getWarehouseId());
        }
        processInventoryAdjustment(warehouse, inventory, originalQuantity, newQuantity);

    }
    public void processInventoryAdjustment(Warehouse warehouse, Inventory inventory, Long originalQuantity, Long newQuantity) {


        InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation =
                new InventoryAdjustmentConfirmation(warehouse, inventory, originalQuantity, newQuantity);

        logger.debug("Will send inventory adjust confirmation\n {}", inventoryAdjustmentConfirmation);
        kafkaSender.send(inventoryAdjustmentConfirmation);

    }
}
