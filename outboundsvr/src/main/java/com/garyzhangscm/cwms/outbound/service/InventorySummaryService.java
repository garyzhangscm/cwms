package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.model.Inventory;
import com.garyzhangscm.cwms.outbound.model.InventorySummary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventorySummaryService {

    public List<InventorySummary> getInventorySummaryForAllocation(List<Inventory> inventories) {
        Map<String, InventorySummary> inventorySummaryMap = new HashMap<>();

        inventories.forEach(inventory -> {
            String inventorySummaryKey = getInventorySummaryKey(inventory);
            InventorySummary inventorySummary;
            if (inventorySummaryMap.containsKey(inventorySummaryKey)) {
                inventorySummary = inventorySummaryMap.get(inventorySummaryKey);
                inventorySummary.setQuantity(inventorySummary.getQuantity() + inventory.getQuantity());
            }
            else {
                inventorySummary = new InventorySummary(inventory);
            }
            inventorySummaryMap.put(inventorySummaryKey, inventorySummary);
        });

        return new ArrayList<>(inventorySummaryMap.values());
    }

    private String getInventorySummaryKey(Inventory inventory) {
        StringBuilder inventorySummaryKey = new StringBuilder();
        inventorySummaryKey.append(inventory.getLocationId())
                .append("-")
                .append(inventory.getInventoryStatus().getId())
                .append("-")
                .append(inventory.getItemPackageType().getId());
        return inventorySummaryKey.toString();
    }
}
