package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.client.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.dblink.model.DBBasedItem;
import com.garyzhangscm.cwms.dblink.model.DBBasedItemPackageType;
import com.garyzhangscm.cwms.dblink.model.IntegrationStatus;
import com.garyzhangscm.cwms.dblink.repository.DBBasedItemPackageTypeRepository;
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
import java.util.stream.Collectors;

@Service
public class DBBasedItemPackageTypeService {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItemPackageTypeService.class);

    @Autowired
    DBBasedItemPackageTypeRepository dbBasedItemPackageTypeRepository;
    @Autowired
    IntegrationServiceRestemplateClient integrationServiceRestemplateClient;

    @Value("${integration.record.process.limit:100}")
    int recordLimit;

    // filter will be in the format of
    // foo1=bar1&foo2=bar2, the same format like URL parameters
    @Value("${integration.filter.itemPackageType:\"\"}")
    String filter;

    public List<DBBasedItemPackageType> findAll() {
        return dbBasedItemPackageTypeRepository.findAll();
    }

    private List<DBBasedItemPackageType> findPendingIntegration(String filter) {
        Pageable limit = PageRequest.of(0,recordLimit);

        logger.debug("start to find pending item package type integration by filter: {}",
                filter);

        Page<DBBasedItemPackageType> dbBasedItemPackageTypes =  dbBasedItemPackageTypeRepository.findAll(
                (Root<DBBasedItemPackageType> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

        return dbBasedItemPackageTypes.getContent();
    }

    private void save(DBBasedItemPackageType dbBasedItemPackageType) {
        dbBasedItemPackageTypeRepository.save(dbBasedItemPackageType);
    }

    public void sendIntegrationData() {
        List<DBBasedItemPackageType> pendingDBBasedItemPackageType = findPendingIntegration(filter);
        logger.debug("# find " +  pendingDBBasedItemPackageType.size() + " pendingDBBasedItemPackageType");

        AtomicReference<LocalDateTime> startProcessingDateTime = new AtomicReference<>(LocalDateTime.now());
        AtomicReference<LocalDateTime> lastProcessingDateTime = new AtomicReference<>(LocalDateTime.now());
        AtomicInteger i = new AtomicInteger();


        pendingDBBasedItemPackageType.forEach(
                dbBasedItemPackageType -> {
                    lastProcessingDateTime.set(LocalDateTime.now());
                    // logger.debug("# start to process item package type " + dbBasedItemPackageType);
                    String result = "";
                    String errorMessage = "";
                    try {
                        result = integrationServiceRestemplateClient.sendIntegrationData("item-package-type", dbBasedItemPackageType);
                        logger.debug("# get result " + result);
                        dbBasedItemPackageType.setStatus(IntegrationStatus.COMPLETED);
                        dbBasedItemPackageType.setErrorMessage("");
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        dbBasedItemPackageType.setStatus(IntegrationStatus.ERROR);
                        dbBasedItemPackageType.setErrorMessage(ex.getMessage());
                    }
                    save(dbBasedItemPackageType);
                    logger.debug("====> record {}, total processing time: {} millisecond(1/1000 second)",
                            i, ChronoUnit.MILLIS.between(lastProcessingDateTime.get(), LocalDateTime.now()));
                    i.getAndIncrement();
                }
        );
        logger.debug("====> total processing time for {} pendingDBBasedItemPackageType: {} millisecond(1/1000 second)",
                pendingDBBasedItemPackageType.size(),
                ChronoUnit.MILLIS.between(startProcessingDateTime.get(), LocalDateTime.now()));
    }



}
