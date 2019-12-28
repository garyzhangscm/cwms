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

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.inventory.model.Inventory;
import com.garyzhangscm.cwms.inventory.model.InventoryMovement;
import com.garyzhangscm.cwms.inventory.model.InventoryStatus;
import com.garyzhangscm.cwms.inventory.repository.InventoryMovementRepository;
import com.garyzhangscm.cwms.inventory.repository.InventoryStatusRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class InventoryMovementService{
    private static final Logger logger = LoggerFactory.getLogger(InventoryMovementService.class);

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;


    public List<InventoryMovement> findByInventory(Inventory inventory) {
        return findByInventoryId(inventory.getId());
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

}
