/**
 * Copyright 2019
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TestDataInitService {
    ItemService itemService;

    ItemFamilyService itemFamilyService;

    ItemPackageTypeService itemPackageTypeService;

    ItemUnitOfMeasureService itemUnitOfMeasureService;

    InventoryStatusService inventoryStatusService;

    InventoryService inventoryService;

    MovementPathService movementPathService;

    Map<String, TestDataInitiableService> initiableServices = new HashMap<>();
    List<String> serviceNames = new ArrayList<>();
    @Autowired
    public TestDataInitService(ItemFamilyService itemFamilyService,
                               ItemService itemService,
                               ItemUnitOfMeasureService itemUnitOfMeasureService,
                               ItemPackageTypeService itemPackageTypeService,
                               InventoryStatusService inventoryStatusService,
                               InventoryService inventoryService,
                               MovementPathService movementPathService) {
        this.itemFamilyService = itemFamilyService;
        this.itemService = itemService;
        this.itemPackageTypeService = itemPackageTypeService;
        this.itemUnitOfMeasureService = itemUnitOfMeasureService;
        this.inventoryStatusService = inventoryStatusService;
        this.inventoryService = inventoryService;
        this.movementPathService = movementPathService;


        initiableServices.put("Item Family", itemFamilyService);
        serviceNames.add("Item Family");
        initiableServices.put("Item", itemService);
        serviceNames.add("Item");
        initiableServices.put("Item Package Type", itemPackageTypeService);
        serviceNames.add("Item Package Type");
        initiableServices.put("Item Unit of Measure", itemUnitOfMeasureService);
        serviceNames.add("Item Unit of Measure");
        initiableServices.put("Inventory status", inventoryStatusService);
        serviceNames.add("Inventory status");
        initiableServices.put("Inventory", inventoryService);
        serviceNames.add("Inventory");
        initiableServices.put("Movement Path", movementPathService);
        serviceNames.add("Movement Path");
    }
    public String[] getTestDataNames() {
        return serviceNames.toArray(new String[0]);
    }
    public void init() {
        for(TestDataInitiableService testDataInitiableService : initiableServices.values()) {
            testDataInitiableService.initTestData();
        }
    }
    public void init(String name) {
        initiableServices.get(name).initTestData();
    }
    public TestDataInitiableService getInitiableService(String name) {
        return initiableServices.get(name);
    }

}
