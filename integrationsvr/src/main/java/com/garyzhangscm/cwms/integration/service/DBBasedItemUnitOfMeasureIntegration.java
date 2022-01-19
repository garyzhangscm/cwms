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
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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


    public List<DBBasedItemUnitOfMeasure> findAll(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date,
            String statusList) {

        return dbBasedItemUnitOfMeasureRepository.findAll(
                (Root<DBBasedItemUnitOfMeasure> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("insertTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("insertTime"), endTime));

                    }
                    if (Objects.nonNull(date)) {
                        LocalDateTime dateStartTime = date.atTime(0, 0, 0, 0);
                        LocalDateTime dateEndTime = date.atTime(23, 59, 59, 999999999);
                        predicates.add(criteriaBuilder.between(
                                root.get("insertTime"), dateStartTime, dateEndTime));

                    }

                    if (Strings.isNotBlank(statusList)) {
                        CriteriaBuilder.In<IntegrationStatus> inStatus = criteriaBuilder.in(root.get("status"));
                        for(String status : statusList.split(",")) {
                            inStatus.value(IntegrationStatus.valueOf(status));
                        }
                        predicates.add(criteriaBuilder.and(inStatus));
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
            Long warehouseId
                    = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                    dbBasedItemUnitOfMeasure.getCompanyId(),
                    dbBasedItemUnitOfMeasure.getCompanyCode(),
                    dbBasedItemUnitOfMeasure.getWarehouseId(),
                    dbBasedItemUnitOfMeasure.getWarehouseName()
            );
            Item item = new Item();
            item.setName(dbBasedItemUnitOfMeasure.getItemName());
            item.setWarehouseId(warehouseId);

            ItemPackageType itemPackageType = new ItemPackageType();
            itemPackageType.setWarehouseId(warehouseId);
            itemPackageType.setName(dbBasedItemUnitOfMeasure.getItemPackageTypeName());

            item.addItemPackageType(itemPackageType);

            logger.debug(">> will process Item:\n{}", item);

            ItemUnitOfMeasure itemUnitOfMeasure = dbBasedItemUnitOfMeasure.convertToItemUnitOfMeasure(inventoryServiceRestemplateClient,
                    commonServiceRestemplateClient,
                    warehouseLayoutServiceRestemplateClient);
            itemUnitOfMeasure.setWarehouseId(warehouseId);

            if (itemUnitOfMeasure.getUnitOfMeasureId() == null) {
                UnitOfMeasure unitOfMeasure
                        = commonServiceRestemplateClient.getUnitOfMeasureByName(
                        warehouseId, dbBasedItemUnitOfMeasure.getUnitOfMeasureName());
                logger.debug("Get id {} from unit of measure name: {}",
                        unitOfMeasure.getId(), dbBasedItemUnitOfMeasure.getUnitOfMeasureName());
                itemUnitOfMeasure.setUnitOfMeasureId(unitOfMeasure.getId());

            }

            kafkaSender.send(IntegrationType.INTEGRATION_ITEM_UNIT_OF_MEASURE, item, itemUnitOfMeasure);


            dbBasedItemUnitOfMeasure.setStatus(IntegrationStatus.COMPLETED);
            dbBasedItemUnitOfMeasure.setErrorMessage("");

            logger.debug(">> Item Unit of Measure data process, {}", dbBasedItemUnitOfMeasure.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedItemUnitOfMeasure.setStatus(IntegrationStatus.ERROR);
        }
        dbBasedItemUnitOfMeasure.setLastUpdateTime(LocalDateTime.now());
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
        dbBasedItemUnitOfMeasure.setLastUpdateTime(LocalDateTime.now());
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
