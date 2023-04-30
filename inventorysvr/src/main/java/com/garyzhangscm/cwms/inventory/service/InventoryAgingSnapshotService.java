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
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventoryAgingSnapshotRepository;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryAgingSnapshotService {
    private static final Logger logger = LoggerFactory.getLogger(InventoryAgingSnapshotService.class);

    @Autowired
    private InventoryAgingSnapshotRepository inventoryAgingSnapshotRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    @Autowired
    private InventoryService inventoryService;

    public InventoryAgingSnapshot findById(Long id) {
        return inventoryAgingSnapshotRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Inventory Aging Snapshot not found by id: " + id));
    }

    public InventoryAgingSnapshot findByNumber(Long warehouseId, String number) {
        return inventoryAgingSnapshotRepository.findByWarehouseIdAndNumber(warehouseId, number);

    }


    public List<InventoryAgingSnapshot> findAll(Long warehouseId,
                                                String status,
                                                String number,
                                                ZonedDateTime startTime,
                                                ZonedDateTime endTime) {
        return findAll(warehouseId, status, number,
                startTime, endTime, true);

    }
    public List<InventoryAgingSnapshot> findAll(Long warehouseId,
                                                String status,
                                                String number,
                                                ZonedDateTime startTime,
                                                ZonedDateTime endTime,
                                                Boolean loadDetails) {

        List<InventoryAgingSnapshot> inventoryAgingSnapshots =
                inventoryAgingSnapshotRepository.findAll(
                (Root<InventoryAgingSnapshot> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Strings.isNotBlank(status)) {

                        predicates.add(criteriaBuilder.equal(root.get("status"), InventoryAgingSnapshotStatus.valueOf(status)));
                    }

                    if (Strings.isNotBlank(number)) {

                        predicates.add(criteriaBuilder.equal(root.get("number"), number));
                    }
                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("startTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("startTime"), endTime));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.DESC, "createdTime")
        );

        if (!inventoryAgingSnapshots.isEmpty() && Boolean.TRUE.equals(loadDetails)) {
            loadAttribute(inventoryAgingSnapshots);
        }
        return inventoryAgingSnapshots;

    }

    public InventoryAgingSnapshot save(InventoryAgingSnapshot inventoryAgingSnapshot) {
        return inventoryAgingSnapshotRepository.save(inventoryAgingSnapshot);
    }



    public InventoryAgingSnapshot saveOrUpdate(InventoryAgingSnapshot inventoryAgingSnapshot) {
        if (Objects.isNull(inventoryAgingSnapshot.getId()) &&
                Objects.nonNull(findByNumber(inventoryAgingSnapshot.getWarehouseId(), inventoryAgingSnapshot.getNumber()))) {
            inventoryAgingSnapshot.setId(
                    findByNumber(
                            inventoryAgingSnapshot.getWarehouseId(), inventoryAgingSnapshot.getNumber()
                    ).getId()
            );
        }
        return save(inventoryAgingSnapshot);
    }

    private void loadAttribute(List<InventoryAgingSnapshot> inventoryAgingSnapshots) {
        inventoryAgingSnapshots.forEach(
                inventoryAgingSnapshot -> loadAttribute(inventoryAgingSnapshot)
        );
    }
    private void loadAttribute(InventoryAgingSnapshot inventoryAgingSnapshot) {

        inventoryAgingSnapshot.getClientInventoryAgingSnapshots().forEach(
                clientInventoryAgingSnapshot -> loadAttribute(clientInventoryAgingSnapshot)
        );
    }
    private void loadAttribute(ClientInventoryAgingSnapshot clientInventoryAgingSnapshot) {

        if (Objects.nonNull(clientInventoryAgingSnapshot.getClientId()) &&
                Objects.isNull(clientInventoryAgingSnapshot.getClient())) {
            Client client = commonServiceRestemplateClient.getClientById(
                    clientInventoryAgingSnapshot.getClientId()
            );
            if (Objects.nonNull(client)) {
                clientInventoryAgingSnapshot.setClient(client);
            }
        }

    }


    public InventoryAgingSnapshot getInprocessInventoryAgingSnapshot(Long warehouseId) {

        List<InventoryAgingSnapshot> inventoryAgingSnapshots = findAll(
                warehouseId, LocationUtilizationSnapshotStatus.PROCESSING.toString(),
                null, null, null
        );
        if (inventoryAgingSnapshots.size() > 0) {
            return inventoryAgingSnapshots.get(0);
        }
        return null;
    }

    /**
     * Generate inventory aging snapshot. We will need to make sure
     * we will only have one in process batch.
     * @param warehouseId
     * @return
     */
    public InventoryAgingSnapshot generateInventoryAgingSnapshot(
            Long warehouseId) {
        logger.debug(" start to generate inventory aging snapshot for warehouse {}",
                warehouseId);
        InventoryAgingSnapshot inventoryAgingSnapshot =
                getInprocessInventoryAgingSnapshot(
                        warehouseId);
        // if we have a inprocess snapshot, return it.
        // It make no sense to have 2 snapshot concurrently running
        if (Objects.nonNull(inventoryAgingSnapshot)) {

            logger.debug("  return an existing in process inventory aging snapshot {}",
                    inventoryAgingSnapshot.getNumber());
        }
        else {
            inventoryAgingSnapshot = generateInventoryAgingSnapshot(warehouseId,
                    getNextBatchNumber(warehouseId));
        }

        return inventoryAgingSnapshot;
    }

    private String getNextBatchNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "inventory-aging-snapshot-number");
    }

    /**
     * Generate a new inventory aging snapshot for the warehouse. We will create a empty snapshot first, then
     * use a separate thread to fill in all the details and calcualte the total number
     * @param warehouseId
     * @param number
     * @return
     */
    public InventoryAgingSnapshot generateInventoryAgingSnapshot(
            Long warehouseId, String number) {

        // initiate the request. We will generate the snapshot in a separated thread
        InventoryAgingSnapshot inventoryAgingSnapshot =
                initiateInventoryAgingSnapshot(warehouseId, number);


        // start to generate snapshot for each item
        new Thread(() -> {
            logger.debug("Start to fill in the location utilization snapshot for warehouse {}, number {}",
                    warehouseId, number);
            generateInventoryAgingSnapshot(warehouseId, inventoryAgingSnapshot);
        }).start();


        return inventoryAgingSnapshot;

    }

    /**
     * Get the initiated snapshot and fill in all the details(by client / by inventory) and calculate the total aging days / weeks
     * @param inventoryAgingSnapshot
     */
    private void generateInventoryAgingSnapshot(Long warehouseId, InventoryAgingSnapshot inventoryAgingSnapshot) {
        Collection<InventoryAgingSnapshotDetail> inventoryAgingSnapshotDetails =
                getInventoryAgingSnapshotDetails(inventoryAgingSnapshot.getWarehouseId());


        if (inventoryAgingSnapshotDetails.isEmpty()) {
            logger.debug("Nothing returned from the inventory aging snapshot");
            completeInventoryAgingSnapshot(inventoryAgingSnapshot);
            return;
        }

        logger.debug("get {} details that will we need to fill in the snapshot {}",
                inventoryAgingSnapshotDetails.size(),
                inventoryAgingSnapshot.getNumber());
        // group the details by client

        List<ClientInventoryAgingSnapshot> clientInventoryAgingSnapshots
                = groupInventoryAgingSnapshotDetail(warehouseId, inventoryAgingSnapshot,
                    inventoryAgingSnapshotDetails);

        inventoryAgingSnapshot.setClientInventoryAgingSnapshots(
                clientInventoryAgingSnapshots
        );

        completeInventoryAgingSnapshot(inventoryAgingSnapshot);
    }

    private void completeInventoryAgingSnapshot(InventoryAgingSnapshot inventoryAgingSnapshot) {
        inventoryAgingSnapshot.setStatus(InventoryAgingSnapshotStatus.DONE.DONE);
        inventoryAgingSnapshot.setCompleteTime(ZonedDateTime.now(ZoneOffset.UTC));

        // save the result
        logger.debug(">>   3 start to save details to inventory aging snapshot {}",
                inventoryAgingSnapshot.getNumber());
        saveOrUpdate(inventoryAgingSnapshot);
        logger.debug(">>   4 end of save details to inventory aging snapshot {}",
                inventoryAgingSnapshot.getNumber());
    }

    private List<ClientInventoryAgingSnapshot> groupInventoryAgingSnapshotDetail(
            Long warehouseId,
            InventoryAgingSnapshot inventoryAgingSnapshot,
            Collection<InventoryAgingSnapshotDetail> inventoryAgingSnapshotDetails
    ) {
        Map<Long, ClientInventoryAgingSnapshot> clientInventoryAgingSnapshotMap =
                new HashMap<>();

        inventoryAgingSnapshotDetails.forEach(
                inventoryAgingSnapshotDetail -> {
                    ClientInventoryAgingSnapshot clientInventoryAgingSnapshot =
                            clientInventoryAgingSnapshotMap.getOrDefault(
                                    inventoryAgingSnapshotDetail.getInventory().getClientId(),
                                    new ClientInventoryAgingSnapshot(
                                            warehouseId,
                                            inventoryAgingSnapshotDetail.getInventory().getClientId(),
                                            inventoryAgingSnapshot
                                    )
                            );
                    clientInventoryAgingSnapshot.addInventoryAgingSnapshotDetail(
                            inventoryAgingSnapshotDetail
                    );
                    clientInventoryAgingSnapshotMap.put(
                            inventoryAgingSnapshotDetail.getInventory().getClientId(),
                            clientInventoryAgingSnapshot

                    );
                }
        );

        return new ArrayList(clientInventoryAgingSnapshotMap.values());

    }


    /**
     * Initiate the location utilization snapshot batch with 0 quantity
     * @param warehouseId
     * @param number
     * @return
     */
    private InventoryAgingSnapshot initiateInventoryAgingSnapshot(Long warehouseId, String number) {

        logger.debug("initiate a inventory aging snapshot batch with number {}",
                number);
        InventoryAgingSnapshot inventoryAgingSnapshot =
                new InventoryAgingSnapshot(warehouseId, number);
        return saveOrUpdate(inventoryAgingSnapshot);
    }


    /**
     * Generate inventory aging snapshot for certain warehouse
     * @param warehouseId
     * @return
     */
    private Collection<InventoryAgingSnapshotDetail> getInventoryAgingSnapshotDetails(
            Long warehouseId ) {

        // get all the client from this warehouse and calculate the
        //  inventory aging snapshot separately. We will calculate the inventory aging snapshot
        // for the warehouse(client id is null) as well
        List<Client> clients = commonServiceRestemplateClient.getAllClients(warehouseId);

        logger.debug("We get {} clients", clients.size());

        logger.debug("Start to get location utilization snapshot for warehouse {}, without any client",
                warehouseId);
        Collection<InventoryAgingSnapshotDetail> inventoryAgingSnapshotDetails = getInventoryAgingSnapshotDetails(
                warehouseId, null);

        logger.debug(">> We get {} for inventory aging snapshot of warehouse {}, no client",
                inventoryAgingSnapshotDetails.size(), warehouseId);
        clients.forEach(
                client -> {

                    logger.debug("Start to get inventory aging snapshot for warehouse {}, client {}",
                            warehouseId, client.getName());
                    Collection<InventoryAgingSnapshotDetail> clientInventoryAgingSnapshotDetails=
                            getInventoryAgingSnapshotDetails(warehouseId, client.getId());
                    logger.debug(">> We get {} for inventory aging snapshot of warehouse {}, client {}",
                            clientInventoryAgingSnapshotDetails.size(),  warehouseId,
                            client.getName());
                    inventoryAgingSnapshotDetails.addAll(clientInventoryAgingSnapshotDetails);

                }
        );
        return inventoryAgingSnapshotDetails;

    }
    private Collection<InventoryAgingSnapshotDetail> getInventoryAgingSnapshotDetails(
            Long warehouseId, Long clientId) {

        // get four wall inventory by client
        List<Inventory> inventories = inventoryService.findByClientId(warehouseId, clientId, false);

        return inventories.stream().map(
                inventory -> new InventoryAgingSnapshotDetail(inventory)
        ).collect(Collectors.toList());
    }

    public void remove(Long warehouseId, Long id) {
        inventoryAgingSnapshotRepository.deleteById(id);
    }

    public List<ClientInventoryAgingSnapshot> getClientInventoryAgingSnapshotGroupByLPN(Long warehouseId, Long clientId,
                                                                                  ZonedDateTime startTime,
                                                                                  ZonedDateTime endTime) {

        List<InventoryAgingSnapshot> inventoryAgingSnapshots = findAll(warehouseId,
                InventoryAgingSnapshotStatus.DONE.toString(), null,
                startTime,
                endTime).stream()
                // only return the inventory snapshot that has the client's information
                .filter(
                        inventoryAgingSnapshot ->
                                inventoryAgingSnapshot.getClientInventoryAgingSnapshots()
                                        .stream().anyMatch(clientInventoryAgingSnapshot -> clientId.equals(clientInventoryAgingSnapshot.getClientId()))
                ).collect(Collectors.toList());

        if (inventoryAgingSnapshots.isEmpty()) {
            return new ArrayList<>();
        }

        Collections.sort(inventoryAgingSnapshots, Comparator.comparing(InventoryAgingSnapshot::getStartTime));

        // make sure we will only have one record per day
        // key: YYYYMMDD
        // value: InventoryAgingSnapshot
        Map<String, InventoryAgingSnapshot> inventoryAgingSnapshotMap = new HashMap<>();
        inventoryAgingSnapshots.forEach(
                inventoryAgingSnapshot -> {
                    String key = inventoryAgingSnapshot.getStartTime().format(DateTimeFormatter.ofPattern("YYYYMMDD"));
                    if (!inventoryAgingSnapshotMap.containsKey(key)) {
                        inventoryAgingSnapshotMap.put(key, inventoryAgingSnapshot);
                    }
                }
        );

        return inventoryAgingSnapshotMap.values().stream().map(
                inventoryAgingSnapshot ->
                        inventoryAgingSnapshot.getClientInventoryAgingSnapshots().stream().filter(
                                clientInventoryAgingSnapshot -> clientId.equals(clientInventoryAgingSnapshot.getClientId()))
                                .findFirst().orElse(null)
        ).filter(inventoryAgingSnapshot -> Objects.nonNull(inventoryAgingSnapshot))
                .map(
                        clientInventoryAgingSnapshot -> {
                            clientInventoryAgingSnapshot.setupInventoryAgingByLPN();
                            // clear the details since we don't need it any more and
                            // won't need to send to the caller
                            clientInventoryAgingSnapshot.setInventoryAgingSnapshotDetails(Collections.emptyList());
                            return clientInventoryAgingSnapshot;
                        }
                ).collect(Collectors.toList());

    }
    public ClientInventoryAgingSnapshot getClientInventoryAgingSnapshotGroupByLPN(Long warehouseId, Long clientId, ZonedDateTime date) {
        // get the date's inventory that is on the specific date
        ZonedDateTime startTime = date.toLocalDate().atStartOfDay(date.getZone());
        ZonedDateTime endTime = date.toLocalDate().plusDays(1).atStartOfDay(date.getZone()).minusMinutes(1);

        List<InventoryAgingSnapshot> inventoryAgingSnapshots = findAll(warehouseId,
                InventoryAgingSnapshotStatus.DONE.toString(), null,
                startTime,
                endTime).stream()
                // only return the inventory snapshot that has the client's information
                .filter(
                        inventoryAgingSnapshot ->
                                inventoryAgingSnapshot.getClientInventoryAgingSnapshots()
                                        .stream().anyMatch(clientInventoryAgingSnapshot -> clientId.equals(clientInventoryAgingSnapshot.getClientId()))
                ).collect(Collectors.toList());

        if (inventoryAgingSnapshots.isEmpty()) {
            return null;
        }
        // get the first snapshot of the day
        Collections.sort(inventoryAgingSnapshots, Comparator.comparing(InventoryAgingSnapshot::getCreatedTime));

        ClientInventoryAgingSnapshot matchedClientInventoryAgingSnapshot =
                inventoryAgingSnapshots.get(0).getClientInventoryAgingSnapshots().stream().filter(
                    clientInventoryAgingSnapshot -> clientId.equals(clientInventoryAgingSnapshot.getClientId()))
                .findFirst().orElse(null);

        if (Objects.isNull(matchedClientInventoryAgingSnapshot)) {
            return null;
        }

        matchedClientInventoryAgingSnapshot.setupInventoryAgingByLPN();
        // clear the details since we don't need it any more and
        // won't need to send to the caller
        matchedClientInventoryAgingSnapshot.setInventoryAgingSnapshotDetails(Collections.emptyList());

        return matchedClientInventoryAgingSnapshot;

    }
}
