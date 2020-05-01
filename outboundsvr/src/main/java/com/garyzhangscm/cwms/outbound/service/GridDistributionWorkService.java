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

package com.garyzhangscm.cwms.outbound.service;


import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.GridException;
import com.garyzhangscm.cwms.outbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.*;


@Service
public class GridDistributionWorkService {
    private static final Logger logger = LoggerFactory.getLogger(GridDistributionWorkService.class);

    @Autowired
    private GridConfigurationService gridConfigurationService;
    @Autowired
    private PickListService pickListService;
    @Autowired
    private CartonizationService cartonizationService;


    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    /**
     * Get grid distribution work basedon the ID passed in
     * ID can be list id / carton id / LPN / P&D location
     * @param warehouseId warehouse id
     * @param id list id / carton id / LPN / P&D location
     * @return
     */
    public List<GridDistributionWork> getGridDistributionWork(Long warehouseId, Long locationGroupId, String id) {

        // Get the Grid information first
        GridConfiguration gridConfiguration = gridConfigurationService.findByWarehouseIdAndLocationGroupId(warehouseId, locationGroupId);
        logger.debug("Get grid configuration by location group id: {}, \n {}", locationGroupId, gridConfiguration);

        // See if the ID is list number
        PickList pickList = pickListService.findByNumber(warehouseId, id);
        if (Objects.nonNull(pickList)) {

            logger.debug("Current ID {} is a list identifier. FInd list:\n {}", id, pickList);
            return getGridDistributionWork(warehouseId, gridConfiguration, pickList);

        }

        // check if the id is a cartonization number
        Cartonization cartonization = cartonizationService.findByNumber(warehouseId, id);
        if (Objects.nonNull(cartonization)) {

            logger.debug("Current ID {} is a cartonization identifier. FInd cartonization:\n {}", id, cartonization);
            return getGridDistributionWork(warehouseId, gridConfiguration, cartonization);
        }

        logger.debug(" We can't recognize this id: {}", id);
        throw GridException.raiseException("ID not support");
    }


    public List<GridDistributionWork> getGridDistributionWork(Long warehouseId, GridConfiguration gridConfiguration, PickList pickList) {
        return getGridDistributionWork(warehouseId, gridConfiguration, pickList.getNumber());

    }
    public List<GridDistributionWork> getGridDistributionWork(Long warehouseId, GridConfiguration gridConfiguration, Cartonization cartonization) {
        return getGridDistributionWork(warehouseId, gridConfiguration, cartonization.getNumber());

    }
    public List<GridDistributionWork> getGridDistributionWork(Long warehouseId, GridConfiguration gridConfiguration, String locationName) {

        // We will only allow inventory that is
        // 1. on the pick list
        // 2. next location is within the current grid
        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(warehouseId, locationName);
        if (Objects.isNull(location)) {

            logger.debug(" Fail to get location by name {}", locationName);
            return new ArrayList<>();
        }
        List<Inventory> inventories = inventoryServiceRestemplateClient.getInventoryByLocation(location);
        if (inventories.size() == 0) {
            logger.debug(" There's no inventory at location {}", locationName);
            return new ArrayList<>();
        }

        logger.debug("Will start to get distribution work by inventory\n {}", inventories);
        Map<String, GridDistributionWork> gridDistributionWorkMap = new HashMap<>();
        inventories.stream().forEach(inventory -> {
            logger.debug(">> Check grid location for current inventory: \n{}", inventory);
            if (inventory.getInventoryMovements().size() > 0) {
                Location nextLocation = inventory.getInventoryMovements().get(0).getLocation();
                logger.debug(">> The inventory's next location is \n{}" +
                        "next location's group id :\n{} " +
                        "current grid's location group id:\n {}",
                        nextLocation,
                        nextLocation.getLocationGroup().getId(),
                        gridConfiguration.getLocationGroupId());
                if (nextLocation.getLocationGroup().getId().equals(gridConfiguration.getLocationGroupId())) {

                    logger.debug(">> THe next location belongs to the same grid ");
                    String key = nextLocation.getName() + "-" + inventory.getItem().getName();
                    GridDistributionWork gridDistributionWork = gridDistributionWorkMap.getOrDefault(key,
                            new GridDistributionWork(nextLocation.getName(), inventory.getItem().getName()));

                    gridDistributionWork.addQuantity(inventory.getQuantity());
                    logger.debug(">> key: {}, quantity: {}", key, gridDistributionWork.getQuantity());
                    gridDistributionWorkMap.put(key, gridDistributionWork);
                }
            }
        });
        return new ArrayList<>(gridDistributionWorkMap.values());
    }





}
