package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.client.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.dblink.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.dblink.model.DBBasedWorkOrder;
import com.garyzhangscm.cwms.dblink.model.IntegrationStatus;
import com.garyzhangscm.cwms.dblink.repository.DBBasedWorkOrderRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DBBasedWorkOrderService {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedWorkOrderService.class);

    @Autowired
    DBBasedWorkOrderRepository dbBasedWorkOrderRepository;
    @Autowired
    IntegrationServiceRestemplateClient integrationServiceRestemplateClient;

    @Value("${integration.record.process.limit:100}")
    int recordLimit;


    public DBBasedWorkOrder findById(Long id) {
        return dbBasedWorkOrderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order integration data not found by id: " + id));
    }

    public List<DBBasedWorkOrder> findAll(String companyCode,
                                          Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
                                          String statusList, Long id,
                                          int limit) {

        Pageable pageable = PageRequest.of(0, limit,
                Sort.by(Sort.Direction.DESC, "insertTime"));

        Page<DBBasedWorkOrder> dbBasedWorkOrders =  dbBasedWorkOrderRepository.findAll(
                (Root<DBBasedWorkOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();


                    if (Strings.isNotBlank(companyCode)) {
                        predicates.add(criteriaBuilder.equal(root.get("companyCode"), companyCode));


                    }
                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    }

                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdTime"), endTime));

                    }
                    if (Objects.nonNull(date)) {
                        LocalDateTime dateStartTime = date.atStartOfDay();
                        LocalDateTime dateEndTime = date.atStartOfDay().plusDays(1).minusSeconds(1);
                        predicates.add(criteriaBuilder.between(
                                root.get("createdTime"),
                                dateStartTime.atZone(ZoneOffset.UTC), dateEndTime.atZone(ZoneOffset.UTC)));

                    }

                    if (Strings.isNotBlank(statusList)) {
                        CriteriaBuilder.In<IntegrationStatus> inStatus = criteriaBuilder.in(root.get("status"));
                        for(String status : statusList.split(",")) {
                            inStatus.value(IntegrationStatus.valueOf(status));
                        }
                        predicates.add(criteriaBuilder.and(inStatus));
                    }

                    if (Objects.nonNull(id)) {
                        predicates.add(criteriaBuilder.equal(
                                root.get("id"), id));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                pageable

        );
        return dbBasedWorkOrders.getContent();
    }


    private List<DBBasedWorkOrder> findPendingIntegration() {
        Pageable limit = PageRequest.of(0,recordLimit);

        Page<DBBasedWorkOrder> dbBasedWorkOrders =  dbBasedWorkOrderRepository.findAll(
                (Root<DBBasedWorkOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                limit
        );

        return dbBasedWorkOrders.getContent();
    }

    private DBBasedWorkOrder save(DBBasedWorkOrder dbBasedWorkOrder) {
        return dbBasedWorkOrderRepository.save(dbBasedWorkOrder);
    }

    public void sendIntegrationData() {
        List<DBBasedWorkOrder> pendingDBBasedWorkOrder = findPendingIntegration();
        logger.debug("# find " +  pendingDBBasedWorkOrder.size() + " pendingDBBasedWorkOrder");
        AtomicReference<LocalDateTime> startProcessingDateTime = new AtomicReference<>(LocalDateTime.now());
        AtomicReference<LocalDateTime> lastProcessingDateTime = new AtomicReference<>(LocalDateTime.now());
        AtomicInteger i = new AtomicInteger();
        pendingDBBasedWorkOrder.forEach(
                dbBasedWorkOrder -> {
                    lastProcessingDateTime.set(LocalDateTime.now());
                    logger.debug("# {} start to process work order {}", i, dbBasedWorkOrder.getNumber());
                    String result = "";
                    String errorMessage = "";
                     try {
                         result = integrationServiceRestemplateClient.sendIntegrationData("work-orders", dbBasedWorkOrder);
                         logger.debug("# get result " + result);
                     }
                     catch (Exception ex) {
                         ex.printStackTrace();
                         result = "false";
                         errorMessage = ex.getMessage();
                     }
                     if (result.equalsIgnoreCase("success")) {
                         dbBasedWorkOrder.setStatus(IntegrationStatus.COMPLETED);
                     }
                     else {
                         dbBasedWorkOrder.setStatus(IntegrationStatus.ERROR);
                     }
                     save(dbBasedWorkOrder);
                     logger.debug("====> record {}, total processing time: {} millisecond(1/1000 second)",
                             i, ChronoUnit.MILLIS.between(lastProcessingDateTime.get(), LocalDateTime.now()));
                     i.getAndIncrement();
                }
        );

        logger.debug("====> total processing time for {} pendingDBBasedWorkOrder: {} millisecond(1/1000 second)",
               pendingDBBasedWorkOrder.size(),
                ChronoUnit.MILLIS.between(startProcessingDateTime.get(), LocalDateTime.now()));
    }

    public boolean testLive() {
        long totalCount = dbBasedWorkOrderRepository.count();
        logger.debug("Test probe in DBBasedWorkOrderService, total count of DBBasedWorkOrder record: {}", totalCount);
        return true;
    }

    public DBBasedWorkOrder resetStatus(Long id) {
        DBBasedWorkOrder dbBasedWorkOrder = findById(id);

        dbBasedWorkOrder.setStatus(IntegrationStatus.PENDING);
        dbBasedWorkOrder.setLastUpdateTime(LocalDateTime.now());
        dbBasedWorkOrder.setErrorMessage("");

        return save(dbBasedWorkOrder);
    }
}
