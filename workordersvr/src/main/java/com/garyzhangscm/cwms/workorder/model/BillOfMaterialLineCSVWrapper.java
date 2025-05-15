package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.util.Strings;

public class BillOfMaterialLineCSVWrapper {

    private String number;
    private String client;


    // BOM number
    private String billOfMaterial;
    // BOM Item number
    private String bomItem;
    private Double bomExpectedQuantity;

    private String warehouse;
    private String company;

    private Double expectedQuantity;

    // item number
    private String item;

    private String inventoryStatus;

    private Boolean createKitItem;

    public BillOfMaterialLineCSVWrapper trim() {

        number = Strings.isBlank(number) ? "" : number.trim();
        client = Strings.isBlank(client) ? "" : client.trim();
        billOfMaterial = Strings.isBlank(billOfMaterial) ? "" : billOfMaterial.trim();
        bomItem = Strings.isBlank(bomItem) ? "" : bomItem.trim();
        warehouse = Strings.isBlank(warehouse) ? "" : warehouse.trim();
        company = Strings.isBlank(company) ? "" : company.trim();
        item = Strings.isBlank(item) ? "" : item.trim();
        inventoryStatus = Strings.isBlank(inventoryStatus) ? "" : inventoryStatus.trim();


        return this;
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

    public String getBillOfMaterial() {
        return billOfMaterial;
    }

    public void setBillOfMaterial(String billOfMaterial) {
        this.billOfMaterial = billOfMaterial;
    }

    public Double getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Double expectedQuantity) {
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

    public String getBomItem() {
        return bomItem;
    }

    public void setBomItem(String bomItem) {
        this.bomItem = bomItem;
    }

    public Double getBomExpectedQuantity() {
        return bomExpectedQuantity;
    }

    public void setBomExpectedQuantity(Double bomExpectedQuantity) {
        this.bomExpectedQuantity = bomExpectedQuantity;
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

    public Boolean getCreateKitItem() {
        return createKitItem;
    }

    public void setCreateKitItem(Boolean createKitItem) {
        this.createKitItem = createKitItem;
    }
}
