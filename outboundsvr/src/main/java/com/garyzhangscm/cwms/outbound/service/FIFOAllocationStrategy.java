package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.model.*;
import com.netflix.discovery.converters.Auto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FIFOAllocationStrategy extends DefaultAllocationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AllocationService.class);
    @Autowired
    private PickService pickService;
    @Autowired
    private InventorySummaryService inventorySummaryService;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    @Override
    public AllocationStrategyType getType() {
        return AllocationStrategyType.FIRST_IN_FIRST_OUT;
    }

    @Override
    protected List<InventorySummary> sort(List<InventorySummary> inventorySummaries) {

        // by default, we do nothing
        Collections.sort(inventorySummaries, Comparator.comparing(InventorySummary::getFifoDate));
        return inventorySummaries;

    }

}
