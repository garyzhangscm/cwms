package com.garyzhangscm.cwms.inventory.model;

public enum InventoryQuantityChangeType {
    RECEIVING,
    PRODUCING,
    PRODUCING_BY_PRODUCT,
    RETURN_MATERAIL,
    CONSUME_MATERIAL,
    INVENTORY_ADJUST,
    CYCLE_COUNT,
    AUDIT_COUNT,
    UNKNOWN;
}
