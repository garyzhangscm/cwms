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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.OutboundServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.GenericException;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderRepository;
import org.apache.commons.lang.StringUtils;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class WorkOrderService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderService.class);

    @Autowired
    private WorkOrderRepository workOrderRepository;
    @Autowired
    private WorkOrderLineService workOrderLineService;
    @Autowired
    private WorkOrderByProductService workOrderByProductService;
    @Autowired
    private BillOfMaterialService billOfMaterialService;
    @Autowired
    private ProductionLineService productionLineService;
    @Autowired
    private ProductionPlanLineService productionPlanLineService;

    @Autowired
    private WorkOrderInstructionService workOrderInstructionService;
    @Autowired
    private WorkOrderKPIService workOrderKPIService;
    @Autowired
    private WorkOrderKPITransactionService workOrderKPITransactionService;
    @Autowired
    private ProductionLineAssignmentService productionLineAssignmentService;

    @Autowired
    private OutboundServiceRestemplateClient outboundServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;
    @Autowired
    private FileService fileService;
    @Autowired
    private IntegrationService integrationService;

    @Value("${fileupload.test-data.work-order:work-order}")
    String testDataFile;

    public WorkOrder findById(Long id, boolean loadDetails) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order not found by id: " + id));
        if (loadDetails) {
            loadAttribute(workOrder);
        }
        return workOrder;
    }

    public WorkOrder findById(Long id) {
        return findById(id, true);
    }


    public List<WorkOrder> findAll(Long warehouseId, String number,
                                   String itemName, Long productionPlanId,
                                   boolean loadDetails) {

        List<WorkOrder> workOrders =  workOrderRepository.findAll(
                (Root<WorkOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (!StringUtils.isBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));

                    }
                    if (!StringUtils.isBlank(itemName)) {
                        Item item = inventoryServiceRestemplateClient.getItemByName(warehouseId, itemName);
                        if (item != null) {
                            predicates.add(criteriaBuilder.equal(root.get("itemId"), item.getId()));
                        }
                        else {
                            // The client passed in an invalid item name, let's return nothing
                            predicates.add(criteriaBuilder.equal(root.get("itemId"), -1L));
                        }
                    }

                    if (Objects.nonNull(productionPlanId)) {
                        Join<WorkOrder, ProductionPlanLine> joinProductionPlanLine = root.join("productionPlanLine", JoinType.INNER);
                        Join<ProductionPlanLine, ProductionPlan> joinProductionPlan = joinProductionPlanLine.join("productionPlan", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinProductionPlan.get("id"), productionPlanId));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (workOrders.size() > 0 && loadDetails) {
            loadAttribute(workOrders);
        }
        return workOrders;
    }

    public List<WorkOrder> findAll(Long warehouseId, String number,
                                   String itemName, Long productionPlanId) {
        return findAll(warehouseId, number, itemName, productionPlanId, true);
    }


    public WorkOrder findByNumber(Long warehouseId, String number, boolean loadDetails) {
        WorkOrder workOrder = workOrderRepository.findByWarehouseIdAndNumber(warehouseId, number);
        if (workOrder != null && loadDetails) {
            loadAttribute(workOrder);
        }
        return workOrder;
    }

    public WorkOrder findByNumber(Long warehouseId,String number) {
        return findByNumber(warehouseId, number, true);
    }


    public void loadAttribute(List<WorkOrder> workOrders) {
        for (WorkOrder workOrder : workOrders) {
            loadAttribute(workOrder);
        }
    }

    public void loadAttribute(WorkOrder workOrder) {

        if (workOrder.getItemId() != null && workOrder.getItem() == null) {
            workOrder.setItem(inventoryServiceRestemplateClient.getItemById(workOrder.getItemId()));
        }
        if (workOrder.getWarehouseId() != null && workOrder.getWarehouse() == null) {
            workOrder.setWarehouse(warehouseLayoutServiceRestemplateClient.getWarehouseById(workOrder.getWarehouseId()));
        }

        // Load the item and inventory status information for each lines
        workOrder.getWorkOrderLines()
                .forEach(workOrderLine -> {
                    // Setup the work order on the work order line as
                    // the work order line depends on the work order's information
                    // to load some attributes
                    if (Objects.isNull(workOrderLine.getWorkOrder())) {
                        workOrderLine.setWorkOrder(workOrder);
                    }
                    workOrderLineService.loadAttribute(workOrderLine);
                });

        // Load the item and inventory status information for each lines
        workOrder.getWorkOrderByProducts()
                .forEach(workOrderByProduct -> {
                    // Setup the work order on the work order line as
                    // the work order line depends on the work order's information
                    // to load some attributes
                    if (Objects.isNull(workOrderByProduct.getWorkOrder())) {
                        workOrderByProduct.setWorkOrder(workOrder);
                    }
                    workOrderByProductService.loadAttribute(workOrderByProduct);
                });
    }


    public WorkOrder save(WorkOrder workOrder) {
        return save(workOrder, true);
    }

    public WorkOrder save(WorkOrder workOrder, boolean loadDetails) {
        WorkOrder newWorkOrder = workOrderRepository.save(workOrder);
        if (loadDetails) {

            loadAttribute(newWorkOrder);
        }
        return newWorkOrder;
    }

    public WorkOrder saveOrUpdate(WorkOrder workOrder) {
        return saveOrUpdate(workOrder, true);
    }
    public WorkOrder saveOrUpdate(WorkOrder workOrder, boolean loadDetails) {
        if (workOrder.getId() == null && findByNumber(workOrder.getWarehouseId(), workOrder.getNumber()) != null) {
            workOrder.setId(
                    findByNumber(workOrder.getWarehouseId(), workOrder.getNumber()).getId());
        }
        return save(workOrder, loadDetails);
    }


    public void delete(WorkOrder workOrder) {
        workOrderRepository.delete(workOrder);
    }

    public void delete(Long id) {
        workOrderRepository.deleteById(id);
    }

    public void delete(String workOrderIds) {
        if (!workOrderIds.isEmpty()) {
            long[] workOrderIdArray = Arrays.asList(workOrderIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : workOrderIdArray) {
                delete(id);
            }
        }
    }

    public List<WorkOrderCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("number").
                addColumn("item").
                addColumn("expectedQuantity").
                build().withHeader();

        return fileService.loadData(inputStream, schema, WorkOrderCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<WorkOrderCSVWrapper> workOrderCSVWrappers = loadData(inputStream);
            workOrderCSVWrappers.stream().forEach(workOrderCSVWrapper -> saveOrUpdate(convertFromWrapper(workOrderCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private WorkOrder convertFromWrapper(WorkOrderCSVWrapper workOrderCSVWrapper) {

        WorkOrder workOrder = new WorkOrder();
        workOrder.setNumber(workOrderCSVWrapper.getNumber());
        workOrder.setExpectedQuantity(workOrderCSVWrapper.getExpectedQuantity());
        workOrder.setProducedQuantity(0L);
        workOrder.setStatus(WorkOrderStatus.PENDING);

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                workOrderCSVWrapper.getCompany(),
                workOrderCSVWrapper.getWarehouse()
        );

        workOrder.setItemId(
                inventoryServiceRestemplateClient.getItemByName(
                        warehouse.getId(), workOrderCSVWrapper.getItem()).getId()
        );
        workOrder.setWarehouseId(warehouse.getId());

        return workOrder;
    }
    public WorkOrder createWorkOrderFromProductionPlanLine(ProductionPlanLine productionPlanLine,
                                            String workOrderNumber, Long expectedQuantity,
                                            Long productionLineId) {
        WorkOrder workOrder = createWorkOrderFromBOM(
                productionPlanLine.getBillOfMaterial().getId(),
                workOrderNumber, expectedQuantity,
                productionLineId
        );
        workOrder.setProductionPlanLine(productionPlanLine);
        return saveOrUpdate(workOrder);
    }
    public WorkOrder createWorkOrderFromBOM(Long billOfMaterialId,
                            String workOrderNumber, Long expectedQuantity,
                            Long productionLineId) {

        BillOfMaterial billOfMaterial = billOfMaterialService.findById(billOfMaterialId, false);

        WorkOrder workOrder = new WorkOrder();
        workOrder.setNumber(workOrderNumber);
        workOrder.setItemId(billOfMaterial.getItemId());
        workOrder.setWarehouseId(billOfMaterial.getWarehouseId());
        workOrder.setExpectedQuantity(expectedQuantity);
        workOrder.setProducedQuantity(0L);
        workOrder.setStatus(WorkOrderStatus.PENDING);


        WorkOrder savedWorkOrder = save(workOrder);


        // if the production line is passed in, assign the work order onto
        // the production line
        if (Objects.nonNull(productionLineId)) {
            productionLineAssignmentService.assignWorkOrderToProductionLines(
                    savedWorkOrder,
                    productionLineService.findById(productionLineId),
                    workOrder.getExpectedQuantity());
        }

        Long workOrderCount = expectedQuantity / billOfMaterial.getExpectedQuantity();
        // Start to create work order line
        billOfMaterial.getBillOfMaterialLines()
                .forEach(billOfMaterialLine ->
                        workOrderLineService.createWorkOrderLineFromBOMLine(savedWorkOrder, workOrderCount, billOfMaterialLine));

        // Start to create work order instruction
        billOfMaterial.getWorkOrderInstructionTemplates()
                .forEach(workOrderInstructionTemplate ->
                        workOrderInstructionService.createWorkOrderInstructionFromBOMLine(savedWorkOrder,workOrderInstructionTemplate));

        billOfMaterial.getBillOfMaterialByProducts()
                .forEach(billOfMaterialByProduct ->
                        workOrderByProductService.createWorkOrderByProductFromBOMByProduct(savedWorkOrder, workOrderCount, billOfMaterialByProduct));
        return findById(savedWorkOrder.getId());
    }

    public WorkOrder allocateWorkOrder(Long workOrderId) {
        WorkOrder workOrder = findById(workOrderId);
        logger.debug("Start to allocate work order: \n {}", workOrder);
            AllocationResult allocationResult
                    = outboundServiceRestemplateClient.allocateWorkOrder(workOrder);

            logger.debug("Get result for work order {} \n, {} picks, {} short allocations, {}",
                    workOrder.getNumber(), allocationResult.getPicks().size(),
                    allocationResult.getShortAllocations().size(), allocationResult);
            // After we get the allocation result,
            // let's update the quantity in each work order line

            // A map to store the quantities
            // Key: work order line id
            // value: pick quantity + short allocation quantity
            Map<Long, Long> inprocessQuantities = new HashMap<>();

            allocationResult.getPicks().forEach(pick -> {
                Long workOrderLineId = pick.getWorkOrderLineId();
                Long inprocessQuantity = inprocessQuantities.getOrDefault(workOrderLineId, 0L);
                inprocessQuantities.put(workOrderLineId, (inprocessQuantity + pick.getQuantity()));
            });

            allocationResult.getShortAllocations().forEach(shortAllocation -> {
                Long workOrderLineId = shortAllocation.getWorkOrderLineId();
                Long inprocessQuantity = inprocessQuantities.getOrDefault(workOrderLineId, 0L);
                inprocessQuantities.put(workOrderLineId, (inprocessQuantity + shortAllocation.getQuantity()));
            });

            inprocessQuantities.entrySet().stream().forEach(entry ->{
                WorkOrderLine workOrderLine = workOrderLineService.findById(entry.getKey());
                // Move the 'inprocess quantity' we just calculated from 'Open quantity'
                // to 'inprocess quantity'
                Long inprocessQuantity = entry.getValue();
                logger.debug("work order line {}'s inprocess quantity will be updated by {}",
                        workOrderLine.getNumber(), inprocessQuantity);
                // we may allocate more than necessary(when round up for the item is allowed)
                if (workOrderLine.getOpenQuantity() < inprocessQuantity) {
                    workOrderLine.setOpenQuantity(0L);
                }
                else {
                    workOrderLine.setOpenQuantity(workOrderLine.getOpenQuantity() - inprocessQuantity);

                }
                workOrderLine.setInprocessQuantity(workOrderLine.getInprocessQuantity() + inprocessQuantity);
                workOrderLineService.save(workOrderLine);
            });
            // If the current work order's status is 'Pending', change it to 'INPROCESS'
            if (workOrder.getStatus().equals(WorkOrderStatus.PENDING)) {
                workOrder.setStatus(WorkOrderStatus.INPROCESS);
                saveOrUpdate(workOrder);
            }


        // return the latest work order information
        return findById(workOrderId);
    }

/****
    public WorkOrder changeProductionLine(Long id,
                                          Long productionLineId){
        // only allow to change the production line when the status is
        // still pending
        WorkOrder workOrder = findById(id);
        if (!workOrder.getStatus().equals(WorkOrderStatus.PENDING)) {
            throw WorkOrderException.raiseException("Can't change the production line once the work order is started");
        }

        ProductionLine newProductionLine = productionLineService.findById(productionLineId);

        if (newProductionLine.getWorkOrderExclusiveFlag() == true &&
            newProductionLine.getWorkOrders().size() > 0) {
            // the production line is set to be exclusively occupies by
            // any work order and there's already some work work on this production line
            throw WorkOrderException.raiseException("There's already work order " +newProductionLine.getWorkOrders().get(0).getNumber()
                  + " on this production line");
        }

        // OK we are good to go
        workOrder.setProductionLine(newProductionLine);
        return saveOrUpdate(workOrder);

    }
 **/

    public WorkOrder produce(WorkOrder workOrder, Long producedQuantity) {
        workOrder.setProducedQuantity(workOrder.getProducedQuantity() + producedQuantity);
        return saveOrUpdate(workOrder);
    }


    public List<Inventory> getProducedInventory(Long workOrderId) {

        WorkOrder workOrder = findById(workOrderId);
        return inventoryServiceRestemplateClient.findProducedInventory(
                workOrder.getWarehouseId(),
                workOrderId
        );

    }

    public List<Inventory> getProducedByProduct(Long workOrderId) {

        WorkOrder workOrder = findById(workOrderId);
        String workOrderByProductIds =
                workOrder.getWorkOrderByProducts().stream()
                        .map(WorkOrderByProduct::getId).map(String::valueOf).collect(Collectors.joining(","));
        return inventoryServiceRestemplateClient.getProducedByProduct(
                workOrder.getWarehouseId(),
                workOrderByProductIds
        );

    }

    public List<Inventory> getDeliveredInventory(Long workOrderId) {
        WorkOrder workOrder = findById(workOrderId);

        // Get all the picked inventory that already arrived at the
        // right location
        if (workOrder.getProductionLineAssignments().size() == 0) {
            // The work order doesn't have production line assigned yet, probably
            // it is a new work order;
            return new ArrayList<>();
        }

        List<Inventory> deliveredInventory = new ArrayList<>();

        try {
            // Get all the picks that belongs to this work order
            List<Pick> picks = outboundServiceRestemplateClient.getWorkOrderPicks(workOrder);

            if (picks.size() > 0) {
                String pickIds = picks.stream()
                        .map(Pick::getId).map(String::valueOf).collect(Collectors.joining(","));

                // loop through each production line's stage location and find the inventory
                // that is delivered
                workOrder.getProductionLineAssignments()
                        .stream()
                        .map(productionLineAssignment -> productionLineAssignment.getProductionLine().getInboundStageLocationId())
                        .forEach(productionInStagingLocationId -> {

                            deliveredInventory.addAll(
                                    inventoryServiceRestemplateClient.findDeliveredInventory(
                                            workOrder.getWarehouseId(),
                                            productionInStagingLocationId,
                                            pickIds)
                            );

                        });

            }
        } catch (IOException e) {
            // in case we can't get any picks, just return empty result to indicate
            // we don't have any delivered inventory

        }



        return deliveredInventory;
    }

    public List<Inventory> getReturnedInventory(Long workOrderId) {

        WorkOrder workOrder = findById(workOrderId);
        String workOrderLineIds =
                workOrder.getWorkOrderLines().stream()
                        .map(WorkOrderLine::getId).map(String::valueOf).collect(Collectors.joining(","));
        return inventoryServiceRestemplateClient.getReturnedInventory(
                workOrder.getWarehouseId(),workOrderLineIds
        );
    }

    public List<WorkOrderKPI> getKPIs(Long id) {
        return workOrderKPIService.findByWorkOrder(findById(id));
    }


    public List<WorkOrderKPITransaction> getKPITransactions(Long id) {

        return workOrderKPITransactionService.findByWorkOrder(findById(id));
    }


    public WorkOrder modifyWorkOrderLines(Long id,
                                          WorkOrder workOrder) {
        WorkOrder existingWorkOrder = findById(id);

        // we allow the user to
        // 1. remove work order lines
        // 2. add new work order lines
        // 3. change the quantity of the work order line

        // Let's save the existing work order line and the new work order line
        // into map so we can easily get the removed line and new lines
        Map<String, WorkOrderLine> existingWorkOrderLineMap = new HashMap<>();
        Map<String, WorkOrderLine> newWorkOrderLineMap = new HashMap<>();
        existingWorkOrder.getWorkOrderLines()
                .forEach(workOrderLine -> existingWorkOrderLineMap.put(workOrderLine.getNumber(), workOrderLine));
        workOrder.getWorkOrderLines()
                .forEach(workOrderLine -> newWorkOrderLineMap.put(workOrderLine.getNumber(), workOrderLine));

        existingWorkOrderLineMap.entrySet().forEach(entry -> {
            String workOrderLineNumber = entry.getKey();
            if (newWorkOrderLineMap.containsKey(workOrderLineNumber)) {
                // the new work order line map still contains the work order line
                // number, let's see if the quantity changed
                WorkOrderLine existingWorkOrderLine = entry.getValue();
                WorkOrderLine newWorkOrderLine = newWorkOrderLineMap.get(workOrderLineNumber);
                if (!existingWorkOrderLine.getExpectedQuantity()
                        .equals(newWorkOrderLine.getExpectedQuantity())) {
                    // we will need to update teh quantity
                    workOrderLineService.modifyWorkOrderLineExpectedQuantity(existingWorkOrderLine, newWorkOrderLine.getExpectedQuantity());
                }
                // Let's remove the work order line from the new map so anything left
                // after the loop are new line that we will need to add
                newWorkOrderLineMap.remove(workOrderLineNumber);
            }
            else {
                // the new work order doesn't contains this line,
                // we already remove it
                workOrderLineService.removeWorkOrderLine(workOrder, workOrderLineNumber);
            }

        });
        newWorkOrderLineMap.entrySet().forEach(entry -> {

            // Ok, this is a new work order line. we will make sure
            // the open quantity is setup as the expected quantity
            WorkOrderLine workOrderLine = entry.getValue();
            workOrderLine.setOpenQuantity(workOrderLine.getExpectedQuantity());
            //TO-DO: default the allocation strategy type to FIFO
            workOrderLine.setAllocationStrategyType(AllocationStrategyType.FIRST_IN_FIRST_OUT);
            workOrderLineService.addWorkOrderLine(workOrder, workOrderLine);
        });

        return findById(id);

    }

    public Inventory unpickInventory(Long id,
                                           Long inventoryId,
                                           Long unpickedQuantity,
                                           Boolean overrideConsumedQuantity,
                                           Long consumedQuantity,
                                           Long destinationLocationId,
                                           String destinationLocationName,
                                           boolean immediateMove) {

        WorkOrder workOrder = findById(id);
        Inventory inventory = inventoryServiceRestemplateClient.getInventoryById(inventoryId);
        if (Objects.isNull(unpickedQuantity)) {
            unpickedQuantity = inventory.getQuantity();
        }


        // Let's make sure we have enough quantity to be unpicked.
        // The consumed quantity + inventory's quantity should be
        // less than the total delivered quantity
        validateWorkOrderUnpick(workOrder, inventory, unpickedQuantity, overrideConsumedQuantity, consumedQuantity);

        if (overrideConsumedQuantity == true) {
            // Let's override the consumed quantity
            overrideConsumedQuantity(workOrder, inventory.getItem().getId(), consumedQuantity);
        }


        // Now we can call the inventory service to unpick the inventory
        inventory = inventoryServiceRestemplateClient.unpickFromWorkOrder(inventory,
                  destinationLocationId, destinationLocationName, immediateMove);


        // refresh the quantities after we unpicked the inventory
        refreshQuantityAfterUnpickInventory(workOrder, inventory.getItem().getId(),unpickedQuantity);

        return inventory;

    }

    private void refreshQuantityAfterUnpickInventory(WorkOrder workOrder, Long itemId, Long unpickedQuantity) {
        WorkOrderLine matchedWorkOrderLine = findMatchedWorkOrderLine(workOrder, itemId);

        workOrderLineService.refreshQuantityAfterUnpickInventory(matchedWorkOrderLine, unpickedQuantity);
    }

    private void overrideConsumedQuantity(WorkOrder workOrder, Long itemId, Long consumedQuantity) {
        // Let's assume we won't have duplicated item in the same work order
        WorkOrderLine matchedWorkOrderLine = findMatchedWorkOrderLine(workOrder, itemId);

        workOrderLineService.overrideConsumedQuantity(matchedWorkOrderLine, consumedQuantity);
    }

    private WorkOrderLine findMatchedWorkOrderLine(WorkOrder workOrder, Long itemId) {
        return  workOrder.getWorkOrderLines().stream().filter(workOrderLine -> workOrderLine.getItemId().equals(itemId))
                .findFirst()
                .orElseThrow(() ->
                        WorkOrderException.raiseException("Can't find item with id " + itemId + " from work order " + workOrder.getNumber()));
    }

    private void validateWorkOrderUnpick(WorkOrder workOrder, Inventory inventory,
                                         Long unpickedQuantity, Boolean overrideConsumedQuantity, Long consumedQuantity) {

        if (inventory.getQuantity() < unpickedQuantity) {
            throw WorkOrderException.raiseException("Can't unpick quantity " + unpickedQuantity +
                    " from inventory LPN " + inventory.getLpn() + " , the inventory has less quantity of " + inventory.getQuantity());
        }
        Long totalDeliveredQuantity = workOrder.getWorkOrderLines().stream().filter(
                workOrderLine -> workOrderLine.getItemId().equals(inventory.getItem().getId())
        ).map(WorkOrderLine::getDeliveredQuantity).mapToLong(Long::longValue).sum();

        Long totalConsumedQuantity = workOrder.getWorkOrderLines().stream().filter(
                workOrderLine -> workOrderLine.getItemId().equals(inventory.getItem().getId())
        ).map(WorkOrderLine::getConsumedQuantity).mapToLong(Long::longValue).sum();

        Long maxUnpickableQuantity = overrideConsumedQuantity ?
                totalDeliveredQuantity - consumedQuantity :
                totalDeliveredQuantity - totalConsumedQuantity;

        if (maxUnpickableQuantity < unpickedQuantity) {

            throw WorkOrderException.raiseException("Can't unpick quantity " + unpickedQuantity +
                    " from inventory LPN " + inventory.getLpn() + " , the work order  has the item " + inventory.getItem().getName() +
                    " with quantity " +  totalDeliveredQuantity + " delivered but already consumed " +
                    (totalDeliveredQuantity - maxUnpickableQuantity) +
                    ", hence only allow to unpick quantity " + maxUnpickableQuantity);
        }
    }



    public WorkOrder completeWorkOrder(WorkOrder workOrder) {
        workOrder.setStatus(WorkOrderStatus.COMPLETED);
        // Let's consume all the material.
        // If there's any leftover, it should go through the
        // return material process
        consumeAllMaterials(workOrder);

        WorkOrder newWorkOrder = saveOrUpdate(workOrder);
        sendWorkOrderConfirmationIntegration(newWorkOrder);

        if(Objects.nonNull(workOrder.getProductionPlanLine())) {

            productionPlanLineService.registerWorkOrderComplete(workOrder);
        }
        return newWorkOrder;
    }

    private void consumeAllMaterials(WorkOrder workOrder) {
        String workOrderLineIds = workOrder.getWorkOrderLines().stream()
                .map(WorkOrderLine::getId).map(String::valueOf).collect(Collectors.joining(","));
        logger.debug("Start to consume the materials for work order lines: {}", workOrderLineIds);
        inventoryServiceRestemplateClient.consumeAllMaterials(workOrder.getWarehouseId(), workOrderLineIds);
    }


    private void sendWorkOrderConfirmationIntegration(WorkOrder workOrder) {

        integrationService.process(new WorkOrderConfirmation(workOrder));
    }

    public String validateNewNumber(Long warehouseId, String number) {

        WorkOrder workOrder =
                findByNumber(warehouseId, number, false);

        return Objects.isNull(workOrder) ? "" : ValidatorResult.VALUE_ALREADY_EXISTS.name();
    }

    public ReportHistory generatePickReportByWorkOrder(Long workOrderId, String locale) throws IOException {

        return generatePickReportByWorkOrder(findById(workOrderId), locale);
    }

    public ReportHistory generatePickReportByWorkOrder(WorkOrder workOrder, String locale)
            throws IOException {

        Long warehouseId = workOrder.getWarehouseId();


        Report reportData = new Report();
        setupWorkOrderPickReportParameters(
                reportData, workOrder
        );
        setupWorkOrderPickReportData(
                reportData, workOrder
        );

        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        // logger.debug("####   Report   Data  ######");
        // logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.ORDER_PICK_SHEET, reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;

    }


    private void setupWorkOrderPickReportParameters(
            Report report, WorkOrder workOrder) throws IOException {

        // set the parameters to be the meta data of
        // the order

        report.addParameter("order_number", workOrder.getNumber());


        report.addParameter("customer_name", "N/A");

        Integer totalLineCount =
                workOrder.getWorkOrderLines().size();
        Integer totalItemCount =
                workOrder.getWorkOrderLines().size();
        Long totalQuantity =
                outboundServiceRestemplateClient.getWorkOrderPicks(workOrder)
                    .stream().mapToLong(Pick::getQuantity).sum();

        report.addParameter("totalLineCount", totalLineCount);
        report.addParameter("totalItemCount", totalItemCount);
        report.addParameter("totalQuantity", totalQuantity);
    }

    private void setupWorkOrderPickReportData(Report report, WorkOrder workOrder) throws IOException {

        // set data to be all picks
        List<Pick> picks = outboundServiceRestemplateClient.getWorkOrderPicks(workOrder);
        report.setData(picks);
    }
}
