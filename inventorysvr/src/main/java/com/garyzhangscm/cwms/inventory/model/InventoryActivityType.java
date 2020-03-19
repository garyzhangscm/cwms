package com.garyzhangscm.cwms.inventory.model;

public enum InventoryActivityType {
    INVENTORY_MOVEMENT,
    RECEIVING,
    REVERSE_RECEIVING,
    PICKING,
    UNPICKING,
    COUNT,
    CYCLE_COUNT,
    AUDIT_COUNT,
    INVENTORY_ADJUSTMENT,
    INVENTORY_CONSOLIDATION,
    INVENTORY_SPLIT;
}
