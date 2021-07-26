package com.garyzhangscm.cwms.workorder.model;

public class ProductionLineKanbanData {

    private String productionLineName;
    private String workOrderNumber;
    private String productionLineModel;

    private Long productionLineTargetOutput;
    private Long productionLineActualOutput;

    private Long productionLineTotalTargetOutput;
    private Long productionLineTotalActualOutput;

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

    public Long getProductionLineTargetOutput() {
        return productionLineTargetOutput;
    }

    public void setProductionLineTargetOutput(Long productionLineTargetOutput) {
        this.productionLineTargetOutput = productionLineTargetOutput;
    }

    public Long getProductionLineActualOutput() {
        return productionLineActualOutput;
    }

    public void setProductionLineActualOutput(Long productionLineActualOutput) {
        this.productionLineActualOutput = productionLineActualOutput;
    }

    public Long getProductionLineTotalTargetOutput() {
        return productionLineTotalTargetOutput;
    }

    public void setProductionLineTotalTargetOutput(Long productionLineTotalTargetOutput) {
        this.productionLineTotalTargetOutput = productionLineTotalTargetOutput;
    }

    public Long getProductionLineTotalActualOutput() {
        return productionLineTotalActualOutput;
    }

    public void setProductionLineTotalActualOutput(Long productionLineTotalActualOutput) {
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
