package com.garyzhangscm.cwms.inventory.model;

// Define the timing when we can consume the
// material
public enum WorkOrderMaterialConsumeTiming {
    WHEN_DELIVER,
    BY_TRANSACTION,
    WHEN_CLOSE,
}
