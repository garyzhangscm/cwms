package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedInventoryAdjustmentConfirmationRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedOrderConfirmationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DBBasedOrderConfirmationIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedOrderConfirmationIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedOrderConfirmationRepository dbBasedOrderConfirmationRepository;


    public List<DBBasedOrderConfirmation> findAll(Long warehouseId, String warehouseName,
                                                  String number) {
        return dbBasedOrderConfirmationRepository.findAll(
                (Root<DBBasedOrderConfirmation> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }
    public DBBasedOrderConfirmation findById(Long id) {
        return dbBasedOrderConfirmationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("order confirmation data not found by id: " + id));
    }

    private DBBasedOrderConfirmation save(DBBasedOrderConfirmation dbBasedOrderConfirmation) {
        return dbBasedOrderConfirmationRepository.save(dbBasedOrderConfirmation);
    }

    public IntegrationOrderConfirmationData sendIntegrationOrderConfirmationData(OrderConfirmation orderConfirmation){


        // Convert inventoryAdjustmentConfirmation to integration data
        DBBasedOrderConfirmation dbBasedOrderConfirmation =
                getDBBasedOrderConfirmation(orderConfirmation);

        dbBasedOrderConfirmation.setStatus(IntegrationStatus.COMPLETED);
        dbBasedOrderConfirmation.setInsertTime(LocalDateTime.now());
        dbBasedOrderConfirmation.setLastUpdateTime(LocalDateTime.now());
        return save(dbBasedOrderConfirmation);
    }

    private DBBasedOrderConfirmation getDBBasedOrderConfirmation(OrderConfirmation orderConfirmation) {
        return new DBBasedOrderConfirmation(orderConfirmation);
    }
}
