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

import com.garyzhangscm.cwms.inventory.exception.InventoryException;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.ItemSamplingRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItemSamplingService {
    private static final Logger logger = LoggerFactory.getLogger(ItemSamplingService.class);

    @Autowired
    private ItemSamplingRepository itemSamplingRepository;

    @Value("${item.qc.sampleImageFolder}")
    private String qcSampleImageFolder;

    @Autowired
    private FileService fileService;



    public ItemSampling findById(Long id) {
        ItemSampling itemSampling = itemSamplingRepository.findById(id)
                 .orElseThrow(() -> ResourceNotFoundException.raiseException("item sampling not found by id: " + id));
         return itemSampling;
    }


    public List<ItemSampling> findAll(Long warehouseId,
                                      String number,
                                      String itemName,
                                      Long itemId,
                                      Boolean enabled,
                                      Boolean currentSampleOnly) {

        return itemSamplingRepository.findAll(
            (Root<ItemSampling> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();

                predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));


                if (StringUtils.isNotBlank(number)) {
                    if (number.contains("%")) {
                        predicates.add(criteriaBuilder.like(root.get("number"), number));
                    }
                    else {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));
                    }
                }

                if (Objects.nonNull(itemId)) {

                    Join<ItemSampling, Item> joinItem = root.join("item", JoinType.INNER);

                    predicates.add(criteriaBuilder.equal(joinItem.get("id"), itemId));
                }
                else if (StringUtils.isNotBlank(itemName)) {

                    Join<ItemSampling, Item> joinItem = root.join("item", JoinType.INNER);

                    predicates.add(criteriaBuilder.equal(joinItem.get("name"), itemName));
                }
                if (Boolean.TRUE.equals(currentSampleOnly)) {

                    predicates.add(criteriaBuilder.equal(root.get("enabled"), true));
                }

                if (Objects.nonNull(enabled)) {

                    predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
                }


                Predicate[] p = new Predicate[predicates.size()];
                return criteriaBuilder.and(predicates.toArray(p));
            }
        );

    }



    public ItemSampling findByNumber(Long warehouseId, String number){
        return itemSamplingRepository.findByWarehouseIdAndNumber(warehouseId, number);
    }

    @Transactional
    public ItemSampling save(ItemSampling itemSampling) {
        return itemSamplingRepository.save(itemSampling);
    }


    @Transactional
    public ItemSampling saveOrUpdate(ItemSampling itemSampling) {
        if (itemSampling.getId() == null && findByNumber(itemSampling.getWarehouseId(), itemSampling.getNumber()) != null) {
            itemSampling.setId(findByNumber(itemSampling.getWarehouseId(), itemSampling.getNumber()).getId());
        }
        return save(itemSampling);
    }
    @Transactional
    public void delete(ItemSampling itemSampling) {
        itemSamplingRepository.delete(itemSampling);
    }
    @Transactional
    public void delete(Long id) {
        itemSamplingRepository.deleteById(id);
    }
    @Transactional
    public void removeItemSampling(Long id) {
        delete(id);
    }
    @Transactional
    public ItemSampling addItemSampling(ItemSampling itemSampling) {
        // we will need to disable previous item sampling for the same item
        disablePreviousItemSampling(itemSampling);
        // copy the files from the temporary folder into the permanent folder
        itemSampling = saveOrUpdate(itemSampling);
        logger.debug("item sampling {} is added, start to copy the images files",
                itemSampling.getNumber());
        saveSamplingImagePermanently(itemSampling);
        return itemSampling;

    }

    /**
     * Copy the image file from temp folder into permanent folder
     * @param itemSampling
     */
    private void saveSamplingImagePermanently(ItemSampling itemSampling) {
        logger.debug("start to process images urls {} for item sampling {}",
                itemSampling.getImageUrls(),
                itemSampling.getNumber());
        Arrays.stream(itemSampling.getImageUrls().split(",")).forEach(
            imageUrl -> {
                logger.debug("> start to process image {}", imageUrl);
                File imageFile = getItemSamplingImage(itemSampling.getWarehouseId(),
                        itemSampling.getItem().getId(), imageUrl);
                if (imageFile.exists()) {
                    // file exists, copy it to the permenate folder
                    logger.debug(">> file exists, start copy into the permanent folder");
                    try {
                        fileService.copyFile(imageFile,
                                getItemSamplingImage(
                                        itemSampling.getWarehouseId(),
                                        itemSampling.getItem().getId(),
                                        itemSampling.getNumber(),
                                        imageUrl
                                ));
                        logger.debug(">> copy is done, start to remove the original file");
                        logger.debug(">>>> original file is removed? {}", imageFile.delete());
                    } catch (IOException e) {
                        logger.debug(">> error while copy the image into permanent folder");
                        e.printStackTrace();
                    }
                }
                else {
                    logger.debug(">> ignore this image as the file doesn't exists");
                }
            }
        );
    }

    private void disablePreviousItemSampling(ItemSampling itemSampling) {
        List<ItemSampling> itemSamplings = findAll(
                itemSampling.getWarehouseId(),
                null,
                null,
                itemSampling.getItem().getId(),
                true,
                false
        );
        itemSamplings.forEach(
                existingItemSampling -> {
                existingItemSampling.setEnabled(false);
                save(existingItemSampling);
            }
        );
    }

    @Transactional
    public ItemSampling changeItemSampling(Long id, ItemSampling itemSampling) {
        itemSampling.setId(id);
        if (itemSampling.getEnabled()) {
            // OK, we are enable the item sampling, let's disable all
            // other item sampling to make sure we will only have one sampling
            // for this item
            disablePreviousItemSampling(itemSampling);
        }
        return saveOrUpdate(itemSampling);
    }

    /**
     * Get the item sampling image folder. it will be in the predefined folder
     * and a subfolder
     * @param subFolder
     * @return
     */
    private String getWorkOrderQCSampleImageFolder(String subFolder) {
        return qcSampleImageFolder + "/" + subFolder + "/";
    }

    public String uploadQCSampleImage(Long itemId, MultipartFile file) throws IOException {

        String subFolder = itemId.toString();
        return uploadQCSampleImage(subFolder, file);

    }
    public String uploadQCSampleImage(Long itemId, String number, MultipartFile file) throws IOException {

        String subFolder = itemId.toString() + "/" + number;
        return uploadQCSampleImage(subFolder, file);

    }

    public String uploadQCSampleImage(String subFolder, MultipartFile file) throws IOException {


        String filePath = getWorkOrderQCSampleImageFolder(subFolder);
        logger.debug("Save file to {}{}",
                filePath, file.getOriginalFilename());

        File savedFile =
                fileService.saveFile(
                        file, filePath, file.getOriginalFilename());

        logger.debug("File saved, path: {}",
                savedFile.getAbsolutePath());
        return file.getOriginalFilename();

    }

    public File getItemSamplingImage(Long warehouseId, Long itemId, String number, String fileName) {


        String subFolder = itemId.toString() + "/" + number;
        String fileUrl = getWorkOrderQCSampleImageFolder(subFolder) + fileName;

        logger.debug("Will return {} to the client",
                fileUrl);
        return new File(fileUrl);
    }

    public File getItemSamplingImage(Long warehouseId,  Long itemId,  String fileName) {


        String subFolder = itemId.toString();
        String fileUrl = getWorkOrderQCSampleImageFolder(subFolder) + fileName;

        logger.debug("Will return {} to the client",
                fileUrl);
        return new File(fileUrl);
    }

    /**
     * For display purpose, we will return one record for each item. If the last item sampling is disabled, then we will return an
     * record but without any detail information, so that the user will be able to query the history of the item sampling for this item
     *  from the web client
     * @param warehouseId
     * @param number
     * @param itemName
     * @param itemId
     * @param currentSampleOnly
     * @return
     */
    public List<ItemSampling> findAllItemSamplingForDisplay(Long warehouseId, String number, String itemName, Long itemId, Boolean currentSampleOnly) {
        // let's get all the item sampling first
        List<ItemSampling> itemSamplings =  findAll(warehouseId, number, itemName, itemId, null, currentSampleOnly);

        // map of the item sampling,
        // key: item id
        // value: latest item sampling
        // for each item, we will have one item sampling returned from the above list
        // if the item has an enabled item sampling, then it will be the enabled item sampling
        // otherwise, we will save any of the disabled item sampling in the map then we will
        // clear the image url so it will show as an empty sampling with the item informaton only
        // in the web client
        Map<Long, ItemSampling> itemSamplingMap = new HashMap<>();
        itemSamplings.forEach(
                itemSampling -> {
                    ItemSampling existingItemSampling = itemSamplingMap.get(itemSampling.getItem().getId());
                    if (Objects.isNull(existingItemSampling) || itemSampling.getEnabled()) {
                        itemSamplingMap.put(itemSampling.getItem().getId(), itemSampling);
                    }
                }
        );

        return itemSamplingMap.values().stream().map(
                itemSampling -> {
                    // if the item smapling is disabled, then we will clear the image url so it looks like
                    // the
                    if(!itemSampling.getEnabled()) {

                        return new ItemSampling(
                                "",
                                "",
                                itemSampling.getWarehouseId(),
                                "",
                                itemSampling.getItem(),
                                false) ;
                    }
                    else {
                        return itemSampling;
                    }
                }
        ).collect(Collectors.toList());


    }

    public ItemSampling disableItemSampling(Long id) {
        ItemSampling itemSampling = findById(id);
        itemSampling.setEnabled(false);
        return saveOrUpdate(itemSampling);
    }

    public List<ItemSampling> findAllPreviousItemSamplingForDisplay(Long warehouseId, String itemName, Long itemId) {

        // we need to have at least the item name or item id as we will only return
        // the item sampling for one single item
        if (Strings.isBlank(itemName) && Objects.isNull(itemId)) {
            throw InventoryException.raiseException("Please pass in either the item's name or item's id");
        }
        return findAll(warehouseId, null, itemName, itemId, false, null);
    }

    public void removeItemSamplingImage(Long warehouseId, Long itemId, String number, String fileName) {

        File imageFile = getItemSamplingImage(warehouseId, itemId, number, fileName);
        if (imageFile.exists()) {
            logger.debug("File {} exists for item id {}, item sampling number {}, we will remove it",
                    fileName, itemId, fileName);
            imageFile.delete();
        }
    }

    public void removeItemSamplingImage(Long warehouseId, Long itemId, String fileName) {

        File imageFile = getItemSamplingImage(warehouseId, itemId, fileName);
        if (imageFile.exists()) {
            logger.debug("File {} exists for item id {}, item sampling number {}, we will remove it",
                    fileName, itemId, fileName);
            imageFile.delete();
        }
    }
}
