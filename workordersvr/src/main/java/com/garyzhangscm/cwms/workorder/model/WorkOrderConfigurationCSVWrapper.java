package com.garyzhangscm.cwms.workorder.model;

public class WorkOrderConfigurationCSVWrapper extends AuditibleEntity<String>{


    private String company;
    private String warehouse;
    private String materialConsumeTiming;

    private boolean overConsumeIsAllowed;
    private boolean overProduceIsAllowed;

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

    public String getMaterialConsumeTiming() {
        return materialConsumeTiming;
    }

    public void setMaterialConsumeTiming(String materialConsumeTiming) {
        this.materialConsumeTiming = materialConsumeTiming;
    }

    public boolean isOverConsumeIsAllowed() {
        return overConsumeIsAllowed;
    }

    public void setOverConsumeIsAllowed(boolean overConsumeIsAllowed) {
        this.overConsumeIsAllowed = overConsumeIsAllowed;
    }

    public boolean isOverProduceIsAllowed() {
        return overProduceIsAllowed;
    }

    public void setOverProduceIsAllowed(boolean overProduceIsAllowed) {
        this.overProduceIsAllowed = overProduceIsAllowed;
    }
}
