package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.model.Item;
import com.garyzhangscm.cwms.inventory.model.ItemFamily;
import com.garyzhangscm.cwms.inventory.model.ItemPackageType;
import com.garyzhangscm.cwms.inventory.model.ItemUnitOfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    ItemService itemService;
    @Autowired
    ItemFamilyService itemFamilyService;

    // Add/ change item unit of measure
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
}
