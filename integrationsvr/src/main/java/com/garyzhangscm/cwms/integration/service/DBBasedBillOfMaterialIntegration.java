package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.*;
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
public class DBBasedBillOfMaterialIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedBillOfMaterialIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedBillOfMaterialRepository dbBasedBillOfMaterialRepository;
    @Autowired
    DBBasedBillOfMaterialLineRepository dbBasedBillOfMaterialLineRepository;
    @Autowired
    DBBasedWorkOrderInstructionTemplateRepository dbBasedWorkOrderInstructionTemplateRepository;
    @Autowired
    DBBasedBillOfMaterialByProductRepository dbBasedBillOfMaterialByProductRepository;

    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    InventoryServiceRestemplateClient inventoryServiceRestemplateClient;



    public List<DBBasedBillOfMaterial> findAll(
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList, Long id) {

        return dbBasedBillOfMaterialRepository.findAll(
                (Root<DBBasedBillOfMaterial> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

    public DBBasedBillOfMaterial findById(Long id) {
        return dbBasedBillOfMaterialRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("bill of material data not found by id: " + id));
    }


    public IntegrationBillOfMaterialData addIntegrationBillOfMaterialData(DBBasedBillOfMaterial dbBasedBillOfMaterial) {
        return dbBasedBillOfMaterialRepository.save(dbBasedBillOfMaterial);
    }

    private List<DBBasedBillOfMaterial> findPendingIntegration() {
        return dbBasedBillOfMaterialRepository.findAll(
                (Root<DBBasedBillOfMaterial> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ).stream().limit(30).collect(Collectors.toList());
    }

    private DBBasedBillOfMaterial save(DBBasedBillOfMaterial dbBasedBillOfMaterial) {
        return dbBasedBillOfMaterialRepository.save(dbBasedBillOfMaterial);
    }

    public void listen() {
        logger.debug("Start to process Bill Of Material data");
        List<DBBasedBillOfMaterial> dbBasedBillOfMaterials = findPendingIntegration();
        logger.debug(">> get {} B.O.M data to be processed", dbBasedBillOfMaterials.size());
        dbBasedBillOfMaterials.forEach(dbBasedBillOfMaterial -> process(dbBasedBillOfMaterial));
    }

    private void process(DBBasedBillOfMaterial dbBasedBillOfMaterial) {

        try {
            BillOfMaterial billOfMaterial = dbBasedBillOfMaterial.convertToBillOfMaterial();

            setupMissingField(billOfMaterial, dbBasedBillOfMaterial);


            // Item item = getItemFromDatabase(dbBasedItem);
            logger.debug(">> will process B.O.M :\n{}", billOfMaterial);

            kafkaSender.send(IntegrationType.INTEGRATION_BILL_OF_MATERIAL,
                    billOfMaterial.getWarehouseId() + "-" + dbBasedBillOfMaterial.getId(), billOfMaterial);

            dbBasedBillOfMaterial.setStatus(IntegrationStatus.SENT);
            dbBasedBillOfMaterial.setErrorMessage("");
            dbBasedBillOfMaterial = save(dbBasedBillOfMaterial);

            // Save the WORK order line as well
            dbBasedBillOfMaterial.getBillOfMaterialLines().forEach(dbBasedBillOfMaterialLine ->{
                dbBasedBillOfMaterialLine.setStatus(IntegrationStatus.SENT);
                dbBasedBillOfMaterialLine.setErrorMessage("");
                dbBasedBillOfMaterialLineRepository.save(dbBasedBillOfMaterialLine);
            });

            dbBasedBillOfMaterial.getWorkOrderInstructionTemplates().forEach(dbBasedWorkOrderInstructionTemplate ->{
                dbBasedWorkOrderInstructionTemplate.setStatus(IntegrationStatus.SENT);
                dbBasedWorkOrderInstructionTemplate.setErrorMessage("");
                dbBasedWorkOrderInstructionTemplateRepository.save(dbBasedWorkOrderInstructionTemplate);
            });

            dbBasedBillOfMaterial.getBillOfMaterialByProducts().forEach(dbBasedBillOfMaterialByProduct ->{
                dbBasedBillOfMaterialByProduct.setStatus(IntegrationStatus.SENT);
                dbBasedBillOfMaterialByProduct.setErrorMessage("");
                dbBasedBillOfMaterialByProductRepository.save(dbBasedBillOfMaterialByProduct);
            });

            logger.debug(">> Work Order data process, {}", dbBasedBillOfMaterial.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedBillOfMaterial.setStatus(IntegrationStatus.ERROR);
            dbBasedBillOfMaterial.setErrorMessage(ex.getMessage());
            dbBasedBillOfMaterial = save(dbBasedBillOfMaterial);

            // Save the WORK order line as well
            dbBasedBillOfMaterial.getBillOfMaterialLines().forEach(dbBasedBillOfMaterialLine ->{
                dbBasedBillOfMaterialLine.setStatus(IntegrationStatus.ERROR);
                dbBasedBillOfMaterialLine.setErrorMessage(ex.getMessage());
                dbBasedBillOfMaterialLineRepository.save(dbBasedBillOfMaterialLine);
            });

            dbBasedBillOfMaterial.getWorkOrderInstructionTemplates().forEach(dbBasedWorkOrderInstructionTemplate ->{
                dbBasedWorkOrderInstructionTemplate.setStatus(IntegrationStatus.ERROR);
                dbBasedWorkOrderInstructionTemplate.setErrorMessage(ex.getMessage());
                dbBasedWorkOrderInstructionTemplateRepository.save(dbBasedWorkOrderInstructionTemplate);
            });

            dbBasedBillOfMaterial.getBillOfMaterialByProducts().forEach(dbBasedBillOfMaterialByProduct ->{
                dbBasedBillOfMaterialByProduct.setStatus(IntegrationStatus.ERROR);
                dbBasedBillOfMaterialByProduct.setErrorMessage(ex.getMessage());
                dbBasedBillOfMaterialByProductRepository.save(dbBasedBillOfMaterialByProduct);
            });
        }

    }




    private void setupMissingField(BillOfMaterial billOfMaterial, DBBasedBillOfMaterial dbBasedBillOfMaterial){

        Long warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                dbBasedBillOfMaterial.getCompanyId(),
                dbBasedBillOfMaterial.getCompanyCode(),
                dbBasedBillOfMaterial.getWarehouseId(),
                dbBasedBillOfMaterial.getWarehouseName()
        );
        billOfMaterial.setWarehouseId(warehouseId);

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId);



        billOfMaterial.setItemId(
                inventoryServiceRestemplateClient.getItemByName(
                        warehouse.getCompany().getId(), warehouseId, null, dbBasedBillOfMaterial.getItemName()
                ).getId()
        );

        billOfMaterial.getBillOfMaterialLines().forEach(billOfMaterialLine -> {
            // Get the matched order line and setup the missing field
            // for
            // 1. item Id
            // 2. warehouse Id
            // 3. inventory status ID
            // 4. carrier ID
            // 5. carrier service level id
            dbBasedBillOfMaterial.getBillOfMaterialLines().forEach(dbBasedBillOfMaterialLine -> {
                if (billOfMaterialLine.getNumber().equals(dbBasedBillOfMaterialLine.getNumber())) {
                    setupMissingField(warehouse, billOfMaterialLine, dbBasedBillOfMaterialLine);
                }
            });

        });


        billOfMaterial.getBillOfMaterialByProducts().forEach(billOfMaterialByProduct -> {

            dbBasedBillOfMaterial.getBillOfMaterialByProducts().forEach(dbBasedBillOfMaterialByProduct -> {
                if (billOfMaterialByProduct.getItemName().equals(dbBasedBillOfMaterialByProduct.getItemName())) {
                    setupMissingField(warehouse, billOfMaterialByProduct, dbBasedBillOfMaterialByProduct);
                }
            });

        });



    }

    /**
     * Setup the missing field. When we read from the database, we allow the host
     * to pass in name instead of id for the following feilds. We will need to
     * translate to id so that the correspondent service can recognize it     *
     * 1. item Id
     * 2. warehouse Id
     * 3. inventory status ID
     * @param warehouse      Warehouse
     * @param billOfMaterialLine
     * @param dbBasedBillOfMaterialLine
     */
    private void setupMissingField(Warehouse warehouse, BillOfMaterialLine billOfMaterialLine, DBBasedBillOfMaterialLine dbBasedBillOfMaterialLine){

        billOfMaterialLine.setWarehouseId(warehouse.getId());

        // 1. item Id
        if(Objects.isNull(billOfMaterialLine.getItemId())) {
            billOfMaterialLine.setItemId(
                    inventoryServiceRestemplateClient.getItemByName(
                            warehouse.getCompany().getId(), warehouse.getId(), null,  dbBasedBillOfMaterialLine.getItemName()
                    ).getId()
            );
        }



        // 3. inventory status ID
        if(Objects.isNull(billOfMaterialLine.getInventoryStatusId())) {
            billOfMaterialLine.setInventoryStatusId(
                    inventoryServiceRestemplateClient.getInventoryStatusByName(
                            warehouse.getId(), dbBasedBillOfMaterialLine.getInventoryStatusName()
                    ).getId()
            );
        }

    }


    private void setupMissingField(Warehouse warehouse, BillOfMaterialByProduct billOfMaterialByProduct,
                                   DBBasedBillOfMaterialByProduct dbBasedBillOfMaterialByProduct){

        billOfMaterialByProduct.setWarehouseId(warehouse.getId());

        // 1. item Id
        if(Objects.isNull(billOfMaterialByProduct.getItemId())) {
            billOfMaterialByProduct.setItemId(
                    inventoryServiceRestemplateClient.getItemByName(
                            warehouse.getCompany().getId(),
                            warehouse.getId(), null, dbBasedBillOfMaterialByProduct.getItemName()
                    ).getId()
            );
        }



        // 3. inventory status ID
        if(Objects.isNull(billOfMaterialByProduct.getInventoryStatusId())) {
            billOfMaterialByProduct.setInventoryStatusId(
                    inventoryServiceRestemplateClient.getInventoryStatusByName(
                            warehouse.getId(), dbBasedBillOfMaterialByProduct.getInventoryStatusName()
                    ).getId()
            );
        }

    }

    public void saveIntegrationResult(IntegrationResult integrationResult) {
        logger.debug("will update the Bill Of Material integration {}'s result to {}",
                integrationResult.getIntegrationId(),
                integrationResult.isSuccess());
        DBBasedBillOfMaterial dbBasedBillOfMaterial = findById(
                integrationResult.getIntegrationId()
        );
        IntegrationStatus integrationStatus =
                integrationResult.isSuccess() ? IntegrationStatus.COMPLETED : IntegrationStatus.ERROR;
        dbBasedBillOfMaterial.setStatus(integrationStatus);
        dbBasedBillOfMaterial.setErrorMessage(integrationResult.getErrorMessage());
        save(dbBasedBillOfMaterial);

        dbBasedBillOfMaterial.getBillOfMaterialLines().forEach(dbBasedBillOfMaterialLine ->{
            dbBasedBillOfMaterialLine.setStatus(integrationStatus);
            dbBasedBillOfMaterialLine.setErrorMessage(integrationResult.getErrorMessage());
            dbBasedBillOfMaterialLineRepository.save(dbBasedBillOfMaterialLine);
        });

        dbBasedBillOfMaterial.getBillOfMaterialByProducts().forEach(dbBasedBillOfMaterialByProduct ->{
            dbBasedBillOfMaterialByProduct.setStatus(integrationStatus);
            dbBasedBillOfMaterialByProduct.setErrorMessage(integrationResult.getErrorMessage());
            dbBasedBillOfMaterialByProductRepository.save(dbBasedBillOfMaterialByProduct);
        });

        dbBasedBillOfMaterial.getWorkOrderInstructionTemplates().forEach(dbBasedWorkOrderInstructionTemplate ->{
            dbBasedWorkOrderInstructionTemplate.setStatus(integrationStatus);
            dbBasedWorkOrderInstructionTemplate.setErrorMessage(integrationResult.getErrorMessage());
            dbBasedWorkOrderInstructionTemplateRepository.save(dbBasedWorkOrderInstructionTemplate);
        });

    }


    public IntegrationBillOfMaterialData resendBillOfMaterialData(Long id) {
        DBBasedBillOfMaterial dbBasedBillOfMaterial =
                findById(id);
        dbBasedBillOfMaterial.setStatus(IntegrationStatus.PENDING);
        dbBasedBillOfMaterial.setErrorMessage("");
        return save(dbBasedBillOfMaterial);
    }

}
