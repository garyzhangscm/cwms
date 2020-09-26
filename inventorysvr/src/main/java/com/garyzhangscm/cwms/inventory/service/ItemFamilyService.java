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
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ItemException;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.ItemFamilyRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ItemFamilyService implements TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(ItemFamilyService.class);

    @Autowired
    private ItemFamilyRepository itemFamilyRepository;
    @Autowired
    private ItemService itemService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.item-families:item_families}")
    String testDataFile;

    public ItemFamily findById(Long id) {
        return itemFamilyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("item family not found by id: " + id));
    }

    public List<ItemFamily> findAll(Long warehouseId, String name) {
        return findAll(warehouseId, name, true);
    }
    public List<ItemFamily> findAll(Long warehouseId, String name, boolean loadAttributes) {


        List<ItemFamily> itemFamilies = itemFamilyRepository.findAll(
                (Root<ItemFamily> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));


                    if (StringUtils.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }




                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (itemFamilies.size() > 0 && loadAttributes) {
            loadAttributes(itemFamilies);
        }
        return itemFamilies;

    }

    private void loadAttributes(List<ItemFamily> itemFamilies) {
        itemFamilies.forEach(itemFamily -> loadAttributes(itemFamily));
    }

    private void loadAttributes(ItemFamily itemFamily) {
        itemFamily.setTotalItemCount(
                itemFamilyRepository.getItemCount(itemFamily.getId())
        );
    }

    public ItemFamily findByName(Long warehouseId, String name){
        return itemFamilyRepository.findByWarehouseIdAndName(warehouseId, name);
    }

    public ItemFamily save(ItemFamily itemFamily) {
        return itemFamilyRepository.save(itemFamily);
    }

    public ItemFamily saveOrUpdate(ItemFamily itemFamily) {
        if (itemFamily.getId() == null && findByName(itemFamily.getWarehouseId(), itemFamily.getName()) != null) {
            itemFamily.setId(findByName(itemFamily.getWarehouseId(),itemFamily.getName()).getId());
        }
        return save(itemFamily);
    }
    public void delete(ItemFamily itemFamily) {
        itemFamilyRepository.delete(itemFamily);
    }
    public void delete(Long id) {
        itemFamilyRepository.deleteById(id);
    }

    public void delete(String itemFamilyIds) {
        if (!itemFamilyIds.isEmpty()) {
            long[] itemFamilyIdArray = Arrays.asList(itemFamilyIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : itemFamilyIdArray) {
                delete(id);
            }
        }

    }


    public List<ItemFamilyCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                build().withHeader();
        return fileService.loadData(file, schema, ItemFamilyCSVWrapper.class);
    }
    public List<ItemFamilyCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ItemFamilyCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ItemFamilyCSVWrapper> itemFamilyCSVWrappers = loadData(inputStream);
            itemFamilyCSVWrappers.stream().forEach(itemFamilyCSVWrapper -> saveOrUpdate(convertFromWrapper(itemFamilyCSVWrapper)));
            itemFamilyRepository.flush();
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private ItemFamily convertFromWrapper(ItemFamilyCSVWrapper itemFamilyCSVWrapper) {
        ItemFamily itemFamily = new ItemFamily();
        itemFamily.setName(itemFamilyCSVWrapper.getName());
        itemFamily.setDescription(itemFamilyCSVWrapper.getDescription());
        logger.debug(">>   Start to save item family: {}", itemFamilyCSVWrapper.getDescription());

        // warehouse
        if (!StringUtils.isBlank(itemFamilyCSVWrapper.getWarehouse())) {
            Warehouse warehouse =
                    warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                            itemFamilyCSVWrapper.getCompany(),
                            itemFamilyCSVWrapper.getWarehouse());
            if (warehouse != null) {
                itemFamily.setWarehouseId(warehouse.getId());
            }
        }
        return itemFamily;
    }

    public void removeItemFamilies(String itemFamilyIds) {
        // make sure there's no item related to this family
        long totalItemCount = Arrays.stream(itemFamilyIds.split(","))
                .map(Long::parseLong)
                .filter(itemFamilyId -> itemFamilyRepository.getItemCount(itemFamilyId) > 0)
                .count();

        if (totalItemCount > 0) {
            // we have some item family that still have existing item
            throw ItemException.raiseException("Can't remove the item family. There's still item belong to this family");
        }
        delete(itemFamilyIds);
    }
}
