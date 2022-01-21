package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedOrderConfirmationRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedWorkOrderConfirmationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DBBasedWorkOrderConfirmationIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedWorkOrderConfirmationIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedWorkOrderConfirmationRepository dbBasedWorkOrderConfirmationRepository;


    public List<DBBasedWorkOrderConfirmation> findAll(Long warehouseId, String warehouseName,
                                                      String number,
                                                      LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return dbBasedWorkOrderConfirmationRepository.findAll(
                (Root<DBBasedWorkOrderConfirmation> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }
                    if (StringUtils.isNotBlank(warehouseName)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseName"), warehouseName));
                    }
                    if (StringUtils.isNotBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));
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
                        LocalDateTime dateStartTime = date.atTime(0, 0, 0, 0);
                        LocalDateTime dateEndTime = date.atTime(23, 59, 59, 999999999);
                        predicates.add(criteriaBuilder.between(
                                root.get("createdTime"), dateStartTime, dateEndTime));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }
    public DBBasedWorkOrderConfirmation findById(Long id) {
        return dbBasedWorkOrderConfirmationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order confirmation data not found by id: " + id));
    }

    private DBBasedWorkOrderConfirmation save(DBBasedWorkOrderConfirmation dbBasedWorkOrderConfirmation) {
        return dbBasedWorkOrderConfirmationRepository.save(dbBasedWorkOrderConfirmation);
    }

    public IntegrationWorkOrderConfirmationData sendIntegrationWorkOrderConfirmationData(WorkOrderConfirmation workOrderConfirmation){


        // Convert inventoryAdjustmentConfirmation to integration data
        DBBasedWorkOrderConfirmation dbBasedWorkOrderConfirmation =
                getDBBasedWorkOrderConfirmation(workOrderConfirmation);


        return save(dbBasedWorkOrderConfirmation);
    }

    private DBBasedWorkOrderConfirmation getDBBasedWorkOrderConfirmation(WorkOrderConfirmation workOrderConfirmation) {
        return new DBBasedWorkOrderConfirmation(workOrderConfirmation);
    }


    public IntegrationWorkOrderConfirmationData resendWorkOrderConfirmationData(Long id) {
        DBBasedWorkOrderConfirmation dbBasedWorkOrderConfirmation =
                findById(id);
        dbBasedWorkOrderConfirmation.setStatus(IntegrationStatus.PENDING);
        dbBasedWorkOrderConfirmation.setErrorMessage("");
        return save(dbBasedWorkOrderConfirmation);
    }
}
