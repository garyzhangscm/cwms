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
import com.garyzhangscm.cwms.inventory.repository.ItemPackageTypeRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItemPackageTypeService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(ItemPackageTypeService.class);

    @Autowired
    private ItemPackageTypeRepository itemPackageTypeRepository;
    @Autowired
    private ItemService itemService;
    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.items:item_package_types}")
    String testDataFile;

    public ItemPackageType findById(Long id) {
         return itemPackageTypeRepository.findById(id)
                 .orElseThrow(() -> ResourceNotFoundException.raiseException("item package type not found by id: " + id));
    }

    public List<ItemPackageType> findAll(Long companyId,
                                         Long warehouseId,
                                         String name,
                                         Long itemId) {

        List<ItemPackageType> itemPackageTypes = itemPackageTypeRepository.findAll(
                (Root<ItemPackageType> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();


                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    if (StringUtils.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }

                    if (Objects.nonNull(itemId)) {

                        Join<ItemPackageType, Item> joinItem = root.join("item", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinItem.get("id"), itemId));


                    }


                    Predicate[] p = new Predicate[predicates.size()];

                    // special handling for warehouse id
                    // if warehouse id is passed in, then return both the warehouse level item
                    // and the company level item information.
                    // otherwise, return the company level item information
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));
                    if (Objects.nonNull(warehouseId)) {
                        return criteriaBuilder.and(predicate,
                                criteriaBuilder.or(
                                        criteriaBuilder.equal(root.get("warehouseId"), warehouseId),
                                        criteriaBuilder.isNull(root.get("warehouseId"))));
                    }
                    else  {
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
            removeDuplicatedItemPackageTypeRecords(itemPackageTypes);
        }
        return itemPackageTypes;
    }
    /**
     * Remove teh duplicated item package type record. If we have 2 record with the same item package type
     *  name and item name but different warehouse, then we will remove the one without any warehouse information
     * from the result
     * @param itemPackageTypes
     */
    private void removeDuplicatedItemPackageTypeRecords(List<ItemPackageType> itemPackageTypes) {
        Iterator<ItemPackageType> itemPackageTypeIterator = itemPackageTypes.listIterator();
        Set<String> itemPackageTypeProcessed = new HashSet<>();
        while(itemPackageTypeIterator.hasNext()) {
            ItemPackageType itemPackageType = itemPackageTypeIterator.next();

            String key = itemPackageType.getItem().getId() + "-" + itemPackageType.getName();
            if (itemPackageTypeProcessed.contains(key) &&
                    Objects.isNull(itemPackageType.getWarehouseId())) {
                // ok, we already processed the item and the current
                // record is a company level item, then we will remove
                // this record from the result
                itemPackageTypeIterator.remove();
            }
            itemPackageTypeProcessed.add(key);
        }
    }

    public ItemPackageType save(ItemPackageType itemPackageType) {
        return itemPackageTypeRepository.save(itemPackageType);
    }

    //item name, item package name, unit of measure id
    public ItemPackageType findByNaturalKeys(ItemPackageType itemPackageType) {
        // Natrual key: name,  item_id
        return itemPackageTypeRepository.findByNaturalKeys(itemPackageType.getWarehouseId(),
                itemPackageType.getName(), itemPackageType.getItem().getId());
    }

    public ItemPackageType findByNaturalKeys(Long warehouseId, String name, Long itemId) {
        // Natrual key: name,  item_id
        return itemPackageTypeRepository.findByNaturalKeys(warehouseId, name, itemId);
    }

    public ItemPackageType findByNaturalKeys(Long warehouseId, String name, String itemName) {
        // Natrual key: name,  item name
        return itemPackageTypeRepository.findByNaturalKeys(warehouseId, name, itemName);
    }

    // Natural Key: item &
    public ItemPackageType saveOrUpdate(ItemPackageType itemPackageType) {
        if (itemPackageType.getId() == null && findByNaturalKeys(itemPackageType) != null) {
            itemPackageType.setId(findByNaturalKeys(itemPackageType).getId());
        }
        return save(itemPackageType);
    }
    public void delete(ItemPackageType itemPackageType) {
        itemPackageTypeRepository.delete(itemPackageType);
    }
    public void delete(Long id) {
        itemPackageTypeRepository.deleteById(id);
    }


    public List<ItemPackageType> loadItemPackageTypeData(File  file) throws IOException {
        List<ItemPackageTypeCSVWrapper> itemPackageTypeCSVWrappers = loadData(file);
        return itemPackageTypeCSVWrappers.stream().map(itemPackageTypeCSVWrapper -> convertFromWrapper(itemPackageTypeCSVWrapper)).collect(Collectors.toList());
    }


    public List<ItemPackageTypeCSVWrapper> loadData(File file) throws IOException {
        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("item").
                addColumn("client").
                addColumn("supplier").
                addColumn("name").
                addColumn("description").
                build().withHeader();
        return fileService.loadData(file, schema, ItemPackageTypeCSVWrapper.class);
    }


    public List<ItemPackageTypeCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("item").
                addColumn("client").
                addColumn("supplier").
                addColumn("name").
                addColumn("description").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ItemPackageTypeCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ItemPackageTypeCSVWrapper> itemPackageTypeCSVWrappers = loadData(inputStream);
            itemPackageTypeCSVWrappers.stream().forEach(itemPackageTypeCSVWrapper -> saveOrUpdate(convertFromWrapper(itemPackageTypeCSVWrapper)));
            itemPackageTypeRepository.flush();
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private ItemPackageType convertFromWrapper(ItemPackageTypeCSVWrapper itemPackageTypeCSVWrapper) {
        ItemPackageType itemPackageType = new ItemPackageType();
        itemPackageType.setName(itemPackageTypeCSVWrapper.getName());
        itemPackageType.setDescription(itemPackageTypeCSVWrapper.getDescription());

        // warehouse is mandate
        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                        itemPackageTypeCSVWrapper.getCompany(),
                        itemPackageTypeCSVWrapper.getWarehouse());
        itemPackageType.setWarehouseId(warehouse.getId());

        if (!itemPackageTypeCSVWrapper.getClient().isEmpty()) {
            Client client = commonServiceRestemplateClient.getClientByName(warehouse.getId(), itemPackageTypeCSVWrapper.getClient());
            itemPackageType.setClientId(client.getId());
        }
        if (!itemPackageTypeCSVWrapper.getSupplier().isEmpty()) {
            Supplier supplier = commonServiceRestemplateClient.getSupplierByName(warehouse.getId(),itemPackageTypeCSVWrapper.getSupplier());
            itemPackageType.setSupplierId(supplier.getId());
        }
        if (!itemPackageTypeCSVWrapper.getItem().isEmpty()) {
            Item item = itemService.findByName(warehouse.getId(), itemPackageTypeCSVWrapper.getItem());
            itemPackageType.setItem(item);
        }
        return itemPackageType;

    }

    public boolean isItemPackageTypeRemovable(Long itemPackageTypeId) {
        logger.debug("Start to get item package type by id {}",
                itemPackageTypeId);
        ItemPackageType itemPackageType = findById(itemPackageTypeId);
        logger.debug("check if item package type  {} / {} / {} is removable",
                itemPackageType.getItem().getWarehouseId(),
                itemPackageType.getItem().getName(), itemPackageType.getName());
        return isItemPackageTypeRemovable(itemPackageType.getItem(), itemPackageType);
    }
    public boolean isItemPackageTypeRemovable(Item item, ItemPackageType itemPackageType) {
        // Check if we have inventory that is using this item package type

        if (inventoryService.
                findAll(item.getWarehouseId(), null, item.getName(), itemPackageType.getName(),
                null,null,null,null,null,
                null,null,null,null,
                null,null,null,null,null,null, null, false)
                  .size() > 0) {
            logger.debug("There's inventory attached to this item package type {} / {}, can't remove it",
                    item.getName(), itemPackageType.getName());

            return false;
        };

        logger.debug("There's NO inventory attached to this item package type {} / {}, WE CAN remove it",
                item.getName(), itemPackageType.getName());
        return true;
    }


}
