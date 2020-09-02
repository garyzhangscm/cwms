package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.model.AllocationStrategy;
import com.garyzhangscm.cwms.outbound.model.AllocationStrategyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AllocationStrategyFactory {
    @Autowired
    List<AllocationStrategy> allocationStrategies;


    /**
     * Get allocation strategy by type. We should only have one bean for each type
     * @return Allocation Strategy
     */
    public Optional<AllocationStrategy> getAllocationStrategyByType(AllocationStrategyType allocationStrategyType) {
        return allocationStrategies.stream()
                .filter(allocationStrategy -> allocationStrategy.getType().equals(allocationStrategyType))
                .findFirst();
    }


    public AllocationStrategy getDefaultAllocationStrategy() {
        return new FIFOAllocationStrategy();
    }
}
