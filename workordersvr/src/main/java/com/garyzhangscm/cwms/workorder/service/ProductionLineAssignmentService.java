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
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineAssignmentLineRepository;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineAssignmentRepository;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class ProductionLineAssignmentService   {
    private static final Logger logger = LoggerFactory.getLogger(ProductionLineAssignmentService.class);

    @Autowired
    private ProductionLineAssignmentRepository productionLineAssignmentRepository;

    @Autowired
    private ProductionLineAssignmentLineRepository productionLineAssignmentLineRepository;
    @Autowired
    private WorkOrderQCSampleService workOrderQCSampleService;

    @Autowired
    private ProductionLineDeliveryService productionLineDeliveryService;
    @Autowired
    WorkOrderService workOrderService;
    @Autowired
    ProductionLineService productionLineService;
    @Autowired
    private ProductionLineCapacityService productionLineCapacityService;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private OutboundServiceRestemplateClient outboundServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;



    public ProductionLineAssignment findById(Long id) {
        return findById(id, true);
    }
    public ProductionLineAssignment findById(Long id, boolean loadDetail ) {
        ProductionLineAssignment productionLineAssignment = productionLineAssignmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("production line assignment not found by id: " + id));
        if (loadDetail) {
            loadAttribute(productionLineAssignment);
        }
        return productionLineAssignment;
    }
    public void loadAttribute(List<ProductionLineAssignment> productionLineAssignments) {
        productionLineAssignments.forEach(
                productionLineAssignment -> loadAttribute(productionLineAssignment)
        );
    }

    public void loadAttribute(ProductionLineAssignment productionLineAssignment) {
        if (Objects.nonNull(productionLineAssignment.getWorkOrder())) {
                productionLineAssignment.setWorkOrderId(
                        productionLineAssignment.getWorkOrder().getId()
                );
                productionLineAssignment.setWorkOrderNumber(
                        productionLineAssignment.getWorkOrder().getNumber()
                );
                workOrderService.loadAttribute(productionLineAssignment.getWorkOrder());
        }
    }


    public List<ProductionLineAssignment> findAll(Long warehouseId,
                                                  Long productionLineId,
                                                  String productionLineIds,
                                                  Long workOrderId,
                                                  String productionLineNames) {
        return productionLineAssignmentRepository.findAll(
                        (Root<ProductionLineAssignment> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                            List<Predicate> predicates = new ArrayList<Predicate>();


                            Join<ProductionLineAssignment, ProductionLine> joinProductionLine = root.join("productionLine", JoinType.INNER);

                            predicates.add(criteriaBuilder.equal(joinProductionLine.get("warehouseId"), warehouseId));

                            if (Objects.nonNull(productionLineId)) {
                                predicates.add(criteriaBuilder.equal(joinProductionLine.get("id"), productionLineId));
                            }
                            else if (Strings.isNotBlank(productionLineIds)) {

                                CriteriaBuilder.In<Long> inProductionLineIds = criteriaBuilder.in(joinProductionLine.get("id"));
                                for(String id : productionLineIds.split(",")) {
                                    inProductionLineIds.value(Long.parseLong(id));
                                }
                                predicates.add(criteriaBuilder.and(inProductionLineIds));

                            }
                            else if (Strings.isNotBlank(productionLineNames)) {

                                CriteriaBuilder.In<String> inProductionLineNames = criteriaBuilder.in(joinProductionLine.get("name"));
                                for(String name : productionLineNames.split(",")) {
                                    inProductionLineNames.value(name);
                                }
                                predicates.add(criteriaBuilder.and(inProductionLineNames));

                            }

                            if (Objects.nonNull(workOrderId)) {
                                Join<ProductionLineAssignment, WorkOrder> joinWorkOrder = root.join("workOrder", JoinType.INNER);
                                predicates.add(criteriaBuilder.equal(joinWorkOrder.get("id"), workOrderId));

                            }
                            Predicate[] p = new Predicate[predicates.size()];
                            return criteriaBuilder.and(predicates.toArray(p));
                        }
                );

    }

    public ProductionLineAssignment findByWorkOrderAndProductionLine(Long productionLineId,
                                                                     Long workOrderId) {
        return productionLineAssignmentRepository.findByWorkOrderAndProductionLine(
                workOrderId, productionLineId
        );
    }




    public ProductionLineAssignment save(ProductionLineAssignment productionLineAssignment) {
        return productionLineAssignmentRepository.save(productionLineAssignment);
    }

    public ProductionLineAssignment saveOrUpdate(ProductionLineAssignment productionLineAssignment) {
        if (productionLineAssignment.getId() == null &&
                findByWorkOrderAndProductionLine(
                        productionLineAssignment.getWorkOrder().getId(),
                        productionLineAssignment.getProductionLine().getId()
                ) != null) {
            productionLineAssignment.setId(
                    findByWorkOrderAndProductionLine(
                            productionLineAssignment.getWorkOrder().getId(),
                            productionLineAssignment.getProductionLine().getId()
                    ).getId());
        }
        return save(productionLineAssignment);
    }


    public void delete(ProductionLineAssignment productionLineAssignment) {
        productionLineAssignmentRepository.delete(productionLineAssignment);
    }

    public void delete(Long id) {
        productionLineAssignmentRepository.deleteById(id);
    }

    public void removeProductionLineAssignmentForWorkOrder(Long warehouseId, Long workOrderId) {
        List<ProductionLineAssignment> productionLineAssignments = findAll(
                warehouseId, null, null, workOrderId, null
        );
        productionLineAssignments.forEach(productionLineAssignment -> {
            delete(productionLineAssignment);
        });
    }

    public List<ProductionLineAssignment> assignWorkOrderToProductionLines(
            Long workOrderId, List<ProductionLineAssignment> productionLineAssignments) {

        WorkOrder workOrder = workOrderService.findById(workOrderId);

        for (ProductionLineAssignment productionLineAssignment : productionLineAssignments) {

            assignWorkOrderToProductionLines(workOrder,productionLineAssignment);
            productionLineAssignment.setAssignedTime(ZonedDateTime.now(ZoneOffset.UTC));
            productionLineAssignment.setDeassigned(false);

        }
        // if the work order is still in PENDING status, change it into work in process
        //
        logger.debug("Current work order's status {}", workOrder.getStatus());
        if (workOrder.getStatus().equals(WorkOrderStatus.PENDING)) {
            logger.debug("change work order {}'s status from {} to {}",
                    workOrder.getNumber(),
                    workOrder.getStatus(),
                    WorkOrderStatus.INPROCESS);
            workOrder.setStatus(WorkOrderStatus.INPROCESS);
            workOrderService.save(workOrder);
        }

        return findAll(workOrder.getWarehouseId(), null, null, workOrderId, null);

    }
    public List<ProductionLineAssignment> assignWorkOrderToProductionLines(Long warehouseId, Long workOrderId, String productionLineIds, String quantities) {
        // remove the assignment for the work order first

        removeProductionLineAssignmentForWorkOrder(warehouseId, workOrderId);

        String[] productionLineIdArray = productionLineIds.split(",");
        String[] quantityArray = quantities.split(",");

        if (productionLineIdArray.length == 0 ||
                productionLineIdArray.length != quantityArray.length) {
            throw WorkOrderException.raiseException("Can't assign production lines to the work order");
        }

        WorkOrder workOrder = workOrderService.findById(workOrderId);
        // let's make sure the total quantity matches with the work order quantity
        Long totalQuantity = Arrays.stream(quantityArray).mapToLong(Long::parseLong).sum();

        if (workOrder.getExpectedQuantity() != totalQuantity) {

            throw WorkOrderException.raiseException("Can't assign production lines to the work order, total quantity doesn't match with work order's quantity");
        }

        for (int i = 0; i < productionLineIdArray.length; i++) {
            Long productionLineId = Long.parseLong(productionLineIdArray[i]);
            Long quantity = Long.parseLong(quantityArray[i]);

            assignWorkOrderToProductionLines(workOrder,
                    productionLineService.findById(productionLineId)
                    , quantity);

        }

        return findAll(warehouseId, null, null, workOrderId, null);
    }




    public void assignWorkOrderToProductionLines(WorkOrder workOrder, ProductionLine productionLine, Long quantity) {
        ProductionLineAssignment productionLineAssignment = new ProductionLineAssignment(
                workOrder,
                productionLine,
                quantity
        );
        saveOrUpdate(productionLineAssignment);
    }

    public ProductionLineAssignmentLine saveLine(ProductionLineAssignmentLine productionLineAssignmentLine) {

        return productionLineAssignmentLineRepository.save(productionLineAssignmentLine);
    }


    public void assignWorkOrderToProductionLines(WorkOrder workOrder, ProductionLineAssignment productionLineAssignment) {
        if (productionLineAssignment.getQuantity() == 0) {
            return;
        }
        productionLineAssignment.setWorkOrder(workOrder);
        if (productionLineAssignment.getLines().isEmpty()) {
            // if we haven't do so, let's split the work line quantity as well and
            // create the assignment lines, one for each production line & work order line
            // we will use this information to allow the user to allocate by work order line
            logger.debug("start to split the work order line quantity to the production line {}",
                    productionLineAssignment.getProductionLine().getName());
            Double ratio = productionLineAssignment.getQuantity() * 1.0 / workOrder.getExpectedQuantity();
            workOrder.getWorkOrderLines().forEach(
                    workOrderLine -> {
                        long assignedWorkOrderQuantity = (long)Math.floor(
                                workOrderLine.getExpectedQuantity() * ratio
                        );
                        logger.debug("> will assign {} from work order line {}  / {}",
                                assignedWorkOrderQuantity, workOrderLine.getId(),
                                Objects.isNull(workOrderLine.getItem()) ?
                                        workOrderLine.getItemId() : workOrderLine.getItem().getName());
                        productionLineAssignment.addLine(
                                new ProductionLineAssignmentLine(
                                        workOrderLine,
                                        productionLineAssignment,
                                        assignedWorkOrderQuantity,
                                        assignedWorkOrderQuantity
                                )
                        );
                    }
            );

        }
        // set the open quantity to the total quantity of the assignment
        // we will use this quantity to keep track of how much quantity we can still
        // allocate from the work order on this production line. When this number
        // become 0, it means we have fully allocated this work order
        productionLineAssignment.setOpenQuantity(productionLineAssignment.getQuantity());
        // logger.debug("Save production line assignment\n{}",
        //         productionLineAssignment);
        productionLineAssignment.getLines().forEach(
                productionLineAssignmentLine -> productionLineAssignmentLine.setProductionLineAssignment(
                        productionLineAssignment
                )
        );
        saveOrUpdate(productionLineAssignment);
    }

    public List<WorkOrder> getAssignedWorkOrderByProductionLine(Long warehouseId, Long productionLineId) {
        List<ProductionLineAssignment> productionLineAssignments =
                findAll(warehouseId, productionLineId, null, null, null);

        logger.debug("get {} assignment from production line {}, warehouse id {}",
            productionLineAssignments.size(), productionLineId, warehouseId);
        return productionLineAssignments.stream().map(
                productionLineAssignment -> productionLineAssignment.getWorkOrder()
        ).map(workOrder -> {
            workOrderService.loadAttribute(workOrder, false, false);
            return workOrder;
        }).collect(Collectors.toList());
    }

    private void processReturnableMaterial(
            WorkOrder workOrder, ProductionLine productionLine, List<Inventory> returnableMaterial) {
        // start to create the returned inventory;
        List<Inventory> deliveredInventory = workOrderService.getDeliveredInventory(workOrder.getId(), productionLine.getId());
        logger.debug("We got {} delivered inventory that still in the system, and the user told us there's {} inventory left from the production line {}",
                deliveredInventory.size(), returnableMaterial.size(), productionLine.getName());

        // setup the work order line. We will need to setup the work order line id for the inventory
        // so that we know the inventory is a return material for certain work order line
        // key: item id
        // value: work order id
        Map<Long, Long> workOrderLineMap = new HashMap<>();
        workOrder.getWorkOrderLines().forEach(workOrderLine ->
                workOrderLineMap.put(workOrderLine.getItemId(), workOrderLine.getId()));

        // for each delivered inventory,
        // 1. if the inventory doesn't exists in the returnable material list, then we will need to remove it
        // 2. if the quantity changed, then we will need to update the quantity
        // for each returnable material, if the inventory doesn't exists in the delivered inventory list(the id of
        // the inventory is null) then we will create the inventory in the production's out location

        // new inventory structure that without an id, will be added to the system
        returnableMaterial.stream().filter(
                inventory -> Objects.isNull(inventory.getId())
        ).forEach(inventory -> {
           inventory.setWorkOrderLineId(workOrderLineMap.getOrDefault(inventory.getItem().getId(), null));
           if (Objects.isNull(inventory.getLocation())) {
               // location is not setup yet for the new inventory,
               // we will default to the production line's out location
               inventory.setLocationId(productionLine.getOutboundStageLocationId());
               if (Objects.nonNull(productionLine.getOutboundStageLocation())) {
                   inventory.setLocation(productionLine.getOutboundStageLocation());
               } else {
                   inventory.setLocation(
                           warehouseLayoutServiceRestemplateClient.getLocationById(
                                   productionLine.getOutboundStageLocationId()
                           ));
               }
           }
           inventoryServiceRestemplateClient.receiveInventoryFromWorkOrderLine(inventory);
        });

        // returnable material that with an id, we will compare to the existing inventory with same id
        // to see whether the quantity is changed
        // Key: inventory id
        // value: inventory quantity
        Map<Long, Long> existingRetunableMaterialMap = new HashMap<>();
        returnableMaterial.stream().filter(
                inventory -> Objects.nonNull(inventory.getId())
        ).forEach(
                inventory -> existingRetunableMaterialMap.put(inventory.getId(), inventory.getQuantity())
        );

        for(Inventory inventory : deliveredInventory) {
            if (!existingRetunableMaterialMap.containsKey(inventory.getId())) {
                // OK, the delivered inventory is already removed from the final result,
                // let's consume the whole quantity
                inventoryServiceRestemplateClient.consumeMaterialForWorkOrderLine(
                        workOrderLineMap.getOrDefault(inventory.getItem().getId(), null),
                        workOrder.getWarehouseId(),
                        inventory.getQuantity(),
                        productionLine.getInboundStageLocationId(),
                        inventory.getId(), "", null
                );
            }
            // OK, the delivered inventory is still in the  final result,
            // let's check if the quantity is changed
            // we will only handle if the final quantity is less than
            // the original quantity.
            else if (inventory.getQuantity() >
                        existingRetunableMaterialMap.get(inventory.getId())
            ){
                // OK, the quantity changed, let's adjust the quantity
                inventoryServiceRestemplateClient.consumeMaterialForWorkOrderLine(
                        workOrderLineMap.getOrDefault(inventory.getItem().getId(), null),
                        workOrder.getWarehouseId(),
                        inventory.getQuantity() - existingRetunableMaterialMap.get(inventory.getId()),
                        productionLine.getInboundStageLocationId(),
                        inventory.getId(), "", null
                );
            }
        }

    }

    @Transactional
    public ProductionLineAssignment deassignWorkOrderToProductionLines(
            Long workOrderId, Long productionLineId, List<Inventory> returnableMaterial) {

        logger.debug("Start to deassign work order {} from production line {}",
                workOrderId, productionLineId);
        WorkOrder workOrder = workOrderService.findById(workOrderId, false);
        ProductionLineAssignment productionLineAssignment =
                workOrder.getProductionLineAssignments().stream().filter(
                        existingProductionLineAssignment -> existingProductionLineAssignment.getProductionLine().getId().equals(productionLineId)
                ).findFirst()
                        .orElseThrow(() ->
                                WorkOrderException.raiseException(
                                        "cannot deassign production line id " + productionLineId + " from work order, THe production line doesn't exist"));


        logger.debug("We found the production line assigned! work order: {}, production line {}",
                productionLineAssignment.getWorkOrder().getNumber(),
                productionLineAssignment.getProductionLine().getName());

        // remove the qc sample, if there's any
        workOrderQCSampleService.removeQCSamples(productionLineAssignment);

        // remove the produdction line assignment
        productionLineAssignment.setDeassignedTime(ZonedDateTime.now(ZoneOffset.UTC));
        productionLineAssignment.setDeassigned(true);
        productionLineAssignment = saveOrUpdate(productionLineAssignment);

        // delete(productionLineAssignment);
        logger.debug("production line assignment removed, let's start to process the material");

        processReturnableMaterial(
                workOrder, productionLineAssignment.getProductionLine(), returnableMaterial);

        // we will cancell all the existsing picks that will come into this production line
        logger.debug("Start to cancel pick that will go into the production line {}",
                productionLineAssignment.getProductionLine().getName());
        cancelPicks(workOrder, productionLineAssignment.getProductionLine());


        return productionLineAssignment;

    }

    private void cancelPicks(WorkOrder workOrder, ProductionLine productionLine) {
        // get all the open picks and cancel them
        List<Pick> openPicks =
                outboundServiceRestemplateClient.getWorkOrderPicks(workOrder)
                        .stream()
                        // we will only cancel open pick
                .filter(pick -> pick.getQuantity() > pick.getPickedQuantity())
                        // we will only cancel the picks that go into this production line
                        .filter(pick ->
                                Objects.equals(productionLine.getInboundStageLocationId(), pick.getDestinationLocationId()))
                .collect(Collectors.toList());

        openPicks.forEach(
                pick -> {
                    logger.debug("Will cancel pick {} ", pick.getNumber());
                    outboundServiceRestemplateClient.cancelPick(pick.getId());
                }
        );


    }


    public ReportHistory generateProductionLineAssignmentLabel(
            Long productionLineAssignmentId, String locale, String printerName) throws JsonProcessingException {
        ProductionLineAssignment productionLineAssignment =
                findById(productionLineAssignmentId);

        Report reportData = new Report();
        setupProductionLineAssignmentLabelData(
                reportData, productionLineAssignment
        );
        logger.debug("will call resource service to print the label with locale: {}",
                locale);
        logger.debug("####   Production Line Assignment Label   Data  ######");
        logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        productionLineAssignment.getWorkOrder().getWarehouseId(),
                        ReportType.PRODUCTION_LINE_ASSIGNMENT_LABEL,
                        reportData, locale,
                        printerName
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;
    }

    public ReportHistory generateProductionLineAssignmentReport(
            Long productionLineAssignmentId, String locale, String printerName) throws JsonProcessingException {
        ProductionLineAssignment productionLineAssignment =
                findById(productionLineAssignmentId);

        Report reportData = new Report();
        setupProductionLineAssignmentReportData(
                reportData, productionLineAssignment
        );
        logger.debug("will call resource service to print the report with locale: {}",
                locale);
        // logger.debug("####   Report   Data  ######");
        // logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        productionLineAssignment.getWorkOrder().getWarehouseId(),
                        ReportType.PRODUCTION_LINE_ASSIGNMENT_REPORT,
                        reportData, locale, printerName
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;
    }

    private void setupProductionLineAssignmentReportData(
            Report reportData, ProductionLineAssignment productionLineAssignment) {


        ProductionLineAssignmentReportData productionLineAssignmentReportData =
                new ProductionLineAssignmentReportData();

        if (Objects.nonNull(productionLineAssignment.getWorkOrder().getItem())) {
            productionLineAssignmentReportData.setItemName(
                    productionLineAssignment.getWorkOrder().getItem().getName()
            );
        }
        else {

            productionLineAssignmentReportData.setItemName(
                    inventoryServiceRestemplateClient.getItemById(
                            productionLineAssignment.getWorkOrder().getItemId()
                    ).getName()
            );


        }

        productionLineAssignmentReportData.setProductionLineName(
                productionLineAssignment.getProductionLine().getName()
        );
        productionLineAssignmentReportData.setWorkOrderNumber(
                productionLineAssignment.getWorkOrder().getNumber()
        );
        productionLineAssignmentReportData.setQuantity(productionLineAssignment.getQuantity());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        productionLineAssignmentReportData.setStartTime(
                Objects.isNull(productionLineAssignment.getStartTime()) ?
                        "" :
                        productionLineAssignment.getStartTime().format(formatter));
        productionLineAssignmentReportData.setEndTime(
                Objects.isNull(productionLineAssignment.getEndTime()) ?
                        "" : productionLineAssignment.getEndTime().format(formatter));

        productionLineAssignmentReportData.setProductionLineCapacity(getProductionLineDailyTargetOutput(productionLineAssignment));


        reportData.setData(Collections.singleton(productionLineAssignmentReportData));
    }

    private void setupProductionLineAssignmentLabelData(
            Report reportData, ProductionLineAssignment productionLineAssignment) {


        Map<String, Object> parameters = new HashMap<>();
        parameters.put("lpn", "LPN-TEST-001");
        reportData.setParameters(parameters);
    }


    /**
     * Get daily production line target output
     */
    private Long getProductionLineDailyTargetOutput(ProductionLineAssignment productionLineAssignment) {
        // Get the total quantity from the transactions
        ProductionLineCapacity productionLineCapacity =
                productionLineCapacityService.findByProductionLineAndItem(
                        productionLineAssignment.getWorkOrder().getWarehouseId(),
                        productionLineAssignment.getProductionLine().getId(),
                        productionLineAssignment.getWorkOrder().getItemId(),
                        false
                );
        if (Objects.nonNull(productionLineCapacity)) {
            return productionLineCapacity.getCapacity();
        }
        else {
            return 0L;
        }
    }

    /**
     * Get all production line assignment that was assigned between start time and end time
     * @param warehouseId
     * @param startTime
     * @param endTime
     * @return
     */
    public List<ProductionLineAssignment> getProductionAssignmentByTimeRange(Long warehouseId,
                                                                              ZonedDateTime startTime,
                                                                              ZonedDateTime endTime,
                                                                             Boolean loadDetails) {
        logger.debug("start to get production line assignment between [{}, {}] for warehouse {}",
                startTime, endTime, warehouseId);
        List<ProductionLineAssignment> productionLineAssignments =
                productionLineAssignmentRepository.getProductionAssignmentByTimeRange(
                        warehouseId, startTime, endTime
                );
        if (loadDetails) {
            loadAttribute(productionLineAssignments);
        }
        return productionLineAssignments;
    }

    public List<ProductionLineAssignment> getProductionAssignmentByTimeRange(Long warehouseId,
                                                                             ZonedDateTime startTime,
                                                                             ZonedDateTime endTime) {
        return getProductionAssignmentByTimeRange(
                warehouseId, startTime, endTime, true
        );
    }
}
