package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedInventoryAdjustmentConfirmationRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedInventoryAttributeChangeConfirmationRepository;
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
public class DBBasedInventoryAttributeChangeConfirmationIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedInventoryAttributeChangeConfirmationIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedInventoryAttributeChangeConfirmationRepository dbBasedInventoryAttributeChangeConfirmationRepository;



    public List<DBBasedInventoryAttributeChangeConfirmation> findAll(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {

        return dbBasedInventoryAttributeChangeConfirmationRepository.findAll(
                (Root<DBBasedInventoryAttributeChangeConfirmation> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdTime"), endTime));

                    }
                    logger.debug(">> Date is passed in {}", date);
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

    public DBBasedInventoryAttributeChangeConfirmation findById(Long id) {
        return dbBasedInventoryAttributeChangeConfirmationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("client data not found by id: " + id));
    }

    private DBBasedInventoryAttributeChangeConfirmation save(DBBasedInventoryAttributeChangeConfirmation dbBasedInventoryAdjustmentConfirmation) {
        return dbBasedInventoryAttributeChangeConfirmationRepository.save(dbBasedInventoryAdjustmentConfirmation);
    }

    public IntegrationInventoryAttributeChangeConfirmationData sendInventoryAdjustmentConfirmationData(InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation) {

        // Convert inventoryAdjustmentConfirmation to integration data
        DBBasedInventoryAttributeChangeConfirmation dbBasedInventoryAttributeChangeConfirmation =
                getDBBasedInventoryAttributeChangeConfirmation(inventoryAttributeChangeConfirmation);

        dbBasedInventoryAttributeChangeConfirmation.setStatus(IntegrationStatus.COMPLETED);
        return save(dbBasedInventoryAttributeChangeConfirmation);
    }

    private DBBasedInventoryAttributeChangeConfirmation getDBBasedInventoryAttributeChangeConfirmation(InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation) {
        return new DBBasedInventoryAttributeChangeConfirmation(inventoryAttributeChangeConfirmation);
    }
}
