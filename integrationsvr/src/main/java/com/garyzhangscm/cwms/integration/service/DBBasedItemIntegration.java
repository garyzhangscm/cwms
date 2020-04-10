package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DBBasedItemIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItemIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedItemRepository dbBasedItemRepository;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public List<DBBasedItem> findAll() {
        return dbBasedItemRepository.findAll();
    }
    public DBBasedItem findById(Long id) {
        return dbBasedItemRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("item data not found by id: " + id));
    }

    public IntegrationItemData addIntegrationItemData(DBBasedItem dbBasedItem) {

        return dbBasedItemRepository.save(dbBasedItem);
    }

    private List<DBBasedItem> findPendingIntegration() {
        return dbBasedItemRepository.findAll(
                (Root<DBBasedItem> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    private DBBasedItem save(DBBasedItem dbBasedItem) {
        return dbBasedItemRepository.save(dbBasedItem);
    }

    public void listen() {
        logger.debug("Start to process Item data");
        List<DBBasedItem> dbBasedItems = findPendingIntegration();
        logger.debug(">> get {} Item data to be processed", dbBasedItems.size());
        dbBasedItems.forEach(dbBasedItem -> process(dbBasedItem));
    }

    private void process(DBBasedItem dbBasedItem) {

        Item item = dbBasedItem.convertToItem();
        // Item item = getItemFromDatabase(dbBasedItem);
        logger.debug(">> will process Item:\n{}", item);

        kafkaSender.send(item);


        dbBasedItem.setStatus(IntegrationStatus.COMPLETED);
        dbBasedItem.setLastUpdateTime(LocalDateTime.now());
        dbBasedItem = save(dbBasedItem);

        logger.debug(">> Item data process, {}", dbBasedItem.getStatus());
    }




}
