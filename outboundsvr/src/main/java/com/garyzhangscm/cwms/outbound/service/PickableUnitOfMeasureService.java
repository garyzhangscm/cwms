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

package com.garyzhangscm.cwms.outbound.service;


import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;

import com.garyzhangscm.cwms.outbound.repository.PickableUnitOfMeasureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.*;



@Service
public class PickableUnitOfMeasureService {
    private static final Logger logger = LoggerFactory.getLogger(PickableUnitOfMeasureService.class);

    @Autowired
    private PickableUnitOfMeasureRepository pickableUnitOfMeasureRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public AllocationConfigurationPickableUnitOfMeasure findById(Long id) {
        return pickableUnitOfMeasureRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("pickable unit of measure not found by id: " + id));
    }

    public List<AllocationConfigurationPickableUnitOfMeasure> findAll(Long warehouseId,
                                                                      Long allocationConfigurationId) {

        return pickableUnitOfMeasureRepository.findAll(
                (Root<AllocationConfigurationPickableUnitOfMeasure> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(allocationConfigurationId)) {

                        Join<AllocationConfigurationPickableUnitOfMeasure, AllocationConfiguration> joinAllocationConfiguration
                                = root.join("allocationConfiguration", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinAllocationConfiguration.get("id"), allocationConfigurationId));

                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );


    }

    public void loadAttribute(List<AllocationConfigurationPickableUnitOfMeasure> allocationConfigurationPickableUnitOfMeasures) {
        allocationConfigurationPickableUnitOfMeasures.forEach(pickableUnitOfMeasure -> loadAttribute(pickableUnitOfMeasure));
    }
    public void loadAttribute(AllocationConfigurationPickableUnitOfMeasure allocationConfigurationPickableUnitOfMeasure) {

        if (Objects.nonNull(allocationConfigurationPickableUnitOfMeasure.getUnitOfMeasureId()) &&
            Objects.isNull(allocationConfigurationPickableUnitOfMeasure.getUnitOfMeasure())) {
            allocationConfigurationPickableUnitOfMeasure.setUnitOfMeasure(
                    commonServiceRestemplateClient.getUnitOfMeasureById(
                            allocationConfigurationPickableUnitOfMeasure.getUnitOfMeasureId()
                    )
            );
        }

        if (Objects.nonNull(allocationConfigurationPickableUnitOfMeasure.getWarehouseId()) &&
                Objects.isNull(allocationConfigurationPickableUnitOfMeasure.getWarehouse())) {
            allocationConfigurationPickableUnitOfMeasure.setWarehouse(
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(
                            allocationConfigurationPickableUnitOfMeasure.getWarehouseId()
                    )
            );
        }



    }


    public AllocationConfigurationPickableUnitOfMeasure save(AllocationConfigurationPickableUnitOfMeasure allocationConfigurationPickableUnitOfMeasure) {
        logger.debug("Save pickableUnitOfMeasure\n{}", allocationConfigurationPickableUnitOfMeasure);
        return pickableUnitOfMeasureRepository.save(allocationConfigurationPickableUnitOfMeasure);
    }



    public void delete(AllocationConfigurationPickableUnitOfMeasure allocationConfigurationPickableUnitOfMeasure) {
        pickableUnitOfMeasureRepository.delete(allocationConfigurationPickableUnitOfMeasure);
    }

    public void delete(Long id) {
        pickableUnitOfMeasureRepository.deleteById(id);
    }

    public void delete(String pickableUnitOfMeasureIds) {
        if (!pickableUnitOfMeasureIds.isEmpty()) {
            long[] pickableUnitOfMeasureIdArray = Arrays.asList(pickableUnitOfMeasureIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : pickableUnitOfMeasureIdArray) {
                delete(id);
            }
        }
    }

}
