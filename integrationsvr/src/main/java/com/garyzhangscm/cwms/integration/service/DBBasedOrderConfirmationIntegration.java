package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.HostRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedOrderConfirmationRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
    @Autowired
    HostRestemplateClient hostRestemplateClient;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Value("${integration.record.process.limit:100}")
    int recordLimit;

    private List<DBBasedOrderConfirmation> findPendingIntegration() {
        Pageable limit = PageRequest.of(0,recordLimit);

        Page<DBBasedOrderConfirmation> dbBasedOrderConfirmations
                = dbBasedOrderConfirmationRepository.findAll(
                (Root<DBBasedOrderConfirmation> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                limit
        );
        return dbBasedOrderConfirmations.getContent();
    }

    public List<DBBasedOrderConfirmation> findAll(Long warehouseId, String warehouseName,
                                                  String number,
                                                  ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
                                                  String statusList, Long id) {
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
                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdTime"), endTime));

                    }
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
    public DBBasedOrderConfirmation findById(Long id) {
        return dbBasedOrderConfirmationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("order confirmation data not found by id: " + id));
    }

    private DBBasedOrderConfirmation save(DBBasedOrderConfirmation dbBasedOrderConfirmation) {
        return dbBasedOrderConfirmationRepository.save(dbBasedOrderConfirmation);
    }

    public IntegrationOrderConfirmationData sendIntegrationOrderConfirmationData(OrderConfirmation orderConfirmation){

        DBBasedOrderConfirmation dbBasedOrderConfirmation =
                getDBBasedOrderConfirmation(orderConfirmation);

        setupCompanyInformation(dbBasedOrderConfirmation);

        return save(dbBasedOrderConfirmation);
    }

    private DBBasedOrderConfirmation getDBBasedOrderConfirmation(OrderConfirmation orderConfirmation) {
        return new DBBasedOrderConfirmation(orderConfirmation);
    }


    /**
     * Send to host's API endpoint, in case host's db is in a different network
     */
    public void sendToHost() {
        List<DBBasedOrderConfirmation> dbBasedOrderConfirmations =
                findPendingIntegration();
        logger.debug("# find " +  dbBasedOrderConfirmations.size() + " dbBasedOrderConfirmations");

        dbBasedOrderConfirmations.forEach(
                dbBasedOrderConfirmation -> {
                    String result = "";
                    String errorMessage = "";
                    try {
                        result = hostRestemplateClient.sendIntegrationData("order-confirmation", dbBasedOrderConfirmation);
                        logger.debug("# get result " + result);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        result = "false";
                        errorMessage = ex.getMessage();
                    }
                    if (result.equalsIgnoreCase("success")) {
                        dbBasedOrderConfirmation.setStatus(IntegrationStatus.COMPLETED);
                    }
                    else {
                        dbBasedOrderConfirmation.setStatus(IntegrationStatus.ERROR);
                        dbBasedOrderConfirmation.setErrorMessage(errorMessage);
                    }
                    dbBasedOrderConfirmation = save(dbBasedOrderConfirmation);
                    sendAlert(dbBasedOrderConfirmation);
                }
        );

    }

    public IntegrationOrderConfirmationData resendOrderConfirmationData(Long id) {
        DBBasedOrderConfirmation dbBasedOrderConfirmation =
                findById(id);
        dbBasedOrderConfirmation.setStatus(IntegrationStatus.PENDING);
        dbBasedOrderConfirmation.setErrorMessage("");
        return save(dbBasedOrderConfirmation);
    }
    /**
     * When we receive integration from the warehouse, normally it will only include the warehouse id / warehouse name.
     * we will need to setup the company id and company name as well, especailly in the multi-tenancy environment
     * @param dbBasedOrderConfirmation
     */
    private void setupCompanyInformation(DBBasedOrderConfirmation dbBasedOrderConfirmation) {
        if (Objects.isNull(dbBasedOrderConfirmation.getCompanyId()) ||
                Objects.isNull(dbBasedOrderConfirmation.getCompanyCode())) {
            Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(dbBasedOrderConfirmation.getWarehouseId());
            if (Objects.nonNull(warehouse)) {
                dbBasedOrderConfirmation.setCompanyId(warehouse.getCompany().getId());
                dbBasedOrderConfirmation.setCompanyCode(warehouse.getCompany().getCode());
            }
        }
    }

    private void sendAlert(DBBasedOrderConfirmation dbBasedOrderConfirmation) {
        Alert alert = dbBasedOrderConfirmation.getStatus().equals(IntegrationStatus.COMPLETED) ?
                new Alert(dbBasedOrderConfirmation.getCompanyId(),
                        AlertType.INTEGRATION_TO_HOST_SUCCESS,
                        "INTEGRATION-ORDER-CONFIRM-TO-HOST-" + dbBasedOrderConfirmation.getId(),
                        "Integration ORDER-CONFIRM send to HOST " +
                                ", id: " + dbBasedOrderConfirmation.getId() + " succeed!",
                        "Integration Succeed: \n" +
                                "Type: ORDER-CONFIRM send to HOST\n" +
                                "Id: " + dbBasedOrderConfirmation.getId() + "\n" +
                                "Order Number: " + dbBasedOrderConfirmation.getNumber() + "\n")
                :
                new Alert(dbBasedOrderConfirmation.getCompanyId(),
                        AlertType.INTEGRATION_TO_HOST_FAIL,
                        "INTEGRATION-ORDER-CONFIRM-TO-HOST-" + dbBasedOrderConfirmation.getId(),
                        "Integration ORDER-CONFIRM send to HOST " +
                                ", id: " + dbBasedOrderConfirmation.getId() + " fail!",
                        "Integration Fail: \n" +
                                "Type: ORDER-CONFIRM send to HOST\n" +
                                "Id: " + dbBasedOrderConfirmation.getId() + "\n" +
                                "Order Number: " + dbBasedOrderConfirmation.getNumber() + "\n" +
                                "\n\nERROR: " + dbBasedOrderConfirmation.getErrorMessage() + "\n")
                ;


        kafkaSender.send(alert);
    }

    public List<? extends IntegrationOrderConfirmationData> getPendingIntegrationOrderConfirmationData(
            Long warehouseId, String companyCode, String warehouseName) {


        Warehouse warehouse =
                Objects.nonNull(warehouseId) ? warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId)
                        :
                        warehouseLayoutServiceRestemplateClient.getWarehouseByName(companyCode, warehouseName);

        return dbBasedOrderConfirmationRepository.findPendingIntegration(
                warehouse.getCompany().getId(), warehouse.getCompany().getCode(),
                warehouse.getId(), warehouse.getName()
        );
    }
}
