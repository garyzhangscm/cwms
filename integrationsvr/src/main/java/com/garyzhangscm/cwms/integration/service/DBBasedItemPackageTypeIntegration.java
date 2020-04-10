package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedItemPackageTypeRepository;
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
public class DBBasedItemPackageTypeIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItemPackageTypeIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedItemPackageTypeRepository dbBasedItemPackageTypeRepository;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public List<DBBasedItemPackageType> findAll() {
        return dbBasedItemPackageTypeRepository.findAll();
    }
    public DBBasedItemPackageType findById(Long id) {
        return dbBasedItemPackageTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("item package type data not found by id: " + id));
    }

    public IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(DBBasedItemPackageType dbBasedItemPackageType) {

        return dbBasedItemPackageTypeRepository.save(dbBasedItemPackageType);
    }

    private List<DBBasedItemPackageType> findPendingIntegration() {
        return dbBasedItemPackageTypeRepository.findAll(
                (Root<DBBasedItemPackageType> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    private DBBasedItemPackageType save(DBBasedItemPackageType dbBasedItemPackageType) {
        return dbBasedItemPackageTypeRepository.save(dbBasedItemPackageType);
    }

    public void listen() {
        logger.debug("Start to process Item Package Type data");
        List<DBBasedItemPackageType> dbBasedItemPackageTypes = findPendingIntegration();
        logger.debug(">> get {} Item Package Type data to be processed", dbBasedItemPackageTypes.size());
        dbBasedItemPackageTypes.forEach(dbBasedItemPackageType -> process(dbBasedItemPackageType));
    }

    private void process(DBBasedItemPackageType dbBasedItemPackageType) {

        ItemPackageType itemPackageType = dbBasedItemPackageType.convertToItemPackageType();
        // Item item = getItemFromDatabase(dbBasedItem);
        logger.debug(">> will process Item Package Type:\n{}", itemPackageType);

        kafkaSender.send(itemPackageType);


        dbBasedItemPackageType.setStatus(IntegrationStatus.COMPLETED);
        dbBasedItemPackageType.setLastUpdateTime(LocalDateTime.now());
        dbBasedItemPackageType = save(dbBasedItemPackageType);

        logger.debug(">> Item Package Type data process, {}", dbBasedItemPackageType.getStatus());
    }




}
