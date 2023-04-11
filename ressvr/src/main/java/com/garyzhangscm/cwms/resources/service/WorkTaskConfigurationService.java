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
import java.util.stream.Collectors;


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

    public List<WorkTaskConfiguration> findByWarehouse(Long warehouseId) {
        return workTaskConfigurationRepository.findByWarehouseId(warehouseId);
    }


    public WorkTaskConfiguration save(WorkTaskConfiguration workTaskConfiguration) {
        return workTaskConfigurationRepository.save(workTaskConfiguration);
    }

    public void delete(WorkTaskConfiguration workTaskConfiguration) {
        workTaskConfigurationRepository.delete(workTaskConfiguration);
    }

    public void delete(Long id) {
        workTaskConfigurationRepository.deleteById(id);
    }



    public WorkTaskConfiguration addWorkTaskConfiguration(WorkTaskConfiguration workTaskConfiguration) {
        return save(workTaskConfiguration);
    }

    public WorkTaskConfiguration changeWorkTaskConfiguration(Long id, WorkTaskConfiguration workTaskConfiguration) {
        workTaskConfiguration.setId(id);
        return save(workTaskConfiguration);
    }

    public WorkTaskConfiguration findBestMatch(Long warehouseId,
                                               Long sourceLocationId,
                                               Long destinationLocationId,
                                               WorkTaskType workTaskType) {
        List<WorkTaskConfiguration> workTaskConfigurations = findByWarehouse(warehouseId);

        // work task type is a required field on the configuration
        return workTaskConfigurations.stream().filter(
                workTaskConfiguration -> workTaskType.equals(workTaskConfiguration.getWorkTaskType()) &&
                        isMatch(sourceLocationId, destinationLocationId, workTaskConfiguration)
        ).sorted((a, b) -> compareParticularity(a, b))
                .findFirst().orElse(null);

    }

    /**
     * Compare the particularity for 2 configuration. return -1 is a is more particular than b
     * particularity level
     * 1. source location id is setup
     * 2. destination location is setup
     * 3. source location group is setup
     * 4. destination location group is setup
     * 5. source location group type is setup
     * 6. destination location group type is setup
     * @param a
     * @param b
     * @return
     */
    private int compareParticularity(WorkTaskConfiguration a, WorkTaskConfiguration b) {
        // particularity level
        // 1. source location id is setup
        // 2. destination location is setup
        // 3. source location group is setup
        // 4. destination location group is setup
        // 5. source location group type is setup
        // 6. destination location group type is setup
        // we will implement a score system and both configuration start with 10000.
        // 1. if the source location id is setup, then reduce by 2000
        // 2. if the destination location id is setup, then reduce by 1000
        // 3. if the source location group id is setup, then reduce by 200
        // 4. if the destination location group id is setup, then reduce by 100
        // 5. if the source location group type id is setup, then reduce by 20
        // 6. if the destination location group type id is setup, then reduce by 10
        // the configuration has lower score is more particular
        int scoreA = score(a);
        int scoreB = score(b);
        return scoreA - scoreB;
    }

    /**
     * A score system so we can compare 2 configuration's particularity
     * we will implement a score system and both configuration start with 10000.
     *  1. if the source location id is setup, then reduce by 2000
     *  2. if the destination location id is setup, then reduce by 1000
     *  3. if the source location group id is setup, then reduce by 200
     *  4. if the destination location group id is setup, then reduce by 100
     *  5. if the source location group type id is setup, then reduce by 20
     *  6. if the destination location group type id is setup, then reduce by 10
     *  the configuration has lower score is more particular
     * @param workTaskConfiguration
     * @return
     */
    private int score(WorkTaskConfiguration workTaskConfiguration) {
        int score = 10000;
        if (Objects.nonNull(workTaskConfiguration.getSourceLocationId())) {
            score -= 2000;
        }
        if (Objects.nonNull(workTaskConfiguration.getSourceLocationGroupId())) {
            score -= 200;
        }
        if (Objects.nonNull(workTaskConfiguration.getSourceLocationGroupTypeId())) {
            score -= 20;
        }
        if (Objects.nonNull(workTaskConfiguration.getDestinationLocationId())) {
            score -= 1000;
        }
        if (Objects.nonNull(workTaskConfiguration.getDestinationLocationGroupId())) {
            score -= 100;
        }
        if (Objects.nonNull(workTaskConfiguration.getDestinationLocationGroupTypeId())) {
            score -= 10;
        }
        return score;
    }

    public boolean isMatch(Long sourceLocationId, Long destinationLocationId,
                            WorkTaskConfiguration workTaskConfiguration) {
        if (Objects.nonNull(sourceLocationId)) {
            Location sourceLocation = layoutServiceRestemplateClient.getLocationById(sourceLocationId);
            if (Objects.nonNull(workTaskConfiguration.getSourceLocationGroupTypeId()) &&
                !workTaskConfiguration.getSourceLocationGroupTypeId().equals(
                        sourceLocation.getLocationGroup().getLocationGroupType().getId()
                )) {
                logger.debug("The source location group type is setup for the work task configuration as id = {}, " +
                        " but the source location to be compared belongs to the location group type id = {}, NOT MATCH",
                        workTaskConfiguration.getSourceLocationGroupTypeId(),
                        sourceLocation.getLocationGroup().getLocationGroupType().getId());
                return false;
            }
            if (Objects.nonNull(workTaskConfiguration.getSourceLocationGroupId()) &&
                    !workTaskConfiguration.getSourceLocationGroupId().equals(
                            sourceLocation.getLocationGroup().getId()
                    )) {
                logger.debug("The source location group is setup for the work task configuration as id = {}, " +
                                " but the source location to be compared belongs to the location group id = {}, NOT MATCH",
                        workTaskConfiguration.getSourceLocationGroupId(),
                        sourceLocation.getLocationGroup().getId());
                return false;
            }
            if (Objects.nonNull(workTaskConfiguration.getSourceLocationId()) &&
                    !workTaskConfiguration.getSourceLocationId().equals(
                            sourceLocation.getId()
                    )) {
                logger.debug("The source location is setup for the work task configuration as id = {}, " +
                                " but the source location to be compared has id = {}, NOT MATCH",
                        workTaskConfiguration.getSourceLocationId(),
                        sourceLocation.getId());
                return false;
            }
        }


        if (Objects.nonNull(destinationLocationId)) {
            Location destinationLocation = layoutServiceRestemplateClient.getLocationById(destinationLocationId);
            if (Objects.nonNull(workTaskConfiguration.getDestinationLocationGroupTypeId()) &&
                    !workTaskConfiguration.getDestinationLocationGroupTypeId().equals(
                            destinationLocation.getLocationGroup().getLocationGroupType().getId()
                    )) {
                logger.debug("The destination location group type is setup for the work task configuration as id = {}, " +
                                " but the destination location to be compared belongs to the location group type id = {}, NOT MATCH",
                        workTaskConfiguration.getDestinationLocationGroupTypeId(),
                        destinationLocation.getLocationGroup().getLocationGroupType().getId());
                return false;
            }
            if (Objects.nonNull(workTaskConfiguration.getDestinationLocationGroupId()) &&
                    !workTaskConfiguration.getDestinationLocationGroupId().equals(
                            destinationLocation.getLocationGroup().getId()
                    )) {
                logger.debug("The destination location group is setup for the work task configuration as id = {}, " +
                                " but the destination location to be compared belongs to the location group id = {}, NOT MATCH",
                        workTaskConfiguration.getDestinationLocationGroupId(),
                        destinationLocation.getLocationGroup().getId());
                return false;
            }
            if (Objects.nonNull(workTaskConfiguration.getDestinationLocationId()) &&
                    !workTaskConfiguration.getDestinationLocationId().equals(
                            destinationLocation.getId()
                    )) {
                logger.debug("The destination location is setup for the work task configuration as id = {}, " +
                                " but the destination location to be compared has id = {}, NOT MATCH",
                        workTaskConfiguration.getDestinationLocationId(),
                        destinationLocation.getId());
                return false;
            }
        }



        return true;
    }
}
