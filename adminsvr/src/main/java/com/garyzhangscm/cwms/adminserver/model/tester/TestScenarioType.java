package com.garyzhangscm.cwms.adminserver.model.tester;

public enum TestScenarioType {
    CREATE_LOCATION("Test Creating Location"),
    INTEGRATION_DOWNLOAD_ITEM("Test Integration - Download Item Family"),
    INTEGRATION_DOWNLOAD_ITEM_FAMILY("Test Integration - Download Item"),
    INTEGRATION_DOWNLOAD_SUPPLIER("Test Integration - Download Supplier"),
    INTEGRATION_DOWNLOAD_CLIENT("Test Integration - Download Client"),
    INTEGRATION_DOWNLOAD_CUSTOMER("Test Integration - Download Customer"),
    CREATE_INVENTORY_ADJUSTMENT_THRESHOLD("Test Creating Inventory Adjustment Threshold"),
    CREATE_PUTAWAY_CONFIGURATION("Test Creating putaway configuration"),
    CREATE_ALLOCATION_CONFIGURATION("Test Creating allocation configuration"),
    CREATE_EMERGENCY_REPLENISHMENT_CONFIGURATION("Test Creating emergency replenishment configuration"),
    INVENTORY_ADJUST("Test inventory adjust up and down"),
    CYCLE_AUDIT_COUNT("Test cycle count and audit count"),
    BASIC_RECEIVING_PUTAWAY("Basic receiving and putaway process"),
    BASIC_SALES_ORDER("Basic shipping process with Each Picking(Cartonization) / Case Picking / Pallet Picking"),
    SALES_ORDER_WITH_SHORT_ALLOCATION("Sales order with short allocation and emergency replenishment");;

    private String description;

    private TestScenarioType(String description) {
        this.description = description;
    }

    public String getDescription(){
        return description;
    }
}
