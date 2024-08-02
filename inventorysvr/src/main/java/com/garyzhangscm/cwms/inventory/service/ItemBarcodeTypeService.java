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
import com.garyzhangscm.cwms.inventory.model.ItemBarcodeType;
import com.garyzhangscm.cwms.inventory.repository.ItemBarcodeTypeRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;


/**
 *
 */
@Service
public class ItemBarcodeTypeService {
    private static final Logger logger = LoggerFactory.getLogger(ItemBarcodeTypeService.class);

    @Autowired
    private ItemBarcodeTypeRepository itemBarcodeTypeRepository;

    public ItemBarcodeType findById(Long id) {
        return itemBarcodeTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Item Barcode Type not found by id: " + id));}

    public List<ItemBarcodeType> findAll(Long warehouseId,
                                         String name) {
        return itemBarcodeTypeRepository.findAll(
                (Root<ItemBarcodeType> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(name)) {
                        if (name.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("name"), name.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("name"), name));
                        }
                    }

                    Predicate[] p = new Predicate[predicates.size()];

                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.DESC, "name")
        );

    }


    public ItemBarcodeType findByName(Long warehouseId, String name){
        return itemBarcodeTypeRepository.findByWarehouseIdAndName(warehouseId, name);
    }

    @Transactional
    public ItemBarcodeType save(ItemBarcodeType itemBarcodeType) {
        return itemBarcodeTypeRepository.save(itemBarcodeType);
    }

    // Save when the supplier's name doesn't exists
    // update when the supplier already exists
    @Transactional
    public ItemBarcodeType saveOrUpdate(ItemBarcodeType itemBarcodeType) {
        if (itemBarcodeType.getId() == null &&
                findByName(itemBarcodeType.getWarehouseId(), itemBarcodeType.getName()) != null) {
            itemBarcodeType.setId(
                    findByName(itemBarcodeType.getWarehouseId(), itemBarcodeType.getName()).getId());
        }
        return save(itemBarcodeType);
    }
    @Transactional
    public void delete(ItemBarcodeType itemBarcodeType) {
        itemBarcodeTypeRepository.delete(itemBarcodeType);
    }
    @Transactional
    public void delete(Long id) {
        itemBarcodeTypeRepository.deleteById(id);
    }

    public ItemBarcodeType changeItemBarcodeType(ItemBarcodeType itemBarcodeType) {
        return saveOrUpdate(itemBarcodeType);
    }

    public ItemBarcodeType addItemBarcodeType(ItemBarcodeType itemBarcodeType) {
        return saveOrUpdate(itemBarcodeType);
    }
}