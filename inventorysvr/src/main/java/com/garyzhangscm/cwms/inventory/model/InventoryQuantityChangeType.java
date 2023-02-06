package com.garyzhangscm.cwms.inventory.model;

public enum InventoryQuantityChangeType {

    RECEIVING(true, false),
    REVERSE_RECEIVING(true, false),
    PRODUCING(true, false),
    PRODUCING_BY_PRODUCT(true, false),
    RETURN_MATERAIL(true, false),
    CONSUME_MATERIAL(true, false),
    INVENTORY_ADJUST(false, true),
    CYCLE_COUNT(false, true),
    AUDIT_COUNT(false, true),
    REVERSE_PRODUCTION(true, false),
    REVERSE_BY_PRODUCT(true, false),
    INVENTORY_UPLOAD(true, true),   // upload CSV file to create inventory, normally used when we first go live
    UNKNOWN(false, true);

    private boolean noApprovalNeeded;
    private boolean triggerInventoryAdjustmentIntegration;

    InventoryQuantityChangeType(boolean noApprovalNeeded, boolean triggerInventoryAdjustmentIntegration) {
        this.noApprovalNeeded = noApprovalNeeded;
        this.triggerInventoryAdjustmentIntegration = triggerInventoryAdjustmentIntegration;
    }

    public boolean isNoApprovalNeeded() {
        return noApprovalNeeded;
    }
    public boolean triggerInventoryAdjustmentIntegration() {
        return triggerInventoryAdjustmentIntegration;
    }
}
