package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.model.AllocationStrategyType;
import com.garyzhangscm.cwms.outbound.model.InventorySummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class AllocateByPickSequenceAllocationStrategy extends DefaultAllocationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AllocationService.class);

    @Autowired
    protected PickService pickService;

    @Autowired
    protected InventorySummaryService inventorySummaryService;

    @Autowired
    protected ShortAllocationService shortAllocationService;

    @Autowired
    protected InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    @Override
    public AllocationStrategyType getType() {
        return AllocationStrategyType.BY_PICK_SEQUENCE;
    }

    @Override
    protected List<InventorySummary> sort(List<InventorySummary> inventorySummaries) {

        // by default, we do nothing
        Collections.sort(inventorySummaries, Comparator.comparing(a -> a.getLocation().getPickSequence()));
        return inventorySummaries;

    }
    @Override
    public boolean isDefault() {
        return false;
    }

}
