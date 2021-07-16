package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.clients.KafkaSender;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ItemException;
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
        itemService.saveOrUpdate(item);
        logger.debug(">> item information saved!");



    }

    // Add/ change item
    public void process(Item item,ItemPackageType itemPackageType) {

        Item matchedItem = null;
        if (Objects.nonNull(item.getId())) {
            matchedItem = itemService.findById(item.getId());
        }
        else {
            matchedItem = itemService.findByName(item.getWarehouseId(), item.getName());


        }
        if (Objects.isNull(matchedItem)) {
            throw ItemException.raiseException("Can't process item package type integration. " +
                    " We can't find matched item by id： " + item.getId() +
                    ", warehouse id: " + item.getWarehouseId() +
                    ", item name: " + item.getName());
        }
        itemPackageType.setItem(matchedItem);
        itemPackageTypeService.saveOrUpdate(itemPackageType);




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

    public void processInventoryAdjustment(InventoryQuantityChangeType inventoryQuantityChangeType,
                                           Inventory inventory, Long originalQuantity, Long newQuantity) {
        if (inventoryQuantityChangeType.equals(InventoryQuantityChangeType.RECEIVING) ||
                inventoryQuantityChangeType.equals(InventoryQuantityChangeType.CONSUME_MATERIAL)||
                inventoryQuantityChangeType.equals(InventoryQuantityChangeType.RETURN_MATERAIL)||
                inventoryQuantityChangeType.equals(InventoryQuantityChangeType.PRODUCING)||
                inventoryQuantityChangeType.equals(InventoryQuantityChangeType.PRODUCING_BY_PRODUCT)) {
            // when we are receiving inventory, produce inventory from work order, or
            // return material from a work order, we will not send integration data for individual
            // inventory. instead we will send integration data for the whole receipt
            return;
        }
        Warehouse warehouse = inventory.getWarehouse();
        if (Objects.isNull(warehouse)) {
            warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(inventory.getWarehouseId());
        }
        processInventoryAdjustment(inventoryQuantityChangeType, warehouse, inventory, originalQuantity, newQuantity);

    }
    public void processInventoryAdjustment(InventoryQuantityChangeType inventoryQuantityChangeType, Warehouse warehouse,
                                           Inventory inventory, Long originalQuantity, Long newQuantity) {


        if (inventoryQuantityChangeType.equals(InventoryQuantityChangeType.RECEIVING)) {
            // when we are receiving inventory, we will not send integration data for individual
            // inventory. instead we will send integration data for the whole receipt
            return;
        }
        InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation =
                new InventoryAdjustmentConfirmation(warehouse, inventory, originalQuantity, newQuantity);

        logger.debug("Will send inventory adjust confirmation\n ");
        kafkaSender.send(inventoryAdjustmentConfirmation);

    }
}
