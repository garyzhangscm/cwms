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
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
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

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.inventories:inventories.csv}")
    String testDataFile;

    public Inventory findById(Long id) {
        return findById(id, true);
    }
    public Inventory findById(Long id, boolean includeDetails) {
        Inventory inventory = inventoryRepository.findById(id).orElse(null);
        logger.debug("load details? {} for inventory id: {}", includeDetails, id);
        if (includeDetails && inventory != null) {
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
    public List<Inventory> findAll(String itemName,
                                   String clientIds,
                                   String itemFamilyIds,
                                   String locationName,
                                   String receiptId) {
        return findAll(itemName, clientIds, itemFamilyIds, locationName, receiptId, true);
    }


    public List<Inventory> findAll(String itemName,
                                   String clientIds,
                                   String itemFamilyIds,
                                   String locationName,
                                   String receiptId,
                                   boolean includeDetails) {
        List<Inventory> inventories =  inventoryRepository.findAll(
                (Root<Inventory> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    if (!itemName.isEmpty() || !clientIds.isEmpty()) {
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
                    if (!itemFamilyIds.isEmpty()) {
                        Join<Inventory, Item> joinItem = root.join("item", JoinType.INNER);
                        Join<Item, ItemFamily> joinItemFamily = joinItem.join("itemFamily", JoinType.INNER);

                        CriteriaBuilder.In<Long> inItemFamilyIds = criteriaBuilder.in(joinItemFamily.get("id"));
                        for(String id : itemFamilyIds.split(",")) {
                            inItemFamilyIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inItemFamilyIds));
                    }
                    if (!locationName.isEmpty()) {
                        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(locationName);
                        if (location != null) {
                            predicates.add(criteriaBuilder.equal(root.get("locationId"), location.getId()));
                        }
                    }
                    if (!StringUtils.isBlank(receiptId)) {
                        predicates.add(criteriaBuilder.equal(root.get("receiptId"), receiptId));

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
        return inventories;
    }


    public Inventory findByLpn(String lpn){
        return findByLpn(lpn,true);
    }
    public Inventory findByLpn(String lpn, boolean includeDetails){
        Inventory inventory = inventoryRepository.findByLpn(lpn);
        if (inventory != null && includeDetails) {
            loadInventoryAttribute(inventory);
        }
        return inventory;
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
        return savedInventory;
    }

    public Inventory saveOrUpdate(Inventory inventory) {
        if (inventory.getId() == null && findByLpn(inventory.getLpn()) != null) {
            inventory.setId(findByLpn(inventory.getLpn()).getId());
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


        logger.debug("inventory.getInventoryMovements() == null? " + (inventory.getInventoryMovements() == null));
        logger.debug("inventory.getInventoryMovements().size()? " + (inventory.getInventoryMovements() == null ? 0 : inventory.getInventoryMovements().size()));
        logger.debug("inventory.getInventoryMovements()? " + (inventory.getInventoryMovements() == null ? "" : inventory.getInventoryMovements()));
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


    }

    public List<Inventory> loadInventoryData(File  file) throws IOException {
        List<InventoryCSVWrapper> inventoryCSVWrappers = loadData(file);
        return inventoryCSVWrappers.stream().map(inventoryCSVWrapper -> convertFromWrapper(inventoryCSVWrapper)).collect(Collectors.toList());
    }

    public List<InventoryCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
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
                addColumn("lpn").
                addColumn("location").
                addColumn("item").
                addColumn("itemPackageType").
                addColumn("quantity").
                addColumn("inventoryStatus").
                build().withHeader();

        return fileService.loadData(inputStream, schema, InventoryCSVWrapper.class);
    }

    public void initTestData() {
        try {
            InputStream inputStream = new ClassPathResource(testDataFile).getInputStream();
            List<InventoryCSVWrapper> inventoryCSVWrappers = loadData(inputStream);
            inventoryCSVWrappers.stream().forEach(inventoryCSVWrapper -> saveOrUpdate(convertFromWrapper(inventoryCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private Inventory convertFromWrapper(InventoryCSVWrapper inventoryCSVWrapper) {
        Inventory inventory = new Inventory();
        inventory.setLpn(inventoryCSVWrapper.getLpn());
        inventory.setQuantity(inventoryCSVWrapper.getQuantity());
        inventory.setVirtual(false);


        // item
        if (!inventoryCSVWrapper.getItem().isEmpty()) {
            inventory.setItem(itemService.findByName(inventoryCSVWrapper.getItem()));
        }

        // itemPackageType
        if (!inventoryCSVWrapper.getItemPackageType().isEmpty() &&
                !inventoryCSVWrapper.getItem().isEmpty()) {
            inventory.setItemPackageType(itemPackageTypeService.findByNaturalKeys(inventoryCSVWrapper.getItemPackageType(), inventoryCSVWrapper.getItem()));
        }

        // inventoryStatus
        if (!inventoryCSVWrapper.getInventoryStatus().isEmpty()) {
            inventory.setInventoryStatus(inventoryStatusService.findByName(inventoryCSVWrapper.getInventoryStatus()));
        }

        // location
        if (!inventoryCSVWrapper.getLocation().isEmpty()) {
            logger.debug("start to get location by name: {}", inventoryCSVWrapper.getLocation());
            Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(inventoryCSVWrapper.getLocation());
            inventory.setLocationId(location.getId());
        }

        return inventory;

    }

    public void removeInventory(Long id) {
        removeInventory(id, warehouseLayoutServiceRestemplateClient.getDefaultRemovedInventoryLocation());
    }

    public void removeInventory(Inventory inventory) {
        removeInventory(inventory, warehouseLayoutServiceRestemplateClient.getDefaultRemovedInventoryLocation());
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

    public void moveInventory(Inventory inventory, Location destination) {
        inventory.setLocationId(destination.getId());
        if (destination.getLocationGroup().getLocationGroupType().getVirtual()) {
            // The inventory is moved to the virtual location, let's mark the inventory
            // as virtual
            inventory.setVirtual(true);
        }
        save(inventory);
    }

    public void adjustDownInventory(Long id) {
        removeInventory(id, warehouseLayoutServiceRestemplateClient.getLocationForInventoryAdjustment());
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

            // Reset the final move's sequence to 0
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
}
