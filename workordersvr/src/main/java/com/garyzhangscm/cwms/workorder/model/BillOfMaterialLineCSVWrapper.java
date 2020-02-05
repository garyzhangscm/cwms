package com.garyzhangscm.cwms.workorder.model;

public class BillOfMaterialLineCSVWrapper {

    private String number;

    private String billOfMaterial;

    private String warehouse;

    private Long expectedQuantity;

    private String item;

    private String inventoryStatus;


    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
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

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(String inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }
}
