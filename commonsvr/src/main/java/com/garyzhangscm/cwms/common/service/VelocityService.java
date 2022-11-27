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

import com.garyzhangscm.cwms.common.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.common.model.ABCCategory;
import com.garyzhangscm.cwms.common.model.Velocity;
import com.garyzhangscm.cwms.common.repository.ABCCategoryRepository;
import com.garyzhangscm.cwms.common.repository.VelocityRepository;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class VelocityService {

    private static final Logger logger = LoggerFactory.getLogger(VelocityService.class);

    @Autowired
    private VelocityRepository velocityRepository;

    public Velocity findById(Long id) {
        return velocityRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("velocity not found by id: " + id));
    }

    public List<Velocity> findAll(Long warehouseId,
                                  String name,
                                  String description  ) {
        return velocityRepository.findAll(
                (Root<Velocity> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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
                Sort.by(Sort.Direction.ASC, "warehouseId", "name")
        );
    }

    public Velocity findByName(Long warehouseId, String name){
        return velocityRepository.findByWarehouseIdAndName(warehouseId, name);
    }

    @Transactional
    public Velocity save(Velocity velocity) {
        return velocityRepository.save(velocity);
    }
    // Save when the client's name doesn't exists
    // update when the client already exists
    @Transactional
    public Velocity saveOrUpdate(Velocity velocity) {
        if (velocity.getId() == null && findByName(velocity.getWarehouseId(), velocity.getName()) != null) {
            velocity.setId(findByName(velocity.getWarehouseId(), velocity.getName()).getId());
        }
        return save(velocity);
    }

    @Transactional
    public void delete(Velocity velocity) {
        velocityRepository.delete(velocity);
    }
    @Transactional
    public void delete(Long id) {
        velocityRepository.deleteById(id);
    }


    public Velocity addVelocity(Velocity velocity) {
        return saveOrUpdate(velocity);
    }

    public Velocity changeVelocity(Velocity velocity) {
        return saveOrUpdate(velocity);
    }

    public void removeVelocity(Long id) {
        delete(id);
    }
}
