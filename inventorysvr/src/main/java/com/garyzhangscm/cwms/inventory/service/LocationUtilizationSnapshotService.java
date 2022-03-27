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
import java.util.*;
import java.util.stream.Collectors;

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

    public List<LocationUtilizationSnapshot> generateLocationUtilizationSnapshot(
            Long warehouseId ) {

        Collection<LocationUtilizationSnapshotDetail> locationUtilizationSnapshotDetails =
                getLocationUtilizationSnapshotDetails(warehouseId);

        // group the details into location utilization snapshot
        // group by warehouse id + client id + item id
        Map<String, LocationUtilizationSnapshot> locationUtilizationSnapshotMap = new HashMap<>();

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
                    existingLocationUtilizationSnapshot.setGrossVolume(
                            existingLocationUtilizationSnapshot.getGrossVolume() +
                                    locationUtilizationSnapshotDetail.getGrossVolume()
                    );
                    existingLocationUtilizationSnapshot.setTotalLocations(
                            existingLocationUtilizationSnapshot.getTotalLocations() + 1
                    );
                    existingLocationUtilizationSnapshot.addLocationUtilizationSnapshotDetail(
                            locationUtilizationSnapshotDetail
                    );
                    locationUtilizationSnapshotDetail.setLocationUtilizationSnapshot(
                            existingLocationUtilizationSnapshot
                    );

                    locationUtilizationSnapshotMap.put(key, existingLocationUtilizationSnapshot);
                }
        );
        return locationUtilizationSnapshotMap.entrySet().stream().map(
                entry -> save(entry.getValue())
        ).collect(Collectors.toList());
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
