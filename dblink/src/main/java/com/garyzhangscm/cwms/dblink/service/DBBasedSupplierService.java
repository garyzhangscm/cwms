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

    // filter will be in the format of
    // foo1=bar1&foo2=bar2, the same format like URL parameters
    @Value("${integration.filter.supplier:\"\"}")
    String filter;


    public List<DBBasedSupplier> findAll() {
        return dbBasedSupplierRepository.findAll();
    }



    private List<DBBasedSupplier> findPendingIntegration(String filter) {
        Pageable limit = PageRequest.of(0,recordLimit);

        logger.debug("start to find pending supplier integration by filter: {}",
                filter);

        Page<DBBasedSupplier> dbBasedSuppliers =  dbBasedSupplierRepository.findAll(
                (Root<DBBasedSupplier> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    if (Strings.isNotBlank(filter)) {
                        String[] parameters = filter.split("&");
                        for(String parameter : parameters) {
                            String[] nameValue = parameter.split("=");
                            if (nameValue.length != 2) {
                                continue;
                            }
                            logger.debug("apply filter {} = {} to find pending item integration",
                                    nameValue[0], nameValue[1]);

                            predicates.add(criteriaBuilder.equal(root.get(nameValue[0]), nameValue[1]));
                        }
                    }

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
        List<DBBasedSupplier> pendingDBBasedSuppliers = findPendingIntegration(filter);
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
                         dbBasedSupplier.setStatus(IntegrationStatus.COMPLETED);
                         dbBasedSupplier.setErrorMessage("");
                     }
                     catch (Exception ex) {
                         ex.printStackTrace();
                         dbBasedSupplier.setStatus(IntegrationStatus.ERROR);
                         dbBasedSupplier.setErrorMessage(ex.getMessage());
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
