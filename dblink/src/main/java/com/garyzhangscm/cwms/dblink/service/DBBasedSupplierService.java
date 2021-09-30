package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.client.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.dblink.model.DBBasedSupplier;
import com.garyzhangscm.cwms.dblink.model.DBBasedWorkOrder;
import com.garyzhangscm.cwms.dblink.model.IntegrationStatus;
import com.garyzhangscm.cwms.dblink.repository.DBBasedSupplierRepository;
import com.garyzhangscm.cwms.dblink.repository.DBBasedWorkOrderRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DBBasedSupplierService {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedSupplierService.class);

    @Autowired
    DBBasedSupplierRepository dbBasedSupplierRepository;
    @Autowired
    IntegrationServiceRestemplateClient integrationServiceRestemplateClient;

    @Value("${integration.record.process.limit:100}")
    int recordLimit;

    public List<DBBasedSupplier> findAll() {
        return dbBasedSupplierRepository.findAll();
    }



    private List<DBBasedSupplier> findPendingIntegration() {
        Pageable limit = PageRequest.of(0,recordLimit);

        Page<DBBasedSupplier> dbBasedSuppliers =  dbBasedSupplierRepository.findAll(
                (Root<DBBasedSupplier> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                limit
        );

        return dbBasedSuppliers.getContent();
    }

    private void save(DBBasedSupplier dbBasedSupplier) {
        dbBasedSupplierRepository.save(dbBasedSupplier);
    }

    public void sendIntegrationData() {
        List<DBBasedSupplier> pendingDBBasedSuppliers = findPendingIntegration();
        logger.debug("# find " +  pendingDBBasedSuppliers.size() + " pendingDBBasedSupplier");
        AtomicReference<LocalDateTime> startProcessingDateTime = new AtomicReference<>(LocalDateTime.now());
        AtomicReference<LocalDateTime> lastProcessingDateTime = new AtomicReference<>(LocalDateTime.now());
        AtomicInteger i = new AtomicInteger();
        pendingDBBasedSuppliers.forEach(
                dbBasedSupplier -> {
                    lastProcessingDateTime.set(LocalDateTime.now());
                    logger.debug("# {} start to process supplier {}", i, dbBasedSupplier.getName());


                    String result = "";
                    String errorMessage = "";
                     try {
                         result = integrationServiceRestemplateClient.sendIntegrationData("suppliers", dbBasedSupplier);
                         logger.debug("# get result " + result);
                     }
                     catch (Exception ex) {
                         ex.printStackTrace();
                         result = "false";
                         errorMessage = ex.getMessage();
                     }
                     if (result.equalsIgnoreCase("success")) {
                         dbBasedSupplier.setStatus(IntegrationStatus.COMPLETED);
                     }
                     else {
                         dbBasedSupplier.setStatus(IntegrationStatus.ERROR);
                     }
                     save(dbBasedSupplier);
                     logger.debug("====> record {}, total processing time: {} millisecond(1/1000 second)",
                             i, ChronoUnit.MILLIS.between(lastProcessingDateTime.get(), LocalDateTime.now()));
                     i.getAndIncrement();
                }
        );

        logger.debug("====> total processing time for {} pendingDBBasedSuppliers: {} millisecond(1/1000 second)",
                pendingDBBasedSuppliers.size(),
                ChronoUnit.MILLIS.between(startProcessingDateTime.get(), LocalDateTime.now()));
    }



}
