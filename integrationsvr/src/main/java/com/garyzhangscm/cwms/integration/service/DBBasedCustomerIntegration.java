package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedCustomerRepository;
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
import java.util.stream.Collectors;

@Service
public class DBBasedCustomerIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedCustomerIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedCustomerRepository dbBasedCustomerRepository;

    public List<DBBasedCustomer> findAll() {
        return dbBasedCustomerRepository.findAll();
    }
    public DBBasedCustomer findById(Long id) {
        return dbBasedCustomerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("customer data not found by id: " + id));
    }

    public IntegrationCustomerData addIntegrationCustomerData(DBBasedCustomer dbBasedCustomer) {

        return dbBasedCustomerRepository.save(dbBasedCustomer);
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

            kafkaSender.send(IntegrationType.INTEGRATION_CUSTOMER, customer);


            dbBasedCustomer.setStatus(IntegrationStatus.COMPLETED);
            dbBasedCustomer.setErrorMessage("");

            logger.debug(">> customer data process, {}", dbBasedCustomer.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedCustomer.setStatus(IntegrationStatus.ERROR);
            dbBasedCustomer.setErrorMessage(ex.getMessage());
        }
        dbBasedCustomer.setLastUpdateTime(LocalDateTime.now());
        dbBasedCustomer = save(dbBasedCustomer);

    }
}
