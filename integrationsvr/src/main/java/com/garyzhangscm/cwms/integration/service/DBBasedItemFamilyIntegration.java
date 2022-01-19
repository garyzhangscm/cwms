package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedItemFamilyRepository;
import com.garyzhangscm.cwms.integration.repository.DBBasedItemRepository;
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
public class DBBasedItemFamilyIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItemFamilyIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedItemFamilyRepository dbBasedItemFamilyRepository;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    public List<DBBasedItemFamily> findAll(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date,
            String statusList, Long id) {

        return dbBasedItemFamilyRepository.findAll(
                (Root<DBBasedItemFamily> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

    public DBBasedItemFamily findById(Long id) {
        return dbBasedItemFamilyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("item data not found by id: " + id));
    }

    public IntegrationItemFamilyData addIntegrationItemFamilyData(DBBasedItemFamily dbBasedItemFamily) {

        return dbBasedItemFamilyRepository.save(dbBasedItemFamily);
    }


    private List<DBBasedItemFamily> findPendingIntegration() {
        return dbBasedItemFamilyRepository.findAll(
                (Root<DBBasedItemFamily> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ).stream().limit(30).collect(Collectors.toList());
    }

    public DBBasedItemFamily save(DBBasedItemFamily dbBasedItemFamily) {
        return dbBasedItemFamilyRepository.save(dbBasedItemFamily);
    }

    public void listen() {
        logger.debug("Start to process Item family data");
        List<DBBasedItemFamily> dbBasedItemFamilies = findPendingIntegration();
        logger.debug(">> get {} Item family data to be processed", dbBasedItemFamilies.size());
        dbBasedItemFamilies.forEach(dbBasedItemFamily -> process(dbBasedItemFamily));
    }

    private void process(DBBasedItemFamily dbBasedItemFamily) {

        try {


            ItemFamily itemFamily = dbBasedItemFamily.convertToItemFamily(warehouseLayoutServiceRestemplateClient);
            // Item item = getItemFromDatabase(dbBasedItem);
            logger.debug(">> will process Item Family:\n{}", itemFamily);

            kafkaSender.send(IntegrationType.INTEGRATION_ITEM_FAMILY, dbBasedItemFamily.getId(), itemFamily);


            dbBasedItemFamily.setStatus(IntegrationStatus.SENT);
            dbBasedItemFamily.setErrorMessage("");

            logger.debug(">> Item family data process, {}", dbBasedItemFamily.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedItemFamily.setStatus(IntegrationStatus.ERROR);
            dbBasedItemFamily.setErrorMessage(ex.getMessage());
        }
        save(dbBasedItemFamily);
    }

    public void saveIntegrationResult(IntegrationResult integrationResult) {
        logger.debug("will update the item family integration {}'s result to {}",
                integrationResult.getIntegrationId(),
                integrationResult.isSuccess());
        DBBasedItemFamily dbBasedItemFamily = findById(
                integrationResult.getIntegrationId()
        );
        IntegrationStatus integrationStatus =
                integrationResult.isSuccess() ? IntegrationStatus.COMPLETED : IntegrationStatus.ERROR;
        dbBasedItemFamily.setStatus(integrationStatus);
        dbBasedItemFamily.setErrorMessage(integrationResult.getErrorMessage());
        save(dbBasedItemFamily);


    }

    public IntegrationItemFamilyData resendItemFamilyData(Long id) {
        DBBasedItemFamily dbBasedItemFamily =
                findById(id);
        dbBasedItemFamily.setStatus(IntegrationStatus.PENDING);
        dbBasedItemFamily.setErrorMessage("");
        return save(dbBasedItemFamily);
    }



}
