package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.HostRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedInventoryAdjustmentConfirmationRepository;
import org.apache.commons.lang.StringUtils;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class DBBasedInventoryAdjustmentConfirmationIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedInventoryAdjustmentConfirmationIntegration.class);

    @Autowired
    DBBasedInventoryAdjustmentConfirmationRepository dbBasedInventoryAdjustmentConfirmationRepository;
    @Autowired
    HostRestemplateClient hostRestemplateClient;

    @Value("${integration.record.process.limit:100}")
    int recordLimit;

    private List<DBBasedInventoryAdjustmentConfirmation> findPendingIntegration() {
        Pageable limit = PageRequest.of(0,recordLimit);

        Page<DBBasedInventoryAdjustmentConfirmation> dbBasedInventoryAdjustmentConfirmations
                = dbBasedInventoryAdjustmentConfirmationRepository.findAll(
                (Root<DBBasedInventoryAdjustmentConfirmation> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                limit
        );
        return dbBasedInventoryAdjustmentConfirmations.getContent();
    }


    public List<DBBasedInventoryAdjustmentConfirmation> findAll(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {

        return dbBasedInventoryAdjustmentConfirmationRepository.findAll(
                (Root<DBBasedInventoryAdjustmentConfirmation> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("insertTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("insertTime"), endTime));

                    }
                    logger.debug(">> Date is passed in {}", date);
                    if (Objects.nonNull(date)) {
                        LocalDateTime dateStartTime = date.atTime(0, 0, 0, 0);
                        LocalDateTime dateEndTime = date.atTime(23, 59, 59, 999999999);
                        predicates.add(criteriaBuilder.between(
                                root.get("insertTime"), dateStartTime, dateEndTime));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }
    public DBBasedInventoryAdjustmentConfirmation findById(Long id) {
        return dbBasedInventoryAdjustmentConfirmationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("client data not found by id: " + id));
    }

    private DBBasedInventoryAdjustmentConfirmation save(DBBasedInventoryAdjustmentConfirmation dbBasedInventoryAdjustmentConfirmation) {
        return dbBasedInventoryAdjustmentConfirmationRepository.save(dbBasedInventoryAdjustmentConfirmation);
    }

    public IntegrationInventoryAdjustmentConfirmationData saveInventoryAdjustmentConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation) {

        // Convert inventoryAdjustmentConfirmation to integration data
        DBBasedInventoryAdjustmentConfirmation dbBasedInventoryAdjustmentConfirmation =
                getDBBasedInventoryAdjustmentConfirmation(inventoryAdjustmentConfirmation);

        dbBasedInventoryAdjustmentConfirmation.setStatus(IntegrationStatus.PENDING);
        dbBasedInventoryAdjustmentConfirmation.setInsertTime(LocalDateTime.now());
        dbBasedInventoryAdjustmentConfirmation.setLastUpdateTime(LocalDateTime.now());
        return save(dbBasedInventoryAdjustmentConfirmation);
    }

    private DBBasedInventoryAdjustmentConfirmation getDBBasedInventoryAdjustmentConfirmation(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation) {
        return new DBBasedInventoryAdjustmentConfirmation(inventoryAdjustmentConfirmation);
    }

    public void sendToHost() {
        List<DBBasedInventoryAdjustmentConfirmation> dbBasedInventoryAdjustmentConfirmations =
                findPendingIntegration();
        logger.debug("# find " +  dbBasedInventoryAdjustmentConfirmations.size() + " dbBasedInventoryAdjustmentConfirmations");

        dbBasedInventoryAdjustmentConfirmations.forEach(
                dbBasedInventoryAdjustmentConfirmation -> {
                    String result = "";
                    String errorMessage = "";
                    try {
                        result = hostRestemplateClient.sendIntegrationData("inventory-adjustment-confirmation", dbBasedInventoryAdjustmentConfirmation);
                        logger.debug("# get result " + result);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        result = "false";
                        errorMessage = ex.getMessage();
                    }
                    if (result.equalsIgnoreCase("success")) {
                        dbBasedInventoryAdjustmentConfirmation.setStatus(IntegrationStatus.COMPLETED);
                    }
                    else {
                        dbBasedInventoryAdjustmentConfirmation.setStatus(IntegrationStatus.ERROR);
                        dbBasedInventoryAdjustmentConfirmation.setErrorMessage(errorMessage);
                    }
                    save(dbBasedInventoryAdjustmentConfirmation);
                }
        );

    }
}
