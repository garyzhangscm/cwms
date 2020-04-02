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
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.AuditCountResultRepository;
import com.garyzhangscm.cwms.inventory.repository.CycleCountResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditCountResultService {
    private static final Logger logger = LoggerFactory.getLogger(AuditCountResultService.class);

    @Autowired
    private AuditCountResultRepository auditCountResultRepository;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private AuditCountRequestService auditCountRequestService;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    public List<AuditCountResult> findByBatchId(String batchId) {
        return warehouseLayoutServiceRestemplateClient.setupAuditCountResultLocations(auditCountResultRepository.findByBatchId(batchId));
    }


    @Transactional
    public AuditCountResult save(AuditCountResult auditCountResult) {
        return auditCountResultRepository.save(auditCountResult);
    }

    public List<AuditCountResult> getEmptyAuditCountResults(String batchId,
                                                 Long locationId){
        List<AuditCountResult> auditCountResults = new ArrayList<>();


        Location location = warehouseLayoutServiceRestemplateClient.getLocationById(locationId);

        // Get all inventory from the location
        List<Inventory> inventories = inventoryService.findByLocationId(locationId);
        if (inventories.size() == 0) {
            // There's nothing left in the location, let's
            // count it as 0;
            auditCountResults.add(
                    AuditCountResult.emptyLocationAuditCountResult(
                            location.getWarehouse().getId(), batchId, locationId, location));
        }
        else {
            // We will generate an empty audit count result(count quantity is 0) for each inventory
            // so that the front end user can start to input the quantity
            inventories.forEach(inventory ->
                    auditCountResults.add(
                            new AuditCountResult(location.getWarehouse().getId(), batchId, locationId,
                                    location, inventory, inventory.getQuantity(), 0L)));
        }
        return auditCountResults;
    }
    @Transactional
    public List<AuditCountResult> confirmAuditCountResults(String batchId,
                                                           Long locationId,
                                                           List<AuditCountResult> auditCountResults){
        List<AuditCountResult> confirmedAuditCountResults = new ArrayList<>();
        for (AuditCountResult auditCountResult : auditCountResults) {
            if (auditCountResult.getInventory() == null) {
                // Ok, there's no inventory information, which happens when
                // it is a empty location, let's just skip it.
                continue;
            }
            if (auditCountResult.getInventory().getId() == null && auditCountResult.getCountQuantity() == 0) {
                // the inventory is a new inventory but the user count as 0 quantity, which is
                // a bad data
                continue;
            }
            confirmedAuditCountResults.add(confirmAuditCountResult(auditCountResult));
        }

        if (confirmedAuditCountResults.size() == 0) {
            confirmAuditCountResultAsEmptyLocation(batchId, locationId);
        }

        auditCountRequestService.removeAuditCountRequestByBatchIdAndLocationId(batchId, locationId);
        return confirmedAuditCountResults;
    }

    @Transactional
    public AuditCountResult confirmAuditCountResult(AuditCountResult auditCountResult) {


        // for each result, if we already have the inventory, save the quantity change to the inventory
        // -- if the count quantity is 0, then we will remove the inventory
        // If the inventory doesn't exits, let's create the new inventory
        if (auditCountResult.getInventory().getId() != null) {
            if (auditCountResult.getCountQuantity() == 0) {
                inventoryService.removeInventory(auditCountResult.getInventory(),
                        InventoryQuantityChangeType.AUDIT_COUNT);
            }
            else {
                auditCountResult.getInventory().setQuantity(auditCountResult.getCountQuantity());
                inventoryService.save(auditCountResult.getInventory());
            }
            return save(auditCountResult);
        }
        else {
            auditCountResult.getInventory().setQuantity(auditCountResult.getCountQuantity());
            auditCountResult.setInventory(inventoryService.save(auditCountResult.getInventory()));
            return save(auditCountResult);
        }
    }

    @Transactional
    public void confirmAuditCountResultAsEmptyLocation(String batchId,
                                                       Long locationId) {
        List<AuditCountResult> auditCountResults = getEmptyAuditCountResults(batchId, locationId);
        auditCountResults.forEach(auditCountResult -> save(auditCountResult));

    }

}

