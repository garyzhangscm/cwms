package com.garyzhangscm.cwms.outbound.model;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AllocationRequest {

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


    public AllocationRequest(WorkOrder workOrder, WorkOrderLine workOrderLine,
                             ProductionLineAssignment productionLineAssignment) {

        this.workOrder = workOrder;
        this.item = workOrderLine.getItem();
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
        // since we may have multiple product lines assigned to the work order, we will distribute
        // the allocate quantity across the production lines as well

        this.quantity = (workOrderLine.getOpenQuantity() * productionLineAssignment.getQuantity()) / workOrder.getExpectedQuantity();

    }

    public AllocationRequest(WorkOrder workOrder, WorkOrderLine workOrderLine,
                             ProductionLineAssignment productionLineAssignment,
                             Long allocatingWorkOrderQuantity) {
        this.workOrder = workOrder;
        this.item = workOrderLine.getItem();
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
        // since we may have multiple product lines assigned to the work order, we will distribute
        // the allocate quantity across the production lines as well
        // if the user doesn't specify the quantity to be allocated on the work order, we will
        // try to allocate the whole work order quantity
        if (Objects.isNull(allocatingWorkOrderQuantity)) {
            allocatingWorkOrderQuantity = workOrderLine.getOpenQuantity();
        }
        this.quantity = (allocatingWorkOrderQuantity * productionLineAssignment.getQuantity()) / workOrder.getExpectedQuantity();
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
}
