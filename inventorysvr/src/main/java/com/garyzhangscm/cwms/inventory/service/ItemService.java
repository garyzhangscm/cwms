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
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.ItemRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

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
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.items:items}")
    String testDataFile;

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

    public List<Item> findAll(boolean includeDetails) {
        List<Item> items = itemRepository.findAll();
        if (items.size() > 0 && includeDetails)
            loadAttribute(items);
        return items;
    }

    public List<Item> findAll() {
        return findAll(true);
    }

    public List<Item> findAll(Long warehouseId,
                              String name,
                              String clientIds,
                              String itemFamilyIds) {

        List<Item> items = itemRepository.findAll(
            (Root<Item> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();

                predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                if (StringUtils.isNotBlank(itemFamilyIds)) {

                    Join<Item, ItemFamily> joinItemFamily = root.join("itemFamily", JoinType.INNER);
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(joinItemFamily.get("id"));
                    for(String id : itemFamilyIds.split(",")) {
                        in.value(Long.parseLong(id));
                    }
                    predicates.add(criteriaBuilder.and(in));
                }

                if (StringUtils.isNotBlank(name)) {
                    predicates.add(criteriaBuilder.equal(root.get("name"), name));
                }

                if (StringUtils.isNotBlank(clientIds)) {
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(root.get("clientId"));
                    for(String id : clientIds.split(",")) {
                        in.value(Long.parseLong(id));
                    }
                    predicates.add(criteriaBuilder.and(in));
                }


                Predicate[] p = new Predicate[predicates.size()];
                return criteriaBuilder.and(predicates.toArray(p));
            }
        );

        if (items.size() > 0) {
            loadAttribute(items);
        }
        return items;
    }

    private void loadAttribute(List<Item> items) {
        items.stream().forEach(this::loadAttribute);
    }

    private void loadAttribute(Item item) {

        if (item.getClientId() != null && item.getClient() == null) {
            item.setClient(commonServiceRestemplateClient.getClientById(item.getClientId()));
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


    public Item findByName(Long warehouseId, String name, boolean includeDetails){
        Item item = itemRepository.findByWarehouseIdAndName(warehouseId, name);
        if (item != null && includeDetails) {
            loadAttribute(item);
        }
        return item;
    }
    public Item findByName(Long warehouseId, String name){
        return findByName(warehouseId, name, true);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public Item saveOrUpdate(Item item) {
        if (item.getId() == null && findByName(item.getWarehouseId(), item.getName()) != null) {
            item.setId(findByName(item.getWarehouseId(), item.getName()).getId());
        }
        return save(item);
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
        return itemCSVWrappers.stream().map(itemCSVWrapper -> convertFromWrapper(itemCSVWrapper)).collect(Collectors.toList());
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
                build().withHeader();
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
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
        item.setName(itemCSVWrapper.getName());
        item.setDescription(itemCSVWrapper.getDescription());
        item.setUnitCost(itemCSVWrapper.getUnitCost());
        item.setAllowCartonization(itemCSVWrapper.getAllowCartonization());


        item.setTrackingVolumeFlag(itemCSVWrapper.isTrackingVolumeFlag());
        item.setTrackingLotNumberFlag(itemCSVWrapper.isTrackingLotNumberFlag());
        item.setTrackingManufactureDateFlag(itemCSVWrapper.isTrackingManufactureDateFlag());
        item.setTrackingExpirationDateFlag(itemCSVWrapper.isTrackingExpirationDateFlag());
        item.setShelfLifeDays(itemCSVWrapper.getShelfLifeDays());

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
        return item;

    }


    public Item addItem(Item item) {
        item.getItemPackageTypes().forEach(itemPackageType -> {
            itemPackageType.setItem(item);
            itemPackageType.getItemUnitOfMeasures().forEach(
                    itemUnitOfMeasure -> itemUnitOfMeasure.setItemPackageType(
                            itemPackageType
                    )
            );

        });

        return  saveOrUpdate(item);
    }

    public Item changeItem(Long id, Item item) {
        Item existingItem = findById(id);

        BeanUtils.copyProperties(item, existingItem, "id", "itemPackageTypes");

        copyItemPackageTypes(existingItem, item);

        return  saveOrUpdate(item);




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
                                    "itemUnitOfMeasures");
                            copyItemUnitOfMeasure(existingItemPackageType, itemPackageType);
                        });
            }
            else {
                // OK, this is a new item package type, let's just add it to the
                // item
                existingItem.addItemPackageType(itemPackageType);
            }
        });

        // Remove the item package type that is no long exists
        Iterator<ItemPackageType> itemPackageTypeIterator = existingItem.getItemPackageTypes().iterator();
        while(itemPackageTypeIterator.hasNext()) {
            ItemPackageType existingItemPackageType = itemPackageTypeIterator.next();
            boolean itemPackageTypeStillExists =
                    item.getItemPackageTypes().stream().filter(
                            itemPackageType -> existingItemPackageType.getId().equals(
                                    itemPackageType.getId()
                            )
                    ).count() > 0;
            if (!itemPackageTypeStillExists) {
                itemPackageTypeIterator.remove();
            }
        }

    }

    private void copyItemUnitOfMeasure(ItemPackageType existingItemPackageType, ItemPackageType itemPackageType) {

    }
}
