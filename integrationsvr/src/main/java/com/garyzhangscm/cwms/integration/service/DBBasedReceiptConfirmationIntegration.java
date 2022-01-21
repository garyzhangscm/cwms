package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.HostRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedOrderConfirmationRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedReceiptConfirmationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.LocalDate;
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
    @Autowired
    HostRestemplateClient hostRestemplateClient;

    @Value("${integration.record.process.limit:100}")
    int recordLimit;

    private List<DBBasedReceiptConfirmation> findPendingIntegration() {
        Pageable limit = PageRequest.of(0,recordLimit);

        Page<DBBasedReceiptConfirmation> dbBasedReceiptConfirmations
                = dbBasedReceiptConfirmationRepository.findAll(
                (Root<DBBasedReceiptConfirmation> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                limit
        );
        return dbBasedReceiptConfirmations.getContent();
    }

    public List<DBBasedReceiptConfirmation> findAll(Long warehouseId, String warehouseName,
                                                    String number, Long clientId, String clientName,
                                                    Long supplierId, String supplierName,
                                                    LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {

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



    /**
     * Send to host's API endpoint, in case host's db is in a different network
     */
    public void sendToHost() {
        List<DBBasedReceiptConfirmation> dbBasedReceiptConfirmations =
                findPendingIntegration();
        logger.debug("# find " +  dbBasedReceiptConfirmations.size() + " dbBasedReceiptConfirmations");

        dbBasedReceiptConfirmations.forEach(
                dbBasedReceiptConfirmation -> {
                    String result = "";
                    String errorMessage = "";
                    try {
                        result = hostRestemplateClient.sendIntegrationData("receipt-confirmation", dbBasedReceiptConfirmation);
                        logger.debug("# get result " + result);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        result = "false";
                        errorMessage = ex.getMessage();
                    }
                    if (result.equalsIgnoreCase("success")) {
                        dbBasedReceiptConfirmation.setStatus(IntegrationStatus.COMPLETED);
                    }
                    else {
                        dbBasedReceiptConfirmation.setStatus(IntegrationStatus.ERROR);
                        dbBasedReceiptConfirmation.setErrorMessage(errorMessage);
                    }
                    save(dbBasedReceiptConfirmation);
                }
        );

    }

    public IntegrationReceiptConfirmationData resendReceiptConfirmationData(Long id) {
        DBBasedReceiptConfirmation dbBasedReceiptConfirmation =
                findById(id);
        dbBasedReceiptConfirmation.setStatus(IntegrationStatus.PENDING);
        dbBasedReceiptConfirmation.setErrorMessage("");
        return save(dbBasedReceiptConfirmation);
    }
}
