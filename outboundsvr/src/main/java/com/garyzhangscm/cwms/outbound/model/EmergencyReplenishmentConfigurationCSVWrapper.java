package com.garyzhangscm.cwms.outbound.model;

public class EmergencyReplenishmentConfigurationCSVWrapper {


    private Integer sequence;

    private String unitOfMeasure;

    private String item;

    private String itemFamily;

    private String client;


    private String sourceLocation;
    private String sourceLocationGroup;
    private String destinationLocation;
    private String destinationLocationGroup;
    private String warehouse;
    private String company;

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getItemFamily() {
        return itemFamily;
    }

    public void setItemFamily(String itemFamily) {
        this.itemFamily = itemFamily;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(String sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public String getSourceLocationGroup() {
        return sourceLocationGroup;
    }

    public void setSourceLocationGroup(String sourceLocationGroup) {
        this.sourceLocationGroup = sourceLocationGroup;
    }

    public String getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(String destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public String getDestinationLocationGroup() {
        return destinationLocationGroup;
    }

    public void setDestinationLocationGroup(String destinationLocationGroup) {
        this.destinationLocationGroup = destinationLocationGroup;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }
}
