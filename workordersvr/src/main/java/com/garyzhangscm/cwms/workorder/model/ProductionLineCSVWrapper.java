package com.garyzhangscm.cwms.workorder.model;

public class ProductionLineCSVWrapper {


    private String warehouse;
    private String company;

    private String name;

    private String inboundStageLocation;

    private String outboundStageLocation;


    private String productionLineLocation;


    private Boolean workOrderExclusiveFlag;

    private Boolean enabled;
    private Boolean genericPurpose;

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInboundStageLocation() {
        return inboundStageLocation;
    }

    public void setInboundStageLocation(String inboundStageLocation) {
        this.inboundStageLocation = inboundStageLocation;
    }

    public String getOutboundStageLocation() {
        return outboundStageLocation;
    }

    public void setOutboundStageLocation(String outboundStageLocation) {
        this.outboundStageLocation = outboundStageLocation;
    }

    public String getProductionLineLocation() {
        return productionLineLocation;
    }

    public void setProductionLineLocation(String productionLineLocation) {
        this.productionLineLocation = productionLineLocation;
    }

    public Boolean getWorkOrderExclusiveFlag() {
        return workOrderExclusiveFlag;
    }

    public void setWorkOrderExclusiveFlag(Boolean workOrderExclusiveFlag) {
        this.workOrderExclusiveFlag = workOrderExclusiveFlag;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getGenericPurpose() {
        return genericPurpose;
    }

    public void setGenericPurpose(Boolean genericPurpose) {
        this.genericPurpose = genericPurpose;
    }
}
