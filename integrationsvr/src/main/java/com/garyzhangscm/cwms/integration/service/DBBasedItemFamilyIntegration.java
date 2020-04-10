package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedItemFamilyRepository;
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
public class DBBasedItemFamilyIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItemFamilyIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedItemFamilyRepository dbBasedItemFamilyRepository;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    public List<DBBasedItemFamily> findAll() {
        return dbBasedItemFamilyRepository.findAll();
    }
    public DBBasedItemFamily findById(Long id) {
        return dbBasedItemFamilyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("item data not found by id: " + id));
    }

    public IntegrationItemFamilyData addIntegrationItemFamilyData(DBBasedItemFamily dbBasedItemFamily) {

        return dbBasedItemFamilyRepository.save(dbBasedItemFamily);
    }


    private List<DBBasedItemFamily> findPendingIntegration() {
        return dbBasedItemFamilyRepository.findAll(
                (Root<DBBasedItemFamily> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    private DBBasedItemFamily save(DBBasedItemFamily dbBasedItemFamily) {
        return dbBasedItemFamilyRepository.save(dbBasedItemFamily);
    }

    public void listen() {
        logger.debug("Start to process Item family data");
        List<DBBasedItemFamily> dbBasedItemFamilies = findPendingIntegration();
        logger.debug(">> get {} Item family data to be processed", dbBasedItemFamilies.size());
        dbBasedItemFamilies.forEach(dbBasedItemFamily -> process(dbBasedItemFamily));
    }

    private void process(DBBasedItemFamily dbBasedItemFamily) {

        ItemFamily itemFamily = dbBasedItemFamily.convertToItemFamily();
        // Item item = getItemFromDatabase(dbBasedItem);
        logger.debug(">> will process Item Family:\n{}", itemFamily);

        kafkaSender.send(itemFamily);


        dbBasedItemFamily.setStatus(IntegrationStatus.COMPLETED);
        dbBasedItemFamily.setLastUpdateTime(LocalDateTime.now());
        dbBasedItemFamily = save(dbBasedItemFamily);

        logger.debug(">> Item family data process, {}", dbBasedItemFamily.getStatus());
    }




}
