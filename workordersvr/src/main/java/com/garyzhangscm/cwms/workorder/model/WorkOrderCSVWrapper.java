package com.garyzhangscm.cwms.workorder.model;


public class WorkOrderCSVWrapper {

    private String warehouse;
    private String company;

    private String number;

    private String item;


    private Long expectedQuantity;

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

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

}
