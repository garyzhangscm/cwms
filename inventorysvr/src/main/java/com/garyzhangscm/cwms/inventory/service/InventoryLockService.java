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

import com.garyzhangscm.cwms.inventory.exception.InventoryException;
import com.garyzhangscm.cwms.inventory.exception.InventoryLockException;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventoryLockRepository;
import com.garyzhangscm.cwms.inventory.repository.ItemSamplingRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryLockService {
    private static final Logger logger = LoggerFactory.getLogger(InventoryLockService.class);

    @Autowired
    private InventoryLockRepository inventoryLockRepository;

    @Autowired
    private InventoryWithLockService inventoryWithLockService;
    @Autowired
    private InventoryService inventoryService;


    public InventoryLock findById(Long id) {
        InventoryLock inventoryLock = inventoryLockRepository.findById(id)
                 .orElseThrow(() -> ResourceNotFoundException.raiseException("inventory lock not found by id: " + id));
         return inventoryLock;
    }


    public List<InventoryLock> findAll(Long warehouseId,
                                      String name) {

        return inventoryLockRepository.findAll(
            (Root<InventoryLock> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();

                predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));


                if (StringUtils.isNotBlank(name)) {
                    if (name.contains("*")) {
                        predicates.add(criteriaBuilder.like(root.get("name"), name.replaceAll("\\*", "%")));
                    }
                    else {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }
                }


                Predicate[] p = new Predicate[predicates.size()];
                return criteriaBuilder.and(predicates.toArray(p));
            }
        );

    }



    public InventoryLock findByName(Long warehouseId, String name){
        return inventoryLockRepository.findByWarehouseIdAndName(warehouseId, name);
    }

    @Transactional
    public InventoryLock save(InventoryLock inventoryLock) {
        return inventoryLockRepository.save(inventoryLock);
    }


    @Transactional
    public InventoryLock saveOrUpdate(InventoryLock inventoryLock) {
        if (inventoryLock.getId() == null && findByName(inventoryLock.getWarehouseId(), inventoryLock.getName()) != null) {
            inventoryLock.setId(findByName(inventoryLock.getWarehouseId(), inventoryLock.getName()).getId());
        }
        return save(inventoryLock);
    }
    @Transactional
    public void delete(InventoryLock inventoryLock) {
        inventoryLockRepository.delete(inventoryLock);
    }
    @Transactional
    public void delete(Long id) {
        inventoryLockRepository.deleteById(id);
    }


    public InventoryLock addInventoryLock(InventoryLock inventoryLock) {
        return saveOrUpdate(inventoryLock);
    }

    public InventoryLock changeInventoryLock(Long id, InventoryLock inventoryLock) {
        inventoryLock.setId(id);
        return saveOrUpdate(inventoryLock);
    }

    public void removeInventoryLock(Long id) {
        // make sure there's no inventory with this lock
        InventoryLock inventoryLock = findById(id);

        if (!inventoryWithLockService.findInventoryWithLock(inventoryLock).isEmpty()) {
            // there's inventory locked by the lock
            throw InventoryLockException.raiseException("Can't remove the lock as there's inventory locked with this lock");
        }

        delete(id);
    }

    public InventoryLock disableInventoryLock(Long id) {
        // make sure there's no inventory with this lock
        InventoryLock inventoryLock = findById(id);

        if (!inventoryWithLockService.findInventoryWithLock(inventoryLock).isEmpty()) {
            // there's inventory locked by the lock
            throw InventoryLockException.raiseException("Can't disable the lock as there's inventory locked with this lock");
        }

        inventoryLock.setEnabled(false);
        return saveOrUpdate(inventoryLock);
    }

    public InventoryLock enableInventoryLock(Long id) {
        // make sure there's no inventory with this lock
        InventoryLock inventoryLock = findById(id);
        inventoryLock.setEnabled(true);
        return saveOrUpdate(inventoryLock);
    }

    public InventoryLock unlockInventory(Long id, Long inventoryId) {
        // see if the inventory is locked by this lock
        InventoryLock inventoryLock = findById(id);
        Inventory inventory = inventoryService.findById(inventoryId);
        InventoryWithLock inventoryWithLock = inventoryWithLockService.findByInventoryAndLock(
                inventory, inventoryLock
        );
        if (Objects.isNull(inventoryWithLock)) {
            // the inventory is not locked, or not locked by this lock
            throw InventoryLockException.raiseException("Inventory " + inventory.getLpn() + " is not locked by lock " + inventoryLock.getName());
        }
        // let's remove this lock
        inventoryWithLockService.releaseInventoryLock(inventoryWithLock);
        return findById(id);
    }

    public List<Inventory> getLockedInventory(Long id) {
        InventoryLock inventoryLock = findById(id);
        return inventoryWithLockService.findInventoryWithLock(
                 inventoryLock
        );
    }

    public String validateNewInventoryLockName(Long warehouseId, String inventoryLockName) {
        InventoryLock inventoryLock =
                findByName(warehouseId, inventoryLockName);

        return Objects.isNull(inventoryLock) ? "" : ValidatorResult.VALUE_ALREADY_EXISTS.name();
    }
}
