package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.client.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.dblink.model.DBBasedOrder;
import com.garyzhangscm.cwms.dblink.model.IntegrationStatus;
import com.garyzhangscm.cwms.dblink.repository.DBBasedOrderRepository;
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
public class DBBasedOrderService {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedOrderService.class);

    @Autowired
    DBBasedOrderRepository dbBasedOrderRepository;
    @Autowired
    IntegrationServiceRestemplateClient integrationServiceRestemplateClient;

    @Value("${integration.record.process.limit:100}")
    int recordLimit;

    // filter will be in the format of
    // foo1=bar1&foo2=bar2, the same format like URL parameters
    @Value("${integration.filter.order:\"\"}")
    String filter;

    public List<DBBasedOrder> findAll() {
        return dbBasedOrderRepository.findAll();
    }



    private List<DBBasedOrder> findPendingIntegration(String filter) {
        Pageable limit = PageRequest.of(0,recordLimit);

        logger.debug("start to find pending receipt integration by filter: {}",
                filter);

        Page<DBBasedOrder> dbBasedOrders =  dbBasedOrderRepository.findAll(
                (Root<DBBasedOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    if (Strings.isNotBlank(filter)) {
                        String[] parameters = filter.split("&");
                        for(String parameter : parameters) {
                            String[] nameValue = parameter.split("=");
                            if (nameValue.length != 2) {
                                continue;
                            }
                            logger.debug("apply filter {} = {} to find pending order integration",
                                    nameValue[0], nameValue[1]);

                            predicates.add(criteriaBuilder.equal(root.get(nameValue[0]), nameValue[1]));
                        }
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                limit
        );

        return dbBasedOrders.getContent();
    }

    private void save(DBBasedOrder dbBasedOrder) {
        dbBasedOrderRepository.save(dbBasedOrder);
    }

    public void sendIntegrationData() {
        List<DBBasedOrder> pendingDBBasedOrders = findPendingIntegration(filter);
        logger.debug("# find " +  pendingDBBasedOrders.size() + " pendingDBBasedOrders");
        AtomicReference<LocalDateTime> startProcessingDateTime = new AtomicReference<>(LocalDateTime.now());
        AtomicReference<LocalDateTime> lastProcessingDateTime = new AtomicReference<>(LocalDateTime.now());
        AtomicInteger i = new AtomicInteger();
        pendingDBBasedOrders.forEach(
                dbBasedOrder -> {
                    lastProcessingDateTime.set(LocalDateTime.now());
                    logger.debug("# {} start to process order {}", i, dbBasedOrder.getNumber());
                    logger.debug("======   order information ======");
                    logger.debug(dbBasedOrder.toString());
                    String result = "";
                    String errorMessage = "";
                     try {
                         result = integrationServiceRestemplateClient.sendIntegrationData("orders", dbBasedOrder);
                         logger.debug("# get result " + result);
                         dbBasedOrder.setStatus(IntegrationStatus.COMPLETED);
                         dbBasedOrder.setErrorMessage("");
                     }
                     catch (Exception ex) {
                         ex.printStackTrace();
                         dbBasedOrder.setStatus(IntegrationStatus.ERROR);
                         dbBasedOrder.setErrorMessage(ex.getMessage());
                     }

                     save(dbBasedOrder);
                     logger.debug("====> record {}, total processing time: {} millisecond(1/1000 second)",
                             i, ChronoUnit.MILLIS.between(lastProcessingDateTime.get(), LocalDateTime.now()));
                     i.getAndIncrement();
                }
        );

        logger.debug("====> total processing time for {} pendingDBBasedOrders: {} millisecond(1/1000 second)",
               pendingDBBasedOrders.size(),
                ChronoUnit.MILLIS.between(startProcessingDateTime.get(), LocalDateTime.now()));
    }


}
