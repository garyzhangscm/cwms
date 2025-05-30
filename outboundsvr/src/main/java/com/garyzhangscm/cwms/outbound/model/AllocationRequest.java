package com.garyzhangscm.cwms.outbound.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.service.AllocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import java.util.*;

public class AllocationRequest {


    private static final Logger logger = LoggerFactory.getLogger(AllocationRequest.class);

    private Item item;

    private Warehouse warehouse;

    private InventoryStatus inventoryStatus;

    private Long inventoryStatusId;

    Long quantity = 0L;

    private List<ShipmentLine> shipmentLines = new ArrayList<>();

    private WorkOrder workOrder;

    private List<WorkOrderLine> workOrderLines = new ArrayList<>();


    private List<AllocationStrategyType> allocationStrategyTypes = new ArrayList<>();

    private Location destinationLocation;


    private Long destinationLocationId;


    private boolean manualAllocation = false;


    private String color;
    private String productSize;
    private String style;

    private String inventoryAttribute1;
    private String inventoryAttribute2;
    private String inventoryAttribute3;
    private String inventoryAttribute4;
    private String inventoryAttribute5;

    private String allocateByReceiptNumber;

    // if the allocation needs to skip certain locations
    private Set<Long> skipLocations = new HashSet<>();


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
        this(shipmentLine, new HashSet<>());
    }

    public AllocationRequest(ShipmentLine shipmentLine,
                             Set<Long> skipLocations) {
        this.item = shipmentLine.getOrderLine().getItem();
        this.warehouse = shipmentLine.getWarehouse();
        this.shipmentLines = Collections.singletonList(shipmentLine);
        this.inventoryStatus = shipmentLine.getOrderLine().getInventoryStatus();

        this.workOrderLines = new ArrayList<>();
        this.allocationStrategyTypes = Collections.singletonList(shipmentLine.getOrderLine().getAllocationStrategyType());
        this.quantity = shipmentLine.getOpenQuantity();

        this.color = shipmentLine.getOrderLine().getColor();
        this.productSize = shipmentLine.getOrderLine().getProductSize();
        this.style = shipmentLine.getOrderLine().getStyle();

        this.inventoryAttribute1 = shipmentLine.getOrderLine().getInventoryAttribute1();
        this.inventoryAttribute2 = shipmentLine.getOrderLine().getInventoryAttribute2();
        this.inventoryAttribute3 = shipmentLine.getOrderLine().getInventoryAttribute3();
        this.inventoryAttribute4 = shipmentLine.getOrderLine().getInventoryAttribute4();
        this.inventoryAttribute5 = shipmentLine.getOrderLine().getInventoryAttribute5();

        this.allocateByReceiptNumber = shipmentLine.getOrderLine().getAllocateByReceiptNumber();
        this.skipLocations = skipLocations;
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
        this.warehouse = Objects.nonNull(workOrder.getWarehouse()) ?
                workOrder.getWarehouse() : workOrderLine.getWarehouse();

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
        if (Boolean.TRUE.equals(this.isManualAllocation())) {
            // for manual allocation, setup the
            // allocation strategy type to MANUAL_ALLOCATION
            allocationStrategyTypes = Collections.singletonList(AllocationStrategyType.MANUAL_ALLOCATION);
        }
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getProductSize() {
        return productSize;
    }

    public void setProductSize(String productSize) {
        this.productSize = productSize;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getAllocateByReceiptNumber() {
        return allocateByReceiptNumber;
    }

    public void setAllocateByReceiptNumber(String allocateByReceiptNumber) {
        this.allocateByReceiptNumber = allocateByReceiptNumber;
    }

    public String getInventoryAttribute1() {
        return inventoryAttribute1;
    }

    public void setInventoryAttribute1(String inventoryAttribute1) {
        this.inventoryAttribute1 = inventoryAttribute1;
    }

    public String getInventoryAttribute2() {
        return inventoryAttribute2;
    }

    public void setInventoryAttribute2(String inventoryAttribute2) {
        this.inventoryAttribute2 = inventoryAttribute2;
    }

    public String getInventoryAttribute3() {
        return inventoryAttribute3;
    }

    public void setInventoryAttribute3(String inventoryAttribute3) {
        this.inventoryAttribute3 = inventoryAttribute3;
    }

    public String getInventoryAttribute4() {
        return inventoryAttribute4;
    }

    public void setInventoryAttribute4(String inventoryAttribute4) {
        this.inventoryAttribute4 = inventoryAttribute4;
    }

    public String getInventoryAttribute5() {
        return inventoryAttribute5;
    }

    public void setInventoryAttribute5(String inventoryAttribute5) {
        this.inventoryAttribute5 = inventoryAttribute5;
    }

    public Set<Long> getSkipLocations() {
        return skipLocations;
    }

    public void setSkipLocations(Set<Long> skipLocations) {
        this.skipLocations = skipLocations;
    }

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }
}
