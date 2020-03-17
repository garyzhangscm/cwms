package com.garyzhangscm.cwms.inventory.model;

import com.garyzhangscm.cwms.inventory.exception.GenericException;
import com.garyzhangscm.cwms.inventory.exception.InventoryConsolidationException;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class InventoryConsolidationByLPN implements InventoryConsolidationHandler {

    private static final Logger logger = LoggerFactory.getLogger(InventoryConsolidationByLPN.class);
    /**
     * Consolidate the inventory into inventories by LPN. We will still keep both inventory
     * structure but replace the LPN of the source inventory with the destination's LPN
     * @param inventory Source inventory.
     * @param inventories: destination inventory
     * @return new source inventory to be persist
     */
    public Inventory consolidate(Inventory inventory, List<Inventory> inventories){
        logger.debug(">> will try to consolidate by LPN");

        if (inventories.contains(inventory)){
            logger.debug(">> the inventory array already contains the current inventory, will remove it first");
            inventories.remove(inventory);
        }

        if (inventories.size() == 0) {
            // nothing to be consolidated
            logger.debug(">> no existing inventory to be consolidated with");
            return inventory;
        }

        // Let's make sure there's only one LPN to be consolidated
        List<String> lpns = inventories.stream().map(Inventory::getLpn)
                .filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (lpns.size() == 0) {
            // we can't get a LPN to be consolidated into
            // just return
            logger.debug(">> no existing LPN to be consolidated with");
            return inventory;
        }
        else if (lpns.size() > 1) {
            // we got multiple LPNs to be consolidated into, which we don't know
            // how to consolidate, raise an error
            throw InventoryConsolidationException.raiseException("Multiple LPN found at destination, not sure how to consolidate");

        }
        logger.debug(">> Will consolidate the inventory with the existing LPN: {}", lpns.get(0));
        inventory.setLpn(lpns.get(0));

        return inventory;

    }
}
