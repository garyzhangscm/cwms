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
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.ItemUnitOfMeasureRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class ItemUnitOfMeasureService implements TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(ItemUnitOfMeasureService.class);

    @Autowired
    private ItemUnitOfMeasureRepository itemUnitOfMeasureRepository;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private ItemPackageTypeService itemPackageTypeService;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.item-unit-of-measures:item_unit_of_measures}")
    String testDataFile;

    public ItemUnitOfMeasure findById(Long id) {
        return itemUnitOfMeasureRepository.findById(id).orElse(null);
    }

    public List<ItemUnitOfMeasure> findAll() {

        return itemUnitOfMeasureRepository.findAll();
    }
    public ItemUnitOfMeasure findByNaturalKeys(ItemUnitOfMeasure itemUnitOfMeasure) {
        // Natrual Keys: item name, item package name, unit of measure id
        // Natrual Keys: item id, item package name, unit of measure id
        // Natrual Keys: item package id, unit of measure id
        return itemUnitOfMeasureRepository.findByNaturalKeys(
                itemUnitOfMeasure.getWarehouseId(),
                itemUnitOfMeasure.getItemPackageType().getId(),
                itemUnitOfMeasure.getUnitOfMeasureId());
    }




    public ItemUnitOfMeasure save(ItemUnitOfMeasure itemUnitOfMeasure) {
        return itemUnitOfMeasureRepository.save(itemUnitOfMeasure);
    }
    public ItemUnitOfMeasure saveOrUpdate(ItemUnitOfMeasure itemUnitOfMeasure) {

        if (itemUnitOfMeasure.getId() == null && findByNaturalKeys(itemUnitOfMeasure) != null) {
            itemUnitOfMeasure.setId(findByNaturalKeys(itemUnitOfMeasure).getId());
        }
        return save(itemUnitOfMeasure);
    }

    public void delete(ItemUnitOfMeasure itemUnitOfMeasure) {
        itemUnitOfMeasureRepository.delete(itemUnitOfMeasure);
    }
    public void delete(Long id) {
        itemUnitOfMeasureRepository.deleteById(id);
    }

    public List<ItemUnitOfMeasureCSVWrapper> loadData(File file) throws IOException {
        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("item").
                addColumn("itemPackageType").
                addColumn("unitOfMeasure").
                addColumn("quantity").
                addColumn("weight").
                addColumn("length").
                addColumn("width").
                addColumn("height").
                build().withHeader();
        return fileService.loadData(file, schema, ItemUnitOfMeasureCSVWrapper.class);
    }
    public List<ItemUnitOfMeasureCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("item").
                addColumn("itemPackageType").
                addColumn("unitOfMeasure").
                addColumn("quantity").
                addColumn("weight").
                addColumn("length").
                addColumn("width").
                addColumn("height").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ItemUnitOfMeasureCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            logger.debug(">> Start to get item unit of measure from {}", testDataFileName);
            List<ItemUnitOfMeasureCSVWrapper> itemUnitOfMeasureCSVWrappers = loadData(inputStream);
            itemUnitOfMeasureCSVWrappers.stream().forEach(itemUnitOfMeasureCSVWrapper -> saveOrUpdate(convertFromWrapper(itemUnitOfMeasureCSVWrapper)));
            logger.debug(">> item unit of measure loaded from {}", testDataFile);
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private ItemUnitOfMeasure convertFromWrapper(ItemUnitOfMeasureCSVWrapper itemUnitOfMeasureCSVWrapper) {
        logger.debug("===>Start to create item unit of measure with \n item: {}, packate type: {}",
                itemUnitOfMeasureCSVWrapper.getItem(), itemUnitOfMeasureCSVWrapper.getItemPackageType());
        ItemUnitOfMeasure itemUnitOfMeasure = new ItemUnitOfMeasure();
        itemUnitOfMeasure.setQuantity(itemUnitOfMeasureCSVWrapper.getQuantity());
        itemUnitOfMeasure.setWeight(itemUnitOfMeasureCSVWrapper.getWeight());
        itemUnitOfMeasure.setLength(itemUnitOfMeasureCSVWrapper.getLength());
        itemUnitOfMeasure.setWidth(itemUnitOfMeasureCSVWrapper.getWidth());
        itemUnitOfMeasure.setHeight(itemUnitOfMeasureCSVWrapper.getHeight());

        // warehouse  is mandate
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(itemUnitOfMeasureCSVWrapper.getWarehouse());
        itemUnitOfMeasure.setWarehouseId(warehouse.getId());

        if (!itemUnitOfMeasureCSVWrapper.getUnitOfMeasure().isEmpty()) {
            UnitOfMeasure unitOfMeasure = commonServiceRestemplateClient.getUnitOfMeasureByName(itemUnitOfMeasureCSVWrapper.getUnitOfMeasure());
            itemUnitOfMeasure.setUnitOfMeasureId(unitOfMeasure.getId());
        }
        if (!(itemUnitOfMeasureCSVWrapper.getItem().isEmpty() || itemUnitOfMeasureCSVWrapper.getItemPackageType().isEmpty())) {
            ItemPackageType itemPackageType = itemPackageTypeService.findByNaturalKeys(
                    warehouse.getId(), itemUnitOfMeasureCSVWrapper.getItemPackageType(),
                    itemUnitOfMeasureCSVWrapper.getItem() );
            itemUnitOfMeasure.setItemPackageType(itemPackageType);
        }
        return itemUnitOfMeasure;
    }


}
