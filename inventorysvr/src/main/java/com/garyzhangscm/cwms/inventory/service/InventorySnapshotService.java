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

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.inventory.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.InboundServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.OutbuondServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.InventoryException;
import com.garyzhangscm.cwms.inventory.exception.MissingInformationException;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventoryRepository;
import com.garyzhangscm.cwms.inventory.repository.InventorySnapshotRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
@Service
public class InventorySnapshotService  {
    private static final Logger logger = LoggerFactory.getLogger(InventorySnapshotService.class);

    @Autowired
    private InventorySnapshotRepository inventorySnapshotRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventorySnapshotConfigurationService inventorySnapshotConfigurationService;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;
    @Autowired
    private InboundServiceRestemplateClient inboundServiceRestemplateClient;
    @Autowired
    private IntegrationService integrationService;
    @Autowired
    private FileService fileService;


    public InventorySnapshot findById(Long id) {
        return findById(id, true);
    }
    public InventorySnapshot findById(Long id, boolean includeDetails) {
        InventorySnapshot inventorySnapshot = inventorySnapshotRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("inventory snapshot not found by id: " + id));
        if (Objects.nonNull(inventorySnapshot) && includeDetails) {
            loadDetails(inventorySnapshot);
        }
        return inventorySnapshot;
    }


    public InventorySnapshot save(InventorySnapshot inventorySnapshot) {
        InventorySnapshot newInventorySnapshot =  inventorySnapshotRepository.save(inventorySnapshot);
        loadDetails(newInventorySnapshot);
        return newInventorySnapshot;
    }

    public InventorySnapshot saveOrUpdate(InventorySnapshot inventorySnapshot) {
        if (inventorySnapshot.getId() == null &&
                findByBatchNumber(inventorySnapshot.getWarehouseId(), inventorySnapshot.getBatchNumber()) != null) {
            inventorySnapshot.setId(
                    findByBatchNumber(inventorySnapshot.getWarehouseId(), inventorySnapshot.getBatchNumber()).getId());
        }
        return save(inventorySnapshot);
    }

    private InventorySnapshot findByBatchNumber(Long warehouseId, String batchNumber) {
        List<InventorySnapshot> inventorySnapshots = findAll(warehouseId, null, batchNumber);
        if (inventorySnapshots.size() > 0) {
            return inventorySnapshots.get(0);
        }
        else {
            return null;
        }
    }

    public List<InventorySnapshot> findAll(Long warehouseId,
                                           String status,
                                   String batchNumber) {
        return findAll(warehouseId, status, batchNumber, true);
    }


    public List<InventorySnapshot> findAll(Long warehouseId,
                                           String status,
                                   String batchNumber,
                                   boolean includeDetails) {

        List<InventorySnapshot> inventorySnapshots =  inventorySnapshotRepository.findAll(
                (Root<InventorySnapshot> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(status)) {

                        predicates.add(criteriaBuilder.equal(
                                root.get("status"), InventorySnapshotStatus.valueOf(status)));
                    }

                    if (StringUtils.isNotBlank(batchNumber)) {

                        predicates.add(criteriaBuilder.equal(root.get("batchNumber"), batchNumber));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (inventorySnapshots.size() > 0 && includeDetails) {
            loadDetails(inventorySnapshots);
        }

        return inventorySnapshots;
    }


    public InventorySnapshot getInprocessInventorySnapshot(Long warehouseId) {

        return getInprocessInventorySnapshot(warehouseId, true);
    }
    /**
     * For each warehouse, we should at most have one inprocess inventory snapshot
     * at a certain time
     * @param warehouseId
     * @return
     */
    public InventorySnapshot getInprocessInventorySnapshot(Long warehouseId, boolean includeDetails) {
        List<InventorySnapshot> inventorySnapshots = findAll(
                warehouseId, InventorySnapshotStatus.PROCESSING.toString(),
                null
        );
        if (inventorySnapshots.size() > 0) {
            InventorySnapshot inventorySnapshot =  inventorySnapshots.get(0);
            if (includeDetails) {
                loadDetails(inventorySnapshot);
                return inventorySnapshot;
            }
        }
        return null;
    }

    public void loadDetails(List<InventorySnapshot> inventorySnapshots) {
        // map to temporary save the location group type name
        // key: location group type id
        // value: location group type name
        Map<Long, String> locationGroupTypeNameMap = new HashMap<>();

        // Setup the location group type name, which we will display
        // in the client side
        for (InventorySnapshot inventorySnapshot : inventorySnapshots) {

            inventorySnapshot.getInventorySnapshotDetails().forEach(
                    inventorySnapshotDetail -> {
                        Long locationGroupTypeId = inventorySnapshotDetail.getLocationGroupTypeId();
                        if (locationGroupTypeNameMap.containsKey(locationGroupTypeId)) {
                            inventorySnapshotDetail.setLocationGroupTypeName(
                                    locationGroupTypeNameMap.get(locationGroupTypeId)
                            );
                        }
                        else {
                            LocationGroupType locationGroupType
                                    = warehouseLayoutServiceRestemplateClient.getLocationGroupTypeById(locationGroupTypeId);

                            inventorySnapshotDetail.setLocationGroupTypeName(
                                    locationGroupType.getName()
                            );
                        }
                        locationGroupTypeNameMap.putIfAbsent(locationGroupTypeId, inventorySnapshotDetail.getLocationGroupTypeName());

                    }
            );
        }
    }
    public void loadDetails(InventorySnapshot inventorySnapshot) {
        // map to temporary save the location group type name
        // key: location group type id
        // value: location group type name
        Map<Long, String> locationGroupTypeNameMap = new HashMap<>();

        // Setup the location group type name, which we will display
        // in the client side
        inventorySnapshot.getInventorySnapshotDetails().forEach(
                inventorySnapshotDetail -> {
                    Long locationGroupTypeId = inventorySnapshotDetail.getLocationGroupTypeId();
                    if (locationGroupTypeNameMap.containsKey(locationGroupTypeId)) {
                        inventorySnapshotDetail.setLocationGroupTypeName(
                                locationGroupTypeNameMap.get(locationGroupTypeId)
                        );
                    }
                    else {
                        LocationGroupType locationGroupType
                                = warehouseLayoutServiceRestemplateClient.getLocationGroupTypeById(locationGroupTypeId);

                        inventorySnapshotDetail.setLocationGroupTypeName(
                                locationGroupType.getName()
                        );
                    }
                    locationGroupTypeNameMap.putIfAbsent(locationGroupTypeId, inventorySnapshotDetail.getLocationGroupTypeName());

                }
        );
    }


    public InventorySnapshot generateInventorySnapshot(Long warehouseId) {
        logger.debug(" start to generate inventory snapshot for warehouse {}",
                  warehouseId);
        InventorySnapshot inventorySnapshot = getInprocessInventorySnapshot(
                warehouseId, false
        );
        // if we have a inprocess snapshot, return it.
        // It make no sense to have 2 snapshot concurrently running
        if (Objects.nonNull(inventorySnapshot)) {

            logger.debug("  return an existing in process inventory snapshot {}",
                      inventorySnapshot.getBatchNumber());
        }
        else {
            inventorySnapshot = generateInventorySnapshot(warehouseId,
                                    getNextBatchNumber(warehouseId));
        }

        loadDetails(inventorySnapshot);
        return inventorySnapshot;
    }

    private String getNextBatchNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "inventory-snapshot-batch-number");
    }

    private InventorySnapshot generateInventorySnapshot(Long warehouseId, String batchNumber) {
        logger.debug("  Start to generate inventory snpashot for {} with batch number {}",
                  warehouseId, batchNumber);
        InventorySnapshot inventorySnapshot = new InventorySnapshot();
        inventorySnapshot.setWarehouseId(warehouseId);
        inventorySnapshot.setBatchNumber(batchNumber);

        inventorySnapshot.setStatus(InventorySnapshotStatus.PROCESSING);
        inventorySnapshot.setStartTime(LocalDateTime.now());
        InventorySnapshot savedInventorySnapshot = saveOrUpdate(inventorySnapshot);

        logger.debug("  inventory snapshot with batch number {} is generated",
                  batchNumber);


        List<Inventory> inventories = inventoryService.findAll(inventorySnapshot.getWarehouseId());

        logger.debug(">   1. we find {} inventory record",
                inventories.size());

        // start to generate snapshot for each item
        new Thread(() -> {
            generateInventorySnapshotDetails(savedInventorySnapshot, inventories);
        }).start();

        return savedInventorySnapshot;
    }

    /**
     * Generate inventory snapshot details for each item
     * @param inventorySnapshot inventory snapshot head
     */
    private void generateInventorySnapshotDetails(InventorySnapshot inventorySnapshot,
                                                 List<Inventory> inventories) {

        logger.debug("  Start to generate inventory snapshot details  for  batch number {}",
                 inventorySnapshot.getBatchNumber());
        // inventoryService.findAll will ignore the vitural inventory

        // key: item id - item package type id - inventory status id - location group type id
        // value: inventory snapshot details
        Map<String, InventorySnapshotDetail> inventorySnapshotDetailMap =
                new HashMap<>();

        inventories.stream().forEach(inventory -> {
            String key = new StringBuilder()
                    .append(inventory.getItem().getId()).append("-")
                    .append(inventory.getItemPackageType().getId()).append("-")
                    .append(inventory.getLocation().getLocationGroup().getLocationGroupType().getId())
                    .toString();

            InventorySnapshotDetail inventorySnapshotDetail;
            // if we already have the snapshot detail entry that has the same key(inventory attirbute)
            // let's just add the quantity on top of it. other wise, create a new entry and
            // save it to the map
            if (inventorySnapshotDetailMap.containsKey(key)) {
                inventorySnapshotDetail = inventorySnapshotDetailMap.get(key);
                inventorySnapshotDetail.setQuantity(
                        inventorySnapshotDetail.getQuantity() + inventory.getQuantity()
                );
            }
            else {

                logger.debug(">>   2.1 add key {} to the map",
                         key);
                inventorySnapshotDetail = new InventorySnapshotDetail(inventorySnapshot, inventory);
            }
            inventorySnapshotDetailMap.put(key, inventorySnapshotDetail);
            logger.debug(">>  2.2 key {} 's quantity {}",
                     key, inventorySnapshotDetail.getQuantity());
        });

        // add the details into the current inventory snapshot
        inventorySnapshot.setInventorySnapshotDetails(
                new ArrayList<>(inventorySnapshotDetailMap.values())
        );
        inventorySnapshot.setStatus(InventorySnapshotStatus.DONE);
        inventorySnapshot.setCompleteTime(LocalDateTime.now());

        // save the result
        logger.debug(">>   3 start to save details to batch {}",
                  inventorySnapshot.getBatchNumber());
        saveOrUpdate(inventorySnapshot);
        logger.debug(">>   4 end of save details to batch {}",
                  inventorySnapshot.getBatchNumber());


    }

    public List<InventorySnapshotDetail> findAllInventorySnapshotDetails(Long warehouseId, String batchNumber) {
        InventorySnapshot inventorySnapshot = findByBatchNumber(warehouseId, batchNumber);
        return Objects.nonNull(inventorySnapshot) ? inventorySnapshot.getInventorySnapshotDetails() : new ArrayList<>();
    }


}