package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedOrderConfirmationRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedReceiptConfirmationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DBBasedReceiptConfirmationIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedReceiptConfirmationIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedReceiptConfirmationRepository dbBasedReceiptConfirmationRepository;


    public List<DBBasedReceiptConfirmation> findAll(Long warehouseId, String warehouseName,
                                                    String number, Long clientId, String clientName,
                                                    Long supplierId, String supplierName) {

        return dbBasedReceiptConfirmationRepository.findAll(
                (Root<DBBasedReceiptConfirmation> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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
                    if (Objects.nonNull(clientId)) {
                        predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
                    }
                    if (StringUtils.isNotBlank(clientName)) {
                        predicates.add(criteriaBuilder.equal(root.get("clientName"), clientName));
                    }
                    if (Objects.nonNull(supplierId)) {
                        predicates.add(criteriaBuilder.equal(root.get("supplierId"), supplierId));
                    }
                    if (StringUtils.isNotBlank(supplierName)) {
                        predicates.add(criteriaBuilder.equal(root.get("supplierName"), supplierName));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

    }
    public DBBasedReceiptConfirmation findById(Long id) {
        return dbBasedReceiptConfirmationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("receipt confirmation data not found by id: " + id));
    }

    private DBBasedReceiptConfirmation save(DBBasedReceiptConfirmation dbBasedReceiptConfirmation) {
        return dbBasedReceiptConfirmationRepository.save(dbBasedReceiptConfirmation);
    }

    public IntegrationReceiptConfirmationData sendIntegrationReceiptConfirmationData(ReceiptConfirmation receiptConfirmation){

        // Convert receiptConfirmation to integration data
        DBBasedReceiptConfirmation dbBasedReceiptConfirmation =
                getDBBasedReceiptConfirmation(receiptConfirmation);

        return save(dbBasedReceiptConfirmation);
    }

    private DBBasedReceiptConfirmation getDBBasedReceiptConfirmation(ReceiptConfirmation receiptConfirmation) {
        return new DBBasedReceiptConfirmation(receiptConfirmation);
    }
}