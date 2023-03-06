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

import com.garyzhangscm.cwms.inventory.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.LocationUtilizationSnapshotRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class LocationUtilizationSnapshotService   {

    private static final Logger logger = LoggerFactory.getLogger(LocationUtilizationSnapshotService.class);
    @Autowired
    private LocationUtilizationSnapshotRepository locationUtilizationSnapshotRepository;

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private ItemService itemService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public LocationUtilizationSnapshot findById(Long id) {
        return locationUtilizationSnapshotRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("location utilization snapshot not found by id: " + id));
    }

    public List<LocationUtilizationSnapshot> findAll(Long warehouseId,
                                                     String itemName, Long itemId,
                                                     String clientName, Long clientId,
                                                     ZonedDateTime startTime,
                                                     ZonedDateTime endTime,
                                                     Boolean loadDetails) {

        List<LocationUtilizationSnapshot> locationUtilizationSnapshots = locationUtilizationSnapshotRepository.findAll(
            (Root<LocationUtilizationSnapshot> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();

                predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                if (Objects.nonNull(itemId)) {

                    predicates.add(criteriaBuilder.equal(root.get("itemId"), itemId));
                }
                else if (Strings.isNotBlank(itemName)) {
                    Join<LocationUtilizationSnapshot, Item> joinItem = root.join("item", JoinType.INNER);
                    predicates.add(criteriaBuilder.equal(joinItem.get("name"), itemName));
                }
                if (Objects.nonNull(clientId)) {

                    predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
                }
                else if (Strings.isNotBlank(clientName)) {
                    Client client = commonServiceRestemplateClient.getClientByName(warehouseId, clientName);
                    if (Objects.nonNull(client)) {

                        predicates.add(criteriaBuilder.equal(root.get("clientId"), client.getId()));
                    }
                    else {
                        ResourceNotFoundException.raiseException("location utilization snapshot not found by client: " + clientName);
                    }
                }

                if (Objects.nonNull(startTime)) {

                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                            root.get("createdTime"), startTime));

                }
                if (Objects.nonNull(endTime)) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(
                            root.get("createdTime"), endTime));

                }

                Predicate[] p = new Predicate[predicates.size()];
                return criteriaBuilder.and(predicates.toArray(p));
            }
        );

        if (!locationUtilizationSnapshots.isEmpty() && Boolean.TRUE.equals(loadDetails)) {
            loadAttribute(locationUtilizationSnapshots);
        }
        return locationUtilizationSnapshots;

    }



    private void loadAttribute(List<LocationUtilizationSnapshot> locationUtilizationSnapshots) {
        locationUtilizationSnapshots.forEach(this::loadAttribute);
    }

    private void loadAttribute(LocationUtilizationSnapshot locationUtilizationSnapshot) {

        if (Objects.nonNull(locationUtilizationSnapshot.getClientId()) &&
                Objects.isNull(locationUtilizationSnapshot.getClient())) {
            locationUtilizationSnapshot.setClient(
                    commonServiceRestemplateClient.getClientById(
                            locationUtilizationSnapshot.getClientId()
                    )
            );
        }
    }

    public LocationUtilizationSnapshot save(LocationUtilizationSnapshot locationUtilizationSnapshot) {
        return locationUtilizationSnapshotRepository.save(locationUtilizationSnapshot);
    }


    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        locationUtilizationSnapshotRepository.processItemOverride(warehouseId,
                oldItemId, newItemId);
    }

}
