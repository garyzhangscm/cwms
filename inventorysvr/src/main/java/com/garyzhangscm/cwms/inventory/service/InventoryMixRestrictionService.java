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

import com.garyzhangscm.cwms.inventory.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventoryMixRestrictionRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
@Service
public class InventoryMixRestrictionService {
    private static final Logger logger = LoggerFactory.getLogger(InventoryMixRestrictionService.class);

    @Autowired
    private InventoryMixRestrictionRepository inventoryMixRestrictionRepository;

    @Autowired
    private ClientRestrictionUtil clientRestrictionUtil;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;


    public InventoryMixRestriction findById(Long id) {
        return findById(id, true);
    }
    public InventoryMixRestriction findById(Long id, boolean includeDetails) {
        InventoryMixRestriction inventoryMixRestriction = inventoryMixRestrictionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("inventory mix restrction not found by id: " + id));
        if (includeDetails) {
            loadAttribute(inventoryMixRestriction);
        }
        return inventoryMixRestriction;
    }

    public List<InventoryMixRestriction> findAll(Long warehouseId,
                                                 Long locationGroupTypeId,
                                                 Long locationGroupId,
                                                 Long locationId,
                                                 String locationName,
                                                 Long clientId,
                                                 ClientRestriction clientRestriction) {
        return findAll(warehouseId,
                locationGroupTypeId,
                locationGroupId,
                locationId,
                locationName,
                clientId,
                clientRestriction,
                true);
    }


    public List<InventoryMixRestriction> findAll(Long warehouseId,
                                                 Long locationGroupTypeId,
                                                 Long locationGroupId,
                                                 Long locationId,
                                                 String locationName,
                                                 Long clientId,
                                                 ClientRestriction clientRestriction,
                                                 boolean includeDetails) {

        List<InventoryMixRestriction> inventoryMixRestrictions =  inventoryMixRestrictionRepository.findAll(
                (Root<InventoryMixRestriction> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(locationGroupTypeId)) {
                        predicates.add(criteriaBuilder.equal(root.get("locationGroupTypeId"), locationGroupTypeId));
                    }
                    if (Objects.nonNull(locationGroupId)) {
                        predicates.add(criteriaBuilder.equal(root.get("locationGroupId"), locationGroupId));
                    }
                    if (Objects.nonNull(locationId)) {
                        predicates.add(criteriaBuilder.equal(root.get("locationId"), locationId));
                    }
                    if (Strings.isNotBlank(locationName)) {
                        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                                warehouseId, locationName
                        );
                        if (Objects.nonNull(location)) {

                            predicates.add(criteriaBuilder.equal(root.get("locationId"), location.getId()));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("locationId"), -9999));
                        }
                    }
                    if (Objects.nonNull(clientId)) {
                        predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
                    }

                    return clientRestrictionUtil.addClientRestriction(root,
                            predicates,
                            clientRestriction,
                            criteriaBuilder);


                }
        );


        // if we need to load the details, or asked to return the inventory
        // that is only in receiving stage,
        if (includeDetails  && inventoryMixRestrictions.size() > 0) {
            loadAttribute(inventoryMixRestrictions);
        }

        return inventoryMixRestrictions;
    }

    public InventoryMixRestriction save(InventoryMixRestriction inventoryMixRestriction) {
        return inventoryMixRestrictionRepository.save(inventoryMixRestriction);
    }


    public void removeInventoryMixRule(InventoryMixRestriction inventoryMixRestriction) {
        inventoryMixRestrictionRepository.delete(inventoryMixRestriction);
    }

    public void removeInventoryMixRule(Long id) {
        inventoryMixRestrictionRepository.deleteById(id);
    }


    private void loadAttribute(List<InventoryMixRestriction> inventoryMixRestrictions) {
        inventoryMixRestrictions.stream().forEach(this::loadAttribute);
    }

    private void loadAttribute(InventoryMixRestriction inventoryMixRestriction) {
        if (Objects.nonNull(inventoryMixRestriction.getLocationGroupTypeId()) &&
            Objects.isNull(inventoryMixRestriction.getLocationGroupType())) {
            inventoryMixRestriction.setLocationGroupType(

                    warehouseLayoutServiceRestemplateClient.getLocationGroupTypeById(
                            inventoryMixRestriction.getLocationGroupTypeId()
                    )
            );
        }

        if (Objects.nonNull(inventoryMixRestriction.getLocationGroupId()) &&
                Objects.isNull(inventoryMixRestriction.getLocationGroup())) {
            inventoryMixRestriction.setLocationGroup(

                    warehouseLayoutServiceRestemplateClient.getLocationGroupById(
                            inventoryMixRestriction.getLocationGroupId()
                    )
            );
        }

        if (Objects.nonNull(inventoryMixRestriction.getLocationId()) &&
                Objects.isNull(inventoryMixRestriction.getLocation())) {
            inventoryMixRestriction.setLocation(

                    warehouseLayoutServiceRestemplateClient.getLocationById(
                            inventoryMixRestriction.getLocationId()
                    )
            );
        }

        if (Objects.nonNull(inventoryMixRestriction.getClientId()) &&
                Objects.isNull(inventoryMixRestriction.getClient())) {
            inventoryMixRestriction.setClient(

                    commonServiceRestemplateClient.getClientById(
                            inventoryMixRestriction.getClientId()
                    )
            );
        }

    }


    /**
     * Check whether the inventory is allowed to move into the destination before we start
     * the actual movement
     * @param inventory
     * @param destinationLocation
     * @return
     */
    public boolean checkMovementAllowed(Inventory inventory, Location destinationLocation) {
        // get the inventory in the destination location
        logger.debug("start to check if we can move inventory {} into location {} via mixing restriction",
                inventory.getLpn(),
                destinationLocation.getName());
        List<Inventory> destinationInventory = inventoryService.findByLocationId(
                destinationLocation.getId(), false
        );
        if (destinationInventory.isEmpty()) {
            logger.debug("There's no inventory in the destination location {}, " +
                    "we will always allow the movement for inventory {}",
                    inventory.getLpn());
        }
        // get all the movement restriction that matches with the inventory
        List<InventoryMixRestriction> inventoryMixRestrictions =
                getMatchedInventoryMixRestriction(inventory, destinationLocation);


        // return false if any one of the movement restriction fail
        return inventoryMixRestrictions.stream().noneMatch(
                inventoryMixRestriction -> !checkMovementAllowed(inventoryMixRestriction, inventory,
                        destinationInventory, destinationLocation)
        );
    }

    /**
     * Check if the movement violate the restriction
     * @param inventoryMixRestriction
     * @param inventory
     * @param destinationInventory
     * @return
     */
    public boolean checkMovementAllowed(InventoryMixRestriction inventoryMixRestriction,
                                        Inventory inventory,
                                        List<Inventory> destinationInventory,
                                        Location destinationLocation) {

        // if any one line of the restriction fail, the validation is fail and
        // the inventory is not allowed to be moved into this location

        return inventoryMixRestriction.getLines().stream().noneMatch(
                line -> !checkMovementAllowed(line, inventory, destinationInventory, destinationLocation)
        );

    }

    public boolean checkMovementAllowed(InventoryMixRestrictionLine inventoryMixRestrictionLine,
                                        Inventory inventory,
                                        List<Inventory> destinationInventories,
                                        Location destinationLocation) {


        if (inventoryMixRestrictionLine.getType().equals(InventoryMixRestrictionLineType.BY_LPN)) {
            // if we are not allowed to be mixed certain attribute at the LPN level,
            // then we will check if we 'will' consolidate the LPN after the movement.
            // if we won't consolidate the LPN or inventory, then we won't bother check
            // if we will consolidate the LPN or inventory, then we will check if the inventory
            // can be mixed with other inventory

            InventoryConsolidationStrategy inventoryConsolidationStrategy =
                    warehouseLayoutServiceRestemplateClient.getInventoryConsolidationStrategy(destinationLocation.getLocationGroup().getId());
            if (inventoryConsolidationStrategy.equals(InventoryConsolidationStrategy.NONE)) {
                // inventory won't be mixed so there's no way to mix the inventory into any existing LPN
                // so the inventory mix restriction is by default a pass
                return true;
            }
        }

        // validate the inventory against any other inventory in the location
        // we allow mix only if all inventory in the location allow mix with the
        // new inventory
        return destinationInventories.stream().noneMatch(
                destinationInventory -> {
                    boolean allowMix = checkMovementAllowed(inventoryMixRestrictionLine, inventory, destinationInventory);
                    logger.debug("start to check the inventory {} against destination inventory {} by attribute {} / {}, " +
                                    "allow mix?: {}",
                            inventory.getLpn(),
                            destinationInventory.getLpn(),
                            inventoryMixRestrictionLine.getType(),
                            inventoryMixRestrictionLine.getAttribute(),
                            allowMix);

                    return !allowMix;
                }
        );

    }

    public boolean checkMovementAllowed(InventoryMixRestrictionLine inventoryMixRestrictionLine,
                                        Inventory inventory,
                                        Inventory destinationInventory) {
        // check which attribute are not allowed to be mixed
        String inventoryAttributeName = inventoryMixRestrictionLine.getAttribute().name();

        try {

            Class clazz = inventoryMixRestrictionLine.getAttribute().getClazz();
            Field field = inventory.getClass().getDeclaredField(inventoryAttributeName);
            field.setAccessible(true);

            Object valueForInventory = field.get(inventory);
            Object valueForDestinationInventory = field.get(destinationInventory);
            if (clazz == java.lang.String.class) {
                if (Strings.isBlank(valueForInventory.toString()) && Strings.isBlank(valueForInventory.toString())) {
                    return true;
                }
                else if (Strings.isBlank(valueForInventory.toString()) && Strings.isNotBlank(valueForInventory.toString())) {
                    return false;

                }
                else if (Strings.isNotBlank(valueForInventory.toString()) && Strings.isBlank(valueForInventory.toString())) {
                    return false;

                }
                else {

                    return clazz.cast(valueForInventory).equals(clazz.cast(valueForDestinationInventory));
                }
            }
            else if (Objects.isNull(valueForInventory) && Objects.isNull(valueForDestinationInventory)) {
                return true;
            }

            return clazz.cast(valueForInventory).equals(clazz.cast(valueForDestinationInventory));



        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.debug("Error while compare inventory with attribute {}, \n error message: {} ",
                    inventoryAttributeName, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private List<InventoryMixRestriction> getMatchedInventoryMixRestriction(Inventory inventory, Location destinationLocation) {
        List<InventoryMixRestriction> allInventoryMixRestriction = findAll(inventory.getWarehouseId(),
                null, null, null, null, null, null, false);

        logger.debug("We have {} mixing restriction setup for this warehouse {}" +
                        ", let's see if any of any match with the current inventory",
                allInventoryMixRestriction.size(), inventory.getWarehouseId());
        return allInventoryMixRestriction.stream().filter(
                inventoryMixRestriction -> isMatch(inventoryMixRestriction, inventory, destinationLocation)
        ).collect(Collectors.toList());
    }

    /**
     * Check if we can apply the inventory mix restriction on the inventory
     * @param inventoryMixRestriction
     * @param inventory
     * @return
     */
    private boolean isMatch(InventoryMixRestriction inventoryMixRestriction, Inventory inventory, Location destinationLocation) {

        // the client id should match. Either both empty, or for the same client
        if (!Objects.equals(inventoryMixRestriction.getClientId(), inventory.getClientId())) {
            logger.debug(">> Not Match(client id), mixing restriction id : {}, mixing restriction's client id: {}" +
                    ", inventory's client id: {}",
                    inventoryMixRestriction.getId(),
                    Objects.isNull(inventoryMixRestriction.getClientId()) ?
                        "N/A" : inventoryMixRestriction.getClientId(),
                    Objects.isNull(inventory.getClientId()) ?
                            "N/A" : inventory.getClientId());
            return false;
        }
        if (Objects.nonNull(inventoryMixRestriction.getLocationGroupTypeId()) &&
               !inventoryMixRestriction.getLocationGroupTypeId().equals(destinationLocation.getLocationGroup().getLocationGroupType().getId())) {
            logger.debug(">> Not Match(location group type id), mixing restriction id : {}, " +
                            " mixing restriction's location group type id: {}" +
                            ", destination location's location group type id: {}",
                    inventoryMixRestriction.getId(),
                    Objects.isNull(inventoryMixRestriction.getLocationGroupTypeId()) ?
                            "N/A" : inventoryMixRestriction.getLocationGroupTypeId(),
                    Objects.isNull(destinationLocation.getLocationGroup().getLocationGroupType().getId()) ?
                            "N/A" : destinationLocation.getLocationGroup().getLocationGroupType().getId());
            return false;
        }
        if (Objects.nonNull(inventoryMixRestriction.getLocationGroupId()) &&
                !inventoryMixRestriction.getLocationGroupId().equals(destinationLocation.getLocationGroup().getId())) {
            logger.debug(">> Not Match(location group id), mixing restriction id : {}, " +
                            " mixing restriction's location group id: {}" +
                            ", destination location's location group id: {}",
                    inventoryMixRestriction.getId(),
                    Objects.isNull(inventoryMixRestriction.getLocationGroupId()) ?
                            "N/A" : inventoryMixRestriction.getLocationGroupId(),
                    Objects.isNull(destinationLocation.getLocationGroup().getId()) ?
                            "N/A" : destinationLocation.getLocationGroup().getId());
            return false;
        }
        if (Objects.nonNull(inventoryMixRestriction.getLocationId()) &&
                !inventoryMixRestriction.getLocationId().equals(destinationLocation.getId())) {
            logger.debug(">> Not Match(location id), mixing restriction id : {}, " +
                            " mixing restriction's location id: {}" +
                            ", destination location's location id: {}",
                    inventoryMixRestriction.getId(),
                    Objects.isNull(inventoryMixRestriction.getLocationId()) ?
                            "N/A" : inventoryMixRestriction.getLocationId(),
                    Objects.isNull(destinationLocation.getId()) ?
                            "N/A" : destinationLocation.getId());
            return false;
        }
        logger.debug("last check, see if the warehouse is matching? {}",
                inventoryMixRestriction.getWarehouseId().equals(inventory.getWarehouseId()));
        return inventoryMixRestriction.getWarehouseId().equals(inventory.getWarehouseId());
    }

    public InventoryMixRestriction addInventoryMixRestriction(Long warehouseId, InventoryMixRestriction inventoryMixRestriction) {
        inventoryMixRestriction.setWarehouseId(warehouseId);
        inventoryMixRestriction.getLines().forEach(
                inventoryMixRestrictionLine -> inventoryMixRestrictionLine.setInventoryMixRestriction(
                        inventoryMixRestriction
                )
        );
        return save(inventoryMixRestriction);
    }

    public InventoryMixRestriction changeInventoryMixRestriction(Long warehouseId, InventoryMixRestriction inventoryMixRestriction) {
        inventoryMixRestriction.setWarehouseId(warehouseId);
        inventoryMixRestriction.getLines().forEach(
                inventoryMixRestrictionLine -> inventoryMixRestrictionLine.setInventoryMixRestriction(
                        inventoryMixRestriction
                )
        );
        return save(inventoryMixRestriction);
    }
}