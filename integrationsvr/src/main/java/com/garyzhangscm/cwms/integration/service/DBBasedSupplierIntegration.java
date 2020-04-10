package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedClientRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedSupplierRepository;
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
public class DBBasedSupplierIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedSupplierIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedSupplierRepository dbBasedSupplierRepository;

    public List<DBBasedSupplier> findAll() {
        return dbBasedSupplierRepository.findAll();
    }
    public DBBasedSupplier findById(Long id) {
        return dbBasedSupplierRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("supplier data not found by id: " + id));
    }

    public IntegrationSupplierData addIntegrationSupplierData(DBBasedSupplier dbBasedSupplier) {

        return dbBasedSupplierRepository.save(dbBasedSupplier);
    }


    private List<DBBasedSupplier> findPendingIntegration() {
        return dbBasedSupplierRepository.findAll(
                (Root<DBBasedSupplier> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    private DBBasedSupplier save(DBBasedSupplier dbBasedSupplier) {
        return dbBasedSupplierRepository.save(dbBasedSupplier);
    }

    public void listen() {
        logger.debug("Start to process supplier data");
        List<DBBasedSupplier> dbBasedSuppliers = findPendingIntegration();
        logger.debug(">> get {} supplier data to be processed", dbBasedSuppliers.size());
        dbBasedSuppliers.forEach(dbBasedSupplier -> process(dbBasedSupplier));
    }

    private void process(DBBasedSupplier dbBasedSupplier) {
        Supplier supplier = dbBasedSupplier.convertToSupplier();
        logger.debug(">> will process Supplier :\n{}", supplier);

        kafkaSender.send(supplier);


        dbBasedSupplier.setStatus(IntegrationStatus.COMPLETED);
        dbBasedSupplier.setLastUpdateTime(LocalDateTime.now());
        dbBasedSupplier = save(dbBasedSupplier);

        logger.debug(">> customer data process, {}", dbBasedSupplier.getStatus());
    }
}
