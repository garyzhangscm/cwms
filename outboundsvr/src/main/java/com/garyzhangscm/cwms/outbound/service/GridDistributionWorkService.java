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


import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.GridException;
import com.garyzhangscm.cwms.outbound.exception.PickingException;
import com.garyzhangscm.cwms.outbound.model.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class GridDistributionWorkService {
    private static final Logger logger = LoggerFactory.getLogger(GridDistributionWorkService.class);

    @Autowired
    private GridConfigurationService gridConfigurationService;
    @Autowired
    private GridLocationConfigurationService gridLocationConfigurationService;
    @Autowired
    private PickListService pickListService;
    @Autowired
    private CartonizationService cartonizationService;
    @Autowired
    private PickMovementService pickMovementService;


    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;



    public List<GridDistributionWork> getGridDistributionWork(Long warehouseId,
                                                              Long locationGroupId, String id,
                                                              Long gridLocationConfigurationId) {

        if (Objects.nonNull(gridLocationConfigurationId)) {
            return getGridDistributionWorkByGridLocation(warehouseId, gridLocationConfigurationId);

        } else {

            return getGridDistributionWorkByContainer(warehouseId, locationGroupId, id);
        }
    }


    public List<GridDistributionWork> getGridDistributionWorkByGridLocation(Long warehouseId,
                                                                         Long gridLocationConfigurationId) {

        GridLocationConfiguration gridLocationConfiguration =
                gridLocationConfigurationService.findById(gridLocationConfigurationId);
        List<PickMovement> pickMovements = pickMovementService.findByHopLocation(
                gridLocationConfiguration.getLocationId()
        );
        if (pickMovements.size() == 0) {
            return new ArrayList<>();
        }

        // Sum up the quantity for each item that will be moved through
        // this hop location
        Map<String, Long> pendingItemQuantity = new HashMap<>();
        pickMovements.forEach(pickMovement -> {
            Item item = Optional.ofNullable(pickMovement.getPick().getItem()).orElse(
                    inventoryServiceRestemplateClient.getItemById(
                            pickMovement.getPick().getItemId()
                    )
            );
            Long quantity = pendingItemQuantity.getOrDefault(
                    item.getName(), 0L
            );
            quantity +=   (pickMovement.getPick().getPickedQuantity() - pickMovement.getArrivedQuantity()) ;
            pendingItemQuantity.put(item.getName(), quantity);
        });

        return pendingItemQuantity.entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> {
                    GridDistributionWork gridDistributionWork = new GridDistributionWork();
                    gridDistributionWork.setGridLocationName(gridLocationConfiguration.getLocation().getName());
                    gridDistributionWork.setItemName(entry.getKey());
                    gridDistributionWork.setQuantity(entry.getValue());
                    return gridDistributionWork;
        }).collect(Collectors.toList());
    }

    /**
     * Get grid distribution work basedon the ID passed in
     * ID can be list id / carton id / LPN / P&D location
     * @param warehouseId warehouse id
     * @param id list id / carton id / LPN / P&D location
     * @return
     */
    public List<GridDistributionWork> getGridDistributionWorkByContainer(Long warehouseId,
                                                              Long locationGroupId, String id) {
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


    public void confirmGridDistributionWork(Long warehouseId,
                                            String id,
                                            Long gridLocationConfigurationId,
                                            String itemName,
                                            Long quantity) {

        GridLocationConfiguration gridLocationConfiguration =
                gridLocationConfigurationService.findById(gridLocationConfigurationId);
        logger.debug("Confirm by moving all inventory from id {} into location {} ",
                id, gridLocationConfiguration.getLocation().getName());

        // See if the ID is list number
        PickList pickList = pickListService.findByNumber(warehouseId, id);
        if (Objects.nonNull(pickList)) {

            logger.debug("Current ID {} is a list identifier. FInd list:\n {}", id, pickList);
            confirmGridDistributionWork(warehouseId, pickList, gridLocationConfiguration.getLocation(),
                    itemName, quantity);
            return ;

        }

        // check if the id is a cartonization number
        Cartonization cartonization = cartonizationService.findByNumber(warehouseId, id);
        if (Objects.nonNull(cartonization)) {

            logger.debug("Current ID {} is a cartonization identifier. FInd cartonization:\n {}", id, cartonization);
            confirmGridDistributionWork(warehouseId, cartonization, gridLocationConfiguration.getLocation(),
                    itemName, quantity);
            return ;
        }

        logger.debug(" We can't recognize this id: {}", id);
        throw GridException.raiseException("ID not support");
    }


    public void confirmGridDistributionWork(Long warehouseId, PickList pickList, Location location,
                                            String itemName,
                                            Long quantity) {



        confirmGridDistributionWork(warehouseId, pickList.getNumber(), location,
                itemName, quantity);

    }
    public void confirmGridDistributionWork(Long warehouseId,
                                            Cartonization cartonization,
                                            Location location,
                                            String itemName,
                                            Long quantity)  {
        confirmGridDistributionWork(warehouseId, cartonization.getNumber(), location, itemName, quantity);

    }

    public void confirmGridDistributionWork(Long warehouseId,
                                            String sourceLocationNumber,
                                            Location nextLocation,
                                            String itemName,
                                            Long quantity)   {

        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(warehouseId, sourceLocationNumber);
        if (Objects.isNull(location)) {

            logger.debug(" Fail to get location by name {}", sourceLocationNumber);
            return ;
        }
        List<Inventory> inventories;
        if (StringUtils.isNotBlank(itemName) ) {

            logger.debug("# Start to confirm grid distribution work by {}, quantity {}",
                    itemName, quantity);
            inventories = new ArrayList<>();
            List<Inventory> matchedInventory = inventoryServiceRestemplateClient.getInventoryByLocationAndItemName(location, itemName);
            logger.debug("# Find {} inventory record by item {} in location {}",
                    matchedInventory.size(), itemName, location.getName());
            // We will sort by quantity
            matchedInventory.sort(Comparator.comparingLong(o -> Math.abs(o.getQuantity() - quantity)));

            Long quantityToBeMoved = quantity;
            Iterator<Inventory> inventoryIterator = matchedInventory.iterator();
            while(quantityToBeMoved > 0 && inventoryIterator.hasNext()) {
                Inventory inventory = inventoryIterator.next();
                logger.debug("## Will confirm with inventory id {}, quantity {}. Quantity left: {}",
                        inventory.getId(), inventory.getQuantity(), quantityToBeMoved);
                if (inventory.getQuantity() > quantityToBeMoved) {
                    // Current inventory is enough for the quantity to be moved
                    // let's split the inventory

                    String newLpn = commonServiceRestemplateClient.getNextNumber("lpn");
                    List<Inventory> splitInventory = inventoryServiceRestemplateClient.split(inventory, newLpn, quantityToBeMoved);
                    if (splitInventory.size() != 2) {
                        throw GridException.raiseException("Inventory split for pick error! Inventory is not split into 2");
                    }
                    inventories.add(splitInventory.get(1));
                    break;
                }
                else {
                    inventories.add(inventory);
                    quantityToBeMoved -= inventory.getQuantity();
                }
            }
        }
        else {
            inventories = inventoryServiceRestemplateClient.getInventoryByLocation(location);

        }
        if (inventories.size() == 0) {
            logger.debug(" There's no inventory at location {}", sourceLocationNumber);
            return ;
        }
        try {
            Iterator<Inventory> inventoryIterator = inventories.iterator();
            while (inventoryIterator.hasNext()) {
                Inventory inventory = inventoryIterator.next();
                inventoryServiceRestemplateClient.moveInventory(inventory, nextLocation);

                // Update the status of the grid location after we move the inventory
                // into the location
                gridLocationConfigurationService.confirmGridDistributionWork(warehouseId, nextLocation, inventory);

            }
        }
        catch (IOException ex) {
            throw GridException.raiseException("Can't get any inventory by id:" + sourceLocationNumber);
        }

    }


}
