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

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@Service
public class WorkOrderLineService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderLineService.class);

    @Autowired
    private WorkOrderLineRepository workOrderLineRepository;
    @Autowired
    private WorkOrderService workOrderService;

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
            loadAttribute(workOrderLine);
        }
        return workOrderLine;
    }

    public WorkOrderLine findById(Long id) {
        return findById(id, true);
    }


    public List<WorkOrderLine> findAll(boolean loadDetails) {
        List<WorkOrderLine> workOrderLines = workOrderLineRepository.findAll();

        if (workOrderLines.size() > 0 && loadDetails) {
            loadAttribute(workOrderLines);
        }
        return workOrderLines;
    }

    public List<WorkOrderLine> findAll() {
        return findAll(true);
    }


    public void loadAttribute(List<WorkOrderLine> workOrderLines) {
        for (WorkOrderLine workOrderLine : workOrderLines) {
            loadAttribute(workOrderLine);
        }
    }

    public void loadAttribute(WorkOrderLine workOrderLine) {

        if (Objects.nonNull(workOrderLine.getWorkOrder())) {

            if (workOrderLine.getWorkOrder().getWarehouseId() != null && workOrderLine.getWorkOrder().getWarehouse() == null) {
                workOrderLine.getWorkOrder().setWarehouse(
                        warehouseLayoutServiceRestemplateClient.getWarehouseById(workOrderLine.getWorkOrder().getWarehouseId()));
            }
        }
        if (workOrderLine.getItemId() != null && workOrderLine.getItem() == null) {
            workOrderLine.setItem(inventoryServiceRestemplateClient.getItemById(workOrderLine.getItemId()));
        }
        if (workOrderLine.getInventoryStatusId() != null && workOrderLine.getInventoryStatus() == null) {
            workOrderLine.setInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(workOrderLine.getInventoryStatusId()));
        }

        workOrderLine.setPicks(outboundServiceRestemplateClient.getWorkOrderLinePicks(workOrderLine));

        workOrderLine.setShortAllocations(outboundServiceRestemplateClient.getWorkOrderLineShortAllocations(workOrderLine));


    }

    public WorkOrderLine findByNumber(Long warehouseId, String workOrderNumber, String number) {
        return findByNumber(warehouseId, workOrderNumber, number, true);

    }

    public WorkOrderLine findByNumber(Long warehouseId, String workOrderNumber, String number, boolean loadDetails) {

        WorkOrderLine workOrderLine
                = workOrderLineRepository.findByNaturalKey(
                         warehouseId, workOrderNumber,number);
        if (workOrderLine != null && loadDetails) {
            loadAttribute(workOrderLine);
        }
        return workOrderLine;
    }



    public WorkOrderLine save(WorkOrderLine workOrderLine) {
        return save(workOrderLine, true);
    }

    public WorkOrderLine save(WorkOrderLine workOrderLine, boolean loadDetails) {
        WorkOrderLine newWorkOrderLine = workOrderLineRepository.save(workOrderLine);
        if (loadDetails) {

            loadAttribute(newWorkOrderLine);
        }
        return newWorkOrderLine;
    }

    public WorkOrderLine saveAndFlush(WorkOrderLine workOrderLine) {
        WorkOrderLine newWorkOrderLine = workOrderLineRepository.save(workOrderLine);
        workOrderLineRepository.flush();
        loadAttribute(newWorkOrderLine);
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
                && findByNumber(warehouseId, workOrderNumber, number) != null) {
            workOrderLine.setId(
                    findByNumber(warehouseId, workOrderNumber, number).getId());
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


    public WorkOrderLine createWorkOrderLineFromBOMLine(WorkOrder workOrder, Long workOrderCount,  BillOfMaterialLine billOfMaterialLine) {
        WorkOrderLine workOrderLine = new WorkOrderLine();
        workOrderLine.setWorkOrder(workOrder);
        workOrderLine.setNumber(billOfMaterialLine.getNumber());
        workOrderLine.setItemId(billOfMaterialLine.getItemId());
        workOrderLine.setInventoryStatusId(billOfMaterialLine.getInventoryStatusId());

        workOrderLine.setExpectedQuantity(billOfMaterialLine.getExpectedQuantity() * workOrderCount);
        workOrderLine.setOpenQuantity(billOfMaterialLine.getExpectedQuantity() * workOrderCount);
        workOrderLine.setInprocessQuantity(0L);
        workOrderLine.setDeliveredQuantity(0L);
        workOrderLine.setConsumedQuantity(0L);
        //TO-DO: Default to FIFO for now
        workOrderLine.setAllocationStrategyType(AllocationStrategyType.FIRST_IN_FIRST_OUT);

        return save(workOrderLine);


    }


    @Transactional
    public void registerPickCancelled(Long workOrderLineId, Long cancelledQuantity) {
        registerPickCancelled(findById(workOrderLineId), cancelledQuantity);
    }
    @Transactional
    public void registerPickCancelled(WorkOrderLine workOrderLine, Long cancelledQuantity) {
        logger.debug("registerPickCancelled: work order line: {}, cancelledQuantity: {}",
                workOrderLine.getNumber(), cancelledQuantity);
        workOrderLine.setOpenQuantity(workOrderLine.getOpenQuantity() + cancelledQuantity);
        workOrderLine.setInprocessQuantity(workOrderLine.getInprocessQuantity() - cancelledQuantity);
        workOrderLine = save(workOrderLine);
        logger.debug("after pick cancelled, work order line {} has open quantity {}, in process quantity: {}",
                workOrderLine.getNumber(), workOrderLine.getOpenQuantity(), workOrderLine.getInprocessQuantity());

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
    public void consume(WorkOrderLine workOrderLine, Long consumedQuantity, ProductionLine productionLine) {

        // make sure the total consumed quantity won't exceed total delivered quantity
        if (workOrderLine.getConsumedQuantity() + consumedQuantity > workOrderLine.getDeliveredQuantity()) {

            throw WorkOrderException.raiseException("Can't consume more than delivered. Total Delivered: " +
                    workOrderLine.getDeliveredQuantity() + ", already consumed: " +
                    workOrderLine.getConsumedQuantity() + ", will be consumed this time: " + consumedQuantity);
        }
        // Let's consume the inventory and remove it from the production line

        inventoryServiceRestemplateClient.consumeMaterialForWorkOrderLine(
                workOrderLine.getId(),
                workOrderLine.getWorkOrder().getWarehouseId(),
                consumedQuantity,
                productionLine.getInboundStageLocationId()
        );
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
     * @return
     */
    @Transactional
    synchronized public WorkOrderLine changeDeliveredQuantity(Long workOrderLineId,
                                                 Long quantityBeingDelivered,
                                                 Long deliveredLocationId) {

        WorkOrderLine workOrderLine = findById(workOrderLineId);

        logger.debug("Will check if we need to update the delivered quantity");

        logger.debug("quantity delivered: {}", quantityBeingDelivered);
        // Make sure the inventory was delivered to the right location,
        // which should be the IN staging of the production line
        if (workOrderLine.getWorkOrder()
                .getProductionLineAssignments().stream()
                .anyMatch(productionLineAssignment ->
                        productionLineAssignment.getProductionLine().getInboundStageLocationId().equals(deliveredLocationId))) {

            workOrderLine.setDeliveredQuantity(workOrderLine.getDeliveredQuantity() + quantityBeingDelivered);

            return saveAndFlush(workOrderLine);
        }
        else {
            return workOrderLine;
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
}
