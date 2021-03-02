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
import com.garyzhangscm.cwms.inventory.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.MissingInformationException;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventoryAdjustmentThresholdRepository;
import com.garyzhangscm.cwms.inventory.repository.InventoryRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

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
public class InventoryAdjustmentThresholdService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(InventoryAdjustmentThresholdService.class);

    @Autowired
    private InventoryAdjustmentThresholdRepository inventoryAdjustmentThresholdRepository;

    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemFamilyService itemFamilyService;

    @Autowired
    private InventoryActivityService inventoryActivityService;

    @Autowired
    private UserService userService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.inventory_adjustment_threshold:inventory_adjustment_threshold}")
    String testDataFile;

    public InventoryAdjustmentThreshold findById(Long id) {
        return findById(id, true);
    }
    public InventoryAdjustmentThreshold findById(Long id, boolean includeDetails) {
        InventoryAdjustmentThreshold inventoryAdjustmentThreshold = inventoryAdjustmentThresholdRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("inventory adjustment threshold policy not found by id: " + id));
        if (includeDetails) {
            loadAttribute(inventoryAdjustmentThreshold);
        }
        return inventoryAdjustmentThreshold;
    }

    public List<InventoryAdjustmentThreshold> findAll() {
        return findAll(true);
    }

    public List<InventoryAdjustmentThreshold> findAll(boolean includeDetails) {

        // Only return actual inventory
        List<InventoryAdjustmentThreshold> inventoryAdjustmentThresholds = inventoryAdjustmentThresholdRepository.findAll();
        if (includeDetails && inventoryAdjustmentThresholds.size() > 0) {
            loadAttribute(inventoryAdjustmentThresholds);
        }
        return inventoryAdjustmentThresholds;
    }
    public List<InventoryAdjustmentThreshold> findAll(Long warehouseId,
                                                       String itemName,
                                                      Long clientId,
                                                      String itemFamilyName,
                                                      InventoryQuantityChangeType type,
                                                      Long userId,
                                                      Long roleId) {
        return findAll(warehouseId, itemName, clientId, null,  itemFamilyName, null , type, null,
                userId, roleId, true, true);
    }

    public List<InventoryAdjustmentThreshold> findAll(Long warehouseId,
                                                      String itemName,
                                                      String itemFamilyName,
                                                      String clientIds,
                                                      String itemFamilyIds,
                                                      String inventoryQuantityChangeTypes,
                                                      String username,
                                                      String roleName,
                                                      Boolean enabled) {
        Long userId = null;
        Long companyId = warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId).getCompanyId();
        if (StringUtils.isNotBlank(username)) {
            userId = resourceServiceRestemplateClient.getUserByUsername(companyId, username).getId();
        }
        Long roleId = null;
        if (StringUtils.isNotBlank(roleName)) {
            roleId = resourceServiceRestemplateClient.getUserByUsername(companyId, roleName).getId();
        }
        return findAll(warehouseId, itemName, null, clientIds, itemFamilyName,itemFamilyIds,
                null, inventoryQuantityChangeTypes, userId, roleId, enabled, true);

    }
    public List<InventoryAdjustmentThreshold> findAll(Long warehouseId) {
        return findAll(warehouseId, null, null, null, null,
                null, null, null,  null, null, null, true);
    }
    public List<InventoryAdjustmentThreshold> findAll(Long warehouseId,
                                                      String itemName,
                                                      Long clientId,
                                                      String clientIds,
                                                      String itemFamilyName,
                                                      String itemFamilyIds,
                                                      InventoryQuantityChangeType type,
                                                      String inventoryQuantityChangeTypes,
                                                      Long userId,
                                                      Long roleId,
                                                      Boolean enabled,
                                                        boolean includeDetails) {
        List<InventoryAdjustmentThreshold> inventoryAdjustmentThresholds =  inventoryAdjustmentThresholdRepository.findAll(
                (Root<InventoryAdjustmentThreshold> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {


                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(itemName)) {
                        Join<InventoryAdjustmentThreshold, Item> joinItem = root.join("item", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinItem.get("name"), itemName));
                    }
                    if (Objects.nonNull(clientId)) {
                        predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
                    }
                    else if (StringUtils.isNotBlank(clientIds)) {

                        CriteriaBuilder.In<Long> inClientIds = criteriaBuilder.in(root.get("clientId"));
                        for(String id : clientIds.split(",")) {
                            inClientIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inClientIds));

                    }
                    if (StringUtils.isNotBlank(itemFamilyName)) {
                        Join<InventoryAdjustmentThreshold, ItemFamily> joinItemFamily = root.join("itemFamily", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinItemFamily.get("name"), itemFamilyName));
                    }
                    else if (StringUtils.isNotBlank(itemFamilyIds)) {

                        Join<InventoryAdjustmentThreshold, ItemFamily> joinItemFamily = root.join("itemFamily", JoinType.INNER);

                        CriteriaBuilder.In<Long> inItemFamilyIds = criteriaBuilder.in(joinItemFamily.get("id"));
                        for(String id : itemFamilyIds.split(",")) {
                            inItemFamilyIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inItemFamilyIds));


                    }
                    if (Objects.nonNull(type)) {
                        predicates.add(criteriaBuilder.equal(root.get("type"), type));
                    }
                    else  if (StringUtils.isNotBlank(inventoryQuantityChangeTypes)) {

                        CriteriaBuilder.In<InventoryQuantityChangeType> inInventoryQuantityChangeTypes
                                = criteriaBuilder.in(root.get("type"));
                        for(String inInventoryQuantityChangeType : inventoryQuantityChangeTypes.split(",")) {
                            inInventoryQuantityChangeTypes.value(InventoryQuantityChangeType.valueOf(inInventoryQuantityChangeType));
                        }
                        predicates.add(criteriaBuilder.and(inInventoryQuantityChangeTypes));
                    }
                    if (Objects.nonNull(userId)) {
                        predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
                    }
                    if (Objects.nonNull(roleId)) {
                        predicates.add(criteriaBuilder.equal(root.get("roleId"), roleId));
                    }
                    if (Objects.nonNull(enabled)) {
                        predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
                    }



                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (includeDetails && inventoryAdjustmentThresholds.size() > 0) {
            loadAttribute(inventoryAdjustmentThresholds);
        }

        return inventoryAdjustmentThresholds;
    }


    public InventoryAdjustmentThreshold save(InventoryAdjustmentThreshold inventoryAdjustmentThreshold) {
        logger.debug("Will start to save InventoryAdjustmentThreshold: {}", inventoryAdjustmentThreshold);
        return inventoryAdjustmentThresholdRepository.save(inventoryAdjustmentThreshold);
    }

    public void delete(InventoryAdjustmentThreshold inventoryAdjustmentThreshold) {
        inventoryAdjustmentThresholdRepository.delete(inventoryAdjustmentThreshold);
    }
    public void delete(Long id) {
        inventoryAdjustmentThresholdRepository.deleteById(id);
    }
    public void delete(String inventoryAdjustmentThresholdIds) {
        if (!inventoryAdjustmentThresholdIds.isEmpty()) {
            long[] inventoryAdjustmentThresholdIdArray = Arrays.asList(inventoryAdjustmentThresholdIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : inventoryAdjustmentThresholdIdArray) {
                delete(id);
            }
        }
    }


    public void loadAttribute(List<InventoryAdjustmentThreshold> inventoryAdjustmentThresholds) {
        for(InventoryAdjustmentThreshold inventoryAdjustmentThreshold : inventoryAdjustmentThresholds) {
            loadAttribute(inventoryAdjustmentThreshold);
        }
    }

    public void loadAttribute(InventoryAdjustmentThreshold inventoryAdjustmentThreshold) {

        if (Objects.nonNull(inventoryAdjustmentThreshold.getClientId()) &&
                Objects.isNull(inventoryAdjustmentThreshold.getClient())) {
            inventoryAdjustmentThreshold.setClient(
                    commonServiceRestemplateClient.getClientById(inventoryAdjustmentThreshold.getClientId()));
        }

        if (Objects.nonNull(inventoryAdjustmentThreshold.getWarehouseId()) &&
                Objects.isNull(inventoryAdjustmentThreshold.getWarehouse())) {
            inventoryAdjustmentThreshold.setWarehouse(
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(inventoryAdjustmentThreshold.getWarehouseId()));
        }

        if (Objects.nonNull(inventoryAdjustmentThreshold.getUserId()) &&
                Objects.isNull(inventoryAdjustmentThreshold.getUser())) {
            inventoryAdjustmentThreshold.setUser(
                    resourceServiceRestemplateClient.getUserById(inventoryAdjustmentThreshold.getUserId()));
        }
        if (Objects.nonNull(inventoryAdjustmentThreshold.getRoleId()) &&
                Objects.isNull(inventoryAdjustmentThreshold.getRole())) {
            inventoryAdjustmentThreshold.setRole(
                    resourceServiceRestemplateClient.getRoleById(inventoryAdjustmentThreshold.getRoleId()));
        }





    }

    public List<InventoryAdjustmentThresholdCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("item").
                addColumn("client").
                addColumn("itemFamily").
                addColumn("warehouse").
                addColumn("type").
                addColumn("user").
                addColumn("role").
                addColumn("quantityThreshold").
                addColumn("costThreshold").
                addColumn("enabled").
                build().withHeader();
        return fileService.loadData(file, schema, InventoryAdjustmentThresholdCSVWrapper.class);
    }


    public List<InventoryAdjustmentThresholdCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("item").
                addColumn("client").
                addColumn("itemFamily").
                addColumn("warehouse").
                addColumn("type").
                addColumn("user").
                addColumn("role").
                addColumn("quantityThreshold").
                addColumn("costThreshold").
                addColumn("enabled").
                build().withHeader();

        return fileService.loadData(inputStream, schema, InventoryAdjustmentThresholdCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            logger.debug("Start to load data for inventory adjustment threshold from {}", testDataFileName);
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<InventoryAdjustmentThresholdCSVWrapper> inventoryAdjustmentThresholdCSVWrappers = loadData(inputStream);
            logger.debug("get {} line csv data", inventoryAdjustmentThresholdCSVWrappers.size());
            inventoryAdjustmentThresholdCSVWrappers.stream().forEach(inventoryAdjustmentThresholdCSVWrapper -> {
                save(convertFromWrapper(inventoryAdjustmentThresholdCSVWrapper));

            });
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private InventoryAdjustmentThreshold convertFromWrapper(InventoryAdjustmentThresholdCSVWrapper inventoryAdjustmentThresholdCSVWrapper) {
        logger.debug("Start to convert from CSV: {}", inventoryAdjustmentThresholdCSVWrapper);
        InventoryAdjustmentThreshold inventoryAdjustmentThreshold = new InventoryAdjustmentThreshold();

        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                    inventoryAdjustmentThresholdCSVWrapper.getCompany(),
                    inventoryAdjustmentThresholdCSVWrapper.getWarehouse()
            );
        inventoryAdjustmentThreshold.setWarehouseId(warehouse.getId());

        if (StringUtils.isNotBlank(inventoryAdjustmentThresholdCSVWrapper.getItem())) {
            inventoryAdjustmentThreshold.setItem(
                    itemService.findByName(warehouse.getId(), inventoryAdjustmentThresholdCSVWrapper.getItem())
            );
        }
        if (StringUtils.isNotBlank(inventoryAdjustmentThresholdCSVWrapper.getClient())) {
            inventoryAdjustmentThreshold.setClientId(
                    commonServiceRestemplateClient.getClientByName(
                            warehouse.getId(), inventoryAdjustmentThresholdCSVWrapper.getClient()).getId()
            );
        }
        if (StringUtils.isNotBlank(inventoryAdjustmentThresholdCSVWrapper.getItemFamily())) {
            inventoryAdjustmentThreshold.setItemFamily(
                    itemFamilyService.findByName(warehouse.getId(), inventoryAdjustmentThresholdCSVWrapper.getItemFamily())
            );
        }
        if (StringUtils.isNotBlank(inventoryAdjustmentThresholdCSVWrapper.getType())) {
            inventoryAdjustmentThreshold.setType(
                    InventoryQuantityChangeType.valueOf(inventoryAdjustmentThresholdCSVWrapper.getType())
            );
        }

        if (StringUtils.isNotBlank(inventoryAdjustmentThresholdCSVWrapper.getUser())) {
            inventoryAdjustmentThreshold.setUserId(
                    resourceServiceRestemplateClient.getUserByUsername(
                            warehouse.getCompanyId(), inventoryAdjustmentThresholdCSVWrapper.getUser()
                    ).getId()
            );
        }
        if (StringUtils.isNotBlank(inventoryAdjustmentThresholdCSVWrapper.getRole())) {
            inventoryAdjustmentThreshold.setRoleId(
                    resourceServiceRestemplateClient.getRoleByName(
                            warehouse.getCompanyId(), inventoryAdjustmentThresholdCSVWrapper.getRole()
                    ).getId()
            );
        }

        if (Objects.isNull(inventoryAdjustmentThresholdCSVWrapper.getQuantityThreshold()) &&
                Objects.isNull(inventoryAdjustmentThresholdCSVWrapper.getCostThreshold()) ) {

            throw MissingInformationException.raiseException("either quantity threshold or cost threshold must be passed in");
        }

        if (Objects.nonNull(inventoryAdjustmentThresholdCSVWrapper.getQuantityThreshold())) {
            inventoryAdjustmentThreshold.setQuantityThreshold(inventoryAdjustmentThresholdCSVWrapper.getQuantityThreshold());
        }
        if (Objects.nonNull(inventoryAdjustmentThresholdCSVWrapper.getCostThreshold())) {
            inventoryAdjustmentThreshold.setCostThreshold(inventoryAdjustmentThresholdCSVWrapper.getCostThreshold());
        }
        if (Objects.nonNull(inventoryAdjustmentThresholdCSVWrapper.getEnabled())) {
            inventoryAdjustmentThreshold.setEnabled(inventoryAdjustmentThresholdCSVWrapper.getEnabled());
        }
        return inventoryAdjustmentThreshold;

    }

    public boolean isInventoryAdjustExceedThreshold(Inventory inventory, InventoryQuantityChangeType type,
                                                    Long oldQuantity, Long newQuantity) {

        Long companyId = warehouseLayoutServiceRestemplateClient.getWarehouseById(
                inventory.getWarehouseId()
        ).getCompanyId();
        return isInventoryAdjustExceedThreshold(inventory, type, oldQuantity, newQuantity, userService.getCurrentUser(companyId));
    }

    public boolean isInventoryAdjustExceedThreshold(Inventory inventory, InventoryQuantityChangeType type,
                                                    Long oldQuantity, Long newQuantity, User user) {
        List<InventoryAdjustmentThreshold> inventoryAdjustmentThresholds = findAll(inventory.getWarehouseId());

        // Loop through all the rules and find all the matched inventory adjustment threshold
        Optional<InventoryAdjustmentThreshold> mappedInventoryAdjustmentThreshold =
                inventoryAdjustmentThresholds.
                        stream().
                        filter(inventoryAdjustmentThreshold -> match(inventoryAdjustmentThreshold, inventory, type, user)).
                        sorted((rule1, rule2) -> compareInventoryAdjustmentThreshold(rule1, rule2, inventory.getItem())).findFirst();

        if (mappedInventoryAdjustmentThreshold.isPresent()) {
            long adjustedQuantity = Math.abs(oldQuantity - newQuantity);
            logger.debug("We find several adjust threashold defined that match with the inventory and the small one is: {}",
                    mappedInventoryAdjustmentThreshold.get().getId());
            if (Objects.nonNull(mappedInventoryAdjustmentThreshold.get().getQuantityThreshold())) {
                logger.debug("will compare the rule with the adjust, quantity: {}", adjustedQuantity);
                return adjustedQuantity > mappedInventoryAdjustmentThreshold.get().getQuantityThreshold();
            }
            else if (Objects.nonNull(mappedInventoryAdjustmentThreshold.get().getCostThreshold()) &&
                        Objects.nonNull(inventory.getItem().getUnitCost())) {
                logger.debug("will compare the rule with the adjust, cost: {}", (adjustedQuantity * inventory.getItem().getUnitCost()));
                return (adjustedQuantity * inventory.getItem().getUnitCost()) > mappedInventoryAdjustmentThreshold.get().getCostThreshold();
            }
            else {
                // If we are here, then either
                // 1. Both the quantity threshold and cost threshold are not defined, which should never happen
                // 2. The cost threshold is defined but we don't have the unit cost defined for the item, which
                //    make the adjust threshold invalid for this adjustment.
                logger.debug("No need for approve for small amount adjust");
                return false;
            }
        }
        else {
            // We don't find any matched rules for the inventory adjustment, let's just return true
            // to allow the adjustment
            logger.debug("Can't find any threshold defined that match with this type of inventory adjust");
            return false;
        }


    }

    private boolean match(InventoryAdjustmentThreshold inventoryAdjustmentThreshold,
                         Inventory inventory,
                         InventoryQuantityChangeType type,
                         User user) {
        // If we have any criteria defined in the rule but doesn't match
        // with the inventory being adjust, then it is a non match
        // Otherwise it is a match
        if (Objects.nonNull(inventoryAdjustmentThreshold.getItem()) &&
                !inventoryAdjustmentThreshold.getItem().equals(inventory.getItem())) {
            return false;
        }
        if (Objects.nonNull(inventoryAdjustmentThreshold.getClientId()) &&
                !inventoryAdjustmentThreshold.getClientId().equals(inventory.getItem().getClientId())) {
            return false;
        }
        if (Objects.nonNull(inventoryAdjustmentThreshold.getItemFamily()) &&
                !inventoryAdjustmentThreshold.getItemFamily().equals(inventory.getItem().getItemFamily())) {
            return false;
        }
        if (Objects.nonNull(inventoryAdjustmentThreshold.getWarehouseId()) &&
                !inventoryAdjustmentThreshold.getWarehouseId().equals(inventory.getWarehouseId())) {
            return false;
        }
        if (Objects.nonNull(inventoryAdjustmentThreshold.getType()) &&
                !inventoryAdjustmentThreshold.getType().equals(type)) {
            return false;
        }
        if (Objects.nonNull(inventoryAdjustmentThreshold.getUserId()) &&
                !inventoryAdjustmentThreshold.getUserId().equals(user.getId())) {
            return false;
        }
        if (Objects.nonNull(inventoryAdjustmentThreshold.getRoleId())) {
            return user.getRoles().stream().
                    filter(role -> role.getId().equals(inventoryAdjustmentThreshold.getRoleId())).count() > 0;
        }

        return true;


    }

    private int compareInventoryAdjustmentThreshold(InventoryAdjustmentThreshold rule1, InventoryAdjustmentThreshold rule2,
                                                    Item item) {
        if (Objects.isNull(rule1.getCostThreshold()) && Objects.isNull(rule1.getQuantityThreshold()) ) {
            throw MissingInformationException.raiseException(
                    "Not able to compare the inventory adjustment threshold rule as one of the 2 miss both cost threshold and quantity threshold");
        }

        if (Objects.isNull(rule2.getCostThreshold()) && Objects.isNull(rule2.getQuantityThreshold()) ) {
            throw MissingInformationException.raiseException(
                    "Not able to compare the inventory adjustment threshold rule as one of the 2 miss both cost threshold and quantity threshold");
        }

        if (Objects.nonNull(rule1.getQuantityThreshold()) &&
            Objects.nonNull(rule2.getQuantityThreshold())) {
            return rule1.getQuantityThreshold().compareTo(rule2.getQuantityThreshold());
        }

        if (Objects.nonNull(rule1.getCostThreshold()) &&
                Objects.nonNull(rule2.getCostThreshold())) {
            return rule1.getCostThreshold().compareTo(rule2.getCostThreshold());
        }

        // One rule has the quantity defined and another has cost defined
        // let's convert both to cost and then compare

        if (Objects.isNull(item.getUnitCost())) {
            throw MissingInformationException.raiseException(
                    "Not able to compare the inventory adjustment threshold rule as the cost per unit is not defined for the item " + item.getName());

        }

        if (Objects.nonNull(rule1.getQuantityThreshold()) &&
                Objects.nonNull(rule2.getCostThreshold())) {
            Double cost = rule1.getQuantityThreshold() * item.getUnitCost();
            return cost.compareTo(rule2.getCostThreshold());
        }

        if (Objects.nonNull(rule1.getCostThreshold()) &&
                Objects.nonNull(rule2.getQuantityThreshold())) {
            Double cost = rule2.getQuantityThreshold() * item.getUnitCost();
            return rule1.getCostThreshold().compareTo(cost);
        }
        // I don't think we can ever reach here!!!
        return 0;
    }


    public InventoryAdjustmentThreshold addInventoryAdjustmentThreshold(InventoryAdjustmentThreshold inventoryAdjustmentThreshold) {
        return save(inventoryAdjustmentThreshold);
    }

    public InventoryAdjustmentThreshold changeInventoryAdjustmentThreshold(InventoryAdjustmentThreshold inventoryAdjustmentThreshold) {
        return save(inventoryAdjustmentThreshold);
    }

    @RequestMapping(value="/inventory-adjustment-thresholds/{id}", method = RequestMethod.DELETE)
    public void removeInventoryAdjustmentThreshold(@PathVariable Long id) {
        delete(id);
    }

}