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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.workorder.clients.*;
import org.apache.commons.lang3.tuple.Triple;
import com.garyzhangscm.cwms.workorder.exception.MissingInformationException;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderRepository;
import jakarta.persistence.criteria.*;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class WorkOrderService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderService.class);

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

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
    private WorkOrderProduceTransactionService workOrderProduceTransactionService;
    @Autowired
    private WorkOrderReverseProductionInventoryService workOrderReverseProductionInventoryService;

    @Autowired
    private KafkaSender kafkaSender;
    @Autowired
    private WorkOrderLineSparePartDetailService workOrderLineSparePartDetailService;
    @Autowired
    private WorkOrderLineSparePartService workOrderLineSparePartService;

    @Autowired
    private WorkOrderInstructionService workOrderInstructionService;
    @Autowired
    private WorkOrderKPIService workOrderKPIService;
    @Autowired
    private WorkOrderKPITransactionService workOrderKPITransactionService;
    @Autowired
    private ProductionLineAssignmentService productionLineAssignmentService;
    @Autowired
    private WorkOrderQCRuleConfigurationService workOrderQCRuleConfigurationService;

    @Autowired
    private MaterialRequirementsPlanningService materialRequirementsPlanningService;
    @Autowired
    private MasterProductionScheduleService masterProductionScheduleService;
    @Autowired
    private ProductionLineCapacityService productionLineCapacityService;

    @Autowired
    private UserService userService;

    @Autowired
    private OutboundServiceRestemplateClient outboundServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private FileService fileService;
    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private SiloRestemplateClient siloRestemplateClient;

    @Value("${fileupload.test-data.work-order:work-order}")
    String testDataFile;

    public WorkOrder findById(Long id ) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> {
                    logger.debug("work order not found by id: " + id);
                    return ResourceNotFoundException.raiseException("work order not found by id: " + id);
                });
    }



    public List<WorkOrder> findAll(Long warehouseId, String number,
                                   String itemName, String statusList, Long productionPlanId) {

        return findAll(warehouseId, number,
                itemName, statusList, productionPlanId,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "number")));

    }
    public List<WorkOrder> findAll(Long warehouseId, String number,
                                   String itemName, String statusList, Long productionPlanId,
                                   Pageable pageable) {

        Page<WorkOrder> workOrderPage =  workOrderRepository.findAll(
                (Root<WorkOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (!StringUtils.isBlank(number)) {
                        if (number.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("number"), number));
                        }
                        else if (number.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("name"), number.replaceAll("\\*", "%")));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("number"), number));
                        }

                    }
                    if (!StringUtils.isBlank(itemName)) {
                        List<Item> items = inventoryServiceRestemplateClient.findItemsByName(warehouseId, itemName);
                        logger.debug("Find {} items by name {}",
                                items.size(),
                                itemName);
                        if (items.isEmpty()) {

                            // The client passed in an invalid item name, let's return nothing
                            predicates.add(criteriaBuilder.equal(root.get("itemId"), -1L));
                        }
                        else {
                            CriteriaBuilder.In<Long> inItemIds = criteriaBuilder.in(root.get("itemId"));
                            for(Item item : items) {
                                inItemIds.value(item.getId());
                            }
                            predicates.add(criteriaBuilder.and(inItemIds));
                        }

                    }


                    if (StringUtils.isNotBlank(statusList)) {
                        CriteriaBuilder.In<WorkOrderStatus> inWorkOrderStatuses = criteriaBuilder.in(root.get("status"));
                        for(String workOrderStatus : statusList.split(",")) {
                            inWorkOrderStatuses.value(WorkOrderStatus.valueOf(workOrderStatus));
                        }
                        predicates.add(criteriaBuilder.and(inWorkOrderStatuses));
                    }


                    if (Objects.nonNull(productionPlanId)) {
                        Join<WorkOrder, ProductionPlanLine> joinProductionPlanLine = root.join("productionPlanLine", JoinType.INNER);
                        Join<ProductionPlanLine, ProductionPlan> joinProductionPlan = joinProductionPlanLine.join("productionPlan", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinProductionPlan.get("id"), productionPlanId));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                pageable
        );

        return workOrderPage.getContent();

    }

    public Page <WorkOrder> findAllByPagination(Long warehouseId, String number,
                                   String itemName, String statusList, Long productionPlanId,
                                   Pageable pageable) {

        return workOrderRepository.findAll(
                (Root<WorkOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (!StringUtils.isBlank(number)) {
                        if (number.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("number"), number));
                        }
                        else if (number.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("name"), number.replaceAll("\\*", "%")));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("number"), number));
                        }

                    }
                    if (!StringUtils.isBlank(itemName)) {
                        List<Item> items = inventoryServiceRestemplateClient.findItemsByName(warehouseId, itemName);
                        logger.debug("Find {} items by name {}",
                                items.size(),
                                itemName);
                        if (items.isEmpty()) {

                            // The client passed in an invalid item name, let's return nothing
                            predicates.add(criteriaBuilder.equal(root.get("itemId"), -1L));
                        }
                        else {
                            CriteriaBuilder.In<Long> inItemIds = criteriaBuilder.in(root.get("itemId"));
                            for(Item item : items) {
                                inItemIds.value(item.getId());
                            }
                            predicates.add(criteriaBuilder.and(inItemIds));
                        }

                    }


                    if (StringUtils.isNotBlank(statusList)) {
                        CriteriaBuilder.In<WorkOrderStatus> inWorkOrderStatuses = criteriaBuilder.in(root.get("status"));
                        for(String workOrderStatus : statusList.split(",")) {
                            inWorkOrderStatuses.value(WorkOrderStatus.valueOf(workOrderStatus));
                        }
                        predicates.add(criteriaBuilder.and(inWorkOrderStatuses));
                    }


                    if (Objects.nonNull(productionPlanId)) {
                        Join<WorkOrder, ProductionPlanLine> joinProductionPlanLine = root.join("productionPlanLine", JoinType.INNER);
                        Join<ProductionPlanLine, ProductionPlan> joinProductionPlan = joinProductionPlanLine.join("productionPlan", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinProductionPlan.get("id"), productionPlanId));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                pageable
        );

    }


    public List<WorkOrder> findAll(Long warehouseId, String number,
                                   String itemName, String statusList, Long productionPlanId,
                                   boolean genericQuery) {
        return findAll(warehouseId, number, itemName,  statusList, productionPlanId);
    }


    public WorkOrder findByNumber(Long warehouseId, String number) {
        return workOrderRepository.findByWarehouseIdAndNumber(warehouseId, number);
    }


