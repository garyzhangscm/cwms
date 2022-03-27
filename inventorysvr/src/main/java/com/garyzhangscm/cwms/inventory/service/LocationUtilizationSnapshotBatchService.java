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

import com.garyzhangscm.cwms.inventory.CustomRequestScopeAttr;
import com.garyzhangscm.cwms.inventory.clients.AuthServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.LocationUtilizationSnapshotBatchRepository;
import com.garyzhangscm.cwms.inventory.repository.LocationUtilizationSnapshotRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

import javax.persistence.criteria.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LocationUtilizationSnapshotBatchService {

    private static final Logger logger = LoggerFactory.getLogger(LocationUtilizationSnapshotBatchService.class);
    @Autowired
    private LocationUtilizationSnapshotBatchRepository locationUtilizationSnapshotBatchRepository;

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private ItemService itemService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    @Autowired
    AuthServiceRestemplateClient authServiceRestemplateClient;
    @Autowired
    @Qualifier("oauth2ClientContext")
    OAuth2ClientContext oauth2ClientContext;


    public LocationUtilizationSnapshotBatch findById(Long id) {
        return locationUtilizationSnapshotBatchRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("location utilization snapshot not found by id: " + id));
    }

    public LocationUtilizationSnapshotBatch findByNumber(Long warehouseId, String number) {
        return locationUtilizationSnapshotBatchRepository.findByWarehouseIdAndNumber(warehouseId, number);

    }


    public List<LocationUtilizationSnapshotBatch> findAll(Long warehouseId,
                                                     String status,
                                                     String number,
                                                     LocalDateTime startTime,
                                                     LocalDateTime endTime) {

        return locationUtilizationSnapshotBatchRepository.findAll(
            (Root<LocationUtilizationSnapshotBatch> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();

                predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                if (Strings.isNotBlank(status)) {

                    predicates.add(criteriaBuilder.equal(root.get("status"), LocationUtilizationSnapshotStatus.valueOf(status)));
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
        );
    }

    public LocationUtilizationSnapshotBatch save(LocationUtilizationSnapshotBatch locationUtilizationSnapshotBatch) {
        return locationUtilizationSnapshotBatchRepository.save(locationUtilizationSnapshotBatch);
    }



    public LocationUtilizationSnapshotBatch saveOrUpdate(LocationUtilizationSnapshotBatch locationUtilizationSnapshotBatch) {
        if (Objects.isNull(locationUtilizationSnapshotBatch.getId()) &&
                Objects.nonNull(findByNumber(locationUtilizationSnapshotBatch.getWarehouseId(), locationUtilizationSnapshotBatch.getNumber()))) {
            locationUtilizationSnapshotBatch.setId(
                    findByNumber(
                            locationUtilizationSnapshotBatch.getWarehouseId(), locationUtilizationSnapshotBatch.getNumber()
                    ).getId()
            );
        }
        return locationUtilizationSnapshotBatchRepository.save(locationUtilizationSnapshotBatch);
    }


    public LocationUtilizationSnapshotBatch getInprocessLocationUtilizationSnapshotBatch(Long warehouseId) {

        List<LocationUtilizationSnapshotBatch> locationUtilizationSnapshotBatches = findAll(
                warehouseId, LocationUtilizationSnapshotStatus.PROCESSING.toString(),
                null, null, null
        );
        if (locationUtilizationSnapshotBatches.size() > 0) {
            return locationUtilizationSnapshotBatches.get(0);
        }
        return null;
    }

    /**
     * Generate location utilization snapshot batch. We will need to make sure
     * we will only have one in process batch.
     * @param warehouseId
     * @return
     */
    public LocationUtilizationSnapshotBatch generateLocationUtilizationSnapshotBatch(
            Long warehouseId) {
        logger.debug(" start to generate location utilization snapshot for warehouse {}",
                warehouseId);
        LocationUtilizationSnapshotBatch locationUtilizationSnapshotBatch =
                getInprocessLocationUtilizationSnapshotBatch(
                    warehouseId);
        // if we have a inprocess snapshot, return it.
        // It make no sense to have 2 snapshot concurrently running
        if (Objects.nonNull(locationUtilizationSnapshotBatch)) {

            logger.debug("  return an existing in process location utilization snapshot {}",
                    locationUtilizationSnapshotBatch.getNumber());
        }
        else {
            locationUtilizationSnapshotBatch = generateLocationUtilizationSnapshotBatch(warehouseId,
                    getNextBatchNumber(warehouseId));
        }

        return locationUtilizationSnapshotBatch;
    }

    private String getNextBatchNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "location-utilization-snapshot-number");
    }

    /**
     * Generate a new location utilization snapshot for the warehouse. We will create a empty snapshot first, then
     * use a separate thread to fill in all the details and calcualte the total number
     * @param warehouseId
     * @param number
     * @return
     */
    public LocationUtilizationSnapshotBatch generateLocationUtilizationSnapshotBatch(
            Long warehouseId, String number) {

        // initiate the request. We will generate the snapshot in a separated thread
        LocationUtilizationSnapshotBatch locationUtilizationSnapshotBatch =
                initiateLocationUtilizationSnapshotBatch(warehouseId, number);


        // start to generate snapshot for each item
        new Thread(() -> {

            try {
                setupOAuth2Context();
                generateLocationUtilizationSnapshotBatch(locationUtilizationSnapshotBatch);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();


        return locationUtilizationSnapshotBatch;

    }

    /**
     * Get the initiated snapshot and fill in all the details(by client / by item) and calculate the total quantity / volume
     * @param locationUtilizationSnapshotBatch
     */
    private void generateLocationUtilizationSnapshotBatch(LocationUtilizationSnapshotBatch locationUtilizationSnapshotBatch) {
        Collection<LocationUtilizationSnapshotDetail> locationUtilizationSnapshotDetails =
                getLocationUtilizationSnapshotDetails(locationUtilizationSnapshotBatch.getWarehouseId());

        if (locationUtilizationSnapshotDetails.isEmpty()) {
            logger.debug("Nothing returned from the location utilization snapshot");
            return;
        }

        // group the details into location utilization snapshot
        // group by warehouse id + client id + item id
        Map<String, LocationUtilizationSnapshot> locationUtilizationSnapshotMap
                = groupLocationUtilizationSnapshotDetail(locationUtilizationSnapshotDetails);

        // group the snapshot into client location utilization snapshot batch
        // group by warehouse id + client id
        Map<String, ClientLocationUtilizationSnapshotBatch> clientLocationUtilizationSnapshotBatchMap
                = groupLocationUtilizationSnapshotByClient(locationUtilizationSnapshotMap);


        // group by warehouse
        // since warehouse is part of the argument, we know
        // we will only get one batch for the warehouse

        locationUtilizationSnapshotBatch = groupClientLocationUtilizationSnapshots(
                locationUtilizationSnapshotBatch, clientLocationUtilizationSnapshotBatchMap);

        locationUtilizationSnapshotBatch.setStatus(LocationUtilizationSnapshotStatus.DONE.DONE);
        locationUtilizationSnapshotBatch.setCompleteTime(LocalDateTime.now());

        // save the result
        logger.debug(">>   3 start to save details to batch {}",
                locationUtilizationSnapshotBatch.getNumber());
        saveOrUpdate(locationUtilizationSnapshotBatch);
        logger.debug(">>   4 end of save details to batch {}",
                locationUtilizationSnapshotBatch.getNumber());

    }

    private LocationUtilizationSnapshotBatch groupClientLocationUtilizationSnapshots(
            LocationUtilizationSnapshotBatch locationUtilizationSnapshotBatch,
            Map<String, ClientLocationUtilizationSnapshotBatch> clientLocationUtilizationSnapshotBatchMap) {


        // map of processed location.
        // key: warehouse-location
        // we will calculate the total gross volume based on the locations' size that
        // is used by the warehouse
        Set<String> processLocations = new HashSet<>();

        clientLocationUtilizationSnapshotBatchMap.entrySet().stream().forEach(
                entry -> {
                    ClientLocationUtilizationSnapshotBatch clientLocationUtilizationSnapshotBatch =
                            entry.getValue();
                    locationUtilizationSnapshotBatch.setNetVolume(
                            locationUtilizationSnapshotBatch.getNetVolume() +
                                    clientLocationUtilizationSnapshotBatch.getNetVolume()
                    );
                    clientLocationUtilizationSnapshotBatch.getLocationUtilizationSnapshots().forEach(
                            locationUtilizationSnapshot -> {
                                locationUtilizationSnapshot.getLocationUtilizationSnapshotDetails().forEach(
                                        locationUtilizationSnapshotDetail -> {
                                            String locationKey = new StringBuilder()
                                                    .append(locationUtilizationSnapshotDetail.getWarehouseId())
                                                    .append("-")
                                                    .append(locationUtilizationSnapshotDetail.getLocationId())
                                                    .toString();
                                            if (!processLocations.contains(locationKey)) {
                                                // the location for this warehouse is not processed yet, we will
                                                // add the location's size into the warehouse's gross volume

                                                locationUtilizationSnapshotBatch.setGrossVolume(
                                                        locationUtilizationSnapshotBatch.getGrossVolume() +
                                                                locationUtilizationSnapshotDetail.getLocationSize()
                                                );
                                                locationUtilizationSnapshotBatch.setTotalLocations(
                                                        locationUtilizationSnapshotBatch.getTotalLocations() + 1
                                                );
                                                processLocations.add(locationKey);
                                            }
                                        }
                                );
                            }
                    );
                    locationUtilizationSnapshotBatch.addClientLocationUtilizationSnapshotBatch(
                            clientLocationUtilizationSnapshotBatch
                    );
                    clientLocationUtilizationSnapshotBatch.setLocationUtilizationSnapshotBatch(
                            locationUtilizationSnapshotBatch
                    );

                }
        );
        return locationUtilizationSnapshotBatch;
    }

    /**
     * Setup the OAuth2 token for the background job
     * OAuth2 token will be setup automatically in a web request context
     * but for a separate thread outside the web context, we will need to
     * setup the OAuth2 manually
     * @throws IOException
     */
    private void setupOAuth2Context() throws IOException {

        // Setup the request context so we can utilize the OAuth
        // as if we were in a web request context
        RequestContextHolder.setRequestAttributes(new CustomRequestScopeAttr());

        // Get token. We will use a default user to login and get
        // the OAuth2 token by the default user
        String token = authServiceRestemplateClient.getCurrentLoginUser().getToken();
        // logger.debug("# start to setup the oauth2 token for background job: {}", token);
        // Setup the access toke for the current thread
        // oauth2ClientContext is a scope = request bean that hold
        // the Oauth2 token
        oauth2ClientContext.setAccessToken(new DefaultOAuth2AccessToken(token));

    }
    /**
     * Initiate the location utilization snapshot batch with 0 quantity
     * @param warehouseId
     * @param number
     * @return
     */
    private LocationUtilizationSnapshotBatch initiateLocationUtilizationSnapshotBatch(Long warehouseId, String number) {

        LocationUtilizationSnapshotBatch locationUtilizationSnapshotBatch =
                new LocationUtilizationSnapshotBatch(warehouseId, number);
        return saveOrUpdate(locationUtilizationSnapshotBatch);
    }

    private Map<String, ClientLocationUtilizationSnapshotBatch> groupLocationUtilizationSnapshotByClient(
            Map<String, LocationUtilizationSnapshot> locationUtilizationSnapshotMap) {

        // map of processed location.
        // key: client-location
        // we will calculate the total gross volume based on the locations' size that
        // is used by the client
        Set<String> processLocations = new HashSet<>();

        Map<String, ClientLocationUtilizationSnapshotBatch> clientLocationUtilizationSnapshotBatchMap
                = new HashMap<>();

        // group by client
        locationUtilizationSnapshotMap.entrySet().stream().forEach(
                entry -> {
                    LocationUtilizationSnapshot locationUtilizationSnapshot = entry.getValue();
                    String key = new StringBuilder()
                            .append(locationUtilizationSnapshot.getWarehouseId())
                            .append("-")
                            .append(Objects.nonNull(locationUtilizationSnapshot.getClientId()) ? locationUtilizationSnapshot.getClientId().toString() : "")
                            .toString();
                    ClientLocationUtilizationSnapshotBatch existingClientLocationUtilizationSnapshotBatch =
                            clientLocationUtilizationSnapshotBatchMap.containsKey(key) ?
                                    clientLocationUtilizationSnapshotBatchMap.get(key) :
                                    new ClientLocationUtilizationSnapshotBatch(
                                            locationUtilizationSnapshot.getWarehouseId(),
                                            locationUtilizationSnapshot.getClientId(),
                                            0.0,
                                            0.0,
                                            0
                                    );

                    // net volume: the size of the inventory
                    // gross volume: the size of the locations used by the client
                    existingClientLocationUtilizationSnapshotBatch.setNetVolume(
                            existingClientLocationUtilizationSnapshotBatch.getNetVolume() +
                                    locationUtilizationSnapshot.getNetVolume()
                    );
                    // add the location's size to the total gross volume
                    locationUtilizationSnapshot.getLocationUtilizationSnapshotDetails().forEach(
                            locationUtilizationSnapshotDetail -> {

                                String clientLocationKey = new StringBuilder()
                                        .append(locationUtilizationSnapshot.getWarehouseId())
                                        .append("-")
                                        .append(Objects.nonNull(locationUtilizationSnapshot.getClientId()) ? locationUtilizationSnapshot.getClientId().toString() : "")
                                        .append("-")
                                        .append(locationUtilizationSnapshotDetail.getLocationId())
                                        .toString();
                                if (!processLocations.contains(clientLocationKey)) {
                                    // the location for this client is not processed yet, we will
                                    // add the location's size into the client's gross volume

                                    existingClientLocationUtilizationSnapshotBatch.setGrossVolume(
                                            existingClientLocationUtilizationSnapshotBatch.getGrossVolume() +
                                                    locationUtilizationSnapshotDetail.getLocationSize()
                                    );
                                    existingClientLocationUtilizationSnapshotBatch.setTotalLocations(
                                            existingClientLocationUtilizationSnapshotBatch.getTotalLocations() +
                                                    1
                                    );
                                    processLocations.add(clientLocationKey);
                                }
                            }
                    );
                    existingClientLocationUtilizationSnapshotBatch.addLocationUtilizationSnapshot(
                            locationUtilizationSnapshot
                    );
                    locationUtilizationSnapshot.setClientLocationUtilizationSnapshotBatch(
                            existingClientLocationUtilizationSnapshotBatch
                    );

                    clientLocationUtilizationSnapshotBatchMap.put(key, existingClientLocationUtilizationSnapshotBatch);

                }
        );
        return clientLocationUtilizationSnapshotBatchMap;
    }

    private Map<String, LocationUtilizationSnapshot> groupLocationUtilizationSnapshotDetail(
            Collection<LocationUtilizationSnapshotDetail> locationUtilizationSnapshotDetails) {


        Map<String, LocationUtilizationSnapshot> locationUtilizationSnapshotMap
                = new HashMap<>();

        // map of processed location.
        // key: client-item-location
        // we will calculate the total gross volume based on the locations' size that
        // is used by the client
        Set<String> processLocations = new HashSet<>();

        // item cache
        // key: item id
        // value: item
        Map<Long, Item> itemMap = new HashMap<>();

        // group all the details into location utilization snapshots
        locationUtilizationSnapshotDetails.stream().forEach(
                locationUtilizationSnapshotDetail ->
                {
                    String key = new StringBuilder()
                            .append(locationUtilizationSnapshotDetail.getWarehouseId())
                            .append("-")
                            .append(Objects.nonNull(locationUtilizationSnapshotDetail.getClientId()) ? locationUtilizationSnapshotDetail.getClientId().toString() : "")
                            .append("-")
                            .append(locationUtilizationSnapshotDetail.getItemId())
                            .toString();
                    // get the item from the cache
                    // we will need the item to create the location utilization snapshot
                    Item item = itemMap.getOrDefault(locationUtilizationSnapshotDetail.getItemId(),
                            itemService.findById(locationUtilizationSnapshotDetail.getItemId()));
                    itemMap.putIfAbsent(locationUtilizationSnapshotDetail.getItemId(), item);

                    LocationUtilizationSnapshot existingLocationUtilizationSnapshot =
                            locationUtilizationSnapshotMap.containsKey(key) ?
                                    locationUtilizationSnapshotMap.get(key) :
                                    new LocationUtilizationSnapshot(
                                            locationUtilizationSnapshotDetail.getWarehouseId(), item,
                                            locationUtilizationSnapshotDetail.getClientId(),
                                            0.0,
                                            0.0, 0
                                    );
                    existingLocationUtilizationSnapshot.setNetVolume(
                            existingLocationUtilizationSnapshot.getNetVolume() +
                                    locationUtilizationSnapshotDetail.getNetVolume()
                    );

                    // add the location's size to the total gross volume
                    String clientItemLocationKey = new StringBuilder()
                                        .append(locationUtilizationSnapshotDetail.getWarehouseId())
                                        .append("-")
                                        .append(Objects.nonNull(locationUtilizationSnapshotDetail.getClientId()) ? locationUtilizationSnapshotDetail.getClientId().toString() : "")
                                        .append("-")
                                        .append(locationUtilizationSnapshotDetail.getItemId())
                                        .append("-")
                                        .append(locationUtilizationSnapshotDetail.getLocationId())
                                        .toString();
                    if (!processLocations.contains(clientItemLocationKey)) {
                        // the location for this client and item is not processed yet, we will
                        // add the location's size into the client's gross volume

                        existingLocationUtilizationSnapshot.setGrossVolume(
                                existingLocationUtilizationSnapshot.getGrossVolume() +
                                        locationUtilizationSnapshotDetail.getLocationSize()
                        );
                        existingLocationUtilizationSnapshot.setTotalLocations(
                                existingLocationUtilizationSnapshot.getTotalLocations() + 1
                        );
                        processLocations.add(clientItemLocationKey);
                    }
                    existingLocationUtilizationSnapshot.addLocationUtilizationSnapshotDetail(
                            locationUtilizationSnapshotDetail
                    );
                    locationUtilizationSnapshotDetail.setLocationUtilizationSnapshot(
                            existingLocationUtilizationSnapshot
                    );

                    locationUtilizationSnapshotMap.put(key, existingLocationUtilizationSnapshot);
                }
        );
        return locationUtilizationSnapshotMap;
    }

    /**
     * Generate location utilization snapshot for certain warehouse
     * @param warehouseId
     * @return
     */
    private Collection<LocationUtilizationSnapshotDetail> getLocationUtilizationSnapshotDetails(
            Long warehouseId ) {

        // get all the client from this warehouse and calculate the
        // location utilization separately. We will calculate the location utilization
        // for the warehouse(client id is null) as well
        List<Client> clients = commonServiceRestemplateClient.getAllClients(warehouseId);

        // key: location id
        // value: location size
        // we will cache the location's size
        Map<Long, Double> locationSizes = new HashMap<>();

        Collection<LocationUtilizationSnapshotDetail> locationUtilizationSnapshotDetails = getLocationUtilizationSnapshotDetail(
                warehouseId, null, locationSizes
        );
        clients.forEach(
                client -> locationUtilizationSnapshotDetails.addAll(
                        getLocationUtilizationSnapshotDetail(warehouseId, client.getId(), locationSizes)
                )
        );
        return locationUtilizationSnapshotDetails;

    }
    private Collection<LocationUtilizationSnapshotDetail> getLocationUtilizationSnapshotDetail(
            Long warehouseId, Long clientId, Map<Long, Double> locationSizes) {
        Collection<LocationUtilizationSnapshotDetail> locationUtilizationSnapshotDetails = new ArrayList<>();
        // key: itemVolumeTrackingLevel
        // value: list of location ids, separated by comma
        Map<String, String> utilizationTrackingLocations = warehouseLayoutServiceRestemplateClient.getUtilizationTrackingLocations(warehouseId);

        // for each location, we will load all the inventory and calculate the
        // LocationUtilizationSnapshotDetail structure
        utilizationTrackingLocations.entrySet().forEach(
                entry -> {
                    String itemVolumeTrackingLevel = entry.getKey();
                    String locationIds = entry.getValue();

                    // get the invenotry in the location
                    List<Inventory> inventories = inventoryService.findAll(warehouseId,
                            null,
                            null,
                            null,
                            clientId,
                            null,
                            null,
                            null,
                            null,
                            null,
                            locationIds,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false);

                    // add the location utilization details to the result set
                    // the location utilization details is from current inventory list
                    locationUtilizationSnapshotDetails.addAll(
                            getLocationUtilizationSnapshotDetails(inventories,
                                    ItemVolumeTrackingLevel.valueOf(itemVolumeTrackingLevel), locationSizes)

                    );

                }
        );


        return locationUtilizationSnapshotDetails;

    }

    /**
     * Get location utilization snapshot detail for the inventories in the list. We will
     * use the itemVolumeTrackingLevel to see if we need to calculate the net volume of the
     * inventory by stock UOM or case uom
     * @param inventories
     * @param itemVolumeTrackingLevel by stock uom or case uom
     * @param locationSizes location size cache
     * @return
     */
    private Collection<LocationUtilizationSnapshotDetail> getLocationUtilizationSnapshotDetails(
            List<Inventory> inventories, ItemVolumeTrackingLevel itemVolumeTrackingLevel,
            Map<Long, Double> locationSizes){


        // we will group by client id,  warehouse id and item id
        // key: client id - warehouse id - item id - location id
        // value: location utilization snapshot detail record
        Map<String, LocationUtilizationSnapshotDetail> locationUtilizationSnapshotDetailMap = new HashMap<>();

        inventories.forEach(
                inventory -> {
                    String key = new StringBuilder()
                            .append(Objects.nonNull(inventory.getClientId()) ? inventory.getClientId().toString() : "")
                            .append("-")
                            .append(inventory.getWarehouseId())
                            .append("-")
                            .append(inventory.getItem().getId())
                            .append("-")
                            .append(inventory.getLocationId())
                            .toString();
                    // get the location size first
                    double locationSize = 0.0;
                    if (locationSizes.containsKey(inventory.getLocationId())) {
                        locationSize = locationSizes.get(inventory.getLocationId());
                    }
                    else {
                        Location location = warehouseLayoutServiceRestemplateClient.getLocationById(
                                inventory.getLocationId()
                        );
                        locationSize = location.getCapacity();
                        locationSizes.put(inventory.getLocationId(), locationSize);
                    }
                    // get the net volume and gross volume of the inventory
                    double netVolume = getNetVolume(inventory, itemVolumeTrackingLevel);
                    double grossVolume = getGrossVolume(inventory, itemVolumeTrackingLevel);
                    if (locationUtilizationSnapshotDetailMap.containsKey(key)) {

                        LocationUtilizationSnapshotDetail existingLocationUtilizationSnapshotDetail =
                                locationUtilizationSnapshotDetailMap.get(key);
                        netVolume += existingLocationUtilizationSnapshotDetail.getNetVolume();
                        grossVolume += existingLocationUtilizationSnapshotDetail.getGrossVolume();
                    }
                    locationUtilizationSnapshotDetailMap.put(
                            key,
                            new LocationUtilizationSnapshotDetail(
                                    inventory.getWarehouseId(),
                                    inventory.getItem().getId(),
                                    inventory.getClientId(),
                                    netVolume,
                                    grossVolume,
                                    inventory.getLocationId(),
                                    locationSize
                            )
                    );

                }
        );

        return locationUtilizationSnapshotDetailMap.values();
    }

    private double getGrossVolume(Inventory inventory, ItemVolumeTrackingLevel itemVolumeTrackingLevel) {

        // to be implement
        return 0.0;
    }

    private double getNetVolume(Inventory inventory, ItemVolumeTrackingLevel itemVolumeTrackingLevel) {
        switch (itemVolumeTrackingLevel) {
            case BY_CASE_UOM:
                return getNetVolumeByCase(inventory);
            case BY_STOCK_UOM:
                return getNetVolumeByStockUOM(inventory);
            default:
                return 0.0;
        }
    }

    private double getNetVolumeByStockUOM(Inventory inventory) {
        // see how many stock does the inventory have
        ItemUnitOfMeasure stockUOM = inventory.getItemPackageType().getStockItemUnitOfMeasure();
        return stockUOM.getLength() * stockUOM.getWeight() * stockUOM.getHeight() * (
                inventory.getQuantity() / stockUOM.getQuantity()
        );
    }

    private double getNetVolumeByCase(Inventory inventory) {
        // see how many cases does the inventory have
        ItemUnitOfMeasure caseUOM = inventory.getItemPackageType().getCaseUnitOfMeasure();

        if (Objects.nonNull(caseUOM)) {
            return caseUOM.getLength() * caseUOM.getWidth() * caseUOM.getHeight() * (
                    inventory.getQuantity() / caseUOM.getQuantity()
            );
        }
        else {
            // case uom is not defined for this item, let's
            // get the net volume by stock uom
            return getNetVolumeByStockUOM(inventory);
        }
    }

}
