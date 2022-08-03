package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedCustomerRepository;
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
public class DBBasedCustomerIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedCustomerIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedCustomerRepository dbBasedCustomerRepository;


    public List<DBBasedCustomer> findAll(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date,
            String statusList, Long id) {

        return dbBasedCustomerRepository.findAll(
                (Root<DBBasedCustomer> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

    public DBBasedCustomer findById(Long id) {
        return dbBasedCustomerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("customer data not found by id: " + id));
    }

    public IntegrationCustomerData addIntegrationCustomerData(DBBasedCustomer dbBasedCustomer) {
        dbBasedCustomer.setStatus(IntegrationStatus.PENDING);
        setupMissingField(dbBasedCustomer);
        return dbBasedCustomerRepository.save(dbBasedCustomer);
    }

    private void setupMissingField(DBBasedCustomer dbBasedCustomer) {
        if (Strings.isBlank(dbBasedCustomer.getDescription())) {
            dbBasedCustomer.setDescription(dbBasedCustomer.getName());
        }

        if (Strings.isBlank(dbBasedCustomer.getContactorFirstname())) {
            dbBasedCustomer.setContactorFirstname("----");
        }

        if (Strings.isBlank(dbBasedCustomer.getContactorLastname())) {
            dbBasedCustomer.setContactorLastname("----");
        }

        if (Strings.isBlank(dbBasedCustomer.getAddressCountry())) {
            dbBasedCustomer.setAddressCountry("----");
        }
        if (Strings.isBlank(dbBasedCustomer.getAddressState())) {
            dbBasedCustomer.setAddressState("----");
        }
        if (Strings.isBlank(dbBasedCustomer.getAddressCounty())) {
            dbBasedCustomer.setAddressCounty("----");
        }
        if (Strings.isBlank(dbBasedCustomer.getAddressCity())) {
            dbBasedCustomer.setAddressCity("----");
        }
        if (Strings.isBlank(dbBasedCustomer.getAddressLine1())) {
            dbBasedCustomer.setAddressLine1("----");
        }
        if (Strings.isBlank(dbBasedCustomer.getAddressPostcode())) {
            dbBasedCustomer.setAddressPostcode("----");
        }
    }


    private List<DBBasedCustomer> findPendingIntegration() {
        return dbBasedCustomerRepository.findAll(
                (Root<DBBasedCustomer> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ).stream().limit(30).collect(Collectors.toList());
    }

    private DBBasedCustomer save(DBBasedCustomer dbBasedCustomer) {
        return dbBasedCustomerRepository.save(dbBasedCustomer);
    }

    public void listen() {
        logger.debug("Start to process customer data");
        List<DBBasedCustomer> dbBasedCustomers = findPendingIntegration();
        logger.debug(">> get {} customer data to be processed", dbBasedCustomers.size());
        dbBasedCustomers.forEach(dbBasedCustomer -> process(dbBasedCustomer));
    }

    private void process(DBBasedCustomer dbBasedCustomer) {

        try {

            Customer customer = dbBasedCustomer.convertToCustomer();
            logger.debug(">> will process customer:\n{}", customer);

            kafkaSender.send(IntegrationType.INTEGRATION_CUSTOMER,
                    dbBasedCustomer.getWarehouseId() + "-" + dbBasedCustomer.getId(), customer);


            dbBasedCustomer.setStatus(IntegrationStatus.SENT);
            dbBasedCustomer.setErrorMessage("");

            logger.debug(">> customer data process, {}", dbBasedCustomer.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedCustomer.setStatus(IntegrationStatus.ERROR);
            dbBasedCustomer.setErrorMessage(ex.getMessage());
        }
        dbBasedCustomer = save(dbBasedCustomer);

    }

    public void saveIntegrationResult(IntegrationResult integrationResult) {
        logger.debug("will update the customer integration {}'s result to {}",
                integrationResult.getIntegrationId(),
                integrationResult.isSuccess());
        DBBasedCustomer dbBasedCustomer = findById(
                integrationResult.getIntegrationId()
        );
        IntegrationStatus integrationStatus =
                integrationResult.isSuccess() ? IntegrationStatus.COMPLETED : IntegrationStatus.ERROR;
        dbBasedCustomer.setStatus(integrationStatus);
        dbBasedCustomer.setErrorMessage(integrationResult.getErrorMessage());
        save(dbBasedCustomer);


    }

    public IntegrationCustomerData resendCustomerData(Long id) {
        DBBasedCustomer dbBasedCustomer =
                findById(id);
        dbBasedCustomer.setStatus(IntegrationStatus.PENDING);
        dbBasedCustomer.setErrorMessage("");
        return save(dbBasedCustomer);
    }
}
