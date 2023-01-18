package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.HostRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedInventoryAdjustmentConfirmationRepository;
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
public class DBBasedInventoryAdjustmentConfirmationIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedInventoryAdjustmentConfirmationIntegration.class);

    @Autowired
    DBBasedInventoryAdjustmentConfirmationRepository dbBasedInventoryAdjustmentConfirmationRepository;
    @Autowired
    HostRestemplateClient hostRestemplateClient;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    KafkaSender kafkaSender;

    @Value("${integration.record.process.limit:100}")
    int recordLimit;

    private List<DBBasedInventoryAdjustmentConfirmation> findPendingIntegration() {
        Pageable limit = PageRequest.of(0,recordLimit);

        Page<DBBasedInventoryAdjustmentConfirmation> dbBasedInventoryAdjustmentConfirmations
                = dbBasedInventoryAdjustmentConfirmationRepository.findAll(
                (Root<DBBasedInventoryAdjustmentConfirmation> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                limit
        );
        return dbBasedInventoryAdjustmentConfirmations.getContent();
    }


    public List<DBBasedInventoryAdjustmentConfirmation> findAll(
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList, Long id) {

        return dbBasedInventoryAdjustmentConfirmationRepository.findAll(
                (Root<DBBasedInventoryAdjustmentConfirmation> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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
                                root.get("createdTime"), dateStartTime.atZone(ZoneOffset.UTC), dateEndTime.atZone(ZoneOffset.UTC)));

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
    public DBBasedInventoryAdjustmentConfirmation findById(Long id) {
        return dbBasedInventoryAdjustmentConfirmationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("client data not found by id: " + id));
    }

    private DBBasedInventoryAdjustmentConfirmation save(DBBasedInventoryAdjustmentConfirmation dbBasedInventoryAdjustmentConfirmation) {
        return dbBasedInventoryAdjustmentConfirmationRepository.save(dbBasedInventoryAdjustmentConfirmation);
    }

    public IntegrationInventoryAdjustmentConfirmationData saveInventoryAdjustmentConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation) {

        // Convert inventoryAdjustmentConfirmation to integration data
        DBBasedInventoryAdjustmentConfirmation dbBasedInventoryAdjustmentConfirmation =
                getDBBasedInventoryAdjustmentConfirmation(inventoryAdjustmentConfirmation);

        setupCompanyInformation(dbBasedInventoryAdjustmentConfirmation);

        dbBasedInventoryAdjustmentConfirmation.setStatus(IntegrationStatus.PENDING);
        return save(dbBasedInventoryAdjustmentConfirmation);
    }

    /**
     * When we receive integration from the warehouse, normally it will only include the warehouse id / warehouse name.
     * we will need to setup the company id and company name as well, especailly in the multi-tenancy environment
     * @param dbBasedInventoryAdjustmentConfirmation
     */
    private void setupCompanyInformation(DBBasedInventoryAdjustmentConfirmation dbBasedInventoryAdjustmentConfirmation) {
        if (Objects.isNull(dbBasedInventoryAdjustmentConfirmation.getCompanyId()) ||
                Objects.isNull(dbBasedInventoryAdjustmentConfirmation.getCompanyCode())) {
            Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(dbBasedInventoryAdjustmentConfirmation.getWarehouseId());
            if (Objects.nonNull(warehouse)) {
                dbBasedInventoryAdjustmentConfirmation.setCompanyId(warehouse.getCompany().getId());
                dbBasedInventoryAdjustmentConfirmation.setCompanyCode(warehouse.getCompany().getCode());
            }
        }
    }

    private DBBasedInventoryAdjustmentConfirmation getDBBasedInventoryAdjustmentConfirmation(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation) {
        return new DBBasedInventoryAdjustmentConfirmation(inventoryAdjustmentConfirmation);
    }

    public void sendToHost() {
        List<DBBasedInventoryAdjustmentConfirmation> dbBasedInventoryAdjustmentConfirmations =
                findPendingIntegration();
        logger.debug("# find " +  dbBasedInventoryAdjustmentConfirmations.size() + " dbBasedInventoryAdjustmentConfirmations");

        dbBasedInventoryAdjustmentConfirmations.forEach(
                dbBasedInventoryAdjustmentConfirmation -> {
                    String result = "";
                    String errorMessage = "";
                    try {
                        result = hostRestemplateClient.sendIntegrationData("inventory-adjustment-confirmation", dbBasedInventoryAdjustmentConfirmation);
                        logger.debug("# get result " + result);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        result = "false";
                        errorMessage = ex.getMessage();
                    }
                    if (result.equalsIgnoreCase("success")) {
                        dbBasedInventoryAdjustmentConfirmation.setStatus(IntegrationStatus.COMPLETED);
                    }
                    else {
                        dbBasedInventoryAdjustmentConfirmation.setStatus(IntegrationStatus.ERROR);
                        dbBasedInventoryAdjustmentConfirmation.setErrorMessage(errorMessage);
                    }
                    dbBasedInventoryAdjustmentConfirmation = save(dbBasedInventoryAdjustmentConfirmation);
                    sendAlert(dbBasedInventoryAdjustmentConfirmation);

                }
        );

    }

    private void sendAlert(DBBasedInventoryAdjustmentConfirmation dbBasedInventoryAdjustmentConfirmation) {
        Alert alert = dbBasedInventoryAdjustmentConfirmation.getStatus().equals(IntegrationStatus.COMPLETED) ?
                new Alert(dbBasedInventoryAdjustmentConfirmation.getCompanyId(),
                        AlertType.INTEGRATION_TO_HOST_SUCCESS,
                        "INTEGRATION-INVENTORY-ADJUSTMENT-CONFIRM-TO-HOST-" + dbBasedInventoryAdjustmentConfirmation.getId(),
                        "Integration INVENTORY-ADJUSTMENT-CONFIRM send to HOST " +
                                ", id: " + dbBasedInventoryAdjustmentConfirmation.getId() + " succeed!",
                        "Integration Succeed: \n" +
                                "Type: INVENTORY-ADJUSTMENT-CONFIRM send to HOST\n" +
                                "Id: " + dbBasedInventoryAdjustmentConfirmation.getId() + "\n" +
                                "Company Code: " + dbBasedInventoryAdjustmentConfirmation.getCompanyCode() + "\n" +
                                "Warehouse Name: " + dbBasedInventoryAdjustmentConfirmation.getWarehouseName() + "\n" +
                                "Item: " + dbBasedInventoryAdjustmentConfirmation.getItemName() + "\n" +
                                "Inventory Status: " + dbBasedInventoryAdjustmentConfirmation.getInventoryStatusName() + "\n" +
                                "Adjust Quantity: " + dbBasedInventoryAdjustmentConfirmation.getAdjustQuantity() + "\n")
                :
                new Alert(dbBasedInventoryAdjustmentConfirmation.getCompanyId(),
                        AlertType.INTEGRATION_TO_HOST_FAIL,
                        "INTEGRATION-INVENTORY-ADJUSTMENT-CONFIRM-TO-HOST-" + dbBasedInventoryAdjustmentConfirmation.getId(),
                        "Integration INVENTORY-ADJUSTMENT-CONFIRM send to HOST " +
                                ", id: " + dbBasedInventoryAdjustmentConfirmation.getId() + " fail!",
                        "Integration Fail: \n" +
                                "Type: INVENTORY-ADJUSTMENT-CONFIRM send to HOST\n" +
                                "Id: " + dbBasedInventoryAdjustmentConfirmation.getId() + "\n" +
                                "Company Code: " + dbBasedInventoryAdjustmentConfirmation.getCompanyCode() + "\n" +
                                "Warehouse Name: " + dbBasedInventoryAdjustmentConfirmation.getWarehouseName() + "\n" +
                                "Item: " + dbBasedInventoryAdjustmentConfirmation.getItemName() + "\n" +
                                "Inventory Status: " + dbBasedInventoryAdjustmentConfirmation.getInventoryStatusName() + "\n" +
                                "Adjust Quantity: " + dbBasedInventoryAdjustmentConfirmation.getAdjustQuantity() + "\n" +
                                "\n\nERROR: " + dbBasedInventoryAdjustmentConfirmation.getErrorMessage() + "\n")
                ;


        kafkaSender.send(alert);
    }

    public IntegrationInventoryAdjustmentConfirmationData resendInventoryAdjustmentConfirmationData(Long id) {
        DBBasedInventoryAdjustmentConfirmation dbBasedInventoryAdjustmentConfirmation =
                findById(id);
        dbBasedInventoryAdjustmentConfirmation.setStatus(IntegrationStatus.PENDING);
        dbBasedInventoryAdjustmentConfirmation.setErrorMessage("");
        return save(dbBasedInventoryAdjustmentConfirmation);
    }
}
