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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.clients.*;
import com.garyzhangscm.cwms.inventory.exception.GenericException;
import com.garyzhangscm.cwms.inventory.exception.InventoryException;
import com.garyzhangscm.cwms.inventory.exception.MissingInformationException;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventoryRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
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
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
@Service
public class InventoryService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    HttpServletRequest httpServletRequest;

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
    private UserService userService;
    @Autowired
    private InventoryConfigurationService inventoryConfigurationService;
    @Autowired
    private QCRuleConfigurationService qcRuleConfigurationService;

    private Map<String, Double> inventoryFileUploadProgress = new ConcurrentHashMap<>();

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private WorkOrderServiceRestemplateClient workOrderServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;
    @Autowired
    private InboundServiceRestemplateClient inboundServiceRestemplateClient;
    @Autowired
    private QCInspectionRequestService qcInspectionRequestService;

    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;
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

    public List<Inventory> findAll(Long warehouseId) {
        return findAll(warehouseId, true);
    }

    public List<Inventory> findAll(Long warehouseId, boolean includeDetails) {

        // Only return actual inventory
        List<Inventory> inventories = inventoryRepository.findByVirtual(warehouseId, false);
        if (includeDetails && inventories.size() > 0) {
            loadInventoryAttribute(inventories);
        }
        return inventories;
    }
    public List<Inventory> findAll(Long warehouseId,
                                   Long itemId,
                                   String itemName,
                                   String itemPackageTypeName,
                                   Long clientId,
                                   String clientIds,
                                   String itemFamilyIds,
                                   Long inventoryStatusId,
                                   String locationName,
                                   Long locationId,
                                   String locationIds,
                                   Long locationGroupId,
                                   String receiptId,
                                   String customerReturnOrderId,
                                   Long workOrderId,
                                   String workOrderLineIds,
                                   String workOrderByProductIds,
                                   String pickIds,
                                   String lpn,
                                   String inventoryIds,
                                   Boolean notPutawayInventoryOnly,
                                   Boolean includeVirturalInventory,
                                   ClientRestriction clientRestriction) {
        return findAll(warehouseId, itemId,
                itemName, itemPackageTypeName, clientId, clientIds, itemFamilyIds, inventoryStatusId,
                locationName, locationId, locationIds, locationGroupId,
                receiptId, customerReturnOrderId,  workOrderId, workOrderLineIds,
                workOrderByProductIds,
                pickIds, lpn,
                inventoryIds, notPutawayInventoryOnly, includeVirturalInventory,
                clientRestriction,
                true);
    }


    public List<Inventory> findAll(Long warehouseId,
                                   Long itemId,
                                   String itemName,
                                   String itemPackageTypeName,
                                   Long clientId,
                                   String clientIds,
                                   String itemFamilyIds,
                                   Long inventoryStatusId,
                                   String locationName,
                                   Long locationId,
                                   String locationIds,
                                   Long locationGroupId,
                                   String receiptId,
                                   String customerReturnOrderId,
                                   Long workOrderId,
                                   String workOrderLineIds,
                                   String workOrderByProductIds,
                                   String pickIds,
                                   String lpn,
                                   String inventoryIds,
                                   Boolean notPutawayInventoryOnly,
                                   Boolean includeVirturalInventory,
                                   ClientRestriction clientRestriction,
                                   boolean includeDetails) {

        LocalDateTime currentLocalDateTime = LocalDateTime.now();
        logger.debug("====> Start to find all inventory that match criteria @ {}", currentLocalDateTime );
        List<Inventory> inventories =  inventoryRepository.findAll(
                (Root<Inventory> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    criteriaQuery.distinct(true);

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(clientId)) {
                        predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
                    }
                    else if (Strings.isNotBlank(clientIds)) {

                        CriteriaBuilder.In<Long> inClientIds = criteriaBuilder.in(root.get("clientId"));
                        for(String id : clientIds.split(",")) {
                            inClientIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inClientIds));
                    }

                    if (Objects.nonNull(itemId)) {
                        Join<Inventory, Item> joinItem = root.join("item", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinItem.get("id"), itemId));

                    }
                    if (StringUtils.isNotBlank(itemName) || StringUtils.isNotBlank(clientIds)) {
                        Join<Inventory, Item> joinItem = root.join("item", JoinType.INNER);
                        if (StringUtils.isNotBlank(itemName)) {

                                if (itemName.contains("%")) {
                                    predicates.add(criteriaBuilder.like(joinItem.get("name"), itemName));
                                }
                                else {
                                    predicates.add(criteriaBuilder.equal(joinItem.get("name"), itemName));
                                }
                        }

                        if (StringUtils.isNotBlank(clientIds)) {
                            CriteriaBuilder.In<Long> inClientIds = criteriaBuilder.in(joinItem.get("clientId"));
                            for(String id : clientIds.split(",")) {
                                inClientIds.value(Long.parseLong(id));
                            }
                            predicates.add(criteriaBuilder.and(inClientIds));

                        }
                    }
                    if (StringUtils.isNotBlank(itemPackageTypeName)) {

                        Join<Inventory, ItemPackageType> joinItemPackageType = root.join("itemPackageType", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinItemPackageType.get("name"), itemPackageTypeName));

                    }
                    if (StringUtils.isNotBlank(itemFamilyIds)) {
                        Join<Inventory, Item> joinItem = root.join("item", JoinType.INNER);
                        Join<Item, ItemFamily> joinItemFamily = joinItem.join("itemFamily", JoinType.INNER);

                        CriteriaBuilder.In<Long> inItemFamilyIds = criteriaBuilder.in(joinItemFamily.get("id"));
                        for(String id : itemFamilyIds.split(",")) {
                            inItemFamilyIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inItemFamilyIds));
                    }
                    if (Objects.nonNull(inventoryStatusId)) {
                        Join<Inventory, InventoryStatus> joinInventoryStatus = root.join("inventoryStatus", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinInventoryStatus.get("id"), inventoryStatusId));

                    }

                    // if location ID is passed in, we will only filter by location id, no matter whether
                    // the location name is passed in or not
                    // otherwise, we will try to filter by location name
                    if (Objects.nonNull(locationId)) {
                        predicates.add(criteriaBuilder.equal(root.get("locationId"), locationId));
                    }
                    else if (StringUtils.isNotBlank(locationName) &&
                            Objects.nonNull(warehouseId)) {
                        logger.debug("Will get inventory from location {} / {}",
                                warehouseId, locationName);
                        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(warehouseId, locationName);

                        if (location != null) {
                            logger.debug(">> location id: {}",
                                    location.getId());
                            predicates.add(criteriaBuilder.equal(root.get("locationId"), location.getId()));
                        }
                        else {
                            // since the user passed in a wrong location, we will add a
                            // wrong id into the query so it won't return anything
                            predicates.add(criteriaBuilder.equal(root.get("locationId"), -9999));
                        }
                    }

                    if (StringUtils.isNotBlank(locationIds)) {


                        CriteriaBuilder.In<Long> inLocationIds = criteriaBuilder.in(root.get("locationId"));
                        for(String id : locationIds.split(",")) {
                            inLocationIds.value(Long.parseLong(id));
                        }



                        predicates.add(criteriaBuilder.and(inLocationIds));
                    }


                    if (StringUtils.isNotBlank(receiptId)) {
                        predicates.add(criteriaBuilder.equal(root.get("receiptId"), receiptId));

                    }

                    if (StringUtils.isNotBlank(customerReturnOrderId)) {
                        predicates.add(criteriaBuilder.equal(root.get("customerReturnOrderId"), customerReturnOrderId));

                    }

                    if (Objects.nonNull(workOrderId)) {
                        predicates.add(criteriaBuilder.equal(root.get("workOrderId"), workOrderId));

                    }


                    if (StringUtils.isNotBlank(workOrderLineIds)) {
                        CriteriaBuilder.In<Long> inClause = criteriaBuilder.in(root.get("workOrderLineId"));
                        Arrays.stream(workOrderLineIds.split(","))
                                .map(Long::parseLong).forEach(workOrderLineId -> inClause.value(workOrderLineId));
                        predicates.add(inClause);

                    }
                    if (StringUtils.isNotBlank(workOrderByProductIds)) {
                        CriteriaBuilder.In<Long> inClause = criteriaBuilder.in(root.get("workOrderByProductId"));
                        Arrays.stream(workOrderByProductIds.split(","))
                                .map(Long::parseLong).forEach(workOrderByProductId -> inClause.value(workOrderByProductId));
                        predicates.add(inClause);

                    }

                    if (StringUtils.isNotBlank(pickIds)) {
                        CriteriaBuilder.In<Long> inClause = criteriaBuilder.in(root.get("pickId"));
                        Arrays.stream(pickIds.split(",")).map(Long::parseLong).forEach(pickId -> inClause.value(pickId));
                        predicates.add(inClause);

                    }
                    if (StringUtils.isNotBlank(lpn)) {

                        logger.debug("lpn {} , lpn.contains(%) ? {}",
                                lpn, lpn.contains("%"));
                        if (lpn.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("lpn"), lpn));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("lpn"), lpn));
                        }

                    }


                    if (StringUtils.isNotBlank(inventoryIds)) {

                        CriteriaBuilder.In<Long> inClause = criteriaBuilder.in(root.get("id"));
                        Arrays.stream(inventoryIds.split(","))
                                .map(Long::parseLong).forEach(inventoryId -> inClause.value(inventoryId));
                        predicates.add(inClause);
                    }

                    // Only return actual inventory
                    if(!Boolean.TRUE.equals(includeVirturalInventory)) {
                        predicates.add(criteriaBuilder.equal(root.get("virtual"), false));
                    }


                    Predicate[] p = new Predicate[predicates.size()];

                    // special handling for warehouse id
                    // if warehouse id is passed in, then return both the warehouse level item
                    // and the company level item information.
                    // otherwise, return the company level item information
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));

                    if (Objects.isNull(clientRestriction) ||
                            !Boolean.TRUE.equals(clientRestriction.getThreePartyLogisticsFlag()) ||
                            Boolean.TRUE.equals(clientRestriction.getAllClientAccess())) {
                        // not a 3pl warehouse, let's not put any restriction on the client
                        // (unless the client restriction is from the web request, which we already
                        // handled previously
                        return predicate;
                    }


                    // build the accessible client list predicated based on the
                    // client ID that the user has access
                    Predicate accessibleClientListPredicate;
                    if (clientRestriction.getClientAccesses().trim().isEmpty()) {
                        // the user can't access any client, then the user
                        // can only access the non 3pl data
                        accessibleClientListPredicate = criteriaBuilder.isNull(root.get("clientId"));
                    }
                    else {
                        CriteriaBuilder.In<Long> inClientIds = criteriaBuilder.in(root.get("clientId"));
                        for(String id : clientRestriction.getClientAccesses().trim().split(",")) {
                            inClientIds.value(Long.parseLong(id));
                        }
                        accessibleClientListPredicate = criteriaBuilder.and(inClientIds);
                    }

                    if (Boolean.TRUE.equals(clientRestriction.getNonClientDataAccessible())) {
                        // the user can access the non 3pl data
                        return criteriaBuilder.and(predicate,
                                    criteriaBuilder.or(
                                            criteriaBuilder.isNull(root.get("clientId")),
                                            accessibleClientListPredicate));
                    }
                    else {

                        // the user can NOT access the non 3pl data
                        return criteriaBuilder.and(predicate,
                                    criteriaBuilder.and(
                                            criteriaBuilder.isNotNull(root.get("clientId")),
                                            accessibleClientListPredicate));
                    }
                }
        );
        inventories.sort((inventory1, inventory2) -> {
            if(inventory1.getLocationId().equals(inventory2.getLocationId())) {
                return inventory1.getLpn().compareToIgnoreCase(inventory2.getLpn());
            }
            else {
                return inventory1.getLocationId().compareTo(inventory2.getLocationId());
            }
        });

        logger.debug("====> after : {} millisecond(1/1000 second) @ {}, we found {} record",
                 ChronoUnit.MILLIS.between(currentLocalDateTime, LocalDateTime.now()),
                LocalDateTime.now(),
                inventories.size());
        currentLocalDateTime = LocalDateTime.now();

        // if we need to load the details, or asked to return the inventory
        // that is only in receiving stage,
        if ((includeDetails || Boolean.TRUE.equals(notPutawayInventoryOnly))
                && inventories.size() > 0) {
            loadInventoryAttribute(inventories);
            logger.debug("====> after : {} millisecond(1/1000 second) @ {}, we loaded the details for {} record",
                    ChronoUnit.MILLIS.between(currentLocalDateTime, LocalDateTime.now()),
                    LocalDateTime.now(),
                    inventories.size());
            currentLocalDateTime = LocalDateTime.now();


            logger.debug("Boolean.TRUE.equals(notPutawayInventoryOnly)? {}", Boolean.TRUE.equals(notPutawayInventoryOnly));
            if (Boolean.TRUE.equals(notPutawayInventoryOnly)) {
                // the inventory may be in the receipt or in the receiving stage
                String receiptLocationGroup =
                        commonServiceRestemplateClient.getPolicyByKey(warehouseId, "LOCATION-GROUP-RECEIPT").getValue();

                logger.debug("receiptLocationGroup? {}", receiptLocationGroup);
                List<Inventory> inventoriesOnReceipt = inventories.stream().filter(
                        inventory -> inventory.getLocation().getLocationGroup().getName().equals(
                                receiptLocationGroup
                        )
                ).collect(Collectors.toList());

                List<Inventory> inventoriesInReceivingStage =
                        inventories.stream().filter(
                                inventory -> inventory.getLocation().getLocationGroup().getLocationGroupType().getReceivingStage() == true
                        ).collect(Collectors.toList());


                List<Inventory> inventoryNotPutawayYet = new ArrayList<>();
                // combine those 2 inventory list
                inventoryNotPutawayYet.addAll(inventoriesOnReceipt);
                inventoryNotPutawayYet.addAll(inventoriesInReceivingStage);

                inventories = inventoryNotPutawayYet;

            }
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
        logger.debug("====> after : {} millisecond(1/1000 second) @ {}, we will return inventory for {} record",
                ChronoUnit.MILLIS.between(currentLocalDateTime, LocalDateTime.now()),
                LocalDateTime.now(),
                inventories.size());
        return inventories;
    }

    public List<Inventory> findPendingInventoryByLocationId(Long locationId) {
        return inventoryRepository.findPendingInventoryByLocationId(locationId);
    }

    public List<Inventory> findInventoryByLocationIds(Long warehouseId, String locationIds) {
        return findAll(warehouseId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                locationIds,
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
                null);
    }

    public List<Inventory> findPickableInventories(Long itemId, Long inventoryStatusId) {
        return findPickableInventories(itemId, inventoryStatusId, true);
    }
    public List<Inventory> findPickableInventories(Long itemId,
                                                   Long inventoryStatusId,
                                                   boolean includeDetails) {
        return findPickableInventories(itemId, inventoryStatusId, null, null, includeDetails);
    }
    public List<Inventory> findPickableInventories(Long itemId,
                                                   Long inventoryStatusId,
                                                   Long locationId,
                                                   String lpn) {
        return findPickableInventories(itemId, inventoryStatusId, locationId, lpn, true);
    }
    public List<Inventory> findPickableInventories(Long itemId,
                                                   Long inventoryStatusId,
                                                   Long locationId,
                                                   String lpn,
                                                   boolean includeDetails) {
        List<Inventory> availableInventories =
                Objects.isNull(locationId) ?
                        inventoryRepository.findByItemIdAndInventoryStatusId(itemId, inventoryStatusId)
                        :
                        inventoryRepository.findByItemIdAndInventoryStatusIdAndLocationId(itemId, inventoryStatusId, locationId);

        List<Inventory>  pickableInventories
                =  availableInventories
                        .stream()
                .filter(this::isInventoryPickable)
                .map(inventory -> {
                    // setup the location so we can filter the inventory by pickable location only
                    if (Objects.nonNull(inventory.getLocationId()) &&
                            Objects.isNull(inventory.getLocation())) {
                        inventory.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(inventory.getLocationId()));
                    }
                    return inventory;
                }).filter(this::isLocationPickable)
                .filter(inventory -> {
                    // if LPN is passed in, only return the inventory that match with the LPN
                    if(Strings.isNotBlank(lpn)) {
                        return inventory.getLpn().equals(lpn);
                    }
                    else {
                        return true;
                    }
                })
                .collect(Collectors.toList());

        if (includeDetails && pickableInventories.size() > 0) {
            loadInventoryAttribute(pickableInventories);
        }

        logger.debug("We found {} pickable inventory for item id {}, inventory status id {}",
                pickableInventories.size(), itemId, inventoryStatusId);
        // only return inventory in the pickable location;
        return pickableInventories;
    }

    /**
     * CHeck if the inventory is pickable
     * 1. it is not picked
     * 2. it is not already allocated
     * 3. it is not virutal
     * 4. it is not qc required
     * 5. it is not locked for adjust
     * 6. it is not locked by certain lock that doesn't allow pick
     * @param inventory
     * @return
     */
    private boolean isInventoryPickable(Inventory inventory) {
        return Objects.isNull(inventory.getPickId()) &&
                Objects.isNull(inventory.getAllocatedByPickId()) &&
                !Boolean.TRUE.equals(inventory.getVirtual()) &&
                !Boolean.TRUE.equals(inventory.getInboundQCRequired()) &&
                !Boolean.TRUE.equals(inventory.getLockedForAdjust()) &&
                // if inventory is locked by a 'not pickable lock', then
                // the inventory is not pickable
                inventory.getLocks().stream().noneMatch(
                        inventoryWithLock -> Boolean.TRUE.equals(inventoryWithLock.getLock().getAllowPick())
                );
    }

    // CHeck if the inventory is in a pickable location
    private boolean isLocationPickable(Inventory inventory) {
        return inventory.getLocation().getEnabled() == true &&
                inventory.getLocation().getLocationGroup().getPickable() == true &&
                inventory.getLocation().getLocationGroup().getLocationGroupType().getFourWallInventory() == true;
    }


    public List<Inventory> findByLpn(Long warehouseId, String lpn){
        return findByLpn(warehouseId, lpn,true);
    }
    public List<Inventory> findByLpn(Long warehouseId, String lpn, boolean includeDetails){
        List<Inventory> inventories = inventoryRepository.findByWarehouseIdAndLpn(warehouseId, lpn);
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


    public List<Inventory> findByLocationGroupId(Long warehouseId, Long clientId,  Long locationGroupId, boolean includeDetails) {
        return findAll(
                warehouseId,
                null,
                null,
                null,
                clientId,
                null,
                null,
                null,
                null,
                null,
                null,
                locationGroupId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null, null,
                includeDetails
        );
    }

    public List<Inventory> findByLocationGroupId(Long warehouseId, Long clientId,  Long locationGroupId) {
        return findByLocationGroupId(warehouseId, clientId, locationGroupId, true);
    }

    public Inventory save(Inventory inventory) {
        LocalDateTime currentLocalDateTime = LocalDateTime.now();
        if (Objects.isNull(inventory.getInboundQCRequired())) {
            inventory.setInboundQCRequired(false);
        }

        Inventory savedInventory = inventoryRepository.save(inventory);

        // reset location's status and volume
        logger.debug("====> after : {} millisecond(1/1000 second) @ {}, we saved the inventory {}",
                ChronoUnit.MILLIS.between(currentLocalDateTime, LocalDateTime.now()),
                LocalDateTime.now(),
                savedInventory.getId());

        currentLocalDateTime = LocalDateTime.now();

        logger.debug("Start to refresh location {}'s volume",
                inventory.getLocationId());

        if (Objects.nonNull(inventory.getLocation())) {
            warehouseLayoutServiceRestemplateClient.resetLocation(
                    inventory.getLocation()
            );
        }
        else {

            warehouseLayoutServiceRestemplateClient.resetLocation(inventory.getLocationId());
        }

        logger.debug("====> after : {} millisecond(1/1000 second) @ {}, we reset the location {}",
                ChronoUnit.MILLIS.between(currentLocalDateTime, LocalDateTime.now()),
                LocalDateTime.now(),
                inventory.getLocationId());

        return savedInventory;
    }

    public Inventory saveOrUpdate(Inventory inventory) {
        /***
        if (inventory.getId() == null &&
                findByLpn(inventory.getWarehouseId(), inventory.getLpn(), false).size() == 1) {
            inventory.setId(findByLpn(inventory.getLpn()).get(0).getId());
        }
         ***/
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

    @Transactional
    public void deleteInventoryByLocation(Long locationId) {
        inventoryRepository.deleteByLocationId(locationId);
    }


    public void loadInventoryAttribute(List<Inventory> inventories) {
        // Temp map to save the details so that we don't have to call
        // web API to get the same value again

        /**
         * Switch to sprint boot managed cache implementation
         *
        Map<Long, Location> locationMap = new HashMap<>();
        Map<Long, UnitOfMeasure> unitOfMeasureHashMap = new HashMap<>();
        Map<Long, WorkOrder> workOrderHashMap = new HashMap<>();
        for(Inventory inventory : inventories) {
            loadInventoryAttribute(inventory,
                    locationMap, unitOfMeasureHashMap, workOrderHashMap);
        }*/
        for(Inventory inventory : inventories) {
            loadInventoryAttribute(inventory);
        }
    }

    public void loadInventoryAttribute(Inventory inventory) {

     LocalDateTime currentLocalDateTime = LocalDateTime.now();
     logger.debug("========> @ {} start to load inventory details for lpn {}",
     currentLocalDateTime, inventory.getLpn());

        // Load location information
        if (Objects.nonNull(inventory.getClientId()) &&
                Objects.isNull(inventory.getClient())) {

            inventory.setClient(
                    commonServiceRestemplateClient.getClientById(
                            inventory.getClientId()
                    ));

        }

     // Load location information
     if (Objects.nonNull(inventory.getLocationId()) &&
           Objects.isNull(inventory.getLocation())) {

         inventory.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(inventory.getLocationId()));

     }

     logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the location for LPN {}",
     ChronoUnit.MILLIS.between(
             currentLocalDateTime, LocalDateTime.now()),
             LocalDateTime.now(),
             inventory.getLpn());
     currentLocalDateTime = LocalDateTime.now();

     if (inventory.getInventoryMovements() != null && inventory.getInventoryMovements().size() > 0) {
         inventory.getInventoryMovements().forEach(
             inventoryMovement -> {
                 if (inventoryMovement.getLocation() == null && inventoryMovement.getLocationId() != null) {
                    inventoryMovement.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(inventoryMovement.getLocationId()));
                 }
             }
         );
     }
     logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the movement path for LPN {}",
     ChronoUnit.MILLIS.between(
             currentLocalDateTime, LocalDateTime.now()),
             LocalDateTime.now(),
             inventory.getLpn());
     currentLocalDateTime = LocalDateTime.now();

     // load the unit of measure details for the packate types
     // logger.debug("Start to load item unit of measure for item package type: {}",
     //         inventory.getItemPackageType());
     inventory.getItemPackageType().getItemUnitOfMeasures().forEach(itemUnitOfMeasure ->

             itemUnitOfMeasure.setUnitOfMeasure(
                     commonServiceRestemplateClient.getUnitOfMeasureById(itemUnitOfMeasure.getUnitOfMeasureId()))

     );
     logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the unit of measure for LPN {}",
     ChronoUnit.MILLIS.between(
             currentLocalDateTime, LocalDateTime.now()),
             LocalDateTime.now(),
             inventory.getLpn());
     currentLocalDateTime = LocalDateTime.now();

     if (inventory.getPickId() != null) {
        inventory.setPick(outbuondServiceRestemplateClient.getPickById(inventory.getPickId()));
     }

     logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the pick for LPN {}",
     ChronoUnit.MILLIS.between(
             currentLocalDateTime, LocalDateTime.now()),
             LocalDateTime.now(),
             inventory.getLpn());
     currentLocalDateTime = LocalDateTime.now();

     if (inventory.getAllocatedByPickId() != null) {
        inventory.setAllocatedByPick(outbuondServiceRestemplateClient.getPickById(inventory.getAllocatedByPickId()));
     }
     logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the allocated by pick for LPN {}",
     ChronoUnit.MILLIS.between(
             currentLocalDateTime, LocalDateTime.now()),
             LocalDateTime.now(),
             inventory.getLpn());
     currentLocalDateTime = LocalDateTime.now();

     if (Objects.nonNull(inventory.getWorkOrderId()) &&
            Objects.isNull(inventory.getWorkOrder())) {
         inventory.setWorkOrder(workOrderServiceRestemplateClient.getWorkOrderById(inventory.getWorkOrderId()));

     }
     logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the work order for LPN {}",
     ChronoUnit.MILLIS.between(
             currentLocalDateTime, LocalDateTime.now()),
             LocalDateTime.now(),
             inventory.getLpn());




     }
    /**
     *
    public void loadInventoryAttribute(Inventory inventory,
                                       Map<Long, Location> locationMap,
                                        Map<Long, UnitOfMeasure> unitOfMeasureHashMap,
                                        Map<Long, WorkOrder> workOrderHashMap) {

        LocalDateTime currentLocalDateTime = LocalDateTime.now();
        logger.debug("========> @ {} start to load inventory details for lpn {}",
                currentLocalDateTime, inventory.getLpn());


        // Load location information
        if (inventory.getLocationId() != null) {
            if (locationMap.containsKey(inventory.getLocationId())) {
                inventory.setLocation(locationMap.get(inventory.getLocationId()));
            }
            else {
                Location location = warehouseLayoutServiceRestemplateClient.getLocationById(inventory.getLocationId());

                inventory.setLocation(location);
                locationMap.put(
                        inventory.getLocationId(),
                        location
                );
            }
        }

        logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the location for LPN {}",
                ChronoUnit.MILLIS.between(currentLocalDateTime, LocalDateTime.now()),
                LocalDateTime.now(),
                inventory.getLpn());
        currentLocalDateTime = LocalDateTime.now();

        if (inventory.getInventoryMovements() != null && inventory.getInventoryMovements().size() > 0) {
            inventory.getInventoryMovements().forEach(
                    inventoryMovement -> {
                        if (inventoryMovement.getLocation() == null && inventoryMovement.getLocationId() != null) {
                            inventoryMovement.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(inventoryMovement.getLocationId()));
                        }
                    }
            );
        }
        logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the movement path for LPN {}",
                ChronoUnit.MILLIS.between(currentLocalDateTime, LocalDateTime.now()),
                LocalDateTime.now(),
                inventory.getLpn());
        currentLocalDateTime = LocalDateTime.now();

        // load the unit of measure details for the packate types
        // logger.debug("Start to load item unit of measure for item package type: {}",
        //         inventory.getItemPackageType());
        inventory.getItemPackageType().getItemUnitOfMeasures().forEach(itemUnitOfMeasure -> {
            // logger.debug(">> Load information for item unit of measure: {}", itemUnitOfMeasure);
            if (unitOfMeasureHashMap.containsKey(itemUnitOfMeasure.getUnitOfMeasureId())) {
                itemUnitOfMeasure.setUnitOfMeasure(
                        unitOfMeasureHashMap.get(itemUnitOfMeasure.getUnitOfMeasureId())
                );

            }
            else {
                UnitOfMeasure unitOfMeasure =
                        commonServiceRestemplateClient.getUnitOfMeasureById(itemUnitOfMeasure.getUnitOfMeasureId());
                itemUnitOfMeasure.setUnitOfMeasure(unitOfMeasure);
                unitOfMeasureHashMap.put(
                        itemUnitOfMeasure.getUnitOfMeasureId(),
                        unitOfMeasure
                );

            }
        });
        logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the unit of measure for LPN {}",
                ChronoUnit.MILLIS.between(currentLocalDateTime, LocalDateTime.now()),
                LocalDateTime.now(),
                inventory.getLpn());
        currentLocalDateTime = LocalDateTime.now();

        if (inventory.getPickId() != null) {
            inventory.setPick(outbuondServiceRestemplateClient.getPickById(inventory.getPickId()));
        }

        logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the pick for LPN {}",
                ChronoUnit.MILLIS.between(currentLocalDateTime, LocalDateTime.now()),
                LocalDateTime.now(),
                inventory.getLpn());
        currentLocalDateTime = LocalDateTime.now();

        if (inventory.getAllocatedByPickId() != null) {
            inventory.setAllocatedByPick(outbuondServiceRestemplateClient.getPickById(inventory.getAllocatedByPickId()));
        }
        logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the allocated by pick for LPN {}",
                ChronoUnit.MILLIS.between(currentLocalDateTime, LocalDateTime.now()),
                LocalDateTime.now(),
                inventory.getLpn());
        currentLocalDateTime = LocalDateTime.now();

        if (Objects.nonNull(inventory.getWorkOrderId()) &&
               Objects.isNull(inventory.getWorkOrder())) {
            if (workOrderHashMap.containsKey(inventory.getWorkOrderId())) {
                inventory.setWorkOrder(
                        workOrderHashMap.get(inventory.getWorkOrderId())
                );
            }
            else {
                WorkOrder workOrder =
                        workOrderServiceRestemplateClient.getWorkOrderById(inventory.getWorkOrderId());
                inventory.setWorkOrder(workOrder);
                workOrderHashMap.put(
                        inventory.getWorkOrderId(),
                        workOrder
                );
            }

        }
        logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the work order for LPN {}",
                ChronoUnit.MILLIS.between(currentLocalDateTime, LocalDateTime.now()),
                LocalDateTime.now(),
                inventory.getLpn());




    } */

    public List<Inventory> loadInventoryData(File  file) throws IOException {
        List<InventoryCSVWrapper> inventoryCSVWrappers = loadData(file);
        return inventoryCSVWrappers.stream().map(inventoryCSVWrapper -> convertFromWrapper(
                inventoryCSVWrapper, null, null, null, null, null, null)).collect(Collectors.toList());
    }

    public List<InventoryCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("client").
                addColumn("lpn").
                addColumn("location").
                addColumn("item").
                addColumn("itemPackageType").
                addColumn("quantity").
                addColumn("inventoryStatus").
                addColumn("color").
                addColumn("productSize").
                addColumn("style").
                build().withHeader();
        return fileService.loadData(file, schema, InventoryCSVWrapper.class);
    }


    public List<InventoryCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("client").
                addColumn("lpn").
                addColumn("location").
                addColumn("item").
                addColumn("itemPackageType").
                addColumn("quantity").
                addColumn("inventoryStatus").
                addColumn("color").
                addColumn("productSize").
                addColumn("style").
                build().withHeader();

        return fileService.loadData(inputStream, schema, InventoryCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<InventoryCSVWrapper> inventoryCSVWrappers = loadData(inputStream);
            inventoryCSVWrappers.stream().forEach(inventoryCSVWrapper -> {
                Inventory savedInvenotry = saveOrUpdate(
                        convertFromWrapper(inventoryCSVWrapper, null, null, null, null, null, null));
                // re-calculate the size of the location

                Location destination =
                        warehouseLayoutServiceRestemplateClient.getLocationByName(
                                getWarehouseId(inventoryCSVWrapper.getCompany(),
                                        inventoryCSVWrapper.getWarehouse()),
                                inventoryCSVWrapper.getLocation()
                        );
                recalculateLocationSizeForInventoryMovement(null, destination, savedInvenotry.getSize());
            });
            inventoryRepository.flush();
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private List<Inventory> convertFromWrapper(Long warehouseId,
                                               List<InventoryCSVWrapper> inventoryCSVWrappers,
                                               boolean preLoadDetails) {
        if (!preLoadDetails) {
            return inventoryCSVWrappers.stream().map(
                    inventoryCSVWrapper -> convertFromWrapper(inventoryCSVWrapper)
            ).collect(Collectors.toList());
        }


            // in case we have a big chunk of data in the list, we will preload all the
            // item first, so that we don't have to get the data from database / API endpoint
            // for each record. Instead we will bulk get the data
            // 1. warehouse
            // 2. item
            // 3. item package type
            // 4. inventory status
            // 5. location


        Map<String, Warehouse> warehouseIdMap = new HashMap<>();
        Map<String, Item> itemMap = new HashMap<>();
        Map<String, ItemPackageType> itemPackageTypeMap = new HashMap<>();
        Map<String, InventoryStatus> inventoryStatusMap = new HashMap<>();
        Map<String, Location> locationMap = new HashMap<>();
        Map<String, Client> clientMap = new HashMap<>();

        Warehouse warehouse = null;
        for (InventoryCSVWrapper inventoryCSVWrapper : inventoryCSVWrappers) {

            if (!warehouseIdMap.containsKey(inventoryCSVWrapper.getCompany() + "-" + inventoryCSVWrapper.getWarehouse())) {

                warehouse =
                        warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                                inventoryCSVWrapper.getCompany(),
                                inventoryCSVWrapper.getWarehouse());
                if (Objects.isNull(warehouse)) {
                    // warehouse information is wrong, skip the line
                    logger.debug("Can't find warehouse from company {}, warehouse {}",
                            inventoryCSVWrapper.getCompany(),
                            inventoryCSVWrapper.getWarehouse());
                    continue;
                }
                else if (Objects.nonNull(warehouseId) && !warehouseId.equals(warehouse.getId())) {
                    // the user will only allowed to upload the records from the same warehouse
                    logger.debug("skip the record as current warehouse id is {}, but the warehouse id from the record is {}",
                            warehouseId, warehouse.getId());
                    continue;
                }
                warehouseIdMap.put(inventoryCSVWrapper.getCompany() + "-" + inventoryCSVWrapper.getWarehouse(),
                        warehouse);
            }

            Client client = null;
            if (Strings.isNotBlank(inventoryCSVWrapper.getClient())) {
                if (!clientMap.containsKey(inventoryCSVWrapper.getClient())) {
                    client = commonServiceRestemplateClient.getClientByName(warehouse.getId(), inventoryCSVWrapper.getClient());
                    clientMap.put(inventoryCSVWrapper.getClient(), client);
                }
                else {
                    // load the client for current row so that we can load the right item
                    // based on the warehouse and client infomration along with the item name
                    client = clientMap.get(inventoryCSVWrapper.getClient());
                }
            }

            if (!itemMap.containsKey(inventoryCSVWrapper.getItem()) && Objects.nonNull(warehouse)) {

                Item item = itemService.findByName(warehouse.getId(),
                        client.getId(), inventoryCSVWrapper.getItem());
                if (Objects.isNull(item)) {

                    logger.debug("skip the record as we can't find the item by name {} from warehouse {} / {}",
                            inventoryCSVWrapper.getItem(),
                            warehouse.getId(), warehouse.getName());
                    continue;
                }
                itemMap.put(inventoryCSVWrapper.getItem(),item);
            }
            if (!itemPackageTypeMap.containsKey(inventoryCSVWrapper.getItemPackageType())
                    && Objects.nonNull(warehouse)) {
                ItemPackageType itemPackageType = itemPackageTypeService.findByNaturalKeys(
                        warehouse.getId(),
                        inventoryCSVWrapper.getItemPackageType(),
                        inventoryCSVWrapper.getItem());
                if (Objects.isNull(itemPackageType)) {
                    logger.debug("skip the record as we can't find item package type by " +
                            "warehouse {} / {}, item {}, item package type {}",
                            warehouse.getId(), warehouse.getName(),
                            inventoryCSVWrapper.getItem(),
                            inventoryCSVWrapper.getItemPackageType());
                    continue;
                }
                itemPackageTypeMap.put(
                        inventoryCSVWrapper.getItemPackageType(),
                        itemPackageType
                        );
            }
            if (!inventoryStatusMap.containsKey(inventoryCSVWrapper.getInventoryStatus())
                    && Objects.nonNull(warehouse)) {

                InventoryStatus inventoryStatus = inventoryStatusService.findByName(
                        warehouse.getId(), inventoryCSVWrapper.getInventoryStatus());
                if (Objects.isNull(inventoryStatus)) {
                    logger.debug("Skip the record as we can't find the inventory status by" +
                            "warehouse {} / {}, inventory status {}",
                            warehouse.getId(), warehouse.getName(),
                            inventoryCSVWrapper.getInventoryStatus());
                    continue;
                }
                inventoryStatusMap.put(
                        inventoryCSVWrapper.getInventoryStatus(),
                        inventoryStatus
                        );
            }
            if (!locationMap.containsKey(inventoryCSVWrapper.getLocation())
                    && Objects.nonNull(warehouse)) {
                Location location =
                        warehouseLayoutServiceRestemplateClient.getLocationByName(
                                warehouse.getId(), inventoryCSVWrapper.getLocation());
                if (Objects.isNull(location)) {
                    logger.debug("skip the record as we can't find the location by " +
                            "warehosue {} / {}, location name {}",
                            warehouse.getId(), warehouse.getName(),
                            inventoryCSVWrapper.getLocation());
                    continue;
                }
                locationMap.put(
                        inventoryCSVWrapper.getLocation(), location);
            }
        }

        // only process the record if we have all the value ready
        return inventoryCSVWrappers.stream().filter(
                inventoryCSVWrapper ->  warehouseIdMap.containsKey(inventoryCSVWrapper.getCompany() + "-" + inventoryCSVWrapper.getWarehouse()) &&
                itemMap.containsKey(inventoryCSVWrapper.getItem()) &&
                itemPackageTypeMap.containsKey(inventoryCSVWrapper.getItemPackageType()) &&
                inventoryStatusMap.containsKey(inventoryCSVWrapper.getInventoryStatus()) &&
                        locationMap.containsKey(inventoryCSVWrapper.getLocation())
        ).map(
                inventoryCSVWrapper -> convertFromWrapper(inventoryCSVWrapper,
                        warehouseIdMap.get(inventoryCSVWrapper.getCompany() + "-" + inventoryCSVWrapper.getWarehouse()),
                        itemMap.get(inventoryCSVWrapper.getItem()),
                        itemPackageTypeMap.get(inventoryCSVWrapper.getItemPackageType()),
                        inventoryStatusMap.get(inventoryCSVWrapper.getInventoryStatus()),
                        locationMap.get(inventoryCSVWrapper.getLocation()),
                        clientMap.get(inventoryCSVWrapper.getClient()))
        ).collect(Collectors.toList());



    }
    private Inventory convertFromWrapper(InventoryCSVWrapper inventoryCSVWrapper) {
        return convertFromWrapper(inventoryCSVWrapper, null, null, null, null, null, null);
    }
    private Inventory convertFromWrapper(InventoryCSVWrapper inventoryCSVWrapper,
                                         Warehouse warehouse,
                                         Item item,
                                         ItemPackageType itemPackageType,
                                         InventoryStatus inventoryStatus,
                                         Location location,
                                         Client client) {
        Inventory inventory = new Inventory();
        inventory.setLpn(inventoryCSVWrapper.getLpn());
        inventory.setQuantity(inventoryCSVWrapper.getQuantity());
        inventory.setVirtual(false);

        inventory.setColor(inventoryCSVWrapper.getColor());
        inventory.setProductSize(inventoryCSVWrapper.getProductSize());
        inventory.setStyle(inventoryCSVWrapper.getStyle());

        // warehouse is a mandate field
        if (Objects.isNull(warehouse)) {
            warehouse =
                    warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                            inventoryCSVWrapper.getCompany(),
                            inventoryCSVWrapper.getWarehouse());
        }
        inventory.setWarehouseId(warehouse.getId());
        inventory.setWarehouse(warehouse);

        // client
        if (Objects.nonNull(client)) {
            inventory.setClientId(client.getId());
        }
        else if (Strings.isNotBlank(inventoryCSVWrapper.getClient())) {
            client = commonServiceRestemplateClient.getClientByName(warehouse.getId(),
                    inventoryCSVWrapper.getClient());
            if (Objects.nonNull(client)) {
                inventory.setClientId(client.getId());
            }
        }

        // item
        if (Objects.nonNull(item)) {
            inventory.setItem(item);
        }
        else if (Strings.isNotBlank(inventoryCSVWrapper.getItem())) {
            inventory.setItem(itemService.findByName(warehouse.getId(), inventory.getClientId(),
                    inventoryCSVWrapper.getItem()));
        }

        // itemPackageType
        if (Objects.nonNull(itemPackageType)) {
            inventory.setItemPackageType(itemPackageType);
        }
        else if (Strings.isNotBlank(inventoryCSVWrapper.getItemPackageType()) &&
                Strings.isNotBlank(inventoryCSVWrapper.getItem())) {
            inventory.setItemPackageType(
                    itemPackageTypeService.findByNaturalKeys(
                            warehouse.getId(),
                            inventoryCSVWrapper.getItemPackageType(),
                            inventoryCSVWrapper.getItem()));
        }

        // inventoryStatus
        if (Objects.nonNull(inventoryStatus)) {
            inventory.setInventoryStatus(inventoryStatus);
        }
        else if (Strings.isNotBlank(inventoryCSVWrapper.getInventoryStatus())) {
            logger.debug("will set inventory status: {} / {}",
                    warehouse.getId(),
                    inventoryCSVWrapper.getInventoryStatus());
            inventory.setInventoryStatus(inventoryStatusService.findByName(
                    warehouse.getId(), inventoryCSVWrapper.getInventoryStatus()));
        }

        // location
        if (Objects.nonNull(location)) {

            inventory.setLocationId(location.getId());
            inventory.setLocation(location);
        }
        else if (Strings.isNotBlank(inventoryCSVWrapper.getLocation())) {
            logger.debug("start to get location by name: {}", inventoryCSVWrapper.getLocation());
            location =
                    warehouseLayoutServiceRestemplateClient.getLocationByName(
                            warehouse.getId(), inventoryCSVWrapper.getLocation());
            inventory.setLocationId(location.getId());
            inventory.setLocation(location);
        }

        return inventory;

    }

    public Inventory reverseProduction(Long id, String documentNumber, String comment) {
        Inventory inventory = findById(id);
        logger.debug("Start to reverse production of inventory with lpn {}",
                inventory.getLpn());
        return removeInventory(inventory, InventoryQuantityChangeType.REVERSE_PRODUCTION, documentNumber, comment);
    }

    public Inventory reverseByProduct(Long id, String documentNumber, String comment) {
        Inventory inventory = findById(id);
        logger.debug("Start to reverse by product of inventory with lpn {}",
                inventory.getLpn());
        return removeInventory(inventory, InventoryQuantityChangeType.REVERSE_BY_PRODUCT, documentNumber, comment);
    }

    public Inventory reverseReceiving(Long id, String documentNumber, String comment) {
        Inventory inventory = findById(id);
        return removeInventory(inventory, InventoryQuantityChangeType.REVERSE_RECEIVING, documentNumber, comment);
    }
    public List<Inventory> removeInventoryByLocation(Long locationId) {
        return removeInventoryByLocation(locationId, true);
    }
    public List<Inventory> removeInventoryByLocation(Long locationId, boolean loadDetail) {
        List<Inventory> inventories = findByLocationId(locationId, loadDetail);
        inventories.forEach(
                inventory -> removeInventory(inventory, "", "")
        );

        return inventories;
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
            case REVERSE_PRODUCTION:
                inventoryActivityType = InventoryActivityType.REVERSE_PRODUCTION;
                break;
            case REVERSE_BY_PRODUCT:
                inventoryActivityType = InventoryActivityType.REVERSE_BY_PRODUCT;
                break;
            default:
                inventoryActivityType = InventoryActivityType.INVENTORY_ADJUSTMENT;
        }
        inventoryActivityService.logInventoryActivitiy(inventory, inventoryActivityType,
                "quantity", String.valueOf(inventory.getQuantity()), "0",
                documentNumber, comment);

        // ignore the integration when it is consumption of work order material
        // if (!inventoryQuantityChangeType.equals(InventoryQuantityChangeType.CONSUME_MATERIAL)) {

            integrationService.processInventoryAdjustment(InventoryQuantityChangeType.INVENTORY_ADJUST, inventory, inventory.getQuantity(), 0L);
        // }

        // clear the movement path
        inventory = clearMovementPath(inventory);
        logger.debug("Movement path for {} is removed!", inventory.getLpn());

        Location destination
                = warehouseLayoutServiceRestemplateClient.getLogicalLocationForAdjustInventory(
                inventoryQuantityChangeType, inventory.getWarehouseId().longValue()
        );
        inventory = moveInventory(inventory, destination);

        // see if we will need to relable the LPN so that the LPN can be reused
        logger.debug("check if we will need to reuse the LPN of the removed inventory");
        WarehouseConfiguration warehouseConfiguration = warehouseLayoutServiceRestemplateClient.getWarehouseConfiguration(inventory.getWarehouseId());
        if (Objects.nonNull(warehouseConfiguration) && Boolean.TRUE.equals(warehouseConfiguration.getReuseLPNAfterRemovedFlag())) {
            logger.debug("warehouse configuration is setup to reuse the LPN after removed");
            // the lpn will needs to be relabeled by adding a post fix with time stamp

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

            String strDate = LocalDateTime.now().format(formatter);
            String newLPN = inventory.getLpn() + "-REM-" + strDate;
            inventory = relabelLPN(inventory, newLPN);
        }
        return inventory;
    }

    private Inventory relabelLPN(Inventory inventory, String newLPN) {

        inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.RELABEL_LPN,
                "LPN", String.valueOf(inventory.getLpn()), newLPN,
                "", "");
        inventory.setLpn(newLPN);

        return saveOrUpdate(inventory);
    }

    public Inventory moveInventory(Inventory inventory, Location destination) {
        return moveInventory(inventory, destination, null, true, null);
    }
    public Inventory moveInventory(Long inventoryId, Location destination, Long pickId, boolean immediateMove,
                                   String destinationLpn) {
        return moveInventory(findById(inventoryId), destination, pickId, immediateMove,
                destinationLpn);

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
    public Inventory moveInventory(Inventory inventory, Location destination, Long pickId, boolean immediateMove,
                                   String destinationLpn) {
        if (immediateMove) {
            return processImmediateMoveInventory(inventory, destination, pickId, destinationLpn);
        }
        else {
            // For pick, we are not allow a movement work since pick itself is already
            // a work
            // we will ignore the destination LPN as well since when we genreate the movement work
            // we are not sure whether the LPN is still in the destination when the work is
            // actually done
            return generateMovementWork(inventory, destination);
        }
    }

    /**
     * Move the inventory into the new location,
     * @param inventory Inventory to be moved
     * @param destination destination fo the inventory
     * @param pickId pick work related to the inventory movement
     * @return inventory after the movement(can be consolidated with the existing invenotry in the destination)
     */
    @Transactional
    public Inventory processImmediateMoveInventory(Inventory inventory, Location destination, Long pickId,
                                                   String destinationLpn) {
        logger.debug("Start to move inventory {} to destination {}, pickId: {} is null? {}, new LPN? {}, movement path record: {}",
                inventory.getLpn(),
                destination.getName(),
                pickId,
                Objects.isNull(pickId),
                destinationLpn,
                inventory.getInventoryMovements().size());

        // logger.debug("==> Before the inventory move, the destination location {} 's volume is {}",
        //         warehouseLayoutServiceRestemplateClient.getLocationById(destination.getId()).getName(),
        //         warehouseLayoutServiceRestemplateClient.getLocationById(destination.getId()).getCurrentVolume());


        // If we passed in a destination LPN, Let's replace the LPN first
        // before the same location check.
        if (StringUtils.isNotBlank(destinationLpn) && !destinationLpn.equals(inventory.getLpn())) {
            inventory.setLpn(destinationLpn);

        }
        Location sourceLocation = inventory.getLocation();

        // quick check. If the source location equals the
        // destination, then we don't have to move
        if (Objects.equals(sourceLocation, destination)) {
            return saveOrUpdate(inventory);
        }
        inventory.setLocationId(destination.getId());
        inventory.setLocation(destination);

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

        if (Objects.isNull(pickId)) {
            recalculateMovementPathForInventoryMovement(inventory, destination);
        }
        else {

            markAsPicked(inventory, destination, pickId);
            logger.debug("After markAsPicked, we still have {} movement path on the inventory {}",
                    inventory.getInventoryMovements().size(), inventory.getLpn());
        }


        // Log a inventory movement activity before we can log
        // another possible inventory consolidation activity

        inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.INVENTORY_MOVEMENT,
                "location", sourceLocation.getName(), destination.getName());

        // if we are moving inventory to a production line inbound area, let's update the delivery
        // quantity of the work order

        if (Boolean.TRUE.equals(destination.getLocationGroup().getLocationGroupType().getProductionLineInbound())) {
            logger.debug("Inventory is moved into {}, which is a production line inbound location",
                    destination.getName());
            if (Objects.nonNull(inventory.getPick())) {

                logger.debug("will check if we will need to update the delivery quantity for work order line {}",
                        inventory.getPick().getWorkOrderLineId());
                WorkOrderMaterialConsumeTiming workOrderMaterialConsumeTiming =
                        workOrderServiceRestemplateClient.inventoryDeliveredForWorkOrderLine(
                        inventory.getPick().getWorkOrderLineId(),
                        inventory.getQuantity(),
                        destination.getId(), inventory.getId()
                );
                if (workOrderMaterialConsumeTiming.equals(
                        WorkOrderMaterialConsumeTiming.WHEN_DELIVER
                )) {
                    // ok the work order's consume time is when delivered
                    // the inventory being delivered should already be consumed.
                    // by the above call,
                    // we can simply return here
                    logger.debug("Inventory is moved to the production line and consumed right after");
                    return inventory;
                }
            }

        }

        // Reset the destination location's size
        recalculateLocationSizeForInventoryMovement(sourceLocation, destination, inventory.getSize());


        if (Objects.nonNull(inventory.getPickId())) {
            outbuondServiceRestemplateClient.refreshPickMovement(inventory.getPickId(), destination.getId(), inventory.getQuantity());
        }
        // consolidate the inventory at the destination, if necessary
        logger.debug("before consolidation, we still have {} movement path on the inventory {}",
                inventory.getInventoryMovements().size(), inventory.getLpn());
        logger.debug("if the destination location {} is in a virtual location group, we will skip the consolidation. Is virtual? {}",
                destination.getName(),
                Boolean.TRUE.equals(destination.getLocationGroup().getLocationGroupType().getVirtual()));
        Inventory consolidatedInventory =
                Boolean.TRUE.equals(destination.getLocationGroup().getLocationGroupType().getVirtual()) ?
                        inventory : inventoryConsolidationService.consolidateInventoryAtLocation(destination, inventory);

        logger.debug("After consolidation, we still have {} movement path on the inventory {}",
                consolidatedInventory.getInventoryMovements().size(), consolidatedInventory.getLpn());
        logger.debug("6. destination {} has {} inventory",
                destination.getName(), findByLocationId(destination.getId(), false).size());
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
                destination.getName(), findByLocationId(destination.getId(), false).size());
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


        // logger.debug("==> after the inventory move, the destination location {} 's volume is {}",
        //         warehouseLayoutServiceRestemplateClient.getLocationById(destination.getId()).getName(),
        //         warehouseLayoutServiceRestemplateClient.getLocationById(destination.getId()).getCurrentVolume());
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

    private  void recalculateMovementPathForPickedInventoryMovement(Inventory inventory, Location destination, Long pickId) {
        Pick pick = outbuondServiceRestemplateClient.getPickById(pickId);
        logger.debug("Start to setup movement path for inventory {}, current location {} / {}, pick's destination: {}",
                inventory.getLpn(),
                inventory.getLocationId(),
                destination.getId(),
                pick.getDestinationLocationId());

        // clear all inventory movement first.
        // if the inventory is already in the destination, then we will not assign any movement
        // otherwise, we will setup the destination of the pick as the only destination
        inventory.getInventoryMovements().clear();

        // if the inventory is already at the pick's destination, then do nothing
        if (destination.getId().equals(pick.getDestinationLocationId())) {
            logger.debug("the LPN is currently at its destination, no need to setup the movement path");
            return;
        }

        // if the inventory is picked inventory, then we will clear the
        // movement path that already assigned to the current inventory.
        // we want to start off with a clear movement path and assign
        // it with the ones only necessary for outbound
        InventoryMovement inventoryMovement = new InventoryMovement();
        inventoryMovement.setInventory(inventory);
        inventoryMovement.setLocationId(pick.getDestinationLocation().getId());
        inventoryMovement.setSequence(0);
        inventoryMovement.setWarehouseId(inventory.getWarehouseId());

        inventory.getInventoryMovements().add(inventoryMovement);


        logger.debug("Add destination {} to the movement path of picked inventory LPN / {}",
                pick.getDestinationLocation().getName(), inventory.getLpn());


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

        // If we are here, we know we have a movement path defined for the inventory before
        // and if we have nothing left, let's remove the possible work task that attached to
        // this inventory and movement path
        if (inventory.getInventoryMovements().size() == 0) {
            commonServiceRestemplateClient.removeWorkTask(inventory, WorkType.INVENTORY_MOVEMENT);
        }

    }

    // Note here the movement path only contains final destination location. We will need to get the
    // hop location as well and save it
    public Inventory setupMovementPath(Long inventoryId, List<InventoryMovement> inventoryMovements) {
        return setupMovementPath(findById(inventoryId), inventoryMovements);
    }
    public Inventory setupMovementPath(Inventory inventory, List<InventoryMovement> inventoryMovements) {

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
        // logger.debug(">> movement path: {}", inventoryMovements);
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
        // logger.debug("Will return following movement path to the end user: {}", inventory.getInventoryMovements());
        return inventory;
    }

    public Inventory markAsPicked(Long inventoryId, Location destination, Long pickId) {
        return markAsPicked(findById(inventoryId),destination,  pickId);
    }
    public Inventory markAsPicked(Inventory inventory, Location destination,  Long pickId) {
        if (Objects.nonNull(inventory.getAllocatedByPickId())) {
            // The inventory is allocated by certain pick. make sure it is
            // not picked by a different pick
            if (!pickId.equals(inventory.getAllocatedByPickId())) {
                throw InventoryException.raiseException("Inventory is allocated by other picks. Can't pick from it");
            }

            // after the inventory is picked, reset the allocated by pick id to null
            // so in case it is cancelled / unpicked, the inventory is still available
            // for other picks
            inventory.setAllocatedByPickId(null);

        }
        inventory.setPickId(pickId);
        Pick pick = outbuondServiceRestemplateClient.getPickById(pickId);
        // logger.debug("Get pick by id {} \n {}", pickId, pick);
        inventory.setPick(pick);
        inventory.getInventoryMovements().clear();
        logger.debug("Start to build movement path for picked inventory {}",
                inventory.getLpn());
        // if the inventory is not in the destination location yet, then copy
        // the whole movement path from the pick
        logger.debug("Inventory's current location: {} / {}, pick's destination location: {} / {}",
                inventory.getLocation().getName(),
                inventory.getLocationId(),
                Objects.isNull(pick.getDestinationLocation()) ? "" : pick.getDestinationLocation().getName(),
                pick.getDestinationLocationId());
        if (!inventory.getLocationId().equals(pick.getDestinationLocationId())) {

            // copy the pick movement and assign it to the inventory's movement
            logger.debug("Inventory is not in the destination yet, let's setup the movement");
            copyMovementsFromPick(pick, inventory);
        }
        inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.PICKING,
                "pick", String.valueOf(pickId), "", pick.getNumber());
        return inventory;
    }
    private void copyMovementsFromPick(Pick pick, Inventory inventory) {

        List<InventoryMovement> inventoryMovements = new ArrayList<>();
        pick.getPickMovements().stream().forEach(pickMovement -> {
            logger.debug("will copy {} as a hop for the picked inventory {}",
                    pickMovement.getLocationId(), inventory.getLpn());
            inventoryMovements.add(inventoryMovementService.createInventoryMovementFromPickMovement(inventory, pickMovement));
        });

        // Note the pick's movement path only contains the hop locations. We will need to set
        // the inventory's movement to include the final destination as well
        inventoryMovements.add(inventoryMovementService.createInventoryMovement(inventory, pick.getDestinationLocationId()));

        logger.debug("will copy {} as a destination for the picked inventory {}",
                pick.getDestinationLocationId(), inventory.getLpn());
        inventory.setInventoryMovements(inventoryMovements);
        logger.debug("Now we have {} movement path for the inventory {}",
                inventory.getInventoryMovements().size(), inventory.getLpn());
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
            newLpn = commonServiceRestemplateClient.getNextLpn(inventory.getWarehouseId());
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

    private Inventory generateMovementWork(Inventory inventory, Location destinationLocation) {
        // TO-DO setup the inventory movement path

        allocateLocation(inventory, destinationLocation);


        // Save the work task
        WorkTask workTask = WorkTask.createInventoryMovementWorkTask(
                inventory, destinationLocation
        );
        commonServiceRestemplateClient.addWorkTask(workTask);
        return inventory;
    }
    private Inventory allocateLocation(Inventory inventory, Location destinationLocation){
        logger.debug("start to allocate location for inventory: {}", inventory.getLpn());

        warehouseLayoutServiceRestemplateClient.allocateLocation(destinationLocation, inventory);

        InventoryMovement inventoryMovement = new InventoryMovement();
        inventoryMovement.setInventory(inventory);
        inventoryMovement.setLocation(destinationLocation);

        return setupMovementPath(inventory, Collections.singletonList(inventoryMovement));
    }

    private Long getWarehouseId(String companyCode, String warehouseName) {
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(companyCode, warehouseName);
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
        return addInventory(userService.getCurrentUserName(), inventory, inventoryQuantityChangeType,
                documentNumber, comment);
    }
    @Transactional
    public Inventory addInventory(String username, Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType,
                                  String documentNumber, String comment) {

        logger.debug("Start to add inventory with LPN {}",
                Strings.isBlank(inventory.getLpn()) ? "N/A" : inventory.getLpn());
        // if inventory's LPN is not setup, get next LPN for it
        if (Strings.isBlank(inventory.getLpn())) {

            inventory.setLpn(
                    commonServiceRestemplateClient.getNextLpn(inventory.getWarehouseId())
            );
        }
        if (isApprovalNeededForInventoryAdjust(inventory, 0L, inventory.getQuantity(), inventoryQuantityChangeType)) {

            logger.debug("We will need to get approval, so here we just save the request");
            writeInventoryAdjustRequest(inventory, 0L,  inventory.getQuantity(),
                    inventoryQuantityChangeType, documentNumber, comment);
            inventory.setLockedForAdjust(true);
            return inventory;
        } else {
            logger.debug("No approval needed, let's just go ahread with the adding inventory!");
            return processAddInventory(username, inventory, inventoryQuantityChangeType, documentNumber, comment);
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
        if (inventoryQuantityChangeType.isNoApprovalNeeded()) {
            logger.debug("By default, {} doesn't need approval for quantity change",
                    inventoryQuantityChangeType);
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
        return processAddInventory(
                userService.getCurrentUserName(),
                inventory, inventoryQuantityChangeType,
                documentNumber, comment);
    }
    public Inventory processAddInventory(String username,
                                         Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType,
                                         String documentNumber, String comment) {
        Location location =
                warehouseLayoutServiceRestemplateClient.getLogicalLocationForAdjustInventory(
                        inventoryQuantityChangeType, inventory.getWarehouseId());
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
        logger.debug("inventory {} needs QC? {}",
                inventory.getLpn(), inventory.getInboundQCRequired());


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
        try {
            username = userService.getCurrentUserName();

            inventoryActivityService.logInventoryActivitiy(inventory, inventoryActivityType,
                    username,
                    "quantity", "0", String.valueOf(inventory.getQuantity()),
                    documentNumber, comment);

        }
        catch (Exception ex) {
            logger.debug("If we are not able to get the current user from the context, " +
                    "then we will skip the inventory activity ");
        }

        // send integration to add a new inventory
        integrationService.processInventoryAdjustment(inventoryQuantityChangeType, inventory,
                0L, inventory.getQuantity(),
                documentNumber, comment);

        // if the inventory needs QC, then setup the QC items based on QC Rule
        if (inventory.getInboundQCRequired()) {
            logger.debug("The inventory {} needs QC,  ", inventory.getLpn() );
            setupQCInspectionRequest(inventory);
        }

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

    private void setupQCInspectionRequest(Inventory inventory) {
        logger.debug("Start to generate QC inspection request for inventory {} / {}",
                inventory.getId(), inventory.getLpn());

        // let's setup the item family and inventory status first, just in case
        // we may need to compare

        qcInspectionRequestService.generateInboundQCInspectionRequest(inventory);
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
        logger.debug("inventory's current quantity: {}, new Quantity: {}",
                inventory.getQuantity(), newQuantity);
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
            logger.debug("Will reduce the quantity from {} to {}",
                    inventory.getQuantity(), newQuantity);

            String newLpn = commonServiceRestemplateClient.getNextLpn(inventory.getWarehouseId());

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
            logger.debug("Inventory is split");

            // Remove the new inventory
            processRemoveInventory(newInventory, InventoryQuantityChangeType.INVENTORY_ADJUST,"", "");
            logger.debug("The inventory with reduced quantity has been removed");
            resultInventory =  inventory;
        }
        else {
            // if we are here, we are adjust quantity up
            // We will create a inventory in a logic location
            // and then move the inventory onto the existing inventory

            inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.INVENTORY_ADJUSTMENT,
                    "quantity", String.valueOf(inventory.getQuantity()), String.valueOf(newQuantity),
                    documentNumber, comment);
            logger.debug("Will increase the quantity from {} to {}",
                    inventory.getQuantity(), newQuantity);

            /***
             * NOTE: We will no long to create the inventory from the logic location with added quantity then
             * move it to the current location. Instead we will directly change the quantity of the original
             * inventory
             *
             *
            String newLpn = commonServiceRestemplateClient.getNextLpn(inventory.getWarehouseId());
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

             **/

            // send integration to add a new inventory
            integrationService.processInventoryAdjustment(InventoryQuantityChangeType.INVENTORY_ADJUST, inventory,
                    inventory.getQuantity(), newQuantity,
                    documentNumber, comment);

            inventory.setQuantity(newQuantity);
            return saveOrUpdate(inventory);
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

            logger.debug("Will start to add inventory after the change is approved");
            processAddInventory(inventory, inventoryAdjustmentRequest.getInventoryQuantityChangeType(),
                    inventoryAdjustmentRequest.getDocumentNumber(), inventoryAdjustmentRequest.getComment());
        }
        else {

            logger.debug("Will start to change inventory after the change is approved");
            processAdjustInventoryQuantity(inventory, inventoryAdjustmentRequest.getNewQuantity(),
                    inventoryAdjustmentRequest.getDocumentNumber(), inventoryAdjustmentRequest.getComment());
        }

        // check if we can release the location after the request is approved
        logger.debug("Start to release the lock on the inventory");
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
                        null,
                        null,
                        null,
                        null, null,
                        pickIds,
                        null,
                        null,
                        null, null, null
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


    private List<Inventory> getInventoryForConsume(Long warehouseId, Long inboundLocationId,
                                                   Long workOrderLineId, Long inventoryId,
                                                   String lpn, Boolean nonPickedInventory) throws IOException {
        if (Objects.nonNull(inventoryId)) {
            Inventory inventory = findById(inventoryId);
            return  Collections.singletonList(inventory);
        }
        else if (Boolean.TRUE.equals(nonPickedInventory)) {
            // we allow to consume the non picked inventory. So as long as the inventory is in
            // the location and it is a good item for the work order and
            // possible match with the lpn, then we will use it
            List<Inventory> inventories = findAll(
                    warehouseId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    inboundLocationId,
                    null,
                    null, null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    lpn,
                    null,
                    null, null, null
            );
            // we will only return the inventory without any pick attached to it
            return inventories.stream().filter(inventory -> Objects.isNull(inventory.getPickId())).collect(Collectors.toList());
        }
        else {
            // if the inventory id is not passed in, get the inventory that is picked for
            // the work order line
            List<Inventory> pickedInventories = new ArrayList<>();

            List<Pick> picks = outbuondServiceRestemplateClient.getWorkOrderPicks(warehouseId, String.valueOf(workOrderLineId));
            if (picks.size() > 0) {
                String pickIds = picks.stream()
                        .map(Pick::getId).map(String::valueOf).collect(Collectors.joining(","));
                pickedInventories = findAll(
                        warehouseId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        inboundLocationId,
                        null,
                        null,
                        null,
                        null, null,
                        null,
                        null,
                        pickIds,
                        null,
                        null,
                        null, null, null
                );
            }
            return pickedInventories;
        }
    }

    /**
     * Consume invenotry for work order
     * @param workOrderLineId  work order line
     * @param warehouseId
     * @param quantity quantity to be consumed
     * @param inboundLocationId location of the inventory to be consumed, we may have multiple production lines associated with the work order
     * @param inventoryId consume from a specific inventory
     * @param lpn consume from a specific lpn
     * @return
     */
    public List<Inventory> consumeInventoriesForWorkOrderLine(Long workOrderLineId, Long warehouseId,
                                                              Long quantity, Long inboundLocationId,
                                                              Long inventoryId,
                                                              String lpn,
                                                              Boolean nonPickedInventory) {

        logger.debug("# start to consume quantity {} from work order line id {}, location id {}" +
                ", specified inventory id? {}, specified lpn? {}, nonPickedInventory? {}",
                quantity,
                workOrderLineId,
                inboundLocationId,
                inventoryId, lpn,
                nonPickedInventory);
        try {
            List<Inventory> pickedInventories = getInventoryForConsume(warehouseId, inboundLocationId,
                    workOrderLineId, inventoryId, lpn, nonPickedInventory);

            if (pickedInventories.size() > 0) {

                logger.debug("# Get {} inventory to be consumed", pickedInventories.size());
                // Let's remove those inventories until we reach the
                // required quantity
                Long quantityToBeRemoved = quantity;
                Iterator<Inventory> inventoryIterator = pickedInventories.iterator();
                while (inventoryIterator.hasNext() && quantityToBeRemoved > 0) {
                    Inventory inventory = inventoryIterator.next();
                    logger.debug("# start to consume inventory {}, quantity {}",
                            inventory.getLpn(), quantityToBeRemoved);
                    if (inventory.getQuantity() <= quantityToBeRemoved) {
                        // The whole inventory can be removed
                        quantityToBeRemoved -= inventory.getQuantity();
                        removeInventory(inventory, InventoryQuantityChangeType.CONSUME_MATERIAL);

                        logger.debug("# consume the whole LPN {}, ", inventory.getLpn());


                    }
                    else {
                        // we only need to remove partial quantity of the inventory
                        // so we will need to split the inventory first
                        String newLpn = commonServiceRestemplateClient.getNextLpn(inventory.getWarehouseId());
                        // we will split the quantity to be consumed from the original inventory,
                        // save the origianl inventory and then remove the splited inventory
                        Inventory splitedInventory = inventory.split(newLpn, quantityToBeRemoved);
                        splitedInventory = save(splitedInventory);
                        saveOrUpdate(inventory);
                        quantityToBeRemoved -= splitedInventory.getQuantity();
                        removeInventory(splitedInventory, InventoryQuantityChangeType.CONSUME_MATERIAL);

                        logger.debug("# consume the partial LPN {}", inventory.getLpn());

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

    /**
     * Mark the whole LPN as allocated by certain pick
     * @param lpn lpn number
     * @param allocatedByPickId allocated by certain pick
     * @return inventory records under the LPN
     */
    public List<Inventory> markLPNAllocated(Long warehouseId, String lpn, Long allocatedByPickId) {
        List<Inventory> inventories = findByLpn(warehouseId, lpn, false);
        inventories.forEach(inventory -> {
            inventory.setAllocatedByPickId(allocatedByPickId);
            save(inventory);
        });
        return inventories;
    }

    /**
     * Release the LPN from its allocated pick. Normally this is invoked when
     * the whole LPN is allocated by a pick
     *
     * @param lpn
     * @param allocatedByPickId
     * @return
     */
    public List<Inventory> releaseLPNAllocated(Long warehouseId, String lpn, Long allocatedByPickId) {
        List<Inventory> inventories = findByLpn(warehouseId, lpn, false);
        inventories.stream()
                .filter(inventory -> allocatedByPickId.equals(inventory.getAllocatedByPickId()))
                .forEach(inventory -> {
                    inventory.setAllocatedByPickId(null);
                    save(inventory);
        });
        return inventories;
    }

    /**
     * Reverse the received inventory. We will remove this inventory from the system
     * and return the quantity back to the receipt
     * @param id
     * @return
     */
    @Transactional
    public Inventory reverseReceivedInventory(long id,
                                              Boolean reverseQCQuantity,
                                              Boolean allowReuseLPN) {
        Inventory inventory = findById(id);
        if (Objects.isNull(inventory.getReceiptLineId())) {
            throw InventoryException.raiseException("Can't reverse the inventory. We can't find the receipt attached to this inventory");
        }
        Long quantity = inventory.getQuantity();
        Inventory removedInventory = removeInventory(inventory, InventoryQuantityChangeType.REVERSE_RECEIVING);
        if (Boolean.TRUE.equals(inventory.getInboundQCRequired())) {
            // the inventory needs QC, we may need to remove all the QC request that
            // attached to this inventory
            logger.debug("The inventory needs QC, let's remove all the qc related data");
            qcInspectionRequestService.removeInboundQCInspectionRequest(inventory);
        }
        if (Boolean.TRUE.equals(allowReuseLPN)) {
            // if we allow the user to reuse the lPN, then let's remove the record
            delete(removedInventory.getId());
        }

        logger.debug("Inventory Reserved!");
        inboundServiceRestemplateClient.reverseReceivedInventory(
                inventory.getReceiptId(), inventory.getReceiptLineId(), quantity,
                inventory.getInboundQCRequired(), reverseQCQuantity
        );

        return removedInventory;

    }

    public Inventory clearMovementPath(long id) {

        return clearMovementPath(findById(id));
    }

    public Inventory clearMovementPath(Inventory inventory) {

        logger.debug("Start to clear the movement path LPN {}", inventory.getLpn());
        inventoryMovementService.clearInventoryMovement(inventory);
        inventory.setInventoryMovements(new ArrayList<>());

        // Remove the work task that related to the inventory movement
        // TO-DO
        // commonServiceRestemplateClient.removeWorkTask(inventory, WorkType.INVENTORY_MOVEMENT);

        return inventory;
    }

    public String validateNewLPN(Long warehouseId, String lpn) {
        List<Inventory> inventories =
                findByLpn(warehouseId, lpn, false);

        return Objects.isNull(inventories) || inventories.size() == 0 ? "" : ValidatorResult.VALUE_ALREADY_EXISTS.name();
    }

    /**
     * Generate LPN Label. Based on where the LPN comes from, we may generate LPN label for
     * 1. work order
     * 2. receipt
     * 3. LPN without work order and receipt
     * @param warehouseId
     * @param lpn
     * @param locale
     * @return
     * @throws JsonProcessingException
     */
    public ReportHistory generateLPNLabel(Long warehouseId, String lpn, String locale,
                                                 Long quantity, String printerName) throws JsonProcessingException {
        // see if we can find work order or receipt form the LPN
        // if we can only find one work order, or receipt from it, then we will call
        // the correspodent service to print the LPN label.
        // otherwise, if the LPN is mixed of work order and/or receipt and/or null, then we will print
        // an plain LPN label

        List<Inventory> inventories = findByLpn(warehouseId, lpn);
        Set<Long> workOrderIds = new HashSet<>();
        Set<Long> receiptLineIds = new HashSet<>();
        boolean notFromWorkOrderReceipt = false;
        boolean mixedSource = false;
        for (Inventory inventory : inventories) {
            if (Objects.isNull(inventory.getReceiptLineId()) && Objects.isNull(inventory.getWorkOrderId())) {
                notFromWorkOrderReceipt = true;
            }
            else if (Objects.nonNull(inventory.getReceiptLineId()) && Objects.isNull(inventory.getWorkOrderId())) {
                receiptLineIds.add(inventory.getReceiptLineId());
            }
            else if (Objects.isNull(inventory.getReceiptLineId()) && Objects.nonNull(inventory.getWorkOrderId())) {
                workOrderIds.add(inventory.getWorkOrderId());
            }
            else {
                // if we are here, it means the inventory has both receipt line and work order id set
                // which should never be the case
                mixedSource = true;
            }
        }
        if (mixedSource) {
            return generateLPNLabel(warehouseId, lpn, inventories, locale);
        }
        if (workOrderIds.size() > 1 || receiptLineIds.size() > 1) {
            // the LPN is mixed with differetn work order or receipt line
            return generateLPNLabel(warehouseId, lpn, inventories, locale);
        }
        if (notFromWorkOrderReceipt) {
            // the LPN has inventory record that not from work order nor
            // receipt line. No matter whether there's other inventory
            // record in the same LPN has record from work order / receipt line
            // we will print plain LPN Label
            return generateLPNLabel(warehouseId, lpn, inventories, locale);
        }
        // if we are here, we know the LPN is either from work order, or from
        // receipt, or both
        if (workOrderIds.size() > 0 && receiptLineIds.size() > 0) {
            // the LPN is mixed with inventory from work order and reciept
            return generateLPNLabel(warehouseId, lpn, inventories, locale);
        }
        else if (workOrderIds.size() > 0 ) {

            Long workOrderId = workOrderIds.iterator().next();
            logger.debug("start to print work order LPN by work order id {} for LPN {}",
                    workOrderId, lpn);
            return workOrderServiceRestemplateClient.printLPNLabel(
                    workOrderId, lpn, quantity, printerName
            );

        }
        else if (receiptLineIds.size() > 0){

            Long receiptLineId = receiptLineIds.iterator().next();
            logger.debug("start to print receipt LPN by receipt line id {} for LPN {}",
                    receiptLineId, lpn);
            return inboundServiceRestemplateClient.printLPNLabel(
                    receiptLineId, lpn, quantity, printerName
            );

        }
        else {
            // we should not be here.
            // the LPN is from work order or receipt
            // but both work order id set and receipt line set is empty
            // so we don't know where the LPN comes from
            throw InventoryException.raiseException("Can't print LPN label for " +  lpn + ", " +
                    "We don't know whether the LPN comes from receiving or work order production," +
                    " or none of them");
        }



    }
    public ReportHistory generateLPNLabel(Long warehouseId, String lpn,
                                          List<Inventory> inventories, String locale) throws JsonProcessingException {

        /***
        // setup parameters
        // TO-DO:
        // For now we have specific design for ecotech only
        // parameters
        // > productionLocation: java.lang.String
        // > itemName: java.lang.String
        // > itemDescription: java.lang.String
        // > workOrderNumber: java.lang.String
        // > completeDate: java.lang.String
        // > quantity: java.lang.String
        // > lpn: java.lang.String
        // > poNumber: java.lang.String
        // > supervisor: java.lang.String
        // A map to store the LPN label / report data
        // key: item - work order - poNumber
        // value: LPN report data
        Map<String, LpnReportData> lpnReportDataMap = new HashMap<>();


        inventories.forEach(inventory -> {

            String key = new StringBuilder()
                    .append(inventory.getItem().getName())
                    .append("-")
                    .append(inventory.getWorkOrderId())
                    .append("-")
                    .append(Objects.nonNull(inventory.getWorkOrder()) ? inventory.getWorkOrder().getPoNumber() : "")
                    .toString();
            if (lpnReportDataMap.containsKey(key)) {
                lpnReportDataMap.get(key).addQuantity(inventory.getQuantity());
            }
            else {
                lpnReportDataMap.put(key, new LpnReportData(inventory, userService.getCurrentUserName()));
            }


        });


        Report reportData = new Report();
        setupLPNReportData(
                reportData,  lpnReportDataMap.values()
        );

        logger.debug("Start to fill report by data: \n{}, ",
                reportData);

        logger.debug("will find a printer by : \n{}, ",
                reportData);
         **/
        // we will print one label per Item with its current quantity in the LPN
        // key: item id
        // value: item
        Map<Long, Item> itemMap = new HashMap<>();
        // key: item id
        // value: quantity
        Map<Long, Long> itemQuantityMap = new HashMap<>();

        for (Inventory inventory : inventories) {
            Long itemId = inventory.getItem().getId();
            itemMap.putIfAbsent(itemId, inventory.getItem());
            Long quantity = itemQuantityMap.getOrDefault(itemId, 0L);
            itemQuantityMap.put(itemId, quantity + inventory.getQuantity());
        }

        Report reportData = new Report();
        setupPrePrintLPNLabelData(
                reportData, lpn, itemMap, itemQuantityMap, 1
        );

        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.LPN_LABEL, reportData, locale
                );



        return reportHistory;
    }

    private void setupPrePrintLPNLabelData(Report reportData,
                                           String lpn,
                                           Map<Long, Item> itemMap,
                                           Map<Long, Long> itemQuantityMap,
                                           Integer copies) {

        List<Map<String, Object>> lpnLabelContents = new ArrayList<>();
        for (Map.Entry<Long, Item> itemEntries : itemMap.entrySet()) {
            Long itemId = itemEntries.getKey();
            Item item = itemEntries.getValue();
            Long quantity = itemQuantityMap.getOrDefault(itemId, 0L);

            Map<String, Object> lpnLabelContent =   getLPNLabelContent(
                    lpn, item, quantity
            );
            for (int i = 0; i < copies; i++) {
                lpnLabelContents.add(lpnLabelContent);
            }
        }

        reportData.setData(lpnLabelContents);

    }

    private Map<String, Object> getLPNLabelContent(String lpn, Item item, Long quantity) {

        Map<String, Object> lpnLabelContent = new HashMap<>();

        lpnLabelContent.put("lpn", lpn);
        lpnLabelContent.put("item_family", Objects.nonNull(item.getItemFamily()) ?
                item.getItemFamily().getDescription() : "");
        lpnLabelContent.put("item_name", item.getName());
        lpnLabelContent.put("quantity", quantity);

        return lpnLabelContent;

    }


    public void validateLPN(Long warehouseId, String lpn, boolean newInventory) {
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId);
        // let's get the company ID and then get the lpn validation rule
        if (!newInventory) {
            // if we are validate the inventory against an existing inventory,
            // let's make sure the inventory with the LPN exists in the 4 wall
            validateExistingLPN(warehouse, lpn);
        }
        else {
            // we will need to make sure the LPN doesn't exists yet and
            // if we defined the validation rules, make sure the new LPN follow the rule
            validateNonExistingLPN(warehouse, lpn);
        }

    }


    /**
     * Make sure the LPN is in the warehouse and four wall area
     * @param warehouse
     * @param lpn
     */
    private void validateExistingLPN(Warehouse warehouse, String lpn) {
        // first, make sure the LPN exists in a four wall area
        List<Inventory> inventories = findByLpn(warehouse.getId(), lpn);
        if (inventories.size() == 0) {
            throw InventoryException.raiseException("Can't find existing inventory by LPN " + lpn);
        }
        for (Inventory inventory : inventories) {
            if (inventory.getLocation().getLocationGroup().getLocationGroupType().getFourWallInventory() == false) {
                throw InventoryException.raiseException("LPN " + lpn + " is not in a four wall location");
            }
        }
    }

    /**
     * Make sure the LPN is a new LPN(not in the system) and it follow the right pattern(if defined by inventory configuration)
     * @param warehouse
     * @param lpn
     */
    private void validateNonExistingLPN(Warehouse warehouse, String lpn) {
        // first, make sure the LPN exists in a four wall area
        List<Inventory> inventories = findByLpn(warehouse.getId(), lpn);
        if (inventories.size() > 0) {
            throw InventoryException.raiseException("LPN " + lpn + " already exists");
        }
        // see if we defined any rules to validate the LPN
        InventoryConfiguration inventoryConfiguration =
                inventoryConfigurationService.getLPNValidationRule(warehouse.getId());
        if (Objects.nonNull(inventoryConfiguration)) {
            // ok we have some rule defined to validate the lpn
            String lpnValidationRule = inventoryConfiguration.getLpnValidationRule();

            Pattern pattern = Pattern.compile(lpnValidationRule, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(lpn);
            boolean matchFound = matcher.find();
            if(matchFound == false) {
                throw InventoryException.raiseException("LPN " + lpn + " doesn't match with rule " + lpnValidationRule);
            }
        }


    }

    public List<Inventory> getQCRequiredInventory(Long warehouseId, Long locationId,
                                                  String locationName,
                                                  Long locationGroupId, String locationGroupIds,
                                                  Long itemId, String itemName) {
        // we will only return inventory in qc locations
        List<Location> qcLocations = warehouseLayoutServiceRestemplateClient.getQCLocations(warehouseId);
        if (qcLocations.size() == 0) {
            return new ArrayList<>();
        }

        String locationIds = qcLocations.stream().map(
                qcLocation -> qcLocation.getId()
        ).map(id -> id.toString() )
                .collect( Collectors.joining( "," ) );

        logger.debug("We will only return invenotry from locations: \n {}",
                locationIds);
        return findAll(warehouseId,
                itemId,
                itemName,
                null,
                null,
                null,
                null,
                null,
                locationName,
                locationId,
                locationIds,
                locationGroupId,
                null,
                null,
                null,
                null,
                null, null,
                null,
                null,
                null,
                null, null);


    }

    public List<Inventory> removeInventores(String inventoryIds) {

        return Arrays.stream(inventoryIds.split(",")).map(
                id -> Long.parseLong(id)
        ).map(id -> removeInventory(id, "", "")).collect(Collectors.toList());

    }

    public Long getAvailableQuantityForMPS(Long warehouseId, Long itemId, String itemName) {

        return getAvailableInventoryForMPS(warehouseId, itemId, itemName).stream()
                .mapToLong(Inventory::getQuantity).sum();

    }

    public List<Inventory> getAvailableInventoryForMPS(Long warehouseId, Long itemId, String itemName) {

        // we will only return available inventory status
        Optional<InventoryStatus> availableInventoryStatus =
                inventoryStatusService.getAvailableInventoryStatus(warehouseId);
        if (!availableInventoryStatus.isPresent()) {
            // if we can't find a available status
            logger.debug("Can't get available quantity for MPS with item id {} and item name {}" +
                            " as we can't find any available status",
                    itemId, itemName);
            return Collections.emptyList();
        }

        Stream<Inventory> inventoryStream;
        if (Objects.nonNull(itemId)) {
            inventoryStream = inventoryRepository
                    .findByItemIdAndInventoryStatusId(itemId, availableInventoryStatus.get().getId())
                    .stream();

        }
        else if (Strings.isNotBlank(itemName)) {
            inventoryStream =  inventoryRepository
                    .findByItemNameAndInventoryStatusId(warehouseId, itemName, availableInventoryStatus.get().getId())
                    .stream();
        }
        else {
            throw InventoryException.raiseException("at least item id or item name needs to be passed in");
        }
        return inventoryStream
                .map(inventory -> {
                    // setup the location so we can filter the inventory by four wall inventory only
                    if (Objects.nonNull(inventory.getLocationId()) &&
                            Objects.isNull(inventory.getLocation())) {
                        inventory.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(inventory.getLocationId()));
                    }
                    return inventory;
                })
                .filter(inventory -> inventory.getLocation().getLocationGroup().getLocationGroupType().getFourWallInventory())
                .collect(Collectors.toList());


    }

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        logger.debug("start to process item override, current warehouse {}, from item id {} to item id {}",
                warehouseId, oldItemId, newItemId);
        // let's get the first item package type id for the new item id
        Item item = itemService.findById(newItemId);
        if (Objects.nonNull(item) && Objects.nonNull(item.getItemPackageTypes()) &&
                !item.getItemPackageTypes().isEmpty()) {
            inventoryRepository.processItemOverride(
                    oldItemId, newItemId, item.getItemPackageTypes().get(0).getId(),
                    warehouseId);

        }

    }



    public void removeAllInventories(Long warehouseId,
                                                            Long itemId,
                                                            String itemName,
                                                            String itemPackageTypeName,
                                                            Long clientId,
                                                            String clientIds,
                                                            String itemFamilyIds,
                                                            Long inventoryStatusId,
                                                            String locationName,
                                                            Long locationId,
                                                            String locationIds,
                                                            Long locationGroupId,
                                                            String receiptId,
                                                            String customerReturnOrderId,
                                                            Long workOrderId,
                                                            String workOrderLineIds,
                                                            String workOrderByProductIds,
                                                            String pickIds,
                                                            String lpn,
                                                            String inventoryIds,
                                                            Boolean notPutawayInventoryOnly,
                                                            Boolean includeVirturalInventory,
                                                            ClientRestriction clientRestriction) {
        List<Inventory> inventories = findAll(warehouseId, itemId,
                itemName, itemPackageTypeName, clientId, clientIds, itemFamilyIds, inventoryStatusId,
                locationName, locationId, locationIds, locationGroupId,
                receiptId, customerReturnOrderId,  workOrderId, workOrderLineIds,
                workOrderByProductIds,
                pickIds, lpn,
                inventoryIds, notPutawayInventoryOnly, includeVirturalInventory,
                clientRestriction,
                false);

        logger.debug("find {} inventory to REMOVE by criteria {}",
                inventories.size(),
                new StringBuilder()
                        .append("warehouseId: ").append(warehouseId)
                        .append("itemId: ").append(itemId)
                        .append("itemName: ").append(itemName)
                        .append("itemPackageTypeName: ").append(itemPackageTypeName)
                        .append("clientId: ").append(clientId)
                        .append("clientIds: ").append(clientIds)
                        .append("itemFamilyIds: ").append(itemFamilyIds)
                        .append("inventoryStatusId: ").append(inventoryStatusId)
                        .append("receiptId: ").append(receiptId)
                        .append("customerReturnOrderId: ").append(customerReturnOrderId)
                        .append("workOrderId: ").append(workOrderId)
                        .append("workOrderLineIds: ").append(workOrderLineIds)
                        .append("workOrderByProductIds: ").append(workOrderByProductIds)
                        .append("pickIds: ").append(pickIds)
                        .append("lpn: ").append(lpn)
                        .append("inventoryIds: ").append(inventoryIds)
                        .append("notPutawayInventoryOnly: ").append(notPutawayInventoryOnly)
                        .append("includeVirturalInventory: ").append(includeVirturalInventory)
                        .append("clientRestriction: ").append(clientRestriction));

        inventories.forEach(inventory -> {
            logger.debug("start to remove inventory {}, lpn {}", inventory.getId(), inventory.getLpn());
            delete(inventory.getId());
            logger.debug("inventory {} / {} is removed", inventory.getId(), inventory.getLpn());
        });

        logger.debug("{} inventory is removed", inventories.size());

    }

    public List<QuickbookDesktopInventorySummary> getQuickbookDesktopInventorySummary(String companyCode,
                                                                                      String warehouseName,
                                                                                      String itemName) {

        Long warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseByName(companyCode, warehouseName).getId();

        logger.debug("start get quickbook inventory summary by company {}, warehouse {}, warehouse id {}, item name: {}",
                companyCode, warehouseName, warehouseId, itemName);
        List<Object[]> inventorySummariesResult = new ArrayList<>();
        if (Strings.isNotBlank(itemName)) {

            inventorySummariesResult =
                    inventoryRepository.getQuickbookDesktopInventorySummary(warehouseId, itemName);
        }
        else {
            inventorySummariesResult =
                inventoryRepository.getQuickbookDesktopInventorySummary(warehouseId);
        }

        logger.debug("get {} inventory summary records", inventorySummariesResult.size());

        List<QuickbookDesktopInventorySummary> inventorySummaries =
                inventorySummariesResult.stream().map(
                        inventorySummaryRecord -> new QuickbookDesktopInventorySummary(
                                inventorySummaryRecord[0].toString(),
                                inventorySummaryRecord[1].toString(),
                                inventorySummaryRecord[2].toString(),
                                Long.parseLong(inventorySummaryRecord[3].toString())
                        )
                ).collect(Collectors.toList());

        inventorySummaries.forEach(
                inventorySummary -> logger.debug("==========    Quickbook inventorySummary   ===========\n{}", inventorySummary)
        );

        return inventorySummaries;
    }


    @Transactional
    public String uploadInventoryData(Long warehouseId,
                                               File file, Boolean removeExistingInventory) throws IOException {

        String fileUploadProgressKey = warehouseId + "-" + userService.getCurrentUserName() + "-" + System.currentTimeMillis();
        inventoryFileUploadProgress.put(fileUploadProgressKey, 0.0);

        List<InventoryCSVWrapper> inventoryCSVWrappers = loadData(file);

        logger.debug("get {} record from the file", inventoryCSVWrappers.size());
        // let's clear all the empty space for the name
        inventoryCSVWrappers.forEach(
                inventoryCSVWrapper -> {
                    inventoryCSVWrapper.setCompany(
                            inventoryCSVWrapper.getCompany().trim()
                    );
                    inventoryCSVWrapper.setWarehouse(
                            inventoryCSVWrapper.getWarehouse().trim()
                    );
                    inventoryCSVWrapper.setItem(
                            inventoryCSVWrapper.getItem().trim()
                    );
                    inventoryCSVWrapper.setItemPackageType(
                            inventoryCSVWrapper.getItemPackageType().trim()
                    );
                    inventoryCSVWrapper.setInventoryStatus(
                            inventoryCSVWrapper.getInventoryStatus().trim()
                    );
                    inventoryCSVWrapper.setLocation(
                            inventoryCSVWrapper.getLocation().trim()
                    );
                    inventoryCSVWrapper.setLpn(
                            inventoryCSVWrapper.getLpn().trim()
                    );
                }
        );
        List<Inventory> inventories = convertFromWrapper(warehouseId, inventoryCSVWrappers, true);
        logger.debug("convert {} of the records into inventory structure",
                inventories.size());

        Set<Long> locationIds = new HashSet<>();
        Set<Long> locationGroupIds = new HashSet<>();

        for (Inventory inventory : inventories) {
            locationIds.add(inventory.getLocationId());
            if (Objects.nonNull(inventory.getLocation()) &&
                    Objects.nonNull(inventory.getLocation().getLocationGroup())) {
                locationGroupIds.add(inventory.getLocation().getLocationGroup().getId());
            }
        }

        // ok, we will need to remove the existing inventory.
        // let's find the locations that have inventory
        String locationIdString = locationIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        locationIds = findInventoryByLocationIds(warehouseId, locationIdString)
                .stream().map(inventory -> inventory.getLocationId()).collect(Collectors.toSet());


        logger.debug("we have   {} locations of inventory that we will remove the inventory", locationIds.size());

        logger.debug("Get {} location group ids to load, {}",
                locationGroupIds.size(), locationGroupIds);



        // load the cache so that we won't need to do it again in the new thread as in the new
        // thread, we will lose the request context
        logger.debug("start to load cache so we don't have to request a http call " +
                "in a non http context thread");
        warehouseLayoutServiceRestemplateClient.setupLocalCache(warehouseId,
                locationGroupIds);


        inventoryFileUploadProgress.put(fileUploadProgressKey, 20.0);
        // end of load cache

        // we will use thread to save the inventory
        // get the username of the current user and pass it into the new thread
        String username = userService.getCurrentUserName();
        // List<Inventory> finalInventoryToBeRemoved = inventoryToBeRemoved;
        Set<Long> finalLocationIds = locationIds;
        new Thread(() -> {
            logger.debug("Get {} location ids to be cleared, {}",
                    finalLocationIds.size(), finalLocationIds);
            if (Boolean.TRUE.equals(removeExistingInventory) && !finalLocationIds.isEmpty()) {
                // remove all the inventory from the location first

                int i = 0;
                for (Long finalLocationId : finalLocationIds) {

                    deleteInventoryByLocation(finalLocationId);
                    logger.debug("inventory is removed from location {}",  finalLocationId);

                    inventoryFileUploadProgress.put(fileUploadProgressKey, 20.0 + i * 20.0 / finalLocationIds.size());
                    i++;
                }
                logger.debug("cleared {} locations so we can load the inventory into those locations",
                        finalLocationIds.size() );
            }
            else {
                logger.debug("There's no need to clear those locations");
            }
            /**
            logger.debug("Get {} location ids to be cleared, {}",
                    locationIds.size(), locationIds);
            if (Boolean.TRUE.equals(removeExistingInventory) && !finalInventoryToBeRemoved.isEmpty()) {
                // remove all the inventory from the location first

                for (int i = 0; i < finalInventoryToBeRemoved.size(); i++) {
                    removeInventory(finalInventoryToBeRemoved.get(i), "", "");
                    logger.debug("LPN {} is removed from location {}",
                            finalInventoryToBeRemoved.get(0).getLpn(),
                                Objects.nonNull(finalInventoryToBeRemoved.get(i).getLocation()) ?
                                finalInventoryToBeRemoved.get(i).getLocation().getName() : "UNKNOWN");

                    inventoryFileUploadProgress.put(fileUploadProgressKey, 20.0 + i * 20.0 / finalInventoryToBeRemoved.size());
                }
                logger.debug("cleared {} locations( {} inventory records) so we can load the inventory into those locations",
                        locationIds.size(),
                        finalInventoryToBeRemoved.size());
            }
            else {
                logger.debug("There's no need to clear those locations");
            }
             */
            inventoryFileUploadProgress.put(fileUploadProgressKey, 40.0);
            for (int i = 0; i < inventories.size(); i++) {

                logger.debug("start to add inventory {} from the uploaded file",
                        inventories.get(i).getLpn());
                addInventory(username, inventories.get(i),
                        InventoryQuantityChangeType.INVENTORY_UPLOAD,
                        "", "");

                inventoryFileUploadProgress.put(fileUploadProgressKey, 40.0 + i * 60.0 / inventories.size());
            }

            // once we complete, we will remove from the map

            inventoryFileUploadProgress.remove(fileUploadProgressKey);
            // and clear the local cache
            warehouseLayoutServiceRestemplateClient.clearLocalCache();
            /**
            inventories.stream().forEach(
                    inventory -> {

                        addInventory(username, inventory,
                                InventoryQuantityChangeType.INVENTORY_UPLOAD,
                                "", "");

                    });
             **/
        }).start();
        logger.debug("we will return the key {}", fileUploadProgressKey);

        return fileUploadProgressKey;

        /***
        // if we will remove the inventory from the location before we add inventory, make sure
        // we will only remove the inventory
        Set<Long> locationsHaveInventoryRemoved = new HashSet<>();

        return inventoryCSVWrappers.stream().map(inventoryCSVWrapper -> {

            try{

                Location destination =
                        warehouseLayoutServiceRestemplateClient.getLocationByName(
                                warehouseId,
                                inventoryCSVWrapper.getLocation()
                        );
                // the location should be a valid location . otherwise, let's just skip this one
                if (Objects.nonNull(destination)) {

                    if (Boolean.TRUE.equals(removeExistingInventory) && !locationsHaveInventoryRemoved.contains(destination.getId())) {
                        removeInventoryByLocation(destination.getId());
                        locationsHaveInventoryRemoved.add(destination.getId());
                        logger.debug("Inventory from location {} is cleared!",
                                destination.getName());
                    }

                    Inventory savedInvenotry =
                            addInventory(convertFromWrapper(inventoryCSVWrapper),
                                    InventoryQuantityChangeType.INVENTORY_UPLOAD,
                                    "", "");

                    // Inventory savedInvenotry = saveOrUpdate(convertFromWrapper(inventoryCSVWrapper));
                    // re-calculate the size of the location
                    logger.debug("Save inventory with LPN {}, id {} / {}",
                            inventoryCSVWrapper.getLpn(),
                            savedInvenotry.getId(),
                            savedInvenotry.getItem().getName());

                    recalculateLocationSizeForInventoryMovement(null, destination, savedInvenotry.getSize());
                    return savedInvenotry;
                }

            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }).filter(inventory -> Objects.nonNull(inventory)).collect(Collectors.toList());

         **/
    }

    public double getInventoryFileUploadProgress(String key) {
        return inventoryFileUploadProgress.getOrDefault(key, 100.0);
    }
}