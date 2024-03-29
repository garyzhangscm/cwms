package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.DBBasedItemPackageTypeRepository;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DBBasedItemPackageTypeIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedItemPackageTypeIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedItemPackageTypeRepository dbBasedItemPackageTypeRepository;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;


    public List<DBBasedItemPackageType> findAll(String companyCode,
                                                Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
                                                String statusList, Long id) {

        return dbBasedItemPackageTypeRepository.findAll(
                (Root<DBBasedItemPackageType> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

    public DBBasedItemPackageType findById(Long id) {
        return dbBasedItemPackageTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("item package type data not found by id: " + id));
    }

    public IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(DBBasedItemPackageType dbBasedItemPackageType) {

        return dbBasedItemPackageTypeRepository.save(dbBasedItemPackageType);
    }

    private List<DBBasedItemPackageType> findPendingIntegration() {
        return dbBasedItemPackageTypeRepository.findAll(
                (Root<DBBasedItemPackageType> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ).stream().limit(30).collect(Collectors.toList());
    }

    public DBBasedItemPackageType save(DBBasedItemPackageType dbBasedItemPackageType) {
        return dbBasedItemPackageTypeRepository.save(dbBasedItemPackageType);
    }

    public void listen() {
        logger.debug("Start to process Item Package Type data");
        List<DBBasedItemPackageType> dbBasedItemPackageTypes = findPendingIntegration();
        logger.debug(">> get {} Item Package Type data to be processed", dbBasedItemPackageTypes.size());
        dbBasedItemPackageTypes.forEach(dbBasedItemPackageType -> process(dbBasedItemPackageType));
    }

    private void process(DBBasedItemPackageType dbBasedItemPackageType) {


        try {

            Long warehouseId
                    = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                    dbBasedItemPackageType.getCompanyId(),
                    dbBasedItemPackageType.getCompanyCode(),
                    dbBasedItemPackageType.getWarehouseId(),
                    dbBasedItemPackageType.getWarehouseName()
            );
            Item item = new Item();
            item.setName(dbBasedItemPackageType.getItemName());
            item.setWarehouseId(warehouseId);


            logger.debug(">> will add Item to the item package type:\n{}", item);

            ItemPackageType itemPackageType = dbBasedItemPackageType.convertToItemPackageType(inventoryServiceRestemplateClient,
                    commonServiceRestemplateClient,
                    warehouseLayoutServiceRestemplateClient);
            // Item item = getItemFromDatabase(dbBasedItem);
            logger.debug(">> will process Item Package Type:\n{}", itemPackageType);

            kafkaSender.send(IntegrationType.INTEGRATION_ITEM_PACKAGE_TYPE,
                    itemPackageType.getWarehouseId() + "-" + dbBasedItemPackageType.getId(), itemPackageType);


            dbBasedItemPackageType.setErrorMessage("");
            dbBasedItemPackageType.setStatus(IntegrationStatus.COMPLETED);

            logger.debug(">> Item Package Type data process, {}", dbBasedItemPackageType.getStatus());


        }
        catch(Exception ex) {
            ex.printStackTrace();
            dbBasedItemPackageType.completeIntegration(IntegrationStatus.ERROR, ex.getMessage());

        }

        save(dbBasedItemPackageType);
    }


    public IntegrationItemPackageTypeData resendItemPackageTypeData(Long id) {
        DBBasedItemPackageType dbBasedItemPackageType =
                findById(id);
        dbBasedItemPackageType.setStatus(IntegrationStatus.PENDING);
        dbBasedItemPackageType.setErrorMessage("");
        return save(dbBasedItemPackageType);
    }


}
