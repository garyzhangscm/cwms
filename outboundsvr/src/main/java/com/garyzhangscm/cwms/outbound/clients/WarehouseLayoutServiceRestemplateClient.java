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

package com.garyzhangscm.cwms.outbound.clients;

import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.Location;
import com.garyzhangscm.cwms.outbound.model.LocationGroup;
import com.garyzhangscm.cwms.outbound.model.LocationGroupType;
import com.garyzhangscm.cwms.outbound.model.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;

import org.springframework.core.ParameterizedTypeReference;

import org.springframework.http.HttpMethod;

import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;


import java.util.List;

@Component
@CacheConfig(cacheNames = "locations")
public class WarehouseLayoutServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseLayoutServiceRestemplateClient.class);
    @Autowired
    OAuth2RestOperations restTemplate;
    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;



    public Location getLocationById(Long id) {
        UriComponentsBuilder builder =
            UriComponentsBuilder.newInstance()
                    .scheme("http").host("zuulservice")
                    .path("/api/layout/locations/{id}");

        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public Location getLocationByName(String warehouseName, String name) {
        Warehouse warehouse = getWarehouseByName(warehouseName);
        if (warehouse == null) {
            throw ResourceNotFoundException.raiseException("warehouse name (" + warehouseName +  ")is not valid");
        }
        return getLocationByName(warehouse.getId(), name);
    }

    public Location getLocationByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
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

    public Warehouse getWarehouseById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/warehouses/{id}");

        ResponseBodyWrapper<Warehouse> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Warehouse>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public Warehouse getWarehouseByName(String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/warehouses")
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

    public LocationGroup getLocationGroupById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/locationgroups/{id}");

        ResponseBodyWrapper<LocationGroup> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroup>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public LocationGroup getLocationGroupByName(String warehouseName, String name) {
        Warehouse warehouse = getWarehouseByName(warehouseName);
        if (warehouse == null) {
            throw ResourceNotFoundException.raiseException("warehouse name (" + warehouseName +  ")is not valid");
        }
        return getLocationGroupByName(warehouse.getId(), name);
    }
    public LocationGroup getLocationGroupByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
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
    public LocationGroupType getLocationGroupTypeById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/locationgrouptypes/{id}");



        ResponseBodyWrapper<LocationGroupType> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroupType>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public LocationGroupType getLocationGroupTypeByName(String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/locationgrouptypes")
                        .queryParam("name", name);

        ResponseBodyWrapper<LocationGroupType[]> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroupType[]>>() {}).getBody();

        LocationGroupType[] locationGroupTypes = responseBodyWrapper.getData();

        if (locationGroupTypes.length != 1) {
            logger.debug("getLocationGroupTypeByName / {} return {} location group types. Error!!!", name, locationGroupTypes.length);
            return null;
        }
        else {

            return locationGroupTypes[0];
        }
    }



    public Location reserveLocationFromGroup(Long locationGroupId, String reservedCode,
                                    Double pendingSize, Long pendingQuantity, Integer pendingPalletQuantity) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
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

    public Location reserveLocation(Long locationId, String reservedCode,
                                             Double pendingSize, Long pendingQuantity, Integer pendingPalletQuantity) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/locations/{locationId}/reserveWithVolume")
                        .queryParam("reservedCode", reservedCode)
                        .queryParam("pendingSize", pendingSize)
                        .queryParam("pendingQuantity", pendingQuantity)
                        .queryParam("pendingPalletQuantity", pendingPalletQuantity);

        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(locationId).toUriString(),
                        HttpMethod.PUT,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }


    public List<Location> findEmptyDockLocations(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/locations/dock")
                        .queryParam("empty", true)
                        .queryParam("warehouseId", warehouseId);

        ResponseBodyWrapper<List<Location>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public Location checkInTrailerAtDockLocations(Long dockLocationId, Long trailerId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/locations/dock/{id}/check-in-trailer")
                        .queryParam("trailerId", trailerId);

        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(dockLocationId).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public Location dispatchTrailerFromDockLocations(Long dockLocationId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/locations/dock/{id}/dispatch-trailer");


        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(dockLocationId).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public Location getTrailerLocation(Long warehouseId, Long trailerId) {
        String locationName = "TRLR-" + trailerId;
        return getLocationByName(warehouseId, locationName);
    }

    public Location findEmptyDestinationLocationForEmergencyReplenishment(LocationGroup locationGroup,
                                                                          Double replenishmentSize) {
        logger.debug("Start to find an empty location in group {}, with at least empty capacity of {}, for emergency replenishment"
                        , locationGroup.getName(), replenishmentSize);
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/locations")
                        .queryParam("locationGroupIds", locationGroup.getId())
                        .queryParam("emptyLocationOnly", true)
                        .queryParam("minEmptyCapacity", replenishmentSize)
                        .queryParam("pickableLocationOnly", true)
                        .queryParam("maxResultCount", 1);

        ResponseBodyWrapper<List<Location>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {}).getBody();

        List<Location> locations = responseBodyWrapper.getData();
        if (locations.size() == 0) {
            throw ResourceNotFoundException.raiseException(
                    "Can't find suitable empty location from group: " + locationGroup.getName()
            );
        }
        return locations.get(0);
    }


}
