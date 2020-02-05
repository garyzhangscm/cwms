package com.garyzhangscm.cwms.workorder.model;



public class WorkOrderInstructionTemplateCSVWrapper {

    private Integer sequence;

    private String warehouse;

    private String billOfMaterial;

    private String instruction;


    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public String getBillOfMaterial() {
        return billOfMaterial;
    }

    public void setBillOfMaterial(String billOfMaterial) {
        this.billOfMaterial = billOfMaterial;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }
}
