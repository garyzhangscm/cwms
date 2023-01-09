package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedOrderConfirmationRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedWorkOrderConfirmationRepository;
import org.apache.commons.lang.StringUtils;
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

@Service
public class DBBasedWorkOrderConfirmationIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedWorkOrderConfirmationIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedWorkOrderConfirmationRepository dbBasedWorkOrderConfirmationRepository;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public List<DBBasedWorkOrderConfirmation> findAll(Long warehouseId, String warehouseName,
                                                      String number,
                                                      ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
                                                      String statusList, Long id) {
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

        setupCompanyInformation(dbBasedWorkOrderConfirmation);

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
    /**
     * When we receive integration from the warehouse, normally it will only include the warehouse id / warehouse name.
     * we will need to setup the company id and company name as well, especailly in the multi-tenancy environment
     * @param dbBasedWorkOrderConfirmation
     */
    private void setupCompanyInformation(DBBasedWorkOrderConfirmation dbBasedWorkOrderConfirmation) {
        if (Objects.isNull(dbBasedWorkOrderConfirmation.getCompanyId()) ||
                Objects.isNull(dbBasedWorkOrderConfirmation.getCompanyCode())) {
            Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(dbBasedWorkOrderConfirmation.getWarehouseId());
            if (Objects.nonNull(warehouse)) {
                dbBasedWorkOrderConfirmation.setCompanyId(warehouse.getCompany().getId());
                dbBasedWorkOrderConfirmation.setCompanyCode(warehouse.getCompany().getCode());
            }
        }
    }

    private void sendAlert(DBBasedWorkOrderConfirmation dbBasedWorkOrderConfirmation) {
        Alert alert = dbBasedWorkOrderConfirmation.getStatus().equals(IntegrationStatus.COMPLETED) ?
                new Alert(dbBasedWorkOrderConfirmation.getCompanyId(),
                        AlertType.INTEGRATION_TO_HOST_SUCCESS,
                        "INTEGRATION-WORK-ORDER-CONFIRM-TO-HOST-" + dbBasedWorkOrderConfirmation.getId(),
                        "Integration WORK-ORDER-CONFIRM send to HOST " +
                                ", id: " + dbBasedWorkOrderConfirmation.getId() + " succeed!",
                        "Integration Succeed: \n" +
                                "Type: WORK-ORDER-CONFIRM send to HOST\n" +
                                "Id: " + dbBasedWorkOrderConfirmation.getId() + "\n" +
                                "Receipt Number: " + dbBasedWorkOrderConfirmation.getNumber() + "\n")
                :
                new Alert(dbBasedWorkOrderConfirmation.getCompanyId(),
                        AlertType.INTEGRATION_TO_HOST_FAIL,
                        "INTEGRATION-WORK-ORDER-CONFIRM-TO-HOST-" + dbBasedWorkOrderConfirmation.getId(),
                        "Integration WORK-ORDER-CONFIRM send to HOST " +
                                ", id: " + dbBasedWorkOrderConfirmation.getId() + " fail!",
                        "Integration Fail: \n" +
                                "Type: WORK-ORDER-CONFIRM send to HOST\n" +
                                "Id: " + dbBasedWorkOrderConfirmation.getId() + "\n" +
                                "Receipt Number: " + dbBasedWorkOrderConfirmation.getNumber() + "\n" +
                                "\n\nERROR: " + dbBasedWorkOrderConfirmation.getErrorMessage() + "\n")
                ;


        kafkaSender.send(alert);
    }
}
