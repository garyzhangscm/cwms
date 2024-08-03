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
import com.garyzhangscm.cwms.inventory.model.Item;
import com.garyzhangscm.cwms.inventory.model.ItemBarcode;
import com.garyzhangscm.cwms.inventory.model.ItemBarcodeType;
import com.garyzhangscm.cwms.inventory.model.ItemFamily;
import com.garyzhangscm.cwms.inventory.repository.ItemBarcodeRepository;
import com.garyzhangscm.cwms.inventory.repository.ItemBarcodeTypeRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 *
 */
@Service
public class ItemBarcodeService {
    private static final Logger logger = LoggerFactory.getLogger(ItemBarcodeService.class);

    @Autowired
    private ItemBarcodeRepository itemBarcodeRepository;

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
}