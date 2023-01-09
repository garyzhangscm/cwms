package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedInventoryAdjustmentConfirmationRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedInventoryAttributeChangeConfirmationRepository;
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
public class DBBasedInventoryAttributeChangeConfirmationIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedInventoryAttributeChangeConfirmationIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedInventoryAttributeChangeConfirmationRepository dbBasedInventoryAttributeChangeConfirmationRepository;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;



    public List<DBBasedInventoryAttributeChangeConfirmation> findAll(
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList, Long id) {

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

        setupCompanyInformation(dbBasedInventoryAttributeChangeConfirmation);
        dbBasedInventoryAttributeChangeConfirmation.setStatus(IntegrationStatus.COMPLETED);
        return save(dbBasedInventoryAttributeChangeConfirmation);
    }
    /**
     * When we receive integration from the warehouse, normally it will only include the warehouse id / warehouse name.
     * we will need to setup the company id and company name as well, especailly in the multi-tenancy environment
     * @param dbBasedInventoryAttributeChangeConfirmation
     */
    private void setupCompanyInformation(DBBasedInventoryAttributeChangeConfirmation dbBasedInventoryAttributeChangeConfirmation) {
        if (Objects.isNull(dbBasedInventoryAttributeChangeConfirmation.getCompanyId()) ||
                Objects.isNull(dbBasedInventoryAttributeChangeConfirmation.getCompanyCode())) {
            Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(dbBasedInventoryAttributeChangeConfirmation.getWarehouseId());
            if (Objects.nonNull(warehouse)) {
                dbBasedInventoryAttributeChangeConfirmation.setCompanyId(warehouse.getCompany().getId());
                dbBasedInventoryAttributeChangeConfirmation.setCompanyCode(warehouse.getCompany().getCode());
            }
        }
    }

    private DBBasedInventoryAttributeChangeConfirmation getDBBasedInventoryAttributeChangeConfirmation(InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation) {
        return new DBBasedInventoryAttributeChangeConfirmation(inventoryAttributeChangeConfirmation);
    }
    public IntegrationInventoryAttributeChangeConfirmationData resendInventoryAttributeChangeConfirmationData(Long id) {
        DBBasedInventoryAttributeChangeConfirmation dbBasedInventoryAttributeChangeConfirmation =
                findById(id);
        dbBasedInventoryAttributeChangeConfirmation.setStatus(IntegrationStatus.PENDING);
        dbBasedInventoryAttributeChangeConfirmation.setErrorMessage("");
        return save(dbBasedInventoryAttributeChangeConfirmation);
    }

    private void sendAlert(DBBasedInventoryAttributeChangeConfirmation dbBasedInventoryAttributeChangeConfirmation) {
        Alert alert = dbBasedInventoryAttributeChangeConfirmation.getStatus().equals(IntegrationStatus.COMPLETED) ?
                new Alert(dbBasedInventoryAttributeChangeConfirmation.getCompanyId(),
                        AlertType.INTEGRATION_TO_HOST_SUCCESS,
                        "INTEGRATION-INVENTORY-ATTRIBUTE-CHANGE-CONFIRM-TO-HOST-" + dbBasedInventoryAttributeChangeConfirmation.getId(),
                        "Integration INVENTORY-ATTRIBUTE-CHANGE-CONFIRM send to HOST " +
                                ", id: " + dbBasedInventoryAttributeChangeConfirmation.getId() + " succeed!",
                        "Integration Succeed: \n" +
                                "Type: INVENTORY-ATTRIBUTE-CHANGE-CONFIRM send to HOST\n" +
                                "Id: " + dbBasedInventoryAttributeChangeConfirmation.getId() + "\n")
                :
                new Alert(dbBasedInventoryAttributeChangeConfirmation.getCompanyId(),
                        AlertType.INTEGRATION_TO_HOST_FAIL,
                        "INTEGRATION-INVENTORY-ATTRIBUTE-CHANGE-CONFIRM-TO-HOST-" + dbBasedInventoryAttributeChangeConfirmation.getId(),
                        "Integration INVENTORY-ATTRIBUTE-CHANGE-CONFIRM send to HOST " +
                                ", id: " + dbBasedInventoryAttributeChangeConfirmation.getId() + " fail!",
                        "Integration Fail: \n" +
                                "Type: INVENTORY-ATTRIBUTE-CHANGE-CONFIRM send to HOST\n" +
                                "Id: " + dbBasedInventoryAttributeChangeConfirmation.getId() + "\n" +
                                "\n\nERROR: " + dbBasedInventoryAttributeChangeConfirmation.getErrorMessage() + "\n")
                ;


        kafkaSender.send(alert);
    }
}
