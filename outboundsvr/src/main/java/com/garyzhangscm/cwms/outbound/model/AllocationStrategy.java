package com.garyzhangscm.cwms.outbound.model;

import org.springframework.stereotype.Service;

@Service
public interface AllocationStrategy {

    public AllocationStrategyType getType();
    public AllocationResult allocate(AllocationRequest allocationRequest);
    public AllocationResult allocate(AllocationRequest allocationRequest, Location sourceLocation);

    public boolean isDefault();
}
