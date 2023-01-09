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
import com.garyzhangscm.cwms.inventory.model.Client;
import com.garyzhangscm.cwms.inventory.model.ClientLocationUtilizationSnapshotBatch;
import com.garyzhangscm.cwms.inventory.model.Item;
import com.garyzhangscm.cwms.inventory.model.LocationUtilizationSnapshot;
import com.garyzhangscm.cwms.inventory.repository.ClientLocationUtilizationSnapshotBatchRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ClientLocationUtilizationSnapshotBatchService {

    private static final Logger logger = LoggerFactory.getLogger(ClientLocationUtilizationSnapshotBatchService.class);
    @Autowired
    private ClientLocationUtilizationSnapshotBatchRepository clientLocationUtilizationSnapshotBatchRepository;

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private ItemService itemService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public ClientLocationUtilizationSnapshotBatch findById(Long id) {
        return clientLocationUtilizationSnapshotBatchRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("client location utilization snapshot batch not found by id: " + id));
    }

    public List<ClientLocationUtilizationSnapshotBatch> findAll(Long warehouseId,
                                                     String clientName, Long clientId,
                                                                ZonedDateTime startTime,
                                                                ZonedDateTime endTime,
                                                     Boolean loadDetails) {

        List<ClientLocationUtilizationSnapshotBatch> clientLocationUtilizationSnapshotBatches
                = clientLocationUtilizationSnapshotBatchRepository.findAll(
            (Root<ClientLocationUtilizationSnapshotBatch> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();

                predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

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
                else {
                    // nothing related to the client id passed in, we will return the record that without any
                    // client information, which means the location snapshot is for the warehouse, not for
                    // the client

                    predicates.add(criteriaBuilder.isNull(root.get("clientId")));
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

        if (!clientLocationUtilizationSnapshotBatches.isEmpty() && Boolean.TRUE.equals(loadDetails)) {
            loadAttribute(clientLocationUtilizationSnapshotBatches);
        }
        return clientLocationUtilizationSnapshotBatches;

    }



    private void loadAttribute(List<ClientLocationUtilizationSnapshotBatch> clientLocationUtilizationSnapshotBatches) {
        clientLocationUtilizationSnapshotBatches.forEach(this::loadAttribute);
    }

    private void loadAttribute(ClientLocationUtilizationSnapshotBatch clientLocationUtilizationSnapshotBatch) {

        if (Objects.nonNull(clientLocationUtilizationSnapshotBatch.getClientId()) &&
                Objects.isNull(clientLocationUtilizationSnapshotBatch.getClient())) {
            clientLocationUtilizationSnapshotBatch.setClient(
                    commonServiceRestemplateClient.getClientById(
                            clientLocationUtilizationSnapshotBatch.getClientId()
                    )
            );
        }
    }

    public ClientLocationUtilizationSnapshotBatch save(ClientLocationUtilizationSnapshotBatch clientLocationUtilizationSnapshotBatch) {
        return clientLocationUtilizationSnapshotBatchRepository.save(clientLocationUtilizationSnapshotBatch);
    }


}
