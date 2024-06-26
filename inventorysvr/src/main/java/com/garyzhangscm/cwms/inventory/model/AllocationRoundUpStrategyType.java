package com.garyzhangscm.cwms.inventory.model;

public enum AllocationRoundUpStrategyType {
    NONE,  // ROUND up is not allowed
    BY_PERCENTAGE,   // Round up by percent
    BY_QUANTITY,    // Round up by quantity
    NO_LIMIT   // No limit on round up. can be round up to next suitable level(whole UOM or whole LPN)
}
