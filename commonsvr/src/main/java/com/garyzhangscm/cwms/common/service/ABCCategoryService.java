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

package com.garyzhangscm.cwms.common.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.repository.ABCCategoryRepository;
import com.garyzhangscm.cwms.common.repository.ClientRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class ABCCategoryService {

    private static final Logger logger = LoggerFactory.getLogger(ABCCategoryService.class);

    @Autowired
    private ABCCategoryRepository abcCategoryRepository;

    public ABCCategory findById(Long id) {
        return abcCategoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("abc category not found by id: " + id));
    }

    public List<ABCCategory> findAll(Long warehouseId,
                                     String name,
                                     String description  ) {
        return abcCategoryRepository.findAll(
                (Root<ABCCategory> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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
                        if (description.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("description"), description.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("description"), description));
                        }
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));

                }
                ,
                Sort.by(Sort.Direction.ASC, "warehouseId", "name")
        );
    }

    public ABCCategory findByName(Long warehouseId, String name){
        return abcCategoryRepository.findByWarehouseIdAndName(warehouseId, name);
    }

    @Transactional
    public ABCCategory save(ABCCategory abcCategory) {
        return abcCategoryRepository.save(abcCategory);
    }
    // Save when the client's name doesn't exists
    // update when the client already exists
    @Transactional
    public ABCCategory saveOrUpdate(ABCCategory abcCategory) {
        if (abcCategory.getId() == null && findByName(abcCategory.getWarehouseId(), abcCategory.getName()) != null) {
            abcCategory.setId(findByName(abcCategory.getWarehouseId(), abcCategory.getName()).getId());
        }
        return save(abcCategory);
    }

    @Transactional
    public void delete(ABCCategory abcCategory) {
        abcCategoryRepository.delete(abcCategory);
    }
    @Transactional
    public void delete(Long id) {
        abcCategoryRepository.deleteById(id);
    }


    public ABCCategory addABCCategory(ABCCategory abcCategory) {
        return saveOrUpdate(abcCategory);
    }

    public ABCCategory changeABCCategory(ABCCategory abcCategory) {
        return saveOrUpdate(abcCategory);
    }

    public void removeABCCategory(Long id) {
        delete(id);
    }
}
