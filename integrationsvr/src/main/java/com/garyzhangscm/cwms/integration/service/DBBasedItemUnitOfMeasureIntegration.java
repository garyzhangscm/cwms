package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;

import com.garyzhangscm.cwms.integration.repository.DBBasedItemUnitOfMeasureRepository;
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
public class DBBasedItemUnitOfMeasureIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItemUnitOfMeasureIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedItemUnitOfMeasureRepository dbBasedItemUnitOfMeasureRepository;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;


    public List<DBBasedItemUnitOfMeasure> findAll(String companyCode,
                                                  Long warehouseId, ZonedDateTime startTime,
                                                  ZonedDateTime endTime, LocalDate date,
                                                  String statusList, Long id) {

        return dbBasedItemUnitOfMeasureRepository.findAll(
                (Root<DBBasedItemUnitOfMeasure> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

    public DBBasedItemUnitOfMeasure findById(Long id) {
        return dbBasedItemUnitOfMeasureRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("item unit of measure data not found by id: " + id));
    }

    public IntegrationItemUnitOfMeasureData addIntegrationItemUnitOfMeasureData(DBBasedItemUnitOfMeasure dbBasedItemUnitOfMeasure) {

        return dbBasedItemUnitOfMeasureRepository.save(dbBasedItemUnitOfMeasure);
    }



    private List<DBBasedItemUnitOfMeasure> findPendingIntegration() {
        return dbBasedItemUnitOfMeasureRepository.findAll(
                (Root<DBBasedItemUnitOfMeasure> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ).stream().limit(30).collect(Collectors.toList());
    }

    public DBBasedItemUnitOfMeasure save(DBBasedItemUnitOfMeasure dbBasedItem) {
        return dbBasedItemUnitOfMeasureRepository.save(dbBasedItem);
    }

    public void listen() {
        logger.debug("Start to process Item Unit Of Measure data");
        List<DBBasedItemUnitOfMeasure> dbBasedItemUnitOfMeasures = findPendingIntegration();
        logger.debug(">> get {}  Item Unit Of Measure data to be processed", dbBasedItemUnitOfMeasures.size());
        dbBasedItemUnitOfMeasures.forEach(dbBasedItemUnitOfMeasure -> process(dbBasedItemUnitOfMeasure));
    }

    private void process(DBBasedItemUnitOfMeasure dbBasedItemUnitOfMeasure) {

        try {

            ItemUnitOfMeasure itemUnitOfMeasure = dbBasedItemUnitOfMeasure.convertToItemUnitOfMeasure(inventoryServiceRestemplateClient,
                    commonServiceRestemplateClient,
                    warehouseLayoutServiceRestemplateClient);

            Item item = new Item();
            item.setName(dbBasedItemUnitOfMeasure.getItemName());
            item.setWarehouseId(itemUnitOfMeasure.getWarehouseId());
            item.setCompanyId(itemUnitOfMeasure.getCompanyId());

            ItemPackageType itemPackageType = new ItemPackageType();
            itemPackageType.setWarehouseId(itemUnitOfMeasure.getWarehouseId());
            itemPackageType.setCompanyId(itemUnitOfMeasure.getCompanyId());
            itemPackageType.setName(dbBasedItemUnitOfMeasure.getItemPackageTypeName());

            item.addItemPackageType(itemPackageType);

            logger.debug(">> will process Item:\n{}", item);


            if (itemUnitOfMeasure.getUnitOfMeasureId() == null) {
                UnitOfMeasure unitOfMeasure
                        = commonServiceRestemplateClient.getUnitOfMeasureByName(
                        itemUnitOfMeasure.getCompanyId(), itemUnitOfMeasure.getWarehouseId(), dbBasedItemUnitOfMeasure.getUnitOfMeasureName());
                logger.debug("Get id {} from unit of measure name: {}",
                        unitOfMeasure.getId(), dbBasedItemUnitOfMeasure.getUnitOfMeasureName());
                itemUnitOfMeasure.setUnitOfMeasureId(unitOfMeasure.getId());

            }

            kafkaSender.send(IntegrationType.INTEGRATION_ITEM_UNIT_OF_MEASURE,
                    itemUnitOfMeasure.getWarehouseId() + "-" + dbBasedItemUnitOfMeasure.getId(), itemUnitOfMeasure);


            dbBasedItemUnitOfMeasure.setStatus(IntegrationStatus.COMPLETED);
            dbBasedItemUnitOfMeasure.setErrorMessage("");

            logger.debug(">> Item Unit of Measure data process, {}", dbBasedItemUnitOfMeasure.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedItemUnitOfMeasure.setStatus(IntegrationStatus.ERROR);
        }

        save(dbBasedItemUnitOfMeasure);
    }

    public void saveIntegrationResult(IntegrationResult integrationResult) {
        logger.debug("will update the customer integration {}'s result to {}",
                integrationResult.getIntegrationId(),
                integrationResult.isSuccess());
        DBBasedItemUnitOfMeasure dbBasedItemUnitOfMeasure = findById(
                integrationResult.getIntegrationId()
        );
        IntegrationStatus integrationStatus =
                integrationResult.isSuccess() ? IntegrationStatus.COMPLETED : IntegrationStatus.ERROR;
        dbBasedItemUnitOfMeasure.setStatus(integrationStatus);
        dbBasedItemUnitOfMeasure.setErrorMessage(integrationResult.getErrorMessage());

        save(dbBasedItemUnitOfMeasure);


    }

    public IntegrationItemUnitOfMeasureData resendItemUnitOfMeasureData(Long id) {
        DBBasedItemUnitOfMeasure dbBasedItemUnitOfMeasure =
                findById(id);
        dbBasedItemUnitOfMeasure.setStatus(IntegrationStatus.PENDING);
        dbBasedItemUnitOfMeasure.setErrorMessage("");
        return save(dbBasedItemUnitOfMeasure);
    }

}
