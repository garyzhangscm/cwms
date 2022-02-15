package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.*;
import com.garyzhangscm.cwms.integration.exception.GenericException;
import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
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
    @Autowired
    WorkOrderServiceRestemplateClient workOrderServiceRestemplateClient;


    public List<DBBasedWorkOrder> findAll(String companyCode,
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date,
            String statusList, Long id) {

        return dbBasedWorkOrderRepository.findAll(
                (Root<DBBasedWorkOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

    public DBBasedWorkOrder findById(Long id) {
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

            // make sure this is a new work order
            // or it exists but in a changable status
            /***
             * We will let the application decide how to process the existing work order
             if (!validateExistingWorkOrder(workOrder)) {

                 dbBasedWorkOrder.setStatus(IntegrationStatus.ERROR);
                 dbBasedWorkOrder.setErrorMessage(
                 "work order " + dbBasedWorkOrder.getNumber() +
                 " already exists, and it's status indicate it is not changable"
                 );

                 save(dbBasedWorkOrder);
                 return;
             }
             */

            // Item item = getItemFromDatabase(dbBasedItem);
            logger.debug(">> will process Work Order:\n{}", workOrder);

            kafkaSender.send(IntegrationType.INTEGRATION_WORK_ORDER,
                    workOrder.getWarehouseId() + "-" + dbBasedWorkOrder.getId(),  workOrder);

            dbBasedWorkOrder.setStatus(IntegrationStatus.SENT);
            dbBasedWorkOrder.setErrorMessage("");

            dbBasedWorkOrder = save(dbBasedWorkOrder);

            // Save the WORK order line as well
            dbBasedWorkOrder.getWorkOrderLines().forEach(dbBasedWorkOrderLine ->{
                dbBasedWorkOrderLine.setStatus(IntegrationStatus.SENT);
                dbBasedWorkOrderLine.setErrorMessage("");

                dbBasedWorkOrderLineRepository.save(dbBasedWorkOrderLine);
            });

            dbBasedWorkOrder.getWorkOrderInstructions().forEach(dbBasedWorkOrderInstruction ->{
                dbBasedWorkOrderInstruction.setStatus(IntegrationStatus.SENT);
                dbBasedWorkOrderInstruction.setErrorMessage("");

                dbBasedWorkOrderInstructionRepository.save(dbBasedWorkOrderInstruction);
            });

            dbBasedWorkOrder.getWorkOrderByProduct().forEach(dbBasedWorkOrderByProduct ->{
                dbBasedWorkOrderByProduct.setStatus(IntegrationStatus.SENT);
                dbBasedWorkOrderByProduct.setErrorMessage("");

                dbBasedWorkOrderByProductRepository.save(dbBasedWorkOrderByProduct);
            });

            logger.debug(">> Work Order data process, {}", dbBasedWorkOrder.getStatus());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            dbBasedWorkOrder.setStatus(IntegrationStatus.ERROR);
            dbBasedWorkOrder.setErrorMessage(ex.getMessage());

            dbBasedWorkOrder = save(dbBasedWorkOrder);

            // Save the WORK order line as well
            dbBasedWorkOrder.getWorkOrderLines().forEach(dbBasedWorkOrderLine ->{
                dbBasedWorkOrderLine.setStatus(IntegrationStatus.ERROR);
                dbBasedWorkOrderLine.setErrorMessage(ex.getMessage());

                dbBasedWorkOrderLineRepository.save(dbBasedWorkOrderLine);
            });

            dbBasedWorkOrder.getWorkOrderInstructions().forEach(dbBasedWorkOrderInstruction ->{
                dbBasedWorkOrderInstruction.setStatus(IntegrationStatus.ERROR);
                dbBasedWorkOrderInstruction.setErrorMessage(ex.getMessage());

                dbBasedWorkOrderInstructionRepository.save(dbBasedWorkOrderInstruction);
            });

            dbBasedWorkOrder.getWorkOrderByProduct().forEach(dbBasedWorkOrderByProduct ->{
                dbBasedWorkOrderByProduct.setStatus(IntegrationStatus.ERROR);
                dbBasedWorkOrderByProduct.setErrorMessage(ex.getMessage());

                dbBasedWorkOrderByProductRepository.save(dbBasedWorkOrderByProduct);
            });
        }
    }

    private boolean validateExistingWorkOrder(WorkOrder workOrder) {
        logger.debug("Start to validate work order {} ", workOrder);
        WorkOrder existingWorkOrder = workOrderServiceRestemplateClient.getWorkOrderByNumber(
                workOrder.getWarehouseId(),
                workOrder.getNumber()
        );
        if (Objects.nonNull(existingWorkOrder) && !existingWorkOrder.getStatus().equals(WorkOrderStatus.PENDING)) {

            logger.debug("work order {} exists and current status {} indicate the work order is not changable ",
                    existingWorkOrder.getNumber(),
                    existingWorkOrder.getStatus());
            return false;
        }
        else if (Objects.nonNull(existingWorkOrder)) {

            logger.debug("work order {} exists and current status {} indicate the work order is changable ",
                    existingWorkOrder.getNumber(),
                    existingWorkOrder.getStatus());
        }
        else {

            logger.debug("work order {} doesn't exists",
                    workOrder.getNumber());
        }
        return true;
    }


    private void setupMissingField(WorkOrder workOrder, DBBasedWorkOrder dbBasedWorkOrder){


        Long warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                dbBasedWorkOrder.getCompanyId(),
                dbBasedWorkOrder.getCompanyCode(),
                dbBasedWorkOrder.getWarehouseId(),
                dbBasedWorkOrder.getWarehouseName()
        );

        if (Objects.isNull(warehouseId)) {
            throw ResourceNotFoundException.raiseException("Can't find warehouse id by " +
                    "company id: " + dbBasedWorkOrder.getCompanyId() +
                    "company code: " + dbBasedWorkOrder.getCompanyCode() +
                    "warehouse id: " + dbBasedWorkOrder.getWarehouseId() +
                    "warehouse name: " + dbBasedWorkOrder.getWarehouseName());
        }


        workOrder.setWarehouseId(warehouseId);
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId);

        workOrder.setWarehouse(warehouse);


        Item item = inventoryServiceRestemplateClient.getItemByName(
                warehouse.getCompany().getId(), warehouseId, dbBasedWorkOrder.getItemName()
                );
        if (Objects.isNull(item)) {
            throw ResourceNotFoundException.raiseException("Can't find item   by " +
                    "item name: " + dbBasedWorkOrder.getItemName() +
                    "warehouse id: " + warehouseId);
        }


        workOrder.setItemId(item.getId());
        workOrder.setItem(item);

        for(WorkOrderLine workOrderLine : workOrder.getWorkOrderLines()) {
            // Get the matched order line and setup the missing field
            // for
            // 1. item Id
            // 2. warehouse Id
            // 3. inventory status ID
            // 4. carrier ID
            // 5. carrier service level id
            for (DBBasedWorkOrderLine dbBasedWorkOrderLine: dbBasedWorkOrder.getWorkOrderLines()) {
                if (workOrderLine.getNumber().equals(dbBasedWorkOrderLine.getNumber())) {
                    setupMissingField(warehouse, workOrderLine, dbBasedWorkOrderLine);
                }
            }

        }


        workOrder.getWorkOrderByProducts().forEach(workOrderByProduct -> {

            dbBasedWorkOrder.getWorkOrderByProduct().forEach(dbBasedWorkOrderByProduct -> {
                if (workOrderByProduct.getItemName().equals(dbBasedWorkOrderByProduct.getItemName())) {
                    setupMissingField(warehouse, workOrderByProduct, dbBasedWorkOrderByProduct);
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
     * @param warehouse       Warehouse
     * @param workOrderLine
     * @param dbBasedWorkOrderLine
     */
    private void setupMissingField(Warehouse warehouse, WorkOrderLine workOrderLine, DBBasedWorkOrderLine dbBasedWorkOrderLine){

        workOrderLine.setWarehouseId(warehouse.getId());

        // 1. item Id

        if(Objects.isNull(workOrderLine.getItemId())) {

            Item item = inventoryServiceRestemplateClient.getItemByName(
                    warehouse.getCompany().getId(), warehouse.getId(), dbBasedWorkOrderLine.getItemName()
            );
            if (Objects.isNull(item)) {
                throw ResourceNotFoundException.raiseException("Can't find item   by " +
                        "item name: " + dbBasedWorkOrderLine.getItemName() +
                        "warehouse id: " + warehouse.getId());
            }
            workOrderLine.setItemId(item.getId());
        }



        // 3. inventory status ID
        if(Objects.isNull(workOrderLine.getInventoryStatusId())) {
            InventoryStatus inventoryStatus =
                    inventoryServiceRestemplateClient.getInventoryStatusByName(
                            warehouse.getId(), dbBasedWorkOrderLine.getInventoryStatusName()
                    );

            if (Objects.isNull(inventoryStatus)) {
                throw ResourceNotFoundException.raiseException("Can't find inventory status   by " +
                        "inventory status name: " + dbBasedWorkOrderLine.getInventoryStatusName() +
                        "warehouse id: " + warehouse.getId());
            }

            workOrderLine.setInventoryStatusId(inventoryStatus.getId()
            );
        }

    }


    private void setupMissingField(Warehouse warehouse, WorkOrderByProduct workOrderByProduct,
                                   DBBasedWorkOrderByProduct dbBasedWorkOrderByProduct){



        // 1. item Id
        if(Objects.isNull(workOrderByProduct.getItemId())) {
            workOrderByProduct.setItemId(
                    inventoryServiceRestemplateClient.getItemByName(
                            warehouse.getCompany().getId(), warehouse.getId(), dbBasedWorkOrderByProduct.getItemName()
                    ).getId()
            );
        }



        // 3. inventory status ID
        if(Objects.isNull(workOrderByProduct.getInventoryStatusId())) {
            workOrderByProduct.setInventoryStatusId(
                    inventoryServiceRestemplateClient.getInventoryStatusByName(
                            warehouse.getId(), dbBasedWorkOrderByProduct.getInventoryStatusName()
                    ).getId()
            );
        }

    }

    public void saveIntegrationResult(IntegrationResult integrationResult) {
        logger.debug("will update the Bill Of Material integration {}'s result to {}",
                integrationResult.getIntegrationId(),
                integrationResult.isSuccess());
        DBBasedWorkOrder dbBasedWorkOrder = findById(
                integrationResult.getIntegrationId()
        );
        IntegrationStatus integrationStatus =
                integrationResult.isSuccess() ? IntegrationStatus.COMPLETED : IntegrationStatus.ERROR;
        dbBasedWorkOrder.setStatus(integrationStatus);
        dbBasedWorkOrder.setErrorMessage(integrationResult.getErrorMessage());

        save(dbBasedWorkOrder);

        dbBasedWorkOrder.getWorkOrderLines().forEach(dbBasedWorkOrderLine ->{
            dbBasedWorkOrderLine.setStatus(integrationStatus);
            dbBasedWorkOrderLine.setErrorMessage(integrationResult.getErrorMessage());

            dbBasedWorkOrderLineRepository.save(dbBasedWorkOrderLine);
        });

        dbBasedWorkOrder.getWorkOrderByProduct().forEach(dbBasedWorkOrderByProduct ->{
            dbBasedWorkOrderByProduct.setStatus(integrationStatus);
            dbBasedWorkOrderByProduct.setErrorMessage(integrationResult.getErrorMessage());

            dbBasedWorkOrderByProductRepository.save(dbBasedWorkOrderByProduct);
        });

        dbBasedWorkOrder.getWorkOrderInstructions().forEach(dbBasedWorkOrderInstruction ->{
            dbBasedWorkOrderInstruction.setStatus(integrationStatus);
            dbBasedWorkOrderInstruction.setErrorMessage(integrationResult.getErrorMessage());

            dbBasedWorkOrderInstructionRepository.save(dbBasedWorkOrderInstruction);
        });
    }


    public IntegrationWorkOrderData resendWorkOrderData(Long id) {
        DBBasedWorkOrder dbBasedWorkOrder =
                findById(id);
        dbBasedWorkOrder.setStatus(IntegrationStatus.PENDING);
        dbBasedWorkOrder.setErrorMessage("");
        return save(dbBasedWorkOrder);
    }

}
