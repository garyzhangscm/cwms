package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.KafkaSender;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
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


    public List<DBBasedBillOfMaterial> findAll() {
        return dbBasedBillOfMaterialRepository.findAll();
    }
    public IntegrationBillOfMaterialData findById(Long id) {
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

            kafkaSender.send(IntegrationType.INTEGRATION_BILL_OF_MATERIAL, billOfMaterial);

            dbBasedBillOfMaterial.setStatus(IntegrationStatus.COMPLETED);
            dbBasedBillOfMaterial.setErrorMessage("");
            dbBasedBillOfMaterial.setLastUpdateTime(LocalDateTime.now());
            dbBasedBillOfMaterial = save(dbBasedBillOfMaterial);

            // Save the WORK order line as well
            dbBasedBillOfMaterial.getBillOfMaterialLines().forEach(dbBasedBillOfMaterialLine ->{
                dbBasedBillOfMaterialLine.setStatus(IntegrationStatus.COMPLETED);
                dbBasedBillOfMaterialLine.setErrorMessage("");
                dbBasedBillOfMaterialLine.setLastUpdateTime(LocalDateTime.now());
                dbBasedBillOfMaterialLineRepository.save(dbBasedBillOfMaterialLine);
            });

            dbBasedBillOfMaterial.getWorkOrderInstructionTemplates().forEach(dbBasedWorkOrderInstructionTemplate ->{
                dbBasedWorkOrderInstructionTemplate.setStatus(IntegrationStatus.COMPLETED);
                dbBasedWorkOrderInstructionTemplate.setErrorMessage("");
                dbBasedWorkOrderInstructionTemplate.setLastUpdateTime(LocalDateTime.now());
                dbBasedWorkOrderInstructionTemplateRepository.save(dbBasedWorkOrderInstructionTemplate);
            });

            dbBasedBillOfMaterial.getBillOfMaterialByProducts().forEach(dbBasedBillOfMaterialByProduct ->{
                dbBasedBillOfMaterialByProduct.setStatus(IntegrationStatus.COMPLETED);
                dbBasedBillOfMaterialByProduct.setErrorMessage("");
                dbBasedBillOfMaterialByProduct.setLastUpdateTime(LocalDateTime.now());
                dbBasedBillOfMaterialByProductRepository.save(dbBasedBillOfMaterialByProduct);
            });

            logger.debug(">> Work Order data process, {}", dbBasedBillOfMaterial.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedBillOfMaterial.setStatus(IntegrationStatus.ERROR);
            dbBasedBillOfMaterial.setErrorMessage(ex.getMessage());
            dbBasedBillOfMaterial.setLastUpdateTime(LocalDateTime.now());
            dbBasedBillOfMaterial = save(dbBasedBillOfMaterial);

            // Save the WORK order line as well
            dbBasedBillOfMaterial.getBillOfMaterialLines().forEach(dbBasedBillOfMaterialLine ->{
                dbBasedBillOfMaterialLine.setStatus(IntegrationStatus.ERROR);
                dbBasedBillOfMaterialLine.setErrorMessage(ex.getMessage());
                dbBasedBillOfMaterialLine.setLastUpdateTime(LocalDateTime.now());
                dbBasedBillOfMaterialLineRepository.save(dbBasedBillOfMaterialLine);
            });

            dbBasedBillOfMaterial.getWorkOrderInstructionTemplates().forEach(dbBasedWorkOrderInstructionTemplate ->{
                dbBasedWorkOrderInstructionTemplate.setStatus(IntegrationStatus.ERROR);
                dbBasedWorkOrderInstructionTemplate.setErrorMessage(ex.getMessage());
                dbBasedWorkOrderInstructionTemplate.setLastUpdateTime(LocalDateTime.now());
                dbBasedWorkOrderInstructionTemplateRepository.save(dbBasedWorkOrderInstructionTemplate);
            });

            dbBasedBillOfMaterial.getBillOfMaterialByProducts().forEach(dbBasedBillOfMaterialByProduct ->{
                dbBasedBillOfMaterialByProduct.setStatus(IntegrationStatus.ERROR);
                dbBasedBillOfMaterialByProduct.setErrorMessage(ex.getMessage());
                dbBasedBillOfMaterialByProduct.setLastUpdateTime(LocalDateTime.now());
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


        billOfMaterial.setItemId(
                inventoryServiceRestemplateClient.getItemByName(
                        warehouseId, dbBasedBillOfMaterial.getItemName()
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
                    setupMissingField(warehouseId, billOfMaterialLine, dbBasedBillOfMaterialLine);
                }
            });

        });


        billOfMaterial.getBillOfMaterialByProducts().forEach(billOfMaterialByProduct -> {

            dbBasedBillOfMaterial.getBillOfMaterialByProducts().forEach(dbBasedBillOfMaterialByProduct -> {
                if (billOfMaterialByProduct.getItemName().equals(dbBasedBillOfMaterialByProduct.getItemName())) {
                    setupMissingField(warehouseId, billOfMaterialByProduct, dbBasedBillOfMaterialByProduct);
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
     * @param warehouseId      Warehouse id
     * @param billOfMaterialLine
     * @param dbBasedBillOfMaterialLine
     */
    private void setupMissingField(Long warehouseId, BillOfMaterialLine billOfMaterialLine, DBBasedBillOfMaterialLine dbBasedBillOfMaterialLine){

        billOfMaterialLine.setWarehouseId(warehouseId);

        // 1. item Id
        if(Objects.isNull(billOfMaterialLine.getItemId())) {
            billOfMaterialLine.setItemId(
                    inventoryServiceRestemplateClient.getItemByName(
                            warehouseId, dbBasedBillOfMaterialLine.getItemName()
                    ).getId()
            );
        }



        // 3. inventory status ID
        if(Objects.isNull(billOfMaterialLine.getInventoryStatusId())) {
            billOfMaterialLine.setInventoryStatusId(
                    inventoryServiceRestemplateClient.getInventoryStatusByName(
                            warehouseId, dbBasedBillOfMaterialLine.getInventoryStatusName()
                    ).getId()
            );
        }

    }


    private void setupMissingField(Long warehouseId, BillOfMaterialByProduct billOfMaterialByProduct,
                                   DBBasedBillOfMaterialByProduct dbBasedBillOfMaterialByProduct){

        billOfMaterialByProduct.setWarehouseId(warehouseId);

        // 1. item Id
        if(Objects.isNull(billOfMaterialByProduct.getItemId())) {
            billOfMaterialByProduct.setItemId(
                    inventoryServiceRestemplateClient.getItemByName(
                            warehouseId, dbBasedBillOfMaterialByProduct.getItemName()
                    ).getId()
            );
        }



        // 3. inventory status ID
        if(Objects.isNull(billOfMaterialByProduct.getInventoryStatusId())) {
            billOfMaterialByProduct.setInventoryStatusId(
                    inventoryServiceRestemplateClient.getInventoryStatusByName(
                            warehouseId, dbBasedBillOfMaterialByProduct.getInventoryStatusName()
                    ).getId()
            );
        }

    }

}