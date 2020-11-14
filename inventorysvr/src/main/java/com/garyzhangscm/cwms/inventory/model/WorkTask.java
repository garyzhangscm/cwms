package com.garyzhangscm.cwms.inventory.model;


public class WorkTask   {

    private Long warehouseId;
    private String number;
    private WorkType workType;

    private Long sourceLocationId;

    private Long destinationLocationId;

    private Long inventoryId;

    public WorkTask(){}
    public WorkTask(Long warehouseId,
                    String number,
                    WorkType workType,
                    Long sourceLocationId,
                    Long destinationLocationId,
                    Long inventoryId) {
        this.warehouseId = warehouseId;
        this.number = number;
        this.workType = workType;
        this.sourceLocationId = sourceLocationId;
        this.destinationLocationId = destinationLocationId;
        this.inventoryId = inventoryId;
    }
    public static WorkTask createInventoryMovementWorkTask(Inventory inventory, Location destinationLocation) {

        return new WorkTask(
                inventory.getWarehouseId(),
                null,
                WorkType.INVENTORY_MOVEMENT,
                inventory.getLocationId(),
                destinationLocation.getId(),
                inventory.getId()

        );
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public WorkType getWorkType() {
        return workType;
    }

    public void setWorkType(WorkType workType) {
        this.workType = workType;
    }

    public Long getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(Long sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public Long getDestinationLocationId() {
        return destinationLocationId;
    }

    public void setDestinationLocationId(Long destinationLocationId) {
        this.destinationLocationId = destinationLocationId;
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }
}
