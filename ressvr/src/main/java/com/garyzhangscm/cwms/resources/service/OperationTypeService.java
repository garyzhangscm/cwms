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

package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.OperationType;
import com.garyzhangscm.cwms.resources.repository.OperationTypeRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
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
public class OperationTypeService {
    private static final Logger logger = LoggerFactory.getLogger(OperationTypeService.class);

    @Autowired
    private OperationTypeRepository operationTypeRepository;

    public OperationType findById(Long id) {
        return operationTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("operation type not found by id: " + id));

    }

    public List<OperationType> findAll(Long warehouseId,
                                       String name,
                                       String description) {
        return operationTypeRepository.findAll(
                (Root<OperationType> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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
                Sort.by(Sort.Direction.ASC, "name")
        );
    }



    public OperationType findByName(Long warehouseId, String name) {
        return operationTypeRepository.findByWarehouseIdAndName(warehouseId, name);
    }



    public OperationType save(OperationType operationType) {
        return operationTypeRepository.save(operationType);
    }

    public OperationType saveOrUpdate(OperationType operationType) {
        if (operationType.getId() == null && findByName(
                operationType.getWarehouseId(), operationType.getName()) != null) {
            operationType.setId(findByName(
                    operationType.getWarehouseId(), operationType.getName()).getId());
        }
        return save(operationType);
    }


    public void delete(OperationType operationType) {
        operationTypeRepository.delete(operationType);
    }

    public void delete(Long id) {
        operationTypeRepository.deleteById(id);
    }



    public OperationType addOperationType(OperationType operationType) {
        return saveOrUpdate(operationType);
    }

    public OperationType changeOperationType(Long id, OperationType operationType) {
        operationType.setId(id);
        return saveOrUpdate(operationType);

    }
}
