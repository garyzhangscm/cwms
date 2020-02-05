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

package com.garyzhangscm.cwms.workorder.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.ResponseBodyWrapper;
import com.garyzhangscm.cwms.workorder.exception.GenericException;
import com.garyzhangscm.cwms.workorder.model.Location;
import com.garyzhangscm.cwms.workorder.model.LocationGroup;
import com.garyzhangscm.cwms.workorder.model.LocationGroupType;
import com.garyzhangscm.cwms.workorder.model.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;

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


    public Location getLocationById(Long id) {
        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, id).getBody();

        return responseBodyWrapper.getData();

    }

    public Location getLocationByName(String warehouseName, String name) {
        Warehouse warehouse = getWarehouseByName(warehouseName);
        if (warehouse == null) {
            throw new GenericException(10000, "warehouse name is not valid");
        }
        return getLocationByName(warehouse.getId(), name);
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

    public Warehouse getWarehouseById(Long id) {
        ResponseBodyWrapper<Warehouse> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/warehouses/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Warehouse>>() {
                }, id).getBody();

        return responseBodyWrapper.getData();

    }

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
    public LocationGroup getLocationGroupByName(String warehouseName, String name) {
        Warehouse warehouse = getWarehouseByName(warehouseName);
        if (warehouse == null) {
            throw new GenericException(10000, "Can't find the warehouse name");
        }
        return getLocationGroupByName(warehouse.getId(), name);
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



    public Location allocateLocation(Location location, Double inventorySize) {

        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/locations/{id}/allocate?inventory_size={inventorySize}",
                HttpMethod.PUT, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, location.getId(), inventorySize).getBody();

        return responseBodyWrapper.getData();
    }

    public Location reserveLocationFromGroup(Long locationGroupId, String reservedCode,
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

    public Location reserveLocation(Long locationId, String reservedCode,
                                             Double pendingSize, Long pendingQuantity, Integer pendingPalletQuantity) {

        StringBuilder url = new StringBuilder();
        url.append("http://zuulserver:5555/api/layout/locations/{locationId}/reserveWithVolume?")
                .append("reserved_code={reservedCode}")
                .append("&pending_size={pendingSize}")
                .append("&pending_quantity={pendingQuantity}")
                .append("&pending_pallet_quantity={pendingPalletQuantity}");

        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange(url.toString(),
                HttpMethod.PUT, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, locationId, reservedCode, pendingSize, pendingQuantity, pendingPalletQuantity).getBody();

        return responseBodyWrapper.getData();
    }


    public List<Location> findDockLocations() {

        StringBuilder url = new StringBuilder();
        url.append("http://zuulserver:5555/api/layout/locations/dock");

        ResponseBodyWrapper<List<Location>> responseBodyWrapper = restTemplate.exchange(url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {
                }).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Location> findEmptyDockLocations() {

        StringBuilder url = new StringBuilder();
        url.append("http://zuulserver:5555/api/layout/locations/dock?empty=true");

        ResponseBodyWrapper<List<Location>> responseBodyWrapper = restTemplate.exchange(url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {
                }).getBody();

        return responseBodyWrapper.getData();
    }

    public Location checkInTrailerAtDockLocations(Long dockLocationId, Long trailerId) {

        StringBuilder url = new StringBuilder();
        url.append("http://zuulserver:5555/api/layout/locations/dock/{id}/check-in-trailer?")
           .append("trailerId={trailerId}");

        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange(url.toString(),
                HttpMethod.POST, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, dockLocationId, trailerId).getBody();

        return responseBodyWrapper.getData();
    }

    public Location dispatchTrailerFromDockLocations(Long dockLocationId) {

        StringBuilder url = new StringBuilder();
        url.append("http://zuulserver:5555/api/layout/locations/dock/{id}/dispatch-trailer");

        ResponseBodyWrapper<Location> responseBodyWrapper = restTemplate.exchange(url.toString(),
                HttpMethod.POST, null, new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {
                }, dockLocationId).getBody();

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

        StringBuilder url = new StringBuilder();
        url.append("http://zuulserver:5555/api/layout/locations?")
                .append("location_group_ids={locationGroupId}")
                .append("empty_location_only=true")
                .append("min_empty_capacity={replenishmentSize}")
                .append("pickable_location_only=true")
                .append("max_result_count=1");

        ResponseBodyWrapper<List<Location>> responseBodyWrapper = restTemplate.exchange(url.toString(),
                HttpMethod.POST, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {
                }, locationGroup.getId(), replenishmentSize).getBody();

        List<Location> locations = responseBodyWrapper.getData();
        if (locations.size() == 0) {
            throw new GenericException(10000, "Can't find suitable empty location from group: " + locationGroup.getName());
        }
        return locations.get(0);
    }


}
