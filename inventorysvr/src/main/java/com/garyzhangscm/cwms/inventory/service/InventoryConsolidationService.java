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
        List<Inventory> inventoriesInTheLocation = inventoryService.findByLocationId(location.getId(), false);

        if (inventoriesInTheLocation.isEmpty()) {
            logger.debug("there's nothing to be consolidated in the location {}", location.getName());
            return inventory;
        }

        logger.debug(">> we found {} existing inventory record in the location, included the current moved one", inventoriesInTheLocation.size());
        InventoryConsolidationHandler inventoryConsolidationHandler = getInventoryConsolidationHandler(location);
        Inventory consolidatedInventory = inventoryConsolidationHandler.consolidate(inventory, inventoriesInTheLocation);
        // if the inventory consolidation strategy is consolidate by inventory
        // but there's nothing to be consolidated in this NON empty location(probably the inventory attribute doesn't match)
        // then we will try to consolidate by LPN
        if (inventoryConsolidationHandler instanceof InventoryConsolidationByInventory &&
            consolidatedInventory.equals(inventory)) {
            // the consolidated inventory is the same as source inventory, which means there's
            // no consolidate happens yet
            logger.debug("inventory {} is not able to be consolidated in the NON empty destination {}" +
                    ", let's try consolidate by LPN",
                    inventory.getLpn(), location.getName());
            consolidatedInventory = (new InventoryConsolidationByLPN()).consolidate(inventory, inventoriesInTheLocation);

        }
        logInventoryActivity(location, inventory, consolidatedInventory);
        return consolidatedInventory;
    }

    private void logInventoryActivity(Location location, Inventory inventory, Inventory consolidatedInventory) {
        if (!consolidatedInventory.equals(inventory)) {
            // we made a consolidation, let's log an activitity record
            InventoryConsolidationStrategy inventoryConsolidationStrategy =
                            warehouseLayoutServiceRestemplateClient.getInventoryConsolidationStrategy(location.getLocationGroup().getId());

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
                    warehouseLayoutServiceRestemplateClient.getInventoryConsolidationStrategy(location.getLocationGroup().getId());
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
