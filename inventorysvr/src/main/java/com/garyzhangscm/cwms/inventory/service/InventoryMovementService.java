/**
 * Copyright 2018
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.Inventory;
import com.garyzhangscm.cwms.inventory.model.InventoryMovement;
import com.garyzhangscm.cwms.inventory.model.Location;
import com.garyzhangscm.cwms.inventory.model.PickMovement;
import com.garyzhangscm.cwms.inventory.repository.InventoryMovementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class InventoryMovementService{
    private static final Logger logger = LoggerFactory.getLogger(InventoryMovementService.class);

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    public InventoryMovement findById(Long id) {
        return inventoryMovementRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("inventory movement not found by id: " + id));
    }

    public List<InventoryMovement> findByInventoryId(Long inventoryId) {
        return inventoryMovementRepository.findByInventoryId(inventoryId);
    }
    public InventoryMovement save(InventoryMovement inventoryMovement) {
        return inventoryMovementRepository.save(inventoryMovement);
    }

    // Save the movement path for one inventory, suppose the movement path is sorted by sequence
    public List<InventoryMovement> save(List<InventoryMovement> inventoryMovements) {
        List<InventoryMovement> result = new ArrayList<>();
        int sequence = 0;
        // We will setup the sequence ascend for each movement path that doesn't
        // have the sequence setup yet
        for(InventoryMovement inventoryMovement : inventoryMovements) {
            if (inventoryMovement.getSequence() == null) {
                sequence ++;
                inventoryMovement.setSequence(sequence);
            }
            else {
                sequence = inventoryMovement.getSequence();
            }
            result.add(save(inventoryMovement));
        }
        return result;

    }


    public void delete(InventoryMovement inventoryMovement) {
        inventoryMovementRepository.delete(inventoryMovement);
    }
    public void delete(Long id) {
        inventoryMovementRepository.deleteById(id);
    }

    public void removeInventoryMovement(Long id, Inventory inventory) {
        // Once we remove the movement from the location, we will need to
        // deduct the pending quantity from the location
        InventoryMovement inventoryMovement = findById(id);
        if (inventoryMovement != null) {

            warehouseLayoutServiceRestemplateClient.reduceLocationPendingVolume(inventoryMovement.getLocationId(), inventory.getSize());
        }
        delete(id);
    }
    public void clearInventoryMovement(Inventory inventory) {
        inventory.getInventoryMovements()
                .stream()
                .forEach(inventoryMovement -> {
                    // remove from the DB and clear the location's pending volume

                    logger.debug("will clear movement path: \n {}",
                            inventoryMovement);
                    warehouseLayoutServiceRestemplateClient.deallocateLocation(
                            inventoryMovement.getLocation(), inventory
                    );
                    delete(inventoryMovement);
                });
    }

    public InventoryMovement createInventoryMovementFromPickMovement(Inventory inventory, PickMovement pickMovement) {
        return createInventoryMovement(inventory, pickMovement.getSequence(), pickMovement.getLocationId());
    }

    public InventoryMovement createInventoryMovement(Inventory inventory, Long locationId) {
        List<InventoryMovement> inventoryMovements = inventory.getInventoryMovements();
        int sequence = 0;
        if (inventoryMovements.size() > 0){
            sequence = inventoryMovements.stream().map(InventoryMovement::getSequence).max(Integer::compare).get() +1;
        }
        return createInventoryMovement(inventory, sequence, locationId);

    }
    public InventoryMovement createInventoryMovement(Inventory inventory, int sequence, Long locationId) {
        InventoryMovement inventoryMovement = new InventoryMovement();
        inventoryMovement.setInventory(inventory);
        inventoryMovement.setLocationId(locationId);
        inventoryMovement.setSequence(sequence);
        inventoryMovement.setWarehouseId(inventory.getWarehouseId());
        return save(inventoryMovement);


    }


}
