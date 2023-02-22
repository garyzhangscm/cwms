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

package com.garyzhangscm.cwms.inbound.service;

import com.garyzhangscm.cwms.inbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.model.Inventory;
import com.garyzhangscm.cwms.inbound.model.Location;
import com.garyzhangscm.cwms.inbound.model.PutawayConfigurationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PutawayConfigurationStrategyService {

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    public List<Location> fitlerLocationByStrategy(Long warehouseId,
                                                   List<Location> locations, Inventory inventory, PutawayConfigurationStrategy putawayConfigurationStrategy) {


        switch (putawayConfigurationStrategy) {
            case EMPTY_LOCATIONS:
                return fitlerLocationByStrategyEmptyLocation(locations);
            case PARTIAL_LOCATIONS:
                return fitlerLocationByStrategyPartialLocation(warehouseId, locations, inventory);
            case PARTIAL_LOCATIONS_MIX_ITEM:
                return fitlerLocationByStrategyPartialLocationMixItem(warehouseId, locations, inventory);
            default:
                return fitlerLocationByStrategyEmptyLocation(locations);
        }
    }

    private List<Location> fitlerLocationByStrategyEmptyLocation(List<Location> locations) {
        return locations.stream().filter(location -> location.getCurrentVolume() + location.getPendingVolume() == 0.0)
                .collect(Collectors.toList());
    }

    private List<Location> fitlerLocationByStrategyPartialLocation(Long warehouseId,
                                                                   List<Location> locations, Inventory inventory) {

        // Get all the existing inventory with the same ite. Then filter the locations
        // with the locations that has those inventory

        List<Inventory> existingInventories = inventoryServiceRestemplateClient.findInventoryByItem(warehouseId, inventory.getItem());
        List<String> existingInventoryLocationNames = existingInventories.stream()
                                                        .map(existingInventory -> existingInventory.getLocation().getName())
                                                        .collect(Collectors.toList());
        return locations.stream().filter(location -> existingInventoryLocationNames.contains(location.getName()))
                .collect(Collectors.toList());

    }


    private List<Location> fitlerLocationByStrategyPartialLocationMixItem(Long warehouseId,
                                                                          List<Location> locations, Inventory inventory) {

        // Get all the existing inventory with the same ite. Then filter the locations
        // with the locations that doesn't have those inventory but is not empty
        List<Inventory> existingInventories = inventoryServiceRestemplateClient.findInventoryByItem(
                warehouseId, inventory.getItem());
        List<String> existingInventoryLocationNames = existingInventories.stream()
                .map(existingInventory -> existingInventory.getLocation().getName())
                .collect(Collectors.toList());
        return locations.stream().filter(location -> location.getCurrentVolume() + location.getPendingVolume() > 0.0)
                .filter(location -> !existingInventoryLocationNames.contains(location.getName()))
                .collect(Collectors.toList());
    }

}
