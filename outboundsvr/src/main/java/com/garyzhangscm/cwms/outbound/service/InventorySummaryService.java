package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.model.Inventory;
import com.garyzhangscm.cwms.outbound.model.InventorySummary;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InventorySummaryService {

    private static final Logger logger = LoggerFactory.getLogger(InventorySummaryService.class);

    public List<InventorySummary> getInventorySummaryForAllocation(List<Inventory> inventories) {
        Map<String, InventorySummary> inventorySummaryMap = new HashMap<>();

        inventories.forEach(inventory -> {
            String inventorySummaryKey = getInventorySummaryKey(inventory);
            InventorySummary inventorySummary;
            if (inventorySummaryMap.containsKey(inventorySummaryKey)) {
                inventorySummary = inventorySummaryMap.get(inventorySummaryKey);
                inventorySummary.setQuantity(inventorySummary.getQuantity() + inventory.getQuantity());
                inventorySummary.resetFIFODate(inventory.getCreatedTime());
                inventorySummary.addInventory(inventory);
            }
            else {
                inventorySummary = new InventorySummary(inventory);
                logger.debug("Add inventory to the inventory summary. " +
                        "Inventory's create datetime: {}, inventory summary's FIFO: {}",
                        inventory.getCreatedTime(), inventorySummary.getFifoDate());
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
                // .append(inventory.getItemPackageType().getId())
                //.append("-")
                .append(inventory.getColor())
                .append("-")
                .append(inventory.getProductSize())
                .append("-")
                .append(inventory.getStyle())
                .append("-")
                .append(Objects.isNull(inventory.getReceipt()) ? "" : inventory.getReceipt().getNumber()) ;
        return inventorySummaryKey.toString();
    }
}
