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
import com.garyzhangscm.cwms.inventory.exception.MissingInformationException;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.ItemBarcodeRepository;
import com.garyzhangscm.cwms.inventory.repository.ItemBarcodeTypeRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 *
 */
@Service
public class ItemBarcodeService {
    private static final Logger logger = LoggerFactory.getLogger(ItemBarcodeService.class);

    @Autowired
    private ItemBarcodeRepository itemBarcodeRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private FileService fileService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private ItemBarcodeTypeService itemBarcodeTypeService;

    private final static int FILE_UPLOAD_MAP_SIZE_THRESHOLD = 20;
    private Map<String, Double> fileUploadProgressMap = new ConcurrentHashMap<>();
    private Map<String, List<FileUploadResult>> fileUploadResultsMap = new ConcurrentHashMap<>();


    public ItemBarcode findById(Long id) {
        return itemBarcodeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Item Barcode not found by id: " + id));}

    public List<ItemBarcode> findAll(Long warehouseId,
                                     Long itemId,
                                     String itemName) {
        return itemBarcodeRepository.findAll(
                (Root<ItemBarcode> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(itemId) || Strings.isNotBlank(itemName)) {

                        Join<ItemBarcode, Item> joinItem = root.join("item", JoinType.INNER);
                        if (Objects.nonNull(itemId)) {
                            predicates.add(criteriaBuilder.equal(joinItem.get("id"), itemId));
                        }
                        if (Strings.isNotBlank(itemName)) {
                            predicates.add(criteriaBuilder.equal(joinItem.get("name"), itemName));
                        }
                    }

                    Predicate[] p = new Predicate[predicates.size()];

                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.DESC, "name")
        );

    }


    @Transactional
    public ItemBarcode save(ItemBarcode itemBarcode) {
        return itemBarcodeRepository.save(itemBarcode);
    }
    public ItemBarcode saveOrUpdate(ItemBarcode itemBarcode) {

        if (Objects.isNull(itemBarcode.getId()) &&
                Objects.nonNull(findByCode(itemBarcode.getWarehouseId(), itemBarcode.getItem().getId(),
                        itemBarcode.getCode()))) {
            itemBarcode.setId(
                    findByCode(
                            itemBarcode.getWarehouseId(), itemBarcode.getItem().getId(),
                            itemBarcode.getCode()
                    ).getId()
            );
        }
        return save(itemBarcode);
    }
    public ItemBarcode findByCode(Long warehouseId, Long itemId, String code) {
        return itemBarcodeRepository.findByCode(warehouseId, itemId, code);
    }


    @Transactional
    public void delete(ItemBarcode itemBarcode) {
        itemBarcodeRepository.delete(itemBarcode);
    }
    @Transactional
    public void delete(Long id) {
        itemBarcodeRepository.deleteById(id);
    }

    public ItemBarcode changeItemBarcode(ItemBarcode itemBarcode) {
        return save(itemBarcode);
    }

    public ItemBarcode addItemBarcode(ItemBarcode itemBarcode) {
        return save(itemBarcode);
    }

    public String uploadItemBarcodeData(Long warehouseId, File file) throws IOException {
        String username = userService.getCurrentUserName();

        String fileUploadProgressKey = warehouseId + "-" + username + "-" + System.currentTimeMillis();
        // before we add new
        clearFileUploadMap();
        fileUploadProgressMap.put(fileUploadProgressKey, 0.0);
        fileUploadResultsMap.put(fileUploadProgressKey, new ArrayList<>());

        List<ItemBarcodeCSVWrapper> itemBarcodeCSVWrappers = loadData(file);
        itemBarcodeCSVWrappers.forEach(
                itemBarcodeCSVWrapper -> itemBarcodeCSVWrapper.trim()
        );

        fileUploadProgressMap.put(fileUploadProgressKey, 10.0);

        logger.debug("get {} record from the file", itemBarcodeCSVWrappers.size());

        // start a new thread to process the inventory
        new Thread(() -> {
            // loop through each inventory
            int totalItemBarcodeCount = itemBarcodeCSVWrappers.size();
            int index = 0;
            for (ItemBarcodeCSVWrapper itemBarcodeCSVWrapper : itemBarcodeCSVWrappers) {
                // in case anything goes wrong, we will continue with the next record
                // and save the result with error message to the result set
                fileUploadProgressMap.put(fileUploadProgressKey, 10.0 +  (90.0 / totalItemBarcodeCount) * (index));
                try {

                    saveOrUpdate(convertFromWrapper(warehouseId, itemBarcodeCSVWrapper));
                    // we complete this inventory
                    fileUploadProgressMap.put(fileUploadProgressKey, 10.0 + (90.0 / totalItemBarcodeCount) * (index + 1));

                    List<FileUploadResult> fileUploadResults = fileUploadResultsMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            itemBarcodeCSVWrapper.toString(),
                            "success", ""
                    ));
                    fileUploadResultsMap.put(fileUploadProgressKey, fileUploadResults);

                }
                catch(Exception ex) {

                    ex.printStackTrace();
                    logger.debug("Error while process item upload file record: {}, \n error message: {}",
                            itemBarcodeCSVWrapper,
                            ex.getMessage());
                    List<FileUploadResult> fileUploadResults = fileUploadResultsMap.getOrDefault(
                            fileUploadProgressKey, new ArrayList<>()
                    );
                    fileUploadResults.add(new FileUploadResult(
                            index + 1,
                            itemBarcodeCSVWrapper.toString(),
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

    public List<ItemBarcodeCSVWrapper> loadData(File file) throws IOException {


        return fileService.loadData(file,  ItemBarcodeCSVWrapper.class);
    }

    private ItemBarcode convertFromWrapper(Long warehouseId,
                                    ItemBarcodeCSVWrapper itemBarcodeCSVWrapper) {
        ItemBarcode itemBarcode = new ItemBarcode();
        itemBarcode.setWarehouseId(warehouseId);
        if (Strings.isBlank(itemBarcodeCSVWrapper.getName())) {
            throw MissingInformationException.raiseException("item name is required");
        }
        if (Strings.isBlank(itemBarcodeCSVWrapper.getCode())) {
            throw MissingInformationException.raiseException("item barcode is required");
        }
        itemBarcode.setCode(itemBarcodeCSVWrapper.getCode());

        Long clientId = null;

        if (Strings.isNotBlank(itemBarcodeCSVWrapper.getClient())) {
            Client client = commonServiceRestemplateClient.getClientByName(
                    warehouseId,itemBarcodeCSVWrapper.getClient());
            clientId = client.getId();
        }
        Item item = itemService.findByName(warehouseId, clientId,
                itemBarcodeCSVWrapper.getName(), false);
        if (Objects.isNull(item)) {
            throw MissingInformationException.raiseException("can't find item by client " +
                            (Strings.isBlank(itemBarcodeCSVWrapper.getClient()) ? "N/A" : itemBarcodeCSVWrapper.getClient()) +
                    ", item name " +
                    itemBarcodeCSVWrapper.getName());
        }
        itemBarcode.setItem(item);
        if (Strings.isNotBlank(itemBarcodeCSVWrapper.getType())) {
            ItemBarcodeType itemBarcodeType  = itemBarcodeTypeService.findByName(warehouseId,
                    itemBarcodeCSVWrapper.getType());
            if (Objects.isNull(itemBarcodeType)) {
                throw MissingInformationException.raiseException("can't find type by " +
                        itemBarcodeCSVWrapper.getType());
            }
            itemBarcode.setItemBarcodeType(itemBarcodeType);
        }

        return itemBarcode;

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