package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.clients.KafkaReceiver;
import com.garyzhangscm.cwms.inventory.model.Item;
import com.garyzhangscm.cwms.inventory.model.ItemPackageType;
import com.garyzhangscm.cwms.inventory.model.ItemUnitOfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationService.class);
    @Autowired
    ItemUnitOfMeasureService itemUnitOfMeasureService;

    @Autowired
    ItemPackageTypeService itemPackageTypeService;

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
