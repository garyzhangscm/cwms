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

package com.garyzhangscm.cwms.adminserver.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.adminserver.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.adminserver.model.BillableActivityType;
import com.garyzhangscm.cwms.adminserver.repository.BillableActivityTypeRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class BillableActivityTypeService  {
    private static final Logger logger = LoggerFactory.getLogger(BillableActivityTypeService.class);

    @Autowired
    private BillableActivityTypeRepository billableActivityTypeRepository;

    public BillableActivityType findById(Long id) {
        return billableActivityTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Billable Activity Type not found by id: " + id));
    }


    public List<BillableActivityType> findAll(Long warehouseId, String name,
                                              String description) {

        return billableActivityTypeRepository.findAll(
                (Root<BillableActivityType> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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
                    if (StringUtils.isNotBlank(description)) {
                        String descriptionComparator = description;
                        if (descriptionComparator.contains("*")) {
                            descriptionComparator.replaceAll("\\*", "%");
                        }
                        descriptionComparator = "%" + descriptionComparator + "%";

                        predicates.add(criteriaBuilder.like(root.get("description"), descriptionComparator));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

    }


    public BillableActivityType findByName(Long warehouseId, String name){
        return billableActivityTypeRepository.findByWarehouseIdAndName(warehouseId, name);
    }

    public BillableActivityType save(BillableActivityType billableActivityType) {
        return billableActivityTypeRepository.save(billableActivityType);
    }

    public BillableActivityType saveOrUpdate(BillableActivityType billableActivityType) {
        if (Objects.isNull(billableActivityType.getId()) &&
                Objects.nonNull(findByName(billableActivityType.getWarehouseId(), billableActivityType.getName()))) {

            billableActivityType.setId(findByName(billableActivityType.getWarehouseId(), billableActivityType.getName()).getId());
        }

        return save(billableActivityType);
    }



    public void delete(BillableActivityType billableActivityType) {
        billableActivityTypeRepository.delete(billableActivityType);
    }
    public void delete(Long id) {
        billableActivityTypeRepository.deleteById(id);
    }


    public BillableActivityType createBillableActivityType(BillableActivityType billableActivityType) {
        return saveOrUpdate(billableActivityType);
    }

    public BillableActivityType changeBillableActivityType(BillableActivityType billableActivityType) {
        return saveOrUpdate(billableActivityType);
    }
}
