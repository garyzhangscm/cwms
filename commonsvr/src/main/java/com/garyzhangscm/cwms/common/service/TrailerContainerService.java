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

import com.garyzhangscm.cwms.common.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.common.exception.TrailerException;
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.repository.TrailerContainerRepository;
import com.garyzhangscm.cwms.common.repository.TrailerRepository;
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
import java.util.*;


@Service
public class TrailerContainerService {
    private static final Logger logger = LoggerFactory.getLogger(TrailerContainerService.class);

    @Autowired
    private TrailerContainerRepository trailerContainerRepository;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public TrailerContainer findById(Long id) {
        return trailerContainerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("trailer container not found by id: " + id));
    }


    public List<TrailerContainer> findAll(Long companyId, Long warehouseId,
                                  String number) {
        List<TrailerContainer> trailerContainers = trailerContainerRepository.findAll(
                (Root<TrailerContainer> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    if (StringUtils.isNotBlank(number)) {
                        if (number.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("name"), number));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("name"), number));
                        }
                    }
                    Predicate[] p = new Predicate[predicates.size()];

                    // special handling for warehouse id
                    // if warehouse id is passed in, then return both the warehouse level item
                    // and the company level item information.
                    // otherwise, return the company level item information
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));
                    if (Objects.nonNull(warehouseId)) {
                        return criteriaBuilder.and(predicate,
                                criteriaBuilder.or(
                                        criteriaBuilder.equal(root.get("warehouseId"), warehouseId),
                                        criteriaBuilder.isNull(root.get("warehouseId"))));
                    }
                    else  {
                        return criteriaBuilder.and(predicate,criteriaBuilder.isNull(root.get("warehouseId")));
                    }
                }
                ,
                Sort.by(Sort.Direction.ASC, "warehouseId", "number")
        );

        // we may get duplicated record from the above query when we pass in the warehouse id
        // if so, we may need to remove the company level item if we have the warehouse level item
        if (Objects.nonNull(warehouseId)) {
            removeDuplicatedContainerRecords(trailerContainers);
        }
        return trailerContainers;
    }

    /**
     * Remove the duplicated container record. If we have 2 record with the same container number
     * but different warehouse, then we will remove the one without any warehouse information
     * from the result
     * @param trailerContainers
     */
    private void removeDuplicatedContainerRecords(List<TrailerContainer> trailerContainers) {
        Iterator<TrailerContainer> containerIterator = trailerContainers.listIterator();
        Set<String> containerProcessed = new HashSet<>();
        while(containerIterator.hasNext()) {
            TrailerContainer container = containerIterator.next();

            if (containerProcessed.contains(container.getNumber()) &&
                    Objects.isNull(container.getWarehouseId())) {
                // ok, we already processed the item and the current
                // record is a company level item, then we will remove
                // this record from the result
                containerIterator.remove();
            }
            containerProcessed.add(container.getNumber());
        }
    }

    public TrailerContainer save(TrailerContainer trailerContainer) {
        return trailerContainerRepository.save(trailerContainer);
    }



    public void delete(TrailerContainer trailerContainer) {
        trailerContainerRepository.delete(trailerContainer);
    }

    public void delete(Long id) {
        trailerContainerRepository.deleteById(id);
    }




}
