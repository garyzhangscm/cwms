package com.garyzhangscm.cwms.workorder.model;



public class BillOfMaterialCSVWrapper {

    private String number;

    private String warehouse;
    private String company;

    private String item;

    private String client;

    private Double expectedQuantity;


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

    public Double getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Double expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }
}
