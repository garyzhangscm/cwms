/**
 * Copyright 2019
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

package com.garyzhangscm.cwms.inventory.clients;

import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Component
@CacheConfig(cacheNames = "warehouse_layout")
public class WarehouseLayoutServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseLayoutServiceRestemplateClient.class);

    @Autowired
    OAuth2RestTemplate restTemplate;

    @Autowired
    InventoryService inventoryService;

    public Location getLocationById(Long id) {
        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, id).getBody();

        return responseBodyWrapper.getData();

    }

    public Location getLocationByName(String warehouseName, String name) {

        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/layout/locations?")
                .append("name={name}")
                .append("&warehouseName={warehouseName}");
        ResponseBodyWrapper<Location[]> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {
                }, name, warehouseName).getBody();

        Location[] locations = responseBodyWrapper.getData();
        logger.debug(">> Get {} locations by name: {}", locations.length, name);
        if (locations.length != 1) {
            logger.debug("getLocationByName / {} return {} locations. Error!!!", name, locations.length);
            return null;
        }
        else {
            return locations[0];
        }
    }

    @Cacheable
    public Warehouse getWarehouseById(Long id) {
        ResponseBodyWrapper<Warehouse> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/warehouses/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Warehouse>>() {
                }, id).getBody();

        return responseBodyWrapper.getData();

    }

    @Cacheable
    public Warehouse getWarehouseByName(String name) {

        ResponseBodyWrapper<List<Warehouse>> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/warehouses?name={name}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Warehouse>>>() {
                }, name).getBody();

        List<Warehouse> warehouses = responseBodyWrapper.getData();
        logger.debug("getLocationByName / {} return {} locations. Error!!!", name, warehouses.size());
        if (warehouses.size() != 1) {
            return null;
        }
        else {
            return warehouses.get(0);
        }
    }


    public List<Location> getLocationByLocationGroups(String locationGroupIds) {

        ResponseBodyWrapper<List<Location>> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations?location_group_ids={locationGroupIds}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {
                }, locationGroupIds).getBody();

        return responseBodyWrapper.getData();
    }
    public List<Location> getLocationsByRange(Long beginSequence, Long endSequence, String sequenceType, Boolean includeEmptyLocation) {

        String url = "http://zuulserver:5555/api/layout/locations?" +
                      "begin_sequence={beginSequence}&end_sequence={endSequence}&sequence_type={sequenceType}&include_empty_location={includeEmptyLocation}";

        ResponseBodyWrapper<Location[]> responseBodyWrapper =
                restTemplate.exchange(url,
                        HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {
                }, beginSequence, endSequence, sequenceType, includeEmptyLocation).getBody();

        Location[] locations = responseBodyWrapper.getData();
        return Arrays.asList(locations);
    }

    public List<Location> getLocationByAisle(String aisle) {

        ResponseBodyWrapper<Location[]> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations?aisle={aisle}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {
                }, aisle).getBody();

        Location[] locations = responseBodyWrapper.getData();
        return Arrays.asList(locations);
    }

    public List<Location> getLocationByAisleRange(String beginValue, String endValue) {

        ResponseBodyWrapper<Location[]> responseBodyWrapper =
                restTemplate.exchange("http://zuulserver:5555/api/layout/locations?beginAisle={beginValue}&&beginAisle={endValue}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {
                }, beginValue, endValue).getBody();

        Location[] locations = responseBodyWrapper.getData();
        return Arrays.asList(locations);
    }

    public CycleCountRequest setupLocation(CycleCountRequest cycleCountRequest) {
        if (cycleCountRequest.getLocation() == null || cycleCountRequest.getLocation().getId() != cycleCountRequest.getLocationId()) {
            cycleCountRequest.setLocation(getLocationById(cycleCountRequest.getLocationId()));
        }
        return cycleCountRequest;
    }
    public CycleCountResult setupLocation(CycleCountResult cycleCountResult) {
        if (cycleCountResult.getLocation() == null || cycleCountResult.getLocation().getId() != cycleCountResult.getLocationId()) {
            cycleCountResult.setLocation(getLocationById(cycleCountResult.getLocationId()));
        }
        return cycleCountResult;
    }
    public AuditCountRequest setupLocation(AuditCountRequest auditCountRequest) {
        if (auditCountRequest.getLocation() == null || auditCountRequest.getLocation().getId() != auditCountRequest.getLocationId()) {
            auditCountRequest.setLocation(getLocationById(auditCountRequest.getLocationId()));
        }
        return auditCountRequest;
    }
    public AuditCountResult setupLocation(AuditCountResult auditCountResult) {
        if (auditCountResult.getLocation() == null || auditCountResult.getLocation().getId() != auditCountResult.getLocationId()) {
            auditCountResult.setLocation(getLocationById(auditCountResult.getLocationId()));
        }
        return auditCountResult;
    }


    public List<CycleCountRequest> setupCycleCountRequestLocations(List<CycleCountRequest> cycleCountRequests) {
        return cycleCountRequests.stream().map(cycleCountRequest -> setupLocation(cycleCountRequest))
                .collect(Collectors.toList());
    }
    public List<CycleCountResult> setupCycleCountResultLocations(List<CycleCountResult> cycleCountResults) {
        return cycleCountResults.stream().map(cycleCountResult -> setupLocation(cycleCountResult))
                .collect(Collectors.toList());
    }
    public List<AuditCountRequest> setupAuditCountRequestLocations(List<AuditCountRequest> auditCountRequests) {
        return auditCountRequests.stream().map(auditCountRequest -> setupLocation(auditCountRequest))
                .collect(Collectors.toList());
    }
    public List<AuditCountResult> setupAuditCountResultLocations(List<AuditCountResult> auditCountResults) {
        return auditCountResults.stream().map(auditCountResult -> setupLocation(auditCountResult))
                .collect(Collectors.toList());
    }

    public void resetLocation(Long locationId) {
        resetLocationVolume(locationId);
    }

    private Location resetLocationVolume(Long locationId) {
        List<Inventory> inventories = inventoryService.findByLocationId(locationId);
        Long totalQuantity = 0L;
        Double totalSize = 0.0;
        for (Inventory inventory : inventories) {
            totalQuantity += inventory.getQuantity();
            totalSize += inventory.getSize();
        }

        ResponseBodyWrapper<Location> responseBodyWrapper =
                restTemplate.exchange("http://zuulserver:5555/api/layout/locations/{id}?inventory_quantity={totalQuantity}&inventory_size={totalSize}",
                HttpMethod.PUT, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, locationId, totalQuantity, totalSize).getBody();

        return responseBodyWrapper.getData();

    }

    public Location getDefaultRemovedInventoryLocation() {

        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations/logic/default-removed-inventory-location",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }).getBody();

        return responseBodyWrapper.getData();
    }

    public Location getLocationForAuditCount() {

        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations/logic/audit-count",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }).getBody();

        return responseBodyWrapper.getData();
    }
    public Location getLocationForInventoryAdjustment() {

        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations/logic/inventory-adjust",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }).getBody();

        return responseBodyWrapper.getData();
    }

    public LocationGroup getLocationGroupById(Long id) {

        ResponseBodyWrapper<LocationGroup> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locationgroups/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroup>>() {
                }, id).getBody();

        return responseBodyWrapper.getData();
    }
    public LocationGroup getLocationGroupByName(String warehouseName, String name) {
        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/layout/locationgroups?")
                .append("name={name}")
                .append("&warehouseName={warehouseName}");

        ResponseBodyWrapper<LocationGroup[]> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroup[]>>() {
                }, name, warehouseName).getBody();

        LocationGroup[] locationGroups = responseBodyWrapper.getData();

        if (locationGroups.length != 1) {
            logger.debug("getLocationGroupByName / {} return {} location groups. Error!!!", name, locationGroups.length);
            return null;
        }
        else {

            return locationGroups[0];
        }
    }
    public LocationGroupType getLocationGroupTypeById(Long id) {

        ResponseBodyWrapper<LocationGroupType> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locationgrouptypes/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroupType>>() {
                }, id).getBody();

        return responseBodyWrapper.getData();
    }

    public LocationGroupType getLocationGroupTypeByName(String name) {

        ResponseBodyWrapper<LocationGroupType[]> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locationgrouptypes?name={name}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroupType[]>>() {
                }, name).getBody();

        LocationGroupType[] locationGroupTypes = responseBodyWrapper.getData();

        if (locationGroupTypes.length != 1) {
            logger.debug("getLocationGroupTypeByName / {} return {} location group types. Error!!!", name, locationGroupTypes.length);
            return null;
        }
        else {

            return locationGroupTypes[0];
        }
    }

    public Location reserveLocation(Long locationGroupId, String reservedCode,
                                    Double pendingSize, Long pendingQuantity, Integer pendingPalletQuantity) {

        StringBuilder url = new StringBuilder();
        url.append("http://zuulserver:5555/api/layout/locationgroups/{locationGroupId}/reserve?")
                .append("reserved_code={reservedCode}")
                .append("&pending_size={pendingSize}")
                .append("&pending_quantity={pendingQuantity}")
                .append("&pending_pallet_quantity={pendingPalletQuantity}");

        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange(url.toString(),
                HttpMethod.PUT, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, locationGroupId, reservedCode, pendingSize, pendingQuantity, pendingPalletQuantity).getBody();

        return responseBodyWrapper.getData();
    }
    public Location reduceLocationPendingVolume(Long id, Double pendingVolumeReduced) {

        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations/{id}/pending-volume?reduce={pendingVolumeReduced}",
                HttpMethod.POST, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, id, pendingVolumeReduced).getBody();

        return responseBodyWrapper.getData();
    }

    public Location reduceLocationVolume(Long id, Double volumeReduced) {

        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations/{id}/volume?reduce={volumeReduced}",
                HttpMethod.POST, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, id, volumeReduced).getBody();

        return responseBodyWrapper.getData();
    }

    public Location increaseLocationVolume(Long id, Double volumeIncreased) {

        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations/{id}/volume?increase={volumeIncreased}",
                HttpMethod.POST, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, id, volumeIncreased).getBody();

        return responseBodyWrapper.getData();
    }

}
