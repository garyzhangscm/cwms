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
import com.garyzhangscm.cwms.inventory.exception.MissingInformationException;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventoryRepository;
import org.apache.commons.lang.StringUtils;
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
    private InventoryActivityService inventoryActivityService;
    @Autowired
    private InventoryAdjustmentThresholdService inventoryAdjustmentThresholdService;
    @Autowired
    private InventoryAdjustmentRequestService inventoryAdjustmentRequestService;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;
    @Autowired
    private IntegrationService integrationService;
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
                                   Long workOrderId,
                                   String workOrderLineIds,
                                   String workOrderByProductIds,
                                   String pickIds,
                                   String lpn) {
        return findAll(warehouseId, itemName, clientIds, itemFamilyIds, inventoryStatusId,
                locationName, locationId, locationGroupId, receiptId, workOrderId, workOrderLineIds,
                workOrderByProductIds,
                pickIds, lpn, true);
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
                                   Long workOrderId,
                                   String workOrderLineIds,
                                   String workOrderByProductIds,
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

                    if (Objects.nonNull(workOrderId)) {
                        predicates.add(criteriaBuilder.equal(root.get("workOrderId"), workOrderId));

                    }


                    if (!StringUtils.isBlank(workOrderLineIds)) {
                        CriteriaBuilder.In<Long> inClause = criteriaBuilder.in(root.get("workOrderLineId"));
                        Arrays.stream(workOrderLineIds.split(","))
                                .map(Long::parseLong).forEach(workOrderLineId -> inClause.value(workOrderLineId));
                        predicates.add(inClause);

                    }
                    if (!StringUtils.isBlank(workOrderByProductIds)) {
                        CriteriaBuilder.In<Long> inClause = criteriaBuilder.in(root.get("workOrderByProductId"));
                        Arrays.stream(workOrderByProductIds.split(","))
                                .map(Long::parseLong).forEach(workOrderByProductId -> inClause.value(workOrderByProductId));
                        predicates.add(inClause);

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
            List<Location> locations =
                    warehouseLayoutServiceRestemplateClient.getLocationByLocationGroups(
                            warehouseId, String.valueOf(locationGroupId));
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
        // logger.debug("Find inventory \n{}\n from location ID: {}",
        //        inventories, locationId);
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
        // logger.debug("Start to load item unit of measure for item package type: {}",
        //         inventory.getItemPackageType());
        inventory.getItemPackageType().getItemUnitOfMeasures().forEach(itemUnitOfMeasure -> {
            // logger.debug(">> Load information for item unit of measure: {}", itemUnitOfMeasure);
            itemUnitOfMeasure.setUnitOfMeasure(commonServiceRestemplateClient.getUnitOfMeasureById(itemUnitOfMeasure.getUnitOfMeasureId()));
        });

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
            inventoryRepository.flush();
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
            logger.debug("will set inventory status: {} / {}",
                    warehouse.getId(),
                    inventoryCSVWrapper.getInventoryStatus());
            inventory.setInventoryStatus(inventoryStatusService.findByName(
                    warehouse.getId(), inventoryCSVWrapper.getInventoryStatus()));
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
    public Inventory removeInventory(Long id, String documentNumber, String comment) {
        Inventory inventory = findById(id);
        return removeInventory(inventory, documentNumber, comment);
    }
    public Inventory removeInventory(Inventory inventory, String documentNumber, String comment) {
        return removeInventory(inventory, InventoryQuantityChangeType.INVENTORY_ADJUST, documentNumber, comment);
    }

    public Inventory removeInventory(Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType) {
        return removeInventory(inventory, inventoryQuantityChangeType, "", "");

    }
    public Inventory removeInventory(Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType,
                                            String documentNumber, String comment) {

        logger.debug("Start to remove inventory");
        if (isApprovalNeededForInventoryAdjust(inventory, 0L, inventoryQuantityChangeType)) {

            logger.debug("We will need to get approval, so here we just save the request");
            writeInventoryAdjustRequest(inventory, 0L,
                    inventoryQuantityChangeType, documentNumber, comment);
            return inventory;
        } else {
            logger.debug("No approval needed, let's just go ahread with the adding inventory!");
            return processRemoveInventory(inventory, inventoryQuantityChangeType, documentNumber, comment);
        }
    }
    public Inventory processRemoveInventory(Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType,
                                     String documentNumber, String comment) {

        InventoryActivityType inventoryActivityType;
        switch (inventoryQuantityChangeType) {
            case CYCLE_COUNT:
                inventoryActivityType = InventoryActivityType.CYCLE_COUNT;
                break;
            case AUDIT_COUNT:
                inventoryActivityType = InventoryActivityType.AUDIT_COUNT;
                break;
            case CONSUME_MATERIAL:
                inventoryActivityType = InventoryActivityType.WORK_ORDER_CONSUME;
                break;
            default:
                inventoryActivityType = InventoryActivityType.INVENTORY_ADJUSTMENT;
        }
        inventoryActivityService.logInventoryActivitiy(inventory, inventoryActivityType,
                "quantity", String.valueOf(inventory.getQuantity()), "0",
                documentNumber, comment);

        // ignore the integration when it is consumption of work order material
        if (!inventoryQuantityChangeType.equals(InventoryQuantityChangeType.CONSUME_MATERIAL)) {

            integrationService.processInventoryAdjustment(InventoryQuantityChangeType.INVENTORY_ADJUST, inventory, inventory.getQuantity(), 0L);
        }

        Location destination
                = warehouseLayoutServiceRestemplateClient.getLogicalLocationForAdjustInventory(
                inventoryQuantityChangeType, inventory.getWarehouseId()
        );
        return moveInventory(inventory, destination);
    }

    public Inventory moveInventory(Inventory inventory, Location destination) {
        return moveInventory(inventory, destination, null, true, null);
    }
    public Inventory moveInventory(Long inventoryId, Location destination, Long pickId, boolean immediateMove, String destinationLpn) {
        return moveInventory(findById(inventoryId), destination, pickId, immediateMove, destinationLpn);

    }

    /**
     * Move the inventory into the new location, either immediately confirm the movement, or generate a work
     * so we can assign the movement work to someone to finish it.
     * @param inventory Inventory to be moved
     * @param destination destination fo the inventory
     * @param pickId pick work related to the inventory movement
     * @param immediateMove if we generate work queue or immediately move the inventory
     * @return inventory after the movement(can be consolidated with the existing invenotry in the destination)
     */
    @Transactional
    public Inventory moveInventory(Inventory inventory, Location destination, Long pickId, boolean immediateMove, String destinationLpn) {
        logger.debug("Start to move inventory {} to destination {}, pickId: {} is null? {}, new LPN? {}",
                inventory.getLpn(),
                destination.getName(),
                pickId,
                Objects.isNull(pickId),
                destinationLpn);

        logger.debug("==> Before the inventory move, the destination location {} 's volume is {}",
                warehouseLayoutServiceRestemplateClient.getLocationById(destination.getId()).getName(),
                warehouseLayoutServiceRestemplateClient.getLocationById(destination.getId()).getCurrentVolume());


        // If we passed in a destination LPN, Let's replace the LPN first
        // before the same location check.
        if (StringUtils.isNotBlank(destinationLpn) && !destinationLpn.equals(inventory.getLpn())) {
            inventory.setLpn(destinationLpn);

        }
        Location sourceLocation = inventory.getLocation();

        // quick check. If the source location equals the
        // destination, then we don't have to move
        if (sourceLocation.equals(destination)) {
            return saveOrUpdate(inventory);
        }
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

        // Log a inventory movement activity before we can log
        // another possible inventory consolidation activity

        inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.INVENTORY_MOVEMENT,
                                        "location", sourceLocation.getName(), destination.getName());

        if (Objects.nonNull(inventory.getPickId())) {
            outbuondServiceRestemplateClient.refreshPickMovement(inventory.getPickId(), destination.getId(), inventory.getQuantity());
        }
        // consolidate the inventory at the destination, if necessary
        Inventory consolidatedInventory = inventoryConsolidationService.consolidateInventoryAtLocation(destination, inventory);

        logger.debug("6. destination {} has {} inventory",
                destination.getName(), findByLocationId(destination.getId()).size());
        // check if we will need to remove the original inventory

        // logger.debug(">> after consolidation, the original inventory is \n>> {}", inventory);
        // logger.debug(">> Objects.nonNull(inventory.getId()):  {}", Objects.nonNull(inventory.getId()) );
        // logger.debug(">> inventory.getQuantity(): {}", inventory.getQuantity());

        if (Objects.nonNull(inventory.getId()) &&
            inventory.getQuantity() == 0) {
            // the original inventory is already persist but
            // is consolidated into an existing inventory, let's
            // remove it from the db
            logger.debug(">> after consolidation, we will remove the original inventory");
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


        logger.debug("==> after the inventory move, the destination location {} 's volume is {}",
                warehouseLayoutServiceRestemplateClient.getLocationById(destination.getId()).getName(),
                warehouseLayoutServiceRestemplateClient.getLocationById(destination.getId()).getCurrentVolume());
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

        if (Objects.isNull(inventory.getInventoryMovements()) || inventory.getInventoryMovements().size() == 0) {
            return;
        }
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
        Pick pick = outbuondServiceRestemplateClient.getPickById(pickId);
        if (inventory.getInventoryMovements().size() == 0) {
            // copy the pick movement and assign it to the inventory's movement
            copyMovementsFromPick(pick, inventory);

        }
        inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.PICKING,
                "pick", String.valueOf(pickId), "", pick.getNumber());
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
        if (StringUtils.isBlank(newLpn)) {
            newLpn = commonServiceRestemplateClient.getNextLpn();
        }
        Inventory newInventory = inventory.split(newLpn, newQuantity);
        List<Inventory> inventories = new ArrayList<>();
        inventories.add(saveOrUpdate(inventory));
        inventories.add(saveOrUpdate(newInventory));
        inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.INVENTORY_SPLIT,
                "Lpn,Quantity", inventory.getLpn() + "," + inventory.getQuantity(),
                newLpn + "," + newQuantity);
        return inventories;
    }

    /**
     * unpick an inventory, deattach from pick work and put it back to stock location
     * @param id Inventory ID
     * @param destinationLocationId ID of destination location
     * @param immediateMove immediate move the inventory to the destination, or generate a work
     *                      so someone can do the mvoement later(TO-DO)
     * @return inventory being unpick
     */
    public Inventory unpick(long id,
                            Long warehouseId,
                            Long destinationLocationId,
                            String destinationLocationName,
                            boolean immediateMove) {
        Inventory inventory = findById(id);
        Location destinationLocation;
        if (Objects.nonNull(destinationLocationId)) {
            destinationLocation =
                    warehouseLayoutServiceRestemplateClient.getLocationById(destinationLocationId);
        }
        else if (StringUtils.isNotBlank(destinationLocationName)) {
            destinationLocation =
                    warehouseLayoutServiceRestemplateClient.getLocationByName(warehouseId, destinationLocationName);
        }
        else {
            // if we don't pass in the destination ID or name, the inventory will
            // stay where it is after unpick
            destinationLocation = inventory.getLocation();
        }
        if (Objects.isNull(destinationLocation)) {
            throw ResourceNotFoundException.raiseException("Unable to unpick. Can't find the destination location for the unpicked inventory");
        }
        return unpick(inventory, destinationLocation, immediateMove);

    }

    public Inventory unpick(Inventory inventory, Location destinationLocation, boolean immediateMove) {
        if (inventory.getPickId() == null) {
            // The inventory is not a picked inventory
            return inventory;
        }

        // update the pick
        Pick cancelledPick =
                outbuondServiceRestemplateClient.unpick(inventory.getPickId(), inventory.getQuantity());

        // disconnect the inventory from the pick and
        // clear all the movement path
        inventory.setPickId(null);
        inventoryMovementService.clearInventoryMovement(inventory);
        inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.UNPICKING,
                "quantity", String.valueOf(inventory.getQuantity()), "", cancelledPick.getNumber());


        // Move the inventory to the destination location
        if (immediateMove) {
            logger.debug("Immediate move the unpicked inventory {} to the destinationLocation {}",
                    inventory.getLpn(), destinationLocation.getName());
            inventory = moveInventory(inventory, destinationLocation);
        }
        else {
            generateMovementWork(inventory, destinationLocation);
        }
        return saveOrUpdate(inventory);

    }

    private void generateMovementWork(Inventory inventory, Location destinationLocation) {
        throw new UnsupportedOperationException();
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

    public Inventory addInventory(Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType) {
        return addInventory(inventory, inventoryQuantityChangeType, "", "");

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
    public Inventory addInventory(Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType,
                                  String documentNumber, String comment) {

        logger.debug("Start to add inventory");
        if (isApprovalNeededForInventoryAdjust(inventory, 0L, inventory.getQuantity(), inventoryQuantityChangeType)) {

            logger.debug("We will need to get approval, so here we just save the request");
            writeInventoryAdjustRequest(inventory, 0L,  inventory.getQuantity(),
                    inventoryQuantityChangeType, documentNumber, comment);
            inventory.setLockedForAdjust(true);
            return inventory;
        } else {
            logger.debug("No approval needed, let's just go ahread with the adding inventory!");
            return processAddInventory(inventory, inventoryQuantityChangeType, documentNumber, comment);
        }
    }

    private void lockInventory(Long id) {
        Inventory inventory = findById(id);
        inventory.setLockedForAdjust(true);
        saveOrUpdate(inventory);
    }
    public void releaseInventory(Long id) {
        Inventory inventory = findById(id);
        inventory.setLockedForAdjust(false);
        saveOrUpdate(inventory);
    }


    private void writeInventoryAdjustRequest(Inventory inventory, Long newQuantity,
                                             InventoryQuantityChangeType inventoryQuantityChangeType,
                                             String documentNumber, String comment) {


        writeInventoryAdjustRequest(inventory, inventory.getQuantity(),  newQuantity,
                inventoryQuantityChangeType,
                documentNumber, comment);
    }
    private void writeInventoryAdjustRequest(Inventory inventory, Long oldQuantity, Long newQuantity,
                                             InventoryQuantityChangeType inventoryQuantityChangeType,
                                             String documentNumber, String comment) {

        // if we are manupulating an existing inventory, let's lock teh inventory first
        if (Objects.nonNull(inventory.getId())) {
            lockInventory(inventory.getId());
        }
        else {
            logger.debug("set lockedForAdjust to true for LPN {}", inventory.getLpn());
            inventory.setLockedForAdjust(true);
        }
        inventoryAdjustmentRequestService.writeInventoryAdjustRequest(inventory, oldQuantity, newQuantity, inventoryQuantityChangeType,
                  documentNumber,  comment);

    }

    /**
     * Check if we will need to get approval when adjust inventory from oldQuantity to newQuantity. we may define different rules
     * for different type of adjust action, like adjust / count / etc.
     * @param inventory inventory being adjust
     * @param oldQuantity old quantity
     * @param newQuantity new quantity
     * @param inventoryQuantityChangeType adjust type
     * @return true if we need approval for the adjustment
     */
    private boolean isApprovalNeededForInventoryAdjust(Inventory inventory, Long oldQuantity,
                                                       Long newQuantity, InventoryQuantityChangeType inventoryQuantityChangeType) {
        // Receiving is always allowed without any approval
        logger.debug("Check if we need approval for this adjust. inventory: {}, lpn {}, OLD quantity: {}, NEW quantity: {}, change type: {}",
                inventory.getId(), inventory.getLpn(), oldQuantity, newQuantity, inventoryQuantityChangeType);
        if (inventoryQuantityChangeType.equals(InventoryQuantityChangeType.RECEIVING) ||
                inventoryQuantityChangeType.equals(InventoryQuantityChangeType.PRODUCING)||
                inventoryQuantityChangeType.equals(InventoryQuantityChangeType.PRODUCING_BY_PRODUCT)||
                inventoryQuantityChangeType.equals(InventoryQuantityChangeType.CONSUME_MATERIAL)||
                inventoryQuantityChangeType.equals(InventoryQuantityChangeType.RETURN_MATERAIL)) {
            logger.debug("Receiving / Producing / Return Material doesn't needs any approve");
            return false;
        }

        // we will need approval only when the inventory adjust exceed the threshold of adjustment
        return inventoryAdjustmentThresholdService.isInventoryAdjustExceedThreshold(inventory, inventoryQuantityChangeType, oldQuantity, newQuantity);

    }
    private boolean isApprovalNeededForInventoryAdjust(Inventory inventory, Long newQuantity, InventoryQuantityChangeType inventoryQuantityChangeType) {
        return isApprovalNeededForInventoryAdjust(inventory, inventory.getQuantity(), newQuantity, inventoryQuantityChangeType);
    }

    public Inventory processAddInventory(Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType,
                                         String documentNumber, String comment) {
        Location location =
                warehouseLayoutServiceRestemplateClient.getLogicalLocationForAdjustInventory(
                        inventoryQuantityChangeType,inventory.getWarehouseId());
        // consolidate the inventory at the destination, if necessary

        Location destinationLocation = inventory.getLocation();

        if (Objects.isNull(destinationLocation)) {
            // Location is not setup yet, we should be able to get
            // from the inventory's location id
            destinationLocation = warehouseLayoutServiceRestemplateClient.getLocationById(
                    inventory.getLocationId()
            );
        }

        if (Objects.isNull(destinationLocation)) {
            // If we still can't get destination here, we got
            // some error
            throw MissingInformationException.raiseException("Can't create inventory due to missing location information");
        }

        // create the inventory at the logic location
        // then move the inventory to the final location
        inventory.setLocation(location);
        inventory.setLocationId(location.getId());
        inventory.setVirtual(warehouseLayoutServiceRestemplateClient.isVirtualLocation(location));

        inventory = saveOrUpdate(inventory);

        // Save the activity
        InventoryActivityType inventoryActivityType;
        switch (inventoryQuantityChangeType) {
            case RECEIVING:
                inventoryActivityType = InventoryActivityType.RECEIVING;
                break;
            case PRODUCING:
            case PRODUCING_BY_PRODUCT:
                inventoryActivityType = InventoryActivityType.WORK_ORDER_PRODUCING;
                break;
            case RETURN_MATERAIL:
                inventoryActivityType = InventoryActivityType.WORK_ORDER_RETURN_MATERIAL;
                break;
            case AUDIT_COUNT:
                inventoryActivityType = InventoryActivityType.AUDIT_COUNT;
                break;
            case CYCLE_COUNT:
                inventoryActivityType = InventoryActivityType.CYCLE_COUNT;
                break;
            default:
                inventoryActivityType = InventoryActivityType.INVENTORY_ADJUSTMENT;
        }
        inventoryActivityService.logInventoryActivitiy(inventory, inventoryActivityType,
                "quantity", "0", String.valueOf(inventory.getQuantity()),
                documentNumber, comment);


        // send integration to add a new inventory
        integrationService.processInventoryAdjustment(inventoryQuantityChangeType, inventory,
                0L, inventory.getQuantity());

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


    public Inventory adjustInventoryQuantity(long id, Long newQuantity, String documentNumber, String comment) {

        return adjustInventoryQuantity(findById(id), newQuantity, documentNumber, comment, InventoryQuantityChangeType.INVENTORY_ADJUST);
    }
    /**
     * Adjust the quantity of inventory.
     * 1. When we adjust up, we will actually create inventory
     *    somewhere (a designated location for inventory creation and removal)
     *    and then move inventory to the final location
     * 2. When we adjust down, we will actually move the difference from the
     *     original LPN to a fake location
     * @param inventory inventory to be adjust
     * @param newQuantity new quantity
     * @return inventory after adjust
     */
    public Inventory adjustInventoryQuantity(Inventory inventory, Long newQuantity, String documentNumber, String comment,
                                             InventoryQuantityChangeType inventoryQuantityChangeType) {
        if (inventory.getQuantity().equals(newQuantity)) {
            // nothing changed, let just return
            return inventory;
        }

        if (isApprovalNeededForInventoryAdjust(inventory, newQuantity, inventoryQuantityChangeType)) {
            writeInventoryAdjustRequest(inventory, newQuantity, inventoryQuantityChangeType, documentNumber, comment);
            return inventory;
        } else {
            return processAdjustInventoryQuantity(inventory, newQuantity, documentNumber, comment);
        }
    }

    public Inventory processAdjustInventoryQuantity(Inventory inventory, Long newQuantity, String documentNumber, String comment) {
        Inventory resultInventory = null;
        // Save the original quantity so we can send integration
        Long originalQuantity = inventory.getQuantity();
        if (newQuantity == 0) {
            // a specific case where we are actually removing an inventory
            resultInventory = processRemoveInventory(inventory, InventoryQuantityChangeType.INVENTORY_ADJUST,  documentNumber, comment);
        }
        else if (inventory.getQuantity() > newQuantity) {
            // OK we are adjust down, let's split the original inventory
            // and move the difference into a new location

            inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.INVENTORY_ADJUSTMENT,
                    "quantity", String.valueOf(inventory.getQuantity()), String.valueOf(newQuantity),
                    documentNumber, comment);

            String newLpn = commonServiceRestemplateClient.getNextLpn();

            // log the activity for split inventory for inventory adjust
            // LPN-quantity:
            // from value: Original LPN and original quantity
            // to value: split into the new LPN and new LPN's quantity
            inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.SPLIT_FOR_INVENTORY_ADJUSTMENT,
                    "LPN-quantity",
                    inventory.getLpn() + "-" + inventory.getQuantity(),
                    newLpn + "-" + (inventory.getQuantity() - newQuantity),
                    documentNumber, comment);

            Inventory newInventory = inventory.split(newLpn, inventory.getQuantity() - newQuantity);

            // Save both inventory before move
            inventory = saveOrUpdate(inventory);
            newInventory = save(newInventory);

            // Remove the new inventory
            processRemoveInventory(newInventory, InventoryQuantityChangeType.INVENTORY_ADJUST,"", "");
            resultInventory =  inventory;
        }
        else {
            // if we are here, we are adjust quantity up
            // We will create a inventory in a logic location
            // and then move the inventory onto the existing inventory

            inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.INVENTORY_ADJUSTMENT,
                    "quantity", String.valueOf(inventory.getQuantity()), String.valueOf(newQuantity),
                    documentNumber, comment);

            String newLpn = commonServiceRestemplateClient.getNextLpn();
            // Trick: Split 0 quantity from original inventory, which is
            // actually a copy
            Inventory newInventory = inventory.split(newLpn, 0L);
            newInventory.setQuantity(newQuantity - inventory.getQuantity());
            // Add the inventory to the current location
            newInventory = processAddInventory(newInventory, InventoryQuantityChangeType.INVENTORY_ADJUST, "", "");


            // In case the new LPN is not combined with old LPN, let's do the manual consolidation
            if (!newInventory.getLpn().equals(inventory.getLpn())) {
                newInventory.setLpn(inventory.getLpn());
                resultInventory =  saveOrUpdate(newInventory);
            }
            else {
                resultInventory =  newInventory;
            }
        }

        // integration will be process by
        // processRemoveInventory or processAddInventory method
        // integrationService.processInventoryAdjustment(resultInventory, originalQuantity, newQuantity);
        return resultInventory;
    }

    public void processInventoryAdjustRequest(InventoryAdjustmentRequest inventoryAdjustmentRequest) {
        // We will only process those request that has been approved
        if (!inventoryAdjustmentRequest.getStatus().equals(InventoryAdjustmentRequestStatus.APPROVED)) {
            return;
        }

        Inventory inventory = getInventoryFromAdjustRequest(inventoryAdjustmentRequest);

        if (Objects.isNull(inventory.getId())) {
            // if the inventory doesn't have ID yet, we assume we are adding

            processAddInventory(inventory, inventoryAdjustmentRequest.getInventoryQuantityChangeType(),
                    inventoryAdjustmentRequest.getDocumentNumber(), inventoryAdjustmentRequest.getComment());
        }
        else {
            processAdjustInventoryQuantity(inventory, inventoryAdjustmentRequest.getNewQuantity(),
                    inventoryAdjustmentRequest.getDocumentNumber(), inventoryAdjustmentRequest.getComment());
        }

        // check if we can release the location after the request is approved
        inventoryAdjustmentRequestService.releaseLocationLock(inventoryAdjustmentRequest);
    }


    public Inventory getInventoryFromAdjustRequest(InventoryAdjustmentRequest inventoryAdjustmentRequest) {
        if (Objects.nonNull(inventoryAdjustmentRequest.getInventoryId())) {
            return findById(inventoryAdjustmentRequest.getInventoryId());
        }
        Inventory inventory = new Inventory();
        inventory.setLpn(inventoryAdjustmentRequest.getLpn());

        inventory.setLocationId(inventoryAdjustmentRequest.getLocationId());
        inventory.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(
                inventoryAdjustmentRequest.getLocationId()
        ));

        inventory.setItem(inventoryAdjustmentRequest.getItem());
        inventory.setItemPackageType(inventoryAdjustmentRequest.getItemPackageType());
        inventory.setQuantity(inventoryAdjustmentRequest.getNewQuantity());
        inventory.setVirtual(inventoryAdjustmentRequest.getVirtual());
        inventory.setInventoryStatus(inventoryAdjustmentRequest.getInventoryStatus());
        inventory.setWarehouseId(inventoryAdjustmentRequest.getWarehouseId());
        inventory.setWarehouse(warehouseLayoutServiceRestemplateClient.getWarehouseById(
                inventoryAdjustmentRequest.getWarehouseId()
        ));

        return inventory;
    }

    public Inventory changeInventory( long id, Inventory inventory) {
        Inventory originalInventory = findById(id);
        // Let's see which attribute has been changed
        if (!originalInventory.getLpn().equals(inventory.getLpn())) {

            inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.RELABEL_LPN,
                    "lpn", originalInventory.getLpn(), inventory.getLpn());
        }

        if (!originalInventory.getInventoryStatus().equals(inventory.getInventoryStatus())) {

            inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.INVENTORY_STATUS_CHANGE,
                    "inventory status", originalInventory.getInventoryStatus().getName(),
                    inventory.getInventoryStatus().getName());
        }

        if (!originalInventory.getItemPackageType().equals(inventory.getItemPackageType())) {

            inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.INVENTORY_PACKAGE_TYPE_CHANGE,
                    "item package type", originalInventory.getItemPackageType().getName(), inventory.getItemPackageType().getName());
        }
        return saveOrUpdate(inventory);
    }

    public Inventory changeQuantityByAuditCount(Inventory inventory, Long newQuantity) {
        if (Objects.isNull(inventory.getId())) {
            inventory.setQuantity(newQuantity);
            return addInventory(inventory, InventoryQuantityChangeType.AUDIT_COUNT);
        }
        else {
            // Adjust the quantity without any document and comment
            return adjustInventoryQuantity(inventory, newQuantity, "", "", InventoryQuantityChangeType.AUDIT_COUNT);

        }
        /***
        Long originalQuantity = inventory.getQuantity();
        inventory.setQuantity(newQuantity);
        inventory = saveOrUpdate(inventory);
        integrationService.processInventoryAdjustment(InventoryQuantityChangeType.AUDIT_COUNT,
                inventory, originalQuantity, newQuantity);

        return inventory;
         **/
    }


    /**
     * Consume inventory for a list of work order lines. This is normally called when the work order
     * is complete so we can consume all the left over inventory
     * @param warehouseId
     * @param workOrderLineIds
     * @return
     */
    public List<Inventory> consumeInventoriesForWorkOrderLines(Long warehouseId, String workOrderLineIds) {

        try {
            List<Pick> picks = outbuondServiceRestemplateClient.getWorkOrderPicks(warehouseId, workOrderLineIds);
            if (picks.size() > 0) {
                String pickIds = picks.stream()
                        .map(Pick::getId).map(String::valueOf).collect(Collectors.joining(","));
                List<Inventory> pickedInventories = findAll(
                        warehouseId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        pickIds,
                        null
                );
                // Let's remove those inventories
                pickedInventories.forEach(inventory -> removeInventory(inventory, InventoryQuantityChangeType.CONSUME_MATERIAL));

                return pickedInventories;
            }
        } catch (IOException e) {
            // in case we can't get any picks, just return empty result to indicate
            // we don't have any delivered inventory

        }
        return new ArrayList<>();
    }


    public List<Inventory> consumeInventoriesForWorkOrderLine(Long workOrderLineId, Long warehouseId,Long quantity) {

        try {
            List<Pick> picks = outbuondServiceRestemplateClient.getWorkOrderPicks(warehouseId, String.valueOf(workOrderLineId));
            if (picks.size() > 0) {
                String pickIds = picks.stream()
                        .map(Pick::getId).map(String::valueOf).collect(Collectors.joining(","));
                List<Inventory> pickedInventories = findAll(
                        warehouseId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        pickIds,
                        null
                );
                // Let's remove those inventories until we reach the
                // required quantity
                Long quantityToBeRemoved = quantity;
                Iterator<Inventory> inventoryIterator = pickedInventories.iterator();
                while (inventoryIterator.hasNext() && quantityToBeRemoved > 0) {
                    Inventory inventory = inventoryIterator.next();
                    if (inventory.getQuantity() <= quantityToBeRemoved) {
                        // The whole inventory can be removed
                        quantityToBeRemoved -= inventory.getQuantity();
                        removeInventory(inventory, InventoryQuantityChangeType.CONSUME_MATERIAL);


                    }
                    else {
                        // we only need to remove partial quantity of the inventory
                        // so we will need to split the inventory first
                        String newLpn = commonServiceRestemplateClient.getNextLpn();
                        // we will split the quantity to be consumed from the original inventory,
                        // save the origianl inventory and then remove the splited inventory
                        Inventory splitedInventory = inventory.split(newLpn, quantityToBeRemoved);
                        splitedInventory = save(splitedInventory);
                        saveOrUpdate(inventory);
                        quantityToBeRemoved -= splitedInventory.getQuantity();
                        removeInventory(splitedInventory, InventoryQuantityChangeType.CONSUME_MATERIAL);

                    }
                }

                return pickedInventories;
            }
        } catch (IOException e) {
            // in case we can't get any picks, just return empty result to indicate
            // we don't have any delivered inventory

        }
        return new ArrayList<>();
    }
}