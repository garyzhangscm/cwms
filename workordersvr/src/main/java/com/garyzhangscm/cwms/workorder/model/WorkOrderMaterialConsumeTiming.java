package com.garyzhangscm.cwms.workorder.model;

// Define the timing when we can consume the
// material
public enum WorkOrderMaterialConsumeTiming {
    WHEN_DELIVER,
    BY_TRANSACTION,
    WHEN_CLOSE,
}
