package com.garyzhangscm.cwms.integration.model;


public class WorkOrderByProductConfirmation {

    private Long itemId;
    private String itemName;

    private Long expectedQuantity;
    private Long producedQuantity;


    private Long inventoryStatusId;
    private String inventoryStatusName;


    private WorkOrderConfirmation workOrderConfirmation;

    public WorkOrderConfirmation getWorkOrderConfirmation() {
        return workOrderConfirmation;
    }

    public void setWorkOrderConfirmation(WorkOrderConfirmation workOrderConfirmation) {
        this.workOrderConfirmation = workOrderConfirmation;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public Long getProducedQuantity() {
        return producedQuantity;
    }

    public void setProducedQuantity(Long producedQuantity) {
        this.producedQuantity = producedQuantity;
    }

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    public String getInventoryStatusName() {
        return inventoryStatusName;
    }

    public void setInventoryStatusName(String inventoryStatusName) {
        this.inventoryStatusName = inventoryStatusName;
    }
}
