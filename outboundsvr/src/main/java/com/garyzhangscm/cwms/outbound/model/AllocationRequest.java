package com.garyzhangscm.cwms.outbound.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.service.AllocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AllocationRequest {


    private static final Logger logger = LoggerFactory.getLogger(AllocationRequest.class);

    private Item item;

    private Warehouse warehouse;

    private InventoryStatus inventoryStatus;

    Long quantity = 0L;

    private List<ShipmentLine> shipmentLines = new ArrayList<>();

    private WorkOrder workOrder;

    private List<WorkOrderLine> workOrderLines = new ArrayList<>();


    private List<AllocationStrategyType> allocationStrategyTypes = new ArrayList<>();

    private Location destinationLocation;


    private Long destinationLocationId;

    private boolean manualAllocation = false;

    // will be setup if the allocation request is a
    // manual allocation for a specific LPN
    private String lpn = "";

    public AllocationRequest() {}

    public AllocationRequest(Item item,
                             InventoryStatus inventoryStatus,
                             Warehouse warehouse,
                             List<ShipmentLine> shipmentLines,
                             List<WorkOrderLine> workOrderLines,
                             List<AllocationStrategyType> allocationStrategyTypes,
                             Long quantity) {
        this.item = item;
        this.inventoryStatus = inventoryStatus;
        this.warehouse = warehouse;
        this.shipmentLines = shipmentLines;
        this.workOrderLines = workOrderLines;
        this.allocationStrategyTypes = allocationStrategyTypes;
        this.quantity = quantity;

    }

    public AllocationRequest(ShipmentLine shipmentLine) {
        this.item = shipmentLine.getOrderLine().getItem();
        this.warehouse = shipmentLine.getWarehouse();
        this.shipmentLines = Collections.singletonList(shipmentLine);
        this.inventoryStatus = shipmentLine.getOrderLine().getInventoryStatus();
        this.workOrderLines = new ArrayList<>();
        this.allocationStrategyTypes = Collections.singletonList(shipmentLine.getOrderLine().getAllocationStrategyType());
        this.quantity = shipmentLine.getOpenQuantity();
    }


    public AllocationRequest(WorkOrder workOrder, WorkOrderLine workOrderLine, Item item,
                             ProductionLineAssignment productionLineAssignment,
                             Long allocatingWorkOrderQuantity,
                             Long allocatingWorkingOrderLineQuantity,
                             String lpn) {
        this(workOrder, workOrderLine, item, productionLineAssignment,
                allocatingWorkOrderQuantity, allocatingWorkingOrderLineQuantity);
        this.lpn = lpn;
    }

    public AllocationRequest(WorkOrder workOrder, WorkOrderLine workOrderLine, Item item,
                             ProductionLineAssignment productionLineAssignment,
                             Long allocatingWorkOrderQuantity,
                             Long allocatingWorkingOrderLineQuantity) {
        this.workOrder = workOrder;
        this.item = Objects.isNull(item) ? workOrderLine.getItem() : item;
        this.warehouse = workOrderLine.getWarehouse();
        this.shipmentLines =  new ArrayList<>();
        this.workOrderLines =Collections.singletonList(workOrderLine);
        this.inventoryStatus = workOrderLine.getInventoryStatus();
        this.allocationStrategyTypes = Collections.singletonList(workOrderLine.getAllocationStrategyType());
        this.destinationLocation = productionLineAssignment.getProductionLine().getInboundStageLocation();
        // get the destination location id
        this.destinationLocationId =
                Objects.nonNull(productionLineAssignment.getProductionLine().getInboundStageLocationId()) ?
                        productionLineAssignment.getProductionLine().getInboundStageLocationId() :
                        Objects.nonNull(productionLineAssignment.getProductionLine().getInboundStageLocation()) ?
                                productionLineAssignment.getProductionLine().getInboundStageLocation().getId() :
                                null;
        // if the user specify the work order line quantity, assign it to the allocation
        // request
        if (Objects.nonNull(allocatingWorkingOrderLineQuantity) &&
            allocatingWorkingOrderLineQuantity > 0) {

            logger.debug("We already have the request line quantity: {}",
                    allocatingWorkingOrderLineQuantity);
            this.quantity = allocatingWorkingOrderLineQuantity;
        }
        else {
            logger.debug("The user didn't speicify the quantity on the work order line allocating request, will calculate from the finish goods' quantity");
            // since we may have multiple product lines assigned to the work order, we will distribute
            // the allocate quantity across the production lines as well
            // if the user doesn't specify the quantity to be allocated on the work order, we will
            // try to allocate the whole work order quantity
            if (Objects.isNull(allocatingWorkOrderQuantity)) {
                allocatingWorkOrderQuantity = productionLineAssignment.getOpenQuantity();
            }
            logger.debug("Will allocate {} from the work order {}, production line {}",
                    allocatingWorkOrderQuantity, workOrder.getNumber(), productionLineAssignment.getProductionLine().getName());
            logger.debug("We are suppose to produce {} of item {} out of row material {} of {}, according to the work order",
                    workOrder.getExpectedQuantity(),
                    workOrder.getItem().getName(),
                    workOrderLine.getExpectedQuantity(),
                    workOrderLine.getItem().getName());

            this.quantity = (allocatingWorkOrderQuantity * workOrderLine.getExpectedQuantity()) / workOrder.getExpectedQuantity() ;
            logger.debug("so in order to produce {} of item {}, we will need row material {} of raw material {}",
                    allocatingWorkOrderQuantity,
                    workOrder.getItem().getName(),
                    this.quantity,
                    workOrderLine.getItem().getName());
        }

    }


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public List<ShipmentLine> getShipmentLines() {
        return shipmentLines;
    }

    public void setShipmentLines(List<ShipmentLine> shipmentLines) {
        this.shipmentLines = shipmentLines;
    }

    public List<WorkOrderLine> getWorkOrderLines() {
        return workOrderLines;
    }

    public void setWorkOrderLines(List<WorkOrderLine> workOrderLines) {
        this.workOrderLines = workOrderLines;
    }

    public List<AllocationStrategyType> getAllocationStrategyTypes() {
        return allocationStrategyTypes;
    }

    public void setAllocationStrategyTypes(List<AllocationStrategyType> allocationStrategyTypes) {
        this.allocationStrategyTypes = allocationStrategyTypes;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(Location destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public Long getDestinationLocationId() {
        return destinationLocationId;
    }

    public void setDestinationLocationId(Long destinationLocationId) {
        this.destinationLocationId = destinationLocationId;
    }

    public boolean isManualAllocation() {
        return manualAllocation;
    }

    public void setManualAllocation(boolean manualAllocation) {
        this.manualAllocation = manualAllocation;
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }
}
