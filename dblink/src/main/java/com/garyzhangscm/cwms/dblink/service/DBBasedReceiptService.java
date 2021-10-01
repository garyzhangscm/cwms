package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.client.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.dblink.model.DBBasedReceipt;
import com.garyzhangscm.cwms.dblink.model.DBBasedWorkOrder;
import com.garyzhangscm.cwms.dblink.model.IntegrationStatus;
import com.garyzhangscm.cwms.dblink.repository.DBBasedReceiptRepository;
import com.garyzhangscm.cwms.dblink.repository.DBBasedWorkOrderRepository;
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
public class DBBasedReceiptService {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedReceiptService.class);

    @Autowired
    DBBasedReceiptRepository dbBasedReceiptRepository;
    @Autowired
    IntegrationServiceRestemplateClient integrationServiceRestemplateClient;

    @Value("${integration.record.process.limit:100}")
    int recordLimit;

    public List<DBBasedReceipt> findAll() {
        return dbBasedReceiptRepository.findAll();
    }



    private List<DBBasedReceipt> findPendingIntegration() {
        Pageable limit = PageRequest.of(0,recordLimit);

        Page<DBBasedReceipt> dbBasedWorkOrders =  dbBasedReceiptRepository.findAll(
                (Root<DBBasedReceipt> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                limit
        );

        return dbBasedWorkOrders.getContent();
    }

    private void save(DBBasedReceipt dbBasedReceipt) {
        dbBasedReceiptRepository.save(dbBasedReceipt);
    }

    public void sendIntegrationData() {
        List<DBBasedReceipt> pendingDBBasedReceipts = findPendingIntegration();
        logger.debug("# find " +  pendingDBBasedReceipts.size() + " pendingDBBasedWorkOrder");
        AtomicReference<LocalDateTime> startProcessingDateTime = new AtomicReference<>(LocalDateTime.now());
        AtomicReference<LocalDateTime> lastProcessingDateTime = new AtomicReference<>(LocalDateTime.now());
        AtomicInteger i = new AtomicInteger();
        pendingDBBasedReceipts.forEach(
                dbBasedReceipt -> {
                    lastProcessingDateTime.set(LocalDateTime.now());
                    logger.debug("# {} start to process receipt {}", i, dbBasedReceipt.getNumber());
                    String result = "";
                    String errorMessage = "";
                     try {
                         result = integrationServiceRestemplateClient.sendIntegrationData("receipt", dbBasedReceipt);
                         logger.debug("# get result " + result);
                     }
                     catch (Exception ex) {
                         ex.printStackTrace();
                         result = "false";
                         errorMessage = ex.getMessage();
                     }
                     if (result.equalsIgnoreCase("success")) {
                         dbBasedReceipt.setStatus(IntegrationStatus.COMPLETED);
                     }
                     else {
                         dbBasedReceipt.setStatus(IntegrationStatus.ERROR);
                     }
                     save(dbBasedReceipt);
                     logger.debug("====> record {}, total processing time: {} millisecond(1/1000 second)",
                             i, ChronoUnit.MILLIS.between(lastProcessingDateTime.get(), LocalDateTime.now()));
                     i.getAndIncrement();
                }
        );

        logger.debug("====> total processing time for {} pendingDBBasedReceipts: {} millisecond(1/1000 second)",
               pendingDBBasedReceipts.size(),
                ChronoUnit.MILLIS.between(startProcessingDateTime.get(), LocalDateTime.now()));
    }


}
