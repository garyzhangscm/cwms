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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.inbound.ResponseBodyWrapper;
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
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@CacheConfig(cacheNames = "warehouse_layout")
public class WarehouseLayoutServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseLayoutServiceRestemplateClient.class);

    @Autowired
    OAuth2RestTemplate restTemplate;
    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;

    private ObjectMapper mapper = new ObjectMapper();

    public Location getLocationById(Long id) {
        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, id).getBody();

        return responseBodyWrapper.getData();

    }

    public Location getLocationByName(Long warehouseId, String name) {

        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/layout/locations?")
                .append("name={name}")
                .append("&warehouseId={warehouseId}");
        ResponseBodyWrapper<Location[]> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {
                }, name, warehouseId).getBody();

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

    public String getWarehouseName(Long warehouseId) {
        Warehouse warehouse = getWarehouseById(warehouseId);
        if (warehouse == null) {
            return "";
        }
        else {
            return warehouse.getName();
        }
    }

    @Cacheable
    public Warehouse getWarehouseByName(String name) {

        ResponseBodyWrapper<List<Warehouse>> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/warehouses?name={name}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Warehouse>>>() {
                }, name).getBody();

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
        StringBuilder url = new StringBuilder()
                        .append("http://zuulserver:5555/api/layout/locations?")
                        .append("locationGroupIds={locationGroupIds}")
                        .append("&warehouseId={warehouseId}");

        ResponseBodyWrapper<Location[]> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {},
                locationGroupIds, warehouseId).getBody();

        return responseBodyWrapper.getData();
    }
    public Location[] getLocationByLocationGroupTypes(Long warehouseId, String locationGroupTypeIds) {

        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/layout/locations?")
                .append("locationGroupTypeIds={locationGroupTypeIds}")
                .append("&warehouseId={warehouseId}");

        ResponseBodyWrapper<Location[]> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {
                },
                locationGroupTypeIds, warehouseId).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Location> getLocationsByRnage(Long beginSequence, Long endSequence, String sequenceType) {
        return getLocationsByRnage(beginSequence, endSequence, sequenceType, true);
    }
    public List<Location> getLocationsByRnage(Long beginSequence, Long endSequence, String sequenceType, Boolean includeEmptyLocation) {

        String url = "http://zuulserver:5555/api/layout/locations?" +
                      "begin_sequence={beginSequence}&end_sequence={endSequence}&sequence_type={sequenceType}&include_empty_location={includeEmptyLocation}";

        ResponseBodyWrapper<Location[]> responseBodyWrapper =
                restTemplate.exchange(url,
                        HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {
                }, beginSequence, endSequence, sequenceType, includeEmptyLocation).getBody();

        Location[] locations = responseBodyWrapper.getData();
        return Arrays.asList(locations);
    }

    @Cacheable
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

    public LocationGroup getLocationGroupById(Long id) {

        ResponseBodyWrapper<LocationGroup> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locationgroups/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroup>>() {
                }, id).getBody();

        return responseBodyWrapper.getData();
    }
    public LocationGroup getLocationGroupByName(Long warehouseId, String name) {
        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/layout/locationgroups?")
                .append("name={name}")
                .append("&warehouseId={warehouseId}");

        ResponseBodyWrapper<LocationGroup[]> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroup[]>>() {
                }, name, warehouseId).getBody();

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

    public Location createLocationForReceipt(Receipt receipt) throws IOException {
        Location location = new Location();
        location.setName(receipt.getNumber());
        location.setEnabled(true);
        String receiptLocationGroup = commonServiceRestemplateClient.getPolicyByKey("LOCATION-GROUP-RECEIPT").getValue();
        Warehouse warehouse = getWarehouseById(receipt.getWarehouseId());
        location.setWarehouse(warehouse);

        location.setLocationGroup(getLocationGroupByName(warehouse.getId(),receiptLocationGroup));
        // Convert the inventory to JSON and send to the inventory service
        String requestBody = mapper.writeValueAsString(location);
        logger.debug("add location: {}", requestBody);
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        HttpEntity<String> httpEntity = new HttpEntity<String>(requestBody, headers);

        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations",
                HttpMethod.POST, httpEntity, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
    }


    public Location allocateLocation(Location location, Double inventorySize) {

        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations/{id}/allocate?inventory_size={inventorySize}",
                HttpMethod.PUT, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, location.getId(), inventorySize).getBody();

        return responseBodyWrapper.getData();
    }

}
