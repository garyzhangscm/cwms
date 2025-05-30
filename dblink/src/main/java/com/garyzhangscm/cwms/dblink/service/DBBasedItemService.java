package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.client.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.dblink.model.DBBasedItem;
import com.garyzhangscm.cwms.dblink.model.IntegrationStatus;
import com.garyzhangscm.cwms.dblink.repository.DBBasedItemRepository;
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
public class DBBasedItemService {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItemService.class);

    @Autowired
    DBBasedItemRepository dbBasedItemRepository;
    @Autowired
    IntegrationServiceRestemplateClient integrationServiceRestemplateClient;
    @Value("${integration.record.process.limit:100}")
    int recordLimit;

    // filter will be in the format of
    // foo1=bar1&foo2=bar2, the same format like URL parameters
    @Value("${integration.filter.item:\"\"}")
    String filter;


    public List<DBBasedItem> findAll() {
        return dbBasedItemRepository.findAll();
    }

    private List<DBBasedItem> findPendingIntegration(String filter) {
        Pageable limit = PageRequest.of(0,recordLimit);

        logger.debug("start to find pending item integration by filter: {}",
                filter);
        Page<DBBasedItem> dbBasedItemPage = dbBasedItemRepository.findAll(
                (Root<DBBasedItem> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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
        return dbBasedItemPage.getContent();

    }

    private void save(DBBasedItem dbBasedItem) {
        dbBasedItemRepository.save(dbBasedItem);
    }

    public void sendIntegrationData() {
        List<DBBasedItem> pendingDBBasedItem = findPendingIntegration(filter);
        logger.debug("# find " +  pendingDBBasedItem.size() + " pendingDBBasedItem");

        AtomicReference<LocalDateTime> startProcessingDateTime = new AtomicReference<>(LocalDateTime.now());
        AtomicReference<LocalDateTime> lastProcessingDateTime = new AtomicReference<>(LocalDateTime.now());
        AtomicInteger i = new AtomicInteger();

        pendingDBBasedItem.forEach(
                dbBasedItem -> {
                    lastProcessingDateTime.set(LocalDateTime.now());
                    logger.debug("# start to process item " + dbBasedItem.getName());

                    // logger.debug("===========  Item  Content   ===============\n {} ", dbBasedItem);
                    String result = "";
                    String errorMessage = "";
                    try {
                        result = integrationServiceRestemplateClient.sendIntegrationData("items", dbBasedItem);
                        logger.debug("# get result " + result);
                        dbBasedItem.setStatus(IntegrationStatus.COMPLETED);
                        dbBasedItem.setErrorMessage("");

                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        dbBasedItem.setStatus(IntegrationStatus.ERROR);
                        dbBasedItem.setErrorMessage(ex.getMessage());
                    }

                    save(dbBasedItem);
                    logger.debug("====> record {}, total processing time: {} millisecond(1/1000 second)",
                            i, ChronoUnit.MILLIS.between(lastProcessingDateTime.get(), LocalDateTime.now()));
                    i.getAndIncrement();
                }
        );
        logger.debug("====> total processing time for {} pendingDBBasedItem: {} millisecond(1/1000 second)",
                pendingDBBasedItem.size(),
                ChronoUnit.MILLIS.between(startProcessingDateTime.get(), LocalDateTime.now()));
    }

    public boolean testLive() {
        long totalCount = dbBasedItemRepository.count();
        logger.debug("Test probe in DBBasedItemService, total count of DBBasedItem record: {}", totalCount);
        return true;
    }


}
