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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
 import org.springframework.stereotype.Service;


import javax.persistence.criteria.*;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
@Service
public class InventoryService {
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    HttpServletRequest httpServletRequest;

    @Autowired
    private DateTimeService dateTimeService;
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

    private final static int INVENTORY_FILE_UPLOAD_MAP_SIZE_THRESHOLD = 20;
    private Map<String, Double> inventoryFileUploadProgress = new ConcurrentHashMap<>();
    private Map<String, List<FileUploadResult>> inventoryFileUploadResults = new ConcurrentHashMap<>();

    private Map<String, Double> inventoryPutawayFileUploadProgress = new ConcurrentHashMap<>();
    private Map<String, List<FileUploadResult>> inventoryPutawayFileUploadResults = new ConcurrentHashMap<>();

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
    private InventoryMixRestrictionService inventoryMixRestrictionService;

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
                                   String itemNames,
                                   String itemPackageTypeName,
                                   Long clientId,
                                   String clientIds,
                                   String itemFamilyIds,
                                   Long inventoryStatusId,
                                   String locationName,
                                   Long locationId,
                                   String locationIds,
                                   Long locationGroupId,
                                   Long pickZoneId,
                                   String receiptId,
                                   String receiptIds,
                                   String receiptNumber,
                                   String customerReturnOrderId,
                                   Long workOrderId,
                                   String workOrderLineIds,
                                   String workOrderByProductIds,
                                   String pickIds,
                                   String lpn,
                                   String color,
                                   String productSize,
                                   String style,
                                   String attribute1,
                                   String attribute2,
                                   String attribute3,
                                   String attribute4,
                                   String attribute5,
                                   String inventoryIds,
                                   Boolean notPutawayInventoryOnly,
                                   Boolean includeVirturalInventory,
                                   ClientRestriction clientRestriction,
                                   Integer maxLPNCount) {
        return findAll(warehouseId, itemId,
                itemName, itemNames, itemPackageTypeName, clientId, clientIds, itemFamilyIds, inventoryStatusId,
                locationName, locationId, locationIds, locationGroupId, pickZoneId,
                receiptId, receiptIds, receiptNumber,
                customerReturnOrderId,  workOrderId, workOrderLineIds,
                workOrderByProductIds,
                pickIds, lpn, color, productSize, style,
                attribute1, attribute2, attribute3, attribute4, attribute5,
                inventoryIds, notPutawayInventoryOnly, includeVirturalInventory,
                clientRestriction,
                true, maxLPNCount);
    }

    public List<Inventory> findAll(Long warehouseId,
                                   Long itemId,
                                   String itemName,
                                   String itemNames,
                                   String itemPackageTypeName,
                                   Long clientId,
                                   String clientIds,
                                   String itemFamilyIds,
                                   Long inventoryStatusId,
                                   String locationName,
                                   Long locationId,
                                   String locationIds,
                                   Long locationGroupId,
                                   Long pickZoneId,
                                   String receiptId,
                                   String receiptIds,
                                   String receiptNumber,
                                   String customerReturnOrderId,
                                   Long workOrderId,
                                   String workOrderLineIds,
                                   String workOrderByProductIds,
                                   String pickIds,
                                   String lpn,
                                   String color,
                                   String productSize,
                                   String style,
                                   String attribute1,
                                   String attribute2,
                                   String attribute3,
                                   String attribute4,
                                   String attribute5,
                                   String inventoryIds,
                                   Boolean notPutawayInventoryOnly,
                                   Boolean includeVirturalInventory,
                                   ClientRestriction clientRestriction,
                                   boolean includeDetails,
                                   Integer maxLPNCount) {

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
                    if (StringUtils.isNotBlank(itemName) || StringUtils.isNotBlank(clientIds) ||
                            Strings.isNotBlank(itemNames)) {
                        Join<Inventory, Item> joinItem = root.join("item", JoinType.INNER);
                        if (StringUtils.isNotBlank(itemName)) {

                            if (itemName.contains("*")) {
                                predicates.add(criteriaBuilder.like(joinItem.get("name"), itemName.replaceAll("\\*", "%")));
                            }
                            else {
                                predicates.add(criteriaBuilder.equal(joinItem.get("name"), itemName));
                            }
                        }

                        if (Strings.isNotBlank(itemNames)) {
                            CriteriaBuilder.In<String> inItemNames = criteriaBuilder.in(joinItem.get("name"));
                            for(String name : itemNames.split(",")) {
                                inItemNames.value(name);
                            }
                            predicates.add(criteriaBuilder.and(inItemNames));

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
                        if (locationName.contains("*")) {

                            List<Location> locations = warehouseLayoutServiceRestemplateClient.getLocationsByName(warehouseId, locationName);

                            if (locations.isEmpty()) {
                                // since the user passed in a wrong location, we will add a
                                // wrong id into the query so it won't return anything
                                predicates.add(criteriaBuilder.equal(root.get("locationId"), -9999));
                            }
                            else {

                                CriteriaBuilder.In<Long> inLocationIds = criteriaBuilder.in(root.get("locationId"));
                                for(Long id : locations.stream().map(location -> location.getId()).collect(Collectors.toSet())) {
                                    inLocationIds.value(id);
                                }
                                predicates.add(criteriaBuilder.and(inLocationIds));
                            }
                        }
                        else {
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
                    if (StringUtils.isNotBlank(receiptIds)) {
                        CriteriaBuilder.In<Long> inReceiptIds = criteriaBuilder.in(root.get("receiptId"));
                        for(String id : receiptIds.split(",")) {
                            inReceiptIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inReceiptIds));
                    }

                    if (Strings.isNotBlank(receiptNumber)) {
                        Receipt receipt = inboundServiceRestemplateClient.getReceiptByNumber(
                                warehouseId,
                                receiptNumber
                        );
                        if (Objects.nonNull(receipt)) {

                            predicates.add(criteriaBuilder.equal(root.get("receiptId"), receipt.getId()));
                        }
                        else {

                            // since there's no receipt match with the receipt, we will use -1 as the receipt id
                            // to match with the inventory and we will assume there won't be anything return
                            predicates.add(criteriaBuilder.equal(root.get("receiptId"), -1));
                        }
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
                                lpn, lpn.contains("*"));
                        if (lpn.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("lpn"), lpn.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("lpn"), lpn));
                        }

                    }

                    if (StringUtils.isNotBlank(color)) {
                        if (color.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("color"), color.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("color"), color));
                        }
                    }
                    if (StringUtils.isNotBlank(productSize)) {
                        if (productSize.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("productSize"), productSize.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("productSize"), productSize));
                        }
                    }
                    if (StringUtils.isNotBlank(style)) {
                        if (style.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("style"), style.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("style"), style));
                        }
                    }

                    if (StringUtils.isNotBlank(attribute1)) {
                        if (attribute1.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("attribute1"), attribute1.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("attribute1"), attribute1));
                        }
                    }
                    if (StringUtils.isNotBlank(attribute2)) {
                        if (attribute2.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("attribute2"), attribute2.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("attribute2"), attribute2));
                        }
                    }
                    if (StringUtils.isNotBlank(attribute3)) {
                        if (attribute3.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("attribute3"), attribute3.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("attribute3"), attribute3));
                        }
                    }
                    if (StringUtils.isNotBlank(attribute4)) {
                        if (attribute4.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("attribute4"), attribute4.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("attribute4"), attribute4));
                        }
                    }
                    if (StringUtils.isNotBlank(attribute5)) {
                        if (attribute5.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("attribute5"), attribute5.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("attribute5"), attribute5));
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

                    // special handling for 3pl
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));

                    // return addClientRestriction(predicate, clientRestriction,
                    //        root, criteriaBuilder);
                    return Objects.isNull(clientRestriction) ?
                            predicate :
                            clientRestriction.addClientRestriction(predicate,
                                    root, criteriaBuilder);


                }
        );

        if (Objects.nonNull(maxLPNCount)) {
            logger.debug("we will only return {} LPNs as restrict", maxLPNCount);
            List<Inventory> lpnInventories = new ArrayList<>();
            Set<String> processedLPN = new HashSet<>();
            for(Inventory inventory : inventories) {
                if (maxLPNCount <= 0) {
                    break;
                }
                else if (processedLPN.contains(inventory.getLpn())) {
                    lpnInventories.add(inventory);
                }
                else {
                    lpnInventories.add(inventory);
                    processedLPN.add(inventory.getLpn());
                    maxLPNCount--;
                }
            }
            inventories = lpnInventories;
            logger.debug("after apply the LPN Count restriction, we only have {} inventory record left", inventories.size());
        }

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

            inventories = inventories.stream().filter(inventory -> locationMap.containsKey(inventory.getLocationId())).collect(Collectors.toList());
        }

        if (pickZoneId != null) {

            List<Location> locations =
                    warehouseLayoutServiceRestemplateClient.getLocationByPickZones(
                            warehouseId, String.valueOf(pickZoneId));
            // convert the list of locations to map of Long so as to speed up
            // when compare the inventory's location id with the locations from the group
            Map<Long, Long> locationMap = new HashMap<>();
            locations.stream().forEach(location -> locationMap.put(location.getId(), location.getId()));

            inventories =  inventories.stream().filter(inventory -> locationMap.containsKey(inventory.getLocationId())).collect(Collectors.toList());
        }

        logger.debug("====> after : {} millisecond(1/1000 second) @ {}, we will return inventory for {} record",
                ChronoUnit.MILLIS.between(currentLocalDateTime, LocalDateTime.now()),
                LocalDateTime.now(),
                inventories.size());

        return inventories;
    }

    public Page<Inventory> findAllByPagination(Long warehouseId,
                                               Long itemId,
                                               String itemName,
                                               String itemNames,
                                               String itemPackageTypeName,
                                               Long clientId,
                                               String clientIds,
                                               String itemFamilyIds,
                                               Long inventoryStatusId,
                                               String locationName,
                                               Long locationId,
                                               String locationIds,
                                               Long locationGroupId,
                                               Long pickZoneId,
                                               String receiptId,
                                               String receiptIds,
                                               String receiptNumber,
                                               String customerReturnOrderId,
                                               Long workOrderId,
                                               String workOrderLineIds,
                                               String workOrderByProductIds,
                                               String pickIds,
                                               String lpn,
                                               String color,
                                               String productSize,
                                               String style,
                                               String attribute1,
                                               String attribute2,
                                               String attribute3,
                                               String attribute4,
                                               String attribute5,
                                               String inventoryIds,
                                               Boolean includeVirturalInventory,
                                               ClientRestriction clientRestriction,
                                               Pageable pageable) {

        List<String> locationIdsInGroup = null;
        if (locationGroupId != null) {

            List<Location> locations =
                    warehouseLayoutServiceRestemplateClient.getLocationByLocationGroups(
                            warehouseId, String.valueOf(locationGroupId));
            locationIdsInGroup = locations.stream().map(location -> location.getId().toString()).collect(Collectors.toList());
        }

        List<String> locationIdsInPickZone = null;
        if (pickZoneId != null) {

            List<Location> locations =
                    warehouseLayoutServiceRestemplateClient.getLocationByPickZones(
                            warehouseId, String.valueOf(pickZoneId));

            locationIdsInPickZone = locations.stream().map(location -> location.getId().toString()).collect(Collectors.toList());
        }

        List<String> finalLocationIdsInGroup = locationIdsInGroup;
        List<String> finalLocationIdsInPickZone = locationIdsInPickZone;
        return inventoryRepository.findAll(
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
                    if (StringUtils.isNotBlank(itemName) || StringUtils.isNotBlank(clientIds) ||
                       Strings.isNotBlank(itemNames)) {
                        Join<Inventory, Item> joinItem = root.join("item", JoinType.INNER);
                        if (StringUtils.isNotBlank(itemName)) {

                                if (itemName.contains("*")) {
                                    predicates.add(criteriaBuilder.like(joinItem.get("name"), itemName.replaceAll("\\*", "%")));
                                }
                                else {
                                    predicates.add(criteriaBuilder.equal(joinItem.get("name"), itemName));
                                }
                        }

                        if (Strings.isNotBlank(itemNames)) {
                            CriteriaBuilder.In<String> inItemNames = criteriaBuilder.in(joinItem.get("name"));
                            for(String name : itemNames.split(",")) {
                                inItemNames.value(name);
                            }
                            predicates.add(criteriaBuilder.and(inItemNames));

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
                        if (locationName.contains("*")) {

                            List<Location> locations = warehouseLayoutServiceRestemplateClient.getLocationsByName(warehouseId, locationName);

                            if (locations.isEmpty()) {
                                // since the user passed in a wrong location, we will add a
                                // wrong id into the query so it won't return anything
                                predicates.add(criteriaBuilder.equal(root.get("locationId"), -9999));
                            }
                            else {

                                CriteriaBuilder.In<Long> inLocationIds = criteriaBuilder.in(root.get("locationId"));
                                for(Long id : locations.stream().map(location -> location.getId()).collect(Collectors.toSet())) {
                                    inLocationIds.value(id);
                                }
                                predicates.add(criteriaBuilder.and(inLocationIds));
                            }
                        }
                        else {
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

                    }

                    if (StringUtils.isNotBlank(locationIds)) {

                        CriteriaBuilder.In<Long> inLocationIds = criteriaBuilder.in(root.get("locationId"));
                        for(String id : locationIds.split(",")) {
                            inLocationIds.value(Long.parseLong(id));
                        }

                        predicates.add(criteriaBuilder.and(inLocationIds));
                    }

                    if (Objects.nonNull(finalLocationIdsInGroup)) {
                        if (finalLocationIdsInGroup.isEmpty()) {

                            // since the user passed in a location group without any location, we will add a
                            // wrong id into the query so it won't return anything
                            predicates.add(criteriaBuilder.equal(root.get("locationId"), -9999));
                        }
                        else {
                            CriteriaBuilder.In<Long> inLocationIds = criteriaBuilder.in(root.get("locationId"));
                            for(String id : finalLocationIdsInGroup) {
                                inLocationIds.value(Long.parseLong(id));
                            }

                            predicates.add(criteriaBuilder.and(inLocationIds));

                        }
                    }


                    if (Objects.nonNull(finalLocationIdsInPickZone)) {
                        if (finalLocationIdsInPickZone.isEmpty()) {

                            // since the user passed in a location pick zone without any location, we will add a
                            // wrong id into the query so it won't return anything
                            predicates.add(criteriaBuilder.equal(root.get("locationId"), -9999));
                        }
                        else {
                            CriteriaBuilder.In<Long> inLocationIds = criteriaBuilder.in(root.get("locationId"));
                            for(String id : finalLocationIdsInPickZone) {
                                inLocationIds.value(Long.parseLong(id));
                            }

                            predicates.add(criteriaBuilder.and(inLocationIds));

                        }
                    }


                    if (StringUtils.isNotBlank(receiptId)) {
                        predicates.add(criteriaBuilder.equal(root.get("receiptId"), receiptId));

                    }
                    if (StringUtils.isNotBlank(receiptIds)) {
                        CriteriaBuilder.In<Long> inReceiptIds = criteriaBuilder.in(root.get("receiptId"));
                        for(String id : receiptIds.split(",")) {
                            inReceiptIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inReceiptIds));
                    }

                    if (Strings.isNotBlank(receiptNumber)) {
                        Receipt receipt = inboundServiceRestemplateClient.getReceiptByNumber(
                                warehouseId,
                                receiptNumber
                        );
                        if (Objects.nonNull(receipt)) {

                            predicates.add(criteriaBuilder.equal(root.get("receiptId"), receipt.getId()));
                        }
                        else {

                            // since there's no receipt match with the receipt, we will use -1 as the receipt id
                            // to match with the inventory and we will assume there won't be anything return
                            predicates.add(criteriaBuilder.equal(root.get("receiptId"), -1));
                        }
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
                                lpn, lpn.contains("*"));
                        if (lpn.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("lpn"), lpn.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("lpn"), lpn));
                        }

                    }

                    if (StringUtils.isNotBlank(color)) {
                        if (color.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("color"), color.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("color"), color));
                        }
                    }
                    if (StringUtils.isNotBlank(productSize)) {
                        if (productSize.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("productSize"), productSize.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("productSize"), productSize));
                        }
                    }
                    if (StringUtils.isNotBlank(style)) {
                        if (style.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("style"), style.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("style"), style));
                        }
                    }

                    if (StringUtils.isNotBlank(attribute1)) {
                        if (attribute1.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("attribute1"), attribute1.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("attribute1"), attribute1));
                        }
                    }
                    if (StringUtils.isNotBlank(attribute2)) {
                        if (attribute2.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("attribute2"), attribute2.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("attribute2"), attribute2));
                        }
                    }
                    if (StringUtils.isNotBlank(attribute3)) {
                        if (attribute3.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("attribute3"), attribute3.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("attribute3"), attribute3));
                        }
                    }
                    if (StringUtils.isNotBlank(attribute4)) {
                        if (attribute4.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("attribute4"), attribute4.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("attribute4"), attribute4));
                        }
                    }
                    if (StringUtils.isNotBlank(attribute5)) {
                        if (attribute5.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("attribute5"), attribute5.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("attribute5"), attribute5));
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

                    // special handling for 3pl
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));

                    // return addClientRestriction(predicate, clientRestriction,
                    //        root, criteriaBuilder);
                    return Objects.isNull(clientRestriction) ?
                            predicate :
                            clientRestriction.addClientRestriction(predicate,
                                root, criteriaBuilder);


                }
                ,
                pageable
        );

    }

    private Predicate addClientRestriction(Predicate predicate,
                                           ClientRestriction clientRestriction,
                                           Root<Inventory> root,
                                           CriteriaBuilder criteriaBuilder) {
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
                null);
    }

    public List<Inventory> findPickableInventories(Long itemId, Long inventoryStatusId, int lpnLimit) {
        return findPickableInventories(itemId, inventoryStatusId, lpnLimit, true);
    }
    public List<Inventory> findPickableInventories(Long itemId,
                                                   Long inventoryStatusId, int lpnLimit,
                                                   boolean includeDetails) {
        return findPickableInventories(itemId, inventoryStatusId, null, null,
                null, null, null, null,null,null,null,null,
                null, null, lpnLimit, includeDetails);
    }
    public List<Inventory> findPickableInventories(Long itemId,
                                                   Long inventoryStatusId,
                                                   Long locationId,
                                                   String lpn,
                                                   String color,
                                                   String productSize,
                                                   String style,
                                                   String attribute1,
                                                   String attribute2,
                                                   String attribute3,
                                                   String attribute4,
                                                   String attribute5,
                                                   String receiptNumber,
                                                   String skipLocationIds,
                                                   int lpnLimit) {
        return findPickableInventories(itemId, inventoryStatusId, locationId, lpn,
                color, productSize, style,
                attribute1, attribute2, attribute3, attribute4, attribute5,
                receiptNumber,  skipLocationIds, lpnLimit, true);
    }
    public List<Inventory> findPickableInventories(Long itemId,
                                                   Long inventoryStatusId,
                                                   Long locationId,
                                                   String lpn,
                                                   String color,
                                                   String productSize,
                                                   String style,
                                                   String attribute1,
                                                   String attribute2,
                                                   String attribute3,
                                                   String attribute4,
                                                   String attribute5,
                                                   String receiptNumber,
                                                   String skipLocationIds,
                                                   int lpnLimit,
                                                   boolean includeDetails) {
        /**
        List<Inventory> availableInventories =
                Objects.isNull(locationId) ?
                        inventoryRepository.findPickableInventoryByItemIdAndInventoryStatusId(itemId, inventoryStatusId,
                                PageRequest.of(0, lpnLimit))
                        :
                        inventoryRepository.findPickableInventoryByItemIdAndInventoryStatusIdAndLocationId(itemId, inventoryStatusId, locationId,
                                PageRequest.of(0, lpnLimit));
**/

        List<Inventory> availableInventories =
                inventoryRepository.findPickableInventoryByItemIdAndInventoryStatusId(itemId, inventoryStatusId,
                                locationId, lpn,
                                PageRequest.of(0, lpnLimit));

        logger.debug("We have found {} available inventory, Let's get all the pickable inventory from it",
                availableInventories.size());

        Set<Long> skipLocationIdSet = new HashSet<>();
        if (Strings.isNotBlank(skipLocationIds)) {
            for (String skipLocationId : skipLocationIds.split(",")) {
                skipLocationIdSet.add(Long.parseLong(skipLocationId));
            }
        }
        logger.debug("We will skip the locations : {}", skipLocationIdSet);

        Set<Long> finalSkipLocationIdSet = skipLocationIdSet;

        List<Inventory>  pickableInventories
                =  availableInventories
                        .stream()
                .filter(inventory -> {
                    if (finalSkipLocationIdSet.isEmpty()) {
                        logger.debug("No need to skip the inventory {} as there's no requirement on the skip location",
                                inventory.getLpn());
                        return true;
                    }
                    else if (finalSkipLocationIdSet.contains(inventory.getLocationId())) {
                        logger.debug("we will need to skip inventory {} as its in a location {} that is marked as skip ({})",
                                inventory.getLpn(),
                                inventory.getLocationId(),
                                finalSkipLocationIdSet);
                        return false;
                    }
                    logger.debug("we don't need to skip inventory {} as its in a location {} that NOT is marked as skip ({})",
                            inventory.getLpn(),
                            inventory.getLocationId(),
                            finalSkipLocationIdSet);
                    return true;
                })
                .filter(this::isInventoryPickable)
                .map(inventory -> {
                    // setup the location so we can filter the inventory by pickable location only
                    if (Objects.nonNull(inventory.getLocationId()) &&
                            Objects.isNull(inventory.getLocation())) {
                        inventory.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(inventory.getLocationId()));
                    }
                    return inventory;
                }).filter(this::isLocationPickable)
                .filter(inventory -> inventoryAttributeMatch(inventory, color, productSize, style,
                        attribute1, attribute2, attribute3, attribute4, attribute5, receiptNumber))
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

    private boolean inventoryAttributeMatch(Inventory inventory, String color, String productSize,
                                            String style,
                                            String attribute1, String attribute2, String attribute3,
                                            String attribute4, String attribute5 , String receiptNumber) {
        logger.debug("start to check if the inventory {} / {} matches with the criteria",
                inventory.getId(), inventory.getLpn());
        logger.debug("color: {}, productSize: {}, style: {}",
                Strings.isBlank(color) ? "N/A" : color,
                Strings.isBlank(productSize) ? "N/A" : productSize,
                Strings.isBlank(style) ? "N/A" : style);
        logger.debug("inventory attribute 1 ~ 5: {}, {}, {}, {}, {}",
                Strings.isBlank(attribute1) ? "N/A" : attribute1,
                Strings.isBlank(attribute2) ? "N/A" : attribute2,
                Strings.isBlank(attribute3) ? "N/A" : attribute3,
                Strings.isBlank(attribute4) ? "N/A" : attribute4,
                Strings.isBlank(attribute5) ? "N/A" : attribute5);
        logger.debug("receiptNumber: {}",
                Strings.isBlank(receiptNumber) ? "N/A" : receiptNumber);

        if (Strings.isNotBlank(color) && !color.equalsIgnoreCase(inventory.getColor())) {
            logger.debug("inventory's color {} doesn't match with the criteria color = {}",
                    Strings.isBlank(inventory.getColor()) ? "N/A" : inventory.getColor(),
                    color);
            return false;
        }
        if (Strings.isNotBlank(productSize) && !productSize.equalsIgnoreCase(inventory.getProductSize())) {
            logger.debug("inventory's productSize {} doesn't match with the criteria productSize = {}",
                    Strings.isBlank(inventory.getProductSize()) ? "N/A" : inventory.getProductSize(),
                    productSize);
            return false;
        }
        if (Strings.isNotBlank(style) && !style.equalsIgnoreCase(inventory.getStyle())) {
            logger.debug("inventory's style {} doesn't match with the criteria style = {}",
                    Strings.isBlank(inventory.getStyle()) ? "N/A" : inventory.getStyle(),
                    style);
            return false;
        }
        if (Strings.isNotBlank(attribute1) && !attribute1.equalsIgnoreCase(inventory.getAttribute1())) {
            logger.debug("inventory's attribute1 {} doesn't match with the criteria attribute1 = {}",
                    Strings.isBlank(inventory.getAttribute1()) ? "N/A" : inventory.getAttribute1(),
                    attribute1);
            return false;
        }
        if (Strings.isNotBlank(attribute2) && !attribute2.equalsIgnoreCase(inventory.getAttribute2())) {
            logger.debug("inventory's attribute2 {} doesn't match with the criteria attribute2 = {}",
                    Strings.isBlank(inventory.getAttribute2()) ? "N/A" : inventory.getAttribute2(),
                    attribute2);
            return false;
        }
        if (Strings.isNotBlank(attribute3) && !attribute3.equalsIgnoreCase(inventory.getAttribute3())) {
            logger.debug("inventory's attribute3 {} doesn't match with the criteria attribute3 = {}",
                    Strings.isBlank(inventory.getAttribute3()) ? "N/A" : inventory.getAttribute3(),
                    attribute3);
            return false;
        }
        if (Strings.isNotBlank(attribute4) && !attribute4.equalsIgnoreCase(inventory.getAttribute4())) {
            logger.debug("inventory's attribute4 {} doesn't match with the criteria attribute4 = {}",
                    Strings.isBlank(inventory.getAttribute4()) ? "N/A" : inventory.getAttribute4(),
                    attribute4);
            return false;
        }
        if (Strings.isNotBlank(attribute5) && !attribute5.equalsIgnoreCase(inventory.getAttribute5())) {
            logger.debug("inventory's attribute5 {} doesn't match with the criteria attribute5 = {}",
                    Strings.isBlank(inventory.getAttribute5()) ? "N/A" : inventory.getAttribute5(),
                    attribute5);
            return false;
        }
        if (Strings.isNotBlank(receiptNumber)) {
            if (Objects.nonNull(inventory.getReceiptId()) && Objects.isNull(inventory.getReceipt())) {
                inventory.setReceipt(
                        inboundServiceRestemplateClient.getReceiptById(
                                inventory.getReceiptId()
                        )
                );
            }
            if(Objects.isNull(inventory.getReceipt())) {
                return false;
            }
            else if (!inventory.getReceipt().getNumber().equalsIgnoreCase(receiptNumber)) {
                return false;
            }
        }
        logger.debug("==== inventory {} / {} matches with the criteria  ====",
                inventory.getId(), inventory.getLpn());
        return true;
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

    public List<Inventory> findByLocationName(Long warehouseId, String locationName, boolean includeDetails) {
        return findAll(warehouseId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                locationName,
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
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null, null, includeDetails,
                null);
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
                includeDetails, null

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
        if (Objects.isNull(inventory.getInWarehouseDatetime())) {
            if (Objects.nonNull(inventory.getCreatedTime())) {
                inventory.setInWarehouseDatetime(inventory.getCreatedTime());
            }
            else if (Objects.nonNull(inventory.getLastModifiedTime())) {
                inventory.setInWarehouseDatetime(inventory.getLastModifiedTime());
            }
            else {
                inventory.setInWarehouseDatetime(ZonedDateTime.now(ZoneId.of("UTC")));
            }
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
             logger.debug("start to get pick by {} from LPN {} / {}",
                     inventory.getPickId(),
                     inventory.getId(),
                     inventory.getLpn());
            inventory.setPick(outbuondServiceRestemplateClient.getPickById(inventory.getPickId()));
         }

         logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the pick for LPN {}",
         ChronoUnit.MILLIS.between(
                 currentLocalDateTime, LocalDateTime.now()),
                 LocalDateTime.now(),
                 inventory.getLpn());
         currentLocalDateTime = LocalDateTime.now();

         if (inventory.getAllocatedByPickId() != null) {
             logger.debug("start to get allocated pick by {} from LPN {} / {}",
                     inventory.getPickId(),
                     inventory.getId(),
                     inventory.getLpn());
             try {

                 inventory.setAllocatedByPick(outbuondServiceRestemplateClient.getPickById(inventory.getAllocatedByPickId()));
             }
             catch (Exception ex) {
                 // ignore the error
                 ex.printStackTrace();
             }
         }
         logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the allocated by pick for LPN {}",
         ChronoUnit.MILLIS.between(
                 currentLocalDateTime, LocalDateTime.now()),
                 LocalDateTime.now(),
                 inventory.getLpn());
         currentLocalDateTime = LocalDateTime.now();

         if (Objects.nonNull(inventory.getWorkOrderId()) &&
                Objects.isNull(inventory.getWorkOrder())) {
             try {

                 inventory.setWorkOrder(workOrderServiceRestemplateClient.getWorkOrderById(
                         inventory.getWorkOrderId(), false, false));
             }
             catch (Exception ex) {
                 ex.printStackTrace();
                 // ignore the error
             }

         }
         logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the work order for LPN {}",
         ChronoUnit.MILLIS.between(
                 currentLocalDateTime, LocalDateTime.now()),
                 LocalDateTime.now(),
                 inventory.getLpn());

        if (Objects.nonNull(inventory.getReceiptId()) &&
                Objects.isNull(inventory.getReceipt())) {
            try {

                inventory.setReceipt(inboundServiceRestemplateClient.getReceiptById(inventory.getReceiptId()));
            }
            catch (Exception ex) {
                // ignore the error
                ex.printStackTrace();
            }

        }
        if (Objects.nonNull(inventory.getReceiptLineId()) &&
                Objects.isNull(inventory.getReceiptLine()) &&
                Objects.nonNull(inventory.getReceipt())) {

            try {

                inventory.setReceiptLine(
                        inventory.getReceipt().getReceiptLines().stream().filter(
                                receiptLine -> inventory.getReceiptLineId().equals(receiptLine.getId())
                        ).findFirst().orElse(null)
                );

            }
            catch (Exception ex) {
                ex.printStackTrace();
                // ignore the error
            }
        }

        logger.debug("====> after : {} millisecond(1/1000 second) @ {},we loaded the work order for LPN {}",
                ChronoUnit.MILLIS.between(
                        currentLocalDateTime, LocalDateTime.now()),
                LocalDateTime.now(),
                inventory.getLpn());




     }

    public List<InventoryCSVWrapper> loadInventoryData(File file) throws IOException {

        return fileService.loadData(file, InventoryCSVWrapper.class);
    }

    private Inventory convertFromWrapper(Long warehouseId,
                                         InventoryCSVWrapper inventoryCSVWrapper) {
        Inventory inventory = new Inventory();

        if (Strings.isBlank(inventoryCSVWrapper.getLpn())) {
            inventory.setLpn(commonServiceRestemplateClient.getNextLpn(warehouseId));
            logger.debug("LPN is not passed in, use the system generated value: {}",
                    inventory.getLpn());
        }
        else {

            inventory.setLpn(inventoryCSVWrapper.getLpn());
        }

        inventory.setVirtual(false);

        inventory.setColor(inventoryCSVWrapper.getColor());
        inventory.setProductSize(inventoryCSVWrapper.getProductSize());
        inventory.setStyle(inventoryCSVWrapper.getStyle());

        inventory.setAttribute1(inventoryCSVWrapper.getAttribute1());
        inventory.setAttribute2(inventoryCSVWrapper.getAttribute2());
        inventory.setAttribute3(inventoryCSVWrapper.getAttribute3());
        inventory.setAttribute4(inventoryCSVWrapper.getAttribute4());
        inventory.setAttribute5(inventoryCSVWrapper.getAttribute5());

        inventory.setWarehouseId(warehouseId);

        if(Strings.isNotBlank(inventoryCSVWrapper.getFifoDate())) {

            inventory.setFifoDate(dateTimeService.getZonedDateTime(inventoryCSVWrapper.getFifoDate()));
        }

        if(Strings.isNotBlank(inventoryCSVWrapper.getInWarehouseDatetime())) {

            inventory.setInWarehouseDatetime(dateTimeService.getZonedDateTime(inventoryCSVWrapper.getInWarehouseDatetime()));

            // set the FIFO date as the in warehouse date, if the fifo date is not specified
            if (Objects.isNull(inventory.getFifoDate())) {
                inventory.setFifoDate(inventory.getInWarehouseDatetime());
            }
        }


        // client
        if (Strings.isNotBlank(inventoryCSVWrapper.getClient())) {
            Client client = commonServiceRestemplateClient.getClientByName(warehouseId,
                    inventoryCSVWrapper.getClient());
            if (Objects.nonNull(client)) {
                logger.debug("client is setup to {}", client.getName());
                inventory.setClientId(client.getId());
            }
            else {
                logger.debug("fail to get client by name {}", inventoryCSVWrapper.getClient());
            }
        }

        // item
        if (Strings.isNotBlank(inventoryCSVWrapper.getItem())) {
            logger.debug("start to get item from inventoryCSVWrapper.getItem(): {}, warehouseId: {}, client id: {}",
                    inventoryCSVWrapper.getItem(),
                    warehouseId,
                    inventory.getClientId());
            inventory.setItem(itemService.findByName(warehouseId, inventory.getClientId(),
                    inventoryCSVWrapper.getItem()));
        }


        // itemPackageType
        if (Strings.isNotBlank(inventoryCSVWrapper.getItemPackageType()) &&
                Strings.isNotBlank(inventoryCSVWrapper.getItem())) {
            inventory.setItemPackageType(
                    itemPackageTypeService.findByNaturalKeys(
                            warehouseId,
                            Objects.isNull(inventory.getClientId()) ? null : inventory.getClientId(),
                            inventoryCSVWrapper.getItem(),
                            inventoryCSVWrapper.getItemPackageType()));
        }
        else {
            // set the inventory's item package type to the item's default item package type
            logger.debug("set the inventory's item package type to default item package type");
            inventory.setItemPackageType(
                    inventory.getItem().getDefaultItemPackageType()
            );
        }


        int unitOfMeasureQuantity = 1;
        if (Strings.isNotBlank(inventoryCSVWrapper.getUnitOfMeasure())) {
            logger.debug("get unit of measure quantity from inventory, item = {}, package type = {}, " +
                    "unit of measure = {}",
                    inventory.getItem().getName(),
                    inventory.getItemPackageType(),
                    inventoryCSVWrapper.getUnitOfMeasure());
            logger.debug("The item package has {} existing unit of measures: {}",
                    inventory.getItemPackageType().getItemUnitOfMeasures().size(),
                    inventory.getItemPackageType().getItemUnitOfMeasures().stream().map(
                            itemUnitOfMeasure ->
                                    Objects.nonNull(itemUnitOfMeasure.getUnitOfMeasure()) ?
                                            itemUnitOfMeasure.getUnitOfMeasure().getName() :
                                            itemUnitOfMeasure.getUnitOfMeasureId()
                    ).collect(Collectors.toList()));

            unitOfMeasureQuantity = inventory.getItemPackageType().getItemUnitOfMeasures()
                    .stream().filter(itemUnitOfMeasure ->
                            {
                                if (Objects.nonNull(itemUnitOfMeasure.getUnitOfMeasure())) {
                                    return itemUnitOfMeasure.getUnitOfMeasure().getName().equalsIgnoreCase(
                                                    inventoryCSVWrapper.getUnitOfMeasure()
                                            );
                                }
                                else {
                                    return commonServiceRestemplateClient.getUnitOfMeasureById(
                                            itemUnitOfMeasure.getUnitOfMeasureId()
                                    ).getName().equalsIgnoreCase(
                                            inventoryCSVWrapper.getUnitOfMeasure()
                                    );
                                }
                            })
                    .map(itemUnitOfMeasure -> itemUnitOfMeasure.getQuantity())
                    .findFirst().orElse(1);
        }
        inventory.setQuantity(inventoryCSVWrapper.getQuantity() * unitOfMeasureQuantity);


        // inventoryStatus
        if (Strings.isNotBlank(inventoryCSVWrapper.getInventoryStatus())) {
            logger.debug("will set inventory status: {} / {}",
                    warehouseId,
                    inventoryCSVWrapper.getInventoryStatus());
            inventory.setInventoryStatus(inventoryStatusService.findByName(
                    warehouseId, inventoryCSVWrapper.getInventoryStatus()));
        }
        else {
            // inventory status is not passed in, let's get the default available inventory
            inventory.setInventoryStatus(
                    inventoryStatusService.getAvailableInventoryStatus(warehouseId).get());

        }

        if (Strings.isNotBlank(inventoryCSVWrapper.getLocation())) {
            logger.debug("start to get location by name: {}", inventoryCSVWrapper.getLocation());
            Location location =
                    warehouseLayoutServiceRestemplateClient.getLocationByName(
                            warehouseId, inventoryCSVWrapper.getLocation());
            inventory.setLocationId(location.getId());
            inventory.setLocation(location);
        }

        return inventory;

    }


    public Inventory reverseProduction(Long id, String documentNumber, String comment) {
        Inventory inventory = findById(id);
        logger.debug("Start to reverse production of inventory with lpn {}",
                inventory.getLpn());
        return removeInventory(inventory, InventoryQuantityChangeType.REVERSE_PRODUCTION, documentNumber, comment, null);
    }

    public Inventory reverseByProduct(Long id, String documentNumber, String comment) {
        Inventory inventory = findById(id);
        logger.debug("Start to reverse by product of inventory with lpn {}",
                inventory.getLpn());
        return removeInventory(inventory, InventoryQuantityChangeType.REVERSE_BY_PRODUCT, documentNumber, comment, null);
    }

    public Inventory reverseReceiving(Long id, String documentNumber, String comment) {
        Inventory inventory = findById(id);
        return removeInventory(inventory, InventoryQuantityChangeType.REVERSE_RECEIVING, documentNumber, comment, null);
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
        return removeInventory(inventory, InventoryQuantityChangeType.INVENTORY_ADJUST, documentNumber, comment, null);
    }

    public Inventory removeInventory(Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType) {
        return removeInventory(inventory, inventoryQuantityChangeType, "", "", null);

    }
    public Inventory removeInventory(Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType,
                                            String documentNumber, String comment, Long reasonCodeId) {

        logger.debug("start to remove inventory {} , lpn {}",
                inventory.getId(),
                inventory.getLpn());
        if (isApprovalNeededForInventoryAdjust(inventory, 0L, inventoryQuantityChangeType)) {

            logger.debug("We will need to get approval, so here we just save the request");
            writeInventoryAdjustRequest(inventory, 0L,
                    inventoryQuantityChangeType, documentNumber, comment, reasonCodeId, false);
            return inventory;
        } else {
            logger.debug("No approval needed, let's just go ahread with the adding inventory!");
            return processRemoveInventory(inventory, inventoryQuantityChangeType, documentNumber, comment, reasonCodeId);
        }
    }
    public Inventory processRemoveInventory(Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType,
                                     String documentNumber, String comment, Long reasonCodeId) {

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
            case REVERSE_RECEIVING:
                inventoryActivityType = InventoryActivityType.REVERSE_RECEIVING;
                break;
            default:
                inventoryActivityType = InventoryActivityType.INVENTORY_ADJUSTMENT;
        }
        inventoryActivityService.logInventoryActivitiy(inventory, inventoryActivityType,
                "quantity", String.valueOf(inventory.getQuantity()), "0",
                documentNumber, comment, reasonCodeId);

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
            inventory = relabelLPN(inventory, newLPN, false);
        }
        return inventory;
    }

    public List<Inventory> relabelLPN(Long warehouseId, String lpn,
                                      String newLPN, Boolean mergeWithExistingInventory) {


        // make sure the LPN exists
        List<Inventory> inventories = findByLpn(warehouseId, lpn);
        if (inventories.isEmpty()) {
            throw InventoryException.raiseException("Can't find inventory with LPN " + lpn +
                    ", relabel fail");
        }

        return inventories.stream().map(inventory -> relabelLPN(inventory, newLPN, mergeWithExistingInventory)).collect(Collectors.toList());
    }
    public Inventory relabelLPN(Long id, String newLPN, Boolean mergeWithExistingInventory) {

        return relabelLPN(findById(id), newLPN, mergeWithExistingInventory);
    }
    public Inventory relabelLPN(Inventory inventory, String newLPN, Boolean mergeWithExistingInventory) {

        // make sure the new LPN is a valid LPN if
        // we are not merge the inventory into existing inventory
        if (!Boolean.TRUE.equals(mergeWithExistingInventory)) {

            logger.debug("start to validate the new LPN is we are not allowed to merge with existing inventory");
            validateLPN(inventory.getWarehouseId(), newLPN, true);
        }

        inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.RELABEL_LPN,
                "LPN", String.valueOf(inventory.getLpn()), newLPN,
                "", "", null);
        inventory.setLpn(newLPN);

        return saveOrUpdate(inventory);
    }

    public Inventory moveInventory(Inventory inventory, Location destination) {
        if (!inventoryMixRestrictionService.checkMovementAllowed(inventory, destination)) {
            throw InventoryException.raiseException("Inventory " + inventory.getLpn() + " is not allowed " +
                    " to move into location " + destination.getName() + " due to the movement restriction");
        }
        return moveInventory(inventory, destination, null, true, null);
    }

    public Inventory moveInventory(Long inventoryId, Location destination, Long pickId, boolean immediateMove,
                                   String destinationLpn) {
        return moveInventory(findById(inventoryId), destination, pickId, immediateMove,
                destinationLpn);

    }

    /**
     * Move invenory by specify the LPN, item name and quantity. this is the method mainly to support partial
     * movement
     * @param warehouseId
     * @param lpn
     * @param itemName
     * @param quantity
     * @param unitOfMeasureName
     * @param destination
     * @param pickId
     * @param immediateMove
     * @param destinationLpn
     * @return
     */
    public List<Inventory> moveInventory(Long warehouseId,
                                         Long clientId,
                                         String lpn,
                                         String itemName,
                                         Long quantity,
                                         String unitOfMeasureName,
                                         Location destination,
                                         Long pickId,
                                         boolean immediateMove,
                                         String destinationLpn) {
        // let's get the inventory by LPN , item name, quantity and unti of measure
        // then move one by one
        List<Inventory> inventories = findByLpn(warehouseId, lpn);

        if (Strings.isBlank(itemName) || Objects.isNull(quantity)) {
            // if item or quantity is not specified, we will move the whole LPN
            return inventories.stream().map(inventory -> moveInventory(
                    inventory, destination, pickId, immediateMove,
                    destinationLpn
            )).collect(Collectors.toList());

        }


        inventories = inventories.stream().filter(
                inventory -> {

                    logger.debug("start to check if inventory {} meet the required client: {}, item name {}, unit of measure name: {}",
                            clientId, itemName, unitOfMeasureName);
                    logger.debug("client id match?: {}", Objects.isNull(clientId) ||  Objects.equals(clientId, inventory.getClientId()));
                    logger.debug("item name match?: {}", inventory.getItem().getName().equalsIgnoreCase(itemName) );
                    logger.debug("item unit of measure match?: {}",
                            ( Strings.isBlank(unitOfMeasureName) ||
                                    inventory.getItemPackageType().getItemUnitOfMeasures().stream().anyMatch(
                                            itemUnitOfMeasure -> itemUnitOfMeasure.getUnitOfMeasure().getName().equalsIgnoreCase(unitOfMeasureName)
                                    ) ) );
                    // if client id is passed in, make sure the required client id matches with the inventory's client id
                    return (Objects.isNull(clientId) ||  Objects.equals(clientId, inventory.getClientId()))
                            &&
                            inventory.getItem().getName().equalsIgnoreCase(itemName)
                            &&
                            ( Strings.isBlank(unitOfMeasureName) ||
                                    inventory.getItemPackageType().getItemUnitOfMeasures().stream().anyMatch(
                                            itemUnitOfMeasure -> itemUnitOfMeasure.getUnitOfMeasure().getName().equalsIgnoreCase(unitOfMeasureName)
                                    )
                            );
                }
        ).collect(Collectors.toList());
        // ok, then we will only return the inventory's up to the specific quantity
        // since we are sure the item name is specified, let's get the item and
        // all the unit of measure so that we know the actual we will need to move
        // based on the passed in quantity and unit of measure
        if (inventories.isEmpty()) {
            throw InventoryException.raiseException("Can't move inventory as there's no inventory match " +
                    "LPN: " + lpn + ", item : " + itemName);
        }
        long quantityToBeMoved = 0l;
        if (Strings.isBlank(unitOfMeasureName)) {
            quantityToBeMoved = quantity;
        }
        else {
            long unitOfMeasureQuantity = inventories.get(0).getItemPackageType().getItemUnitOfMeasures().stream()
                    .filter(itemUnitOfMeasure -> itemUnitOfMeasure.getUnitOfMeasure().getName().equalsIgnoreCase(unitOfMeasureName))
                    .map(itemUnitOfMeasure -> itemUnitOfMeasure.getQuantity()).findFirst().orElse(1);
            quantityToBeMoved = unitOfMeasureQuantity * quantity;
        }

        logger.debug("start to move {} of item {} from LPN {}, based on the request {} {}",
                quantityToBeMoved, itemName, lpn, quantity, unitOfMeasureName);

        List<Inventory> fullyMovedInventory = new ArrayList<>();
        // we should only have one partial moved inventory if possible, which is the last one
        Inventory partiallyMovedInventory = null;
        long partiallyMovedQuanttiy = 0;
        for (Inventory inventory : inventories) {
            if (inventory.getQuantity() > quantityToBeMoved) {
                // current inventory is enough for the moved quantity, let's marked it as
                // partially moved and end the loop
                partiallyMovedInventory = inventory;
                partiallyMovedQuanttiy = quantityToBeMoved;
                break;
            }
            else if (inventory.getQuantity() == quantityToBeMoved) {
                // currnet invenotry is just enough for the quantity to be moved, let's add
                // the inventory as fully moved inventory and stop here
                fullyMovedInventory.add(inventory);
                break;
            }
            else {
                // current invenotry is not enough for the quantity to be mvoed, let's
                // add the inventory as need to be fully moved and continue
                fullyMovedInventory.add(inventory);
                quantityToBeMoved -= inventory.getQuantity();
            }
        }
        List<Inventory> movedInventory = fullyMovedInventory.stream().map(
                inventory -> moveInventory(
                        inventory, destination, pickId, immediateMove,
                        destinationLpn
                )).collect(Collectors.toList());

        // see if we will need to partially move the inventory
        if (Objects.nonNull(partiallyMovedInventory) && partiallyMovedQuanttiy > 0) {
            // ok, we may need to split the right quantity from the invenotry and then
            // move the new LPN
            // for split inventory, the return list will contain 2 inventories, the first one
            // is the original inventory with remaining quantity and the second one is the
            // the split inventory with split quantity
            List<Inventory> splitInventories = splitInventory(partiallyMovedInventory, "", partiallyMovedQuanttiy);
            movedInventory.add(
                    moveInventory(
                            splitInventories.get(1), destination, pickId, immediateMove,
                            destinationLpn
                    )
            );
        }

        return movedInventory;

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
        logger.debug("start to move LPN {} to location {}, immediate move? {}",
                inventory.getLpn(), Objects.nonNull(destination) ? destination.getName() : "N/A",
                immediateMove);
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
           logger.debug("destination's group is not passed in, let's load the group and group type for it");
           destination = warehouseLayoutServiceRestemplateClient.getLocationById(destination.getId());


       }

       if (destination.getLocationGroup().getLocationGroupType().getVirtual()) {
            // The inventory is moved to the virtual location, let's mark the inventory
            // as virtual
            inventory.setVirtual(true);
            logger.debug("inventory is set to virtual as it is moved into a virtual location");
       }
       else {

            inventory.setVirtual(false);
            logger.debug("inventory is set to NON virtual as it is moved into a NON virtual location");
       }
        // Check if we have finished any movement

        if (Objects.isNull(pickId)) {
            recalculateMovementPathForInventoryMovement(inventory, destination);
            logger.debug("recalculated the movement path for the inventory ");
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

        logger.debug("inventory activity logged for the movement");
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
                if (Objects.nonNull(workOrderMaterialConsumeTiming) &&
                        workOrderMaterialConsumeTiming.equals(
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
        logger.debug("start to recalculate the location's volume after we move new inventory into this destination location {}",
                destination.getName());
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
            // commonServiceRestemplateClient.removeWorkTask(inventory, WorkType.INVENTORY_MOVEMENT);
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

        Pick pick = outbuondServiceRestemplateClient.getPickById(pickId);
        return markAsPicked(inventory, destination, pick);
    }
    public Inventory markAsPicked(Inventory inventory, Location destination,  Pick pick) {
        if (Objects.nonNull(inventory.getAllocatedByPickId())) {
            // The inventory is allocated by certain pick. make sure it is
            // not picked by a different pick
            if (!pick.getId().equals(inventory.getAllocatedByPickId())) {
                throw InventoryException.raiseException("Inventory is allocated by other picks. Can't pick from it");
            }

            // after the inventory is picked, reset the allocated by pick id to null
            // so in case it is cancelled / unpicked, the inventory is still available
            // for other picks
            inventory.setAllocatedByPickId(null);

        }
        inventory.setPickId(pick.getId());
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
                "pick", String.valueOf(pick.getId()), "", pick.getNumber());
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
        List<Pick> newPicks =
                outbuondServiceRestemplateClient.unpick(inventory.getPickId(), inventory.getQuantity());

        // disconnect the inventory from the pick and
        // clear all the movement path
        inventory.setPickId(null);
        inventoryMovementService.clearInventoryMovement(inventory);
        inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.UNPICKING,
                "quantity", String.valueOf(inventory.getQuantity()), "",
                String.valueOf(inventory.getPickId()));


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
        // commonServiceRestemplateClient.addWorkTask(workTask);
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

    public Inventory addInventory(Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType, Boolean kitInventoryUseDefaultAttribute) {
        return addInventory(inventory, inventoryQuantityChangeType, "", "", kitInventoryUseDefaultAttribute);

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
                                  String documentNumber, String comment, Long reasonCodeId, Boolean kitInventoryUseDefaultAttribute) {
        return addInventory(userService.getCurrentUserName(), inventory, inventoryQuantityChangeType,
                documentNumber, comment, reasonCodeId, kitInventoryUseDefaultAttribute);
    }
    @Transactional
    public Inventory addInventory(Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType,
                                  String documentNumber, String comment, Boolean kitInventoryUseDefaultAttribute) {
        return addInventory(inventory, inventoryQuantityChangeType, documentNumber, comment, null, null);
    }
    @Transactional
    public Inventory addInventory(String username, Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType,
                                  String documentNumber, String comment,  Long reasonCodeId, Boolean kitInventoryUseDefaultAttribute) {

        logger.debug("Start to add inventory with LPN {}",
                Strings.isBlank(inventory.getLpn()) ? "N/A" : inventory.getLpn());
        // if inventory's LPN is not setup, get next LPN for it
        // setup the inventory's client id necessary
        if (Objects.nonNull(inventory.getItem().getClientId()) &&
                Objects.isNull(inventory.getClientId())) {
            inventory.setClientId(inventory.getItem().getClientId());
        }
        if (Strings.isBlank(inventory.getLpn())) {

            inventory.setLpn(
                    commonServiceRestemplateClient.getNextLpn(inventory.getWarehouseId())
            );
        }
        if (isApprovalNeededForInventoryAdjust(inventory, 0L, inventory.getQuantity(), inventoryQuantityChangeType)) {

            logger.debug("We will need to get approval, so here we just save the request");
            writeInventoryAdjustRequest(inventory, 0L,  inventory.getQuantity(),
                    inventoryQuantityChangeType, documentNumber, comment, reasonCodeId, kitInventoryUseDefaultAttribute);
            inventory.setLockedForAdjust(true);
            return inventory;
        } else {
            logger.debug("No approval needed, let's just go ahread with the adding inventory!");
            return processAddInventory(username, inventory, inventoryQuantityChangeType, documentNumber,
                    comment, reasonCodeId, kitInventoryUseDefaultAttribute);
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
                                             String documentNumber, String comment, Long reasonCodeId) {

        writeInventoryAdjustRequest(inventory, inventory.getQuantity(),  newQuantity,
                inventoryQuantityChangeType,
                documentNumber, comment, reasonCodeId, true);
    }

    private void writeInventoryAdjustRequest(Inventory inventory, Long newQuantity,
                                             InventoryQuantityChangeType inventoryQuantityChangeType,
                                             String documentNumber, String comment, Long reasonCodeId, Boolean kitInventoryUseDefaultAttribute) {


        writeInventoryAdjustRequest(inventory, inventory.getQuantity(),  newQuantity,
                inventoryQuantityChangeType,
                documentNumber, comment, reasonCodeId, kitInventoryUseDefaultAttribute);
    }
    private void writeInventoryAdjustRequest(Inventory inventory, Long oldQuantity, Long newQuantity,
                                             InventoryQuantityChangeType inventoryQuantityChangeType,
                                             String documentNumber, String comment,
                                             Long reasonCodeId, Boolean kitInventoryUseDefaultAttribute) {

        // if we are manupulating an existing inventory, let's lock teh inventory first
        if (Objects.nonNull(inventory.getId())) {
            lockInventory(inventory.getId());
        }
        else {
            logger.debug("set lockedForAdjust to true for LPN {}", inventory.getLpn());
            inventory.setLockedForAdjust(true);
        }
        inventoryAdjustmentRequestService.writeInventoryAdjustRequest(inventory, oldQuantity, newQuantity, inventoryQuantityChangeType,
                  documentNumber,  comment, reasonCodeId,
                kitInventoryUseDefaultAttribute);

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
                                         String documentNumber, String comment, Long reasonCodeId, Boolean kitInventoryUseDefaultAttribute) {
        return processAddInventory(
                userService.getCurrentUserName(),
                inventory, inventoryQuantityChangeType,
                documentNumber, comment, reasonCodeId,  kitInventoryUseDefaultAttribute);
    }
    public Inventory processAddInventory(String username,
                                         Inventory inventory, InventoryQuantityChangeType inventoryQuantityChangeType,
                                         String documentNumber, String comment, Long reasonCodeId, Boolean kitInventoryUseDefaultAttribute) {
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
/**
        if (Boolean.TRUE.equals(inventory.getKitInventory())) {
            logger.debug("start to add kit inventory with {} inner inventory ",
                    inventory.getKitInnerInventories().size());
            for (Inventory kitInnerInventory : inventory.getKitInnerInventories()) {
                kitInnerInventory.setKitInventory(inventory);
            }
        }
 **/
        if (Boolean.TRUE.equals(inventory.getItem().getKitItemFlag())) {
            inventory.setKitInventoryFlag(true);
        }

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
                    documentNumber, comment, reasonCodeId);

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

        inventory =  moveInventory(inventory, destinationLocation);

        if (Boolean.TRUE.equals(inventory.getItem().getKitItemFlag())) {
            logger.debug("We just received a kit inventory {}, let's also create the inner" +
                    " inventory " , inventory.getLpn());
            List<Inventory> innerInventories = createKitInnerInventory(inventory, kitInventoryUseDefaultAttribute);
            for(Inventory innerInventory: innerInventories) {

                processAddInventory(username,
                        innerInventory, inventoryQuantityChangeType,
                        documentNumber, comment, reasonCodeId, kitInventoryUseDefaultAttribute);
            }
        }
        return inventory;


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

    private List<Inventory> createKitInnerInventory(Inventory inventory, Boolean kitInventoryUseDefaultAttribute) {
        List<Inventory> innerInventories = new ArrayList<>();

        Item kitItem = inventory.getItem();

        if (!Boolean.TRUE.equals(kitItem.getKitItemFlag()) ||
            Objects.isNull(kitItem.getBillOfMaterialId())) {
            logger.debug("The inventory {}'s item {} is not a proper kit item",
                    inventory.getLpn(), kitItem.getName());
            throw MissingInformationException.raiseException(
                    "The inventory " + inventory.getLpn() + "'s item "
                    + kitItem.getName() + " is not a proper kit item");
        }
        BillOfMaterial billOfMaterial = kitItem.getBillOfMaterial();
        if (Objects.isNull(billOfMaterial)) {
            // load the bill of material. We will get the inner inventory
            // item and quantity from the BOM
            billOfMaterial =
                    workOrderServiceRestemplateClient.getBillOfMaterialById(
                            kitItem.getBillOfMaterialId(),
                            false
                    );
            if (Objects.isNull(billOfMaterial)) {
                throw MissingInformationException.raiseException(
                        "The inventory " + inventory.getLpn() + "'s item "
                                + kitItem.getName() + " doesn't have a BOM setup, fail to " +
                                "create kit inventory ");
            }

        }

        // start to create inner items with the same inventory status
        for(BillOfMaterialLine billOfMaterialLine : billOfMaterial.getBillOfMaterialLines()) {
            Inventory innerInventory = createKitInnerInventory(
                    inventory,billOfMaterial, billOfMaterialLine, kitInventoryUseDefaultAttribute);
            innerInventory.setKitInventory(inventory);
            innerInventory = saveOrUpdate(innerInventory);
            innerInventories.add(innerInventory);
        }

        return innerInventories;

    }
    private Inventory createKitInnerInventory(Inventory inventory,
                                              BillOfMaterial billOfMaterial, BillOfMaterialLine billOfMaterialLine,
                                              Boolean kitInventoryUseDefaultAttribute) {
        Item innerItem = billOfMaterialLine.getItem();
        if (Objects.isNull(innerItem)) {
            innerItem = itemService.findById(billOfMaterialLine.getItemId(), false);
        }
        Long innerInventoryQuantity =(long) (inventory.getQuantity() * billOfMaterialLine.getExpectedQuantity() / billOfMaterial.getExpectedQuantity());


        Inventory innerInventory = inventory.copy(inventory.getLpn(), innerInventoryQuantity);
        innerInventory.setItem(innerItem);

        // setup the inventory attribute from the default attribute of the item
        // or inherit from the parent
        // inventory.copy will copy the inventory attribute from the parent as well
        // so we will only need to change the attribute only if we will need to
        // use the default inventory attribute that associated with the item
        if (Boolean.TRUE.equals(kitInventoryUseDefaultAttribute)) {
            setupDefaultInventoryAttribute(innerInventory);
        }

        innerInventory.setItemPackageType(innerItem.getDefaultItemPackageType());
        innerInventory.setKitInventoryFlag(innerItem.getKitItemFlag());
        innerInventory.setKitInnerInventoryFlag(true);

        return innerInventory;


    }

    private void setupDefaultInventoryAttribute(Inventory inventory) {
        Item item = inventory.getItem();
        if (item.isTrackingColorFlag()) {

            inventory.setColor(item.getDefaultColor());
        }
        else {
            inventory.setColor(null);
        }

        if (item.isTrackingStyleFlag()) {

            inventory.setStyle(item.getDefaultStyle());
        }
        else {
            inventory.setStyle(null);
        }

        if (item.isTrackingProductSizeFlag()) {

            inventory.setProductSize(item.getDefaultProductSize());
        }
        else {
            inventory.setProductSize(null);
        }

        if (item.isTrackingInventoryAttribute1Flag()) {

            inventory.setAttribute1(item.getDefaultInventoryAttribute1());
        }
        else {
            inventory.setAttribute1(null);
        }

        if (item.isTrackingInventoryAttribute2Flag()) {

            inventory.setAttribute2(item.getDefaultInventoryAttribute2());
        }
        else {
            inventory.setAttribute2(null);
        }
        if (item.isTrackingInventoryAttribute3Flag()) {

            inventory.setAttribute3(item.getDefaultInventoryAttribute3());
        }
        else {
            inventory.setAttribute3(null);
        }
        if (item.isTrackingInventoryAttribute4Flag()) {

            inventory.setAttribute4(item.getDefaultInventoryAttribute4());
        }
        else {
            inventory.setAttribute4(null);
        }
        if (item.isTrackingInventoryAttribute5Flag()) {

            inventory.setAttribute5(item.getDefaultInventoryAttribute5());
        }
        else {
            inventory.setAttribute5(null);
        }





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
            writeInventoryAdjustRequest(inventory, newQuantity, inventoryQuantityChangeType, documentNumber, comment, null);
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
            resultInventory = processRemoveInventory(inventory, InventoryQuantityChangeType.INVENTORY_ADJUST,  documentNumber, comment, null);
        }
        else if (inventory.getQuantity() > newQuantity) {
            // OK we are adjust down, let's split the original inventory
            // and move the difference into a new location

            inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.INVENTORY_ADJUSTMENT,
                    "quantity", String.valueOf(inventory.getQuantity()), String.valueOf(newQuantity),
                    documentNumber, comment, null);
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
                    documentNumber, comment, null);

            Inventory newInventory = inventory.split(newLpn, inventory.getQuantity() - newQuantity);

            // Save both inventory before move
            inventory = saveOrUpdate(inventory);
            newInventory = save(newInventory);
            logger.debug("Inventory is split");

            // Remove the new inventory
            processRemoveInventory(newInventory, InventoryQuantityChangeType.INVENTORY_ADJUST,"", "", null);
            logger.debug("The inventory with reduced quantity has been removed");
            resultInventory =  inventory;
        }
        else {
            // if we are here, we are adjust quantity up
            // We will create a inventory in a logic location
            // and then move the inventory onto the existing inventory

            inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.INVENTORY_ADJUSTMENT,
                    "quantity", String.valueOf(inventory.getQuantity()), String.valueOf(newQuantity),
                    documentNumber, comment, null);
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
                    inventoryAdjustmentRequest.getDocumentNumber(), inventoryAdjustmentRequest.getComment(),
                    null, true);
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
            return addInventory(inventory, InventoryQuantityChangeType.AUDIT_COUNT, true);
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
                        null,
                        null,
                        null,
                        null,
                        null, null,
                        pickIds,
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
                        null, null, null,
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
                    null,
                    inboundLocationId,
                    null,
                    null, null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    lpn,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null, null, null,
                    null
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
                        null,
                        inboundLocationId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null, null,
                        null,
                        null,
                        pickIds,
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
                        null, null, null,
                        null
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
        if (Objects.isNull(quantity)) {
            quantity = inventories.stream().map(Inventory::getQuantity).mapToLong(Long::longValue).sum();
        }
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
        List<Inventory> inventories = findByLpn(warehouse.getId(), lpn, false);
        logger.debug("We get {} inventories with warehouse {} / {}, lpn {}",
                inventories.size(),
                warehouse.getId(),
                warehouse.getName(),
                lpn);
        if (inventories.size() > 0) {

            logger.debug("LPN " + lpn + " already exists");
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
                null,
                locationName,
                locationId,
                locationIds,
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
                null,
                null,
                null,
                null, null,
                null,
                null,
                null,
                null,
                null,
                null,
                null, null,
                null);


    }

    public String removeInventores(Long companyId, String inventoryIds, Boolean asyncronized) {

        if (Boolean.TRUE.equals(asyncronized)) {
            User user = userService.getCurrentUser(companyId);

            // we will need to run the removal asyncronized. let's make sure
            // there's no approval needed for the current user
            ExecutorService executor = Executors.newFixedThreadPool(10);

            // for asyncroized we will mark the inventory as removed
            // then actually remove the inventory
            markAsRemoved(inventoryIds);



            for(String id : inventoryIds.split(",")) {
                executor.execute(() -> {

                    Inventory inventory = findById(Long.parseLong(id));
                    if (InventoryQuantityChangeType.INVENTORY_ADJUST.isNoApprovalNeeded()) {

                        logger.debug("No approval needed, let's just go ahread with the adding inventory!");
                        processRemoveInventory(inventory, InventoryQuantityChangeType.INVENTORY_ADJUST, "", "", null);
                    }
                    else if (inventoryAdjustmentThresholdService.isInventoryAdjustExceedThreshold(inventory,
                            InventoryQuantityChangeType.INVENTORY_ADJUST, inventory.getQuantity(), 0l,
                            user)) {

                        writeInventoryAdjustRequest(inventory, 0L,
                                InventoryQuantityChangeType.INVENTORY_ADJUST,
                                "", "", null, false);
                    }
                    else {
                        logger.debug("No approval needed, let's just go ahread with the adding inventory!");
                        processRemoveInventory(inventory, InventoryQuantityChangeType.INVENTORY_ADJUST, "", "", null);
                    }
                });
            }

            return "remove request has been sent";
        }
        else {

            return "all inventory has been removed";
        }

    }

    private void markAsRemoved(String inventoryIds) {
        List<Long> ids =  Arrays.stream(inventoryIds.split(",")).map(Long::parseLong).collect(Collectors.toList());

        inventoryRepository.markAsRemoved(ids);

    }

    public Long getAvailableQuantityForMPS(Long warehouseId, Long itemId, String itemName, int lpnLimit) {

        return getAvailableInventoryForMPS(warehouseId, itemId, itemName, lpnLimit).stream()
                .mapToLong(Inventory::getQuantity).sum();

    }

    public List<Inventory> getAvailableInventoryForMPS(Long warehouseId, Long itemId, String itemName, int lpnLimit) {

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
                    .findByItemIdAndInventoryStatusId(itemId, availableInventoryStatus.get().getId(),
                            PageRequest.of(0, lpnLimit))
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
                                     String itemNames,
                                    String itemPackageTypeName,
                                    Long clientId,
                                    String clientIds,
                                    String itemFamilyIds,
                                    Long inventoryStatusId,
                                    String locationName,
                                    Long locationId,
                                    String locationIds,
                                    Long locationGroupId,
                                     Long pickZoneId,
                                    String receiptId,
                                     String receiptIds,
                                     String receiptNumber,
                                    String customerReturnOrderId,
                                    Long workOrderId,
                                    String workOrderLineIds,
                                    String workOrderByProductIds,
                                    String pickIds,
                                    String lpn,
                                    String color, String productSize, String style,
                                     String attribute1, String attribute2, String attribute3,
                                     String attribute4, String attribute5,
                                    String inventoryIds,
                                    Boolean notPutawayInventoryOnly,
                                    Boolean includeVirturalInventory,
                                    ClientRestriction clientRestriction) {
        List<Inventory> inventories = findAll(warehouseId, itemId,
                itemName, itemNames, itemPackageTypeName, clientId, clientIds, itemFamilyIds, inventoryStatusId,
                locationName, locationId, locationIds, locationGroupId, pickZoneId,
                receiptId, receiptIds, receiptNumber, customerReturnOrderId,  workOrderId, workOrderLineIds,
                workOrderByProductIds,
                pickIds, lpn, color, productSize, style,
                attribute1, attribute2, attribute3, attribute4, attribute5,
                inventoryIds, notPutawayInventoryOnly, includeVirturalInventory,
                clientRestriction,
                false,
                null);

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
    public String uploadInventoryData(Long warehouseId,
                                      File file,
                                      Boolean removeExistingInventory) throws IOException {

        String username = userService.getCurrentUserName();

        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();
        // before we add new
        clearInventoryFileUploadMap();
        inventoryFileUploadProgress.put(fileUploadProgressKey, 0.0);
        inventoryFileUploadResults.put(fileUploadProgressKey, new ArrayList<>());

        List<InventoryCSVWrapper> inventoryCSVWrappers = loadInventoryData(file);
        inventoryFileUploadProgress.put(fileUploadProgressKey, 10.0);

        logger.debug("get {} record from the file", inventoryCSVWrappers.size());

        // List<Inventory> inventories = convertFromWrapper(warehouseId, inventoryCSVWrappers, false);
        // logger.debug("convert {} of the records into inventory structure",
        //        inventories.size());

        // 10% after we setup all the inventory structure
        // inventoryFileUploadProgress.put(fileUploadProgressKey, 10.0);

        // start a new thread to process the inventory
        new Thread(() -> {
            // set to store the locations that we already removed the inventory so that we don't
            // have to clear location mutliple times and after we add inventory in this thread
            Set<Long> inventoryRemovedLocationIdSet = new HashSet<>();

            // loop through each inventory
            int totalInventoryCount = inventoryCSVWrappers.size();
            int index = 0;
            for (InventoryCSVWrapper inventoryCSVWrapper : inventoryCSVWrappers) {
                // in case anything goes wrong, we will continue with the next record
                // and save the result with error message to the result set
                inventoryFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalInventoryCount) * (index));
                try {
                    Inventory inventory = convertFromWrapper(warehouseId, inventoryCSVWrapper);
                    inventoryFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalInventoryCount) * (index + 0.25));

                    // check if we will need to clear the location only if
                    // 1. the removeExistingInventory is passed in and set to true
                    // 2. the location has not been cleared yet
                    if (Boolean.TRUE.equals(removeExistingInventory) &&
                            !inventoryRemovedLocationIdSet.contains(inventory.getLocationId())) {
                        removeInventoryByLocation(inventory.getLocationId());
                        // add the location to the set so that we won't remove it again
                        inventoryRemovedLocationIdSet.add(inventory.getLocationId());
                    }

                    // we are half way through creating the inventory
                    inventoryFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalInventoryCount) * (index + 0.5));


                    addInventory(username, inventory,
                            InventoryQuantityChangeType.INVENTORY_UPLOAD,
                            "", "", null,
                            true);

                    // we complete this inventory
                    inventoryFileUploadProgress.put(fileUploadProgressKey, 10.0 + (90.0 / totalInventoryCount) * (index + 1));

                    List<FileUploadResult> fileUploadResults = inventoryFileUploadResults.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            inventoryCSVWrapper.toString(),
                            "success", ""
                    ));
                    inventoryFileUploadResults.put(fileUploadProgressKey, fileUploadResults);

                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process inventory upload file record: {}, \n error message: {}",
                            inventoryCSVWrapper,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = inventoryFileUploadResults.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            inventoryCSVWrapper.toString(),
                            "fail", ex.getMessage()
                    ));
                    inventoryFileUploadResults.put(fileUploadProgressKey, fileUploadResults);
                }
                finally {

                    index++;
                }
            }
            // after we process all inventory, mark the progress to 100%
            inventoryFileUploadProgress.put(fileUploadProgressKey, 100.0);
        }).start();

        return fileUploadProgressKey;
    }

    private void clearInventoryFileUploadMap() {

        if (inventoryFileUploadProgress.size() > INVENTORY_FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = inventoryFileUploadProgress.keySet().iterator();
            while(iterator.hasNext()) {
                String key = iterator.next();
                // key should be in the format of
                // warehouseId + "-" + username + "-" + System.currentTimeMillis()
                long lastTimeMillis = Long.parseLong(key.substring(key.lastIndexOf("-")));
                // check the different between current time stamp and the time stamp of when
                // the record is generated
                if (System.currentTimeMillis() - lastTimeMillis > 60 * 60 * 1000) {
                    iterator.remove();
                }
            }
        }

        if (inventoryFileUploadResults.size() > INVENTORY_FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = inventoryFileUploadResults.keySet().iterator();
            while(iterator.hasNext()) {
                String key = iterator.next();
                // key should be in the format of
                // warehouseId + "-" + username + "-" + System.currentTimeMillis()
                long lastTimeMillis = Long.parseLong(key.substring(key.lastIndexOf("-")));
                // check the different between current time stamp and the time stamp of when
                // the record is generated
                if (System.currentTimeMillis() - lastTimeMillis > 60 * 60 * 1000) {
                    iterator.remove();
                }
            }
        }
    }



    public double getInventoryFileUploadProgress(String key) {
        return inventoryFileUploadProgress.getOrDefault(key, 100.0);
    }

    public List<FileUploadResult> getFileUploadResult(Long warehouseId, String key) {
        return inventoryFileUploadResults.getOrDefault(key, new ArrayList<>());
    }

    public String uploadPutawayInventoryData(Long warehouseId, File file) throws IOException {

        String username = userService.getCurrentUserName();

        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();
        // before we add new
        clearInventoryPutawayFileUploadMap();
        inventoryPutawayFileUploadProgress.put(fileUploadProgressKey, 0.0);
        inventoryPutawayFileUploadResults.put(fileUploadProgressKey, new ArrayList<>());


        List<InventoryPutawayCSVWrapper> inventoryPutawayCSVWrappers = loadInventoryPutawayData(file);
        inventoryPutawayCSVWrappers.forEach(
                inventoryPutawayCSVWrapper -> inventoryPutawayCSVWrapper.trim()
        );
        inventoryPutawayFileUploadProgress.put(fileUploadProgressKey, 10.0);

        logger.debug("get {} record from the file", inventoryPutawayCSVWrappers.size());

        new Thread(() -> {

            // loop through each inventory
            int totalInventoryCount = inventoryPutawayCSVWrappers.size();
            int index = 0;
            for (InventoryPutawayCSVWrapper inventoryPutawayCSVWrapper : inventoryPutawayCSVWrappers) {
                // in case anything goes wrong, we will continue with the next record
                // and save the result with error message to the result set
                inventoryPutawayFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalInventoryCount) * (index));
                try {

                    if (Strings.isBlank(inventoryPutawayCSVWrapper.getDestinationLocation())) {
                        throw InventoryException.raiseException("can't move the inventory as the destination location is empty");
                    }

                    Inventory inventory = findBestInventoryForPutaway(warehouseId, inventoryPutawayCSVWrapper);
                    inventoryPutawayFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalInventoryCount) * (index + 0.25));


                    Location destinationLocation = warehouseLayoutServiceRestemplateClient.getLocationByName(
                            warehouseId, inventoryPutawayCSVWrapper.getDestinationLocation()
                    );
                    inventoryPutawayFileUploadProgress.put(fileUploadProgressKey, 10.0 +  (90.0 / totalInventoryCount) * (index + 0.5));
                    if (Objects.isNull(destinationLocation)) {
                        throw InventoryException.raiseException("can't move the inventory as destination location " +
                                inventoryPutawayCSVWrapper.getDestinationLocation() + " is not valid");
                    }
                    moveInventory(inventory, destinationLocation);
                    // we complete this inventory
                    inventoryPutawayFileUploadProgress.put(fileUploadProgressKey, 10.0 + (90.0 / totalInventoryCount) * (index + 1));

                    List<FileUploadResult> fileUploadResults = inventoryPutawayFileUploadResults.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            inventoryPutawayCSVWrapper.toString(),
                            "success", ""
                    ));
                    inventoryPutawayFileUploadResults.put(fileUploadProgressKey, fileUploadResults);

                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process inventory upload file record: {}, \n error message: {}",
                            inventoryPutawayCSVWrapper,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = inventoryPutawayFileUploadResults.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            inventoryPutawayCSVWrapper.toString(),
                            "fail", ex.getMessage()
                    ));
                    inventoryPutawayFileUploadResults.put(fileUploadProgressKey, fileUploadResults);
                }
                finally {

                    index++;
                }
            }
            // after we process all inventory, mark the progress to 100%
            inventoryPutawayFileUploadProgress.put(fileUploadProgressKey, 100.0);
        }).start();

        return fileUploadProgressKey;

    }

    private Inventory findBestInventoryForPutaway(Long warehouseId, InventoryPutawayCSVWrapper inventoryPutawayCSVWrapper) {
        // find the best inventory based on the passed in value from the CSV file
        // 1. LPN: by LPN
        // 2. Location: if LPN is not passed in, then by location + item + quantity + other attribute
        if (Strings.isBlank(inventoryPutawayCSVWrapper.getLocation()) && Strings.isBlank(inventoryPutawayCSVWrapper.getLpn())) {
            throw InventoryException.raiseException("Can't putaway the inventory as at least one of LPN or Location needs to be passed in");
        }
        List<Inventory> inventories = null;
        if (Strings.isNotBlank(inventoryPutawayCSVWrapper.getLpn())) {
            inventories = findByLpn(warehouseId, inventoryPutawayCSVWrapper.getLpn());
        }
        else if (Strings.isNotBlank(inventoryPutawayCSVWrapper.getLocation())) {
            Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                    warehouseId, inventoryPutawayCSVWrapper.getLocation()
            );
            if (Objects.isNull(location)) {
                throw InventoryException.raiseException("Can't find location by name " + inventoryPutawayCSVWrapper.getLocation());
            }
            inventories = findByLocationId(location.getId());
        }

        if (Objects.isNull(inventories) || inventories.isEmpty()) {
            throw InventoryException.raiseException("can't find the inventory by LPN: " +
                    inventoryPutawayCSVWrapper.getLpn() + ", location: " +
                    inventoryPutawayCSVWrapper.getLocation());
        }

        // we get a list of inventory, then loop through the CSV line and see if we can find one that
        // has exact match. If there're multiple lines match, we will just return the first line
        Inventory inventory = findFirstMatchedInventoryForCSVPutaway(inventories, inventoryPutawayCSVWrapper);
        if (Objects.isNull(inventory)) {
            throw InventoryException.raiseException("Can't find matched inventory  by LPN: " +
                    inventoryPutawayCSVWrapper.getLpn() + ", location: " +
                    inventoryPutawayCSVWrapper.getLocation());
        }
        return inventory;
    }

    /**
     * Find the first inventory from the list that matches with the CSV record
     * @param inventories
     * @param inventoryPutawayCSVWrapper
     * @return
     */
    private Inventory findFirstMatchedInventoryForCSVPutaway(List<Inventory> inventories, InventoryPutawayCSVWrapper inventoryPutawayCSVWrapper) {
        return inventories.stream().filter(
                inventory -> isMatch(inventory, inventoryPutawayCSVWrapper)
        ).findFirst().orElse(null);
    }

    /**
     * check if the inventory matches with the CSV record line
     * @param inventory
     * @param inventoryPutawayCSVWrapper
     * @return
     */
    private boolean isMatch(Inventory inventory, InventoryPutawayCSVWrapper inventoryPutawayCSVWrapper) {

        // if the CSV line has the client setup, then only return the inventory
        // that is not empty and match with the client from the CSV line
        if (Strings.isNotBlank(inventoryPutawayCSVWrapper.getClient()) &&
                (Objects.isNull(inventory.getClient()) ||
                        !inventoryPutawayCSVWrapper.getClient().equalsIgnoreCase(
                                inventory.getClient().getName()
                        )
                )) {
            logger.debug(">> Not Match(client), Inventory's client: {}" +
                            ", CSV Line's client: {}",
                    Objects.isNull(inventory.getClient()) ?
                        "N/A" : inventory.getClient().getName(),
                    inventoryPutawayCSVWrapper.getClient());
            return false;

        }
        if (Strings.isBlank(inventoryPutawayCSVWrapper.getClient()) &&
                Objects.nonNull(inventory.getClientId())) {
            logger.debug(">> Not Match(client), Inventory's client: {}" +
                            ", CSV Line has not client setup",
                    inventory.getClientId());
            return false;
        }

        if (Strings.isNotBlank(inventoryPutawayCSVWrapper.getLpn()) &&
                !inventoryPutawayCSVWrapper.getLpn().equalsIgnoreCase(inventory.getLpn())) {
            logger.debug(">> Not Match(LPN), Inventory's lpn: {}" +
                            ", CSV Line's lpn: {}",
                    inventory.getLpn(),
                    inventoryPutawayCSVWrapper.getLpn());
            return false;

        }
        if (Strings.isNotBlank(inventoryPutawayCSVWrapper.getLocation()) &&
                !inventoryPutawayCSVWrapper.getLocation().equalsIgnoreCase(inventory.getLocation().getName())) {
            logger.debug(">> Not Match(Location), Inventory's Location: {}" +
                            ", CSV Line's Location: {}",
                    inventory.getLocation().getName(),
                    inventoryPutawayCSVWrapper.getLocation());
            return false;

        }

        if (Strings.isNotBlank(inventoryPutawayCSVWrapper.getItem()) &&
                !inventoryPutawayCSVWrapper.getItem().equalsIgnoreCase(inventory.getItem().getName())) {
            logger.debug(">> Not Match(Item), Inventory's Item: {}" +
                            ", CSV Line's Item: {}",
                    inventory.getItem().getName(),
                    inventoryPutawayCSVWrapper.getItem());
            return false;

        }
        if (Strings.isNotBlank(inventoryPutawayCSVWrapper.getItemPackageType()) &&
                !inventoryPutawayCSVWrapper.getItemPackageType().equalsIgnoreCase(inventory.getItemPackageType().getName())) {
            logger.debug(">> Not Match(Item Package Type), Inventory's Item Package Type: {}" +
                            ", CSV Line's Item Package Type: {}",
                    inventory.getItemPackageType().getName(),
                    inventoryPutawayCSVWrapper.getItemPackageType());
            return false;

        }
        if (Objects.nonNull(inventoryPutawayCSVWrapper.getQuantity())) {
            // if the unit of measure is pass in, calculate the quantity based on the
            // unit of measure and quantity. Otherwise, the quantity is the unit quantity
            int unitOfMeasureQuantity = 1;
            if (Strings.isNotBlank(inventoryPutawayCSVWrapper.getUnitOfMeasure())) {
                unitOfMeasureQuantity = inventory.getItemPackageType().getItemUnitOfMeasures()
                        .stream().filter(itemUnitOfMeasure ->
                                inventoryPutawayCSVWrapper.getUnitOfMeasure().equalsIgnoreCase(
                                        itemUnitOfMeasure.getUnitOfMeasure().getName()
                                )).map(itemUnitOfMeasure -> itemUnitOfMeasure.getQuantity()
                        ).findFirst().orElse(1);
            }
            if (inventory.getQuantity() != inventoryPutawayCSVWrapper.getQuantity() * unitOfMeasureQuantity) {
                logger.debug(">> Not Match(Quantity), Inventory's Quantity: {}" +
                                ", CSV Line's Quantity: {}, unit of measure: {}, " +
                                " unit of measure quantity: {}, total quantity: {}",
                        inventory.getQuantity(),
                        inventoryPutawayCSVWrapper.getQuantity(),
                        inventoryPutawayCSVWrapper.getUnitOfMeasure(),
                        unitOfMeasureQuantity,
                        inventoryPutawayCSVWrapper.getQuantity() * unitOfMeasureQuantity
                );
                return false;
            }
        }
        if (Strings.isNotBlank(inventoryPutawayCSVWrapper.getInventoryStatus()) &&
                !inventoryPutawayCSVWrapper.getInventoryStatus().equalsIgnoreCase(inventory.getInventoryStatus().getName())) {
            logger.debug(">> Not Match(Inventory Status), Inventory's Inventory Status: {}" +
                            ", CSV Line's Inventory Status: {}",
                    inventory.getInventoryStatus().getName(),
                    inventoryPutawayCSVWrapper.getInventoryStatus());
            return false;

        }
        if (Strings.isNotBlank(inventoryPutawayCSVWrapper.getColor()) &&
                !inventoryPutawayCSVWrapper.getColor().equalsIgnoreCase(inventory.getColor())) {
            logger.debug(">> Not Match(Color), Inventory's Color: {}" +
                            ", CSV Line's Color: {}",
                    inventory.getColor(),
                    inventoryPutawayCSVWrapper.getColor());
            return false;

        }
        if (Strings.isNotBlank(inventoryPutawayCSVWrapper.getProductSize()) &&
                !inventoryPutawayCSVWrapper.getProductSize().equalsIgnoreCase(inventory.getProductSize())) {
            logger.debug(">> Not Match(Product Size), Inventory's Product Size: {}" +
                            ", CSV Line's Product Size: {}",
                    inventory.getProductSize(),
                    inventoryPutawayCSVWrapper.getProductSize());
            return false;

        }
        if (Strings.isNotBlank(inventoryPutawayCSVWrapper.getStyle()) &&
                !inventoryPutawayCSVWrapper.getStyle().equalsIgnoreCase(inventory.getStyle())) {
            logger.debug(">> Not Match(Style), Inventory's Style: {}" +
                            ", CSV Line's Style: {}",
                    inventory.getStyle(),
                    inventoryPutawayCSVWrapper.getStyle());
            return false;

        }
        return true;
    }

    public List<InventoryPutawayCSVWrapper> loadInventoryPutawayData(File file) throws IOException {

        return fileService.loadData(file, InventoryPutawayCSVWrapper.class);
    }

    public double getPutawayInventoryFileUploadProgress(String key) {
        return inventoryPutawayFileUploadProgress.getOrDefault(key, 100.0);
    }

    public List<FileUploadResult> getPutawayFileUploadResult(Long warehouseId, String key) {
        return inventoryPutawayFileUploadResults.getOrDefault(key, new ArrayList<>());
    }

    private void clearInventoryPutawayFileUploadMap() {

        if (inventoryPutawayFileUploadProgress.size() > INVENTORY_FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = inventoryPutawayFileUploadProgress.keySet().iterator();
            while(iterator.hasNext()) {
                String key = iterator.next();
                // key should be in the format of
                // warehouseId + "-" + username + "-" + System.currentTimeMillis()
                long lastTimeMillis = Long.parseLong(key.substring(key.lastIndexOf("-")));
                // check the different between current time stamp and the time stamp of when
                // the record is generated
                if (System.currentTimeMillis() - lastTimeMillis > 60 * 60 * 1000) {
                    iterator.remove();
                }
            }
        }

        if (inventoryPutawayFileUploadResults.size() > INVENTORY_FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = inventoryPutawayFileUploadResults.keySet().iterator();
            while(iterator.hasNext()) {
                String key = iterator.next();
                // key should be in the format of
                // warehouseId + "-" + username + "-" + System.currentTimeMillis()
                long lastTimeMillis = Long.parseLong(key.substring(key.lastIndexOf("-")));
                // check the different between current time stamp and the time stamp of when
                // the record is generated
                if (System.currentTimeMillis() - lastTimeMillis > 60 * 60 * 1000) {
                    iterator.remove();
                }
            }
        }
    }

    public int getLPNCountFromStorageLocation(Long warehouseId, Long clientId) {
        List<LocationGroup> storageLocationGroups =
                warehouseLayoutServiceRestemplateClient.getStorageLocationGroups(warehouseId);
        if (Objects.isNull(storageLocationGroups) || storageLocationGroups.isEmpty()) {
            logger.debug("There's no storage location defined for warehouse {}", warehouseId);
            return 0;
        }
        int totalLPNCount = 0;
        String locationGroupIds = storageLocationGroups.stream().map(LocationGroup::getId)
                .map(String::valueOf).collect(Collectors.joining(","));

        logger.debug("start to get locations from storage location groups {}",
                locationGroupIds);

        List<Long> locationIds =
                warehouseLayoutServiceRestemplateClient.getLocationIdsByLocationGroups(
                        warehouseId, locationGroupIds);

        logger.debug("We get {} storage locations, let's do 100 location a time",
                locationIds.size());
        // split the location id list into smaller list with 100 location ids per list
        int subListSize = 100;
        Collection<List<Long>> listOfLocationIds =
                locationIds.stream()
                        .collect(Collectors.groupingBy(s -> (s-1)/subListSize))
                        .values();
        for (List<Long> subListOfLocationIds : listOfLocationIds) {
            totalLPNCount += inventoryRepository.countByLocationIdInAndClientId(subListOfLocationIds, clientId);
        }
        logger.debug("total LPNs from storage lcoations: {}",
                totalLPNCount);
        return totalLPNCount;
    }

    public List<Inventory> findByClientId(Long warehouseId, Long clientId) {
        return findByClientId(warehouseId, clientId, true);
    }
    public List<Inventory> findByClientId(Long warehouseId, Long clientId,
                                          boolean includeDetails) {
        return findAll(warehouseId,
                null,
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
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null, includeDetails,
                null);
    }

    /**
     * Process bulk pick for the inventory
     * 1. move the lpn to the next location
     * 2. consolidate and split the inventory so that the result of the inventory
     *    matches with the picks in the bulk pick
     * 3. Setup the pick id for each inventory
     * @param warehouseId
     * @param nextLocationId
     * @param lpn
     * @return
     */
    public List<Inventory> processBulkPick(Long warehouseId, String lpn,
                                           Long nextLocationId,
                                           BulkPick bulkPick) {
        List<Inventory> inventories = findByLpn(warehouseId, lpn);
        if (inventories.isEmpty()) {
            throw InventoryException.raiseException("can't find the inventory with LPN "+
                    lpn);
        }
        // before we continue, let's make sure the inventory has exactly
        // the same total quantity as the bulk pick
        Long inventoryQuantity = inventories.stream().mapToLong(Inventory::getQuantity).sum();
        Long pickQuantity = bulkPick.getPicks().stream().mapToLong(Pick::getQuantity).sum();
        if (!inventoryQuantity.equals(pickQuantity)) {
            throw InventoryException.raiseException("Can't bulk pick the inventory of LPN " +
                    lpn + " for bulk pick " + bulkPick.getNumber());
        }

        // ok we get a list of inventory from the LPN,
        // let's consolidate and split based on the bulk pick
        inventories = consolidateAndSplitForBulkPick(inventories, bulkPick);

        // move the whole LPN to the destination location

        logger.debug("after we call the consolidateAndSplitForBulkPick, we have");

        inventories.forEach(
                resultInventory -> {
                    logger.debug("inventory: lpn = {}, quantity = {}, picked id = {}",
                            resultInventory.getLpn(), resultInventory.getQuantity(),
                            resultInventory.getPickId());
                }
        );
        bulkPick.getPicks().forEach(
                pick -> {
                    logger.debug("pick: id = {}, pick number = {}, pick quantity = {}",
                            pick.getId(), pick.getNumber(), pick.getQuantity());
                }
        );

        logger.debug("start to move the inventory to the destination location");
        inventories = moveInventoryForBulkPick(warehouseId, inventories, bulkPick, nextLocationId);


        // return the result
        return inventories.stream().map(inventory -> saveOrUpdate(inventory)).collect(Collectors.toList());


    }

    private List<Inventory> moveInventoryForBulkPick(Long warehouseId, List<Inventory> inventories, BulkPick bulkPick, Long nextLocationId) {
        Location destination = warehouseLayoutServiceRestemplateClient.getLocationById(nextLocationId);
        for (Inventory inventory : inventories) {
            inventory.setLocationId(nextLocationId);
            inventory.setLocation(destination);
            // the inventory should already have the pick id setup
            Pick matchedPick = bulkPick.getPicks().stream().filter(
                    pick -> pick.getId().equals(inventory.getPickId())
            ).findFirst().orElse(null);
            if (Objects.nonNull(matchedPick)) {

                markAsPicked(inventory, destination, matchedPick);
            }
            inventoryActivityService.logInventoryActivitiy(inventory, InventoryActivityType.INVENTORY_MOVEMENT,
                    "location", inventory.getLocation().getName(), destination.getName());

            recalculateLocationSizeForInventoryMovement(inventory.getLocation(), destination, inventory.getSize());

            if (Objects.nonNull(inventory.getPickId())) {
                outbuondServiceRestemplateClient.refreshPickMovement(inventory.getPickId(), destination.getId(), inventory.getQuantity());
            }
        }
        return inventories;
    }

    /**
     * Split and consolidate the inventory based on the buck pick's requirement so that
     *  each pick in this bulk pick will have its own inventory
     * @param inventories
     * @param bulkPick
     * @return
     */
    private List<Inventory> consolidateAndSplitForBulkPick(List<Inventory> inventories, BulkPick bulkPick) {

        // Step 1: find all the inventory that already have quantity match with the pick in the bulk pick
        // we will separate those inventory and pick as they don't require any future process
        Set<Long> processedInventoryIds = new HashSet<>();
        Set<Long> processedPickIds = new HashSet<>();

        // key: inventory id
        // value: pick id
        Map<Long, Long> inventoryPickMap = new HashMap<>();
        inventories.forEach(
                inventory -> {
                    // see if we can find any pick that in the bulk that has the same quantity
                    Pick matchedPick = bulkPick.getPicks().stream().filter(
                            pick -> !processedPickIds.contains(pick.getId()) &&
                                    pick.getQuantity().equals(inventory.getQuantity())
                    ).findFirst().orElse(null);

                    if (Objects.nonNull(matchedPick)) {
                        // OK, we find a pick that can be assigned to the inventory
                        inventoryPickMap.put(inventory.getId(), matchedPick.getId());
                        processedPickIds.add(matchedPick.getId());

                        // mark the inventory as picked by the matched id
                        inventory.setPickId(matchedPick.getId());
                    }
                }
        );
        // now we have a map with matched inventory and a list of all inventories. We will
        // need to get all the un matched inventory and then
        // 1. consolidate them into one inventory structure
        // 2. split the single inventory into multiple inventories based on the pick quantities

        List<Inventory> matchedInventory = inventories.stream().filter(
                inventory -> inventoryPickMap.containsKey(inventory.getId())
        ).collect(Collectors.toList());


        List<Inventory> unmatchedInventory = inventories.stream().filter(
                inventory -> !inventoryPickMap.containsKey(inventory.getId())
        ).collect(Collectors.toList());

        if (!unmatchedInventory.isEmpty()) {
            // ok, let's consolidate unmatched inventory into one inventory
            Inventory consolidatedInventory = consolidateInventory(unmatchedInventory);

            List<Pick> unprocessedPicks = bulkPick.getPicks().stream().filter(
                    pick -> !processedPickIds.contains(pick.getId())
            ).collect(Collectors.toList());

            List<Inventory> splitInventory = splitInventoryForPickGroup(consolidatedInventory, unprocessedPicks);
            // when we split the inventory, the inventory should already have been setup with the
            // matched picks
            // let's just group them into the previous processed inventory
            matchedInventory.addAll(splitInventory);
        }

        // when we are here, we should already have all inventory matched with the pick
        return matchedInventory;


    }

    /**
     * Split the single inventory into multiple inventories and distribute the quantity based on the
     * list of pick
     * @param inventory
     * @param picks
     * @return
     */
    private List<Inventory> splitInventoryForPickGroup(Inventory inventory, List<Pick> picks) {

        logger.debug("start to split the inventory based on the group of picks");
        logger.debug("Inventory: LPN = {}, quantity = {}",
                inventory.getLpn(), inventory.getQuantity());
        picks.forEach(
                pick -> {
                    logger.debug("pick: id = {}, pick number = {}, pick quantity = {}",
                            pick.getId(), pick.getNumber(), pick.getQuantity());
                }
        );
        // before we continue, let's make sure the inventory has exactly
        // the same total quantity as the bulk pick
        Long pickQuantity = picks.stream().mapToLong(Pick::getQuantity).sum();
        if (!inventory.getQuantity().equals(pickQuantity)) {
            throw InventoryException.raiseException("Can't split the inventory's quantity " +
                     " for the group of  pick. inventory's quantity " + inventory.getQuantity() +
                            "doesn't match with the total quantity of the group: " + pickQuantity);
        }


        List<Inventory> inventories = new ArrayList<>();
        if (picks.size() == 0) {
            throw InventoryException.raiseException("No pick left to match with the single inventory");

        }
        else if (picks.size() == 1) {
            logger.debug("There's only one pick left and we only have one inventory, perfect match");
            inventory.setPickId(picks.get(0).getId());
            inventories.add(inventory);
        }
        else {
            logger.debug("we get {} picks and the inventory's quantity is {}, we will split the inventory " +
                    " and distribute the quantity into those picks",
                    picks.size(), inventory.getQuantity());
            for (Pick pick : picks) {
                if(pick.getQuantity().equals(inventory.getQuantity())) {
                    // ok, this is the last inventory and pick
                    logger.debug("the current inventory {} has exactly the same quantity: {} left" +
                            "  for pick {}'s quantity: {}",
                            inventory.getLpn(),
                            inventory.getQuantity(),
                            pick.getNumber(), pick.getQuantity());
                    inventory.setPickId(pick.getId());
                    inventories.add(inventory);
                }
                else {
                    // note: Since we are split inventory for a new pick, we may need to generate a new LPN
                    // as normally this means the new LPN most likely will have a different destination, other
                    // than the original one
                    String newLPN = commonServiceRestemplateClient.getNextLpn(inventory.getWarehouseId());

                    Inventory newInventory = inventory.split(newLPN, pick.getQuantity());
                    newInventory.setPickId(pick.getId());
                    // let's save the inventory first
                    newInventory = saveOrUpdate(newInventory);
                    inventories.add(newInventory);
                    logger.debug("we will have to split {} / {} into a new inventory {} /{} " +
                            " , with new inventory's quantity {} and there's still {} left in the original invenotry",
                            inventory.getId(), inventory.getLpn(),
                            Objects.isNull(newInventory.getId()) ? "N/A" : newInventory.getId(),
                            newInventory.getLpn(),
                            newInventory.getQuantity(),
                            inventory.getQuantity());
                }
            }
        }
        logger.debug("after split the inventory based on the group of picks, we have");

        inventories.forEach(
                resultInventory -> {
                    logger.debug("inventory: lpn = {}, quantity = {}, assigned to the pick {}",
                            resultInventory.getLpn(), resultInventory.getQuantity(),
                            Objects.isNull(resultInventory.getPickId()) ? "N/A" :
                                resultInventory.getPickId());
                }
        );
        picks.forEach(
                pick -> {
                    logger.debug("pick: id = {}, pick number = {}, pick quantity = {}",
                            pick.getId(), pick.getNumber(), pick.getQuantity());
                }
        );
        return inventories;


    }

    /**
     * Consolidate inventory strucutre into one. We will only allow consolidate
     * inventory from the same LPN with same attribute
     * @param inventories
     * @return
     */
    private Inventory consolidateInventory(List<Inventory> inventories) {
        if (inventories.isEmpty()) {
            throw InventoryException.raiseException("there's no inventory to be consolidated");
        }
        else if (inventories.size() == 1) {
            return inventories.get(0);
        }
        // make sure we only have one LPN and same attribute
        validateForConsolidateInventory(inventories);

        // we will get the attribute from the first inventory and then
        // consolidate all the quantity from the list of inventory into this single inventory
        Inventory result = inventories.get(0);
        // we will set the total quantity to the first inventory
        // remove the other inventory
        // and then save the first one
        for (Inventory inventory : inventories) {
            if (!inventory.equals(result)) {
                result.setQuantity(result.getQuantity() + inventory.getQuantity());
                delete(inventory);
            }
        }
        return saveOrUpdate(result);
    }

    private void validateForConsolidateInventory(List<Inventory> inventories) {

        if (inventories.stream().map(Inventory::getLpn).distinct().count() > 1) {
            throw InventoryException.raiseException("Can't consolidate inventory from multiple LPNs");
        }
        if (inventories.stream().map(Inventory::getInventoryStatus).distinct().count() > 1) {
            throw InventoryException.raiseException("Can't consolidate inventory with multiple status");
        }
        if (inventories.stream().map(Inventory::getItemPackageType).distinct().count() > 1) {
            throw InventoryException.raiseException("Can't consolidate inventory with multiple item package type");
        }
        if (inventories.stream().map(Inventory::getItem).distinct().count() > 1) {
            throw InventoryException.raiseException("Can't consolidate inventory with multiple item ");
        }
        if (inventories.stream().map(Inventory::getClientId).distinct().count() > 1) {
            throw InventoryException.raiseException("Can't consolidate inventory with multiple client ");
        }
        if (inventories.stream().map(Inventory::getColor).distinct().count() > 1) {
            throw InventoryException.raiseException("Can't consolidate inventory with multiple attribute: Color ");
        }
        if (inventories.stream().map(Inventory::getProductSize).distinct().count() > 1) {
            throw InventoryException.raiseException("Can't consolidate inventory with multiple attribute: Product Size ");
        }
        if (inventories.stream().map(Inventory::getStyle).distinct().count() > 1) {
            throw InventoryException.raiseException("Can't consolidate inventory with multiple attribute: Style ");
        }
    }


    public List<Inventory> relabelInventories(String ids, String newLPN, Boolean mergeWithExistingInventory) {
        return Arrays.stream(ids.split(",")).map(
                id -> relabelLPN(Long.parseLong(id), newLPN, mergeWithExistingInventory)
        ).collect(Collectors.toList());
    }

    /**
     * Check if we can allocate the item from certain location and lpn(both are optional) for certain
     * # order line
     * # work order
     * # work order line
     * @param warehouseId
     * @param itemId
     * @param inventoryStatusId
     * @param locationId
     * @param lpn
     * @return
     */
    public List<AllocationDryRunResult> getAllocationDryRunResult(Long warehouseId, Long clientId,
                                                                  Long itemId, Long inventoryStatusId,
                                                                  Long locationId, String lpn,
                                                                  ClientRestriction clientRestriction) {

        // let's get all the inventory based on the criteria
        List<Inventory> availableInventories =
                findAll(warehouseId, itemId,
                        null, null, null,
                        clientId, null, null,
                        inventoryStatusId, null,
                        locationId, null, null,
                        null,
                        null, null, null, null, null,
                        null, null, null,
                        lpn, null, null, null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null, null, false, clientRestriction,
                         false, null);

        if (availableInventories.isEmpty()) {
            return new ArrayList<>();
        }

        // Map to save the quantity of the item in each location
        // key: location id
        // value: quantity of the inventory in the location
        Map<Long, Long> locationInventoryQuantityMap = new HashMap<>();
        // if location id or LPN is passed in , we will only verify the typical location
        Long validationSpecificLocation = locationId;
        if (Strings.isNotBlank(lpn)) {
            validationSpecificLocation = availableInventories.get(0).getLocationId();
        }

        List<Inventory> availableInventoryForQuantityValidation =
                findAll(warehouseId, itemId,
                        null, null, null,
                        clientId, null, null,
                        inventoryStatusId, null,
                        validationSpecificLocation, null, null,
                        null, null, null, null,
                        null, null, null,
                        null, null, null, null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null, null, false, clientRestriction,
                        false, null );

        availableInventoryForQuantityValidation.forEach(
                inventory -> {
                    Long quantity = locationInventoryQuantityMap.getOrDefault(inventory.getLocationId(), 0l);
                    locationInventoryQuantityMap.put(inventory.getLocationId(), quantity + inventory.getQuantity());
                }
        );
        // let's get the outstanding picks as well so we know if we can allocate more from the location

        // Map to save the quantity of the open pick in each location
        // key: location id
        // value: quantity of open pick for the specific item in the location
        Map<Long, Long> openPickQuantityMap = new HashMap<>();
        List<Pick> openPicks = outbuondServiceRestemplateClient.getOpenPicks(warehouseId, clientId,
                 itemId, inventoryStatusId, validationSpecificLocation);
        openPicks.forEach(
                pick -> {
                    Long quantity = openPickQuantityMap.getOrDefault(pick.getSourceLocationId(), 0l);
                    openPickQuantityMap.put(pick.getSourceLocationId(),
                            quantity + Math.max(pick.getQuantity() - pick.getPickedQuantity(), 0));
                }
        );


        return availableInventories.stream().map(
                inventory -> dryrunAllocation(inventory, locationInventoryQuantityMap, openPickQuantityMap)).collect(Collectors.toList());
    }

    /**
     * see if we can allocate from the inventory only if
     * # the inventory has no hold / lock / existing picks / etc that prevent the allocation
     * # the location of the inventory allows allocation
     * @param inventory
     * @return
     */
    private AllocationDryRunResult dryrunAllocation(Inventory inventory,
                                                    Map<Long, Long> locationInventoryQuantityMap,
                                                    Map<Long, Long> openPickQuantityMap ) {

        Long locationInventoryQuantity = locationInventoryQuantityMap.containsKey(inventory.getLocationId()) ?
                locationInventoryQuantityMap.get(inventory.getLocationId()) : 0l;

        Long locationOpenPickQuantity  = openPickQuantityMap.containsKey(inventory.getLocationId()) ?
                openPickQuantityMap.get(inventory.getLocationId()) : 0l;

        AllocationDryRunResult result = new AllocationDryRunResult(inventory,
                locationInventoryQuantity, locationOpenPickQuantity);

        // check if we can allocate from the inventory
        if (Objects.nonNull(inventory.getPickId())) {
            return result.fail("The inventory with id " + inventory.getId() +
                    " of LPN " + inventory.getLpn() + " is picked by pick work with id " + inventory.getPickId());
        }

        if (Objects.nonNull(inventory.getAllocatedByPickId())) {
            return result.fail("The inventory with id " + inventory.getId() +
                    " of LPN " + inventory.getLpn() + " is allocated by pick work with id " + inventory.getAllocatedByPickId());
        }
        if (Boolean.TRUE.equals(inventory.getVirtual())) {
            return result.fail("The inventory with id " + inventory.getId() +
                    " of LPN " + inventory.getLpn() + " is a virtual inventory, can't allocate from it");
        }
        if (Boolean.TRUE.equals(inventory.getInboundQCRequired())) {
            return result.fail("The inventory with id " + inventory.getId() +
                    " of LPN " + inventory.getLpn() + " requires inbound QC, please complete the QC first");
        }
        if (Boolean.TRUE.equals(inventory.getLockedForAdjust())) {
            return result.fail("The inventory with id " + inventory.getId() +
                    " of LPN " + inventory.getLpn() + " is locked for inventory adjustment, please complete the inventory adjustment first");
        }
        if (inventory.getLocks().stream().anyMatch(
                inventoryWithLock -> Boolean.TRUE.equals(inventoryWithLock.getLock().getAllowPick()))
        ) {
            return result.fail("The inventory with id " + inventory.getId() +
                    " of LPN " + inventory.getLpn() + " has locks that prevent it from being allocated");
        }

        // check if the location allows allocation
        if (Objects.nonNull(inventory.getLocationId()) &&
                Objects.isNull(inventory.getLocation())) {
            inventory.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(inventory.getLocationId()));
        }
        if (Objects.isNull(inventory.getLocation())) {
            return result.fail("Fail to get location information of the inventory with id " + inventory.getId() +
                    " of LPN " + inventory.getLpn() );
        }
        result.setLocationName(inventory.getLocation().getName());

        if (!Boolean.TRUE.equals(inventory.getLocation().getEnabled())) {

            return result.fail("The inventory with id " + inventory.getId() +
                    " of LPN " + inventory.getLpn() + " is in a disabled location " + inventory.getLocation().getName());
        }
        if (!Boolean.TRUE.equals(inventory.getLocation().getLocationGroup().getPickable())) {

            return result.fail("The inventory with id " + inventory.getId() +
                    " of LPN " + inventory.getLpn() + " is in a NON pickable location group " + inventory.getLocation().getLocationGroup().getName());
        }
        if (!Boolean.TRUE.equals(inventory.getLocation().getLocationGroup().getLocationGroupType().getFourWallInventory())) {

            return result.fail("The inventory with id " + inventory.getId() +
                    " of LPN " + inventory.getLpn() + " is in a location group " +
                    inventory.getLocation().getLocationGroup().getName() + " that is not inside warehouse");
        }

        // let's check if we have enough quantity for the item in the locations

        if (result.getLocationInventoryQuantity() <= result.getLocationOpenPickQuantity()) {

            return result.fail("The inventory with id " + inventory.getId() +
                    " of LPN " + inventory.getLpn() + " is in a location " + inventory.getLocation().getName() +
                    " that has more open pick quantity than inventory quantity.  Nothing left to be allocated");
        }
        else if (result.getLocationInventoryQuantity() - inventory.getQuantity() <= result.getLocationOpenPickQuantity()) {

            return result.fail("The inventory with id " + inventory.getId() +
                    " of LPN " + inventory.getLpn() + " may be partially allocatable from the location " + inventory.getLocation().getName());
        }
        return result.succeed();

    }

    public List<InventoryAgingForBilling> getInventoryAgingForBilling(Long warehouseId, Long clientId,
                                                                      String billableCategory,
                                                                      ZonedDateTime startTime,
                                                                      ZonedDateTime endTime,
                                                                      Boolean includeDaysSinceInWarehouseForStorageFee) {
        logger.debug("start to get inventory aging for billing with ");
        logger.debug("> warehouse id: {}", warehouseId);
        logger.debug("> clientId: {}", clientId);
        logger.debug("> billableCategory: {}", billableCategory);
        logger.debug("> startTime: {}", startTime);
        logger.debug("> endTime: {}", endTime);
        logger.debug("> includeDaysSinceInWarehouseForStorageFee: {}", includeDaysSinceInWarehouseForStorageFee);
        // get inventory that is in the warehouse or already shipped
        List<Inventory> allInventory = findAll(warehouseId,
                null,
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
                null,
                null,
                null, null,
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
                null,
                null,
                null,
                true,
                null, false,
                null);

        logger.debug("start to calculate the {} inventories for billing category {} ",
                allInventory.size(), billableCategory);

        if (Objects.isNull(startTime)) {
            Instant minInstant = Instant.ofEpochMilli(Long.MIN_VALUE);
            startTime  = minInstant.atZone(ZoneOffset.UTC);
        }
        if (Objects.isNull(endTime)) {

            Instant maxInstant = Instant.ofEpochMilli(Long.MAX_VALUE);
            endTime = maxInstant.atZone(ZoneOffset.UTC);
        }
        return getInventoryAgingForBilling(warehouseId, allInventory, billableCategory,
                startTime, endTime, includeDaysSinceInWarehouseForStorageFee);
    }
    public List<InventoryAgingForBilling> getInventoryAgingForBilling(Long warehouseId,
                                                                      List<Inventory> allInventory,
                                                                      String billableCategory,
                                                                      ZonedDateTime startTime,
                                                                      ZonedDateTime endTime,
                                                                      Boolean includeDaysSinceInWarehouseForStorageFee) {
        switch (BillableCategory.valueOf(billableCategory)) {
            case STORAGE_FEE_BY_CASE_COUNT:
                return getInventoryAgingForBillingByCaseCount(warehouseId, allInventory,
                        startTime, endTime, includeDaysSinceInWarehouseForStorageFee);
            default:
                throw InventoryException.raiseException("calculate billing for category " + billableCategory + " is not supported at this moment");
        }
    }

    private List<InventoryAgingForBilling> getInventoryAgingForBillingByCaseCount(Long warehouseId,
                                                                                  List<Inventory> allInventory,
                                                                                  ZonedDateTime startTime,
                                                                                  ZonedDateTime endTime,
                                                                                  Boolean includeDaysSinceInWarehouseForStorageFee) {
        logger.debug("start to get inventory aging for billing by case quantity");
        List<InventoryAgingForBilling> inventoryAgingForBillings = new ArrayList<>();
        // we will convert the time to warehouse local time zone first and calculate the days
        // of the inventory in the warehouse
        WarehouseConfiguration warehouseConfiguration =
                warehouseLayoutServiceRestemplateClient.getWarehouseConfiguration(warehouseId);
        ZoneId warehouseTimeZone = Strings.isBlank(warehouseConfiguration.getTimeZone()) ?
                ZoneId.systemDefault() : ZoneId.of(warehouseConfiguration.getTimeZone());
        LocalDate startDateAtWarehouseTimeZone = startTime.withZoneSameInstant(warehouseTimeZone).toLocalDate();
        LocalDate endDateAtWarehouseTimeZone = endTime.withZoneSameInstant(warehouseTimeZone).toLocalDate();

        logger.debug("local time windows {} - {}", startDateAtWarehouseTimeZone, endDateAtWarehouseTimeZone);

        // let's filter out any inventory that is
        // 1. in warehouse after the end time
        // 2. shipped before the start time
        allInventory = allInventory.stream().filter(
                inventory -> Objects.nonNull(inventory.getInWarehouseDatetime())
        ).filter(
                inventory -> {
                    LocalDate inventoryInWarehouseDate = inventory.getInWarehouseDatetime().withZoneSameInstant(warehouseTimeZone).toLocalDate();
                    if (inventoryInWarehouseDate.isAfter(endDateAtWarehouseTimeZone)) {
                        // inventory is received after the time window
                        return false;
                    }
                    if (Objects.nonNull(inventory.getShippedDatetime())) {
                        // the inventory is already shipped, make sure it is not shipped before
                        // the time windows
                        LocalDate inventoryShippedDate = inventory.getShippedDatetime().withZoneSameInstant(warehouseTimeZone).toLocalDate();
                        if (inventoryShippedDate.isBefore(startDateAtWarehouseTimeZone)) {
                            return false;
                        }
                    }
                    return true;
                }
        ).collect(Collectors.toList());

        // for those inventory
        // write down the quantity and in warehouse days within the start time and end time


        logger.debug("after filter inventory with the time window, we still have {} inventory left",
                allInventory.size());
        // key: days in the warehouse
        // value: total quantity of case
        Map<Long, Long> daysInWarehouseWithCaseQuantityMap = new HashMap<>();

        allInventory.stream()
        .forEach(
                inventory -> {
                    // first: days In Warehouse of the inventory
                    // second: case quantity of the inventory
                    Pair<Long, Long> daysInWarehouseWithCaseQuantity =
                            getDaysInWarehouseWithCaseQuantity(inventory,
                                    startDateAtWarehouseTimeZone,
                                    endDateAtWarehouseTimeZone,
                                    includeDaysSinceInWarehouseForStorageFee,
                                    warehouseTimeZone);
                    if (Objects.nonNull(daysInWarehouseWithCaseQuantity)) {
                        Long caseQuantity = daysInWarehouseWithCaseQuantityMap.getOrDefault(daysInWarehouseWithCaseQuantity.getFirst(), 0l);
                        daysInWarehouseWithCaseQuantityMap.put(daysInWarehouseWithCaseQuantity.getFirst(),
                                caseQuantity + daysInWarehouseWithCaseQuantity.getSecond());
                    }
                }
        );

        logger.debug("after process, here's the result");

        daysInWarehouseWithCaseQuantityMap.entrySet().forEach(
                daysInWarehouseWithCaseQuantity -> {
                    logger.debug("days: {}, case quantities: {}",
                            daysInWarehouseWithCaseQuantity.getKey(),
                            daysInWarehouseWithCaseQuantity.getValue());

                    inventoryAgingForBillings.add(
                            new InventoryAgingForBilling(
                                    daysInWarehouseWithCaseQuantity.getKey(),
                                    daysInWarehouseWithCaseQuantity.getValue()
                            )
                    );
                }
        );
        return inventoryAgingForBillings;

    }

    /**
     * Check the days of the inventory in the warehouse along with the case quantity
     * @param inventory
     * @param startDate
     * @param endDate
     * @param includeDaysSinceInWarehouseForStorageFee
     * @param warehouseTimeZone
     * @return
     */
    private Pair<Long, Long> getDaysInWarehouseWithCaseQuantity(Inventory inventory,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate,
                                                                   Boolean includeDaysSinceInWarehouseForStorageFee,
                                                                   ZoneId warehouseTimeZone) {

        ItemUnitOfMeasure caseItemUnitOfMeasure = inventory.getItemPackageType().getCaseItemUnitOfMeasure();
        // do nothing if we can't get the case unit of measure
        if (Objects.isNull(caseItemUnitOfMeasure)) {
            return null;
        }
        LocalDate inventoryInWarehouseFirstDate = inventory.getInWarehouseDatetime().withZoneSameInstant(warehouseTimeZone).toLocalDate();

        // check the last day of the inventory in the warehouse
        // only if the inventory is already shipped
        LocalDate inventoryInWarehouseLastDate = endDate;
        if (Objects.nonNull(inventory.getShippedDatetime())) {
            inventoryInWarehouseLastDate = inventory.getShippedDatetime().withZoneSameInstant(warehouseTimeZone).toLocalDate();
        }

        long inWarehouseDays = 0;
        if (Boolean.TRUE.equals(includeDaysSinceInWarehouseForStorageFee)) {
            inWarehouseDays = ChronoUnit.DAYS.between(inventoryInWarehouseFirstDate, inventoryInWarehouseLastDate)  + 1;
        }
        else if (inventoryInWarehouseFirstDate.isBefore(startDate)){

            // inventory in warehouse before the time window
            inWarehouseDays = ChronoUnit.DAYS.between(startDate, inventoryInWarehouseLastDate) + 1;
        }
        else {

            // inventory in warehouse after the time window
            inWarehouseDays = ChronoUnit.DAYS.between(inventoryInWarehouseFirstDate, inventoryInWarehouseLastDate)  + 1;
        }

        // get the case quantity of the inventory
        Long caseQuantity = (long)Math.ceil(inventory.getQuantity() * 1.0 / caseItemUnitOfMeasure.getQuantity());

        return Pair.of(inWarehouseDays, caseQuantity);

    }

    /**
     * Mark the inventory as shipped(usually for outbound orders) and move it to the
     * designate location
     * @param id
     * @param location
     * @return
     */
    public Inventory shipInventory(long id, Location location) {
        // move the inventory to the designate location

        Inventory inventory = moveInventory(id, location , null, true, null);
        // update the inventory's shipped date
        inventory.setShippedDatetime(ZonedDateTime.now());

        return saveOrUpdate(inventory);
    }

    public void compress(List<Inventory> inventories) {
        for (Inventory inventory : inventories) {
            inventory.setItemId(inventory.getItem().getId());
            inventory.setItemName(inventory.getItem().getName());

            inventory.setItemtemPackageTypeId(inventory.getItemPackageType().getId());
            inventory.setItemPackageTypeName(inventory.getItemPackageType().getName());

            // set the item and item package type to null to reduce the network traffic
            // in case the enduser need those information , they will need to make another
            // request
            inventory.setItem(null);
            inventory.setItemPackageType(null);
        }
    }


    public List<QCInspectionRequest> generateManualQCInspectionRequests(Long warehouseId, Long inventoryId) {
        Inventory inventory = findById(inventoryId);

        logger.debug("Start to generate manual QC inspection request for inventory {} / {}",
                inventory.getId(), inventory.getLpn());

        // let's setup the item family and inventory status first, just in case
        // we may need to compare

        return qcInspectionRequestService.generateManualQCInspectionRequests(inventory);
    }

}