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
import jakarta.persistence.criteria.*;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ItemService {
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemBarcodeService itemBarcodeService;
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
    private UserService userService;
    @Autowired
    private KafkaSender kafkaSender;

    @Autowired
    private ClientRestrictionUtil clientRestrictionUtil;

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

    @Value("${fileupload.directory.upload.item.workordersop:sop/item/work-order-sop/}")
    String workOrderSOPFolder;


    private final static int FILE_UPLOAD_MAP_SIZE_THRESHOLD = 20;
    private Map<String, Double> fileUploadProgressMap = new ConcurrentHashMap<>();
    private Map<String, List<FileUploadResult>> fileUploadResultsMap = new ConcurrentHashMap<>();

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
                              String names,
                              String quickbookListId,
                              String clientIds,
                              String itemFamilyIds,
                              String itemIdList,
                              Boolean companyItem,
                              Boolean warehouseSpecificItem,
                              String description,
                              boolean loadDetails,
                              ClientRestriction clientRestriction) {

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
                    if (name.contains("*")) {
                        predicates.add(criteriaBuilder.like(root.get("name"), name.replaceAll("\\*", "%")));
                    }
                    else {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }
                }

                if (Strings.isNotBlank(names)) {

                    CriteriaBuilder.In<String> in = criteriaBuilder.in(root.get("name"));
                    for(String itemName : names.split(",")) {
                        in.value(itemName);
                    }
                    predicates.add(criteriaBuilder.and(in));
                }
                if (StringUtils.isNotBlank(quickbookListId)) {
                    if (quickbookListId.contains("*")) {
                        predicates.add(criteriaBuilder.like(root.get("quickbookListId"), quickbookListId.replaceAll("\\*", "%")));
                    }
                    else {
                        predicates.add(criteriaBuilder.equal(root.get("quickbookListId"), quickbookListId));
                    }
                }
                // for description, we will always query by wild card
                if (StringUtils.isNotBlank(description)) {
                    String queryByDescription = description.replaceAll("\\*", "%");
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
                    predicate = criteriaBuilder.and(
                                    predicate,
                                    criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                }
                else if (Objects.nonNull(warehouseId) && !Boolean.TRUE.equals(companyItem)) {
                    predicate =  criteriaBuilder.and(predicate,
                            criteriaBuilder.or(
                                    criteriaBuilder.equal(root.get("warehouseId"), warehouseId),
                                    criteriaBuilder.isNull(root.get("warehouseId"))));
                }
                else {
                    predicate =  criteriaBuilder.and(predicate,criteriaBuilder.isNull(root.get("warehouseId")));
                }

                // special handing for client id

                return clientRestrictionUtil.addClientRestriction(root,
                        predicates,
                        clientRestriction,
                        criteriaBuilder);
                /**
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
                 **/
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
            try {
                item.setClient(commonServiceRestemplateClient.getClientById(item.getClientId()));
            }
            catch (Exception ex) {}
        }
        if (item.getAbcCategoryId() != null && item.getAbcCategory() == null) {
            try {
                item.setAbcCategory(commonServiceRestemplateClient.getABCCategoryById(item.getAbcCategoryId()));
            }
            catch (Exception ex) {}
        }
        if (item.getVelocityId() != null && item.getVelocity() == null) {
            try {
                item.setVelocity(commonServiceRestemplateClient.getVelocityById(item.getVelocityId()));
            }
            catch (Exception ex) {}
        }
        // Setup the unit of measure information for each item package type
        item.getItemPackageTypes().stream().forEach(itemPackageType -> {
            itemPackageType.getItemUnitOfMeasures()
                    .stream().filter(itemUnitOfMeasure -> itemUnitOfMeasure.getUnitOfMeasure() == null)
                    .forEach(itemUnitOfMeasure -> {
                        try {
                            itemUnitOfMeasure.setUnitOfMeasure(
                                    commonServiceRestemplateClient.getUnitOfMeasureById(
                                            itemUnitOfMeasure.getUnitOfMeasureId()));
                        }
                        catch (Exception ex) {}

            });

        });

        // for kit item, we may need to load inner items as well
        if (Boolean.TRUE.equals(item.getKitItemFlag()) && Objects.nonNull(item.getBillOfMaterialId())) {
            BillOfMaterial billOfMaterial =
                    Objects.nonNull(item.getBillOfMaterial()) ?
                            item.getBillOfMaterial() :
                    workOrderServiceRestemplateClient.getBillOfMaterialById(item.getBillOfMaterialId(), false);
            item.setBillOfMaterial(billOfMaterial);
            for (BillOfMaterialLine billOfMaterialLine : billOfMaterial.getBillOfMaterialLines()) {
                Item kitInnerItem = billOfMaterialLine.getItem();
                if (Objects.isNull(kitInnerItem)) {
                    kitInnerItem = findById(billOfMaterialLine.getItemId());
                    billOfMaterialLine.setItem(kitInnerItem);
                }
                item.addKitInnerItem(kitInnerItem);
            }
        }



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

        // send alert for new receipt or receipt change
        boolean newItemFlag = false;
        if (Objects.isNull(item.getId())) {
            newItemFlag = true;
        }
        // in case the item is created out of context, we will need to
        // setup the created by in the context and then pass the username
        // in the down stream so that when we send alert, the alert will
        // contain the right username
        // example: when we create item via uploading CSV file,
        // 1. we will save the username in the main thread
        // 2. in a separate thread, we will create the item according to the
        //    csv file and setup the item's create by with the username from
        //    the main thread
        // 3. we will fetch the right username here(who upload the file) and use
        //    it to send alert
        String username = item.getCreatedBy();

        Item newItem = itemRepository.save(item);
        sendAlertForItem(item, newItemFlag,
                Strings.isBlank(username) ? newItem.getCreatedBy() : username);

        return newItem;
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

    public List<Item> loadItemData(Long warehouseId, File  file) throws IOException {
        List<ItemCSVWrapper> itemCSVWrappers = loadData(file);
        return itemCSVWrappers.stream()
                .map(itemCSVWrapper -> convertFromWrapper(warehouseId, itemCSVWrapper)).collect(Collectors.toList());
    }

    public List<Item> saveItemData(Long warehouseId,
                                   File  file) throws IOException {
        List<Item> items = loadItemData(warehouseId, file);
        return items.stream().map(this::saveOrUpdate).collect(Collectors.toList());
    }

    public List<ItemCSVWrapper> loadData(File file) throws IOException {


        return fileService.loadData(file,  ItemCSVWrapper.class);
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

    /**
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
     **/

    private Item convertFromWrapper(Long warehouseId,
                                    ItemCSVWrapper itemCSVWrapper) {
        String username = "";
        try {
            username = userService.getCurrentUserName();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            logger.debug("We got error while getting username from the session, let's just ignore.\nerror: {}",
                    ex.getMessage());
        }
        return convertFromWrapper(warehouseId, itemCSVWrapper,
                username);

    }
    private Item convertFromWrapper(Long warehouseId,
                                    ItemCSVWrapper itemCSVWrapper,
                                    String username) {
        Item item = new Item();
        BeanUtils.copyProperties(itemCSVWrapper, item);
        item.setCreatedBy(username);
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
        // warehouse
        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId);
        item.setCompanyId(warehouse.getCompanyId());

        item.setWarehouseId(warehouse.getId());

        if (Strings.isNotBlank(itemCSVWrapper.getClient())) {
            Client client = commonServiceRestemplateClient.getClientByName(
                    warehouse.getId(),itemCSVWrapper.getClient());
            item.setClientId(client.getId());
        }
        if (Strings.isNotBlank(itemCSVWrapper.getItemFamily())) {

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
/**
        if(Objects.isNull(item.getItemFamily().getId())) {
            item.setItemFamily(getOrCreateItemFamily(item.getItemFamily()));
        }
 **/
        item.getItemPackageTypes().forEach(itemPackageType -> {
            itemPackageType.setItem(item);
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

        List<Inventory> inventories = inventoryService.
                findAll(item.getWarehouseId(), null, item.getName(), null, null,
                        null,null,null,null,null, null,
                        null,null,null,null,null,
                        null,null,null,null, null, null,null,null, null,
                        null,null,null,null,null,
                        null,null,null,
                        false, null, null )
                .stream().filter(
                        existingInventory -> Objects.equals(item.getClientId(), existingInventory.getClientId())
                ).collect(Collectors.toList());
        if (inventories.size() > 0) {
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

    public List<Item> findByKeyword(Long companyId,  Long warehouseId, String keyword, Boolean loadDetails,
                                    ClientRestriction clientRestriction) {
        // query by name with wildcard first
        logger.debug("Start to query item by keyword: {}", keyword);

        logger.debug("start to get item by name equals to the keyword");
        List<Item> items = findAll(companyId, warehouseId,
                "*" + keyword + "*",null,  null,null, null,null,
                null,null, null, loadDetails, clientRestriction);
        // query by description
        logger.debug("start to get item by description equals to the keyword");
        items.addAll(findAll(companyId, warehouseId, null,null,null,null, null,null,
                null,null, keyword, loadDetails, clientRestriction));

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

    public Item createItem(Long warehouseId, ItemPackageTypeCSVWrapper itemPackageTypeCSVWrapper,
                           String username) {
        ItemCSVWrapper itemCSVWrapper = new ItemCSVWrapper();
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

        itemCSVWrapper.setReceivingRateByUnit(
                itemPackageTypeCSVWrapper.getReceivingRateByUnit()
        );
        itemCSVWrapper.setShippingRateByUnit(
                itemPackageTypeCSVWrapper.getShippingRateByUnit()
        );
        itemCSVWrapper.setHandlingRateByUnit(
                itemPackageTypeCSVWrapper.getHandlingRateByUnit()
        );

        return saveOrUpdate(convertFromWrapper(warehouseId, itemCSVWrapper, username));

    }

    public String uploadItemData(Long warehouseId,
                                      File file) throws IOException {

        String username = userService.getCurrentUserName();

        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();
        // before we add new
        clearFileUploadMap();
        fileUploadProgressMap.put(fileUploadProgressKey, 0.0);
        fileUploadResultsMap.put(fileUploadProgressKey, new ArrayList<>());

        List<ItemCSVWrapper> itemCSVWrappers = loadData(file);
        itemCSVWrappers.forEach(
                itemCSVWrapper -> itemCSVWrapper.trim()
        );

        fileUploadProgressMap.put(fileUploadProgressKey, 10.0);

        logger.debug("get {} record from the file", itemCSVWrappers.size());

        // start a new thread to process the inventory
        new Thread(() -> {
            // loop through each inventory
            int totalItemCount = itemCSVWrappers.size();
            int index = 0;
            for (ItemCSVWrapper itemCSVWrapper : itemCSVWrappers) {
                // in case anything goes wrong, we will continue with the next record
                // and save the result with error message to the result set
                fileUploadProgressMap.put(fileUploadProgressKey, 10.0 +  (90.0 / totalItemCount) * (index));
                try {

                    saveOrUpdate(convertFromWrapper(warehouseId, itemCSVWrapper));
                    // we complete this inventory
                    fileUploadProgressMap.put(fileUploadProgressKey, 10.0 + (90.0 / totalItemCount) * (index + 1));

                    List<FileUploadResult> fileUploadResults = fileUploadResultsMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            itemCSVWrapper.toString(),
                            "success", ""
                    ));
                    fileUploadResultsMap.put(fileUploadProgressKey, fileUploadResults);

                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process item upload file record: {}, \n error message: {}",
                            itemCSVWrapper,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = fileUploadResultsMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            itemCSVWrapper.toString(),
                            "fail", ex.getMessage()
                    ));
                    fileUploadResultsMap.put(fileUploadProgressKey, fileUploadResults);
                }
                finally {

                    index++;
                }
            }
            // after we process all inventory, mark the progress to 100%
            fileUploadProgressMap.put(fileUploadProgressKey, 100.0);
        }).start();

        return fileUploadProgressKey;
    }

    private void clearFileUploadMap() {

        if (fileUploadProgressMap.size() > FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = fileUploadProgressMap.keySet().iterator();
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

        if (fileUploadResultsMap.size() > FILE_UPLOAD_MAP_SIZE_THRESHOLD) {
            // start to clear the date that is already 1 hours old. The file upload should not
            // take more than 1 hour
            Iterator<String> iterator = fileUploadResultsMap.keySet().iterator();
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



    public double getFileUploadProgress(String key) {
        return fileUploadProgressMap.getOrDefault(key, 100.0);
    }

    public List<FileUploadResult> getFileUploadResult(Long warehouseId, String key) {
        return fileUploadResultsMap.getOrDefault(key, new ArrayList<>());
    }

    public Item getLastItemFromSiloLocation(Long warehouseId, String locationName, Boolean loadDetails) {
        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(warehouseId, locationName);
        if (Objects.isNull(location)) {
            logger.debug("can't find WMS location for silo name {}", locationName);
            return null;
        }
        logger.debug("Start to find existing inventory from location {}",
                locationName);
        // let's get the inventory currently in the location
        List<Inventory> inventories = inventoryService.findByLocationId(
                location.getId(), loadDetails
        );
        if (inventories.isEmpty()) {
            logger.debug("There's no inventory in the silo location {}", locationName);
            return null;
        }
        logger.debug("find {} existing inventory in the location {}",
                inventories.size(), locationName);
        // ok , there's inventory in the silo location, let's get the latest one based on the inventory activity
        List<InventoryActivity> latestInventoryActivityList =
                inventories.stream().map(Inventory::getItem).distinct()
                .map(item -> inventoryActivityService.findLatestActivity(location.getId(), item))
                        .filter(inventoryActivity -> Objects.nonNull(inventoryActivity))
                        .collect(Collectors.toList());

        Collections.sort(latestInventoryActivityList,
                (o1, o2) -> o2.getActivityDateTime().compareTo(o1.getActivityDateTime())) ;

        if (latestInventoryActivityList.isEmpty()) {
            logger.debug("Fail to get the latest activity from location {}",
                    locationName);
            return null;
        }
        else {
            logger.debug("Get the latest activity from location {}, for item {}, at {}",
                    locationName,
                    latestInventoryActivityList.get(0).getItem().getName(),
                    latestInventoryActivityList.get(0).getActivityDateTime());
            return latestInventoryActivityList.get(0).getItem();

        }
    }

    /**
     * Send alert for new item or changing item
     * @param item
     */
    private void sendAlertForItem(Item item, boolean newItemFlag, String username) {

        if (Strings.isBlank(username)) {

            try {
                username = userService.getCurrentUserName();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                logger.debug("We got error while getting username from the session, let's just ignore.\nerror: {}",
                        ex.getMessage());
            }

        }

        Long companyId = Objects.nonNull(item.getCompanyId()) ?
                item.getCompanyId() :
                Objects.nonNull(item.getWarehouseId()) ?
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(item.getWarehouseId()).getCompanyId()
                    : null;
        StringBuilder alertParameters = new StringBuilder();
        alertParameters.append("name=").append(item.getName()) ;

        if (newItemFlag) {

            Alert alert = new Alert(companyId,
                    AlertType.NEW_ITEM,
                    "NEW-ITEM-" +
                            (Objects.nonNull(companyId) ? companyId : "")
                            + "-" + (Objects.nonNull(item.getWarehouseId()) ?  item.getWarehouseId() : "")
                            + "-" + item.getName(),
                    "Item " + item.getName() + " created, by " + username,
                    "", alertParameters.toString());
            kafkaSender.send(alert);
        }
        else {

            Alert alert = new Alert(companyId,
                    AlertType.MODIFY_RECEIPT,
                    "MODIFY-ITEM-" +
                        (Objects.nonNull(companyId) ? companyId : "")
                        + "-" + (Objects.nonNull(item.getWarehouseId()) ?  item.getWarehouseId() : "")
                        + "-" + item.getName(),
                    "Item " + item.getName() + " is changed, by " + username,
                    "", alertParameters.toString());
            kafkaSender.send(alert);
        }
    }

    public File getThumbnail(Long id) {

        Item item = findById(id);
        if (Strings.isBlank(item.getThumbnailUrl())) {
            throw ItemException.raiseException("The item " + item.getName() + " doesn't have a thumbnail yet");
        }

        String thumbnailDestination =  uploadFolder +  item.getThumbnailUrl();
        logger.debug("Will get thumbnail file from {}", thumbnailDestination);
        return new File(thumbnailDestination);
    }

    public File getImage(Long id) {

        Item item = findById(id);
        if (Strings.isBlank(item.getImageUrl())) {
            throw ItemException.raiseException("The item " + item.getName() + " doesn't have a image yet");
        }

        String imageDestination =  uploadFolder +  item.getImageUrl();
        logger.debug("Will get image file from {}", imageDestination);
        return new File(imageDestination);
    }

    public File getWorkOrderSOP(Long id) {
        Item item = findById(id);
        if (Strings.isBlank(item.getWorkOrderSOPUrl())) {
            throw ItemException.raiseException("The item " + item.getName() + " doesn't have a work order SOP yet");
        }

        String workOrderSOPDestination =  uploadFolder +  item.getWorkOrderSOPUrl();
        logger.debug("Will get work order SOP file from {}", workOrderSOPDestination);
        return new File(workOrderSOPDestination);
    }


    public Item uploadItemWorkOrderSOP(Long id, MultipartFile file) throws IOException {
        Item item = findById(id);
        logger.debug("Start to save item image: name: {} original fle name:  {} , content type: {}",
                file.getName(), file.getOriginalFilename(), file.getContentType());


        String newFileName  = item.getName() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String workOrderSOPDestination =   uploadFolder + workOrderSOPFolder  + item.getWarehouseId() + "/" + item.getId() + "/" + newFileName;


        logger.debug("start to save item {}'s work order SOP to destination: {}",
                item.getName(), workOrderSOPDestination);
        fileService.saveFile(file, workOrderSOPDestination);

        logger.debug("item {}'s work order SOP is saved to destination: {}",
                item.getName(), workOrderSOPDestination);
        item.setWorkOrderSOPUrl(workOrderSOPFolder  + item.getWarehouseId() + "/" + item.getId() + "/" + newFileName);


        return saveOrUpdate(item);

    }

    /**
     * Add item barcode to the item
     * @param id
     * @param warehouseId
     * @param itemBarcode
     * @return
     */
    public ItemBarcode addItemBarcode(Long id, Long warehouseId, ItemBarcode itemBarcode) {
        itemBarcode.setWarehouseId(warehouseId);

        Item item = findById(id, false);
        itemBarcode.setItem(item);

        return itemBarcodeService.save(itemBarcode);
    }

    /**
     * Find item by barcode
     * barcode can be an item name or item barcode
     * @param companyId
     * @param warehouseId
     * @param barcode
     * @param loadDetails
     * @param clientRestriction
     * @return
     */
    public List<Item> findByBarcode(Long companyId, Long warehouseId, String barcode, Boolean loadDetails, ClientRestriction clientRestriction) {
        List<Item> items = findAll(companyId,
                warehouseId, barcode, null,null,
                null,null,null,null,null,null,
          loadDetails, clientRestriction);

        items.addAll(
                itemBarcodeService.findAll(warehouseId, null, null, barcode).stream().map(
                        itemBarcode -> itemBarcode.getItem()
                ).collect(Collectors.toSet())
        );

        return items;
    }

    /**
     * Mark the item as kit item
     * @param warehouseId
     * @param id
     * @param billOfMaterialId
     * @return
     */
    public Item createKitItem(Long warehouseId, Long id,
                              Long billOfMaterialId) {
        Item item = findById(id, false);

        item.setBillOfMaterialId(billOfMaterialId);
        item.setKitItemFlag(true);

        return saveOrUpdate(item);
    }
}
