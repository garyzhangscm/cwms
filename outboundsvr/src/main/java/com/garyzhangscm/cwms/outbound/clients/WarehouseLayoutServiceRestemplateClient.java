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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.Location;
import com.garyzhangscm.cwms.outbound.model.LocationGroup;
import com.garyzhangscm.cwms.outbound.model.LocationGroupType;
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
@CacheConfig(cacheNames = "locations")
public class WarehouseLayoutServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseLayoutServiceRestemplateClient.class);

    @Autowired
    OAuth2RestTemplate restTemplate;
    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;

    private ObjectMapper mapper = new ObjectMapper();

    @Cacheable
    public Location getLocationById(Long id) {
        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/location/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, id).getBody();

        return responseBodyWrapper.getData();

    }

    @Cacheable
    public Location getLocationByName(String name) {

        ResponseBodyWrapper<Location[]> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations?name={name}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {
                }, name).getBody();

        Location[] locations = responseBodyWrapper.getData();
        if (locations.length != 1) {
            logger.debug("getLocationByName / {} return {} locations. Error!!!", name, locations.length);
            return null;
        }
        else {
            return locations[0];
        }
    }

    @Cacheable
    public Location[] getLocationByLocationGroups(String locationGroupIds) {

        ResponseBodyWrapper<Location[]> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations?location_group_ids={locationGroupIds}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {
                }, locationGroupIds).getBody();

        return responseBodyWrapper.getData();
    }

    @Cacheable
    public Location[] getLocationByLocationGroupTypes(String locationGroupTypeIds) {

        ResponseBodyWrapper<Location[]> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations?location_group_type_ids={locationGroupTypeIds}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {
                }, locationGroupTypeIds).getBody();

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

        ResponseBodyWrapper<LocationGroup> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locationgroup/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroup>>() {
                }, id).getBody();

        return responseBodyWrapper.getData();
    }
    public LocationGroup getLocationGroupByName(String name) {

        ResponseBodyWrapper<LocationGroup[]> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locationgroups?name={name}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroup[]>>() {
                }, name).getBody();

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

        ResponseBodyWrapper<LocationGroupType> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locationgrouptype/{id}",
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



    public Location allocateLocation(Location location, Double inventorySize) {

        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout//location/{id}/allocate?inventory_size={inventorySize}",
                HttpMethod.PUT, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, location.getId(), inventorySize).getBody();

        return responseBodyWrapper.getData();
    }

}
