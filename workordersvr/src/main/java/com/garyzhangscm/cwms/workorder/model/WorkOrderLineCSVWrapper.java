package com.garyzhangscm.cwms.workorder.model;

public class WorkOrderLineCSVWrapper {

    private String warehouse;

    private String company;
    private String workOrder;

    private String number;


    private String item;

    private Long expectedQuantity;

    private String inventoryStatus;
    private String allocationStrategyType;

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(String workOrder) {
        this.workOrder = workOrder;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(String inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public String getAllocationStrategyType() {
        return allocationStrategyType;
    }

    public void setAllocationStrategyType(String allocationStrategyType) {
        this.allocationStrategyType = allocationStrategyType;
    }
}
