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
import com.garyzhangscm.cwms.outbound.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.PickingException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.BulkPickRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class BulkPickService {
    private static final Logger logger = LoggerFactory.getLogger(BulkPickService.class);


    @Autowired
    private BulkPickRepository bulkPickRepository;
    @Autowired
    private PickService pickService;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private PickReleaseService pickReleaseService;
    @Autowired
    private OrderActivityService orderActivityService;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;


    public BulkPick findById(Long id) {
        return findById(id, true);

    }
    public BulkPick findById(Long id,
                             boolean loadDetails) {
        BulkPick bulkPick =
                bulkPickRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("bulk picking not found by id: " + id));

        if (loadDetails) {

            loadAttribute(bulkPick);
        }
        return bulkPick;
    }

    public List<BulkPick> findAll(Long warehouseId,
                                  String pickType,
                                  String number,
                                  String numberList,
                                  Long waveId,
                                  String waveNumber,
                                  Long itemId,
                                  Long clientId,
                                  String itemNumber,
                                  Long sourceLocationId,
                                  String sourceLocationName,
                                  Long inventoryStatusId,
                                  Boolean openPickOnly,
                                  String color,
                                  String style,
                                  String productSize) {
        return findAll(
                warehouseId, pickType,
                number, numberList,
                waveId,
                waveNumber, itemId,
                clientId, itemNumber,
                sourceLocationId, sourceLocationName,
                inventoryStatusId, openPickOnly,
                color, style, productSize,
                true
        );
    }
    public List<BulkPick> findAll(Long warehouseId,
                                  String pickType,
                                  String number,
                                  String numberList,
                                  Long waveId,
                                  String waveNumber,
                                  Long itemId,
                                  Long clientId,
                                  String itemNumber,
                                  Long sourceLocationId,
                                  String sourceLocationName,
                                  Long inventoryStatusId,
                                  Boolean openPickOnly,
                                  String color,
                                  String style,
                                  String productSize,
                                  boolean loadDetails) {
        List<BulkPick> bulkPicks =
                bulkPickRepository.findAll(
                (Root<BulkPick> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));


                    if (Strings.isNotBlank(pickType)) {
                        predicates.add(criteriaBuilder.equal(root.get("pickType"), PickType.valueOf(pickType)));
                    }
                    if (Strings.isNotBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));
                    }

                    if (StringUtils.isNotBlank(numberList)) {
                        CriteriaBuilder.In<String> inNumbers = criteriaBuilder.in(root.get("number"));
                        for(String bulkPickNumber : numberList.split(",")) {
                            inNumbers.value(bulkPickNumber);
                        }
                        predicates.add(criteriaBuilder.and(inNumbers));

                    }

                    if (Strings.isNotBlank(waveNumber)) {
                        predicates.add(criteriaBuilder.equal(root.get("waveNumber"), waveNumber));
                    }
                    if (Objects.nonNull(waveId)) {
                        predicates.add(criteriaBuilder.equal(root.get("waveId"), waveId));
                    }
                    if (Objects.nonNull(itemId)) {
                        predicates.add(criteriaBuilder.equal(root.get("itemId"), itemId));
                    }
                    if (Strings.isNotBlank(itemNumber)) {
                        Item item = inventoryServiceRestemplateClient.getItemByName(warehouseId, clientId, itemNumber);
                        predicates.add(criteriaBuilder.equal(root.get("itemId"), item.getId()));
                    }
                    if (Objects.nonNull(inventoryStatusId)) {
                        predicates.add(criteriaBuilder.equal(root.get("inventoryStatusId"), inventoryStatusId));
                    }

                    if (Objects.nonNull(sourceLocationId)) {
                        predicates.add(criteriaBuilder.equal(root.get("sourceLocationId"), sourceLocationId));
                    }
                    if (Strings.isNotBlank(sourceLocationName)) {
                        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                                warehouseId, sourceLocationName
                        );
                        predicates.add(criteriaBuilder.equal(root.get("sourceLocationId"), location.getId()));
                    }
                    if (Boolean.TRUE.equals(openPickOnly)) {

                        predicates.add(criteriaBuilder.greaterThan(root.get("quantity"), root.get("pickedQuantity")));
                    }

                    if (Strings.isNotBlank(color)) {
                        predicates.add(criteriaBuilder.equal(root.get("color"), color));
                    }
                    if (Strings.isNotBlank(productSize)) {
                        predicates.add(criteriaBuilder.equal(root.get("productSize"), productSize));
                    }
                    if (Strings.isNotBlank(style)) {
                        predicates.add(criteriaBuilder.equal(root.get("style"), style));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (bulkPicks.size() > 0 && loadDetails) {
            loadAttribute(bulkPicks);
        }
        return bulkPicks;
    }

    public void loadAttribute(List<BulkPick> bulkPicks) {
        // load the attribute for the picks in the bulk
        bulkPicks.forEach(
                bulkPick -> loadAttribute(bulkPick)
        );
    }

    public void loadAttribute(BulkPick bulkPick) {
        // Load the details for client and supplier informaiton
        if (bulkPick.getSourceLocationId() != null && bulkPick.getSourceLocation() == null) {
            bulkPick.setSourceLocation(warehouseLayoutServiceRestemplateClient.getLocationById(bulkPick.getSourceLocationId()));
        }

        // Load the item and inventory status information for each lines
        if (bulkPick.getItemId() != null && bulkPick.getItem() == null) {
            bulkPick.setItem(inventoryServiceRestemplateClient.getItemById(bulkPick.getItemId()));
        }

        // load pick's inventory status for
        if (bulkPick.getInventoryStatusId() != null &&
                bulkPick.getInventoryStatus() == null) {
            bulkPick.setInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(
                            bulkPick.getInventoryStatusId()
                    ));
        }


        if (Objects.nonNull(bulkPick.getWorkTaskId()) &&
                Objects.isNull(bulkPick.getWorkTask())) {
            bulkPick.setWorkTask(
                    resourceServiceRestemplateClient.getWorkTaskById(
                            bulkPick.getWarehouseId(), bulkPick.getWorkTaskId())
            );
        }

        // load the attribute for the picks in the bulk
        pickService.loadAttribute(bulkPick.getPicks());
    }

    public BulkPick findByNumber(Long warehouseId, String number) {
        return bulkPickRepository.findByWarehouseIdAndNumber(
                warehouseId, number
        );
    }

    public BulkPick save(BulkPick bulkPick) {
        return bulkPickRepository.save(bulkPick);
    }

    public BulkPick saveOrUpdate(BulkPick bulkPick) {
        if(Objects.isNull(bulkPick.getId()) &&
            Objects.nonNull(findByNumber(bulkPick.getWarehouseId(), bulkPick.getNumber()))) {
            bulkPick.setId(
                    findByNumber(bulkPick.getWarehouseId(), bulkPick.getNumber()).getId()
            );
        }
        return save(bulkPick);
    }

    public void delete(BulkPick bulkPick) {
        bulkPickRepository.delete(bulkPick);
    }

    public List<BulkPick> getBulkPickByLocation(Long warehouseId,
                                                Long locationId,
                                                Long itemId,
                                                Long inventoryStatusId,
                                                String color,
                                                String style,
                                                String productSize) {
        return findAll(warehouseId,
                null,
                null,
                null,
                null,
                null,
                itemId,
                null,
                null,
                locationId,
                null,
                inventoryStatusId,
                true,
                color, style, productSize);
    }

    /**
     * Group the picks from the allocation result into bulk pick and setup the pick's
     * bulk pick id
     * @param allocationResults
     */
    public void groupPicksIntoBulk(String waveNumber,
                                   List<AllocationResult> allocationResults,
                                   Sort.Direction direction) {

        // we will save the relationship between the pick and its allocation
        // result in the map, in case we may need to split the pick during
        // bulk pick process, then we can add the new pick into the same
        // allocation result
        // key: pick number
        // value: its allocation result
        Map<String, AllocationResult> allocationResultMap = new HashMap<>();

        List<Pick> pickCandidates = allocationResults.stream().map(
                allocationResult -> {
                    List<Pick> picks = allocationResult.getPicks();

                    picks.forEach(
                            pick -> allocationResultMap.put(pick.getNumber(), allocationResult)
                    );
                    return picks;
                }
        ).flatMap(List::stream)
                .filter(
                        // only grouping picks that is not in any group
                        // and no one is working on the list
                        pick -> Objects.isNull(pick.getBulkPick()) &&
                                Objects.isNull(pick.getCartonization()) &&
                                Objects.isNull(pick.getPickList()) &&
                                Objects.isNull(pick.getWorkTaskId()) &&
                                Strings.isBlank(pick.getLpn()) &&
                                pick.getPickedQuantity() == 0 &&
                                pick.getStatus() == PickStatus.PENDING
                ).collect(Collectors.toList());

        groupPicksIntoBulk(waveNumber, pickCandidates, direction, allocationResultMap);
    }

    /**
     * Group picks into bulk only if
     * 1. the picks are from the same location , for the same item
     * 2. The total quantity exceed the LPN's quantity
     * we may need to split the
     * @param picks
     * @param allocationResultMap a map saved the relationship between pick and its allocation result
     *                            if we need to split the pick during bulk pick process, we will need the
     *                            map to get the original allocation result and add the new pick into the
     *                            allocation result
     */
    public void groupPicksIntoBulk(String waveNumber,
                                   List<Pick> picks,
                                   Sort.Direction direction,
                                   Map<String, AllocationResult> allocationResultMap) {

        // group the picks by source location and item
        // key: source location id # item id # all inventory attribute
        // value: a list picks
        Map<String, List<Pick>> pickCandidateMap = new HashMap<>();

        // map to save the inventory and item information
        // we will need to get the information to make sure
        // we are good to generate bulk pick
        // right now we will only allow to generate bulk pick when
        // 1.  the location is not mixed with different item package type
        // 2. the total quantity is more than a LPN quantity
        // key: source location id # item id # all inventory attribute
        // value: a list inventory that meet the pick's request
        Map<String, List<Inventory>> pickableInventoryMap = new HashMap<>();

        if (direction.isAscending()) {
            picks.sort(Comparator.comparing(Pick::getQuantity));
        }
        else {
            picks.sort(Comparator.comparing(Pick::getQuantity).reversed());
        }

        picks.forEach(
                pick -> {
                    String key =
                            new StringBuilder().append(pick.getSourceLocationId())
                                    .append("#").append(pick.getItemId())
                                    .append("#").append(pick.getInventoryStatusId())
                                    .append("#").append((Strings.isBlank(pick.getColor()) ? "----" : pick.getColor()))
                                    .append("#").append((Strings.isBlank(pick.getStyle()) ? "----" : pick.getStyle()))
                                    .append("#").append((Strings.isBlank(pick.getProductSize()) ? "----" : pick.getProductSize()))
                            .toString();
                    List<Inventory> pickableInventory = null;
                    if (pickableInventoryMap.containsKey(key)) {
                        pickableInventory = pickableInventoryMap.get(key);
                    }
                    else {
                        pickableInventory = getPickableInventory(pick);
                        pickableInventoryMap.put(
                                key,pickableInventory
                        );
                    }
                    // check pick is ready for bulk pick
                    if (isPickReadyForBulkPick(pick, pickableInventory)) {

                        List<Pick> pickCandidates = pickCandidateMap.getOrDefault(key,
                                new ArrayList<>());
                        pickCandidates.add(pick);
                        pickCandidateMap.put(key, pickCandidates);
                    }
                }
        );

        // ok, if we are here, we know we have the candidate for bulk pick and
        // their candidate pickable inventory
        // let's see if we can start build the bulk pick
        pickCandidateMap.entrySet().forEach(
                entry -> {

                    String key = entry.getKey();
                    logger.debug("start to process picks with key {}",
                            key);
                    List<Inventory> pickableInventory = pickableInventoryMap.get(key);
                    List<Pick> pickCandidates = entry.getValue();

                    setupBulkPick(waveNumber, pickableInventory, pickCandidates, allocationResultMap);
                    logger.debug("complete processing picks with key {}",
                            key);

                }
        );


    }

    /**
     * Setup bulk pick based on the pick candidate and pickable inventory
     * @param pickableInventory
     * @param pickCandidates
     */
    private void setupBulkPick(String waveNumber,
                               List<Inventory> pickableInventory,
                               List<Pick> pickCandidates,
                               Map<String, AllocationResult> allocationResultMap) {

        // get the bulk pick unit of measure
        ItemUnitOfMeasure bulkPickUnitOfMeasure
                = getBulkPickUnitOfMeasure(pickableInventory);
        if (Objects.isNull(bulkPickUnitOfMeasure)) {
            return;
        }

        // convert the inventory to a LPN and quantity map
        // key: LPN
        // value: quantity of the LPN
        Map<String, Long> lpnWithQuantityMap = new HashMap<>();

        pickableInventory.forEach(
                inventory -> {
                    Long existingQuantity = lpnWithQuantityMap.getOrDefault(
                            inventory.getLpn(), 0l
                    );
                    lpnWithQuantityMap.put(
                            inventory.getLpn(),
                            existingQuantity + inventory.getQuantity()
                    );
                }
        );
        logger.debug("we will bulk pick from the inventory candidate");
        lpnWithQuantityMap.entrySet().forEach(
                entry -> logger.debug("lpn: {}, quantity: {}",
                        entry.getKey(), entry.getValue())
        );
        // a temp container to temporary hold the picks for bulk
        List<Pick> picksForCurrentBulkGroup = new ArrayList<>();
        Long totalPickQuantity = 0l;
        for (Pick pick : pickCandidates) {
            totalPickQuantity += pick.getQuantity();
            picksForCurrentBulkGroup.add(pick);
            logger.debug("start to process pick {} for bulk pick",
                    pick.getNumber());
            logger.debug("after adding current pick's quantity {}, the total quantity is {}",
                    pick.getQuantity(), totalPickQuantity);

            // see if we have any LPN that has enough quantity
            // 1. exact match
            // 2. pick quantity is more than lpn quantity so we
            //    will split
            String matchedLPN = "";
            for(Map.Entry<String, Long> lpnWithQuantityEntry : lpnWithQuantityMap.entrySet()) {
                if (lpnWithQuantityEntry.getValue().equals(totalPickQuantity)) {
                    matchedLPN = lpnWithQuantityEntry.getKey();
                    break;
                }
            }

            logger.debug("Do have find an exact match with quantity {}? {}",
                    totalPickQuantity,
                    Strings.isBlank(matchedLPN) ? "N/A" : matchedLPN);

            if (Strings.isBlank(matchedLPN)) {
                // no exact match, get LPN
                for(Map.Entry<String, Long> lpnWithQuantityEntry : lpnWithQuantityMap.entrySet()) {
                    if (lpnWithQuantityEntry.getValue() < totalPickQuantity) {
                        matchedLPN = lpnWithQuantityEntry.getKey();

                        logger.debug("we found a LPN that has more quantity ({}) than the required quantity {}? {}",
                                lpnWithQuantityEntry.getValue(),
                                totalPickQuantity,
                                matchedLPN);
                        break;
                    }
                }
            }

            if (Strings.isBlank(matchedLPN)) {
                // if we still can't find the matched LPN, continue with next pick
                continue;
            }
            Long lpnQuantity = lpnWithQuantityMap.get(matchedLPN);

            if (lpnQuantity.equals(totalPickQuantity)) {
                // OK, we found a LPN for all the picks in the list
                BulkPick bulkPick = createBulkPick(waveNumber, picksForCurrentBulkGroup, bulkPickUnitOfMeasure);
                // attach the picks to the bulk pick
                bulkPick.getPicks().forEach(
                        pickByBulk -> {
                            logger.debug("1. set pick {} / {}'s bulk pick to {}",
                                    pickByBulk.getId(),
                                    pickByBulk.getNumber(),
                                    bulkPick.getNumber());
                            pickByBulk.setBulkPick(bulkPick);
                        }
                );
                saveOrUpdate(bulkPick);
                releaseBulkPick(bulkPick);
                logger.debug("Bulk pick {} / {} saved!",
                        bulkPick.getId(), bulkPick.getNumber());

                // clear the pick list
                totalPickQuantity = 0l;
                picksForCurrentBulkGroup = new ArrayList<>();

                // once we found the match, let's remove the LPN from the map so that the remaining
                // picks will only look at the remaining LPNs
                lpnWithQuantityMap.remove(matchedLPN);
            }
            else if (totalPickQuantity > lpnQuantity){
                // OK, we found a LPN for all the picks in the list
                BulkPick bulkPick = createBulkPick(waveNumber, picksForCurrentBulkGroup, lpnQuantity,
                        bulkPickUnitOfMeasure);

                // attach the picks to the bulk pick
                bulkPick.getPicks().forEach(
                        pickByBulk -> {
                            logger.debug("2. set pick {} / {}'s bulk pick to {}",
                                    pickByBulk.getId(),
                                    pickByBulk.getNumber(),
                                    bulkPick.getNumber());
                            pickByBulk.setBulkPick(bulkPick);
                        }
                );
                saveOrUpdate(bulkPick);
                releaseBulkPick(bulkPick);

                // split the remaining quantity into a new pick,
                // add the pick back to the list
                // and then add the pick back to the allocation result
                Pick newPick = splitPickForBulkPick(pick, totalPickQuantity - lpnQuantity);
                AllocationResult allocationResult =  allocationResultMap.get(pick.getNumber());
                if (Objects.nonNull(allocationResult)) {
                    allocationResult.addPick(newPick);
                }
                // reset the quantity and list
                totalPickQuantity = newPick.getQuantity();
                picksForCurrentBulkGroup = new ArrayList<>();
                picksForCurrentBulkGroup.add(newPick);

                // once we found the match, let's remove the LPN from the map so that the remaining
                // picks will only look at the remaining LPNs
                lpnWithQuantityMap.remove(matchedLPN);
            }

        }

    }

    private Pick splitPickForBulkPick(Pick pick, long newPickQuantity) {

        // reduce the original pick's quantity
        pick.setQuantity(pick.getQuantity() - newPickQuantity);
        pickService.saveOrUpdate(pick, false);

        Pick newPick = pick.clone();
        newPick.setNumber(pickService.getNextPickNumber(pick.getWarehouseId()));
        newPick.setPickedQuantity(0l);
        newPick.setQuantity(newPickQuantity);
        newPick.setBulkPick(null);

        return pickService.saveOrUpdate(newPick);

    }

    private BulkPick createBulkPick(String waveNumber,
                                    List<Pick> picksForCurrentBulkGroup,
                                    ItemUnitOfMeasure bulkPickItemUnitOfMeasure) {
        Long bulkPickQuantity = picksForCurrentBulkGroup.stream()
                .map(Pick::getQuantity).mapToLong(Long::longValue).sum();
        return createBulkPick(waveNumber, picksForCurrentBulkGroup, bulkPickQuantity,
                bulkPickItemUnitOfMeasure);
    }
    private BulkPick createBulkPick(String waveNumber,
                                    List<Pick> picksForCurrentBulkGroup,
                                    Long bulkPickQuantity,
                                    ItemUnitOfMeasure bulkPickItemUnitOfMeasure) {
        BulkPick bulkPick = new BulkPick();
        // the picks that can be group into same bulk should have
        // the same attribute and we already verify it
        Pick pick = picksForCurrentBulkGroup.get(0);
        bulkPick.setWarehouseId(pick.getWarehouseId());
        bulkPick.setNumber(getNextNumber(pick.getWarehouseId()));
        bulkPick.setWaveNumber(waveNumber);
        bulkPick.setSourceLocationId(pick.getSourceLocationId());
        bulkPick.setItemId(pick.getItemId());
        bulkPick.setPicks(picksForCurrentBulkGroup);
        bulkPick.setQuantity(bulkPickQuantity);
        bulkPick.setPickedQuantity(0l);
        // release the bulk pick by default
        bulkPick.setStatus(PickStatus.RELEASED);
        bulkPick.setInventoryStatusId(pick.getInventoryStatusId());
        bulkPick.setUnitOfMeasureId(bulkPickItemUnitOfMeasure.getUnitOfMeasureId());

        bulkPick.setConfirmLocationCodeFlag(pick.isConfirmLocationCodeFlag());
        bulkPick.setConfirmItemFlag(pick.isConfirmItemFlag());
        bulkPick.setConfirmLocationFlag(pick.isConfirmLocationFlag());
        bulkPick.setConfirmLpnFlag(pick.isConfirmLpnFlag());

        bulkPick.setColor(pick.getColor());
        bulkPick.setStyle(pick.getStyle());
        bulkPick.setProductSize(pick.getProductSize());

        return bulkPick;
    }

    public String getNextNumber(Long warehouseId) {

        return commonServiceRestemplateClient.getNextNumber(warehouseId, "bulk-pick-number");
    }

    private ItemUnitOfMeasure getBulkPickUnitOfMeasure(List<Inventory> pickableInventory) {

        // make sure we only have one item package type
        List<ItemPackageType> itemPackageTypes
                = pickableInventory.stream().map(
                inventory -> inventory.getItemPackageType()
        ).distinct().collect(Collectors.toList());
        if (itemPackageTypes.size() > 1) {
            return null;
        }
        else if (itemPackageTypes.size() == 0) {
            return null;
        }

        return itemPackageTypes.get(0).getTrackingLpnUOM();
    }
    /**
     * Check if the pick is good candidate for bulk pick only if
     * 1. the location is not mixed with different item package type
     * 2. The pick's UOM is less than the item's LPN UOM
     * 3. The pick's quantity is less than the LPN's quantity
     * @param pick
     * @param pickableInventory
     * @return
     */
    private boolean isPickReadyForBulkPick(Pick pick, List<Inventory> pickableInventory) {
        // make sure we only have one item package type
        List<ItemPackageType> itemPackageTypes
                = pickableInventory.stream().map(
                        inventory -> inventory.getItemPackageType()
        ).distinct().collect(Collectors.toList());

        if (itemPackageTypes.size() > 1) {
            logger.debug("Skip Bulk pick for pick {} as the location {} is mixed of item package type for item {}",
                    pick.getNumber(),
                    pick.getSourceLocationId(),
                    pick.getItemId());
            return false;
        }
        else if (itemPackageTypes.size() == 0) {
            logger.debug("Skip Bulk pick for pick {} as we can't find any item package type for item {} from location {}",
                    pick.getNumber(),
                    pick.getItemId(),
                    pick.getSourceLocationId());
            return false;
        }

        ItemUnitOfMeasure lpnItemUnitOfMeasure = itemPackageTypes.get(0).getTrackingLpnUOM();
        if (Objects.isNull(lpnItemUnitOfMeasure)) {
            logger.debug("skip the bulk pick as there's no LPN item UOM defined for the item {}, package type {}",
                    pick.getItemId(),
                    itemPackageTypes.get(0).getName());
            return false;
        }
        if (pick.getUnitOfMeasureId() == lpnItemUnitOfMeasure.getUnitOfMeasureId()) {
            logger.debug("Skip the bulk pick for pick {} as the pick already pick at the LPN UOM {}",
                    pick.getNumber(),
                    lpnItemUnitOfMeasure.getUnitOfMeasure().getName());
        }
        if (pick.getQuantity() >= lpnItemUnitOfMeasure.getQuantity()) {
            logger.debug("Skip the bulk pick for pick {} as the pick quantity {} is no less than the LPN's quantity {}",
                    pick.getNumber(),
                    pick.getQuantity(),
                    lpnItemUnitOfMeasure.getQuantity());

        }
        // see if we already have enough bulk pick from the location
        List<BulkPick> existingBulkPicks = getBulkPickByLocation(
                pick.getWarehouseId(),
                pick.getSourceLocationId(),
                pick.getItemId(),
                pick.getInventoryStatusId(),
                pick.getColor(),
                pick.getStyle(),
                pick.getProductSize()).stream().filter(
                        // only return the bulk pick that not started yet
                        bulkPick -> bulkPick.getPickedQuantity() == 0
        ).collect(Collectors.toList());

        logger.debug("We have {} existing bulk pick already in the location {}",
                existingBulkPicks.size(),
                pick.getSourceLocationId());

        // make sure we can still have enough bulk pick
        // map of inventory with quantity that allow Bulk pick
        // key: LPN
        // value: quantity
        Map<String, Long> lpnWithQuantityMap = new HashMap<>();
        pickableInventory.forEach(
                inventory -> {
                    Long existingQuantity = lpnWithQuantityMap.getOrDefault(
                            inventory.getLpn(), 0l
                    );
                    lpnWithQuantityMap.put(
                            inventory.getLpn(),
                            existingQuantity + inventory.getQuantity()
                    );
                }
        );

        Iterator<BulkPick> bulkPickIterator = existingBulkPicks.listIterator();
        // round 1: remove the exact match LPN
        while (bulkPickIterator.hasNext()) {
            BulkPick bulkPick = bulkPickIterator.next();
            String matchedLPN = lpnWithQuantityMap.entrySet().stream()
                    .filter(entry ->  entry.getValue().equals(bulkPick.getQuantity()))
                    .map(entry -> entry.getKey()).findFirst().orElse("");
            if (Strings.isNotBlank(matchedLPN)) {
                // ok we find an LPN that can exactly match with the existing bulk pick
                // let's move the LPN and bulk pick from both container
                bulkPickIterator.remove();
                lpnWithQuantityMap.remove(matchedLPN);
                logger.debug("we will need to match existing bulk picking {} with LPN {} as the quanties matches",
                        bulkPick.getNumber(),
                        matchedLPN,
                        bulkPick.getQuantity());
            }
        }
        // for LPN that still left, see if the quantity is more than
        // the item's LPN quantity that we can bulk pick at
        return lpnWithQuantityMap.entrySet().stream()
                .anyMatch(entry -> entry.getValue() >= lpnItemUnitOfMeasure.getQuantity());


    }

    private List<Inventory> getPickableInventory(Pick pick) {
        return inventoryServiceRestemplateClient.getPickableInventory(
                        pick.getItemId(),
                        pick.getInventoryStatusId(),
                        pick.getSourceLocationId(),
                        null,
                        pick.getColor(),
                        pick.getProductSize(),
                        pick.getStyle(),
                pick.getAllocateByReceiptNumber());
    }

    @Transactional
    public void cancelPick(Long id, Boolean errorLocation, Boolean generateCycleCount) {
        BulkPick bulkPick = findById(id);
        // let's cancel all the picks in this bulk
        // if we will need to generate the cycle count, then we will generate one
        // cycle count no matter how many picks in this bulk
        for (Pick pick : bulkPick.getPicks()) {
            pickService.cancelPick(pick, false, false);
        }

        if (errorLocation) {
            warehouseLayoutServiceRestemplateClient.errorLocation(
                    bulkPick.getWarehouseId(), bulkPick.getSourceLocationId());
        }
        if (generateCycleCount) {

            Location location = Objects.nonNull(bulkPick.getSourceLocation()) ?
                    bulkPick.getSourceLocation() :
                    Objects.nonNull(bulkPick.getSourceLocationId()) ?
                            warehouseLayoutServiceRestemplateClient.getLocationById(bulkPick.getSourceLocationId()) : null;

            if (Objects.nonNull(location)) {

                inventoryServiceRestemplateClient.generateCycleCount(
                        bulkPick.getWarehouseId(), location.getName()
                );
            }
        }
        // if there's already a work task released for this bulk pick, then
        // remove the work task as well
        if (Objects.nonNull(bulkPick.getWorkTaskId())) {
            resourceServiceRestemplateClient.cancelWorkTaskById(
                    bulkPick.getWarehouseId(),
                    bulkPick.getWorkTaskId()
            );
        }

        delete(bulkPick);
    }

    public BulkPick changePick(BulkPick bulkPick) {
        return saveOrUpdate(bulkPick);
    }

    public BulkPick confirmBulkPick(Long id, Long nextLocationId,
                            String nextLocationName,  String lpn) {

        BulkPick bulkPick = findById(id);

        if (Objects.nonNull(nextLocationId)) {
            Location nextLocation = warehouseLayoutServiceRestemplateClient.getLocationById(nextLocationId);
            if (Objects.nonNull(nextLocation)) {
                return confirmBulkPick(bulkPick, nextLocation, lpn);
            }
            else {
                throw PickingException.raiseException(
                        "Can't confirm bulk the pick to destination location with id: " + nextLocationId + ", The id is an invalid location id");
            }
        }
        else if (StringUtils.isNotBlank(nextLocationName)) {
            Location nextLocation = warehouseLayoutServiceRestemplateClient.getLocationByName(
                    bulkPick.getWarehouseId(), nextLocationName);
            if (Objects.nonNull(nextLocation)) {
                return confirmBulkPick(bulkPick, nextLocation, lpn);
            }
            else {
                logger.debug("Can't confirm the bulk pick to destination location with id: " + nextLocationId + ", The id is an invalid location id");
                throw PickingException.raiseException(
                        "Can't confirm the bulk pick to destination location with id: " + nextLocationId + ", The id is an invalid location id");
            }
        }
        else {
            throw PickingException.raiseException(
                    "Can't confirm the bulk pick as there's no destination location information passed in");
        }
    }

    public BulkPick assignToUser(Long id, Long warehouseId, Long userId) {
        BulkPick bulkPick = findById(id);
        // the bulk pick has to be released and
        // there's already work task id attached to it
        if (!bulkPick.getStatus().equals(PickStatus.RELEASED)) {
            throw PickingException.raiseException("You can only assign user to" +
                    " released picks");
        }
        if (Objects.isNull(bulkPick.getWorkTaskId())) {

            throw PickingException.raiseException("You can only assign user to" +
                    " the bulk pick that already released into the work task");
        }

        resourceServiceRestemplateClient.assingUser(warehouseId,
                bulkPick.getWorkTaskId(), userId);

        return bulkPick;
    }

    public BulkPick unassignUser(Long id, Long warehouseId) {
        BulkPick bulkPick = findById(id);
        // the bulk pick has to be released and
        // there's already work task id attached to it
        if (Objects.isNull(bulkPick.getWorkTaskId())) {

            throw PickingException.raiseException("You can only unassign user to" +
                    " the bulk pick that already released into the work task");
        }

        resourceServiceRestemplateClient.unassingUser(warehouseId,
                bulkPick.getWorkTaskId());

        return bulkPick;
    }

    public void releaseBulkPick(BulkPick bulkPick) {

        saveOrUpdate(pickReleaseService.releaseBulkPick(bulkPick));
    }


    public BulkPick confirmBulkPick(BulkPick bulkPick, Location nextLocation, String lpn)   {

        logger.debug("==> Before the pick confirm, the destination location {} 's volume is {}",
                warehouseLayoutServiceRestemplateClient.getLocationById(nextLocation.getId()).getName(),
                warehouseLayoutServiceRestemplateClient.getLocationById(nextLocation.getId()).getCurrentVolume());


        // for bulk pick, we will need to pick the whole LPN. and make sure
        // the LPN quantity matches with bulk pick's quantity




        synchronized (lpn) {

            // we will use synchronized to prevent multiple users picking the same lpn from the
            // location;
            List<Inventory> pickableInventories = inventoryServiceRestemplateClient.getInventoryByLpn(bulkPick.getWarehouseId(), lpn);
            logger.debug(" Get {} valid inventory for bulk pick {}",
                    pickableInventories.size(), bulkPick.getNumber());
            Long inventoryQuantity = pickableInventories.stream().mapToLong(Inventory::getQuantity).sum();
            if (!inventoryQuantity.equals(bulkPick.getQuantity())) {

                throw PickingException.raiseException("Can't pick LPN " + lpn + " for the bulk pick " +
                        bulkPick.getNumber() + ", as the LPN's quantity " + inventoryQuantity +
                        " doesn't match with the bulk pick's requirement " + bulkPick.getQuantity());
            }

            if (pickableInventories.size() == 0) {
                throw PickingException.raiseException("There's no inventory available from location " +
                        (Objects.isNull(bulkPick.getSourceLocation()) ?
                                warehouseLayoutServiceRestemplateClient.getLocationById(bulkPick.getSourceLocationId()).getName() :
                                bulkPick.getSourceLocation().getName()) +
                        ", for item " +
                        (Objects.isNull(bulkPick.getItem()) ?
                                inventoryServiceRestemplateClient.getItemById(bulkPick.getItemId()) :
                                bulkPick.getItem().getName()));
            }
            // pickableInventories.stream().forEach(System.out::print);
            String errorMessage = match(pickableInventories, bulkPick);
            if (Strings.isNotBlank(errorMessage)) {

                throw PickingException.raiseException("Can't pick LPN " + lpn + " for the bulk pick " +
                        bulkPick.getNumber() + ", reason: " + errorMessage);
            }

            inventoryServiceRestemplateClient.processBulkPick(bulkPick, nextLocation, lpn);

            // set all the pick's quantity
            bulkPick.getPicks().forEach(
                    pick -> pick.setPickedQuantity(pick.getQuantity())
            );
            bulkPick.setPickedQuantity(bulkPick.getQuantity());


            logger.debug("==> after the pick confirm, the destination location {} 's volume is {}",
                    warehouseLayoutServiceRestemplateClient.getLocationById(nextLocation.getId()).getName(),
                    warehouseLayoutServiceRestemplateClient.getLocationById(nextLocation.getId()).getCurrentVolume());
        }

        // Get the latest pick information
        BulkPick newBulkPick = saveOrUpdate(bulkPick);

        // start the ord activity transaction
        OrderActivity orderActivity = orderActivityService.createOrderActivity(
                bulkPick.getWarehouseId(), newBulkPick, OrderActivityType.BULK_PICK_CONFIRM
        );
        orderActivityService.sendOrderActivity(orderActivity);
        // complete the work task if any
        if (Objects.nonNull(bulkPick.getWorkTaskId())) {
            resourceServiceRestemplateClient.completeWorkTask(
                    bulkPick.getWarehouseId(), bulkPick.getWorkTaskId()
            );
        }
        return newBulkPick;

    }

    /**
     * Check if the list of inventory can be picked by the bulk pick
     * 1. inventory has to have same item and inventory status and match
     *    with the pick
     * 2. for other attribute, if the bulk pick has requirement on the attribute
     *    then the list of inventory has to have the same value for the attribute
     * @param inventories
     * @param bulkPick
     * @return error message
     */
    private String match(List<Inventory> inventories, BulkPick bulkPick) {
        logger.debug("check if the inventory match with the pick");
        logger.debug("========            Inventory   ===========");
        Set<Long> itemIdSet = new HashSet<>();
        Set<Long> inventoryStatusIDSet = new HashSet<>();
        Set<String> colorSet = new HashSet<>();
        Set<String> productSizeSet = new HashSet<>();
        Set<String> styleSet = new HashSet<>();

        inventories.forEach(
                inventory -> {
                    itemIdSet.add(inventory.getItem().getId());
                    inventoryStatusIDSet.add(inventory.getInventoryStatus().getId());

                    if (Strings.isNotBlank(inventory.getColor())) {
                        colorSet.add(inventory.getColor());
                    }
                    if (Strings.isNotBlank(inventory.getProductSize())) {
                        productSizeSet.add(inventory.getProductSize());
                    }
                    if (Strings.isNotBlank(inventory.getStyle())) {
                        styleSet.add(inventory.getStyle());
                    }
                }
        );
        if (itemIdSet.size() != 1) {
            logger.debug("inventory is mixed with item, not allowed to be bulk pick");
            return "inventory is mixed with item, not allowed to be bulk pick";
        }
        if (inventoryStatusIDSet.size() != 1) {
            logger.debug("inventory is mixed with item, not allowed to be bulk pick");
            return "inventory is mixed with item, not allowed to be bulk pick";
        }
        Long itemId = itemIdSet.iterator().next();
        if (!bulkPick.getItemId().equals(itemId)) {

            logger.debug("inventory's item  {} doesn't match with the bulk pick's item {}",
                    itemId, bulkPick.getItemId());
            return "inventory's item id " + itemId + " doesn't match with the bulk pick's item id "
                    + bulkPick.getItemId();
        }

        Long inventoryStatusID = inventoryStatusIDSet.iterator().next();
        if (!bulkPick.getInventoryStatusId().equals(inventoryStatusID)) {
            logger.debug("inventory's status  {} doesn't match with the bulk pick's status {}",
                    inventoryStatusID, bulkPick.getInventoryStatusId());
            return "inventory's status  " + inventoryStatusID + " doesn't match with the bulk pick's status "
                    + bulkPick.getInventoryStatusId();
        }

        // if the bulk pick requires inventory with certain attribute, make sure
        // it matches with the inventory on the LPN
        if (Strings.isNotBlank(bulkPick.getColor())) {
            if (colorSet.size() != 1) {
                logger.debug("inventory is mixed with color, not allowed to be bulk pick");
                return "inventory is mixed with color, not allowed to be bulk pick";
            }
            String color = colorSet.iterator().next();

            if (!bulkPick.getColor().equalsIgnoreCase(color)) {
                logger.debug("inventory's color  {} doesn't match with the bulk pick's color {}",
                        color, bulkPick.getColor());
                return "inventory's color  " + color + " doesn't match with the bulk pick's color " + bulkPick.getColor();
            }
        }
        if (Strings.isNotBlank(bulkPick.getProductSize())) {
            if (productSizeSet.size() != 1) {
                logger.debug("inventory is mixed with production size, not allowed to be bulk pick");
                return "inventory is mixed with production size, not allowed to be bulk pick";
            }
            String productSize = productSizeSet.iterator().next();

            if (!bulkPick.getProductSize().equalsIgnoreCase(productSize)) {
                logger.debug("inventory's production size  {} doesn't match with the bulk pick's production size {}",
                        productSize, bulkPick.getProductSize());
                return "inventory's production size  " + productSize + " doesn't match with the bulk pick's production size " + bulkPick.getProductSize();
            }
        }
        if (Strings.isNotBlank(bulkPick.getStyle())) {
            if (styleSet.size() != 1) {
                logger.debug("inventory is mixed with production size, not allowed to be bulk pick");
                return "inventory is mixed with production size, not allowed to be bulk pick";
            }
            String style = styleSet.iterator().next();

            if (!bulkPick.getStyle().equalsIgnoreCase(style)) {
                logger.debug("inventory's style {} doesn't match with the bulk pick's style {}",
                        style, bulkPick.getProductSize());
                return "inventory's style " + style + " doesn't match with the bulk pick's style " +
                        bulkPick.getProductSize();
            }
        }
        return "";
    }

    /**
     * Release the pending bulk pick
     * @param id
     * @param warehouseId
     * @return
     */
    public BulkPick releasePick(Long id, Long warehouseId) {
        BulkPick bulkPick = findById(id);
        if (bulkPick.getStatus().equals(PickStatus.RELEASED)) {
            logger.debug("bulk {} is already released",
                    bulkPick.getNumber());
            return bulkPick;
        }
        if (!bulkPick.getStatus().equals(PickStatus.PENDING)) {
            throw PickingException.raiseException(
                    "bulk " + bulkPick.getNumber() + " is not in pending status"
                    );
        }
        return saveOrUpdate(pickReleaseService.releaseBulkPick(bulkPick));
    }
}
