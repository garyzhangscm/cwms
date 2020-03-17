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

package com.garyzhangscm.cwms.inbound.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.inbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inbound.exception.ReceiptOperationException;
import com.garyzhangscm.cwms.inbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@CacheConfig(cacheNames = "warehouse_layout")
public class WarehouseLayoutServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseLayoutServiceRestemplateClient.class);

    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;

    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;

    private ObjectMapper mapper = new ObjectMapper();

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

    @Cacheable
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


    @Cacheable
    public Warehouse getWarehouseByName(String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/warehouses")
                        .queryParam("name", name);

        ResponseBodyWrapper<List<Warehouse>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                    null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Warehouse>>>() {}).getBody();

        List<Warehouse> warehouses = responseBodyWrapper.getData();
        if (warehouses.size() != 1) {
            logger.debug("getLocationByName / {} return {} locations. Error!!!", name, warehouses.size());
            return null;
        }
        else {
            return warehouses.get(0);
        }
    }

    public Location[] getLocationByLocationGroups(Long warehouseId, String locationGroupIds) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/locations")
                        .queryParam("locationGroupIds", locationGroupIds)
                        .queryParam("warehouseId", warehouseId);

        ResponseBodyWrapper<Location[]> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public Location[] getLocationByLocationGroupTypes(Long warehouseId, String locationGroupTypeIds) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/locations")
                        .queryParam("locationGroupTypeIds", locationGroupTypeIds)
                        .queryParam("warehouseId", warehouseId);


        ResponseBodyWrapper<Location[]> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Location> getLocationsByRange(Long beginSequence, Long endSequence, String sequenceType, Boolean includeEmptyLocation) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/locations")
                        .queryParam("beginSequence", beginSequence)
                        .queryParam("endSequence", endSequence)
                        .queryParam("sequenceType", sequenceType)
                        .queryParam("includeEmptyLocation", includeEmptyLocation);


        ResponseBodyWrapper<Location[]> responseBodyWrapper =
                restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {}).getBody();

        Location[] locations = responseBodyWrapper.getData();
        return Arrays.asList(locations);
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

    public Location createLocationForReceipt(Receipt receipt) {
        Location location = new Location();
        location.setName(receipt.getNumber());
        location.setEnabled(true);
        String receiptLocationGroup = commonServiceRestemplateClient.getPolicyByKey("LOCATION-GROUP-RECEIPT").getValue();
        Warehouse warehouse = getWarehouseById(receipt.getWarehouseId());
        location.setWarehouse(warehouse);

        location.setLocationGroup(getLocationGroupByName(warehouse.getId(),receiptLocationGroup));

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/layout/locations");

        ResponseBodyWrapper<Location> responseBodyWrapper
                = null;
        try {
            responseBodyWrapper = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.POST,
                    getHttpEntity(mapper.writeValueAsString(location)),
                    new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();
        } catch (JsonProcessingException e) {
            throw ReceiptOperationException.raiseException(
                    "Can't create the location for receipt due to JsonProcessingException: " + e.getMessage());
        }

        return responseBodyWrapper.getData();
    }


    public Location allocateLocation(Location location, Double inventorySize) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
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


    private HttpEntity<String> getHttpEntity(String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        return new HttpEntity<String>(requestBody, headers);
    }
}
