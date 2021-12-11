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

import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.ItemSamplingRepository;
import org.apache.commons.lang.StringUtils;
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
        return saveOrUpdate(itemSampling);
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
     * and a subfolder defined by the number
     * @param number
     * @return
     */
    private String getWorkOrderQCSampleImageFolder(String number) {
        return qcSampleImageFolder + "/" + number + "/";
    }

    public String uploadQCSampleImage(String number, MultipartFile file) throws IOException {


        String filePath = getWorkOrderQCSampleImageFolder(number);
        logger.debug("Save file to {}{}",
                filePath, file.getOriginalFilename());

        File savedFile =
                fileService.saveFile(
                        file, filePath, file.getOriginalFilename());

        logger.debug("File saved, path: {}",
                savedFile.getAbsolutePath());
        return file.getOriginalFilename();

    }

    public File getItemSamplingImage(Long warehouseId, String number, String fileName) {


        String fileUrl = getWorkOrderQCSampleImageFolder(number) + fileName;

        logger.debug("Will return {} to the client",
                fileUrl);
        return new File(fileUrl);
    }

}
