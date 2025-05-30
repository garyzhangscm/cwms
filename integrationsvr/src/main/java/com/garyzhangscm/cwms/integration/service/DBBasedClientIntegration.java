package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedClientRepository;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DBBasedClientIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedClientIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedClientRepository dbBasedClientRepository;


    public List<DBBasedClient> findAll(
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList, Long id) {

        return dbBasedClientRepository.findAll(
                (Root<DBBasedClient> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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
                }
        );
    }

    public DBBasedClient findById(Long id) {
        return dbBasedClientRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("client data not found by id: " + id));
    }

    public IntegrationClientData addIntegrationClientData(DBBasedClient dbBasedClient) {
        return dbBasedClientRepository.save(dbBasedClient);
    }

    private List<DBBasedClient> findPendingIntegration() {
        return dbBasedClientRepository.findAll(
                (Root<DBBasedClient> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ).stream().limit(30).collect(Collectors.toList());
    }

    private DBBasedClient save(DBBasedClient dbBasedClient) {
        return dbBasedClientRepository.save(dbBasedClient);
    }

    public void listen() {
        logger.debug("Start to process client data");
        List<DBBasedClient> dbBasedClients = findPendingIntegration();
        logger.debug(">> get {} client data to be processed", dbBasedClients.size());
        dbBasedClients.forEach(dbBasedClient -> process(dbBasedClient));
    }

    private void process(DBBasedClient dbBasedClient) {
        try {

            Client client = dbBasedClient.convertToClient();
            logger.debug(">> will process customer:\n{}", client);

            kafkaSender.send(IntegrationType.INTEGRATION_CLIENT,
                    dbBasedClient.getWarehouseId() + "-" + dbBasedClient.getId(), client);


            dbBasedClient.setErrorMessage("");
            dbBasedClient.setStatus(IntegrationStatus.SENT);

            logger.debug(">> customer data process, {}", dbBasedClient.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedClient.setErrorMessage(ex.getMessage());
            dbBasedClient.setStatus(IntegrationStatus.ERROR);
        }
        dbBasedClient = save(dbBasedClient);
    }

    public void saveIntegrationResult(IntegrationResult integrationResult) {
        logger.debug("will update the client integration {}'s result to {}",
                integrationResult.getIntegrationId(),
                integrationResult.isSuccess());
        DBBasedClient dbBasedClient = findById(
                integrationResult.getIntegrationId()
        );
        IntegrationStatus integrationStatus =
                integrationResult.isSuccess() ? IntegrationStatus.COMPLETED : IntegrationStatus.ERROR;
        dbBasedClient.setStatus(integrationStatus);
        dbBasedClient.setErrorMessage(integrationResult.getErrorMessage());
        save(dbBasedClient);


    }

    public IntegrationClientData resendClientData(Long id) {
        DBBasedClient dbBasedClient =
                findById(id);
        dbBasedClient.setStatus(IntegrationStatus.PENDING);
        dbBasedClient.setErrorMessage("");
        return save(dbBasedClient);
    }

}
