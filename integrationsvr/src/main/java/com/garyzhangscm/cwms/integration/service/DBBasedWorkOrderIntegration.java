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
import java.util.stream.Collectors;

@Service
public class DBBasedWorkOrderIntegration {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedWorkOrderIntegration.class);

    @Autowired
    KafkaSender kafkaSender;
    @Autowired
    DBBasedWorkOrderRepository dbBasedWorkOrderRepository;
    @Autowired
    DBBasedWorkOrderLineRepository dbBasedWorkOrderLineRepository;
    @Autowired
    DBBasedWorkOrderInstructionRepository dbBasedWorkOrderInstructionRepository;
    @Autowired
    DBBasedWorkOrderByProductRepository dbBasedWorkOrderByProductRepository;

    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public List<DBBasedWorkOrder> findAll() {
        return dbBasedWorkOrderRepository.findAll();
    }
    public IntegrationWorkOrderData findById(Long id) {
        return dbBasedWorkOrderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order data not found by id: " + id));
    }


    public IntegrationWorkOrderData addIntegrationWorkOrderData(DBBasedWorkOrder dbBasedWorkOrder) {
        dbBasedWorkOrder.getWorkOrderLines().forEach(
                dbBasedWorkOrderLine -> {
                    dbBasedWorkOrderLine.setWorkOrder(dbBasedWorkOrder);
                    dbBasedWorkOrderLine.setId(null);
                }
        );
        dbBasedWorkOrder.setId(null);
        logger.debug("Start to save dbBasedWorkOrder: \n {}",
                dbBasedWorkOrder);
        // dbBasedWorkOrder.setId(null);
        return dbBasedWorkOrderRepository.save(dbBasedWorkOrder);
    }

    private List<DBBasedWorkOrder> findPendingIntegration() {
        return dbBasedWorkOrderRepository.findAll(
                (Root<DBBasedWorkOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("status"), IntegrationStatus.PENDING));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ).stream().limit(30).collect(Collectors.toList());
    }

    private DBBasedWorkOrder save(DBBasedWorkOrder dbBasedWorkOrder) {
        return dbBasedWorkOrderRepository.save(dbBasedWorkOrder);
    }

    public void listen() {
        logger.debug("Start to process Work Order data");
        List<DBBasedWorkOrder> dbBasedWorkOrders = findPendingIntegration();
        logger.debug(">> get {} Work Order data to be processed", dbBasedWorkOrders.size());
        dbBasedWorkOrders.forEach(dbBasedWorkOrder -> process(dbBasedWorkOrder));
    }

    private void process(DBBasedWorkOrder dbBasedWorkOrder) {


        try {


            WorkOrder workOrder = dbBasedWorkOrder.convertToWorkOrder();

            setupMissingField(workOrder, dbBasedWorkOrder);

            // Item item = getItemFromDatabase(dbBasedItem);
            logger.debug(">> will process Work Order:\n{}", workOrder);

            kafkaSender.send(IntegrationType.INTEGRATION_WORK_ORDER, workOrder);

            dbBasedWorkOrder.setStatus(IntegrationStatus.COMPLETED);
            dbBasedWorkOrder.setErrorMessage("");
            dbBasedWorkOrder.setLastUpdateTime(LocalDateTime.now());
            dbBasedWorkOrder = save(dbBasedWorkOrder);

            // Save the WORK order line as well
            dbBasedWorkOrder.getWorkOrderLines().forEach(dbBasedWorkOrderLine ->{
                dbBasedWorkOrderLine.setStatus(IntegrationStatus.COMPLETED);
                dbBasedWorkOrderLine.setErrorMessage("");
                dbBasedWorkOrderLine.setLastUpdateTime(LocalDateTime.now());
                dbBasedWorkOrderLineRepository.save(dbBasedWorkOrderLine);
            });

            dbBasedWorkOrder.getWorkOrderInstructions().forEach(dbBasedWorkOrderInstruction ->{
                dbBasedWorkOrderInstruction.setStatus(IntegrationStatus.COMPLETED);
                dbBasedWorkOrderInstruction.setErrorMessage("");
                dbBasedWorkOrderInstruction.setLastUpdateTime(LocalDateTime.now());
                dbBasedWorkOrderInstructionRepository.save(dbBasedWorkOrderInstruction);
            });

            dbBasedWorkOrder.getWorkOrderByProduct().forEach(dbBasedWorkOrderByProduct ->{
                dbBasedWorkOrderByProduct.setStatus(IntegrationStatus.COMPLETED);
                dbBasedWorkOrderByProduct.setErrorMessage("");
                dbBasedWorkOrderByProduct.setLastUpdateTime(LocalDateTime.now());
                dbBasedWorkOrderByProductRepository.save(dbBasedWorkOrderByProduct);
            });

            logger.debug(">> Work Order data process, {}", dbBasedWorkOrder.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedWorkOrder.setStatus(IntegrationStatus.ERROR);
            dbBasedWorkOrder.setErrorMessage(ex.getMessage());
            dbBasedWorkOrder.setLastUpdateTime(LocalDateTime.now());
            dbBasedWorkOrder = save(dbBasedWorkOrder);

            // Save the WORK order line as well
            dbBasedWorkOrder.getWorkOrderLines().forEach(dbBasedWorkOrderLine ->{
                dbBasedWorkOrderLine.setStatus(IntegrationStatus.ERROR);
                dbBasedWorkOrderLine.setErrorMessage(ex.getMessage());
                dbBasedWorkOrderLine.setLastUpdateTime(LocalDateTime.now());
                dbBasedWorkOrderLineRepository.save(dbBasedWorkOrderLine);
            });

            dbBasedWorkOrder.getWorkOrderInstructions().forEach(dbBasedWorkOrderInstruction ->{
                dbBasedWorkOrderInstruction.setStatus(IntegrationStatus.ERROR);
                dbBasedWorkOrderInstruction.setErrorMessage(ex.getMessage());
                dbBasedWorkOrderInstruction.setLastUpdateTime(LocalDateTime.now());
                dbBasedWorkOrderInstructionRepository.save(dbBasedWorkOrderInstruction);
            });

            dbBasedWorkOrder.getWorkOrderByProduct().forEach(dbBasedWorkOrderByProduct ->{
                dbBasedWorkOrderByProduct.setStatus(IntegrationStatus.ERROR);
                dbBasedWorkOrderByProduct.setErrorMessage(ex.getMessage());
                dbBasedWorkOrderByProduct.setLastUpdateTime(LocalDateTime.now());
                dbBasedWorkOrderByProductRepository.save(dbBasedWorkOrderByProduct);
            });
        }
    }



    private void setupMissingField(WorkOrder workOrder, DBBasedWorkOrder dbBasedWorkOrder){

        Long warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                dbBasedWorkOrder.getCompanyId(),
                dbBasedWorkOrder.getCompanyCode(),
                dbBasedWorkOrder.getWarehouseId(),
                dbBasedWorkOrder.getWarehouseName()
        );
        workOrder.setWarehouseId(warehouseId);


        workOrder.setItemId(
                inventoryServiceRestemplateClient.getItemByName(
                        warehouseId, dbBasedWorkOrder.getItemName()
                ).getId()
        );

        workOrder.getWorkOrderLines().forEach(workOrderLine -> {
            // Get the matched order line and setup the missing field
            // for
            // 1. item Id
            // 2. warehouse Id
            // 3. inventory status ID
            // 4. carrier ID
            // 5. carrier service level id
            dbBasedWorkOrder.getWorkOrderLines().forEach(dbBasedWorkOrderLine -> {
                if (workOrderLine.getNumber().equals(dbBasedWorkOrderLine.getNumber())) {
                    setupMissingField(warehouseId, workOrderLine, dbBasedWorkOrderLine);
                }
            });

        });


        workOrder.getWorkOrderByProducts().forEach(workOrderByProduct -> {

            dbBasedWorkOrder.getWorkOrderByProduct().forEach(dbBasedWorkOrderByProduct -> {
                if (workOrderByProduct.getItemName().equals(dbBasedWorkOrderByProduct.getItemName())) {
                    setupMissingField(warehouseId, workOrderByProduct, dbBasedWorkOrderByProduct);
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
     * @param workOrderLine
     * @param dbBasedWorkOrderLine
     */
    private void setupMissingField(Long warehouseId, WorkOrderLine workOrderLine, DBBasedWorkOrderLine dbBasedWorkOrderLine){

        workOrderLine.setWarehouseId(warehouseId);

        // 1. item Id
        if(Objects.isNull(workOrderLine.getItemId())) {
            workOrderLine.setItemId(
                    inventoryServiceRestemplateClient.getItemByName(
                            warehouseId, dbBasedWorkOrderLine.getItemName()
                    ).getId()
            );
        }



        // 3. inventory status ID
        if(Objects.isNull(workOrderLine.getInventoryStatusId())) {
            workOrderLine.setInventoryStatusId(
                    inventoryServiceRestemplateClient.getInventoryStatusByName(
                            warehouseId, dbBasedWorkOrderLine.getInventoryStatusName()
                    ).getId()
            );
        }

    }


    private void setupMissingField(Long warehouseId, WorkOrderByProduct workOrderByProduct,
                                   DBBasedWorkOrderByProduct dbBasedWorkOrderByProduct){



        // 1. item Id
        if(Objects.isNull(workOrderByProduct.getItemId())) {
            workOrderByProduct.setItemId(
                    inventoryServiceRestemplateClient.getItemByName(
                            warehouseId, dbBasedWorkOrderByProduct.getItemName()
                    ).getId()
            );
        }



        // 3. inventory status ID
        if(Objects.isNull(workOrderByProduct.getInventoryStatusId())) {
            workOrderByProduct.setInventoryStatusId(
                    inventoryServiceRestemplateClient.getInventoryStatusByName(
                            warehouseId, dbBasedWorkOrderByProduct.getInventoryStatusName()
                    ).getId()
            );
        }

    }



}