package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedItemRepository;
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
import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DBBasedItemIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItemIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedItemRepository dbBasedItemRepository;
    @Autowired
    DBBasedItemFamilyIntegration dbBasedItemFamilyIntegration;
    @Autowired
    DBBasedItemPackageTypeIntegration dbBasedItemPackageTypeIntegration;
    @Autowired
    DBBasedItemUnitOfMeasureIntegration dbBasedItemUnitOfMeasureIntegration;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;

    @Value("${integration.record.process.limit:100}")
    int recordLimit;


    public List<DBBasedItem> findAll(String companyCode,
                                     Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
                                     String statusList, Long id) {

        return dbBasedItemRepository.findAll(
                (Root<DBBasedItem> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyCode"), companyCode));

                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    }

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

    public DBBasedItem findById(Long id) {
        return dbBasedItemRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("item data not found by id: " + id));
    }

    @Transactional
    public IntegrationItemData addIntegrationItemData(DBBasedItem dbBasedItem) {

        dbBasedItem.setStatus(IntegrationStatus.PENDING);
        // in case the description is too long
        if (Strings.isNotBlank(dbBasedItem.getDescription()) &&
                dbBasedItem.getDescription().length() > 100) {
            dbBasedItem.setDescription(
                    dbBasedItem.getDescription().substring(0, 96) + "..."
            );
        }
        dbBasedItem.getItemPackageTypes().forEach(
                dbBasedItemPackageType -> {
                    dbBasedItemPackageType.setItem(dbBasedItem);
                    dbBasedItemPackageType.setStatus(IntegrationStatus.ATTACHED);
                    dbBasedItemPackageType.setId(null);
                    dbBasedItemPackageType.getItemUnitOfMeasures().forEach(
                            dbBasedItemUnitOfMeasure -> {
                                dbBasedItemUnitOfMeasure.setItemPackageType(dbBasedItemPackageType);
                                dbBasedItemUnitOfMeasure.setStatus(IntegrationStatus.ATTACHED);
                                dbBasedItemUnitOfMeasure.setId(null);
                            }
                    );
                }
        );
        dbBasedItem.setId(null);
        logger.debug("Start to save dbBasedItem: \n {}",
                dbBasedItem);
        return dbBasedItemRepository.save(dbBasedItem);
    }

    private List<DBBasedItem> findPendingIntegration() {
        Pageable limit = PageRequest.of(0,recordLimit);

        Page<DBBasedItem> dbBasedItemPage
                = dbBasedItemRepository.findAll(
                (Root<DBBasedItem> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                limit
        );
        return dbBasedItemPage.getContent();
    }

    private DBBasedItem save(DBBasedItem dbBasedItem) {
        return dbBasedItemRepository.save(dbBasedItem);
    }

    public void listen() {
        logger.debug("Start to process Item data");
        List<DBBasedItem> dbBasedItems = findPendingIntegration();
        logger.debug(">> get {} Item data to be processed", dbBasedItems.size());
        dbBasedItems.forEach(dbBasedItem -> process(dbBasedItem));
    }

    private void process(DBBasedItem dbBasedItem) {

        try {

            Item item = dbBasedItem.convertToItem(inventoryServiceRestemplateClient,
                    commonServiceRestemplateClient,
                    warehouseLayoutServiceRestemplateClient);
            // setup the warehouse
            // Item item = getItemFromDatabase(dbBasedItem);
            logger.debug(">> will process Item:\n{}", item);

            kafkaSender.send(IntegrationType.INTEGRATION_ITEM,
                    item.getCompanyId() + "-" +
                            (Objects.isNull(item.getWarehouseId()) ? "" : item.getWarehouseId())
                            + "-" + dbBasedItem.getId(), item);


            dbBasedItem.setErrorMessage("");
            dbBasedItem.completeIntegration(IntegrationStatus.SENT);
            if (Objects.nonNull(dbBasedItem.getItemFamily())) {

                dbBasedItem.getItemFamily().setErrorMessage("");
                dbBasedItem.getItemFamily().completeIntegration(IntegrationStatus.SENT);
                dbBasedItemFamilyIntegration.save(dbBasedItem.getItemFamily());
            }
            dbBasedItem.getItemPackageTypes().forEach(
                    dbBasedItemPackageType -> {
                        dbBasedItemPackageType.setErrorMessage("");
                        dbBasedItemPackageType.completeIntegration(IntegrationStatus.SENT);
                        dbBasedItemPackageTypeIntegration.save(dbBasedItemPackageType);
                        dbBasedItemPackageType.getItemUnitOfMeasures().forEach(
                                dbBasedItemUnitOfMeasure -> {
                                    dbBasedItemUnitOfMeasure.setErrorMessage("");
                                    dbBasedItemUnitOfMeasure.completeIntegration(IntegrationStatus.SENT);
                                    dbBasedItemUnitOfMeasureIntegration.save(dbBasedItemUnitOfMeasure);

                                }
                        );
                    }
            );


        }
        catch(Exception ex) {
            ex.printStackTrace();
            logger.debug("Exception : {} \n while process item integration: \n{}",
                    ex.getMessage(), dbBasedItem);
            dbBasedItem.completeIntegration(IntegrationStatus.ERROR, ex.getMessage());
            if (Objects.nonNull(dbBasedItem.getItemFamily())) {

                dbBasedItem.getItemFamily().completeIntegration(IntegrationStatus.ERROR, ex.getMessage());
                dbBasedItemFamilyIntegration.save(dbBasedItem.getItemFamily());
            }

            dbBasedItem.getItemPackageTypes().forEach(
                    dbBasedItemPackageType -> {
                        dbBasedItemPackageType.setErrorMessage(ex.getMessage());
                        dbBasedItemPackageType.completeIntegration(IntegrationStatus.ERROR);
                        dbBasedItemPackageTypeIntegration.save(dbBasedItemPackageType);
                        dbBasedItemPackageType.getItemUnitOfMeasures().forEach(
                                dbBasedItemUnitOfMeasure -> {
                                    dbBasedItemUnitOfMeasure.setErrorMessage(ex.getMessage());
                                    dbBasedItemUnitOfMeasure.completeIntegration(IntegrationStatus.ERROR);
                                    dbBasedItemUnitOfMeasureIntegration.save(dbBasedItemUnitOfMeasure);

                                }
                        );
                    }
            );


        }


        dbBasedItem = save(dbBasedItem);

        logger.debug(">> Item data process, {}", dbBasedItem);
    }


    public void saveIntegrationResult(IntegrationResult integrationResult) {
        logger.debug("will update the item integration {}'s result to {}",
                integrationResult.getIntegrationId(),
                integrationResult.isSuccess());
        DBBasedItem dbBasedItem = findById(
                integrationResult.getIntegrationId()
        );
        IntegrationStatus integrationStatus =
                integrationResult.isSuccess() ? IntegrationStatus.COMPLETED : IntegrationStatus.ERROR;
        dbBasedItem.setStatus(integrationStatus);
        dbBasedItem.setErrorMessage(integrationResult.getErrorMessage());
        save(dbBasedItem);


        dbBasedItem.getItemPackageTypes().forEach(dbBasedItemPackageType ->{
            dbBasedItemPackageType.setStatus(integrationStatus);
            dbBasedItemPackageType.setErrorMessage(integrationResult.getErrorMessage());
            dbBasedItemPackageTypeIntegration.save(dbBasedItemPackageType);

            dbBasedItemPackageType.getItemUnitOfMeasures().forEach(dbBasedItemUnitOfMeasure -> {

                dbBasedItemUnitOfMeasure.setStatus(integrationStatus);
                dbBasedItemUnitOfMeasure.setErrorMessage(integrationResult.getErrorMessage());
                dbBasedItemUnitOfMeasureIntegration.save(dbBasedItemUnitOfMeasure);
            });
        });


    }


    public IntegrationItemData resendItemData(Long id) {
        DBBasedItem dbBasedItem =
                findById(id);
        dbBasedItem.setStatus(IntegrationStatus.PENDING);
        dbBasedItem.setErrorMessage("");
        return save(dbBasedItem);
    }


}
