package com.garyzhangscm.cwms.integration.model;

public enum InventoryQuantityChangeType {

    RECEIVING(true),
    REVERSE_RECEIVING(true),
    PRODUCING(true),
    PRODUCING_BY_PRODUCT(true),
    RETURN_MATERAIL(true),
    CONSUME_MATERIAL(true),
    INVENTORY_ADJUST(false),
    CYCLE_COUNT(false),
    AUDIT_COUNT(false),
    INVENTORY_UPLOAD(true),   // upload CSV file to create inventory, normally used when we first go live
    UNKNOWN(false);

    private boolean noApprovalNeeded;

    InventoryQuantityChangeType(boolean noApprovalNeeded) {
        this.noApprovalNeeded = noApprovalNeeded;
    }

    public boolean isNoApprovalNeeded() {
        return noApprovalNeeded;
    }
}
