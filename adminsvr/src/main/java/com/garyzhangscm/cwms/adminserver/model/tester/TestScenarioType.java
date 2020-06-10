package com.garyzhangscm.cwms.adminserver.model.tester;

public enum TestScenarioType {
    CREATE_LOCATION("Test Creating Location"),
    INTEGRATION_DOWNLOAD_ITEM("Test Integration - Download Item"),
    CREATE_INVENTORY_ADJUSTMENT_THRESHOLD("Test Creating Inventory Adjustment Threshold"),
    INVENTORY_ADJUST("Test inventory adjust up and down"),
    CYCLE_AUDIT_COUNT("Test cycle count and audit count");

    private String description;

    private TestScenarioType(String description) {
        this.description = description;
    }

    public String getDescription(){
        return description;
    }
}
