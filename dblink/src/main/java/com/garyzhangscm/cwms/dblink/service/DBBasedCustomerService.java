package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.client.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.dblink.model.DBBasedCustomer;
import com.garyzhangscm.cwms.dblink.model.DBBasedItem;
import com.garyzhangscm.cwms.dblink.model.IntegrationStatus;
import com.garyzhangscm.cwms.dblink.repository.DBBasedCustomerRepository;
import com.garyzhangscm.cwms.dblink.repository.DBBasedItemRepository;
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
public class DBBasedCustomerService {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedCustomerService.class);

    @Autowired
    DBBasedCustomerRepository dbBasedCustomerRepository;
    @Autowired
    IntegrationServiceRestemplateClient integrationServiceRestemplateClient;
    @Value("${integration.record.process.limit:100}")
    int recordLimit;


    public List<DBBasedCustomer> findAll() {
        return dbBasedCustomerRepository.findAll();
    }

    private List<DBBasedCustomer> findPendingIntegration() {
        Pageable limit = PageRequest.of(0,recordLimit);

        Page<DBBasedCustomer> dbBasedCustomerPage = dbBasedCustomerRepository.findAll(
                (Root<DBBasedCustomer> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                limit
        );
        return dbBasedCustomerPage.getContent();

    }

    private void save(DBBasedCustomer dbBasedCustomer) {
        dbBasedCustomerRepository.save(dbBasedCustomer);
    }

    public void sendIntegrationData() {
        List<DBBasedCustomer> pendingDBBasedCustomer = findPendingIntegration();
        logger.debug("# find " +  pendingDBBasedCustomer.size() + " pendingDBBasedCustomer");

        AtomicReference<LocalDateTime> startProcessingDateTime = new AtomicReference<>(LocalDateTime.now());
        AtomicReference<LocalDateTime> lastProcessingDateTime = new AtomicReference<>(LocalDateTime.now());
        AtomicInteger i = new AtomicInteger();

        pendingDBBasedCustomer.forEach(
                dbBasedCustomer -> {
                    lastProcessingDateTime.set(LocalDateTime.now());
                    logger.debug("# start to process Customer " + dbBasedCustomer.getName());

                    logger.debug("===========  Customer  Content   ===============\n {} ", dbBasedCustomer);
                    String result = "";
                    String errorMessage = "";
                    try {
                        result = integrationServiceRestemplateClient.sendIntegrationData("customer", dbBasedCustomer);
                        logger.debug("# get result " + result);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        result = "false";
                        errorMessage = ex.getMessage();
                    }
                    if (result.equalsIgnoreCase("success")) {
                        dbBasedCustomer.setStatus(IntegrationStatus.COMPLETED);
                    }
                    else {
                        dbBasedCustomer.setStatus(IntegrationStatus.ERROR);
                    }
                    save(dbBasedCustomer);
                    logger.debug("====> record {}, total processing time: {} millisecond(1/1000 second)",
                            i, ChronoUnit.MILLIS.between(lastProcessingDateTime.get(), LocalDateTime.now()));
                    i.getAndIncrement();
                }
        );
        logger.debug("====> total processing time for {} pendingDBBasedCustomer: {} millisecond(1/1000 second)",
                pendingDBBasedCustomer.size(),
                ChronoUnit.MILLIS.between(startProcessingDateTime.get(), LocalDateTime.now()));
    }

    public boolean testLive() {
        long totalCount = dbBasedCustomerRepository.count();
        logger.debug("Test probe in DBBasedCustomerService, total count of DBBasedCustomer record: {}", totalCount);
        return true;
    }



}
