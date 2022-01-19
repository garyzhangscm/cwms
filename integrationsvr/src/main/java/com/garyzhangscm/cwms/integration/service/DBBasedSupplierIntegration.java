package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedClientRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedSupplierRepository;
import org.apache.logging.log4j.util.Strings;
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
import java.util.stream.Collectors;

@Service
public class DBBasedSupplierIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedSupplierIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedSupplierRepository dbBasedSupplierRepository;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public List<DBBasedSupplier> findAll(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date,
            String statusList, Long id) {

        return dbBasedSupplierRepository.findAll(
                (Root<DBBasedSupplier> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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
                    if (Objects.nonNull(date)) {
                        LocalDateTime dateStartTime = date.atTime(0, 0, 0, 0);
                        LocalDateTime dateEndTime = date.atTime(23, 59, 59, 999999999);
                        predicates.add(criteriaBuilder.between(
                                root.get("createdTime"), dateStartTime, dateEndTime));

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
                }
        );
    }

    public DBBasedSupplier findById(Long id) {
        return dbBasedSupplierRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("supplier data not found by id: " + id));
    }

    public IntegrationSupplierData addIntegrationSupplierData(DBBasedSupplier dbBasedSupplier) {

        setupMissingField(dbBasedSupplier);
        return dbBasedSupplierRepository.save(dbBasedSupplier);
    }

    private void setupMissingField(DBBasedSupplier dbBasedSupplier) {
        if (Strings.isBlank(dbBasedSupplier.getDescription())) {
            dbBasedSupplier.setDescription(dbBasedSupplier.getName());
        }

        if (Strings.isBlank(dbBasedSupplier.getContactorFirstname())) {
            dbBasedSupplier.setContactorFirstname("----");
        }

        if (Strings.isBlank(dbBasedSupplier.getContactorLastname())) {
            dbBasedSupplier.setContactorLastname("----");
        }

        if (Strings.isBlank(dbBasedSupplier.getAddressCountry())) {
            dbBasedSupplier.setAddressCountry("----");
        }
        if (Strings.isBlank(dbBasedSupplier.getAddressState())) {
            dbBasedSupplier.setAddressState("----");
        }
        if (Strings.isBlank(dbBasedSupplier.getAddressCounty())) {
            dbBasedSupplier.setAddressCounty("----");
        }
        if (Strings.isBlank(dbBasedSupplier.getAddressCity())) {
            dbBasedSupplier.setAddressCity("----");
        }
        if (Strings.isBlank(dbBasedSupplier.getAddressLine1())) {
            dbBasedSupplier.setAddressLine1("----");
        }
        if (Strings.isBlank(dbBasedSupplier.getAddressPostcode())) {
            dbBasedSupplier.setAddressPostcode("----");
        }
    }


    private List<DBBasedSupplier> findPendingIntegration() {
        return dbBasedSupplierRepository.findAll(
                (Root<DBBasedSupplier> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ).stream().limit(30).collect(Collectors.toList());
    }

    private DBBasedSupplier save(DBBasedSupplier dbBasedSupplier) {
        return dbBasedSupplierRepository.save(dbBasedSupplier);
    }

    public void listen() {
        logger.debug("Start to process supplier data");
        List<DBBasedSupplier> dbBasedSuppliers = findPendingIntegration();
        logger.debug(">> get {} supplier data to be processed", dbBasedSuppliers.size());
        dbBasedSuppliers.forEach(dbBasedSupplier -> process(dbBasedSupplier));
    }

    private void process(DBBasedSupplier dbBasedSupplier) {

        try {

            Supplier supplier = dbBasedSupplier.convertToSupplier();
            setupMissingField(supplier, dbBasedSupplier);

            logger.debug(">> will process Supplier :\n{}", supplier);


            kafkaSender.send(IntegrationType.INTEGRATION_SUPPLIER, dbBasedSupplier.getId(), supplier);


            dbBasedSupplier.setStatus(IntegrationStatus.SENT);
            dbBasedSupplier.setErrorMessage("");

            logger.debug(">> customer data process, {}", dbBasedSupplier.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedSupplier.setStatus(IntegrationStatus.ERROR);
            dbBasedSupplier.setErrorMessage(ex.getMessage());
        }

        save(dbBasedSupplier);

    }

    private void setupMissingField(Supplier supplier, DBBasedSupplier dbBasedSupplier){

        Long warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                dbBasedSupplier.getCompanyId(),
                dbBasedSupplier.getCompanyCode(),
                dbBasedSupplier.getWarehouseId(),
                dbBasedSupplier.getWarehouseName()
        );

        if (Objects.isNull(warehouseId)) {
            throw ResourceNotFoundException.raiseException("Can't find warehouse id by " +
                    "company id: " + dbBasedSupplier.getCompanyId() +
                    "company code: " + dbBasedSupplier.getCompanyCode() +
                    "warehouse id: " + dbBasedSupplier.getWarehouseId() +
                    "warehouse name: " + dbBasedSupplier.getWarehouseName());
        }


        supplier.setWarehouseId(warehouseId);

    }

    public void saveIntegrationResult(IntegrationResult integrationResult) {
        logger.debug("will update the customer integration {}'s result to {}",
                integrationResult.getIntegrationId(),
                integrationResult.isSuccess());
        DBBasedSupplier dbBasedSupplier = findById(
                integrationResult.getIntegrationId()
        );
        IntegrationStatus integrationStatus =
                integrationResult.isSuccess() ? IntegrationStatus.COMPLETED : IntegrationStatus.ERROR;
        dbBasedSupplier.setStatus(integrationStatus);
        dbBasedSupplier.setErrorMessage(integrationResult.getErrorMessage());

        save(dbBasedSupplier);


    }


    public IntegrationSupplierData resendSupplierData(Long id) {
        DBBasedSupplier dbBasedSupplier =
                findById(id);
        dbBasedSupplier.setStatus(IntegrationStatus.PENDING);
        dbBasedSupplier.setErrorMessage("");
        return save(dbBasedSupplier);
    }
}
