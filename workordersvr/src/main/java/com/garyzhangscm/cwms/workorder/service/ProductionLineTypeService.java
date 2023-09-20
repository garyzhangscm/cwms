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

package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.ProductionLineType;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineTypeRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;


@Service
public class ProductionLineTypeService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionLineTypeService.class);

    @Autowired
    private ProductionLineTypeRepository productionLineTypeRepository;

    public ProductionLineType findById(Long id) {
        return productionLineTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("production line type not found by id: " + id));
    }



    public List<ProductionLineType> findAll(Long warehouseId, String name, String description) {
        return productionLineTypeRepository.findAll(
                (Root<ProductionLineType> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(name)) {
                        if (name.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("name"), name));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("name"), name));
                        }
                    }

                    if (StringUtils.isNotBlank(description)) {
                        if (description.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("description"), description));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("description"), description));
                        }
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "name")
        );

    }

    public ProductionLineType findByName(Long warehouseId, String name) {

        return productionLineTypeRepository.findByWarehouseIdAndName(warehouseId, name);
    }

    public ProductionLineType save(ProductionLineType productionLineType) {
        return productionLineTypeRepository.save(productionLineType);
    }

    public ProductionLineType saveOrUpdate(ProductionLineType productionLineType) {
        if (productionLineType.getId() == null &&
                findByName(productionLineType.getWarehouseId(), productionLineType.getName()) != null) {
            productionLineType.setId(
                    findByName(productionLineType.getWarehouseId(), productionLineType.getName()).getId());
        }
        return save(productionLineType);
    }


    public void delete(ProductionLineType productionLineType) {
        productionLineTypeRepository.delete(productionLineType);
    }

    public void delete(Long id) {
        productionLineTypeRepository.deleteById(id);
    }


    public ProductionLineType addProductionLineType(ProductionLineType productionLineType) {
        return saveOrUpdate(productionLineType);
    }

    public ProductionLineType changeProductionLineType(Long id, ProductionLineType productionLineType) {
        return saveOrUpdate(productionLineType);
    }
}
