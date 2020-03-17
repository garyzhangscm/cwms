package com.garyzhangscm.cwms.inventory.model;

import com.garyzhangscm.cwms.inventory.service.InventoryService;

import java.util.List;

public class InventoryConsolidationDefault implements InventoryConsolidationHandler {

    /**
     * Consolidate the inventory into inventories by specific strategy
     * @param inventory Source inventory.
     * @param inventories: destination inventory
     * @return new source inventory to be persist
     */
    public Inventory consolidate(Inventory inventory, List<Inventory> inventories){

        // Default handler.
        // Do nothing
        return inventory;
    }
}
