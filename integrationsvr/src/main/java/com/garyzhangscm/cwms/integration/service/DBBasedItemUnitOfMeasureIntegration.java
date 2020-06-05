package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;

import com.garyzhangscm.cwms.integration.repository.DBBasedItemUnitOfMeasureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DBBasedItemUnitOfMeasureIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItemUnitOfMeasureIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedItemUnitOfMeasureRepository dbBasedItemUnitOfMeasureRepository;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;

    public List<DBBasedItemUnitOfMeasure> findAll() {
        return dbBasedItemUnitOfMeasureRepository.findAll();
    }
    public DBBasedItemUnitOfMeasure findById(Long id) {
        return dbBasedItemUnitOfMeasureRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("item unit of measure data not found by id: " + id));
    }

    public IntegrationItemUnitOfMeasureData addIntegrationItemUnitOfMeasureData(DBBasedItemUnitOfMeasure dbBasedItemUnitOfMeasure) {

        return dbBasedItemUnitOfMeasureRepository.save(dbBasedItemUnitOfMeasure);
    }



    private List<DBBasedItemUnitOfMeasure> findPendingIntegration() {
        return dbBasedItemUnitOfMeasureRepository.findAll(
                (Root<DBBasedItemUnitOfMeasure> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    private DBBasedItemUnitOfMeasure save(DBBasedItemUnitOfMeasure dbBasedItem) {
        return dbBasedItemUnitOfMeasureRepository.save(dbBasedItem);
    }

    public void listen() {
        logger.debug("Start to process Item Unit Of Measure data");
        List<DBBasedItemUnitOfMeasure> dbBasedItemUnitOfMeasures = findPendingIntegration();
        logger.debug(">> get {}  Item Unit Of Measure data to be processed", dbBasedItemUnitOfMeasures.size());
        dbBasedItemUnitOfMeasures.forEach(dbBasedItemUnitOfMeasure -> process(dbBasedItemUnitOfMeasure));
    }

    private void process(DBBasedItemUnitOfMeasure dbBasedItemUnitOfMeasure) {

        try {
            Long warehouseId
                    = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                    dbBasedItemUnitOfMeasure.getWarehouseName()
            ).getId();
            Item item = new Item();
            item.setName(dbBasedItemUnitOfMeasure.getItemName());
            item.setWarehouseId(warehouseId);

            ItemPackageType itemPackageType = new ItemPackageType();
            itemPackageType.setWarehouseId(warehouseId);
            itemPackageType.setName(dbBasedItemUnitOfMeasure.getItemPackageTypeName());

            item.addItemPackageType(itemPackageType);

            logger.debug(">> will process Item:\n{}", item);

            ItemUnitOfMeasure itemUnitOfMeasure = dbBasedItemUnitOfMeasure.convertToItemUnitOfMeasure(inventoryServiceRestemplateClient,
                    commonServiceRestemplateClient,
                    warehouseLayoutServiceRestemplateClient);
            itemUnitOfMeasure.setWarehouseId(warehouseId);

            if (itemUnitOfMeasure.getUnitOfMeasureId() == null) {
                UnitOfMeasure unitOfMeasure
                        = commonServiceRestemplateClient.getUnitOfMeasureByName(
                                dbBasedItemUnitOfMeasure.getUnitOfMeasureName());
                logger.debug("Get id {} from unit of measure name: {}",
                        unitOfMeasure.getId(), dbBasedItemUnitOfMeasure.getUnitOfMeasureName());
                itemUnitOfMeasure.setUnitOfMeasureId(unitOfMeasure.getId());

            }

            kafkaSender.send(IntegrationType.INTEGRATION_ITEM_UNIT_OF_MEASURE, item, itemUnitOfMeasure);


            dbBasedItemUnitOfMeasure.setStatus(IntegrationStatus.COMPLETED);
            dbBasedItemUnitOfMeasure.setLastUpdateTime(LocalDateTime.now());
            dbBasedItemUnitOfMeasure = save(dbBasedItemUnitOfMeasure);

            logger.debug(">> Item Unit of Measure data process, {}", dbBasedItemUnitOfMeasure.getStatus());
        }
        catch (Exception ex) {
            dbBasedItemUnitOfMeasure.setStatus(IntegrationStatus.ERROR);
            dbBasedItemUnitOfMeasure.setLastUpdateTime(LocalDateTime.now());
            save(dbBasedItemUnitOfMeasure);
        }
    }



}
