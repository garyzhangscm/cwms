package com.garyzhangscm.cwms.workorder.model;

public class ProductionLineKanbanData {

    private String productionLineName;
    private String workOrderNumber;
    private String productionLineModel;

    private Double productionLineTargetOutput;
    private Double productionLineActualOutput;

    private Double productionLineTotalTargetOutput;
    private Double productionLineTotalActualOutput;

    private WorkOrderStatus workOrderStatus;

    private String shift;


    public String getProductionLineName() {
        return productionLineName;
    }

    public void setProductionLineName(String productionLineName) {
        this.productionLineName = productionLineName;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public String getProductionLineModel() {
        return productionLineModel;
    }

    public void setProductionLineModel(String productionLineModel) {
        this.productionLineModel = productionLineModel;
    }

    public Double getProductionLineTargetOutput() {
        return productionLineTargetOutput;
    }

    public void setProductionLineTargetOutput(Double productionLineTargetOutput) {
        this.productionLineTargetOutput = productionLineTargetOutput;
    }

    public Double getProductionLineActualOutput() {
        return productionLineActualOutput;
    }

    public void setProductionLineActualOutput(Double productionLineActualOutput) {
        this.productionLineActualOutput = productionLineActualOutput;
    }

    public Double getProductionLineTotalTargetOutput() {
        return productionLineTotalTargetOutput;
    }

    public void setProductionLineTotalTargetOutput(Double productionLineTotalTargetOutput) {
        this.productionLineTotalTargetOutput = productionLineTotalTargetOutput;
    }

    public Double getProductionLineTotalActualOutput() {
        return productionLineTotalActualOutput;
    }

    public void setProductionLineTotalActualOutput(Double productionLineTotalActualOutput) {
        this.productionLineTotalActualOutput = productionLineTotalActualOutput;
    }

    public WorkOrderStatus getWorkOrderStatus() {
        return workOrderStatus;
    }

    public void setWorkOrderStatus(WorkOrderStatus workOrderStatus) {
        this.workOrderStatus = workOrderStatus;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }
}
