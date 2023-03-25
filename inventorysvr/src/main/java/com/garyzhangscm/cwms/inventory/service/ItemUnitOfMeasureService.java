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
import com.garyzhangscm.cwms.inventory.repository.ItemUnitOfMeasureRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ItemUnitOfMeasureService {

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

    @Autowired
    private UserService userService;

    private final static int FILE_UPLOAD_MAP_SIZE_THRESHOLD = 20;
    private Map<String, Double> fileUploadProgressMap = new ConcurrentHashMap<>();
    private Map<String, List<FileUploadResult>> fileUploadResultsMap = new ConcurrentHashMap<>();

    public ItemUnitOfMeasure findById(Long id) {
        return itemUnitOfMeasureRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("item unit of measure not found by id: " + id));
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

        return fileService.loadData(file,  ItemUnitOfMeasureCSVWrapper.class);
    }
    public List<ItemUnitOfMeasureCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("client").
                addColumn("item").
                addColumn("itemDescription").
                addColumn("itemFamily").
                addColumn("trackingColorFlag").
                addColumn("defaultColor").
                addColumn("trackingProductSizeFlag").
                addColumn("defaultProductSize").
                addColumn("trackingStyleFlag").
                addColumn("defaultStyle").
                addColumn("itemPackageType").
                // only needed if the item package type doesn't exists and we need to
                // create one
                addColumn("itemPackageTypeDescription").
                // only needed if the item package type doesn't exists and we need to
                // create one
                addColumn("defaultItemPackageType").
                addColumn("unitOfMeasure").
                addColumn("quantity").
                addColumn("weight").
                addColumn("length").
                addColumn("width").
                addColumn("height").
                        addColumn("defaultForInboundReceiving").
                        addColumn("defaultForWorkOrderReceiving").
                        addColumn("trackingLpn").
                        addColumn("caseFlag").
                        addColumn("defaultForDisplay").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ItemUnitOfMeasureCSVWrapper.class);
    }

    /**
    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            logger.debug(">> Start to get item unit of measure from {}", testDataFileName);
            List<ItemUnitOfMeasureCSVWrapper> itemUnitOfMeasureCSVWrappers = loadData(inputStream);
            itemUnitOfMeasureCSVWrappers.stream().forEach(
                    itemUnitOfMeasureCSVWrapper ->
                        saveOrUpdate(convertFromWrapper(itemUnitOfMeasureCSVWrapper)));
            itemUnitOfMeasureRepository.flush();
            logger.debug(">> item unit of measure loaded from {}", testDataFile);
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
     **/

    private ItemUnitOfMeasure convertFromWrapper(Long warehouseId,
                                                 ItemUnitOfMeasureCSVWrapper itemUnitOfMeasureCSVWrapper,
                                                 boolean autoCreateItemPackageType,
                                                 String username) {
        logger.debug("===>Start to create item unit of measure with \n item: {}, packate type: {}",
                itemUnitOfMeasureCSVWrapper.getItem(), itemUnitOfMeasureCSVWrapper.getItemPackageType());
        ItemUnitOfMeasure itemUnitOfMeasure = new ItemUnitOfMeasure();
        itemUnitOfMeasure.setQuantity(itemUnitOfMeasureCSVWrapper.getQuantity());
        itemUnitOfMeasure.setWeight(itemUnitOfMeasureCSVWrapper.getWeight());
        itemUnitOfMeasure.setLength(itemUnitOfMeasureCSVWrapper.getLength());
        itemUnitOfMeasure.setWidth(itemUnitOfMeasureCSVWrapper.getWidth());
        itemUnitOfMeasure.setHeight(itemUnitOfMeasureCSVWrapper.getHeight());

        // warehouse  is mandate
        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId);

        Client client = null;
        if (Strings.isNotBlank(itemUnitOfMeasureCSVWrapper.getClient())) {
            client = commonServiceRestemplateClient.getClientByName(
                    warehouseId, itemUnitOfMeasureCSVWrapper.getClient()
            );
        }
        itemUnitOfMeasure.setCompanyId(warehouse.getCompanyId());
        itemUnitOfMeasure.setWarehouseId(warehouse.getId());


        itemUnitOfMeasure.setDefaultForInboundReceiving(
                Strings.isBlank(itemUnitOfMeasureCSVWrapper.getDefaultForInboundReceiving()) ?
                        false :
                        itemUnitOfMeasureCSVWrapper.getDefaultForInboundReceiving().trim().equals("1") ||
                                itemUnitOfMeasureCSVWrapper.getDefaultForInboundReceiving().trim().equalsIgnoreCase("true")
        );

        itemUnitOfMeasure.setDefaultForWorkOrderReceiving(
                Strings.isBlank(itemUnitOfMeasureCSVWrapper.getDefaultForWorkOrderReceiving()) ?
                        false :
                        itemUnitOfMeasureCSVWrapper.getDefaultForWorkOrderReceiving().trim().equals("1") ||
                                itemUnitOfMeasureCSVWrapper.getDefaultForWorkOrderReceiving().trim().equalsIgnoreCase("true")
        );
        itemUnitOfMeasure.setTrackingLpn(
                Strings.isBlank(itemUnitOfMeasureCSVWrapper.getTrackingLpn()) ?
                        false :
                        itemUnitOfMeasureCSVWrapper.getTrackingLpn().trim().equals("1") ||
                                itemUnitOfMeasureCSVWrapper.getTrackingLpn().trim().equalsIgnoreCase("true")
        );
        itemUnitOfMeasure.setCaseFlag(
                Strings.isBlank(itemUnitOfMeasureCSVWrapper.getCaseFlag()) ?
                        false :
                        itemUnitOfMeasureCSVWrapper.getCaseFlag().trim().equals("1") ||
                                itemUnitOfMeasureCSVWrapper.getCaseFlag().trim().equalsIgnoreCase("true")
        );

        itemUnitOfMeasure.setDefaultForDisplay(
                Strings.isBlank(itemUnitOfMeasureCSVWrapper.getDefaultForDisplay()) ?
                        false :
                        itemUnitOfMeasureCSVWrapper.getDefaultForDisplay().trim().equals("1") ||
                                itemUnitOfMeasureCSVWrapper.getDefaultForDisplay().trim().equalsIgnoreCase("true")
        );

        if (Strings.isNotBlank(itemUnitOfMeasureCSVWrapper.getUnitOfMeasure())) {
            UnitOfMeasure unitOfMeasure =
                    commonServiceRestemplateClient.getUnitOfMeasureByName(
                            warehouse.getId(), itemUnitOfMeasureCSVWrapper.getUnitOfMeasure());
            itemUnitOfMeasure.setUnitOfMeasureId(unitOfMeasure.getId());
        }
        if (Strings.isNotBlank(itemUnitOfMeasureCSVWrapper.getItem()) &&  Strings.isNotBlank(itemUnitOfMeasureCSVWrapper.getItemPackageType())) {
            ItemPackageType itemPackageType = itemPackageTypeService.findByNaturalKeys(
                    warehouse.getId(),
                    Objects.isNull(client) ? null : client.getId(),
                    itemUnitOfMeasureCSVWrapper.getItem(),
                    itemUnitOfMeasureCSVWrapper.getItemPackageType() );
            // if the item package type is not created yet, let's create the
            // item package type first. We allow the user to create the item package type
            // when load item unit of measure from CSV files
            if (Objects.isNull(itemPackageType) && autoCreateItemPackageType) {
                itemPackageType = createItemPackageType(warehouseId, itemUnitOfMeasureCSVWrapper, username);

            }
            itemUnitOfMeasure.setItemPackageType(itemPackageType);
        }
        return itemUnitOfMeasure;
    }

    private ItemPackageType createItemPackageType(Long warehouseId,
                                                  ItemUnitOfMeasureCSVWrapper itemUnitOfMeasureCSVWrapper,
                                                  String username) {

        return itemPackageTypeService.createItemPackageType(warehouseId,
                itemUnitOfMeasureCSVWrapper, username);
    }


    public String uploadItemUnitOfMeasureData(Long warehouseId,
                                 File file) throws IOException {

        String username = userService.getCurrentUserName();

        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();
        // before we add new
        clearFileUploadMap();
        fileUploadProgressMap.put(fileUploadProgressKey, 0.0);
        fileUploadResultsMap.put(fileUploadProgressKey, new ArrayList<>());

        List<ItemUnitOfMeasureCSVWrapper> itemUnitOfMeasureCSVWrappers = loadData(file);


        fileUploadProgressMap.put(fileUploadProgressKey, 10.0);

        logger.debug("get {} record from the file", itemUnitOfMeasureCSVWrappers.size());

        // start a new thread to process the inventory
        new Thread(() -> {
            // loop through each inventory
            int totalCount = itemUnitOfMeasureCSVWrappers.size();
            int index = 0;
            for (ItemUnitOfMeasureCSVWrapper itemUnitOfMeasureCSVWrapper : itemUnitOfMeasureCSVWrappers) {
                // in case anything goes wrong, we will continue with the next record
                // and save the result with error message to the result set
                fileUploadProgressMap.put(fileUploadProgressKey, 10.0 +  (90.0 / totalCount) * (index));
                try {
                    saveOrUpdate(convertFromWrapper(warehouseId, itemUnitOfMeasureCSVWrapper, true, username));
                    // we complete this inventory
                    fileUploadProgressMap.put(fileUploadProgressKey, 10.0 + (90.0 / totalCount) * (index + 1));

                    List<FileUploadResult> fileUploadResults = fileUploadResultsMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            itemUnitOfMeasureCSVWrapper.toString(),
                            "success", ""
                    ));
                    fileUploadResultsMap.put(fileUploadProgressKey, fileUploadResults);

                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process item unit of measure upload file record: {}, \n error message: {}",
                            itemUnitOfMeasureCSVWrapper,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = fileUploadResultsMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            itemUnitOfMeasureCSVWrapper.toString(),
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

}
