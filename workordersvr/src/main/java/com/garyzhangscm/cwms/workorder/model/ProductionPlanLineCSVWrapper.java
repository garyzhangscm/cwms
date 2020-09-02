package com.garyzhangscm.cwms.workorder.model;

public class ProductionPlanLineCSVWrapper {

    private String warehouse;

    private String productionPlan;

    private String item;


    private String billOfMaterial;


    private Long expectedQuantity;

    private String inventoryStatus;

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getProductionPlan() {
        return productionPlan;
    }

    public void setProductionPlan(String productionPlan) {
        this.productionPlan = productionPlan;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getBillOfMaterial() {
        return billOfMaterial;
    }

    public void setBillOfMaterial(String billOfMaterial) {
        this.billOfMaterial = billOfMaterial;
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
}
