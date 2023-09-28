/**
 * Copyright 2018
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.workorder.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.OutboundServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderLineRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@Service
public class WorkOrderLineService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderLineService.class);

    @Autowired
    private WorkOrderLineRepository workOrderLineRepository;
    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private ProductionLineDeliveryService productionLineDeliveryService;
    @Autowired
    private ProductionLineAssignmentService productionLineAssignmentService;
    @Autowired
    private WorkOrderConfigurationService workOrderConfigurationService;

    @Autowired
    private WorkOrderLineSparePartService workOrderLineSparePartService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    private OutboundServiceRestemplateClient outboundServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.work-order-line:work-order-line}")
    String testDataFile;

    public WorkOrderLine findById(Long id, boolean loadDetails) {
        WorkOrderLine workOrderLine = workOrderLineRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order line not found by id: " + id));
        if (loadDetails) {
            loadAttribute(workOrderLine, true, true);
        }
        return workOrderLine;
    }

    public WorkOrderLine findById(Long id) {
        return findById(id, true);
    }


    public List<WorkOrderLine> findAll(boolean loadDetails) {
        List<WorkOrderLine> workOrderLines = workOrderLineRepository.findAll();

        if (workOrderLines.size() > 0 && loadDetails) {
            loadAttribute(workOrderLines, true, true);
        }
        return workOrderLines;
    }

    public List<WorkOrderLine> findAll() {
        return findAll(true);
    }


    public void loadAttribute(List<WorkOrderLine> workOrderLines,  boolean loadPicks, boolean loadShortAllocations) {
        for (WorkOrderLine workOrderLine : workOrderLines) {
            loadAttribute(workOrderLine, loadPicks, loadShortAllocations);
        }
    }

    public void loadAttribute(WorkOrderLine workOrderLine, boolean loadPicks, boolean loadShortAllocations) {

        workOrderLine.setWorkOrderNumber(
                Objects.nonNull(workOrderLine.getWorkOrder()) ? workOrderLine.getWorkOrder().getNumber() : ""
        );
        if (Objects.nonNull(workOrderLine.getWorkOrder())) {

            if (workOrderLine.getWorkOrder().getWarehouseId() != null && workOrderLine.getWorkOrder().getWarehouse() == null) {
                workOrderLine.getWorkOrder().setWarehouse(
                        warehouseLayoutServiceRestemplateClient.getWarehouseById(workOrderLine.getWorkOrder().getWarehouseId()));
            }
        }
        if (workOrderLine.getItemId() != null &&
                (workOrderLine.getItem() == null || Objects.isNull(workOrderLine.getItem().getId()))) {
             workOrderLine.setItem(inventoryServiceRestemplateClient.getItemById(workOrderLine.getItemId()));
         }
        if (workOrderLine.getInventoryStatusId() != null && workOrderLine.getInventoryStatus() == null) {
             workOrderLine.setInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(workOrderLine.getInventoryStatusId()));
         }

        workOrderLineSparePartService.loadAttribute(workOrderLine.getWorkOrderLineSpareParts());

        if (loadPicks) {


            workOrderLine.setPicks(outboundServiceRestemplateClient.getWorkOrderLinePicks(workOrderLine));

        }

        if (loadShortAllocations) {


            workOrderLine.setShortAllocations(outboundServiceRestemplateClient.getWorkOrderLineShortAllocations(workOrderLine));

        }



    }

    public WorkOrderLine findByNumber(Long warehouseId, String workOrderNumber, String number) {
        return findByNumber(warehouseId, workOrderNumber, number, true);

    }

    public WorkOrderLine findByNumber(Long warehouseId, String workOrderNumber, String number, boolean loadDetails) {

        WorkOrderLine workOrderLine
                = workOrderLineRepository.findByNaturalKey(
                         warehouseId, workOrderNumber,number);
        if (workOrderLine != null && loadDetails) {
            loadAttribute(workOrderLine, true, true);
        }
        return workOrderLine;
    }



    public WorkOrderLine save(WorkOrderLine workOrderLine) {
        return save(workOrderLine, true);
    }

    public WorkOrderLine save(WorkOrderLine workOrderLine, boolean loadDetails) {
        WorkOrderLine newWorkOrderLine = workOrderLineRepository.save(workOrderLine);
        if (loadDetails) {

            loadAttribute(newWorkOrderLine, true, true);
        }
        return newWorkOrderLine;
    }

    public WorkOrderLine saveAndFlush(WorkOrderLine workOrderLine) {
        WorkOrderLine newWorkOrderLine = workOrderLineRepository.saveAndFlush(workOrderLine);
        // workOrderLineRepository.flush();
        loadAttribute(newWorkOrderLine, true, true);
        return newWorkOrderLine;
    }
    public WorkOrderLine saveOrUpdate(WorkOrderLine workOrderLine) {
        return saveOrUpdate(workOrderLine, true);
    }

    public WorkOrderLine saveOrUpdate(WorkOrderLine workOrderLine, boolean loadDetails) {
        Long warehouseId = workOrderLine.getWorkOrder().getWarehouseId();
        String workOrderNumber = workOrderLine.getWorkOrder().getNumber();
        String number = workOrderLine.getNumber();

        if (workOrderLine.getId() == null
                && findByNumber(warehouseId, workOrderNumber, number, false) != null) {
            workOrderLine.setId(
                    findByNumber(warehouseId, workOrderNumber, number, false).getId());
        }
        return save(workOrderLine, loadDetails);
    }


    public void delete(WorkOrderLine workOrderLine) {
        workOrderLineRepository.delete(workOrderLine);
    }

    public void delete(Long id) {
        workOrderLineRepository.deleteById(id);
    }

    public void delete(String workOrderLineIds) {
        if (!workOrderLineIds.isEmpty()) {
            long[] workOrderLineIdArray = Arrays.asList(workOrderLineIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : workOrderLineIdArray) {
                delete(id);
            }
        }
    }


    public List<WorkOrderLineCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("workOrder").
                addColumn("number").
                addColumn("item").
                addColumn("expectedQuantity").
                addColumn("inventoryStatus").
                addColumn("allocationStrategyType").
                build().withHeader();

        return fileService.loadData(inputStream, schema, WorkOrderLineCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<WorkOrderLineCSVWrapper> workOrderLineCSVWrappers = loadData(inputStream);
            workOrderLineCSVWrappers.stream().forEach(workOrderLineCSVWrapper -> saveOrUpdate(convertFromWrapper(workOrderLineCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private WorkOrderLine convertFromWrapper(WorkOrderLineCSVWrapper workOrderLineCSVWrapper) {

        WorkOrderLine workOrderLine = new WorkOrderLine();

        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                        workOrderLineCSVWrapper.getCompany(),
                      workOrderLineCSVWrapper.getWarehouse()
                );

        workOrderLine.setWorkOrder(
                workOrderService.findByNumber(
                        warehouse.getId(), workOrderLineCSVWrapper.getWorkOrder()
                )
        );

        workOrderLine.setNumber(workOrderLineCSVWrapper.getNumber());

        workOrderLine.setItemId(
                inventoryServiceRestemplateClient.getItemByName(
                        warehouse.getId(), workOrderLineCSVWrapper.getItem()).getId()
        );

        workOrderLine.setExpectedQuantity(workOrderLineCSVWrapper.getExpectedQuantity());
        workOrderLine.setOpenQuantity(workOrderLineCSVWrapper.getExpectedQuantity());
        workOrderLine.setInprocessQuantity(0L);
        workOrderLine.setDeliveredQuantity(0L);
        workOrderLine.setConsumedQuantity(0L);

        workOrderLine.setInventoryStatusId(
                inventoryServiceRestemplateClient.getInventoryStatusByName(
                        warehouse.getId(), workOrderLineCSVWrapper.getInventoryStatus()).getId()
        );

        if (StringUtils.isNotBlank(workOrderLineCSVWrapper.getAllocationStrategyType())) {
            workOrderLine.setAllocationStrategyType(AllocationStrategyType.valueOf(
                    workOrderLineCSVWrapper.getAllocationStrategyType()
            ));
            logger.debug("Work Order line's allocation strategy type: {}",
                    workOrderLine.getAllocationStrategyType());
        }
        else {
            workOrderLine.setAllocationStrategyType(AllocationStrategyType.FIRST_IN_FIRST_OUT);
            logger.debug("Work Order line's allocation strategy type default to: {}",
                    workOrderLine.getAllocationStrategyType());
        }
        return workOrderLine;
    }


    public WorkOrderLine createWorkOrderLineFromBOMLine(WorkOrder workOrder, long workOrderExpectedQuantity,
                                                        double billOfMaterialExpectedQuantity,
                                                        BillOfMaterialLine billOfMaterialLine) {
        WorkOrderLine workOrderLine = new WorkOrderLine();
        workOrderLine.setWorkOrder(workOrder);
        workOrderLine.setNumber(billOfMaterialLine.getNumber());
        workOrderLine.setItemId(billOfMaterialLine.getItemId());
        workOrderLine.setInventoryStatusId(billOfMaterialLine.getInventoryStatusId());

        workOrderLine.setExpectedQuantity(
                (long)Math.ceil(
                        billOfMaterialLine.getExpectedQuantity() * 1.0 * workOrderExpectedQuantity / billOfMaterialExpectedQuantity));
        workOrderLine.setOpenQuantity(
                (long)Math.ceil(
                        billOfMaterialLine.getExpectedQuantity()  * 1.0 * workOrderExpectedQuantity / billOfMaterialExpectedQuantity));
        workOrderLine.setInprocessQuantity(0L);
        workOrderLine.setDeliveredQuantity(0L);
        workOrderLine.setConsumedQuantity(0L);
        //TO-DO: Default to FIFO for now
        workOrderLine.setAllocationStrategyType(AllocationStrategyType.FIRST_IN_FIRST_OUT);

        return save(workOrderLine);


    }


    /**
     * Return the quantities when the pick for a work order line is cancelled. We will
     * return the quantity to the work order line as well as the production line assignment
     * @param workOrderLineId
     * @param cancelledQuantity
     * @param destinationLocationId Production In staging Location id. we can use this id to find
     *                              the assigned production line
     */
    @Transactional
    public void registerPickCancelled(Long workOrderLineId, Long cancelledQuantity,
                                      Long destinationLocationId) {
        registerPickCancelled(findById(workOrderLineId), cancelledQuantity, destinationLocationId);
    }

    /**
     * Return the quantities when the pick for a work order line is cancelled. We will
     * return the quantity to the work order line as well as the production line assignment
     * @param workOrderLine
     * @param cancelledQuantity
     * @param destinationLocationId Production In staging Location id. we can use this id to find
     *                              the assigned production line
     */
    @Transactional
    public void registerPickCancelled(WorkOrderLine workOrderLine, Long cancelledQuantity,
                                      Long destinationLocationId) {
        logger.debug("registerPickCancelled: work order line: {}, cancelledQuantity: {}, destinationLocationId: {}",
                workOrderLine.getNumber(), cancelledQuantity, destinationLocationId);
        workOrderLine.setOpenQuantity(workOrderLine.getOpenQuantity() + cancelledQuantity);
        workOrderLine.setInprocessQuantity(workOrderLine.getInprocessQuantity() - cancelledQuantity);
        workOrderLine = save(workOrderLine);
        logger.debug("after pick cancelled, work order line {} has open quantity {}, in process quantity: {}",
                workOrderLine.getNumber(), workOrderLine.getOpenQuantity(), workOrderLine.getInprocessQuantity());

        // if the work order is alraedy assigned to some production, let's reset the prodution line assignment as well
        WorkOrder workOrder = workOrderLine.getWorkOrder();
        List<ProductionLineAssignment> productionLineAssignments =
                productionLineAssignmentService.findAll(workOrderLine.getWorkOrder().getWarehouseId(),
                        null,"", workOrder.getId(), null, false);
        // loop through all production line assignment until we find the one match with the pick's destination location id
        // which should be the production line's in staging location
        long workOrderLineId = workOrderLine.getId();
        productionLineAssignments.stream().filter(
                productionLineAssignment -> productionLineAssignment.getProductionLine().getInboundStageLocationId().equals(destinationLocationId)
        ).forEach(
                productionLineAssignment -> {
                    logger.debug("We find production line {} by its inbound stage location id {}, we will reset the assignment",
                            productionLineAssignment.getProductionLine().getName(),
                            destinationLocationId);
                    productionLineAssignment.getLines().stream().filter(
                            productionLineAssignmentLine -> productionLineAssignmentLine.getWorkOrderLine().getId().equals(workOrderLineId)
                    ).forEach(
                            productionLineAssignmentLine -> {
                                logger.debug("We find the productionLineAssignmentLine for this work order line," +
                                        " will set it's open quantity from {} to {}",
                                productionLineAssignmentLine.getOpenQuantity(), productionLineAssignmentLine.getOpenQuantity() + cancelledQuantity);
                                productionLineAssignmentLine.setOpenQuantity(
                                        productionLineAssignmentLine.getOpenQuantity() + cancelledQuantity
                                );
                                productionLineAssignmentService.saveLine(productionLineAssignmentLine);
                            }
                    );
                }
        );

    }
    @Transactional
    public void registerShortAllocationCancelled(Long workOrderLineId, Long cancelledQuantity) {

        registerShortAllocationCancelled(findById(workOrderLineId), cancelledQuantity);
    }

    @Transactional
    public void registerShortAllocationCancelled(WorkOrderLine workOrderLine, Long cancelledQuantity) {
        logger.debug("registerShortAllocationCancelled: work order line: {}, cancelledQuantity: {}",
                workOrderLine.getNumber(), cancelledQuantity);
        workOrderLine.setOpenQuantity(workOrderLine.getOpenQuantity() + cancelledQuantity);
        workOrderLine.setInprocessQuantity(workOrderLine.getInprocessQuantity() - cancelledQuantity);
        workOrderLine = save(workOrderLine);
        logger.debug("after pick cancelled, work order line {} has open quantity {}, in process quantity: {}",
                workOrderLine.getNumber(), workOrderLine.getOpenQuantity(), workOrderLine.getInprocessQuantity());

    }

    @Transactional
    public void consume(WorkOrderLine workOrderLine, WorkOrderLineConsumeTransaction workOrderLineConsumeTransaction, ProductionLine productionLine) {

        logger.debug("Start to process the work order line consume transaction");
        logger.debug("1. consume directly from the picked inventory. quantity: {}", workOrderLineConsumeTransaction.getConsumedQuantity() );
        logger.debug("2. consume from not picked LPN: {} LPNs", workOrderLineConsumeTransaction.getWorkOrderLineConsumeLPNTransactions().size());
        workOrderLineConsumeTransaction.getWorkOrderLineConsumeLPNTransactions().forEach(
                workOrderLineConsumeLPNTransaction ->
                        logger.debug("> will consume quantity {} from lpn {}",
                                workOrderLineConsumeLPNTransaction.getLpn(),
                                workOrderLineConsumeLPNTransaction.getConsumedQuantity())
        );
        logger.debug("3. consume from another work order? {}",
                Objects.nonNull(workOrderLineConsumeTransaction.getConsumeFromWorkOrder()));
        if (Objects.nonNull(workOrderLineConsumeTransaction.getConsumeFromWorkOrder())) {
            logger.debug("> work order number {}, production line {}, quantity: {}",
                    workOrderLineConsumeTransaction.getConsumeFromWorkOrder().getNumber(),
                    workOrderLineConsumeTransaction.getConsumeFromWorkOrderProductionLine().getName(),
                    workOrderLineConsumeTransaction.getConsumeFromWorkOrderQuantity());
        }

        // step 1, consume from the picked inventory
        if (workOrderLineConsumeTransaction.getConsumedQuantity() > 0) {
            logger.debug("Step 1: consume {} from picked inventory",
                    workOrderLineConsumeTransaction.getConsumedQuantity());
            consume(workOrderLine, workOrderLineConsumeTransaction.getConsumedQuantity(),
                    productionLine);
        }
        if (workOrderLineConsumeTransaction.getWorkOrderLineConsumeLPNTransactions().size() > 0) {

            logger.debug("Step 2: consume  from not picked inventory");
            workOrderLineConsumeTransaction.getWorkOrderLineConsumeLPNTransactions().forEach(
                    workOrderLineConsumeLPNTransaction -> consume(
                            workOrderLine, workOrderLineConsumeLPNTransaction, productionLine
                    )
            );
        }if (Objects.nonNull(workOrderLineConsumeTransaction.getConsumeFromWorkOrder())) {
            logger.debug("Step 3: consume from  work order number {}, production line {}, quantity: {}",
                    workOrderLineConsumeTransaction.getConsumeFromWorkOrder().getNumber(),
                    workOrderLineConsumeTransaction.getConsumeFromWorkOrderProductionLine().getName(),
                    workOrderLineConsumeTransaction.getConsumeFromWorkOrderQuantity());
            consume(workOrderLine,
                    workOrderLineConsumeTransaction.getConsumeFromWorkOrder(),
                    workOrderLineConsumeTransaction.getConsumeFromWorkOrderProductionLine(),
                    workOrderLineConsumeTransaction.getConsumeFromWorkOrderQuantity(),
                    productionLine);
        }
    }

    /**
     * produce the raw material from another work order and consume it for this work order
     * @param workOrderLine this work order's line
     * @param consumeFromWorkOrder another work order that will produce raw material
     * @param consumeFromWorkOrderProductionLine production line of another work order to produce the raw material
     * @param consumeFromWorkOrderQuantity total quantity that will be produced from the other work order
     * @param productionLine this work order's production line
     */
    private void consume(WorkOrderLine workOrderLine, WorkOrder consumeFromWorkOrder,
                         ProductionLine consumeFromWorkOrderProductionLine,
                         Long consumeFromWorkOrderQuantity,
                         ProductionLine productionLine) {
    }

    /**
     * Consume a not picked inventory for the work order line. The lpn needs to be in the
     * inbound staging of the production line
     * @param workOrderLine work order line
     * @param workOrderLineConsumeLPNTransaction non picked lpn to be consumed
     * @param productionLine production line
     */
    private void consume(WorkOrderLine workOrderLine,
                         WorkOrderLineConsumeLPNTransaction workOrderLineConsumeLPNTransaction,
                         ProductionLine productionLine) {

        // consume the LPN
        inventoryServiceRestemplateClient.consumeMaterialForWorkOrderLine(
                workOrderLine.getId(),
                workOrderLine.getWorkOrder().getWarehouseId(),
                workOrderLineConsumeLPNTransaction.getConsumedQuantity(),
                productionLine.getInboundStageLocationId(),
                null, workOrderLineConsumeLPNTransaction.getLpn(), true
        );

        // we don't need to setup the consume quantity on the production line delivery record
        // since the LPN is not picked for this work order. So we won't calculate it as
        // delivered inventory
        // productionLineDeliveryService.addConsumedQuantity(workOrderLine, productionLine, consumedQuantity);


        workOrderLine.setConsumedQuantity(workOrderLine.getConsumedQuantity() +
                workOrderLineConsumeLPNTransaction.getConsumedQuantity());

        save(workOrderLine);
    }

    @Transactional
    public void consume(WorkOrderLine workOrderLine, Long consumedQuantity, ProductionLine productionLine) {
        consume(workOrderLine, consumedQuantity, productionLine, null);
    }
    @Transactional
    public void consume(WorkOrderLine workOrderLine, Long consumedQuantity, ProductionLine productionLine,
                        Long inventoryId) {

        // make sure the total consumed quantity won't exceed total delivered quantity
        /**
         * Obsolete the logic. We will validate the quantity in
         * WorkOrderProduceTransactionService.validateWorkOrderProduceTransaction since we switch
         * to multiple production per work order modal.
         * */
        /***
         *
        if (workOrderLine.getConsumedQuantity() + consumedQuantity > workOrderLine.getDeliveredQuantity()) {

            throw WorkOrderException.raiseException("Can't consume more than delivered. Total Delivered: " +
                    workOrderLine.getDeliveredQuantity() + ", already consumed: " +
                    workOrderLine.getConsumedQuantity() + ", will be consumed this time: " + consumedQuantity);
        }

         */
        // Let's consume the inventory and remove it from the production line

        logger.debug("Start to consume inventory with quantity {} from work order line {} / {}, production line {}",
                consumedQuantity,
                workOrderLine.getWorkOrder().getNumber(),
                workOrderLine.getItem().getName(),
                productionLine.getName());
        inventoryServiceRestemplateClient.consumeMaterialForWorkOrderLine(
                workOrderLine.getId(),
                workOrderLine.getWorkOrder().getWarehouseId(),
                consumedQuantity,
                productionLine.getInboundStageLocationId(),
                inventoryId, "", null
        );

        // setup the consume quantity on the production line delivery record
        productionLineDeliveryService.addConsumedQuantity(workOrderLine, productionLine, consumedQuantity);


        workOrderLine.setConsumedQuantity(workOrderLine.getConsumedQuantity() + consumedQuantity);

        save(workOrderLine);
    }


    /**
     * Change the delivered quantity of the work order line when inventory
     * is delivered.
     * Lock the method so that when the picking routine in the ourbound service
     * delivered several inventory for the same work order line in one transaction
     * we won't end up with incorrect quantity
     * TO-DO: Will need swtich to message queue to get rid of the lock
     * @param workOrderLineId
     * @param quantityBeingDelivered
     * @param deliveredLocationId
     * @return the consume time so we know whether the inventory being delivered needs to be consumed
     */
    synchronized public WorkOrderMaterialConsumeTiming changeDeliveredQuantity(Long workOrderLineId,
                                                 Long quantityBeingDelivered,
                                                 Long deliveredLocationId,
                                                                               Long inventoryId) {
        // clear the cache. We may have scenario that when confirm
        // picks for the same work order line, we will update the same work order line
        // and production line delivery entity at the same time. Even we add the
        // synchronized keyword here to avoid concurrent read and write, we will still
        // need to call entityManger.clear() to clear the cache so every time we
        // start with a most recent work order line object
        // logger.debug("clear the entity manager's cache");
        // entityManager.clear();


        WorkOrderLine workOrderLine = findById(workOrderLineId);

        logger.debug("Will check if we need to update the delivered quantity");
        logger.debug("Current work order line {} / {} 's delivered quantity: {}, delivered inventory id {}",
                workOrderLine.getWorkOrder().getNumber(),
                workOrderLine.getNumber(),
                workOrderLine.getDeliveredQuantity(),
                Objects.isNull(inventoryId) ? "N/A" : inventoryId);

        logger.debug("quantity delivered: {}", quantityBeingDelivered);
        // Make sure the inventory was delivered to the right location,
        // which should be the IN staging of the production line
        Optional<ProductionLineAssignment> matchedProductionLineAssignment =
                workOrderLine.getWorkOrder()
                        .getProductionLineAssignments().stream()
                .filter(productionLineAssignment ->
                        productionLineAssignment.getProductionLine().
                                getInboundStageLocationId().equals(deliveredLocationId))
                .findFirst();
        if (matchedProductionLineAssignment.isPresent()){
            ProductionLineAssignment productionLineAssignment =
                    matchedProductionLineAssignment.get();

            // update the delivery quantity of the production assignment
            logger.debug("Add delivery quantity {} to line {} for work order {}",
                    quantityBeingDelivered,
                    productionLineAssignment.getProductionLine().getName(),
                    productionLineAssignment.getWorkOrder().getNumber());
            productionLineDeliveryService.
                    addDeliveryQuantity(workOrderLine,
                            productionLineAssignment.getProductionLine(),
                             quantityBeingDelivered);
            workOrderLine.setDeliveredQuantity(workOrderLine.getDeliveredQuantity() + quantityBeingDelivered);

            // if we configure to consume the quantity right after deliver, then
            // consume the inventory
            WorkOrderMaterialConsumeTiming workOrderMaterialConsumeTiming =
                    workOrderConfigurationService.getWorkOrderMaterialConsumeTiming(
                            workOrderLine.getWorkOrder());
            if (workOrderMaterialConsumeTiming.equals(WorkOrderMaterialConsumeTiming.WHEN_DELIVER)) {
                logger.debug("# Configuration is setup to consume the inventory right after delivery, will consume the inventory");
                consume(workOrderLine, quantityBeingDelivered, productionLineAssignment.getProductionLine(),
                        inventoryId);
            }

            logger.debug("# will update work order line {} / {} 's delivered quantity to {}",
                    workOrderLine.getWorkOrder().getNumber(),
                    workOrderLine.getNumber(),
                    workOrderLine.getDeliveredQuantity());
            WorkOrderLine newWorkOrderLine = saveAndFlush(workOrderLine);
            logger.debug("# Now work order line {} / {} 's delivered quantity is {}",
                    newWorkOrderLine.getWorkOrder().getNumber(),
                    newWorkOrderLine.getNumber(),
                    newWorkOrderLine.getDeliveredQuantity());
            return workOrderMaterialConsumeTiming;
        }
        else {
            return null;
        }
    }

    @Transactional
    public WorkOrderLine overrideConsumedQuantity(WorkOrderLine workOrderLine, Long newConsumedQuantity) {

        if (newConsumedQuantity >= workOrderLine.getDeliveredQuantity()) {
            throw WorkOrderException.raiseException("Can't consume more than delivered. " +
                    "Consumed Quantity: " + newConsumedQuantity + ", Delivered Quantity: " +
                    workOrderLine.getDeliveredQuantity());
        }

        workOrderLine.setConsumedQuantity(newConsumedQuantity);

        return saveOrUpdate(workOrderLine);
    }


    @Transactional
    public WorkOrderLine refreshQuantityAfterUnpickInventory(WorkOrderLine workOrderLine, Long unpickedQuantity) {

        if (unpickedQuantity >= workOrderLine.getDeliveredQuantity()) {
            throw WorkOrderException.raiseException("Can't unpick more than delivered. " +
                    "Unpicked Quantity: " + unpickedQuantity + ", Delivered Quantity: " +
                    workOrderLine.getDeliveredQuantity());
        }

        workOrderLine.setDeliveredQuantity(workOrderLine.getDeliveredQuantity() - unpickedQuantity);
        // return the unpicked quantity back to open quantity
        workOrderLine.setInprocessQuantity(workOrderLine.getInprocessQuantity() - unpickedQuantity);
        workOrderLine.setOpenQuantity(workOrderLine.getOpenQuantity() + unpickedQuantity);

        return saveOrUpdate(workOrderLine);
    }


    public WorkOrderLine completeWorkOrderLine(WorkOrderLine workOrderLine,
                                      Long consumedQuantity, Long scrappedQuantity, Long returnedMaterialsQuantity) {
        workOrderLine.setConsumedQuantity(consumedQuantity);
        workOrderLine.setInprocessQuantity(0L);
        workOrderLine.setScrappedQuantity(scrappedQuantity);
        workOrderLine.setReturnedQuantity(returnedMaterialsQuantity);
        return saveOrUpdate(workOrderLine);

    }

    public void modifyWorkOrderLineExpectedQuantity(WorkOrderLine existingWorkOrderLine, Long newExpectedQuantity) {
        // If the user is changing the quantity down, then we will need to make sure
        // we still have enough open quantity to adjust down
        if (newExpectedQuantity.equals(existingWorkOrderLine.getExpectedQuantity())) {
            return;
        }
        // we are adjust down the quantity. let's make sure we still
        // have enough open quantity after adjust down
        else if (existingWorkOrderLine.getOpenQuantity() < existingWorkOrderLine.getExpectedQuantity() - newExpectedQuantity){
            throw WorkOrderException.raiseException("There's only " + existingWorkOrderLine.getOpenQuantity() + " left on " +
                    "the line. We can't adjust the expected quantity from " + existingWorkOrderLine.getExpectedQuantity() +
                    " to " + newExpectedQuantity);
        }
        else {
            // we are adjust up the quantity. no validation is needed
            existingWorkOrderLine.setOpenQuantity(
                    existingWorkOrderLine.getOpenQuantity() + (
                            newExpectedQuantity - existingWorkOrderLine.getExpectedQuantity()
                            )
            );
            existingWorkOrderLine.setExpectedQuantity(newExpectedQuantity);
            saveOrUpdate(existingWorkOrderLine);
        }
    }

    public void removeWorkOrderLine(WorkOrder workOrder, String workOrderLineNumber) {
        WorkOrderLine workOrderLine = findByNumber(workOrder.getWarehouseId(), workOrder.getNumber(), workOrderLineNumber);
        delete(workOrderLine);
    }

    public void addWorkOrderLine(WorkOrder workOrder, WorkOrderLine workOrderLine) {
        if (Objects.isNull(workOrderLine.getWorkOrder())) {
            workOrderLine.setWorkOrder(workOrder);
        }

        saveOrUpdate(workOrderLine);
    }

    public WorkOrderLine changeSpareParts(Long id, List<WorkOrderLineSparePart> workOrderLineSpareParts) {
        WorkOrderLine workOrderLine = findById(id);

        // see if we may need to remove any spare part
        List<WorkOrderLineSparePart> existingWorkOrderLineSpareParts = workOrderLine.getWorkOrderLineSpareParts();


        logger.debug("existing order line spare part \n {}",
                existingWorkOrderLineSpareParts);

        logger.debug("new order line spare part \n {}",
                workOrderLineSpareParts);
        Set<Long> sparePartToBeRemoved = new HashSet<>();
        Set<String> newSparePartNames = new HashSet<>();

        workOrderLineSpareParts.forEach(
                newWorkOrderLineSparePart -> newSparePartNames.add(newWorkOrderLineSparePart.getName())
        );

        existingWorkOrderLineSpareParts.forEach(
                existingWorkOrderLineSparePart -> {
                    // if the new spare parts list doesn't have the name any more, we will remove it from
                    // the database
                    if (!newSparePartNames.contains(existingWorkOrderLineSparePart.getName())) {
                        sparePartToBeRemoved.add(existingWorkOrderLineSparePart.getId());
                    }
                }
        );

        workOrderLineSpareParts.forEach(
                newWorkOrderLineSparePart -> {
                    newWorkOrderLineSparePart.setWorkOrderLine(workOrderLine);
                    newWorkOrderLineSparePart.getWorkOrderLineSparePartDetails().forEach(
                            workOrderLineSparePartDetail -> workOrderLineSparePartDetail.setWorkOrderLineSparePart(
                                    newWorkOrderLineSparePart
                            )
                    );
                    workOrderLineSparePartService.saveOrUpdate(newWorkOrderLineSparePart);
                }
        );
        // get the work order line spare part again with latest change. We will remove the spare part
        // information is necessary
        WorkOrderLine newWorkOrderLine = findById(id);

        // see if we may need to remove any spare part
        existingWorkOrderLineSpareParts = newWorkOrderLine.getWorkOrderLineSpareParts();
        Iterator<WorkOrderLineSparePart> workOrderLineSparePartIterator = existingWorkOrderLineSpareParts.iterator();
        while(workOrderLineSparePartIterator.hasNext()) {
            WorkOrderLineSparePart workOrderLineSparePart = workOrderLineSparePartIterator.next();
            if (sparePartToBeRemoved.contains(workOrderLineSparePart.getId())) {
                workOrderLineSparePartIterator.remove();
            }
        }

        return saveOrUpdate(newWorkOrderLine);
    }

    public void refreshSparePartQuantity(WorkOrderLine workOrderLine, long sparePartQuantityAdded) {
        workOrderLine.setSparePartQuantity(
                Objects.isNull(workOrderLine.getSparePartQuantity()) ? 0 : workOrderLine.getSparePartQuantity()
                + sparePartQuantityAdded
        );
        saveOrUpdate(workOrderLine);
    }

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        logger.debug("start to process item override for work order line, current warehouse {}, from item id {} to item id {}",
                warehouseId, oldItemId, newItemId);
        workOrderLineRepository.processItemOverride(warehouseId, oldItemId, newItemId);

    }
}
