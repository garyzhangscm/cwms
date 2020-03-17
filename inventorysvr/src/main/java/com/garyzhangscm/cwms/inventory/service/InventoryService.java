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
import com.garyzhangscm.cwms.inventory.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.OutbuondServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.GenericException;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventoryRepository;
import com.garyzhangscm.cwms.inventory.repository.ItemRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.PropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.*;
import javax.swing.text.html.HTMLDocument;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
@Service
public class InventoryService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemPackageTypeService itemPackageTypeService;
    @Autowired
    private InventoryStatusService inventoryStatusService;
    @Autowired
    private InventoryMovementService inventoryMovementService;
    @Autowired
    private MovementPathService movementPathService;
    @Autowired
    private InventoryConsolidationService inventoryConsolidationService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.inventories:inventories}")
    String testDataFile;

    public Inventory findById(Long id) {
        return findById(id, true);
    }
    public Inventory findById(Long id, boolean includeDetails) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("inventory not found by id: " + id));
        if (includeDetails) {
            loadInventoryAttribute(inventory);
        }
        return inventory;
    }

    public List<Inventory> findAll() {
        return findAll(true);
    }

    public List<Inventory> findAll(boolean includeDetails) {

        // Only return actual inventory
        List<Inventory> inventories = inventoryRepository.findByVirtual(false);
        if (includeDetails && inventories.size() > 0) {
            loadInventoryAttribute(inventories);
        }
        return inventories;
    }
    public List<Inventory> findAll(Long warehouseId,
                                   String itemName,
                                   String clientIds,
                                   String itemFamilyIds,
                                   Long inventoryStatusId,
                                   String locationName,
                                   Long locationId,
                                   Long locationGroupId,
                                   String receiptId,
                                   String pickIds,
                                   String lpn) {
        return findAll(warehouseId, itemName, clientIds, itemFamilyIds, inventoryStatusId,
                locationName, locationId, locationGroupId, receiptId, pickIds, lpn, true);
    }


    public List<Inventory> findAll(Long warehouseId,
                                   String itemName,
                                   String clientIds,
                                   String itemFamilyIds,
                                   Long inventoryStatusId,
                                   String locationName,
                                   Long locationId,
                                   Long locationGroupId,
                                   String receiptId,
                                   String pickIds,
                                   String lpn,
                                   boolean includeDetails) {
        List<Inventory> inventories =  inventoryRepository.findAll(
                (Root<Inventory> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    if (!StringUtils.isBlank(itemName) || !StringUtils.isBlank(clientIds)) {
                        Join<Inventory, Item> joinItem = root.join("item", JoinType.INNER);
                        if (!itemName.isEmpty()) {
                            predicates.add(criteriaBuilder.equal(joinItem.get("name"), itemName));
                        }

                        if (!clientIds.isEmpty()) {
                            CriteriaBuilder.In<Long> inClientIds = criteriaBuilder.in(joinItem.get("clientId"));
                            for(String id : clientIds.split(",")) {
                                inClientIds.value(Long.parseLong(id));
                            }
                            predicates.add(criteriaBuilder.and(inClientIds));

                        }
                    }
                    if (!StringUtils.isBlank(itemFamilyIds)) {
                        Join<Inventory, Item> joinItem = root.join("item", JoinType.INNER);
                        Join<Item, ItemFamily> joinItemFamily = joinItem.join("itemFamily", JoinType.INNER);

                        CriteriaBuilder.In<Long> inItemFamilyIds = criteriaBuilder.in(joinItemFamily.get("id"));
                        for(String id : itemFamilyIds.split(",")) {
                            inItemFamilyIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inItemFamilyIds));
                    }
                    if (inventoryStatusId != null) {
                        Join<Inventory, InventoryStatus> joinInventoryStatus = root.join("inventoryStatus", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinInventoryStatus.get("id"), inventoryStatusId));

                    }

                    // if location ID is passed in, we will only filter by location id, no matter whether
                    // the location name is passed in or not
                    // otherwise, we will try to filter by location name
                    if (locationId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("locationId"), locationId));
                    }
                    else if (!StringUtils.isBlank(locationName) &&
                            warehouseId != null) {
                        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(warehouseId, locationName);
                        if (location != null) {
                            predicates.add(criteriaBuilder.equal(root.get("locationId"), location.getId()));
                        }
                    }
                    if (!StringUtils.isBlank(receiptId)) {
                        predicates.add(criteriaBuilder.equal(root.get("receiptId"), receiptId));

                    }
                    if (!StringUtils.isBlank(pickIds)) {
                        CriteriaBuilder.In<Long> inClause = criteriaBuilder.in(root.get("pickId"));
                        Arrays.stream(pickIds.split(",")).map(Long::parseLong).forEach(pickId -> inClause.value(pickId));
                        predicates.add(inClause);

                    }
                    if (!StringUtils.isBlank(lpn)) {
                        predicates.add(criteriaBuilder.equal(root.get("lpn"), lpn));

                    }


                    // Only return actual inventory
                    predicates.add(criteriaBuilder.equal(root.get("virtual"), false));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (includeDetails && inventories.size() > 0) {
            loadInventoryAttribute(inventories);
        }

        // When location group id is passed in, we will only return inventory from this location group
        if (locationGroupId != null) {
            List<Location> locations = warehouseLayoutServiceRestemplateClient.getLocationByLocationGroups(String.valueOf(locationGroupId));
            // convert the list of locations to map of Long so as to speed up
            // when compare the inventory's location id with the locations from the group
            Map<Long, Long> locationMap = new HashMap<>();
            locations.stream().forEach(location -> locationMap.put(location.getId(), location.getId()));

            return inventories.stream().filter(inventory -> locationMap.containsKey(inventory.getLocationId())).collect(Collectors.toList());
        }
        return inventories;
    }

    public List<Inventory> findPendingInventoryByLocationId(Long locationId) {
        return inventoryRepository.findPendingInventoryByLocationId(locationId);
    }

    public List<Inventory> findPickableInventories(Long itemId, Long inventoryStatusId) {
        return findPickableInventories(itemId, inventoryStatusId, true);
    }
    public List<Inventory> findPickableInventories(Long itemId,
                                                   Long inventoryStatusId,
                                                   boolean includeDetails) {

        List<Inventory> inventories =  inventoryRepository.findByItemIdAndInventoryStatusId(itemId, inventoryStatusId);

        if (includeDetails && inventories.size() > 0) {
            loadInventoryAttribute(inventories);
        }

        // Make sure the location is pickable

        return inventories.stream().filter(this::isInventoryPickable).collect(Collectors.toList());
    }


    // CHeck if the inventory is in a pickable location
    private boolean isInventoryPickable(Inventory inventory) {
        return inventory.getLocation().getEnabled() == true &&
                inventory.getLocation().getLocationGroup().getPickable() == true &&
                inventory.getLocation().getLocationGroup().getLocationGroupType().getFourWallInventory() == true;
    }


    public List<Inventory> findByLpn(String lpn){
        return findByLpn(lpn,true);
    }
    public List<Inventory> findByLpn(String lpn, boolean includeDetails){
        List<Inventory> inventories = inventoryRepository.findByLpn(lpn);
        if (inventories != null && includeDetails) {
            loadInventoryAttribute(inventories);
        }
        return inventories;
    }

    public List<Inventory> findByLocationId(Long locationId, boolean includeDetails) {
        List<Inventory> inventories =  inventoryRepository.findByLocationId(locationId);

        if (includeDetails && inventories.size() > 0) {
            loadInventoryAttribute(inventories);
        }
        return inventories;
    }

    public List<Inventory> findByLocationId(Long locationId) {
        return findByLocationId(locationId, true);
    }

    public Inventory save(Inventory inventory) {
        Inventory savedInventory = inventoryRepository.save(inventory);
        // reset location's status and volume
        warehouseLayoutServiceRestemplateClient.resetLocation(inventory.getLocationId());

        // loadInventoryAttribute(savedInventory);

        return savedInventory;
    }

    public Inventory saveOrUpdate(Inventory inventory) {
        if (inventory.getId() == null && findByLpn(inventory.getLpn()).size() == 1) {
            inventory.setId(findByLpn(inventory.getLpn()).get(0).getId());
        }
        return save(inventory);
    }
    public void delete(Inventory inventory) {
        inventoryRepository.delete(inventory);
    }
    public void delete(Long id) {
        inventoryRepository.deleteById(id);
    }
    public void delete(String inventoryIds) {
        if (!inventoryIds.isEmpty()) {
            long[] inventoryIdArray = Arrays.asList(inventoryIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : inventoryIdArray) {
                delete(id);
            }
        }
    }

    public void loadInventoryAttribute(List<Inventory> inventories) {
        for(Inventory inventory : inventories) {
            loadInventoryAttribute(inventory);
        }
    }

    public void loadInventoryAttribute(Inventory inventory) {

        // Load location information
        if (inventory.getLocationId() != null) {
            inventory.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(inventory.getLocationId()));
        }

        if (inventory.getInventoryMovements() != null && inventory.getInventoryMovements().size() > 0) {
            inventory.getInventoryMovements().forEach(
                    inventoryMovement -> {
                        if (inventoryMovement.getLocation() == null && inventoryMovement.getLocationId() != null) {
                            inventoryMovement.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(inventoryMovement.getLocationId()));
                        }
                    }
            );
        }

        // load the unit of measure details for the packate types
        inventory.getItemPackageType().getItemUnitOfMeasures().forEach(itemUnitOfMeasure ->
                itemUnitOfMeasure.setUnitOfMeasure(commonServiceRestemplateClient.getUnitOfMeasureById(itemUnitOfMeasure.getUnitOfMeasureId())));

        if (inventory.getPickId() != null) {
            inventory.setPick(outbuondServiceRestemplateClient.getPickById(inventory.getPickId()));
        }




    }

    public List<Inventory> loadInventoryData(File  file) throws IOException {
        List<InventoryCSVWrapper> inventoryCSVWrappers = loadData(file);
        return inventoryCSVWrappers.stream().map(inventoryCSVWrapper -> convertFromWrapper(inventoryCSVWrapper)).collect(Collectors.toList());
    }

    public List<InventoryCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("lpn").
                addColumn("location").
                addColumn("item").
                addColumn("itemPackageType").
                addColumn("quantity").
                addColumn("inventoryStatus").
                build().withHeader();
        return fileService.loadData(file, schema, InventoryCSVWrapper.class);
    }


    public List<InventoryCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("lpn").
                addColumn("location").
                addColumn("item").
                addColumn("itemPackageType").
                addColumn("quantity").
                addColumn("inventoryStatus").
                build().withHeader();

        return fileService.loadData(inputStream, schema, InventoryCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<InventoryCSVWrapper> inventoryCSVWrappers = loadData(inputStream);
            inventoryCSVWrappers.stream().forEach(inventoryCSVWrapper -> {
                Inventory savedInvenotry = saveOrUpdate(convertFromWrapper(inventoryCSVWrapper));
                // re-calculate the size of the location

                Location destination =
                        warehouseLayoutServiceRestemplateClient.getLocationByName(
                                getWarehouseId(inventoryCSVWrapper.getWarehouse()), inventoryCSVWrapper.getLocation()
                        );
                recalculateLocationSizeForInventoryMovement(null, destination, savedInvenotry.getSize());
            });
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private Inventory convertFromWrapper(InventoryCSVWrapper inventoryCSVWrapper) {
        Inventory inventory = new Inventory();
        inventory.setLpn(inventoryCSVWrapper.getLpn());
        inventory.setQuantity(inventoryCSVWrapper.getQuantity());
        inventory.setVirtual(false);

        // warehouse is a mandate field
        Warehouse warehouse =
                    warehouseLayoutServiceRestemplateClient.getWarehouseByName(inventoryCSVWrapper.getWarehouse());
        inventory.setWarehouseId(warehouse.getId());

        // item
        if (!inventoryCSVWrapper.getItem().isEmpty()) {
            inventory.setItem(itemService.findByName(warehouse.getId(), inventoryCSVWrapper.getItem()));
        }

        // itemPackageType
        if (!inventoryCSVWrapper.getItemPackageType().isEmpty() &&
                !inventoryCSVWrapper.getItem().isEmpty()) {
            inventory.setItemPackageType(
                    itemPackageTypeService.findByNaturalKeys(
                            warehouse.getId(),
                            inventoryCSVWrapper.getItemPackageType(),
                            inventoryCSVWrapper.getItem()));
        }

        // inventoryStatus
        if (!inventoryCSVWrapper.getInventoryStatus().isEmpty()) {
            inventory.setInventoryStatus(inventoryStatusService.findByName(inventoryCSVWrapper.getInventoryStatus()));
        }

        // location
        if (!inventoryCSVWrapper.getLocation().isEmpty()) {
            logger.debug("start to get location by name: {}", inventoryCSVWrapper.getLocation());
            Location location =
                    warehouseLayoutServiceRestemplateClient.getLocationByName(
                            getWarehouseId(inventoryCSVWrapper.getWarehouse()), inventoryCSVWrapper.getLocation());
            inventory.setLocationId(location.getId());
        }

        return inventory;

    }

    // To remove inventory, we won't actually remove the record.
    // Instead we move the inventory to a 'logical' location
    public void removeInventory(Long id, Location destination) {
        Inventory inventory = findById(id);
        if (inventory != null) {
            moveInventory(inventory, destination);
        }
    }
    public void removeInventory(Inventory inventory, Location destination) {
        if (inventory != null) {
            moveInventory(inventory, destination);
        }
    }

    public Inventory moveInventory(Long inventoryId, Location destination) {
        return moveInventory(findById(inventoryId), destination, null);

    }

    public Inventory moveInventory(Inventory inventory, Location destination) {
        return moveInventory(inventory, destination, null);
    }
    public Inventory moveInventory(Long inventoryId, Location destination, Long pickId) {
        return moveInventory(findById(inventoryId), destination, pickId);

    }

    @Transactional
    public Inventory moveInventory(Inventory inventory, Location destination, Long pickId) {
        logger.debug("Start to move inventory {} to destination {}, pickId: {} is null? {}",
                inventory.getLpn(), destination.getName(), pickId, Objects.isNull(pickId));

        Location sourceLocation = inventory.getLocation();
        inventory.setLocationId(destination.getId());
        if (!Objects.isNull(pickId)) {
            markAsPicked(inventory, pickId);
        }

        // we will need to get the destination location's location group
        // so we can know whether the inventory become a virtual inventory
        if (destination.getLocationGroup() == null || destination.getLocationGroup().getLocationGroupType() == null) {
            // Refresh the location's information from layout service
            destination = warehouseLayoutServiceRestemplateClient.getLocationById(destination.getId());
        }
        if (destination.getLocationGroup().getLocationGroupType().getVirtual()) {
            // The inventory is moved to the virtual location, let's mark the inventory
            // as virtual
            inventory.setVirtual(true);
        }
        else {

            inventory.setVirtual(false);
        }
        // Check if we have finished any movement
        recalculateMovementPathForInventoryMovement(inventory, destination);

        // Reset the destination location's size
        recalculateLocationSizeForInventoryMovement(sourceLocation, destination, inventory.getSize());

        // consolidate the inventory at the destination, if necessary
        Inventory consolidatedInventory = inventoryConsolidationService.consolidateInventoryAtLocation(destination, inventory);

        logger.debug("6. destination {} has {} inventory",
                destination.getName(), findByLocationId(destination.getId()).size());
        // check if we will need to remove the original inventory
        if (!Objects.isNull(inventory.getId()) &&
            !consolidatedInventory.equals(inventory)) {
            // the original inventory is already persist but
            // is consolidated into an existing inventory, let's
            // remove it from the db
            delete(inventory);
        }
        logger.debug("7. destination {} has {} inventory",
                destination.getName(), findByLocationId(destination.getId()).size());
        /***
        logger.debug("Location {}'s  consolidate LPN policy: ",
                destination.getName(), destination.getLocationGroup().getConsolidateLpn());
        if (destination.getLocationGroup().getConsolidateLpn() == true) {
            // check if we can consolidate the inventory with LPN that
            // already in the location
            consolidateLpn(inventory, destination);
            logger.debug("LPN consolidated! Now inventory has new LPN {}", inventory.getLpn());
        }
         ***/
        return save(consolidatedInventory);
    }
    /****
    private void consolidateLpn(Inventory inventory, Location destination) {
        // see if there's already LPN in the location
        List<Inventory> inventories = findByLocationId(destination.getId());
        List<String> existingLPN =
                    inventories.stream()
                        .filter(existingInventory -> existingInventory.getLpn() != inventory.getLpn())
                            .map(existingInventory -> existingInventory.getLpn())
                            .distinct().collect(Collectors.toList());
        logger.debug("There's already {} existing LPN in the location",
                existingLPN.size());
        if (existingLPN.size() > 1) {
            throw new GenericException(10000, "There's more than one LPN to be consolidated into");
        }
        else if (existingLPN.size() == 1) {
            inventory.setLpn(existingLPN.get(0));
        }
    }
     ***/
    private void recalculateLocationSizeForInventoryMovement(Location sourceLocation, Location destination, double volume) {
        if (sourceLocation != null && sourceLocation.getLocationGroup().getTrackingVolume()) {
            logger.debug("re-calculate the source location {} 's size by reduce {}",
                    sourceLocation.getName(), volume);
            warehouseLayoutServiceRestemplateClient.reduceLocationVolume(sourceLocation.getId(), volume);
        }
        if (destination != null && destination.getLocationGroup().getTrackingVolume()) {
            logger.debug("re-calculate the destination location {} 's size by increase {}",
                    destination.getName(), volume);
            warehouseLayoutServiceRestemplateClient.increaseLocationVolume(destination.getId(), volume);
        }
    }
    private void recalculateMovementPathForInventoryMovement(Inventory inventory, Location destination) {

        List<InventoryMovement> matchedMovements = inventory.getInventoryMovements().stream()
                         .filter(inventoryMovement ->  inventoryMovement.getLocationId().equals(destination.getId()))
                         .collect(Collectors.toList());
        if (matchedMovements.size() == 1) {
            // Ok we moved inventory to some hop location
            // let's remove all the locations(including the hop location) from the
            // movement path
            InventoryMovement matchedMovement = matchedMovements.get(0);

            inventory.getInventoryMovements().stream().forEach(inventoryMovement -> {
                if (inventoryMovement.getSequence() <= matchedMovement.getSequence()) {
                    logger.debug("Will remove movement path {} from inventory {}", matchedMovements.size(), inventory.getLpn());
                    inventoryMovementService.removeInventoryMovement(inventoryMovement.getId(), inventory);
                }
            });

            // Reload the inventory movement for the inventory
            inventory.setInventoryMovements(inventoryMovementService.findByInventoryId(inventory.getId()));
            logger.debug("After we removed all the movement pre matched movement, we have left");
            inventory.getInventoryMovements()
                    .stream().forEach(inventoryMovement -> logger.debug("{} - {}", inventoryMovement.getSequence(), inventoryMovement.getLocation().getName()));

        }
        else if (matchedMovements.size() == 0 &&
                inventory.getInventoryMovements().size() > 0 &&
                destination.getLocationGroup().getStorable() == true
        ) {
            // OK, the inventory was moved into a location that is not defined as a hop location
            // of the inventory. If the location is a P&D location, we will still keep
            // inventory movement path. If the location is a storage location, then we will
            // remove all the inventory movement path

            inventory.getInventoryMovements().stream().forEach(inventoryMovement ->
                    inventoryMovementService.removeInventoryMovement(inventoryMovement.getId(), inventory)
            );

            // We should have removed all the movements
            inventory.setInventoryMovements(new ArrayList<>());
        }

    }

    public void adjustDownInventory(Long id, Long warehouseId) {
        removeInventory(id, warehouseLayoutServiceRestemplateClient.getLocationForInventoryAdjustment(warehouseId));
    }


    // Note here the movement path only contains final destination location. We will need to get the
    // hop location as well and save it
    public Inventory setupMovementPath(Long inventoryId, List<InventoryMovement> inventoryMovements) {

        Inventory inventory = findById(inventoryId);
        logger.debug("start to setup movement path for inventory lpn: {}", inventory.getLpn());
        logger.debug("The inventory has {} movement defined", inventoryMovements.size());
        logger.debug(" >> {}", inventoryMovements);

        if (inventoryMovements.size() == 1) {
            // we only have the final destination. let's see if we need
            // any hop
            InventoryMovement finalDestinationMove = inventoryMovements.get(0);
            if (finalDestinationMove.getLocationId() == null && finalDestinationMove.getLocation() != null) {
                finalDestinationMove.setLocationId(finalDestinationMove.getLocation().getId());
            }
            logger.debug("Start to get hop locations for this movement. current location id: {}, final destination id: {}", inventory.getLocationId(), finalDestinationMove.getLocationId());
            List<Location> hopLocations = movementPathService.reserveHopLocations(inventory.getLocationId(), finalDestinationMove.getLocationId(), inventory);
            int sequence = 0;
            for(Location location : hopLocations) {
                InventoryMovement inventoryMovement = new InventoryMovement();
                inventoryMovement.setLocation(location);
                inventoryMovement.setLocationId(location.getId());
                inventoryMovement.setSequence(sequence++);
                inventoryMovement.setInventory(inventory);
                inventoryMovements.add(inventoryMovement);
            }

            // Reset the final move's sequence to the last one in the movement path
            finalDestinationMove.setSequence(sequence);

            logger.debug("After setup the hop locations, now the inventory has {} movement defined", inventoryMovements.size());
            logger.debug(" >> {}", inventoryMovements);

        }


        // Sort by sequence so we can save record by sequence
        inventoryMovements.sort(Comparator.comparingInt(InventoryMovement::getSequence));

        logger.debug("Start to save movement for inventory: {}", inventory.getId());
        // logger.debug(">> movement path: {}", inventoryMovements);
        inventoryMovements.forEach(inventoryMovement -> {
            inventoryMovement.setInventory(inventory);
            inventoryMovement.setWarehouseId(inventory.getWarehouseId());
            if (inventoryMovement.getLocationId() == null && inventoryMovement.getLocation() != null) {
                inventoryMovement.setLocationId(inventoryMovement.getLocation().getId());
            }
        });
        logger.debug(">> movement path: {}", inventoryMovements);
        List<InventoryMovement> newInventoryMovements = inventoryMovementService.save(inventoryMovements);
        logger.debug(">> reload inventory informaiton after movement path is saved!");

        // Setup the movement path for the inventory so we can return the
        // latest information
        // Note: We may not be able to get the latest movement path by call findById() here
        // as the change may not be serialized yet
        // Also we will need to call clear() and addAll() instead of setInventoryMovements
        // to setup the inventory's movements
        inventory.getInventoryMovements().clear();
        inventory.getInventoryMovements().addAll(newInventoryMovements);
        logger.debug("Will return following movement path to the end user: {}", inventory.getInventoryMovements());
        return inventory;
    }

    public Inventory markAsPicked(Long inventoryId, Long pickId) {
        return markAsPicked(findById(inventoryId), pickId);
    }
    public Inventory markAsPicked(Inventory inventory, Long pickId) {
        inventory.setPickId(pickId);
        if (inventory.getInventoryMovements().size() == 0) {
            // copy the pick movement and assign it to the inventory's movement
            Pick pick = outbuondServiceRestemplateClient.getPickById(pickId);
            copyMovementsFromPick(pick, inventory);
        }
        return inventory;
    }
    private void copyMovementsFromPick(Pick pick, Inventory inventory) {

        List<InventoryMovement> inventoryMovements = new ArrayList<>();
        pick.getPickMovements().stream().forEach(pickMovement -> {
            inventoryMovements.add(inventoryMovementService.createInventoryMovementFromPickMovement(inventory, pickMovement));
        });

        // Note the pick's movement path only contains the hop locations. We will need to set
        // the inventory's movement to include the final destination as well
        inventoryMovements.add(inventoryMovementService.createInventoryMovement(inventory, pick.getDestinationLocationId()));
        inventory.setInventoryMovements(inventoryMovements);
    }

    // Split the inventory based on the quantity, usually into 2 inventory.
    // the first one in the list is the original inventory with updated quantity
    // the second one in the list is the new inventory
    public List<Inventory> splitInventory(long id, String newLpn, Long newQuantity) {
        return splitInventory(findById(id), newLpn, newQuantity);
    }

    // Split the inventory based on the quantity, usually into 2 inventory.
    // the first one in the list is the original inventory with updated quantity
    // the second one in the list is the new inventory
    public List<Inventory> splitInventory(Inventory inventory, String newLpn, Long newQuantity) {
        Inventory newInventory = inventory.split(newLpn, newQuantity);
        List<Inventory> inventories = new ArrayList<>();
        inventories.add(saveOrUpdate(inventory));
        inventories.add(saveOrUpdate(newInventory));
        return inventories;
    }

    public List<Inventory> unpick(String lpn) {
        List<Inventory> inventories = findByLpn(lpn);
        List<Inventory> unpickedInventory = new ArrayList<>();
        return inventories.stream().map(this::unpick).collect(Collectors.toList());
    }

    public Inventory unpick(Long id) {
        return unpick(findById(id));

    }

    public Inventory unpick(Inventory inventory) {
        if (inventory.getPickId() == null) {
            // The inventory is not a picked inventory
            return inventory;
        }

        // update the pick
        outbuondServiceRestemplateClient.unpick(inventory.getPickId(), inventory.getQuantity());

        // disconnect the inventory from the pick and
        // clear all the movement path
        inventory.setPickId(null);
        inventoryMovementService.clearInventoryMovement(inventory);

        return saveOrUpdate(inventory);

    }

    private Long getWarehouseId(String warehouseName) {
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(warehouseName);
        if (warehouse == null) {
            return null;
        }
        else {
            return warehouse.getId();
        }
    }


    /**
     * Add new invenotry to the system, To make it easy, we will always create the inventory structure in a temporary location
     *  based on the inventoryQuantityChangeType and then move the inventory to the destination. So all the complicated logic
     *  will be handled in the 'move inventory' routine
     * @param inventory : Inventory to be added
     * @param inventoryQuantityChangeType: Action type: RECEIVING / INVENTORY ADJUST / COUNT / etc.
     * @return Inventory being added
     */
    @Transactional
    public Inventory addInventory(Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType) {

        Location location =
                warehouseLayoutServiceRestemplateClient.getLogicalLocationForAddingInventory(
                        inventoryQuantityChangeType,inventory.getWarehouseId());
        // consolidate the inventory at the destination, if necessary
        Location destinationLocation = inventory.getLocation();

        // create the inventory at the logic location
        // then move the inventory to the final location
        inventory.setLocation(location);
        inventory.setLocationId(location.getId());
        inventory.setVirtual(warehouseLayoutServiceRestemplateClient.isVirtualLocation(location));

        inventory = saveOrUpdate(inventory);

        return moveInventory(inventory, destinationLocation);


        /***********

        Inventory consolidatedInventory = inventoryConsolidationService.consolidateInventoryAtLocation(inventory.getLocation(), inventory);

        // check if we will need to remove the original inventory
        if (!Objects.isNull(inventory.getId()) &&
                !consolidatedInventory.equals(inventory)) {
            // the original inventory is already persist but
            // is consolidated into an existing inventory, let's
            // remove it from the db
            delete(inventory);
        }
        if (inventory.getVirtual() == null) {
            inventory.setVirtual(warehouseLayoutServiceRestemplateClient.isVirtualLocation(inventory.getLocation()));
        }
        return save(consolidatedInventory);
        *********/
    }

}
