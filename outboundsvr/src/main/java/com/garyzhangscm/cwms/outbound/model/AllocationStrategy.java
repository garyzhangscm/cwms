package com.garyzhangscm.cwms.outbound.model;

public interface AllocationStrategy {

    public AllocationStrategyType getType();
    public AllocationResult allocate(AllocationRequest allocationRequest);
}
