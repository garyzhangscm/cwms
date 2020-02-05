package com.garyzhangscm.cwms.workorder.model;

public class ProductionLineCSVWrapper {


    private String warehouse;

    private String name;

    private String inboundStageLocation;

    private String outboundStageLocation;


    private String productionLineLocation;


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
}
