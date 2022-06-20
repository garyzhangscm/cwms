package com.garyzhangscm.cwms.outbound.model;

import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public interface AllocationStrategy {

    public AllocationStrategyType getType();

    @Transactional
    public AllocationResult allocate(AllocationRequest allocationRequest);

    @Transactional
    public AllocationResult allocate(AllocationRequest allocationRequest, Location sourceLocation);

    public boolean isDefault();
}
