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

import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.http.HttpMethod;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;


import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Component
public class WarehouseLayoutServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseLayoutServiceRestemplateClient.class);

    @Autowired
    CommonServiceRestemplateClient commonServiceRestemplateClient;

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    @Cacheable(cacheNames = "OutboundService_Company", unless="#result == null")
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

    @Cacheable(cacheNames = "OutboundService_Location", unless="#result == null")
    public Location getLocationById(Long id) {
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

    public List<Location> getLocationByIds(Long warehouseId, String ids) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("ids", ids);

        return restTemplateProxy.exchangeList(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    /**
     * Get the location that represent the container. A typical container is
     * 1. Pick List
     * 2. Carton
     * If the location doesn't exist yet, we will create the location on the fly
     * @param warehouseId Warehouse Id
     * @param containerName Container Name
     * @return
     */
    public Location getLocationByContainerId(Long warehouseId, String containerName) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/container/{containerName}")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(containerName).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(containerName).toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public Location getDefaultPackingStation(Long warehouseId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/packing-stations")
                        .queryParam("warehouseId", warehouseId);

/**
        ResponseBodyWrapper<List<Location>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {}).getBody();

        List<Location> locations = responseBodyWrapper.getData();
 **/

        List<Location> locations = restTemplateProxy.exchangeList(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
        if (locations.size() == 0) {
            return null;
        }
        else {
            return locations.get(0);
        }
    }

    @Cacheable(cacheNames = "OutboundService_Location", unless="#result == null")
    public Location getLocationByName(String companyCode, String warehouseName, String name) {
        Warehouse warehouse = getWarehouseByName(companyCode, warehouseName);
        if (warehouse == null) {
            throw ResourceNotFoundException.raiseException("warehouse name (" + warehouseName +  ")is not valid");
        }
        return getLocationByName(warehouse.getId(), name);
    }

    @Cacheable(cacheNames = "OutboundService_Location", unless="#result == null")
    public Location getLocationByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("name", name)
                        .queryParam("warehouseId", warehouseId);

/**
        ResponseBodyWrapper<Location[]> responseBodyWrapper
                = restTemplate.exchange(
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

    public List<Location> getLocationByLocationGroupId(Long warehouseId, Long locationGroupId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("locationGroupIds", String.valueOf(locationGroupId))
                        .queryParam("warehouseId", warehouseId);

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

    @Cacheable(cacheNames = "OutboundService_Warehouse", unless="#result == null")
    public Warehouse getWarehouseById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouses/{id}");
/**
        ResponseBodyWrapper<Warehouse> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Warehouse>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Warehouse.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );
    }

    @Cacheable(cacheNames = "OutboundService_Warehouse", unless="#result == null")
    public Warehouse getWarehouseByName(String companyCode, String name) {
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

        List<Warehouse> warehouses =  restTemplateProxy.exchangeList(
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


    @Cacheable(cacheNames = "OutboundService_WarehouseConfiguration", unless="#result == null")
    public WarehouseConfiguration getWarehouseConfiguration(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouse-configuration/by-warehouse/{warehouseId}");
/**
        ResponseBodyWrapper<WarehouseConfiguration> responseBodyWrapper
                = restTemplate.exchange(
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


    @Cacheable(cacheNames = "OutboundService_LocationGroup", unless="#result == null")
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

    @Cacheable(cacheNames = "OutboundService_LocationGroup", unless="#result == null")
    public LocationGroup getLocationGroupByName(String companyCode,
                                                String warehouseName, String name) {
        Warehouse warehouse = getWarehouseByName(companyCode, warehouseName);
        if (warehouse == null) {
            throw ResourceNotFoundException.raiseException("warehouse name (" + warehouseName +  ")is not valid");
        }
        return getLocationGroupByName(warehouse.getId(), name);
    }
    @Cacheable(cacheNames = "OutboundService_LocationGroup", unless="#result == null")
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
    @Cacheable(cacheNames = "OutboundService_LocationGroupType", unless="#result == null")
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

    @Cacheable(cacheNames = "OutboundService_LocationGroupType", unless="#result == null")
    public LocationGroupType getLocationGroupTypeByName(String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locationgrouptypes")
                        .queryParam("name", name);
/**
        ResponseBodyWrapper<LocationGroupType[]> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroupType[]>>() {}).getBody();

        LocationGroupType[] locationGroupTypes = responseBodyWrapper.getData();
**/
        List<LocationGroupType> locationGroupTypes =  restTemplateProxy.exchangeList(
                LocationGroupType.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
        if (locationGroupTypes.size() != 1) {
            logger.debug("getLocationGroupTypeByName / {} return {} location group types. Error!!!", name, locationGroupTypes.size());
            return null;
        }
        else {

            return locationGroupTypes.get(0);
        }
    }



    public Location reserveLocationFromGroup(Long locationGroupId, String reservedCode,
                                    Double pendingSize, Long pendingQuantity, Integer pendingPalletQuantity) {
        logger.debug("Start to reserve location from group {} \n reserve code: {}, " +
                "pending size: {} \n pending quantity: {} \n pending pallet quantity: {}",
                locationGroupId, reservedCode, pendingSize, pendingQuantity, pendingPalletQuantity);
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

        logger.debug("Get result from reserve location by location group \n{}",
                responseBodyWrapper);
        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(locationGroupId).toUriString(),
                HttpMethod.PUT,
                null
        );
    }

    public Location reserveLocation(Long locationId, String reservedCode,
                                             Double pendingSize, Long pendingQuantity, Integer pendingPalletQuantity) {
        logger.debug("Start to reserve location {} with code {}, size {}, quantity {}, pending pallet quantity {}",
                locationId, reservedCode, pendingSize, pendingQuantity, pendingPalletQuantity);
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/{locationId}/reserveWithVolume")
                        .queryParam("reservedCode", reservedCode)
                        .queryParam("pendingSize", pendingSize)
                        .queryParam("pendingQuantity", pendingQuantity)
                        .queryParam("pendingPalletQuantity", pendingPalletQuantity);


        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(locationId).toUriString(),
                HttpMethod.PUT,
                null
        );

    }


    public List<Location> findEmptyDockLocations(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/dock")
                        .queryParam("empty", true)
                        .queryParam("warehouseId", warehouseId);
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

    public Location checkInTrailerAtDockLocations(Long dockLocationId, Long trailerId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/dock/{id}/check-in-trailer")
                        .queryParam("trailerId", trailerId);
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(dockLocationId).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(dockLocationId).toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public Location dispatchTrailerFromDockLocations(Long dockLocationId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/dock/{id}/dispatch-trailer");

/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(dockLocationId).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(dockLocationId).toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public Location getTrailerLocation(Long warehouseId, Long trailerId) {
        String locationName = "TRLR-" + trailerId;
        return getLocationByName(warehouseId, locationName);
    }

    public Location findEmptyDestinationLocationForEmergencyReplenishment(Long warehouseId,
                                                                          LocationGroup locationGroup,
                                                                          Double replenishmentSize) {
        logger.debug("Start to find an empty location in group {}, with at least empty capacity of {}, for emergency replenishment"
                        , locationGroup.getName(), replenishmentSize);
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("locationGroupIds", locationGroup.getId())
                        .queryParam("emptyLocationOnly", true)
                        .queryParam("minEmptyCapacity", replenishmentSize)
                        .queryParam("pickableLocationOnly", true)
                        .queryParam("maxResultCount", 1);
/**
        ResponseBodyWrapper<List<Location>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {}).getBody();

        List<Location> locations = responseBodyWrapper.getData();
 **/
        List<Location> locations = restTemplateProxy.exchangeList(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

        if (locations.size() == 0) {
            throw ResourceNotFoundException.raiseException(
                    "Can't find suitable empty location from group: " + locationGroup.getName()
            );
        }
        return locations.get(0);
    }


    public Location getShippedParcelLocation(Long warehouseId,
                                             String carrierName,
                                             String serviceLevelName) {

        logger.debug("getShippedParcelLocation: {} / {} / {}",
                warehouseId, carrierName, serviceLevelName);
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/parcel-locations/{carrierName}/{serviceLevelName}")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(carrierName, serviceLevelName).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(carrierName, serviceLevelName).toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public Location createOrderLocation(Long warehouseId, Order order) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/order-locations/{orderNumber}")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(order.getNumber()).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(order.getNumber()).toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public Location createTrailerAppointmentLocation(Long warehouseId, String trailerAppointmentNumber) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/shipped-inventory/trailer-appointment/{trailerAppointmentNumber}")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(trailerAppointmentNumber).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(trailerAppointmentNumber).toUriString(),
                HttpMethod.POST,
                null
        );
    }
    public List<Location> releaseLocations(Long warehouseId, Shipment shipment) {
        return unreserveLocation(warehouseId, null, shipment.getNumber(),null);

    }

    public List<Location> releaseLocations(Long warehouseId, Order order) {
        return unreserveLocation(warehouseId, null, order.getNumber(),null);

    }

    public List<Location> releaseLocations(Long warehouseId, Wave wave) {
        return unreserveLocation(warehouseId, null, wave.getNumber(),null);

    }


    public List<Location> unreserveLocation(Long warehouseId, Long locationId) {
        return unreserveLocation(warehouseId, locationId, "",null);

    }
    public List<Location> unreserveLocation(Long warehouseId, Long locationId, String reservedCode, Boolean clearReservedVolume) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/unreserve")
                        .queryParam("warehouseId", warehouseId);
        if (Objects.nonNull(locationId)) {
            builder = builder.queryParam("locationId", locationId);
        }
        if (Strings.isNotBlank(reservedCode)) {
            builder = builder.queryParam("reservedCode", reservedCode);
        }
        if (Objects.nonNull(clearReservedVolume)) {
            builder = builder.queryParam("clearReservedVolume", clearReservedVolume);
        }
/**
        ResponseBodyWrapper<List<Location>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                Location.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );

    }


    public LocalDate getNextWorkingDay(Long warehouseId, boolean includingToday) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouse-configuration/next-working-day")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("includingToday", includingToday);
/**
        ResponseBodyWrapper<LocalDate> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<LocalDate>>() {}).getBody();

        return responseBodyWrapper.getData();
**/

        return restTemplateProxy.exchange(
                LocalDate.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public Location errorLocation(Long warehouseId, Long locationId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations/{id}/error")
                        .queryParam("error", true)
                .queryParam("warehouseId", warehouseId);
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
}
