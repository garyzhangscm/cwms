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
import com.garyzhangscm.cwms.inventory.clients.KafkaSender;
import com.garyzhangscm.cwms.inventory.clients.OutbuondServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventoryActivityRepository;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import javax.persistence.criteria.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
@Service
public class InventoryActivityService{
    private static final Logger logger = LoggerFactory.getLogger(InventoryActivityService.class);

    @Autowired
    private InventoryActivityRepository inventoryActivityRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private HttpSession httpSession;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;

    @Autowired
    HttpServletRequest httpServletRequest;

    @Autowired
    private KafkaSender kafkaSender;

    public InventoryActivity findById(Long id) {
        return findById(id, true);
    }
    public InventoryActivity findById(Long id, boolean includeDetails) {
        InventoryActivity inventoryActivity = inventoryActivityRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("inventory activity not found by id: " + id));
        if (includeDetails) {
            loadAttribute(inventoryActivity);
        }
        return inventoryActivity;
    }

    public List<InventoryActivity> findAll() {
        return findAll(true);
    }

    public List<InventoryActivity> findAll(boolean includeDetails) {

        // Only return actual inventory
        List<InventoryActivity> inventoryActivities = inventoryActivityRepository.findAll();
        if (includeDetails && inventoryActivities.size() > 0) {
            loadAttribute(inventoryActivities);
        }
        return inventoryActivities;
    }
    public List<InventoryActivity> findAll(Long warehouseId,
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
                                           String inventoryActivityType,
                                           String beginDateTime,
                                           String endDateTime,
                                           String date,
                                           String username) {
        return findAll(warehouseId, itemName, clientIds, itemFamilyIds, inventoryStatusId,
                locationName, locationId, locationGroupId, receiptId, pickIds, lpn,
                inventoryActivityType, beginDateTime, endDateTime, date, username,true);
    }


    public List<InventoryActivity> findAll(Long warehouseId,
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
                                           String inventoryActivityType,
                                           String beginDateTime,
                                           String endDateTime,
                                           String date,
                                           String username,
                                           boolean includeDetails) {
        List<InventoryActivity> inventoryActivities =  inventoryActivityRepository.findAll(
                (Root<InventoryActivity> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    if (!StringUtils.isBlank(itemName) || !StringUtils.isBlank(clientIds)) {
                        Join<Inventory, Item> joinItem = root.join("item", JoinType.INNER);
                        if (!itemName.isEmpty()) {
                            predicates.add(criteriaBuilder.equal(joinItem.get("name"), itemName));
                        }

                        if (StringUtils.isNotBlank(clientIds)) {
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

                    if (!StringUtils.isBlank(inventoryActivityType)) {

                        predicates.add(criteriaBuilder.equal(
                                root.get("inventoryActivityType"), InventoryActivityType.valueOf(inventoryActivityType)));

                    }

                    if (!StringUtils.isBlank(beginDateTime)) {
                        LocalDateTime begin = LocalDateTime.parse(beginDateTime);
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("activityDateTime"), begin));
                    }
                    if (!StringUtils.isBlank(endDateTime)) {
                        LocalDateTime end = LocalDateTime.parse(endDateTime);
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("activityDateTime"), end));
                    }

                    if (!StringUtils.isBlank(date)) {
                        LocalDateTime begin = LocalDate.parse(date).atStartOfDay();
                        LocalDateTime end = begin.plusDays(1).minusNanos(1);
                        predicates.add(criteriaBuilder.between(root.get("activityDateTime"), begin, end));
                    }

                    if (!StringUtils.isBlank(username)) {
                        predicates.add(criteriaBuilder.equal(root.get("username"), username));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (includeDetails && inventoryActivities.size() > 0) {
            loadAttribute(inventoryActivities);
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

            return inventoryActivities.stream().filter(inventoryActivity -> locationMap.containsKey(inventoryActivity.getLocationId())).collect(Collectors.toList());
        }
        return inventoryActivities;
    }




    public List<InventoryActivity> findByLpn(String lpn){
        return findByLpn(lpn,true);
    }
    public List<InventoryActivity> findByLpn(String lpn, boolean includeDetails){
        List<InventoryActivity> inventoryActivities = inventoryActivityRepository.findByLpn(lpn);
        if (!CollectionUtils.isEmpty(inventoryActivities) && includeDetails) {
            loadAttribute(inventoryActivities);
        }
        return inventoryActivities;
    }



    public InventoryActivity save(InventoryActivity inventoryActivity) {
        InventoryActivity savedInventoryActivity = inventoryActivityRepository.save(inventoryActivity);
        return savedInventoryActivity;
    }




    public void loadAttribute(List<InventoryActivity> inventoryActivities) {
        for(InventoryActivity inventoryActivity : inventoryActivities) {
            loadAttribute(inventoryActivity);
        }
    }

    public void loadAttribute(InventoryActivity inventoryActivity) {

        // Load location information
        if (inventoryActivity.getLocationId() != null) {
            inventoryActivity.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(inventoryActivity.getLocationId()));
        }



        // load the unit of measure details for the packate types
        inventoryActivity.getItemPackageType().getItemUnitOfMeasures().forEach(itemUnitOfMeasure ->
                itemUnitOfMeasure.setUnitOfMeasure(commonServiceRestemplateClient.getUnitOfMeasureById(itemUnitOfMeasure.getUnitOfMeasureId())));

        if (inventoryActivity.getPickId() != null) {
            inventoryActivity.setPick(outbuondServiceRestemplateClient.getPickById(inventoryActivity.getPickId()));
        }
    }

    public void logInventoryActivitiy(Inventory inventory, InventoryActivityType inventoryActivityType,
                                      LocalDateTime activityDateTime, String username,
                                      String valueType, String fromValue, String toValue,
                                      String documentNumber, String comment) {
        logger.debug("Start to construct the inventory activities");
        InventoryActivity inventoryActivity = new InventoryActivity(
                inventory, inventoryActivityType,
                getNextTransactionId(inventory.getWarehouseId()),
                getTransactionGroupId(inventory.getWarehouseId()),
                activityDateTime, username,
                valueType, fromValue, toValue,
                documentNumber, comment,
                getRFCode()
        );

        logger.debug("Will send the activity record to kafka");
        // we will raise an kafka message to make the persistence an asyncronized call
        kafkaSender.send(inventoryActivity);
    }
    public void logInventoryActivitiy(Inventory inventory, InventoryActivityType inventoryActivityType) {
        logInventoryActivitiy(inventory, inventoryActivityType,
                LocalDateTime.now(), userService.getCurrentUserName(),
                "", "", "", "", "");
    }

    public void logInventoryActivitiy(Inventory inventory, InventoryActivityType inventoryActivityType,
                                      String valueType, String fromValue, String toValue, String documentNumber) {
        logInventoryActivitiy(inventory, inventoryActivityType,
                LocalDateTime.now(), userService.getCurrentUserName(),
                valueType, fromValue, toValue, documentNumber, "");
    }

    public void logInventoryActivitiy(Inventory inventory, InventoryActivityType inventoryActivityType,
                                      String valueType, String fromValue, String toValue) {
        logInventoryActivitiy(inventory, inventoryActivityType,
                LocalDateTime.now(), userService.getCurrentUserName(),
                valueType, fromValue, toValue, "", "");
    }


    public void logInventoryActivitiy(Inventory inventory, InventoryActivityType inventoryActivityType,
                                       String valueType, String fromValue, String toValue,
                                      String documentNumber, String comment) {
        logInventoryActivitiy(inventory, inventoryActivityType,
                LocalDateTime.now(), userService.getCurrentUserName(),
                valueType, fromValue, toValue, documentNumber, comment);
    }

    public void processInventoryActivityMessage(InventoryActivity inventoryActivity){
        save(inventoryActivity);
    }

    private String getTransactionGroupId(Long warehouseId) {
        String transactionGroupId;
        if (Objects.isNull(httpSession.getAttribute("Inventory-Activity-Transaction-Id"))) {
            logger.debug("Current session doesn't have any transaction id yet, let's get a new one");
            transactionGroupId = commonServiceRestemplateClient.getNextInventoryActivityTransactionGroupId(warehouseId);
            httpSession.setAttribute("Inventory-Activity-Transaction-Id", transactionGroupId);
            logger.debug(">> {}", transactionGroupId);
        }
        else {
            transactionGroupId = httpSession.getAttribute("Inventory-Activity-Transaction-Id").toString();
            logger.debug("Get transaction ID {} from current session", transactionGroupId);
        }
        return transactionGroupId;
    }

    private String getNextTransactionId(Long warehouseId) {
        return commonServiceRestemplateClient.getNextInventoryActivityTransactionId(warehouseId);
    }

    private String getRFCode() {
        return httpServletRequest.getHeader("rfCode") ;
    }









}
