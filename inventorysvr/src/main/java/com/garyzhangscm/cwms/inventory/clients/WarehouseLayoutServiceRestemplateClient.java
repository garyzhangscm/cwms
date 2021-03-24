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
import com.garyzhangscm.cwms.inventory.exception.MissingInformationException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class WarehouseLayoutServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseLayoutServiceRestemplateClient.class);

    @Autowired
    OAuth2RestOperations restTemplate;

    @Autowired
    InventoryService inventoryService;


    public Company getCompanyById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/companies/{id}");

        ResponseBodyWrapper<Company> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Company>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public Location getLocationById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/{id}");

        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public Location getLocationByName(Long warehouseId, String name) {



        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("name", name)
                        .queryParam("warehouseId", warehouseId);


        ResponseBodyWrapper<Location[]> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {}).getBody();

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


    public Warehouse getWarehouseByName(String companyCode, String name)   {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/warehouses")
                        .queryParam("companyCode", companyCode)
                        .queryParam("name", name);

        logger.debug("Start to get warehouse by name: {}, /n >> {}",
                name, builder.toUriString());

        ResponseBodyWrapper<List<Warehouse>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Warehouse>>>() {}).getBody();


        List<Warehouse> warehouses = responseBodyWrapper.getData();

        if (warehouses.size() != 1) {
            return null;
        }
        else {
            return warehouses.get(0);
        }
    }


    public Warehouse getWarehouseById(Long warehouseId)   {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/warehouses/{id}");

        ResponseBodyWrapper<Warehouse> responseBodyWrapper
             = restTemplate.exchange(
                   builder.buildAndExpand(warehouseId).toUriString(),
                   HttpMethod.GET,
                 null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Warehouse>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public Location lockLocation(Long locationId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/{id}/lock")
                        .queryParam("locked", true);

        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(locationId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public Location releaseLocationLock(Long locationId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/{id}/lock")
                        .queryParam("locked", false);

        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(locationId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Location> getLocationByLocationGroups(Long warehouseId, String locationGroupIds) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("locationGroupIds", locationGroupIds);

        ResponseBodyWrapper<List<Location>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public List<Location> getLocationsByRange(Long warehouseId, Long beginSequence, Long endSequence, String sequenceType, Boolean includeEmptyLocation) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("beginSequence", beginSequence)
                        .queryParam("endSequence", endSequence)
                        .queryParam("sequenceType", sequenceType)
                        .queryParam("includeEmptyLocation", includeEmptyLocation);

        ResponseBodyWrapper<List<Location>> responseBodyWrapper =
                restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Location> getLocationByAisle(String aisle) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("aisle", aisle);

        ResponseBodyWrapper<List<Location>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Location> getLocationByAisleRange(String beginValue, String endValue) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("beginAisle", beginValue)
                        .queryParam("beginAisle", endValue);

        ResponseBodyWrapper<List<Location>> responseBodyWrapper =
                restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {}).getBody();

        return responseBodyWrapper.getData();
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


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/{id}")
                        .queryParam("inventoryQuantity", totalQuantity)
                        .queryParam("inventorySize", totalSize);

        ResponseBodyWrapper<Location> responseBodyWrapper =
                restTemplate.exchange(
                        builder.buildAndExpand(locationId).toUriString(),
                        HttpMethod.PUT,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public Location getDefaultRemovedInventoryLocation(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/logic/default-removed-inventory-location")
                .queryParam("warehouseId", warehouseId);

        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public Location getLocationForAuditCount(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/logic/audit-count")
                        .queryParam("warehouseId", warehouseId);


        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public Location getLocationForCount(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/logic/count")
                        .queryParam("warehouseId", warehouseId);


        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public Location getLocationForInventoryAdjustment(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/logic/inventory-adjust")
                        .queryParam("warehouseId", warehouseId);

        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public Location getLocationForReceiving(Long warehouseId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/logic/receiving")
                        .queryParam("warehouseId", warehouseId);

        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public Location getLogicalLocationForAdjustInventory(InventoryQuantityChangeType inventoryQuantityChangeType, Long warehouseId) {
        switch (inventoryQuantityChangeType){
            case INVENTORY_ADJUST:
                return getLocationForInventoryAdjustment(warehouseId);
            case AUDIT_COUNT:
                return getLocationForAuditCount(warehouseId);
            case CYCLE_COUNT:
                return getLocationForCount(warehouseId);
            case RECEIVING:
            case PRODUCING:
            case RETURN_MATERAIL:
                return getLocationForReceiving(warehouseId);
            default:
                return getDefaultRemovedInventoryLocation(warehouseId);
        }
    }
    public LocationGroup getLocationGroupById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locationgroups/{id}");

        ResponseBodyWrapper<LocationGroup> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroup>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public LocationGroup getLocationGroupByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locationgroups")
                        .queryParam("name", name)
                        .queryParam("warehouseId", warehouseId);

        ResponseBodyWrapper<LocationGroup[]> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroup[]>>() {}).getBody();

        LocationGroup[] locationGroups = responseBodyWrapper.getData();

        if (locationGroups.length != 1) {
            logger.debug("getLocationGroupByName / {} return {} location groups. Error!!!", name, locationGroups.length);
            return null;
        }
        else {

            return locationGroups[0];
        }
    }



    public Location reserveLocationFromGroup(Long locationGroupId, String reservedCode,
                                    Double pendingSize, Long pendingQuantity, Integer pendingPalletQuantity) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locationgroups/{locationGroupId}/reserve")
                        .queryParam("reservedCode", reservedCode)
                        .queryParam("pendingSize", pendingSize)
                        .queryParam("pendingQuantity", pendingQuantity)
                        .queryParam("pendingPalletQuantity", pendingPalletQuantity);


        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(locationGroupId).toUriString(),
                        HttpMethod.PUT,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public Location reduceLocationPendingVolume(Long id, Double pendingVolumeReduced) {


        logger.debug("Will reduce pending volume of location for id {} by {}",
                id, pendingVolumeReduced);
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/{id}/pending-volume")
                        .queryParam("reduce", pendingVolumeReduced);

        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public Location reduceLocationVolume(Long id, Double volumeReduced) {



        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/{id}/volume")
                        .queryParam("reduce", volumeReduced);


        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public Location increaseLocationVolume(Long id, Double volumeIncreased) {


        logger.debug("===> Start to increase location volume, location id: {}, volume: {}",
                id, volumeIncreased);

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/{id}/volume")
                        .queryParam("increase", volumeIncreased)
                        .queryParam("fromPendingVolume", true);

        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public InventoryConsolidationStrategy getInventoryConsolidationStrategy(LocationGroup locationGroup) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locationgroups/{id}/inventory-consolidation-strategy");

        ResponseBodyWrapper<InventoryConsolidationStrategy> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(locationGroup.getId()).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<InventoryConsolidationStrategy>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public boolean isVirtualLocation(Location location) {

        if (Objects.isNull(location.getLocationGroup()) ||
                Objects.isNull(location.getLocationGroup().getLocationGroupType())) {
            throw MissingInformationException.raiseException(
                    "Objects.isNull(location.getLocationGroup())?: " + Objects.isNull(location.getLocationGroup()) +
                    "Objects.isNull(location.getLocationGroup().getLocationGroupType())?: " + Objects.isNull(location.getLocationGroup().getLocationGroupType()));
        }

        return location.getLocationGroup().getLocationGroupType().getVirtual();
    }

    public Location deallocateLocation(Location location, Inventory inventory) {
        Double inventorySize = 0.0;
        if (location.getLocationGroup().getVolumeTrackingPolicy().equals(
                LocationVolumeTrackingPolicy.BY_EACH
        )) {
            inventorySize = inventory.getQuantity() * 1.0;
        }
        else {

            inventorySize = inventory.getSize();
        }
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout//locations/{id}/pending-volume")
                        .queryParam("reduce", inventorySize);


        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(location.getId()).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public Location allocateLocation(Location location, Inventory inventory) {
        Double inventorySize = 0.0;
        if (location.getLocationGroup().getVolumeTrackingPolicy().equals(
                LocationVolumeTrackingPolicy.BY_EACH
        )) {
            inventorySize = inventory.getQuantity() * 1.0;
        }
        else {

            inventorySize = inventory.getSize();
        }
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/{id}/allocate")
                        .queryParam("inventorySize", inventorySize);


        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(location.getId()).toUriString(),
                HttpMethod.PUT,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }



}
