package com.garyzhangscm.cwms.inventory.model;


import com.garyzhangscm.cwms.inventory.exception.InventoryConsolidationException;
import com.garyzhangscm.cwms.inventory.service.InventoryConsolidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class InventoryConsolidationByInventory implements InventoryConsolidationHandler {

    private static final Logger logger = LoggerFactory.getLogger(InventoryConsolidationByInventory.class);
    /**
     * Consolidate the inventory into inventories by specific strategy
     * @param inventory Source inventory.
     * @param inventories: destination inventory
     * @return new source inventory to be persist
     */
    public Inventory consolidate(Inventory inventory, List<Inventory> inventories){
        logger.debug(">> will try to consolidate by inventory");

        if (inventories.contains(inventory)){
            logger.debug(">> the inventory array already contains the current inventory, will remove it first");
            inventories.remove(inventory);
        }
        // Let's only consolidate into the inventory with same attribute
        inventories = inventories.stream()
                .filter(destinationInventory -> canConsolidate(inventory, destinationInventory))
                .collect(Collectors.toList());
        // make sure we will only have one destination inventory
        if (inventories.size() == 0) {
            // nothing to be consolidated into
            logger.debug(">> no suitable existing inventory to be consolidated with");
            return inventory;
        }
        else if (inventories.size() > 1) {
            logger.debug("We got {} inventory record that we can consolidate into", inventories.size());
            throw InventoryConsolidationException.raiseException(
                "Too many inventory records in the destination, don't know how to consolidate");

        }
        // make sure we can consolidate
        Inventory destinationInventory = inventories.get(0);
        // Move the quantity from source inventory to destination inventory
        destinationInventory.setQuantity(destinationInventory.getQuantity() + inventory.getQuantity());
        logger.debug(">> Will consolidate the inventory with the existing one: {} / {}", destinationInventory.getId(),  destinationInventory.getLpn());
        return destinationInventory;
    }

    private boolean canConsolidate(Inventory sourceInventory, Inventory destinationInventory) {
        // the inventory must have the same
        // item
        // footprint
        // inventory status
        logger.debug("sourceInventory.getItem().equals(destinationInventory.getItem()): {}",
                sourceInventory.getItem().equals(destinationInventory.getItem()));
        logger.debug("sourceInventory.getItemPackageType().equals(destinationInventory.getItemPackageType()): {}",
                sourceInventory.getItemPackageType().equals(destinationInventory.getItemPackageType()));
        logger.debug("sourceInventory.getInventoryStatus().equals(destinationInventory.getInventoryStatus()):{}",
                sourceInventory.getInventoryStatus().equals(destinationInventory.getInventoryStatus()));
        return sourceInventory.getItem().equals(destinationInventory.getItem()) &&
                sourceInventory.getItemPackageType().equals(destinationInventory.getItemPackageType()) &&
                sourceInventory.getInventoryStatus().equals(destinationInventory.getInventoryStatus());
    }
}
