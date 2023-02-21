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
import com.garyzhangscm.cwms.inventory.clients.*;
import com.garyzhangscm.cwms.inventory.exception.ItemException;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.ItemRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItemService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    ItemFamilyService itemFamilyService;
    @Autowired
    ItemPackageTypeService itemPackageTypeService;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    ImageService imageService;

    @Autowired
    private InboundServiceRestemplateClient inboundServiceRestemplateClient;
    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;
    @Autowired
    private WorkOrderServiceRestemplateClient workOrderServiceRestemplateClient;

    @Autowired
    private QCInspectionRequestService qcInspectionRequestService;

    @Autowired
    private AuditCountResultService auditCountResultService;
    @Autowired
    private CycleCountResultService cycleCountResultService;
    @Autowired
    private InventoryActivityService inventoryActivityService;
    @Autowired
    private InventoryConfigurationService inventoryConfigurationService;
    @Autowired
    private InventoryAdjustmentRequestService inventoryAdjustmentRequestService;
    @Autowired
    private InventoryAdjustmentThresholdService inventoryAdjustmentThresholdService;
    @Autowired
    private InventorySnapshotService inventorySnapshotService;
    @Autowired
    private ItemSamplingService itemSamplingService;
    @Autowired
    private LocationUtilizationSnapshotService locationUtilizationSnapshotService;

    @Autowired
    private QCRuleConfigurationService qcRuleConfigurationService;


    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.items:items}")
    String testDataFile;

    @Value("${fileupload.directory.upload:/upload/}")
    String uploadFolder;

    @Value("${fileupload.directory.upload.item.image:images/item/}")
    String itemImageFolder;
    @Value("${fileupload.directory.upload.item.thumbnail:images/item/thumbnail/}")
    String itemThumbnailFolder;

    public Item findById(Long id, boolean includeDetails) {
         Item item = itemRepository.findById(id)
                 .orElseThrow(() -> ResourceNotFoundException.raiseException("item not found by id: " + id));
         if (includeDetails) {
             loadAttribute(item);
         }
         return item;
    }
    public Item findById(Long id) {
        return findById(id, true);
    }


    public List<Item> findAll(Long companyId,
                              Long warehouseId,
                              String name,
                              String quickbookListId,
                              String clientIds,
                              String itemFamilyIds,
                              String itemIdList,
                              Boolean companyItem,
                              Boolean warehouseSpecificItem,
                              String description,
                              boolean loadDetails) {

        List<Item> items = itemRepository.findAll(
            (Root<Item> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();

                predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));


                if (StringUtils.isNotBlank(itemFamilyIds)) {

                    Join<Item, ItemFamily> joinItemFamily = root.join("itemFamily", JoinType.INNER);
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(joinItemFamily.get("id"));
                    for(String id : itemFamilyIds.split(",")) {
                        in.value(Long.parseLong(id));
                    }
                    predicates.add(criteriaBuilder.and(in));
                }

                if (StringUtils.isNotBlank(name)) {
                    if (name.contains("%")) {
                        predicates.add(criteriaBuilder.like(root.get("name"), name));
                    }
                    else {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }
                }

                if (StringUtils.isNotBlank(quickbookListId)) {
                    if (quickbookListId.contains("%")) {
                        predicates.add(criteriaBuilder.like(root.get("quickbookListId"), quickbookListId));
                    }
                    else {
                        predicates.add(criteriaBuilder.equal(root.get("quickbookListId"), quickbookListId));
                    }
                }
                // for description, we will always query by wild card
                if (StringUtils.isNotBlank(description)) {
                    String queryByDescription = description;
                    if (!queryByDescription.startsWith("%")) {
                        queryByDescription = "%" + queryByDescription;
                    }
                    if (!queryByDescription.endsWith("%")) {
                        queryByDescription = queryByDescription + "%";

                    }
                    predicates.add(criteriaBuilder.like(root.get("description"), queryByDescription));
                }

                if (StringUtils.isNotBlank(clientIds)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("clientId"));
                    for(String id : clientIds.split(",")) {
                        in.value(Long.parseLong(id));
                    }
                    predicates.add(criteriaBuilder.and(in));
                }

                if (StringUtils.isNotBlank(itemIdList)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("id"));
                    for(String id : itemIdList.split(",")) {
                        in.value(Long.parseLong(id));
                    }
                    predicates.add(criteriaBuilder.and(in));
                }


                Predicate[] p = new Predicate[predicates.size()];

                // special handling for warehouse id
                // if warehouse id is passed in, then return both the warehouse level item
                // and the company level item information.
                // otherwise, return the company level item information
                Predicate predicate = criteriaBuilder.and(predicates.toArray(p));
                if (Objects.nonNull(warehouseId) && Boolean.TRUE.equals(warehouseSpecificItem)) {
                        // return the item that specific at the warehouse level
                        return criteriaBuilder.and(
                                    predicate,
                                    criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                }
                else if (Objects.nonNull(warehouseId) && !Boolean.TRUE.equals(companyItem)) {
                    return criteriaBuilder.and(predicate,
                            criteriaBuilder.or(
                                    criteriaBuilder.equal(root.get("warehouseId"), warehouseId),
                                    criteriaBuilder.isNull(root.get("warehouseId"))));
                }
                else {
                    return criteriaBuilder.and(predicate,criteriaBuilder.isNull(root.get("warehouseId")));
                }
            }
            ,
            // we may get duplicated record from the above query when we pass in the warehouse id
            // if so, we may need to remove the company level item if we have the warehouse level item
            Sort.by(Sort.Direction.DESC, "warehouseId")
        );

        // we may get duplicated record from the above query when we pass in the warehouse id
        // if so, we may need to remove the company level item if we have the warehouse level item
        if (Objects.nonNull(warehouseId)) {
            removeDuplicatedItemRecords(items);
        }

        if (items.size() > 0 && loadDetails) {
            loadAttribute(items);
        }
        return items;
    }

    /**
     * Remove teh duplicated item record. If we have 2 record with the same item name
     * but different warehouse, then we will remove the one without any warehouse information
     * from the result
     * @param items
     */
    private void removeDuplicatedItemRecords(List<Item> items) {
        Iterator<Item> itemIterator = items.listIterator();
        Set<String> itemProcessed = new HashSet<>();
        while(itemIterator.hasNext()) {
            Item item = itemIterator.next();

            if (itemProcessed.contains(item.getName()) &&
                   Objects.isNull(item.getWarehouseId())) {
                // ok, we already processed the item and the current
                // record is a company level item, then we will remove
                // this record from the result
                itemIterator.remove();
            }
            itemProcessed.add(item.getName());
        }
    }

    private void loadAttribute(List<Item> items) {
        items.stream().forEach(this::loadAttribute);
    }

    private void loadAttribute(Item item) {

        if (item.getClientId() != null && item.getClient() == null) {
            item.setClient(commonServiceRestemplateClient.getClientById(item.getClientId()));
        }
        if (item.getAbcCategoryId() != null && item.getAbcCategory() == null) {
            item.setAbcCategory(commonServiceRestemplateClient.getABCCategoryById(item.getAbcCategoryId()));
        }
        if (item.getVelocityId() != null && item.getVelocity() == null) {
            item.setVelocity(commonServiceRestemplateClient.getVelocityById(item.getVelocityId()));
        }
        // Setup the unit of measure information for each item package type
        item.getItemPackageTypes().stream().forEach(itemPackageType -> {
            itemPackageType.getItemUnitOfMeasures()
                    .stream().filter(itemUnitOfMeasure -> itemUnitOfMeasure.getUnitOfMeasure() == null)
                    .forEach(itemUnitOfMeasure -> {
                itemUnitOfMeasure.setUnitOfMeasure(
                        commonServiceRestemplateClient.getUnitOfMeasureById(
                            itemUnitOfMeasure.getUnitOfMeasureId()));
            });

        });

    }


    public Item findByName(Long warehouseId, Long clientId, String name, boolean includeDetails){

        Item item = Objects.isNull(clientId) ?
                itemRepository.findByWarehouseIdAndName(warehouseId, name)
        :
                itemRepository.findByWarehouseIdAndClientIdAndName(warehouseId, clientId, name);
        if (item != null && includeDetails) {
            loadAttribute(item);
        }
        return item;
    }
    public Item findByName(Long warehouseId, Long clientId, String name){
        return findByName(warehouseId, clientId, name, true);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }


    public Item saveOrUpdate(Item item) {
        if (item.getId() == null && findByName(item.getWarehouseId(), item.getClientId(), item.getName()) != null) {
            item.setId(findByName(item.getWarehouseId(), item.getClientId(), item.getName()).getId());
        }
        Item newItem = save(item);

        // if the item doesn't have package setup yet, see if we will need to setup
        // default package type for this item
        if (Objects.isNull(newItem.getItemPackageTypes()) || newItem.getItemPackageTypes().size() == 0) {
            // let's see if we will need to setup a default item package type
            logger.debug("item package type is not setup for this item {}, let's see if we will need to setup a default one",
                    newItem.getName());
            if (setupDefaultItemPackageType(newItem)) {
                logger.debug("default item package type is setup for the new item {}" +
                        ", let's save the result",
                        newItem.getName());
                saveOrUpdate(newItem);
            }

        }



        return newItem;
    }

    private boolean setupDefaultItemPackageType(Item newItem) {
        // get the default item package type from the invenotry configuration
        if (Objects.isNull(newItem.getWarehouseId())) {
            // configuration is setup at the warehouse level. So we
            // we will need to have the warehouse id
            logger.debug("can't get warehouse id from item. So we won't default the " +
                    " item package type for this item {}", newItem.getName());
            return false;
        }
        InventoryConfiguration inventoryConfiguration =
                inventoryConfigurationService.findByWarehouseId(
                        newItem.getWarehouseId(), false
                );

        if (Objects.isNull(inventoryConfiguration)) {
            logger.debug("inventory configuration is not setup for warehouse {}. So we won't default the " +
                    " item package type for this item {}",
                    newItem.getWarehouseId(), newItem.getName());
            return false;
        }
        if (!Boolean.TRUE.equals(inventoryConfiguration.getNewItemAutoGenerateDefaultPackageType())) {
            logger.debug("the warehouse {} is setup NOT to create default item package type",
                    inventoryConfiguration.getWarehouseId());
            return false;
        }
        if (Strings.isBlank(inventoryConfiguration.getNewItemDefaultPackageTypeName())) {
            logger.debug("the warehouse {} is setup to create default item package type, but the name is not setup for it",
                    inventoryConfiguration.getWarehouseId());
            return false;

        }
        if (Strings.isBlank(inventoryConfiguration.getNewItemDefaultPackageTypeDescription())) {
            logger.debug("the warehouse {} is setup to create default item package type, but the description is not setup for it",
                    inventoryConfiguration.getWarehouseId());
            return false;

        }
        if (Objects.isNull(inventoryConfiguration.getItemDefaultPackageUOMS()) ||
                inventoryConfiguration.getItemDefaultPackageUOMS().isEmpty()) {
            logger.debug("the warehouse {} is setup to create default item package type, but there's no UOM setup for it",
                    inventoryConfiguration.getWarehouseId());
            return false;
        }

        // ok we pass all the validation, let's copy the default item package type and add it
        // to the item
        logger.debug("We found the default item package type and will assign it to the new item {}", newItem.getName());
        ItemPackageType itemPackageType = new ItemPackageType();
        itemPackageType.setItem(newItem);
        itemPackageType.setWarehouseId(newItem.getWarehouseId());
        itemPackageType.setCompanyId(newItem.getCompanyId());
        itemPackageType.setDefaultFlag(true);
        itemPackageType.setName(inventoryConfiguration.getNewItemDefaultPackageTypeName());
        itemPackageType.setDescription(inventoryConfiguration.getNewItemDefaultPackageTypeDescription());
        // add the unit of measure to the item package type
        inventoryConfiguration.getItemDefaultPackageUOMS().forEach(
                itemDefaultPackageUOM -> {
                    ItemUnitOfMeasure itemUnitOfMeasure = new ItemUnitOfMeasure(
                            newItem.getCompanyId(), newItem.getWarehouseId(), itemDefaultPackageUOM);
                    itemUnitOfMeasure.setItemPackageType(itemPackageType);
                    itemPackageType.addItemUnitOfMeasure(itemUnitOfMeasure);
                }
        );

        newItem.addItemPackageType(itemPackageType);


        return true;

    }

    public void delete(Item item) {
        itemRepository.delete(item);
    }
    public void delete(Long id) {
        itemRepository.deleteById(id);
    }
    public void delete(String itemIds) {
        if (!itemIds.isEmpty()) {
            long[] itemIdArray = Arrays.asList(itemIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : itemIdArray) {
                delete(id);
            }
        }

    }

    public List<Item> loadItemData(File  file) throws IOException {
        List<ItemCSVWrapper> itemCSVWrappers = loadData(file);
        return itemCSVWrappers.stream()
                .map(itemCSVWrapper -> convertFromWrapper(itemCSVWrapper)).collect(Collectors.toList());
    }

    public List<Item> saveItemData(File  file) throws IOException {
        List<Item> items = loadItemData(file);
        return items.stream().map(this::saveOrUpdate).collect(Collectors.toList());
    }

    public List<ItemCSVWrapper> loadData(File file) throws IOException {


        return fileService.loadData(file, getCsvSchema(), ItemCSVWrapper.class);
    }


    public List<ItemCSVWrapper> loadData(InputStream inputStream) throws IOException {

        return fileService.loadData(inputStream, getCsvSchema(), ItemCSVWrapper.class);
    }

    private CsvSchema getCsvSchema() {
        return CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                addColumn("client").
                addColumn("itemFamily").
                addColumn("unitCost").
                addColumn("allowCartonization").
                addColumn("allowAllocationByLPN").
                addColumn("allocationRoundUpStrategyType").
                addColumn("allocationRoundUpStrategyValue").
                addColumn("trackingVolumeFlag").
                addColumn("trackingLotNumberFlag").
                addColumn("trackingManufactureDateFlag").
                addColumn("shelfLifeDays").
                addColumn("trackingExpirationDateFlag").
                addColumn("imageUrl").
                addColumn("thumbnailUrl").
                build().withHeader();
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ItemCSVWrapper> itemCSVWrappers = loadData(inputStream);
            itemCSVWrappers.stream().forEach(itemCSVWrapper -> saveOrUpdate(convertFromWrapper(itemCSVWrapper)));
            itemRepository.flush();
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private Item convertFromWrapper(ItemCSVWrapper itemCSVWrapper) {
        Item item = new Item();
        BeanUtils.copyProperties(itemCSVWrapper, item);
        /***
        item.setName(itemCSVWrapper.getName());
        item.setDescription(itemCSVWrapper.getDescription());
        item.setUnitCost(itemCSVWrapper.getUnitCost());
        item.setAllowCartonization(itemCSVWrapper.getAllowCartonization());


        item.setTrackingVolumeFlag(itemCSVWrapper.isTrackingVolumeFlag());
        item.setTrackingLotNumberFlag(itemCSVWrapper.isTrackingLotNumberFlag());
        item.setTrackingManufactureDateFlag(itemCSVWrapper.isTrackingManufactureDateFlag());
        item.setTrackingExpirationDateFlag(itemCSVWrapper.isTrackingExpirationDateFlag());
        item.setShelfLifeDays(itemCSVWrapper.getShelfLifeDays());
**/
        Company company = warehouseLayoutServiceRestemplateClient.getCompanyByCode(
                itemCSVWrapper.getCompany());
        item.setCompanyId(company.getId());
        // warehouse
        Warehouse warehouse =
                    warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                            itemCSVWrapper.getCompany(),
                            itemCSVWrapper.getWarehouse());

        item.setWarehouseId(warehouse.getId());

        if (!itemCSVWrapper.getClient().isEmpty()) {
            Client client = commonServiceRestemplateClient.getClientByName(
                    warehouse.getId(),itemCSVWrapper.getClient());
            item.setClientId(client.getId());
        }
        if (!itemCSVWrapper.getItemFamily().isEmpty()) {

            ItemFamily itemFamily = itemFamilyService.findByName(warehouse.getId(), itemCSVWrapper.getItemFamily());
            item.setItemFamily(itemFamily);
        }

        if (Objects.nonNull(itemCSVWrapper.getAllowAllocationByLPN())) {
            item.setAllowAllocationByLPN(
                    itemCSVWrapper.getAllowAllocationByLPN()
            );
        }

        if (StringUtils.isNotBlank(itemCSVWrapper.getAllocationRoundUpStrategyType())) {
            item.setAllocationRoundUpStrategyType(
                    AllocationRoundUpStrategyType.valueOf(
                            itemCSVWrapper.getAllocationRoundUpStrategyType()
                    )
            );
        }
        if (Objects.nonNull(itemCSVWrapper.getAllocationRoundUpStrategyValue())) {
            item.setAllocationRoundUpStrategyValue(
                    itemCSVWrapper.getAllocationRoundUpStrategyValue()
            );
        }

        // default to active item
        item.setActiveFlag(true);
        return item;

    }


    public Item addItem(Item item) {

        item.getItemPackageTypes().forEach(itemPackageType -> {
            itemPackageType.setItem(item);
            if(Objects.isNull(item.getItemFamily().getId())) {
                item.setItemFamily(getOrCreateItemFamily(item.getItemFamily()));
            }
            itemPackageType.getItemUnitOfMeasures().forEach(
                    itemUnitOfMeasure -> {
                        itemUnitOfMeasure.setItemPackageType(
                                itemPackageType
                        );
                        if (Objects.isNull(itemUnitOfMeasure.getUnitOfMeasureId())) {
                            //we will need to get the unit of measure. If it doesn't exists yet
                            // we will create it on the fly
                            logger.debug("unit of measure id is null when creating item {}, item package type {}, item unit of measure {}",
                                    item.getName(),
                                    itemPackageType.getName(),
                                    itemUnitOfMeasure.getUnitOfMeasure().getName());
                            UnitOfMeasure unitOfMeasure = null;
                            try {
                                unitOfMeasure = getOrCreateUnitOfMeasure(itemUnitOfMeasure.getUnitOfMeasure());
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }
                            itemUnitOfMeasure.setUnitOfMeasureId(unitOfMeasure.getId());
                        }
                    }
            );

        });
        // check if we are adding a new item at the warehouse level
        // and override the global one. If so, we may need to update the
        // item id on the inventory, cycle count, receipt and order to point to
        // the new item id instead of the global one, for those document in the
        // specific warehouse
        boolean newWarehouseItem = false;
        Long globalItemId = null;
        if (Objects.nonNull(item.getWarehouseId()) &&
                Objects.isNull(findByName(item.getWarehouseId(), item.getClientId(), item.getName()))) {
            newWarehouseItem = true;
            // see if we can find the global item with the same name
            Item globalItem = findByName(null, item.getClientId(), item.getName(), false);
            if (Objects.nonNull(globalItem)) {
                globalItemId = globalItem.getId();
            }
        }

        Item newItem =  saveOrUpdate(item);


        if (newWarehouseItem &&
                Objects.nonNull(globalItemId)) {

            logger.debug("we create a new item {} in the warehouse {} to override the global one(id: {})",
                    newItem.getName(), newItem.getWarehouseId(), globalItemId);
            logger.debug("we will update the item id in the order line, receipt line, inventory " +
                    " and cycle count request to reflect the item id change and point them to the new item id");

            handleItemOverride(newItem.getWarehouseId(),
                    globalItemId, newItem.getId());


        }

        return newItem;
    }

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {

        // inventory service
        // TO-DO
        auditCountResultService.handleItemOverride(warehouseId, oldItemId, newItemId);
        cycleCountResultService.handleItemOverride(warehouseId, oldItemId, newItemId);
        inventoryService.handleItemOverride(warehouseId, oldItemId, newItemId);
        inventoryActivityService.handleItemOverride(warehouseId, oldItemId, newItemId);
        inventoryAdjustmentRequestService.handleItemOverride(warehouseId, oldItemId, newItemId);
        inventoryAdjustmentThresholdService.handleItemOverride(warehouseId, oldItemId, newItemId);
        inventorySnapshotService.handleItemOverride(warehouseId, oldItemId, newItemId);
        itemSamplingService.handleItemOverride(warehouseId, oldItemId, newItemId);
        locationUtilizationSnapshotService.handleItemOverride(warehouseId, oldItemId, newItemId);
        qcInspectionRequestService.handleItemOverride(warehouseId, oldItemId, newItemId);
        qcRuleConfigurationService.handleItemOverride(warehouseId, oldItemId, newItemId);

        // other service
        inboundServiceRestemplateClient.handleItemOverride(warehouseId, oldItemId, newItemId);
        outbuondServiceRestemplateClient.handleItemOverride(warehouseId, oldItemId, newItemId);
        workOrderServiceRestemplateClient.handleItemOverride(warehouseId, oldItemId, newItemId);
    }
    /**
     * Get item family. If the item family  doesn't exists yet, we will create it
     * on the fly
     * use for multi warehouse support
     * @param itemFamily
     * @return
     */
    private ItemFamily getOrCreateItemFamily(ItemFamily itemFamily) {
        if (Objects.nonNull(itemFamily.getId())) {
            return itemFamily;
        }
        ItemFamily existingItemFamily = null;
        if (Objects.nonNull(itemFamily.getWarehouseId())) {
            // MULTI-warehouse support:
            // warehouse id is on the unit of measure, let's
            // get the warehouse specific unit of measure
            logger.debug("start to get the item family {} by warehouse id {}",
                    itemFamily.getName(), itemFamily.getWarehouseId());
            List<ItemFamily> itemFamilies
                    = itemFamilyService.findAll(
                                itemFamily.getCompanyId(),
                                itemFamily.getWarehouseId(),
                                itemFamily.getName(), false, true, false
                        );
            if (!itemFamilies.isEmpty()) {
                existingItemFamily = itemFamilies.get(0);
            }
            logger.debug(">> get existingItemFamily? {}", Objects.nonNull(existingItemFamily) );
        }
        else  {

            logger.debug("start to get the item family {} by company id {}",
                    itemFamily.getName(), itemFamily.getCompanyId());
            List<ItemFamily> itemFamilies
                    = itemFamilyService.findAll(
                    itemFamily.getCompanyId(), null,
                    itemFamily.getName(), true, false, false
            );
            if (!itemFamilies.isEmpty()) {
                existingItemFamily = itemFamilies.get(0);
            }
            logger.debug(">> get existingItemFamily? {}", Objects.nonNull(existingItemFamily) );
        }
        if (Objects.isNull(existingItemFamily)) {
            logger.debug("start to create item family {} ", itemFamily);
            existingItemFamily = itemFamilyService.addItemFamily(
                    itemFamily
            );
            logger.debug(">> item family created! {}", Objects.nonNull(existingItemFamily) );

        }
        return existingItemFamily;
    }

    /**
     * Get unit of measure. If the unit of measure doesn't exists yet, we will create it
     * on the fly
     * use for multi warehouse support
     * @param unitOfMeasure
     * @return
     */
    private UnitOfMeasure getOrCreateUnitOfMeasure(UnitOfMeasure unitOfMeasure) throws JsonProcessingException {
        if (Objects.nonNull(unitOfMeasure.getId())) {
            logger.debug("getOrCreateUnitOfMeasure: we already have the unit of measure created! id {}, name {}",
                    unitOfMeasure.getId(), unitOfMeasure.getName());
            return unitOfMeasure;
        }

        UnitOfMeasure existingUnitOfMeasure;
        if (Objects.nonNull(unitOfMeasure.getWarehouseId())) {
            // MULTI-warehouse support:
            // warehouse id is on the unit of measure, let's
            // get the warehouse specific unit of measure
            logger.debug("start to get the unit of measure {} by warehouse id {}",
                    unitOfMeasure.getName(), unitOfMeasure.getWarehouseId());
            existingUnitOfMeasure
                    = commonServiceRestemplateClient.getUnitOfMeasureByName(
                        unitOfMeasure.getCompanyId(),
                        unitOfMeasure.getWarehouseId(),
                        unitOfMeasure.getName(), false, true
                    );
            logger.debug(">> get existingUnitOfMeasure? {}", Objects.nonNull(existingUnitOfMeasure) );
        }
        else  {

            logger.debug("start to get the unit of measure {} by company id {}",
                    unitOfMeasure.getName(), unitOfMeasure.getCompanyId());
            existingUnitOfMeasure
                    = commonServiceRestemplateClient.getUnitOfMeasureByName(
                    unitOfMeasure.getCompanyId(),
                    null,
                    unitOfMeasure.getName(), true, false
            );
            logger.debug(">> get existingUnitOfMeasure? {}", Objects.nonNull(existingUnitOfMeasure) );
        }
        if (Objects.isNull(existingUnitOfMeasure)) {
            logger.debug("start to create unit of measure {} ", unitOfMeasure);
            existingUnitOfMeasure = commonServiceRestemplateClient.createUnitOfMeasure(
                    unitOfMeasure
            );
            logger.debug(">> unit of measure created! {}", Objects.nonNull(existingUnitOfMeasure) );

        }
        return existingUnitOfMeasure;


    }

    public Item changeItem(Long id, Item item) {
        Item existingItem = findById(id);

        BeanUtils.copyProperties(item, existingItem, "id", "itemPackageTypes");

        copyItemPackageTypes(existingItem, item);

        return  saveOrUpdate(existingItem);




    }

    private void copyItemPackageTypes(Item existingItem, Item item) {

        item.getItemPackageTypes().forEach(itemPackageType -> {
            // If this is an existing item package type, then update
            // the attribute
            if (Objects.nonNull(itemPackageType.getId())) {
                existingItem.getItemPackageTypes().stream().filter(existingItemPackageType ->
                        existingItemPackageType.getId().equals(itemPackageType.getId()))
                        .forEach(existingItemPackageType -> {
                            BeanUtils.copyProperties(itemPackageType, existingItemPackageType,
                                    "itemUnitOfMeasures", "item");
                            copyItemUnitOfMeasure(existingItemPackageType, itemPackageType);
                        });
            }
            else {
                // OK, this is a new item package type, let's just add it to the
                // item
                itemPackageType.setItem(existingItem);
                itemPackageType.getItemUnitOfMeasures().forEach(itemUnitOfMeasure -> {
                    itemUnitOfMeasure.setItemPackageType(itemPackageType);
                });
                existingItem.addItemPackageType(itemPackageType);
            }
            logger.debug("itemPackageType: {} processed", itemPackageType.getName());
        });

        // Remove the item package type that is no long exists
        Iterator<ItemPackageType> itemPackageTypeIterator = existingItem.getItemPackageTypes().iterator();
        while(itemPackageTypeIterator.hasNext()) {
            ItemPackageType existingItemPackageType = itemPackageTypeIterator.next();
            logger.debug("Check ItemPackageType {} still exists ", existingItemPackageType.getName());

            if (Objects.isNull(existingItemPackageType.getId())) {
                logger.debug("Check ItemPackageType {} is just added, ignore ", existingItemPackageType.getName());
                continue;

            }

            boolean itemPackageTypeStillExists =
                    item.getItemPackageTypes().stream().filter(
                            itemPackageType -> existingItemPackageType.getId().equals(
                                    itemPackageType.getId()
                            )
                    ).count() > 0;

            if (!itemPackageTypeStillExists &&
                    itemPackageTypeService.isItemPackageTypeRemovable(existingItem, existingItemPackageType)) {
                // make sure no inventory has this item package type

                itemPackageTypeIterator.remove();
            }
        }

    }

    private void copyItemUnitOfMeasure(ItemPackageType existingItemPackageType, ItemPackageType itemPackageType) {

        itemPackageType.getItemUnitOfMeasures().forEach(itemUnitOfMeasure -> {
            // If this is an existing item package type, then update
            // the attribute
            logger.debug("new item unit of measure / {}: Objects.nonNull(unitOfMeasure.getId())? : {}",
                    itemUnitOfMeasure.getUnitOfMeasure().getName(),
                    Objects.nonNull(itemUnitOfMeasure.getId()));
            if (Objects.nonNull(itemUnitOfMeasure.getId())) {
                // get the existing item unit of measure and copy it over
                existingItemPackageType.getItemUnitOfMeasures().stream()

                        .filter(existingUnitOfMeasure ->
                                Objects.equals(existingUnitOfMeasure.getId(), itemUnitOfMeasure.getId()))
                        .forEach(existingUnitOfMeasure -> {
                            BeanUtils.copyProperties(itemUnitOfMeasure, existingUnitOfMeasure, "itemPackageType");

                        });
            }
            else {
                // OK, this is a new item unit of measure, let's just add it to the
                // item

                itemUnitOfMeasure.setItemPackageType(existingItemPackageType);
                if (Objects.isNull(itemUnitOfMeasure.getUnitOfMeasureId())) {
                    itemUnitOfMeasure.setUnitOfMeasureId(
                            itemUnitOfMeasure.getUnitOfMeasure().getId()
                    );
                }
                existingItemPackageType.addItemUnitOfMeasure(itemUnitOfMeasure);
            }
        });

        // Remove the item package type that is no long exists
        Iterator<ItemUnitOfMeasure> itemUnitOfMeasureIterator = existingItemPackageType.getItemUnitOfMeasures().iterator();
        while(itemUnitOfMeasureIterator.hasNext()) {
            ItemUnitOfMeasure existingItemUnitOfMeasure = itemUnitOfMeasureIterator.next();

            if(Objects.isNull(existingItemUnitOfMeasure.getId())) {
                continue;
            }


            boolean itemUnitOfMeasureStillExists =
                    itemPackageType.getItemUnitOfMeasures().stream().anyMatch(
                            itemUnitOfMeasure -> existingItemUnitOfMeasure.getId().equals(
                                    itemUnitOfMeasure.getId()
                            )
                    );
            if (!itemUnitOfMeasureStillExists) {

                itemUnitOfMeasureIterator.remove();
            }
        }

    }

    public String validateNewItemName(Long warehouseId, Long clientId, String itemName) {

        Item item =
                findByName(warehouseId, clientId,  itemName, false);

        return Objects.isNull(item) ? "" : ValidatorResult.VALUE_ALREADY_EXISTS.name();
    }

    public Item deleteItem(Long id) {
        Item item = findById(id);
        if (!isItemRemovable(item)) {
            throw ItemException.raiseException("Can't remove this item. there's inventory attached to this item");
        }
        delete(id);
        return item;

    }


    public boolean isItemRemovable(Item item) {
        // Check if we have inventory that is using this item package type

        if (inventoryService.
                findAll(item.getWarehouseId(), null, item.getName(), null,
                        null,null,null,null,null, null,
                        null,null,null,null,
                        null,null,null,null, null,null,null,
                        false, null)
                .size() > 0) {
            logger.debug("There's inventory attached to this item  {}  , can't remove it",
                    item.getName());

            return false;
        };

        logger.debug("There's NO inventory attached to this item  {}, WE CAN remove it",
                item.getName());
        return true;
    }

    public Item uploadItemImages(Long id, MultipartFile file) throws IOException {
        Item item = findById(id);
        logger.debug("Start to save item image: name: {} original fle name:  {} , content type: {}",
                file.getName(), file.getOriginalFilename(), file.getContentType());


        String newFileName  = item.getName() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String imageDestination =   uploadFolder + itemImageFolder  + item.getWarehouseId() + "/" + item.getId() + "/" + newFileName;
        String thumbnailDestination =  uploadFolder +  itemThumbnailFolder + item.getWarehouseId() + "/" + item.getId() + "/" + newFileName;

        logger.debug("start to save item {}'s image to destination: {}",
                item.getName(), imageDestination);
        fileService.saveFile(file, imageDestination);

        logger.debug("item {}'s image is saved to destination: {}",
                item.getName(), imageDestination);
        item.setImageUrl(itemImageFolder  + item.getWarehouseId() + "/" + item.getId() + "/" + newFileName);

        // we will save the thumbnail automatically

        logger.debug("start to save item {}'s thumbnail to destination: {}",
                item.getName(), thumbnailDestination);
        imageService.generateThumbnail(imageDestination, thumbnailDestination);
        logger.debug("item {}'s thumbnail is saved to destination: {}",
                item.getName(), thumbnailDestination);
        item.setThumbnailUrl(itemThumbnailFolder + item.getWarehouseId() + "/" + item.getId() + "/" + newFileName);

        return saveOrUpdate(item);

    }


    /**
     * Process integration data.
     * @param item
     */
    public void processIntegration(Item item) {

        // if the item already exists, then update it
        Item existingItem = findByName(item.getWarehouseId(), item.getClientId(), item.getName(), false);
        if (Objects.nonNull(existingItem)) {
            // we found item with the same name, let's update the item
            item.setId(existingItem.getId());
            // merge existing data to the integration information
            // so that we will only adding and update existing information
            // but not removing anything
            mergeExistingItemForIntegration(item, existingItem);
        }


        logger.debug(">> start to save item integration data");
        logger.debug("=========   Item Integration Data Content =========\n {}",
                item.getName());
        saveOrUpdate(item);




    }

    /**
     * Merge the existing item information into the integrated one so that to make
     * sure we are only adding or update information but not removing any existing
     * information that doesn't exists in the integration information
     * @param item
     * @param existingItem
     */
    private void mergeExistingItemForIntegration(Item item, Item existingItem) {

        // for any item package type that already exists, update
        // otherwise, create the new item package type
        existingItem.getItemPackageTypes().forEach(
                existingItemPackageType -> {
                    // for each existing item package type is not in the integration, let's
                    // attached to the integration information
                    Optional<ItemPackageType> matchedIntegratedItemPackageType =
                            item.getItemPackageTypes().stream().filter(
                                        itemPackageType -> existingItemPackageType.getName().equals(itemPackageType.getName())
                                ).findFirst();
                    if (!matchedIntegratedItemPackageType.isPresent()) {
                        // ok, we have an existing item package type but not exists in the integration information,
                        // then we will add it to the integration information
                        item.addItemPackageType(existingItemPackageType);
                        logger.debug("The item package type {} is not in the integration, let's just add it",
                                existingItemPackageType.getName());
                    }
                    else {
                        // ok the existing item package type already exists in the integration, let's make sure
                        // all the UOM matches as well

                        ItemPackageType itemPackageType = matchedIntegratedItemPackageType.get();
                        itemPackageType.setId(existingItemPackageType.getId());

                        logger.debug("The item package type {} exists in the integration, let's just merge the UOM",
                                existingItemPackageType.getName());

                        mergeExistingItemPackageTypeForIntegration(matchedIntegratedItemPackageType.get(), existingItemPackageType);
                    }
                }
        );


        // we are not allow to change those information through integration yet
        String[] fieldNames = {
                "name",
                "clientId","trackingVolumeFlag",
                "trackingLotNumberFlag",
                "trackingManufactureDateFlag","shelfLifeDays",
                "trackingExpirationDateFlag", "warehouseId",
                "companyId", "allowCartonization", "allowAllocationByLPN",
                "allocationRoundUpStrategyType", "allocationRoundUpStrategyValue",
                "imageUrl", "thumbnailUrl", "activeFlag"
        };

        ObjectCopyUtil.copyValue(existingItem, item,  fieldNames);
    }

    /**
     * Merge the existing UOMs from the existing item package type into the integration information
     * @param itemPackageType
     * @param existingItemPackageType
     */
    private void mergeExistingItemPackageTypeForIntegration(ItemPackageType itemPackageType, ItemPackageType existingItemPackageType) {

        existingItemPackageType.getItemUnitOfMeasures().forEach(
                existingItemUnitOfMeasure -> {
                    // for each existing item package type is not in the integration, let's
                    // attached to the integration information
                    Optional<ItemUnitOfMeasure> matchedIntegratedItemUnitOfMeasure =
                            itemPackageType.getItemUnitOfMeasures().stream().filter(
                                    itemUnitOfMeasure -> existingItemUnitOfMeasure.getUnitOfMeasureId().equals(
                                            itemUnitOfMeasure.getUnitOfMeasureId())
                            ).findFirst();
                    if (!matchedIntegratedItemUnitOfMeasure.isPresent()) {
                        // ok, we have an existing item unit of measure but not exists in the integration information,
                        // then we will add it to the integration information
                        itemPackageType.addItemUnitOfMeasure(existingItemUnitOfMeasure);
                        logger.debug("The item unit of measure {} is not in the item package type {} , let's just add it",
                                existingItemUnitOfMeasure.getUnitOfMeasureId(),
                                itemPackageType.getName());
                    }
                    else {
                        // ok the existing item package type already exists in the integration, let's make sure
                        // all the UOM matches as well

                        ItemUnitOfMeasure itemUnitOfMeasure = matchedIntegratedItemUnitOfMeasure.get();
                        itemUnitOfMeasure.setId(existingItemUnitOfMeasure.getId());

                        logger.debug("The item unit of measure {} exists the item package type {} , let's just update it",
                                existingItemUnitOfMeasure.getUnitOfMeasureId(),
                                itemPackageType.getName());
                    }
                }
        );
    }

    public void handleItemFamilyOverride(Long oldItemFamilyId, Long newItemFamilyId, Long warehouseId) {

        logger.debug("start to process item family override, current warehouse {}, from item family id {} to item family id {}",
                warehouseId, oldItemFamilyId, newItemFamilyId);
        itemRepository.processItemFamilyOverride(oldItemFamilyId, newItemFamilyId, warehouseId);
    }

    public List<Item> findByKeyword(Long companyId,  Long warehouseId, String keyword, Boolean loadDetails) {
        // query by name with wildcard first
        logger.debug("Start to query item by keyword: {}", keyword);

        logger.debug("start to get item by name equals to the keyword");
        List<Item> items = findAll(companyId, warehouseId,
                "%" + keyword + "%",null,  null, null,null,
                null,null, null, loadDetails);
        // query by description
        logger.debug("start to get item by description equals to the keyword");
        items.addAll(findAll(companyId, warehouseId, null,null,null, null,null,
                null,null, keyword, loadDetails));

        return items;

    }

    /**
     * Manually process item override. We may need to update the item id in the receipt
     * work order / orders / etc.
     * @param warehouseId warehouse id
     * @param itemId Item Id
     * @return
     */
    public void processItemOverride(Long warehouseId, Long itemId) {
        logger.debug("let's manually refresh all the data when we override the global item into a warehouse specific one");

        if (Objects.isNull(itemId)) {

            // item id is not passed in, we will refresh for all items in the warehouse
            List<Item> warehouseItems = itemRepository.findByWarehouseId(warehouseId);
            processItemOverride(warehouseItems);
        }
        else {
            Item warehouseItem = findById(itemId);
            if (Objects.isNull(warehouseItem.getWarehouseId())) {
                throw ItemException.raiseException("please choose a warehouse item to start refresh");
            }
            else if (!warehouseItem.getWarehouseId().equals(warehouseId)) {
                throw ItemException.raiseException("the item selected doesn't match with the warehouse passed in");
            }
            else {
                processItemOverride(warehouseItem);
            }
        }
    }
    public void processItemOverride(List<Item> warehouseItems) {
        warehouseItems.forEach(
                warehouseItem -> processItemOverride(warehouseItem)
        );
    }
    public void processItemOverride(Item warehouseItem) {

        logger.debug("item name", warehouseItem.getName());
        logger.debug("warehouse specific item id {}", warehouseItem.getId());
        // let's find the global item by name
        Item globalItem = itemRepository.findGlobalItemByName(warehouseItem.getName());
        if (Objects.isNull(globalItem)) {
            return;
        }

        logger.debug("global item id {}", globalItem.getId());
        // ok now we have the warehouse specific item and global item,

        handleItemOverride(warehouseItem.getWarehouseId(),
                globalItem.getId(), warehouseItem.getId());

        logger.debug("refresh is done");
    }

    public Item createItem(ItemPackageTypeCSVWrapper itemPackageTypeCSVWrapper) {
        ItemCSVWrapper itemCSVWrapper = new ItemCSVWrapper();
        itemCSVWrapper.setCompany(itemPackageTypeCSVWrapper.getCompany());
        itemCSVWrapper.setWarehouse(itemPackageTypeCSVWrapper.getWarehouse());
        itemCSVWrapper.setName(itemPackageTypeCSVWrapper.getItem());
        itemCSVWrapper.setDescription(itemPackageTypeCSVWrapper.getItemDescription());
        itemCSVWrapper.setClient(itemPackageTypeCSVWrapper.getClient());
        itemCSVWrapper.setItemFamily(itemPackageTypeCSVWrapper.getItemFamily());
        itemCSVWrapper.setUnitCost(0.0);
        itemCSVWrapper.setAllowCartonization(false);
        itemCSVWrapper.setAllowAllocationByLPN(false);
        itemCSVWrapper.setTrackingVolumeFlag(true);
        itemCSVWrapper.setTrackingLotNumberFlag(false);
        itemCSVWrapper.setTrackingExpirationDateFlag(false);
        itemCSVWrapper.setTrackingManufactureDateFlag(false);

        itemCSVWrapper.setTrackingColorFlag(

                Strings.isBlank(itemPackageTypeCSVWrapper.getTrackingColorFlag()) ?
                        false :
                        itemPackageTypeCSVWrapper.getTrackingColorFlag().trim().equals("1") ||
                                itemPackageTypeCSVWrapper.getTrackingColorFlag().trim().equalsIgnoreCase("true")


        );
        itemCSVWrapper.setDefaultColor(
                itemPackageTypeCSVWrapper.getDefaultColor()
        );
        itemCSVWrapper.setTrackingProductSizeFlag(

                Strings.isBlank(itemPackageTypeCSVWrapper.getTrackingProductSizeFlag()) ?
                        false :
                        itemPackageTypeCSVWrapper.getTrackingProductSizeFlag().trim().equals("1") ||
                                itemPackageTypeCSVWrapper.getTrackingProductSizeFlag().trim().equalsIgnoreCase("true")

        );
        itemCSVWrapper.setDefaultProductSize(
                itemPackageTypeCSVWrapper.getDefaultProductSize()
        );
        itemCSVWrapper.setTrackingStyleFlag(
                Strings.isBlank(itemPackageTypeCSVWrapper.getTrackingStyleFlag()) ?
                        false :
                        itemPackageTypeCSVWrapper.getTrackingStyleFlag().trim().equals("1") ||
                                itemPackageTypeCSVWrapper.getTrackingStyleFlag().trim().equalsIgnoreCase("true")
        );
        itemCSVWrapper.setDefaultStyle(
                itemPackageTypeCSVWrapper.getDefaultStyle()
        );


        return saveOrUpdate(convertFromWrapper(itemCSVWrapper));

    }
}
