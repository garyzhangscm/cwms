package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryConsolidationService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryConsolidationService.class);
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private InventoryActivityService inventoryActivityService;

    /**
     * Consolidate inventory at a location, this method should be called after the inventory
     * being moved into the location.
     * @param location : The location of inventories to be consoldiate
     * @param inventory: The inventory to be consolidated into existing inventory in the location
     */
    public Inventory consolidateInventoryAtLocation(Location location, Inventory inventory) {


        logger.debug("Check if we will need to consolidate inventory at location: {}", location.getName());
        // Let's get all the inventory that already in the location
        List<Inventory> inventories = inventoryService.findByLocationId(location.getId(), false);

        logger.debug(">> we found {} existing inventory record in the location, included the current moved one", inventories.size());
        Inventory consolidatedInventory = getInventoryConsolidationHandler(location).consolidate(inventory, inventories);
        logInventoryActivity(location, inventory, consolidatedInventory);
        return consolidatedInventory;
    }

    private void logInventoryActivity(Location location, Inventory inventory, Inventory consolidatedInventory) {
        if (!consolidatedInventory.equals(inventory)) {
            // we made a consolidation, let's log an activitity record
            InventoryConsolidationStrategy inventoryConsolidationStrategy =
                    warehouseLayoutServiceRestemplateClient.getInventoryConsolidationStrategy(location.getLocationGroup());
            switch (inventoryConsolidationStrategy) {
                case CONSOLIDATE_BY_INVENTORY:
                    inventoryActivityService.logInventoryActivitiy(
                            inventory, InventoryActivityType.INVENTORY_CONSOLIDATION,
                            "Inventory ID / LPN / Quantity",
                            String.valueOf(inventory.getId()) + " / " + inventory.getLpn() + " / " + inventory.getQuantity(),
                            String.valueOf(consolidatedInventory.getId() + " / " + consolidatedInventory.getLpn() + " / " + consolidatedInventory.getQuantity())
                    );
                    break;
                default:
                    inventoryActivityService.logInventoryActivitiy(
                            inventory, InventoryActivityType.INVENTORY_CONSOLIDATION,
                            "LPN", inventory.getLpn(), consolidatedInventory.getLpn()
                    );
                    break;
            }
        }
    }
    private InventoryConsolidationHandler getInventoryConsolidationHandler(Location location) {

        InventoryConsolidationStrategy inventoryConsolidationStrategy =
                warehouseLayoutServiceRestemplateClient.getInventoryConsolidationStrategy(location.getLocationGroup());
        switch (inventoryConsolidationStrategy) {
            case CONSOLIDATE_BY_LPN:
                return new InventoryConsolidationByLPN();
            case CONSOLIDATE_BY_INVENTORY:
                return new InventoryConsolidationByInventory();
            default:
                return new InventoryConsolidationDefault();
        }

    }
}
