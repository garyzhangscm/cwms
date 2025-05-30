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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.exception.MissingInformationException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class WarehouseLayoutServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseLayoutServiceRestemplateClient.class);

    @Autowired
    InventoryService inventoryService;

    @Autowired
    private RestTemplateProxy restTemplateProxy;


    @Cacheable(cacheNames = "InventoryService_Company", unless="#result == null")
    public Company getCompanyById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/companies/{id}");
/**
        ResponseBodyWrapper<Company> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Company>>() {}).getBody();

        return responseBodyWrapper.getData();
**/

        return restTemplateProxy.exchange(
                Company.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public List<Company> getAllCompanies() {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/companies");

        return restTemplateProxy.exchangeList(
                Company.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public List<Warehouse> getWarehouseByCompany(Long companyId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouses")
                        .queryParam("companyId", companyId);

        logger.debug("Start to get warehouse by companyId: {}, /n >> {}",
                companyId, builder.toUriString());

        return restTemplateProxy.exchangeList(
                Warehouse.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    @Cacheable(cacheNames = "InventoryService_Location", unless="#result == null")
    public Location getLocationById(Long id) {
        logger.debug("Will get location by id {}", id);
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/{id}");
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    @Cacheable(cacheNames = "InventoryService_Location", unless="#result == null")
    public Location getLocationByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("name", name)
                        .queryParam("warehouseId", warehouseId);

/**
        ResponseBodyWrapper<Location[]> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {}).getBody();

        Location[] locations = responseBodyWrapper.getData();
 **/
        List<Location> locations = restTemplateProxy.exchangeList(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
        logger.debug(">> Get {} locations by name: {}", locations.size(), name);
        if (locations.size() != 1) {
            logger.debug("getLocationByName / {} return {} locations. Error!!!", name, locations.size());
            return null;
        }
        else {
            return locations.get(0);
        }


    }


    @Cacheable(cacheNames = "InventoryService_Location", unless="#result == null")
    public List<Location> getLocationsByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("name", name)
                        .queryParam("warehouseId", warehouseId);

        return restTemplateProxy.exchangeList(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );


    }


    @Cacheable(cacheNames = "InventoryService_Warehouse", unless="#result == null")
    public Warehouse getWarehouseByName(String companyCode, String name)   {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouses")
                        .queryParam("companyCode", companyCode)
                        .queryParam("name", name);

        logger.debug("Start to get warehouse by name: {}, /n >> {}",
                name, builder.toUriString());
/**
        ResponseBodyWrapper<List<Warehouse>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Warehouse>>>() {}).getBody();


        List<Warehouse> warehouses = responseBodyWrapper.getData();
**/

        List<Warehouse> warehouses = restTemplateProxy.exchangeList(
                Warehouse.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

        if (warehouses.size() != 1) {
            return null;
        }
        else {
            return warehouses.get(0);
        }
    }


    @Cacheable(cacheNames = "InventoryService_Warehouse", unless="#result == null")
    public Warehouse getWarehouseById(long warehouseId)   {
/**
        if (warehouseMap.containsKey(warehouseId)) {
            logger.debug("start to get warehouse from LOCAL cache by id {}", warehouseId);
            return warehouseMap.get(warehouseId);
        }
**/
        logger.debug("start to get warehouse by id {}", warehouseId);
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouses/{id}");
/**
        ResponseBodyWrapper<Warehouse> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.buildAndExpand(warehouseId).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Warehouse>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Warehouse.class,
                builder.buildAndExpand(warehouseId).toUriString(),
                HttpMethod.GET,
                null
        );


    }

    @Cacheable(cacheNames = "InventoryService_WarehouseConfiguration", unless="#result == null")
    public WarehouseConfiguration getWarehouseConfiguration(long warehouseId)   {
        logger.debug("start to get warehouse configuration from warehouse id {}",
                warehouseId);


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouse-configuration/by-warehouse/{id}");
/**
        ResponseBodyWrapper<WarehouseConfiguration> responseBodyWrapper
             = restTemplateProxy.getRestTemplate().exchange(
                   builder.buildAndExpand(warehouseId).toUriString(),
                   HttpMethod.GET,
                 null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WarehouseConfiguration>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                WarehouseConfiguration.class,
                builder.buildAndExpand(warehouseId).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    /**
     * Get all warehouse configuration
     * @return
     */
    public List<WarehouseConfiguration> getWarehouseConfiguration()   {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouse-configuration");
/**
        ResponseBodyWrapper<List<WarehouseConfiguration>> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<WarehouseConfiguration>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                WarehouseConfiguration.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public Location lockLocation(Long locationId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/{id}/lock")
                        .queryParam("locked", true);
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(locationId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(locationId).toUriString(),
                HttpMethod.POST,
                null
        );

    }

    public Location releaseLocationLock(Long locationId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/{id}/lock")
                        .queryParam("locked", false);
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(locationId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(locationId).toUriString(),
                HttpMethod.POST,
                null
        );

    }

    public List<LocationGroup> getStorageLocationGroups(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locationgroups/storage")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<LocationGroup>> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<LocationGroup>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                LocationGroup.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public List<LocationGroup> getQCLocationGroups(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locationgroups/qc")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<LocationGroup>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<LocationGroup>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                LocationGroup.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public List<Location> getQCLocations(Long warehouseId) {
        List<LocationGroup> qcLocationGroups = getQCLocationGroups(warehouseId);
        String locationGroupIds = qcLocationGroups.stream().map(
                qcLocationGroup -> qcLocationGroup.getId()
            ).map(id -> id.toString() )
             .collect( Collectors.joining( "," ) );
        return getLocationByLocationGroups(warehouseId, locationGroupIds);
    }

    public List<Long> getLocationIdsByLocationGroups(Long warehouseId, String locationGroupIds) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/location-ids")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("locationGroupIds", locationGroupIds);
/**
        ResponseBodyWrapper<List<Long>> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Long>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                Long.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }


    public List<Location> getLocationByLocationGroups(Long warehouseId, String locationGroupIds) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("locationGroupIds", locationGroupIds);
/**
        ResponseBodyWrapper<List<Location>> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public List<Location> getLocationByPickZones(Long warehouseId, String pickZoneIds) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("pickZoneIds", pickZoneIds); 
        return restTemplateProxy.exchangeList(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public List<Location> getLocationsByRange(Long warehouseId, Long beginSequence, Long endSequence, String sequenceType, Boolean includeEmptyLocation) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("beginSequence", beginSequence)
                        .queryParam("endSequence", endSequence)
                        .queryParam("sequenceType", sequenceType)
                        .queryParam("includeEmptyLocation", includeEmptyLocation);
/**
        ResponseBodyWrapper<List<Location>> responseBodyWrapper =
                restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );


    }

    public List<Location> getLocationByAisle(Long warehouseId, String aisle, boolean includeEmptyLocation) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("aisle", aisle)
                        .queryParam("includeEmptyLocation", includeEmptyLocation);
/**
        ResponseBodyWrapper<List<Location>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public List<Location> getLocationByAisleRange(
            Long warehouseId, String beginValue, String endValue, boolean includeEmptyLocation) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("beginAisle", beginValue)
                        .queryParam("endAisle", endValue)
                        .queryParam("includeEmptyLocation", includeEmptyLocation);
/**
        ResponseBodyWrapper<List<Location>> responseBodyWrapper =
                restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
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
        Location location = getLocationById(locationId);
        resetLocation(location);
    }
    public void resetLocation(Location location) {
        resetLocationVolume(location);
    }

    private Location resetLocationVolume(Location location) {

        logger.debug("See if we will need to recalculate the location {} 's volume? {}",
                location.getName(), location.getLocationGroup().getTrackingVolume());
        if (!location.getLocationGroup().getTrackingVolume()) {
            return location;
        }

        List<Inventory> inventories = inventoryService.findByLocationId(location.getId());
        Long totalQuantity = 0L;
        Double totalSize = 0.0;
        for (Inventory inventory : inventories) {
            totalQuantity += inventory.getQuantity();
            totalSize += inventory.getSize();
        }


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/{id}")
                        .queryParam("inventoryQuantity", totalQuantity)
                        .queryParam("inventorySize", totalSize);
/**
        ResponseBodyWrapper<Location> responseBodyWrapper =
                restTemplateProxy.getRestTemplate().exchange(
                        builder.buildAndExpand(location.getId()).toUriString(),
                        HttpMethod.PUT,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(location.getId()).toUriString(),
                HttpMethod.PUT,
                null
        );


    }


    public Location getDefaultRemovedInventoryLocation(long warehouseId) {
        logger.debug("Start to get default removed inventory location by warehouse id {}", warehouseId);
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/logic/default-removed-inventory-location")
                .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }


    public Location getLocationForAuditCount(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/logic/audit-count")
                        .queryParam("warehouseId", warehouseId);

/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public Location getLocationForCount(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/logic/count")
                        .queryParam("warehouseId", warehouseId);

/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public Location getLocationForInventoryAdjustment(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/logic/inventory-adjust")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public Location getLocationForReceiving(Long warehouseId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/logic/receiving")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public Location getLogicalLocationForAdjustInventory(InventoryQuantityChangeType inventoryQuantityChangeType, long warehouseId) {
        /**
        if (logicalLocationForAdjustInventoryMap.containsKey(
                inventoryQuantityChangeType + "-" + warehouseId
        )) {
            logger.debug("getLogicalLocationForAdjustInventory from local cache inventoryQuantityChangeType: {}, warehouse id {}",
                    inventoryQuantityChangeType, warehouseId);
            return logicalLocationForAdjustInventoryMap.get(
                    inventoryQuantityChangeType + "-" + warehouseId);
        }
         **/
        logger.debug("getLogicalLocationForAdjustInventory with inventoryQuantityChangeType: {}, warehouse id {}",
                inventoryQuantityChangeType, warehouseId);

        switch (inventoryQuantityChangeType){
            case INVENTORY_ADJUST:
            case REVERSE_PRODUCTION:
                return getLocationForInventoryAdjustment(warehouseId);
            case AUDIT_COUNT:
                return   getLocationForAuditCount(warehouseId);
            case CYCLE_COUNT:
                return  getLocationForCount(warehouseId);
            case RECEIVING:
            case PRODUCING:
            case RETURN_MATERAIL:
                return getLocationForReceiving(warehouseId);
            default:
                return getDefaultRemovedInventoryLocation(warehouseId);
        }
    }
    @Cacheable( cacheNames = "InventoryService_LocationGroupType", unless="#result == null")
    public LocationGroupType getLocationGroupTypeById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locationgrouptypes/{id}");
/**
        ResponseBodyWrapper<LocationGroupType> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroupType>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                LocationGroupType.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }
    @Cacheable( cacheNames = "InventoryService_LocationGroup", unless="#result == null")
    public LocationGroup getLocationGroupById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locationgroups/{id}");
/**
        ResponseBodyWrapper<LocationGroup> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroup>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                LocationGroup.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }
    @Cacheable( cacheNames = "InventoryService_LocationGroup", unless="#result == null")
    public LocationGroup getLocationGroupByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locationgroups")
                        .queryParam("name", name)
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<LocationGroup[]> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroup[]>>() {}).getBody();

        LocationGroup[] locationGroups = responseBodyWrapper.getData();
**/

        List<LocationGroup> locationGroups =  restTemplateProxy.exchangeList(
                LocationGroup.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
        if (locationGroups.size() != 1) {
            logger.debug("getLocationGroupByName / {} return {} location groups. Error!!!", name, locationGroups.size());
            return null;
        }
        else {

            return locationGroups.get(0);
        }
    }



    public Location reserveLocationFromGroup(Long locationGroupId, String reservedCode,
                                    Double pendingSize, Long pendingQuantity, Integer pendingPalletQuantity) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locationgroups/{locationGroupId}/reserve")
                        .queryParam("reservedCode", reservedCode)
                        .queryParam("pendingSize", pendingSize)
                        .queryParam("pendingQuantity", pendingQuantity)
                        .queryParam("pendingPalletQuantity", pendingPalletQuantity);

/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(locationGroupId).toUriString(),
                        HttpMethod.PUT,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(locationGroupId).toUriString(),
                HttpMethod.PUT,
                null
        );

    }
    public Location reduceLocationPendingVolume(Long id, Double pendingVolumeReduced) {


        logger.debug("Will reduce pending volume of location for id {} by {}",
                id, pendingVolumeReduced);
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/{id}/pending-volume")
                        .queryParam("reduce", pendingVolumeReduced);
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.POST,
                null
        );

    }

    public Location reduceLocationVolume(Long id, Double volumeReduced) {



        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/{id}/volume")
                        .queryParam("reduce", volumeReduced);

/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.POST,
                null
        );

    }

    public Location increaseLocationVolume(Long id, Double volumeIncreased) {


        logger.debug("===> Start to increase location volume, location id: {}, volume: {}",
                id, volumeIncreased);

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/{id}/volume")
                        .queryParam("increase", volumeIncreased)
                        .queryParam("fromPendingVolume", true);
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.POST,
                null
        );

    }

    public InventoryConsolidationStrategy getInventoryConsolidationStrategy(long locationGroupId) {
        /**
        if (inventoryConsolidationStrategyMap.containsKey(locationGroupId)) {

            logger.debug("start to load inventory consolidation stragety from LOCAL cache with location group id {}",
                    locationGroupId);
            return inventoryConsolidationStrategyMap.get(locationGroupId);
        }**/

        logger.debug("start to load inventory consolidation stragety with location group id {}",
                locationGroupId);
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locationgroups/{id}/inventory-consolidation-strategy");
/**
        ResponseBodyWrapper<InventoryConsolidationStrategy> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.buildAndExpand(locationGroupId).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<InventoryConsolidationStrategy>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                InventoryConsolidationStrategy.class,
                builder.buildAndExpand(locationGroupId).toUriString(),
                HttpMethod.GET,
                null
        );



    }

    public Map<String, String> getUtilizationTrackingLocations( Long warehouseId)  {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/utilization-tracking")
                .queryParam("warehouseId", warehouseId);

        ResponseBodyWrapper<Map<String, String>> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Map<String, String>>>() {}).getBody();

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
        logger.debug("start to deallocateLocation\n {}",
                location);
        if (Boolean.TRUE.equals(location.getLocationGroup().getTrackingVolume()) &&
              location.getLocationGroup().getVolumeTrackingPolicy().equals(
                LocationVolumeTrackingPolicy.BY_EACH
            )) {
            inventorySize = inventory.getQuantity() * 1.0;
        }
        else {

            inventorySize = inventory.getSize();
        }
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout//locations/{id}/pending-volume")
                        .queryParam("reduce", inventorySize);
/**

        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(location.getId()).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(location.getId()).toUriString(),
                HttpMethod.POST,
                null
        );

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
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/{id}/allocate")
                        .queryParam("inventorySize", inventorySize);

/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(location.getId()).toUriString(),
                HttpMethod.PUT,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(location.getId()).toUriString(),
                HttpMethod.PUT,
                null
        );

    }
}
