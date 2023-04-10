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

import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.WorkTaskConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class WorkTaskConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(WorkTaskConfigurationService.class);

    @Autowired
    private WorkTaskConfigurationRepository workTaskConfigurationRepository;
    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;

    public WorkTaskConfiguration findById(Long id) {
        return workTaskConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Work task configuration not found by id: " + id));

    }

    public List<WorkTaskConfiguration> findAll(Long warehouseId,
                                               Long sourceLocationGroupTypeId,
                                               String sourceLocationGroupTypeName,
                                               Long sourceLocationGroupId,
                                               String sourceLocationGroupName,
                                               Long sourceLocationId,
                                               String sourceLocationName,
                                               Long destinationLocationGroupTypeId,
                                               String destinationLocationGroupTypeName,
                                               Long destinationLocationGroupId,
                                               String destinationLocationGroupName,
                                               Long destinationLocationId,
                                               String destinationLocationName,
                                               String workTaskType,
                                               String operationTypeName) {
        return workTaskConfigurationRepository.findAll(
                (Root<WorkTaskConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(sourceLocationGroupTypeId)) {
                        predicates.add(criteriaBuilder.equal(root.get("sourceLocationGroupTypeId"), sourceLocationGroupTypeId));
                    }
                    else if (Strings.isNotBlank(sourceLocationGroupTypeName)) {
                        LocationGroupType locationGroupType =
                                layoutServiceRestemplateClient.getLocationGroupTypeByName(sourceLocationGroupTypeName);
                        if (Objects.nonNull(locationGroupType)) {
                            predicates.add(criteriaBuilder.equal(root.get("sourceLocationGroupTypeId"), locationGroupType.getId()));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("sourceLocationGroupTypeId"), -1));
                        }
                    }

                    if (Objects.nonNull(sourceLocationGroupId)) {
                        predicates.add(criteriaBuilder.equal(root.get("sourceLocationGroupId"), sourceLocationGroupId));
                    }
                    else if (Strings.isNotBlank(sourceLocationGroupName)) {
                        LocationGroup locationGroup =
                                layoutServiceRestemplateClient.getLocationGroupByName(warehouseId, sourceLocationGroupName);
                        if (Objects.nonNull(locationGroup)) {
                            predicates.add(criteriaBuilder.equal(root.get("sourceLocationGroupId"), locationGroup.getId()));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("sourceLocationGroupId"), -1));
                        }
                    }

                    if (Objects.nonNull(sourceLocationId)) {
                        predicates.add(criteriaBuilder.equal(root.get("sourceLocationId"), sourceLocationId));
                    }
                    else if (Strings.isNotBlank(sourceLocationName)) {
                        Location location =
                                layoutServiceRestemplateClient.getLocationByName(warehouseId, sourceLocationName);
                        if (Objects.nonNull(location)) {
                            predicates.add(criteriaBuilder.equal(root.get("sourceLocationId"), location.getId()));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("sourceLocationId"), -1));
                        }
                    }

                    if (Objects.nonNull(destinationLocationGroupTypeId)) {
                        predicates.add(criteriaBuilder.equal(root.get("destinationLocationGroupTypeId"), destinationLocationGroupTypeId));
                    }
                    else if (Strings.isNotBlank(destinationLocationGroupTypeName)) {
                        LocationGroupType locationGroupType =
                                layoutServiceRestemplateClient.getLocationGroupTypeByName(destinationLocationGroupTypeName);
                        if (Objects.nonNull(locationGroupType)) {
                            predicates.add(criteriaBuilder.equal(root.get("destinationLocationGroupTypeId"), locationGroupType.getId()));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("destinationLocationGroupTypeId"), -1));
                        }
                    }

                    if (Objects.nonNull(destinationLocationGroupId)) {
                        predicates.add(criteriaBuilder.equal(root.get("destinationLocationGroupId"), destinationLocationGroupId));
                    }
                    else if (Strings.isNotBlank(destinationLocationGroupName)) {
                        LocationGroup locationGroup =
                                layoutServiceRestemplateClient.getLocationGroupByName(warehouseId, destinationLocationGroupName);
                        if (Objects.nonNull(locationGroup)) {
                            predicates.add(criteriaBuilder.equal(root.get("destinationLocationGroupId"), locationGroup.getId()));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("destinationLocationGroupId"), -1));
                        }
                    }

                    if (Objects.nonNull(destinationLocationId)) {
                        predicates.add(criteriaBuilder.equal(root.get("destinationLocationId"), destinationLocationId));
                    }
                    else if (Strings.isNotBlank(destinationLocationName)) {
                        Location location =
                                layoutServiceRestemplateClient.getLocationByName(warehouseId, destinationLocationName);
                        if (Objects.nonNull(location)) {
                            predicates.add(criteriaBuilder.equal(root.get("destinationLocationId"), location.getId()));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("destinationLocationId"), -1));
                        }
                    }
                    if (Strings.isNotBlank(workTaskType)) {

                        predicates.add(criteriaBuilder.equal(root.get("workTaskType"), WorkTaskType.valueOf(workTaskType)));
                    }
                    if (Strings.isNotBlank(operationTypeName)) {

                        Join<WorkTaskConfiguration, OperationType> joinOperationType = root.join("operationType", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinOperationType.get("name"),  operationTypeName ));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "createdTime")
        );
    }



    public WorkTaskConfiguration findByName(Long warehouseId, String name) {
        return workTaskConfigurationRepository.findByWarehouseIdAndName(warehouseId, name);
    }



    public WorkTaskConfiguration save(WorkTaskConfiguration workTaskConfiguration) {
        return workTaskConfigurationRepository.save(workTaskConfiguration);
    }

    public WorkTaskConfiguration saveOrUpdate(WorkTaskConfiguration workTaskConfiguration) {
        if (operationType.getId() == null && findByName(
                operationType.getWarehouseId(), operationType.getName()) != null) {
            operationType.setId(findByName(
                    operationType.getWarehouseId(), operationType.getName()).getId());
        }
        return save(operationType);
    }


    public void delete(WorkTaskConfiguration workTaskConfiguration) {
        workTaskConfigurationRepository.delete(workTaskConfiguration);
    }

    public void delete(Long id) {
        workTaskConfigurationRepository.deleteById(id);
    }



    public OperationType addOperationType(OperationType workTaskConfiguration) {
        return saveOrUpdate(workTaskConfiguration);
    }

    public OperationType changeOperationType(Long id, OperationType workTaskConfiguration) {
        operationType.setId(id);
        return saveOrUpdate(workTaskConfiguration);

    }
}