/**
    public void loadAttribute(List<WorkOrder> workOrders) {
        loadAttribute(workOrders, true, true);
    }
    public void loadAttribute(List<WorkOrder> workOrders, boolean loadPicks, boolean loadShortAllocations) {
        for (WorkOrder workOrder : workOrders) {
            loadAttribute(workOrder, loadPicks, loadShortAllocations, true);
        }
    }
    public void loadAttribute(WorkOrder workOrder) {
        loadAttribute(workOrder, true, true, true);
    }
    public void loadAttribute(WorkOrder workOrder, boolean loadPicks, boolean loadShortAllocations , boolean loadWorkOrderLineDetails) {

        if (workOrder.getItemId() != null &&
                (workOrder.getItem() == null || Objects.isNull(workOrder.getItem().getId()))) {
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
                    if (loadWorkOrderLineDetails) {

                        workOrderLineService.loadAttribute(workOrderLine, loadPicks, loadShortAllocations);
                    }
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

 **/


    public WorkOrder save(WorkOrder workOrder ) {

        // send alert for new receipt or receipt change
        boolean newWorkOrderFlag = false;
        if (Objects.isNull(workOrder.getId())) {
            newWorkOrderFlag = true;
        }
        // in case the work order is created out of context, we will need to
        // setup the created by in the context and then pass the username
        // in the down stream so that when we send alert, the alert will
        // contain the right username
        // example: when we create work order via uploading CSV file,
        // 1. we will save the username in the main thread
        // 2. in a separate thread, we will create the work order according to the
        //    csv file and setup the receipt's create by with the username from
        //    the main thread
        // 3. we will fetch the right username here(who upload the file) and use
        //    it to send alert
        String username = workOrder.getCreatedBy();

        WorkOrder newWorkOrder = workOrderRepository.save(workOrder);
        sendAlertForWorkOrder(newWorkOrder, newWorkOrderFlag,
                Strings.isBlank(username) ? newWorkOrder.getCreatedBy() : username
        );

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
        if (Objects.isNull(workOrder.getId())) {
            // when we add a new work order, we will setup the QC related information
            setupQCQuantity(workOrder);
            workOrder.setQcQuantityRequested(0l);
            workOrder.setQcQuantityCompleted(0l);
        }
        return save(workOrder);
    }

    private void setupQCQuantity(WorkOrder workOrder) {
        logger.debug("Start to setup qc quantity for workOrder {}",
                workOrder.getNumber() );
        // default to the qc quantity to 0
        workOrder.setQcQuantity(0l);
        workOrder.setQcPercentage(0d);

        Warehouse warehouse = workOrder.getWarehouse();
        if (Objects.isNull(warehouse)) {
            warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(
                    workOrder.getWarehouseId()
            );
        }
        if (Objects.isNull(warehouse)) {
            // we should not arrive here！
            logger.debug("Can't get the QC configuration as we can't get the warehouse" +
                    " information from the work order");
            logger.debug("=======   Work Order ======= \n {}",
                    workOrder);
            return;
        }
        Item item =
                Objects.nonNull(workOrder.getItem()) ? workOrder.getItem() :
                        inventoryServiceRestemplateClient.getItemById(workOrder.getItemId());

        WorkOrderQCRuleConfiguration workOrderQCRuleConfiguration =
                workOrderQCRuleConfigurationService.getBestMatchedWorkOrderQCRuleConfiguration(
                        workOrder.getBtoOutboundOrderId(),
                        workOrder.getBtoCustomerId(),
                        Objects.isNull(item.getItemFamily()) ? null : item.getItemFamily().getId(),
                        workOrder.getItemId(),
                        workOrder.getWarehouseId(),
                        warehouse.getCompany().getId()
                );
        if (Objects.isNull(workOrderQCRuleConfiguration)) {
            logger.debug("No work order qc configuration is defined for the work order {}",
                    workOrder.getNumber());
            logger.debug("bto order: {}, bto customer id {}, item {} / {}, warehouse {} / {}, company {} / {}",
                    workOrder.getBtoOutboundOrderId(),
                    workOrder.getBtoCustomerId(),
                    workOrder.getItemId(),
                    Objects.isNull(workOrder.getItem()) ? "" : workOrder.getItem().getName(),
                    warehouse.getId(),
                    warehouse.getName(),
                    Objects.isNull(warehouse.getCompany()) ? "" : warehouse.getCompany().getId(),
                    Objects.isNull(warehouse.getCompany()) ? "" : warehouse.getCompany().getName());
            return;
        }
        // setup the qc quantity and percentage based on the configuration
        if (Objects.nonNull(workOrderQCRuleConfiguration.getQcQuantityPerWorkOrder())) {

            workOrder.setQcQuantity(workOrderQCRuleConfiguration.getQcQuantityPerWorkOrder());
        }
        if (Objects.nonNull(workOrderQCRuleConfiguration.getQcPercentagePerWorkOrder())) {

            workOrder.setQcPercentage(workOrderQCRuleConfiguration.getQcPercentagePerWorkOrder());
        }
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
        workOrder.setBillOfMaterial(billOfMaterial);
        workOrder.setConsumeByBom(billOfMaterial);


        WorkOrder savedWorkOrder = save(workOrder);


        // if the production line is passed in, assign the work order onto
        // the production line
        if (Objects.nonNull(productionLineId)) {
            productionLineAssignmentService.assignWorkOrderToProductionLines(
                    savedWorkOrder,
                    productionLineService.findById(productionLineId),
                    workOrder.getExpectedQuantity());
        }

        double workOrderCount = (expectedQuantity * 1.0 / billOfMaterial.getExpectedQuantity());
        // Start to create work order line
        billOfMaterial.getBillOfMaterialLines()
                .forEach(billOfMaterialLine ->
                        workOrderLineService.createWorkOrderLineFromBOMLine(
                                savedWorkOrder, expectedQuantity,
                                billOfMaterial.getExpectedQuantity(),
                                billOfMaterialLine));

        // Start to create work order instruction
        billOfMaterial.getWorkOrderInstructionTemplates()
                .forEach(workOrderInstructionTemplate ->
                        workOrderInstructionService.createWorkOrderInstructionFromBOMLine(savedWorkOrder,workOrderInstructionTemplate));

        billOfMaterial.getBillOfMaterialByProducts()
                .forEach(billOfMaterialByProduct ->
                        workOrderByProductService.createWorkOrderByProductFromBOMByProduct(
                                savedWorkOrder, expectedQuantity,
                                billOfMaterial.getExpectedQuantity(),
                                billOfMaterialByProduct));
        return findById(savedWorkOrder.getId());
    }

    public WorkOrder allocateWorkOrder(Long workOrderId, String productionLineIds, String quantities) {

        if (StringUtils.isBlank(productionLineIds) ||
               StringUtils.isBlank(quantities)) {
            // if the user doesn't specify the production line or quantities, let's
            // allocate the whole work order
            logger.debug("# Will allocate the whole work order");
            return allocateWorkOrder(workOrderId);
        }

        // when we use passes in the production lines and quantites, we will only
        // allocate the work order based off the production line and quantity
        String[] productionLineIdArray = productionLineIds.split(",");
        String[] quantityArray = quantities.split(",");
        if (productionLineIdArray.length != quantityArray.length) {
            throw WorkOrderException.raiseException("Can't allocate the work order as the length of production line doesn't match with the length of quantity");
        }
        for(int i = 0; i < productionLineIdArray.length; i++) {
            String productionLineId = productionLineIdArray[i];
            String quantity = quantityArray[i];
            if (StringUtils.isBlank(productionLineId) || StringUtils.isBlank(quantity)) {
                // data error, ignore
                continue;
            }
            allocateWorkOrder(workOrderId, Long.parseLong(productionLineId), Long.parseLong(quantity));
        }

        return findById(workOrderId);

    }
    public WorkOrder allocateWorkOrder(Long workOrderId, Long productionLineId, Long quantity) {

        WorkOrder workOrder = findById(workOrderId);
        logger.debug("Start to allocate work order: {} to production line id {}, with quantity {}",
                workOrder.getNumber(), productionLineId, quantity);
        if (Objects.nonNull(quantity) && quantity == 0L) {
            // quantity is passed in as 0
            // will skip this production line
            return workOrder;
        }
        AllocationResult allocationResult
                = outboundServiceRestemplateClient.allocateWorkOrder(workOrder, productionLineId, quantity);


        processAllocateResult(workOrder, allocationResult);
        processProductionLineAssignment(workOrder, productionLineId, quantity);
        // return the latest work order information
        return findById(workOrderId);


    }


    public WorkOrder allocateWorkOrder(Long workOrderId, List<ProductionLineAllocationRequest> productionLineAllocationRequests) {

        validateWOrkOrderForAllocation(workOrderId);

        // if the user didn't specify the production line
        // then we will allocate the whole work order
        if (productionLineAllocationRequests.size() == 0) {
            allocateWorkOrder(workOrderId);
        }
        productionLineAllocationRequests.forEach(
                productionLineAllocationRequest -> {
                    if (Boolean.FALSE.equals(productionLineAllocationRequest.getAllocateByLine())) {
                        // allocate by work order
                        allocateWorkOrder(
                                workOrderId,
                                productionLineAllocationRequest.getProductionLineId(),
                                productionLineAllocationRequest.getAllocatingQuantity());
                    }
                    else {
                        // allocate by work order line
                        productionLineAllocationRequest.getLines().forEach(
                                productionLineAllocationRequestLine -> {

                                    allocateWorkOrderLine(
                                            productionLineAllocationRequestLine.getWorkOrderLineId(),
                                            productionLineAllocationRequest.getProductionLineId(),
                                            productionLineAllocationRequestLine.getAllocatingQuantity());
                                }
                        );

                    }
                }
        );

        return findById(workOrderId);


    }

    /**
     * Make sure we are good to allocate the work order
     * @param workOrderId
     */
    private void validateWOrkOrderForAllocation(Long workOrderId) {
        WorkOrder workOrder = findById(workOrderId);
        if (Boolean.TRUE.equals(workOrder.getConsumeByBomOnly()) &&
                Objects.isNull(workOrder.getConsumeByBom())) {
            // the work order is setup to be consumed by BOM but
            // there's no BOM setup for it
            logger.error("The work order {} is setup to be consumed by BOM but there's no " +
                    "BOM setup yet", workOrder.getNumber());
            throw WorkOrderException.raiseException(
                    "The work order " + workOrder.getNumber() + " is setup to be consumed by BOM" +
                            "but there's no BOM setup yet"
            );
        }
    }

    private void allocateWorkOrderLine(Long workOrderLineId, Long productionLineId, Long allocatingQuantity) {

        WorkOrderLine workOrderLine = workOrderLineService.findById(workOrderLineId);
        logger.debug("Start to allocate work order item: {} to production line id {}, with quantity {}",
                workOrderLine.getItem().getName(), productionLineId, allocatingQuantity);
        if (Objects.nonNull(allocatingQuantity) && allocatingQuantity == 0L) {
            // quantity is passed in as 0
            // will skip this production line / work order line
            return ;
        }
        logger.debug("Start to allocate work order line");
        AllocationResult allocationResult
                = outboundServiceRestemplateClient.allocateWorkOrderLine(workOrderLine, productionLineId, allocatingQuantity);


        logger.debug("will process the allocation result ");
        processAllocateResult(workOrderLine.getWorkOrder(), workOrderLine, allocationResult);
        logger.debug("will process the production line assignment ");
        processProductionLineAssignment(workOrderLine.getWorkOrder(), workOrderLine, productionLineId, allocatingQuantity);


    }


    private void processProductionLineAssignment(WorkOrder workOrder, WorkOrderLine workOrderLine, Long productionLineId, Long quantity) {

        workOrder.getProductionLineAssignments().stream().filter(
                productionLineAssignment -> productionLineAssignment.getProductionLine().getId().equals(productionLineId)
        ).forEach(
                productionLineAssignment -> {
                    logger.debug("Will process the production line assignment {} for work order line {} / {}",
                            productionLineAssignment,
                            workOrder.getNumber(), workOrderLine.getItem().getName());
                    productionLineAssignment.getLines().stream().filter(
                            productionLineAssignmentLine -> productionLineAssignmentLine.getWorkOrderLine().getId().equals(workOrderLine.getId())
                    ).forEach(
                            productionLineAssignmentLine -> {
                                Long remainOpenQuantity = productionLineAssignmentLine.getOpenQuantity() >= quantity ?
                                        productionLineAssignmentLine.getOpenQuantity() - quantity : 0L;
                                logger.debug("> we found a production line assignment for this work order line, will update the open quantity from {} to {}",
                                        productionLineAssignmentLine.getOpenQuantity(), remainOpenQuantity);
                                productionLineAssignmentLine.setOpenQuantity(remainOpenQuantity);
                                productionLineAssignmentService.saveLine(productionLineAssignmentLine);
                            }
                    );
                }
        );
    }
    private void processProductionLineAssignment(WorkOrder workOrder, Long productionLineId, Long quantity) {
        if (Objects.isNull(productionLineId) && Objects.isNull(quantity)) {
            // we assume the user allocate the whole work order, then let's set
            // the open quantity of each production line assignment to 0
            for(ProductionLineAssignment productionLineAssignment : workOrder.getProductionLineAssignments()) {
                productionLineAssignment.setOpenQuantity(0L);
                productionLineAssignmentService.saveOrUpdate(productionLineAssignment);
            }
        }
        else if (Objects.nonNull(productionLineId) && Objects.nonNull(quantity)) {
            // both production line and quantity are passed in, let's
            // deduct the open quantity from this production line assignment

            for(ProductionLineAssignment productionLineAssignment : workOrder.getProductionLineAssignments()) {
                if (productionLineId.equals(productionLineAssignment.getProductionLine().getId())){
                    Long remainOpenQuantity = productionLineAssignment.getOpenQuantity() >= quantity ?
                            productionLineAssignment.getOpenQuantity() - quantity : 0L;
                    productionLineAssignment.setOpenQuantity(remainOpenQuantity);
                    productionLineAssignmentService.saveOrUpdate(productionLineAssignment);
                    // since we specify the production line id and we assume one work order will only have one
                    // record on each production, so we will break the loop here to increase the performance
                    break;
                }
            }
        }
        else {
            // we should never be here
            throw WorkOrderException.raiseException(
                    "Error, can't update the production line assignment. work order:" + workOrder.getNumber() +
                            ",  production ID: " + productionLineId +", Quantity: " + quantity);
        }
    }

    /**
     * Get the inprocess quantity out of the allocation result
     * it will return a map
     * - Key: work order line id
     * - value: pick quantity + short allocation quantity
     * @param allocationResult
     * @return
     */
    private Map<Long, Long> getInprocessQuantities(AllocationResult allocationResult) {
        Map<Long, Long> inprocessQuantities = new HashMap<>();

        allocationResult.getPicks().stream().filter(
                pick -> !isPickSparePart(pick.getWorkOrderLineId(), pick)
        ).forEach(pick -> {
            Long workOrderLineId = pick.getWorkOrderLineId();
            Long inprocessQuantity = inprocessQuantities.getOrDefault(workOrderLineId, 0L);
            inprocessQuantities.put(workOrderLineId, (inprocessQuantity + pick.getQuantity()));
        });

        allocationResult.getShortAllocations().forEach(shortAllocation -> {
            Long workOrderLineId = shortAllocation.getWorkOrderLineId();
            Long inprocessQuantity = inprocessQuantities.getOrDefault(workOrderLineId, 0L);
            inprocessQuantities.put(workOrderLineId, (inprocessQuantity + shortAllocation.getQuantity()));
        });
        return inprocessQuantities;
    }


    /**
     * See if the pick is to pick spare part
     * @param workOrderLineId
     * @param pick
     * @return
     */
    private boolean isPickSparePart(Long workOrderLineId, Pick pick) {
        WorkOrderLine workOrderLine = workOrderLineService.findById(workOrderLineId);
        if (pick.getItemId().equals(workOrderLine.getItemId())) {
            return false;
        }
        else {
            Stream<WorkOrderLineSparePartDetail> workOrderLineSparePartDetailStream =
                    workOrderLine.getWorkOrderLineSpareParts().stream()
                    .map(workOrderLineSparePart -> workOrderLineSparePart.getWorkOrderLineSparePartDetails())
                    .flatMap(List::stream);
            if (workOrderLineSparePartDetailStream.anyMatch(
                    workOrderLineSparePartDetail -> workOrderLineSparePartDetail.getItemId().equals(pick.getItemId()))) {
                return true;
            }
            else {
                throw WorkOrderException.raiseException("the pick " + pick.getNumber() +
                        "doesn't match with the work order line " + workOrderLineId);
            }
        }
    }

    private void processAllocateResult(WorkOrder workOrder, WorkOrderLine workOrderLine, AllocationResult allocationResult) {

        // A map to store the quantities
        // Key: work order line id
        // value: pick quantity + short allocation quantity
        Map<Long, Long> inprocessQuantities = getInprocessQuantities(allocationResult);
        logger.debug("Get in process quantity out of allocation result for the work order line {} :\n {}",
                workOrderLine.getId(), inprocessQuantities);

        inprocessQuantities.entrySet().stream()
                .filter(entry -> entry.getKey().equals(workOrderLine.getId()))
                .forEach(entry ->{
                    WorkOrderLine existingWorkOrderLine = workOrderLineService.findById(entry.getKey());
                    // Move the 'inprocess quantity' we just calculated from 'Open quantity'
                    // to 'inprocess quantity'
                    Long inprocessQuantity = entry.getValue();
                    logger.debug("work order line {}'s inprocess quantity will be updated by {}",
                            existingWorkOrderLine.getNumber(), inprocessQuantity);
                    // we may allocate more than necessary(when round up for the item is allowed)
                    if (existingWorkOrderLine.getOpenQuantity() < inprocessQuantity) {
                        existingWorkOrderLine.setOpenQuantity(0L);
                    }
                    else {
                        existingWorkOrderLine.setOpenQuantity(existingWorkOrderLine.getOpenQuantity() - inprocessQuantity);

                    }
                    existingWorkOrderLine.setInprocessQuantity(existingWorkOrderLine.getInprocessQuantity() + inprocessQuantity);
                    workOrderLineService.save(existingWorkOrderLine);
        });
        // If the current work order's status is 'Pending', change it to 'INPROCESS'
        if (workOrder.getStatus().equals(WorkOrderStatus.PENDING)) {
            workOrder.setStatus(WorkOrderStatus.INPROCESS);
            saveOrUpdate(workOrder);
        }

    }
    private void processAllocationResultForSpareParts(AllocationResult allocationResult) {

        allocationResult.getPicks().stream().filter(
                pick -> isPickSparePart(pick.getWorkOrderLineId(), pick)
        ).forEach(
                pick -> {
                    // for spare part, get the information first
                    WorkOrderLine workOrderLine = workOrderLineService.findById(
                            pick.getWorkOrderLineId()
                    );
                    WorkOrderLineSparePartDetail matchedWorkOrderLineSparePartDetail =
                            workOrderLine.getWorkOrderLineSpareParts().stream().map(
                                    workOrderLineSparePart -> workOrderLineSparePart.getWorkOrderLineSparePartDetails()
                            ).flatMap(List::stream).filter(
                                    workOrderLineSparePartDetail -> workOrderLineSparePartDetail.getItemId().equals(pick.getItemId())
                            ).findFirst().orElse(null);

                    if (Objects.nonNull(matchedWorkOrderLineSparePartDetail)) {
                        // we should always be able to find the matched work order line spare part detail with the pick
                        Long newOpenQuantity = Math.max(0, matchedWorkOrderLineSparePartDetail.getOpenQuantity() - pick.getQuantity());
                        Long newInprocessQuantity = matchedWorkOrderLineSparePartDetail.getInprocessQuantity() + pick.getQuantity();
                        matchedWorkOrderLineSparePartDetail.setInprocessQuantity(newInprocessQuantity);
                        matchedWorkOrderLineSparePartDetail.setOpenQuantity(newOpenQuantity);
                        workOrderLineSparePartDetailService.saveOrUpdate(matchedWorkOrderLineSparePartDetail);

                        // everytime we changed the in process quantity of the spare part details,
                        // we will need to refresh the spare part head's quantity as well
                        workOrderLineSparePartService.refreshInprocessQuantity(matchedWorkOrderLineSparePartDetail.getWorkOrderLineSparePart());

                    }
                }
        );
    }
    private void processAllocateResult(WorkOrder workOrder, AllocationResult allocationResult) {


        // A map to store the quantities
        // Key: work order line id
        // value: pick quantity + short allocation quantity
        Map<Long, Long> inprocessQuantities = getInprocessQuantities(allocationResult);


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
    }
    public WorkOrder allocateWorkOrder(Long workOrderId) {
        WorkOrder workOrder = findById(workOrderId);
        logger.debug("Start to allocate work order: \n {}", workOrder);
        AllocationResult allocationResult
                = outboundServiceRestemplateClient.allocateWorkOrder(workOrder, null, null);

        logger.debug("Get result for work order {} \n, {} picks, {} short allocations, {}",
                    workOrder.getNumber(), allocationResult.getPicks().size(),
                    allocationResult.getShortAllocations().size(), allocationResult);
        // After we get the allocation result,
        // let's update the quantity in each work order line
        processAllocateResult(workOrder, allocationResult);


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

    public WorkOrder produce(WorkOrder workOrder, Long producedQuantity, boolean loadDetails) {
        logger.debug("Will change the work order's produced quantity from {}, to {}",
                workOrder.getProducedQuantity(),
                workOrder.getProducedQuantity() + producedQuantity);
        workOrder.setProducedQuantity(workOrder.getProducedQuantity() + producedQuantity);
        return saveOrUpdate(workOrder, loadDetails);
    }


    public List<Inventory> getProducedInventory(Long workOrderId) {

        WorkOrder workOrder = findById(workOrderId);
        return inventoryServiceRestemplateClient.findProducedInventory(
                workOrder.getWarehouseId(),
                workOrderId
        );

    }

    public List<Inventory> getProducedByProduct(Long workOrderId) {
        return getProducedByProduct(workOrderId, "");
    }
    public List<Inventory> getProducedByProduct(Long workOrderId, String lpn) {

        WorkOrder workOrder = findById(workOrderId);
        // if we don't have by product setup, then return empty
        if (Objects.isNull(workOrder.getWorkOrderByProducts()) ||
                workOrder.getWorkOrderByProducts().size() == 0) {
            return new ArrayList<>();
        }
        String workOrderByProductIds =
                workOrder.getWorkOrderByProducts().stream()
                        .map(WorkOrderByProduct::getId).map(String::valueOf).collect(Collectors.joining(","));
        return inventoryServiceRestemplateClient.getProducedByProduct(
                workOrder.getWarehouseId(),
                workOrderByProductIds, lpn
        );

    }

    public List<Inventory> getDeliveredInventory(Long workOrderId) {
        // return the delivered inventory for the whole work order, regardless of the
        // assigned production line
        ProductionLine productionLine = null;
        return getDeliveredInventory(workOrderId, productionLine);
    }
    public List<Inventory> getDeliveredInventory(Long workOrderId, Long productionLineId) {
        // return the delivered inventory for the whole work order, regardless of the
        // assigned production line
        logger.debug("start to get delivered invenotry workOrderId: {} / productionLineId: {}",
                workOrderId, productionLineId);
        ProductionLine productionLine = null;
        if (Objects.nonNull(productionLineId)) {
            logger.debug("will get production line by id: {}", productionLineId);
            productionLine = productionLineService.findById(productionLineId);
        }
        return getDeliveredInventory(workOrderId, productionLine);
    }
    public List<Inventory> getDeliveredInventory(Long workOrderId, ProductionLine productionLine) {
        logger.debug("Will get delivered inventory for work order by id {}, production line {}",
                workOrderId,
                Objects.isNull(productionLine)? "N/A" : productionLine.getName());
        WorkOrder workOrder = findById(workOrderId);
        logger.debug("Will get delivered inventory for work order {}, production line {}",
                workOrder.getNumber(),
                Objects.isNull(productionLine)? "N/A" : productionLine.getName());

        // Get all the picked inventory that already arrived at the
        // right location
        if (workOrder.getProductionLineAssignments().size() == 0) {
            logger.debug("The work order has no production line assignment, so we can't get the delivered inventory");
            // The work order doesn't have production line assigned yet, probably
            // it is a new work order;
            return new ArrayList<>();
        }

        List<Inventory> deliveredInventory = new ArrayList<>();

        // Get all the picks that belongs to this work order
        List<Pick> picks = outboundServiceRestemplateClient.getWorkOrderPicks(workOrder);

        logger.debug("We got {} picks for this work order", picks.size());
        if (picks.size() > 0) {
                String pickIds = picks.stream()
                        .map(Pick::getId).map(String::valueOf).collect(Collectors.joining(","));

                // loop through each production line's stage location and find the inventory
                // that is delivered
                Stream<Long> productionInStagingLocationIds =
                            workOrder.getProductionLineAssignments()
                            .stream()
                            .map(productionLineAssignment -> productionLineAssignment.getProductionLine().getInboundStageLocationId());
                // if the location is passed in, filter the result by the location only

                if (Objects.nonNull(productionLine)) {
                    productionInStagingLocationIds = productionInStagingLocationIds.filter(
                            productionInStagingLocationId -> productionInStagingLocationId.equals(productionLine.getInboundStageLocationId())
                    );
                }

                productionInStagingLocationIds.forEach(productionInStagingLocationId -> {

                    logger.debug("Start to find delivered invenotry with production in staging location id {]",
                            productionInStagingLocationId);
                            deliveredInventory.addAll(
                                    inventoryServiceRestemplateClient.findDeliveredInventory(
                                            workOrder.getWarehouseId(),
                                            productionInStagingLocationId,
                                            pickIds)
                            );


                        });

            }



        logger.debug("return {} delivered invenotry", deliveredInventory.size());
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
                findByNumber(warehouseId, number);

        return Objects.isNull(workOrder) ? "" : ValidatorResult.VALUE_ALREADY_EXISTS.name();
    }

    public ReportHistory generatePickReportByWorkOrder(Long workOrderId, String locale,
                                                       String printerName) throws IOException {

        return generatePickReportByWorkOrder(findById(workOrderId), locale,
                printerName);
    }

    public ReportHistory generatePickReportByWorkOrder(WorkOrder workOrder, String locale,
                                                       String printerName)
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
                        warehouseId, ReportType.WORK_ORDER_PICK_SHEET, reportData, locale,
                        printerName
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;

    }


    private void setupWorkOrderPickReportParameters(
            Report report, WorkOrder workOrder) throws IOException {

        // set the parameters to be the meta data of
        // the order

        report.addParameter("work_order_number", workOrder.getNumber());

    }

    private void setupWorkOrderPickReportData(Report report, WorkOrder workOrder) throws IOException {

        // set data to be all picks
        List<Pick> picks = outboundServiceRestemplateClient.getWorkOrderPicks(workOrder);
        report.setData(picks);
    }

    public List<WorkOrder> findInprocessWorkOrder(Long warehouseId) {
        return workOrderRepository.findInprocessWorkOrder(warehouseId);
    }
    public List<WorkOrder> getWorkOrdersWithOpenPick(Long warehouseId) {

        List<WorkOrder> workOrders = findInprocessWorkOrder(warehouseId);
        // only return the work order that has any line with  open quantity
        workOrders = workOrders.stream().filter(workOrder ->
                workOrder.getWorkOrderLines().stream().anyMatch(
                    workOrderLine -> workOrderLine.getInprocessQuantity() > 0)

        ).collect(Collectors.toList());

        // for each work order, see if we still have open picks
        workOrders = workOrders.stream().filter(workOrder ->
                outboundServiceRestemplateClient
                        .getWorkOrderPicks(workOrder)
                        .stream().anyMatch(pick -> pick.getPickedQuantity() < pick.getQuantity())

        ).collect(Collectors.toList());


        return workOrders;
    }

    public WorkOrder changeConsumeMethod(Long id, String materialConsumeTiming, Boolean consumeByBomFlag, Long consumeByBOMId) {

        // make sure the values are correct
        WorkOrder workOrder = findById(id);
        WorkOrderMaterialConsumeTiming workOrderMaterialConsumeTiming =
                WorkOrderMaterialConsumeTiming.valueOf(materialConsumeTiming);
        if (!workOrderMaterialConsumeTiming.equals(WorkOrderMaterialConsumeTiming.BY_TRANSACTION)) {
            // we will only need to setup the BOM when we consume by transaction
            logger.debug("The work order's consume timing will change to {}, no need to setup the BOM",
                    materialConsumeTiming);
            workOrder.setMaterialConsumeTiming(workOrderMaterialConsumeTiming);
            workOrder.setConsumeByBomOnly(false);
            workOrder.setConsumeByBom(null);
            return saveOrUpdate(workOrder);
        }
        else if (Boolean.TRUE.equals(consumeByBomFlag)) {
            // ok the user is setup the work order to consumed by BOM
            BillOfMaterial billOfMaterial = billOfMaterialService.findById(consumeByBOMId);
            if (Objects.isNull(billOfMaterial)) {
                throw WorkOrderException.raiseException("Can't change the consume method as we can't find BOM by id " + consumeByBOMId);
            }
            workOrder.setMaterialConsumeTiming(workOrderMaterialConsumeTiming);
            workOrder.setConsumeByBomOnly(true);
            workOrder.setConsumeByBom(billOfMaterial);
            return saveOrUpdate(workOrder);
        }
        else {
            workOrder.setMaterialConsumeTiming(workOrderMaterialConsumeTiming);
            workOrder.setConsumeByBomOnly(false);
            workOrder.setConsumeByBom(null);
            return saveOrUpdate(workOrder);
        }
    }

    /**
     * Reverse production, remove the inventory and return the quantity to the lines
     * @param id
     * @param lpn
     * @return
     */
    public WorkOrder reverseProduction(Long id, String lpn)   {
        WorkOrder workOrder = findById(id);

        List<Inventory> inventories = inventoryServiceRestemplateClient.findProducedInventoryByLPN(
                workOrder.getWarehouseId(), workOrder.getId(),
                lpn
        );
        logger.debug("We found {} inventory to be reversed by LPN {}",
                inventories.size(), lpn);

        // if we can find any inventory that matches with the work order and id,
        // let's remove the inventory and return the quantity

        // key: work order produce transaction id
        // value: work order produce transaction
        // we will save the work order
        Map<Long, WorkOrderProduceTransaction> inventoryProducedTransactionSet = new HashMap<>();
        Long totalQuantity = 0l;
        for (Inventory inventory : inventories) {
            totalQuantity += inventory.getQuantity();
            logger.debug("will remove inventory with id {}, lpn {}, item {}, quantity {}",
                    inventory.getId(),
                    inventory.getLpn(),
                    inventory.getItem().getName(),
            inventory.getQuantity());
            inventoryServiceRestemplateClient.reverseProduction(inventory.getId());


            // see if we can find transaction that created the inventory.
            // if so, we may be able to return the quantity back to the
            // work order line and adjust the quantity of the transaction
            logger.debug("Inventory's work order: {}, transaction id {}",
                    inventory.getWorkOrderId(), inventory.getCreateInventoryTransactionId());
            if (Objects.nonNull(inventory.getWorkOrderId()) &&
                    Objects.nonNull(inventory.getCreateInventoryTransactionId())) {
                try {

                    WorkOrderProduceTransaction workOrderProduceTransaction =
                            workOrderProduceTransactionService.findById(inventory.getCreateInventoryTransactionId());
                    logger.debug("Found right work order produce transaction by id {} ? {}",
                            inventory.getCreateInventoryTransactionId(),
                            Objects.nonNull(workOrderProduceTransaction));
                    if (Objects.nonNull(workOrderProduceTransaction)) {
                        logger.debug("Add a new reverse production transaction");
                        workOrderReverseProductionInventoryService.save(new WorkOrderReverseProductionInventory(
                                workOrderProduceTransaction, inventory.getLpn(), inventory.getQuantity()
                        ));
                        // we will put the quantity back
                        logger.debug("will return the consumed quantity back, only if we can ");
                        processReverseProductionQuantity(workOrderProduceTransaction, inventory.getQuantity());
                    }
                }
                catch (Exception exception) {
                    exception.printStackTrace();
                    logger.debug("Ignore workOrderProduceTransaction error when we reverse production");
                }
            }
        }

        // return the quantity back to work order
        logger.debug("Deduct the produced quantity of the work order by {}",
                totalQuantity);
        workOrder.setProducedQuantity(workOrder.getProducedQuantity() - totalQuantity);
        save(workOrder);


        // we will return the quantity back to work order line only if
        // the inventory was produced by a transaction that marked as 'consume by bom'
        // so that we know how much material we consumed in order to produce the inventory



        return workOrder;
    }

    /**
     * Reverse by product, remove the inventory and return the quantity to the by product
     * @param id
     * @param lpn
     * @return
     */
    public WorkOrder reverseByProduct(Long id, String lpn) {
        WorkOrder workOrder = findById(id);

        List<Inventory> inventories = getProducedByProduct(workOrder.getId(), lpn);

        logger.debug("We found {} inventory to be reversed by LPN {}",
                inventories.size(), lpn);

        // if we can find any inventory that matches with the work order and id,
        // let's remove the inventory and return the quantity

        // key: work order produce transaction id
        // value: work order produce transaction
        // we will save the work order
        Map<Long, WorkOrderProduceTransaction> inventoryProducedTransactionSet = new HashMap<>();
        Long totalQuantity = 0l;
        for (Inventory inventory : inventories) {
            totalQuantity += inventory.getQuantity();
            logger.debug("will remove inventory with id {}, lpn {}, item {}, quantity {}",
                    inventory.getId(),
                    inventory.getLpn(),
                    inventory.getItem().getName(),
                    inventory.getQuantity());
            inventoryServiceRestemplateClient.reverseByProduct(inventory.getId());


            // see if we can find transaction that created the inventory.
            // if so, we may be able to return the quantity back to the
            // work order line and adjust the quantity of the transaction
            logger.debug("Inventory's work order: {}, transaction id {}",
                    inventory.getWorkOrderId(), inventory.getCreateInventoryTransactionId());
            if (Objects.nonNull(inventory.getWorkOrderId()) &&
                    Objects.nonNull(inventory.getCreateInventoryTransactionId())) {
                WorkOrderProduceTransaction workOrderProduceTransaction =
                        workOrderProduceTransactionService.findById(inventory.getCreateInventoryTransactionId());
                logger.debug("Found right work order produce transaction by id {} ? {}",
                        inventory.getCreateInventoryTransactionId(),
                        Objects.nonNull(workOrderProduceTransaction));
                if (Objects.nonNull(workOrderProduceTransaction)) {
                    logger.debug("Add a new reverse production transaction");
                    workOrderReverseProductionInventoryService.save(new WorkOrderReverseProductionInventory(
                            workOrderProduceTransaction, inventory.getLpn(), inventory.getQuantity()
                    ));
                    // we will put the quantity back
                    // logger.debug("will return the consumed quantity back, only if we can ");
                    // processReverseProductionQuantity(workOrderProduceTransaction, inventory.getQuantity());
                }
            }
        }

        // return the quantity back to work order's by product
        logger.debug("Deduct the produced quantity of the work order by {}",
                totalQuantity);
        inventories.forEach(
                inventory -> {
                    for (WorkOrderByProduct workOrderByProduct : workOrder.getWorkOrderByProducts()) {
                        if (workOrderByProduct.getItemId().equals(inventory.getItem().getId())) {
                            workOrderByProduct.setProducedQuantity(
                                    workOrderByProduct.getProducedQuantity() - inventory.getQuantity()
                            );
                        }
                    }
                }
        );

        return saveOrUpdate(workOrder);
    }
    /**
     * return the quantity back to work order line / production line / etc if necessary
     * @param workOrderProduceTransaction
     * @param quantity quantity of the inventory we are reverse
     */
    private void processReverseProductionQuantity(WorkOrderProduceTransaction workOrderProduceTransaction, Long quantity) {

        // only continue if we consumed the quantity by BOM since this is the only way we
        // can calcuate the exact quantity we consumed to produce this inventory
        if (workOrderProduceTransaction.getConsumeByBomQuantity() &&
                Objects.nonNull(workOrderProduceTransaction.getConsumeByBom())) {
            logger.debug("We will calculate the quantity of each work order line we consumed to produce {} quantity of the final finish good",
                    quantity);
            logger.debug("Bom: {}", workOrderProduceTransaction.getConsumeByBom().getNumber());
            BillOfMaterial matchedBOM = workOrderProduceTransaction.getConsumeByBom();
            for (WorkOrderLine workOrderLine : workOrderProduceTransaction.getWorkOrder().getWorkOrderLines()) {
                // get the matched bom line
                Optional<BillOfMaterialLine> matchedBomLineOptional = matchedBOM.getBillOfMaterialLines().stream().filter(
                        billOfMaterialLine -> workOrderLine.getItemId().equals(billOfMaterialLine.getItemId())
                ).findFirst();
                if (matchedBomLineOptional.isPresent()) {
                    BillOfMaterialLine matchedBomLine = matchedBomLineOptional.get();
                    // get the quantity we consumed
                    Long consumedQuantity = (long)(quantity * matchedBomLine.getExpectedQuantity() / matchedBOM.getExpectedQuantity());
                    logger.debug("Will return {} quantity back to work order {}, item {}",
                            consumedQuantity, workOrderProduceTransaction.getWorkOrder().getNumber(),
                            workOrderLine.getItem().getName());
                    logger.debug("> the new consumed quantity will be {}",
                            Math.max(workOrderLine.getConsumedQuantity() - consumedQuantity, 0));

                    workOrderLine.setConsumedQuantity(Math.max(workOrderLine.getConsumedQuantity() - consumedQuantity, 0));
                    workOrderLineService.save(workOrderLine);
                }


            }
        }
    }

    public ReportHistory generatePrePrintLPNLabel(Long id, String lpnNumber, Long lpnQuantity,
                                                  String productionLineName,
                                                  String locale,
                                                  String printerName) throws JsonProcessingException {

        return generatePrePrintLPNLabel(findById(id), lpnNumber, lpnQuantity,
                productionLineName, locale, printerName);
    }

    public ReportHistory generatePrePrintLPNLabel(WorkOrder workOrder, String lpnNumber, Long lpnQuantity,
                                                  String productionLineName,
                                                  String locale, String printerName) {

        Long warehouseId = workOrder.getWarehouseId();


        Report reportData = new Report();
        // setup the parameters for the label;
        // for label, we don't need the actual data.
        setupPrePrintLPNLabelParameters(
                reportData, workOrder, lpnNumber, lpnQuantity, productionLineName
        );
        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        // logger.debug("####   Report   Data  ######");
        // logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.PRODUCTION_LINE_ASSIGNMENT_LABEL,
                        reportData, locale, printerName

                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;

    }

    private void setupPrePrintLPNLabelParameters(
            Report report, WorkOrder workOrder, String lpnNumber,
            Long lpnQuantity, String productionLineName) {

        // set the parameters to be the meta data of
        // the order

        Map<String, Object> lpnLabelContent =   getLPNLabelContent(
                workOrder, lpnNumber, lpnQuantity, productionLineName
        );
        for(Map.Entry<String, Object> entry : lpnLabelContent.entrySet()) {

            report.addParameter(entry.getKey(), entry.getValue());
        }


    }


    private Map<String, Object> getLPNLabelContent( WorkOrder workOrder, String lpnNumber,
                                                    Long lpnQuantity, String productionLineName) {

        Map<String, Object> lpnLabelContent = new HashMap<>();
        if (Objects.isNull(workOrder.getItem())) {
            workOrder.setItem(
                    inventoryServiceRestemplateClient.getItemById(
                            workOrder.getItemId()
                    )
            );
        }

        if (Objects.isNull(workOrder.getItem())) {
            throw MissingInformationException.raiseException("Not able to print LPN label for work order " +
                    workOrder.getNumber() + ". Fail to get item for this work order");
        }


        lpnLabelContent.put("lpn", lpnNumber);
        lpnLabelContent.put("item_family", Objects.nonNull(workOrder.getItem().getItemFamily()) ?
                workOrder.getItem().getItemFamily().getDescription() : "");
        lpnLabelContent.put("item_name", workOrder.getItem().getName());
        lpnLabelContent.put("item_description", workOrder.getItem().getDescription());
        lpnLabelContent.put("item_description_1", workOrder.getItem().getDescription());

        if (Strings.isNotBlank(workOrder.getItem().getDescription().trim()) &&
                workOrder.getItem().getDescription().trim().length() > 20) {

            // split the description into lines,
            String[] tokens = workOrder.getItem().getDescription().split(" ");
            String line = tokens[0];
            int lineIndex = 1;

            for(int i = 1; i < tokens.length; i++) {
                if (Strings.isBlank(tokens[i].trim())) {
                    continue;
                }
                if (line.length() + tokens[i].length() > 25) {

                    lpnLabelContent.put("item_description_" + lineIndex, line);
                    line = tokens[i];
                    lineIndex++;
                }
                else {
                    line += " " + tokens[i];
                }
            }
            lpnLabelContent.put("item_description_" + lineIndex, line);
        }

        lpnLabelContent.put("work_order_number", workOrder.getNumber());
        lpnLabelContent.put("production_line_name", productionLineName);
        if (Objects.nonNull(lpnQuantity)) {
            logger.debug("LPN Quantity is passed in: {}", lpnQuantity);
            lpnLabelContent.put("quantity", lpnQuantity);

            if (Objects.nonNull(workOrder.getItem().getDefaultItemPackageType()) &&
                    Objects.nonNull(workOrder.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasure())) {
                ItemUnitOfMeasure stockItemUnitOfMeasure = workOrder.getItem().getDefaultItemPackageType().getStockItemUnitOfMeasure();
                if (Objects.isNull(stockItemUnitOfMeasure.getUnitOfMeasure())) {
                    stockItemUnitOfMeasure.setUnitOfMeasure(
                            commonServiceRestemplateClient.getUnitOfMeasureById(
                                    stockItemUnitOfMeasure.getUnitOfMeasureId()
                            )
                    );
                }
                lpnLabelContent.put("stockUOM", stockItemUnitOfMeasure.getUnitOfMeasure().getName());

            }

        }
        else if (workOrder.getItem().getItemPackageTypes().size() > 0){
            logger.debug("LPN Quantity is not passed in, let's get from the UOM");
            // the user doesn't specify hte lpn quantity, let's get from the item's default package type
            ItemPackageType itemPackageType = workOrder.getItem().getItemPackageTypes().get(0);
            // get the biggest item uom
            if (itemPackageType.getItemUnitOfMeasures().size() > 0) {

                Long lpnQuantityFromItemUOM
                        = itemPackageType.getItemUnitOfMeasures().stream().mapToLong(ItemUnitOfMeasure::getQuantity).max().orElse(0l);

                logger.debug("LPN Quantity is setup to {}, according to item {}, package type {}",
                        lpnQuantityFromItemUOM, workOrder.getItem().getName(),
                        itemPackageType.getName());
                lpnLabelContent.put("quantity", lpnQuantityFromItemUOM);
            }
            else  {

                logger.debug("item {} , package type {} have no UOM defined yet",
                        workOrder.getItem().getName(), itemPackageType.getName());
                lpnLabelContent.put("quantity", 0);
            }
        }
        else {

            logger.debug("item {} have no item package type defined yet", workOrder.getItem().getName());
            lpnLabelContent.put("quantity", 0);
        }


        return lpnLabelContent;

    }

    /**
     * Generate multiple labels in a batch, one for each lpn
     * @param id
     * @param lpnNumber
     * @param lpnQuantity
     * @param count
     * @param locale
     * @return
     */
    public ReportHistory generatePrePrintLPNLabelInBatch(Long id, String lpnNumber,
                                                         Long lpnQuantity, Integer count,
                                                         Integer copies,
                                                         String productionLineName,
                                                         String locale,
                                                         String printerName) throws JsonProcessingException {
        return generatePrePrintLPNLabelInBatch(
                findById(id),
                lpnNumber, lpnQuantity, count, copies, productionLineName,
                locale, printerName
        );
    }

    public ReportHistory generatePrePrintLPNLabelInBatch(WorkOrder workOrder, String lpnNumber, Long lpnQuantity,
                                                         Integer count,
                                                         Integer copies,
                                                         String productionLineName,
                                                         String locale,
                                                         String printerName) throws JsonProcessingException {

        Long warehouseId = workOrder.getWarehouseId();
        List<String> lpnNumbers;
        if (Strings.isNotBlank(lpnNumber)) {
            // if the user specify the start lpn, then generate lpns based on this
            lpnNumbers = getNextLPNNumbers(lpnNumber, count);
        }
        else {
            lpnNumbers = commonServiceRestemplateClient.getNextNumberInBatch(warehouseId, "work-order-lpn-number", count);
        }
        logger.debug("we will print labels for lpn : {}", lpnNumbers);
        if (lpnNumbers.size() > 0) {


            Report reportData = new Report();
            // setup the parameters for the label;
            // for label, we don't need the actual data.
            setupPrePrintLPNLabelData(
                    reportData, workOrder, lpnNumbers, lpnQuantity, productionLineName, copies
            );
            logger.debug("will call resource service to print the report with locale: {}",
                    locale);
            logger.debug("####   Report   Data  ######");
            logger.debug(reportData.toString());
            ReportHistory reportHistory =
                    resourceServiceRestemplateClient.generateReport(
                            warehouseId, ReportType.PRODUCTION_LINE_ASSIGNMENT_LABEL,
                            reportData, locale,
                            printerName
                    );


            logger.debug("####   Report   printed: {}", reportHistory.getFileName());
            return reportHistory;
        }
        throw WorkOrderException.raiseException("Can't get lpn numbers");
    }

    private void setupPrePrintLPNLabelData(Report reportData, WorkOrder workOrder, List<String> lpnNumbers,
                                           Long lpnQuantity, String productionLineName, Integer copies) {

        List<Map<String, Object>> lpnLabelContents = new ArrayList<>();
        lpnNumbers.forEach(
                lpnNumber -> {

                    Map<String, Object> lpnLabelContent =   getLPNLabelContent(
                            workOrder, lpnNumber, lpnQuantity, productionLineName
                    );
                    for (int i = 0; i < copies; i++) {
                        lpnLabelContents.add(lpnLabelContent);
                    }
                }
        );
        reportData.setData(lpnLabelContents);

    }

    private List<String> getNextLPNNumbers(String lpn, Integer count) {


        // num[0] will be 21

        List<String> lpnNumbers = new ArrayList<>();
        logger.debug("start to get next batch of lpn number from user input lpn {}", lpn);
        Pattern prefixLetterPattern =  Pattern.compile("[a-zA-Z]+");
        Matcher matcher = prefixLetterPattern.matcher(lpn);
        if (matcher.find()) {
            logger.debug("we found the prefix letters");
            String prefixLetters = matcher.group();
            logger.debug("> {}", prefixLetters);
            Long startNumber = Long.parseLong(lpn.replace(prefixLetters, ""));
            logger.debug("> and the startNumber is {}", startNumber);

            for(int i = 0; i<count ; i++) {
                // padding leading 0 to the number
                String numberPattern = "%0" + (lpn.length() - prefixLetters.length())+ "d";
                lpnNumbers.add(
                        prefixLetters + String.format(numberPattern, (i + startNumber))
                );
            }
        }
        return lpnNumbers;
    }

    public WorkOrder recalculateQCQuantity(Long workOrderId, Long qcQuantity, Double qcPercentage) {

        WorkOrder workOrder = findById(workOrderId);
        if (Objects.isNull(qcQuantity) && Objects.isNull(qcPercentage)) {
            // the user doesn't specify any field, let's re-run the configuration to get the
            // quantity or percentage

            setupQCQuantity(workOrder);
        }
        // if the user specify at least quantity of percentage, then update the field
        // based on the user's input
        else{
            if (Objects.nonNull(qcQuantity)){
                workOrder.setQcQuantity(qcQuantity);
            }
            if (Objects.nonNull(qcPercentage)){
                workOrder.setQcPercentage(qcPercentage);
            }
        }

        return saveOrUpdate(workOrder);
    }

    public WorkOrder addQCQuantity(Long id, Long qcQuantity) {
        WorkOrder workOrder = findById(id);
        logger.debug("start to add qc quantity  {} to the work order {}",
                qcQuantity, workOrder.getNumber());
        workOrder.setQcQuantityCompleted(
                Objects.isNull(workOrder.getQcQuantityCompleted()) ?
                        qcQuantity :
                        workOrder.getQcQuantityCompleted() + qcQuantity
        );
        logger.debug("after the quantity is added, the new qc quantity is {}",
                workOrder.getQcQuantityCompleted());
        return saveOrUpdate(workOrder,false);
    }

    public List<WorkOrder> getAvailableWorkOrderForMPS(Long warehouseId, Long itemId) {

        return workOrderRepository.findOpenWorkOrderByItem(itemId);

    }

    /**
     * Process manual pick for lpn, we will move the LPN to the destination first,
     * then generate the actual asyncronized
     * @param workOrderId
     * @param lpn
     * @param productionLineId
     * @param pickWholeLPN
     * @return
     */
    public List<Pick> processManualPick(Long warehouseId,
                                        Long workOrderId,
                                        String lpn,
                                        Long productionLineId,
                                        Long nextLocationId,
                                        Boolean pickWholeLPN) {


        WorkOrder workOrder = findById(workOrderId);

        validateWorkOrderStatusForManualPick(workOrder);

        Location nextLocation = warehouseLayoutServiceRestemplateClient.getLocationById(nextLocationId);
        if (Objects.isNull(nextLocation)) {
            throw WorkOrderException.raiseException("can't find the location. Fail to generate the manual pick");
        }


        // make the work order to be in process
        workOrder.setStatus(WorkOrderStatus.INPROCESS);
        workOrder = saveOrUpdate(workOrder, false);

        Triple<Long, Boolean, List<Inventory>> pickableInventory = getPickableInventoryForManualPick(workOrder, lpn, productionLineId, pickWholeLPN);

        // TO -DO!!!

        // moveInventoryForManualPick(warehouseId, lpn, nextLocation, pickableInventory.getLeft(),
        //        pickableInventory.getMiddle(), pickableInventory.getRight());

        return new ArrayList<>();
    }

    /**
     * Move the LPN for manual pick. We will generate a pick work number and return so that
     * we can create the pick asyncronized.
     * @param warehouseId
     * @param lpn
     * @param nextLocation
     * @param pickableQuantity
     * @param moveWholeLpn
     * @param pickableInventory
     */
    private List<Inventory> moveInventoryForManualPick(Long warehouseId, String lpn, Location nextLocation,
                                                        Long pickableQuantity, Boolean moveWholeLpn, List<Inventory> pickableInventory,
                                                       Long workOrderId,
                                                       Long workOrderLineId) {
        if(!moveWholeLpn) {
            if (pickableInventory.size() > 1) {

                throw WorkOrderException.raiseException("fail to move LPN " + lpn +
                        " for the manual pick. The LPN has multiple inventory records but " +
                        " not whole LPN needs to be picked, which is not allowed");
            }
            String newLpn = commonServiceRestemplateClient.getNextNumber(warehouseId, "lpn");
            List<Inventory> newInventory = inventoryServiceRestemplateClient.split(
                    pickableInventory.get(0), newLpn, pickableQuantity

            );
            // we will pick from the new LPN
            pickableInventory.clear();
            pickableInventory.add(newInventory.get(1));
        };

        // at this point, we will either
        // 1. move the whole LPN, which is everything in the pickableInventory
        // 2. move partial LPN, which we already split and again still everything is in the pickableInventory
        // in both case, we will move by the whole LPN at this point
        lpn = pickableInventory.get(0).getLpn();
        List<Inventory> movedInventory = moveInventoryForManualPick(warehouseId, lpn, nextLocation);

        String pickedLpn = lpn;
        new Thread(() -> {
            // start to create the pick work
            // and assign to the moved inventory
            /**
            Pick pick = outboundServiceRestemplateClient.generateManualPick(
                    warehouseId,
                    workOrderId,
                    workOrderLineId,
                    pickedLpn,
                    pickableQuantity,
                    nextLocation.getId()
            );
            logger.debug("Pick {} / {} generated for the lpn {}, let's assign to the inventory",
                    pick.getId(),
                    pick.getNumber(),
                    pickedLpn);

             **/



        }).start();

        return movedInventory;


    }

    private List<Inventory> moveInventoryForManualPick(Long warehouseId, String lpn, Location nextLocation) {

        return inventoryServiceRestemplateClient.moveInventory(
                warehouseId, lpn, nextLocation
        );
    }

    /**
     * get the pickable inventory from LPN for a work order
     * 1. pickable quantity from the LPN
     * 2. whether pick the whole LPN(pickWholeLPN = true, or pickable quantity >= lpn quantity)
     * 3. pickable inventory from the LPN
     * @param workOrder
     * @param lpn
     * @param productionLineId
     * @param pickWholeLPN
     * @return
     */
    private Triple<Long, Boolean, List<Inventory>> getPickableInventoryForManualPick(WorkOrder workOrder,
                                                              String lpn,
                                                              Long productionLineId,
                                                              Boolean pickWholeLPN) {
        // Make sure the production line passed in is valid
        if (workOrder.getProductionLineAssignments().stream().noneMatch(
                productionLineAssignment ->
                        productionLineId.equals(productionLineAssignment.getProductionLine().getId())
        )) {
            throw WorkOrderException.raiseException("production line id " + productionLineId +
                    " is invalid. Fail to process manual pick for the work order " + workOrder.getNumber());

        }
        // make sure the LPN is valid LPN
        logger.debug("work order matches with the production line, let's get the inventory from lpn {}",
                lpn);
        List<Inventory> inventories = inventoryServiceRestemplateClient.findInventoryByLPN(
                workOrder.getWarehouseId(), lpn
        );
        if (inventories.isEmpty()) {

            throw WorkOrderException.raiseException("LPN " + lpn +
                    " is invalid. Fail to process manual pick for the work order " + workOrder.getNumber());
        }

        logger.debug("we are able to find inventory with this LPN, let's see if the item matches with the work order");

        // make sure there's only one item in the LPN
        List<Long> itemIdList = inventories.stream().map(Inventory::getItem).map(Item::getId).distinct().collect(Collectors.toList());
        if (itemIdList.size() > 1) {

            throw WorkOrderException.raiseException("LPN " + lpn +
                    " is mixed with different items. Fail to generate manual pick for the work order " + workOrder.getNumber());
        }

        // get the matched work order line by the item id
        Long itemId = itemIdList.get(0);


        // inventory status required, either from the work order line
        // or from the work order line's spare part
        Long inventoryStatusId;

        // total quantity required, either from the work order line
        // or from the work order line's spare part
        Long quantityRequiredByWorkOrderLine = 0l;

        // make sure the item matches with the work order line, or any spare part of the work order line
        Optional<WorkOrderLine> matchedWorkOrderLineOptional = workOrder.getWorkOrderLines().stream().filter(
                workOrderLine -> workOrderLine.getItemId().equals(itemId)
        ).findFirst();

        // if there's no matched work order line with the item id, see if this item is a
        // spare part
        if (!matchedWorkOrderLineOptional.isPresent()) {
            logger.debug("can't find the item in the work lines, it may be a spare part");
            Optional<WorkOrderLineSparePartDetail> matchedWorkOrderLineSparePartDetailOptional =
                    workOrder.getWorkOrderLines().stream().map(
                            workOrderLine -> workOrderLine.getWorkOrderLineSpareParts())
                            .flatMap(List::stream).map(workOrderLineSparePart -> workOrderLineSparePart.getWorkOrderLineSparePartDetails())
                            .flatMap(List::stream).filter(workOrderLineSparePartDetail -> workOrderLineSparePartDetail.getItemId().equals(itemId))
                            .findFirst();
            if (!matchedWorkOrderLineSparePartDetailOptional.isPresent()) {

                throw WorkOrderException.raiseException("Can't find any work order line matched with item id " + itemId +
                        "Fail to process manual pick");
            }
            else {
                WorkOrderLineSparePartDetail matchedWorkOrderLineSparePartDetail
                        = matchedWorkOrderLineSparePartDetailOptional.get();

                WorkOrderLine matchedWorkOrderLine = matchedWorkOrderLineSparePartDetail.getWorkOrderLineSparePart().getWorkOrderLine();

                // if the open quantity is 0, which means the work order line is fully allocated,
                // we either have pick or short allocation against the work order line
                // we will not allow the user to manual pick
                if (matchedWorkOrderLine.getOpenQuantity() <= 0) {
                    throw WorkOrderException.raiseException("work order " + workOrder.getNumber() +
                            ", line " + matchedWorkOrderLine.getNumber() + " is fully processed." +
                            "Fail to generate manual pick");
                }

                inventoryStatusId = matchedWorkOrderLineSparePartDetail.getInventoryStatusId();
                quantityRequiredByWorkOrderLine = matchedWorkOrderLine.getOpenQuantity() -
                        ((Objects.isNull(matchedWorkOrderLine.getSparePartQuantity())) ? 0 : matchedWorkOrderLine.getSparePartQuantity());
                // we will need to apply the ratio to the required quantity of the work order line
                // as we are processing the spare part
                double ratio = matchedWorkOrderLineSparePartDetail.getQuantity() * 1.0 /
                        matchedWorkOrderLineSparePartDetail.getWorkOrderLineSparePart().getQuantity();
                quantityRequiredByWorkOrderLine = (long)(quantityRequiredByWorkOrderLine * ratio);
            }
        }
        else {
            WorkOrderLine matchedWorkOrderLine = matchedWorkOrderLineOptional.get();
            logger.debug("found matched work order line {} / {}", workOrder.getNumber(),
                    matchedWorkOrderLine.getNumber());

            // if the open quantity is 0, which means the work order line is fully allocated,
            // we either have pick or short allocation against the work order line
            // we will not allow the user to manual pick
            if (matchedWorkOrderLine.getExpectedQuantity() > 0 &&
                    matchedWorkOrderLine.getOpenQuantity() <= 0) {
                throw WorkOrderException.raiseException("work order " + workOrder.getNumber() +
                        ", line " + matchedWorkOrderLine.getNumber() + " is fully processed." +
                        "Fail to generate manual pick");
            }
            inventoryStatusId = matchedWorkOrderLine.getInventoryStatusId();
            quantityRequiredByWorkOrderLine = matchedWorkOrderLine.getOpenQuantity() -
                    ((Objects.isNull(matchedWorkOrderLine.getSparePartQuantity())) ? 0 : matchedWorkOrderLine.getSparePartQuantity());
            logger.debug("> the line still need {} of the item", quantityRequiredByWorkOrderLine);

        }


        // get the pickable inventory
        List<Inventory> pickableInventory = inventoryServiceRestemplateClient.getPickableInventory(
                itemId, inventoryStatusId, inventories.get(0).getLocationId(),
                lpn
        );
        if (pickableInventory.isEmpty()) {

            throw WorkOrderException.raiseException("LPN " + lpn +
                    " is not pickable. Fail to generate manual pick for the work order " + workOrder.getNumber());
        }

        // check if how much we can pick from this LPN
        Long inventoryQuantity = pickableInventory.stream().map(Inventory::getQuantity).mapToLong(Long::longValue).sum();
        logger.debug("> we can pick {} from the LPN {}", inventoryQuantity, lpn);

        if (Boolean.TRUE.equals(pickWholeLPN)) {
            // if the user specify to pick the whole LPN
            // then return the pickable quantity from this LPN
            logger.debug("pickWholeLPN is set to true, we will pick the whole LPN regardless of the quantity");
            return Triple.of(inventoryQuantity, true, pickableInventory);
        }


        // if we don't need the whole LPN and pickWholeLPN is not setup
        // which means we will need to pick partially from the LPN
        // > In this case, if we have multiple record in this LPN, then it may be a bit
        //   difficult to calculate which piece of LPN we will need to pick from
        //   TO-DO: we may review the logic later on
        if (inventoryQuantity > quantityRequiredByWorkOrderLine &&
            !Boolean.TRUE.equals(pickWholeLPN) &&
            pickableInventory.size() > 1) {
            throw WorkOrderException.raiseException("can't pick from LPN " + lpn +
                    " as it has mixed records but pick the whole LPN is not allowed in this case");
        }

        long quantityToBePicked = Math.min(inventoryQuantity, quantityRequiredByWorkOrderLine);


        return Triple.of(
                quantityToBePicked,
                inventoryQuantity <= quantityToBePicked,
                pickableInventory);
    }


    /**
     * generate manual pick for work order
     * @param workOrderId
     * @param lpn
     * @return
     */
    public List<Pick> generateManualPick(Long workOrderId, String lpn,
                                         Long productionLineId,
                                         Boolean pickWholeLPN) {
        WorkOrder workOrder = findById(workOrderId);

        validateWorkOrderStatusForManualPick(workOrder);

        // make the work order to be in process
        workOrder.setStatus(WorkOrderStatus.INPROCESS);
        workOrder = saveOrUpdate(workOrder, false);


        // make sure we can manual pick the LPN for the work order
        Long pickableQuantity = getPickableQuantityForManualPick(workOrder, lpn, productionLineId, pickWholeLPN);

        if (pickableQuantity <= 0 ) {
            throw WorkOrderException.raiseException("there's nothing left to be picked from this LPN " + lpn
                + ", Fail to generate manual pick for the work order " + workOrder.getNumber());
        }
        List<Pick> picks = new ArrayList<>();
        try {

            picks = outboundServiceRestemplateClient.generateManualPick(
                    workOrder.getWarehouseId(),
                    workOrder.getId(),
                    lpn,
                    productionLineId,
                    pickableQuantity
            );
            logger.debug("We got {} manual picks for this work order {}", picks.size(),
                    workOrder.getNumber());
            if (picks.size() > 0) {

                // we should only get the picks from the allocation
                AllocationResult allocationResult = new AllocationResult();
                allocationResult.setPicks(picks);

                // process the quantity in the work order and work order line
                // to reflect the allocation
                logger.debug("let's update the work order's line quantity based on the picks we just generated");
                processAllocateResult(workOrder, allocationResult);
                // process the spare part, if needed
                logger.debug("let's update the work order's spare quantity based on the picks we just generated");
                processAllocationResultForSpareParts(allocationResult);

                Long pickedQuantity = picks.stream()
                        .filter(pick -> !isPickSparePart(pick.getWorkOrderLineId(), pick))
                        .map(Pick::getQuantity).mapToLong(Long::longValue).sum();

                // only process the production line assignment if the pick is not for spare part
                if (pickableQuantity > 0) {
                    logger.debug("we may need to update the production line assignment after we got the picks");
                    processProductionLineAssignment(workOrder, productionLineId, pickedQuantity);

                }


            }

        }
        catch (Exception ex) {
            ex.printStackTrace();
            throw WorkOrderException.raiseException("Can't do manual pick from LPN " + lpn +
                    " for the work order " + workOrder.getNumber() + ", error: " + ex.getMessage());
        }

        // if the work order is still in PENDING process, then


        return picks;
    }

    private void validateWorkOrderStatusForManualPick(WorkOrder workOrder) {
        if (workOrder.getStatus().equals(WorkOrderStatus.CANCELLED) ||
            workOrder.getStatus().equals(WorkOrderStatus.COMPLETED) ||
            workOrder.getStatus().equals(WorkOrderStatus.CLOSED)) {
            throw WorkOrderException.raiseException("Can't generate manual pick for work order " +
                    workOrder.getNumber() + " as its status is " +
                    workOrder.getStatus() + " and not suitable for pick");
        }
    }

    public Long getPickableQuantityForManualPick(Long workOrderId, String lpn, Long productionLineId, Boolean pickWholeLPN) {
        return getPickableQuantityForManualPick(findById(workOrderId), lpn, productionLineId, pickWholeLPN);
    }

    /**
     * Check how mnuch we can pick from this LPN for manual pick with the work order
     * @param workOrder
     * @param lpn
     * @param productionLineId
     * @return
     */
    public Long getPickableQuantityForManualPick(WorkOrder workOrder, String lpn,
                                                 Long productionLineId,
                                                 Boolean pickWholeLPN) {

        // Make sure the production line passed in is valid
        if (workOrder.getProductionLineAssignments().stream().noneMatch(
                productionLineAssignment ->
                        productionLineId.equals(productionLineAssignment.getProductionLine().getId())
        )) {
            throw WorkOrderException.raiseException("production line id " + productionLineId +
                    " is invalid. Fail to generate manual pick for the work order " + workOrder.getNumber());

        }
        // make sure the LPN is valid LPN
        logger.debug("work order matches with the production line, let's get the inventory from lpn {}",
                lpn);
        List<Inventory> inventories = inventoryServiceRestemplateClient.findInventoryByLPN(
                workOrder.getWarehouseId(), lpn
        );
        if (inventories.isEmpty()) {

            throw WorkOrderException.raiseException("LPN " + lpn +
                    " is invalid. Fail to generate manual pick for the work order " + workOrder.getNumber());
        }

        logger.debug("we are able to find inventory with this LPN, let's see if the item matches with the work order");

        // make sure there's only one item in the LPN
        List<Long> itemIdList = inventories.stream().map(Inventory::getItem).map(Item::getId).distinct().collect(Collectors.toList());
        if (itemIdList.size() > 1) {

            throw WorkOrderException.raiseException("LPN " + lpn +
                    " is mixed with different items. Fail to generate manual pick for the work order " + workOrder.getNumber());
        }

        // get the matched work order line by the item id
        Long itemId = itemIdList.get(0);


        // inventory status required, either from the work order line
        // or from the work order line's spare part
        Long inventoryStatusId;

        // total quantity required, either from the work order line
        // or from the work order line's spare part
        Long quantityRequiredByWorkOrderLine = 0l;

        // make sure the item matches with the work order line, or any spare part of the work order line
        Optional<WorkOrderLine> matchedWorkOrderLineOptional = workOrder.getWorkOrderLines().stream().filter(
                workOrderLine -> workOrderLine.getItemId().equals(itemId)
        ).findFirst();

        // if there's no matched work order line with the item id, see if this item is a
        // spare part
        if (!matchedWorkOrderLineOptional.isPresent()) {
            logger.debug("can't find the item in the work lines, it may be a spare part");
            Optional<WorkOrderLineSparePartDetail> matchedWorkOrderLineSparePartDetailOptional =
                    workOrder.getWorkOrderLines().stream().map(
                    workOrderLine -> workOrderLine.getWorkOrderLineSpareParts())
                            .flatMap(List::stream).map(workOrderLineSparePart -> workOrderLineSparePart.getWorkOrderLineSparePartDetails())
                            .flatMap(List::stream).filter(workOrderLineSparePartDetail -> workOrderLineSparePartDetail.getItemId().equals(itemId))
                    .findFirst();
            if (!matchedWorkOrderLineSparePartDetailOptional.isPresent()) {

                throw WorkOrderException.raiseException("Can't find any work order line matched with item id " + itemId +
                        "Fail to generate manual pick");
            }
            else {
                WorkOrderLineSparePartDetail matchedWorkOrderLineSparePartDetail
                        = matchedWorkOrderLineSparePartDetailOptional.get();

                WorkOrderLine matchedWorkOrderLine = matchedWorkOrderLineSparePartDetail.getWorkOrderLineSparePart().getWorkOrderLine();

                // if the open quantity is 0, which means the work order line is fully allocated,
                // we either have pick or short allocation against the work order line
                // we will not allow the user to manual pick
                if (matchedWorkOrderLine.getOpenQuantity() <= 0) {
                    throw WorkOrderException.raiseException("work order " + workOrder.getNumber() +
                            ", line " + matchedWorkOrderLine.getNumber() + " is fully processed." +
                            "Fail to generate manual pick");
                }

                inventoryStatusId = matchedWorkOrderLineSparePartDetail.getInventoryStatusId();
                quantityRequiredByWorkOrderLine = matchedWorkOrderLine.getOpenQuantity() -
                        ((Objects.isNull(matchedWorkOrderLine.getSparePartQuantity())) ? 0 : matchedWorkOrderLine.getSparePartQuantity());
                // we will need to apply the ratio to the required quantity of the work order line
                // as we are processing the spare part
                double ratio = matchedWorkOrderLineSparePartDetail.getQuantity() * 1.0 /
                        matchedWorkOrderLineSparePartDetail.getWorkOrderLineSparePart().getQuantity();
                quantityRequiredByWorkOrderLine = (long)(quantityRequiredByWorkOrderLine * ratio);
            }
        }
        else {
            WorkOrderLine matchedWorkOrderLine = matchedWorkOrderLineOptional.get();
            logger.debug("found matched work order line {} / {}", workOrder.getNumber(),
                    matchedWorkOrderLine.getNumber());

            // if the open quantity is 0, which means the work order line is fully allocated,
            // we either have pick or short allocation against the work order line
            // we will not allow the user to manual pick
            if (matchedWorkOrderLine.getExpectedQuantity() > 0 &&
                    matchedWorkOrderLine.getOpenQuantity() <= 0) {
                throw WorkOrderException.raiseException("work order " + workOrder.getNumber() +
                        ", line " + matchedWorkOrderLine.getNumber() + " is fully processed." +
                        "Fail to generate manual pick");
            }
            inventoryStatusId = matchedWorkOrderLine.getInventoryStatusId();
            quantityRequiredByWorkOrderLine = matchedWorkOrderLine.getOpenQuantity() -
                    ((Objects.isNull(matchedWorkOrderLine.getSparePartQuantity())) ? 0 : matchedWorkOrderLine.getSparePartQuantity());
            logger.debug("> the line still need {} of the item", quantityRequiredByWorkOrderLine);

        }


        // get the pickable inventory
        List<Inventory> pickableInventory = inventoryServiceRestemplateClient.getPickableInventory(
                itemId, inventoryStatusId, inventories.get(0).getLocationId(),
                lpn
        );
        if (pickableInventory.isEmpty()) {

            throw WorkOrderException.raiseException("LPN " + lpn +
                    " is not pickable. Fail to generate manual pick for the work order " + workOrder.getNumber());
        }

        // check if how much we can pick from this LPN
        Long inventoryQuantity = pickableInventory.stream().map(Inventory::getQuantity).mapToLong(Long::longValue).sum();
        logger.debug("> we can pick {} from the LPN {}", inventoryQuantity, lpn);
        if (Boolean.TRUE.equals(pickWholeLPN)) {
            // if the user specify to pick the whole LPN
            // then return the pickable quantity from this LPN
            logger.debug("pickWholeLPN is set to true, we will pick the whole LPN regardless of the quantity");
            return inventoryQuantity;
        }
        return Math.min(inventoryQuantity, quantityRequiredByWorkOrderLine);
    }


    /**
     * Send new work order alert
     * @param workOrder
     */
    private void sendAlertForWorkOrder(WorkOrder workOrder, boolean newWorkOrderFlag, String username) {
        if (Strings.isBlank(username)) {

            try {
                username = userService.getCurrentUserName();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                logger.debug("We got error while getting username from the session, let's just ignore.\nerror: {}",
                        ex.getMessage());
            }

        }

        Long companyId = warehouseLayoutServiceRestemplateClient.getWarehouseById(workOrder.getWarehouseId()).getCompanyId();
        StringBuilder alertParameters = new StringBuilder();
        alertParameters.append("number=").append(workOrder.getNumber())
                .append("&lineCount=").append(workOrder.getWorkOrderLines().size());

        if (newWorkOrderFlag) {

            Alert alert = new Alert(companyId,
                    AlertType.NEW_WORK_ORDER,
                    "NEW-WORK-ORDER-" + companyId + "-" + workOrder.getWarehouseId() + "-" + workOrder.getNumber(),
                    "Work Order " + workOrder.getNumber() + " created, by " + username,
                    "", alertParameters.toString());
            kafkaSender.send(alert);
        }
        else {

            Alert alert = new Alert(companyId,
                    AlertType.MODIFY_WORK_ORDER,
                    "MODIFY-WORK-ORDER-" + companyId + "-" + workOrder.getWarehouseId() + "-" + workOrder.getNumber(),
                    "Work Order " + workOrder.getNumber() + " is changed, by " + username,
                    "", alertParameters.toString());
            kafkaSender.send(alert);
        }
    }

    public WorkOrder createWorkOrderForShortAllocation(
            Long shortAllocationId, Long billOfMaterialId, String workOrderNumber, Long expectedQuantity, Long productionLineId) {
        WorkOrder workOrder = createWorkOrderFromBOM(billOfMaterialId, workOrderNumber,
                expectedQuantity, productionLineId);
        workOrder.setShortAllocationId(shortAllocationId);
        return saveOrUpdate(workOrder);

    }

    public WorkOrder addWorkOrder(WorkOrder workOrder) {
        // init work order
        if (workOrder.getExpectedQuantity() <= 0) {
            throw WorkOrderException.raiseException("work order " + workOrder.getNumber() +
                    " 's quantity can't be less than 0");
        }
        workOrder.setProducedQuantity(0L);
        workOrder.setStatus(WorkOrderStatus.PENDING);

        WorkOrder savedWorkOrder = saveOrUpdate(workOrder, false);

        // init work order line
        for (WorkOrderLine workOrderLine : workOrder.getWorkOrderLines()) {

            if (workOrderLine.getExpectedQuantity() <= 0) {

                throw WorkOrderException.raiseException("work order line" + workOrderLine.getNumber() +
                        " 's quantity can't be less than 0");
            }
            workOrderLine.setWorkOrder(savedWorkOrder);

            workOrderLine.setOpenQuantity(workOrderLine.getExpectedQuantity());
            workOrderLine.setInprocessQuantity(0L);
            workOrderLine.setDeliveredQuantity(0L);
            workOrderLine.setConsumedQuantity(0L);
            //TO-DO: Default to FIFO for now
            workOrderLine.setAllocationStrategyType(AllocationStrategyType.FIRST_IN_FIRST_OUT);
            workOrderLineService.saveOrUpdate(workOrderLine, false);
        }

        // init work order instruction
        workOrder.getWorkOrderInstructions().forEach(
                workOrderInstruction -> {

                    workOrderInstruction.setWorkOrder(savedWorkOrder);
                    workOrderInstructionService.saveOrUpdate(workOrderInstruction);
                }
        );

        // init work order by product
        for (WorkOrderByProduct workOrderByProduct : workOrder.getWorkOrderByProducts()) {

            if (workOrderByProduct.getExpectedQuantity() <= 0) {

                throw WorkOrderException.raiseException("work order by product " +
                        (Objects.isNull(workOrderByProduct.getItem()) ?
                            workOrderByProduct.getItemId().toString() :
                            workOrderByProduct.getItem().getName())
                                +
                        " 's quantity can't be less than 0");
            }
            workOrderByProduct.setWorkOrder(savedWorkOrder);

            workOrderByProduct.setProducedQuantity(0L);
            workOrderByProductService.save(workOrderByProduct);
        }

        return findById(savedWorkOrder.getId());
    }

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        logger.debug("start to process item override for work order line, current warehouse {}, from item id {} to item id {}",
                warehouseId, oldItemId, newItemId);
        workOrderRepository.processItemOverride(oldItemId, newItemId, warehouseId);


        workOrderLineService.handleItemOverride(warehouseId,
                oldItemId, newItemId);

        workOrderByProductService.handleItemOverride(warehouseId,
                oldItemId, newItemId);
        workOrderLineSparePartDetailService.handleItemOverride(warehouseId,
                oldItemId, newItemId);
        workOrderQCRuleConfigurationService.handleItemOverride(warehouseId,
                oldItemId, newItemId);

        billOfMaterialService.handleItemOverride(warehouseId,
                oldItemId, newItemId);


        materialRequirementsPlanningService.handleItemOverride(warehouseId,
                oldItemId, newItemId);

        masterProductionScheduleService.handleItemOverride(warehouseId,
                oldItemId, newItemId);

        productionLineCapacityService.handleItemOverride(warehouseId,
                oldItemId, newItemId);

        productionPlanLineService.handleItemOverride(warehouseId,
                oldItemId, newItemId);
    }

    public WorkOrder changeWorkOrder(WorkOrder workOrder) {
        // we will only allow to change the by product
        workOrder.getWorkOrderByProducts().forEach(
                workOrderByProduct -> workOrderByProduct.setWorkOrder(workOrder)
        );

        return saveOrUpdate(workOrder, false);


    }

}
