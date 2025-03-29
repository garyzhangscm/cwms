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

import com.garyzhangscm.cwms.inventory.model.Inventory;
import com.garyzhangscm.cwms.inventory.model.InventoryLock;
import com.garyzhangscm.cwms.inventory.model.InventoryWithLock;

import com.garyzhangscm.cwms.inventory.repository.InventoryWithLockRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class InventoryWithLockService {
    private static final Logger logger = LoggerFactory.getLogger(InventoryWithLockService.class);

    @Autowired
    private InventoryWithLockRepository inventoryWithLockRepository;


    public InventoryWithLock findByInventoryAndLock(Inventory inventory, InventoryLock lock){
        return inventoryWithLockRepository.findByInventoryAndLock(inventory, lock);
    }

    public boolean inventoryIsLockedByLock(Inventory inventory, InventoryLock lock) {

        return Objects.nonNull(findByInventoryAndLock(inventory, lock));
    }

    @Transactional
    public InventoryWithLock save(InventoryWithLock inventoryWithLock) {
        return inventoryWithLockRepository.save(inventoryWithLock);
    }


    @Transactional
    public InventoryWithLock saveOrUpdate(InventoryWithLock inventoryWithLock) {
        if (inventoryWithLock.getId() == null && findByInventoryAndLock(
                inventoryWithLock.getInventory(), inventoryWithLock.getLock()) != null) {
            inventoryWithLock.setId(findByInventoryAndLock(inventoryWithLock.getInventory(), inventoryWithLock.getLock()).getId());
        }
        return save(inventoryWithLock);
    }

    /**
     * Return all the inventory with certain lock
     * @param inventoryLock
     * @return
     */
    public List<Inventory> findInventoryWithLock(InventoryLock inventoryLock) {

        return inventoryWithLockRepository.findByLock(inventoryLock).stream()
                .map(InventoryWithLock::getInventory).collect(Collectors.toList());
    }

    /**
     * Return all locks that locked the inventory
     * @param inventory
     * @return
     */
    public List<InventoryLock> findInventoryLock(Inventory inventory) {

        return inventoryWithLockRepository.findByInventory(inventory).stream()
                .map(InventoryWithLock::getLock).collect(Collectors.toList());
    }


    public InventoryWithLock lockInventory(Inventory inventory, InventoryLock lock, String comment) {
        InventoryWithLock inventoryWithLock = new InventoryWithLock(
                inventory, lock, comment
        );
        return saveOrUpdate(inventoryWithLock);
    }

    public void releaseInventoryLock(InventoryWithLock inventoryWithLock) {

        delete(inventoryWithLock);
    }

    public void delete(InventoryWithLock inventoryWithLock) {
        inventoryWithLockRepository.delete(inventoryWithLock);
    }
}
