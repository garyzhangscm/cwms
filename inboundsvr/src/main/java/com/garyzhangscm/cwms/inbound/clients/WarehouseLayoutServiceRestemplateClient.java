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

import com.garyzhangscm.cwms.inbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;

@Component
public class WarehouseLayoutServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseLayoutServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;


    @Cacheable(cacheNames = "InboundService_Company", unless="#result == null")
    public Company getCompanyById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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

    @Cacheable(cacheNames = "InboundService_Location", unless="#result == null")
    public Location getLocationById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/{id}");
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
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

    @Cacheable(cacheNames = "InboundService_Location", unless="#result == null")
    public Location getLocationByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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

    @Cacheable(cacheNames = "InboundService_Warehouse", unless="#result == null")
    public Warehouse getWarehouseById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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


    @Cacheable(cacheNames = "InboundService_Warehouse", unless="#result == null")
    public Warehouse getWarehouseByName(String companyCode, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/warehouses")
                        .queryParam("companyCode", companyCode)
                        .queryParam("name", name);
/**
        ResponseBodyWrapper<List<Warehouse>> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
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
            logger.debug("getLocationByName / {} return {} locations. Error!!!", name, warehouses.size());
            return null;
        }
        else {
            return warehouses.get(0);
        }
    }

    public List<Location> getLocationByLocationGroups(Long warehouseId, String locationGroupIds) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("locationGroupIds", locationGroupIds)
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<Location[]> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );


    }
    public List<Location> getLocationByLocationGroupTypes(Long warehouseId, String locationGroupTypeIds) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("locationGroupTypeIds", locationGroupTypeIds)
                        .queryParam("warehouseId", warehouseId);

/**
        ResponseBodyWrapper<Location[]> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location[]>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                Location.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }
/**
    public List<Location> getLocationsByRange(Long beginSequence, Long endSequence, String sequenceType, Boolean includeEmptyLocation) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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
**/

    @Cacheable(cacheNames = "InboundService_LocationGroup", unless="#result == null")
    public LocationGroup getLocationGroupById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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
    @Cacheable(cacheNames = "InboundService_LocationGroup", unless="#result == null")
    public LocationGroup getLocationGroupByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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
        List<LocationGroup> locationGroups = restTemplateProxy.exchangeList(
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
    @Cacheable(cacheNames = "InboundService_LocationGroupType", unless="#result == null")
    public LocationGroupType getLocationGroupTypeById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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

    @Cacheable(cacheNames = "InboundService_LocationGroupType", unless="#result == null")
    public LocationGroupType getLocationGroupTypeByName(String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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
        List<LocationGroupType> locationGroupTypes = restTemplateProxy.exchangeList(
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

    public Location createLocationForReceipt(Receipt receipt) {
        Location location = new Location();
        location.setName(receipt.getNumber());
        location.setEnabled(true);
        String receiptLocationGroup =
                commonServiceRestemplateClient.getPolicyByKey(receipt.getWarehouseId(), "LOCATION-GROUP-RECEIPT").getValue();
        Warehouse warehouse = getWarehouseById(receipt.getWarehouseId());
        location.setWarehouse(warehouse);

        location.setLocationGroup(getLocationGroupByName(warehouse.getId(),receiptLocationGroup));

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations")
                .queryParam("warehouseId", receipt.getWarehouseId());

        /**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = null;
        try {
            responseBodyWrapper = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.POST,
                    getHttpEntity(objectMapper.writeValueAsString(location)),
                    new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();
        } catch (JsonProcessingException e) {
            throw ReceiptOperationException.raiseException(
                    "Can't create the location for receipt due to JsonProcessingException: " + e.getMessage());
        }

        return responseBodyWrapper.getData();
         **/
        return restTemplateProxy.exchange(
                Location.class,
                builder.toUriString(),
                HttpMethod.POST,
                location
        );
    }

    public Location createLocationForCustomerReturnOrder(CustomerReturnOrder customerReturnOrder) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/customer-return-order-stage-locations")
                .queryParam("name", customerReturnOrder.getNumber())
                .queryParam("warehouseId", customerReturnOrder.getWarehouseId());
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.PUT,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();
        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                Location.class,
                builder.toUriString(),
                HttpMethod.PUT,
                null
        );
    }

    public Location allocateLocation(Location location, Inventory inventory) {
        // if the location is not volume tracking, do nothing
        if (Objects.isNull(location.getLocationGroup()) ||
                Objects.isNull(location.getLocationGroup().getVolumeTrackingPolicy())
        ) {
            return location;
        }
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


    public List<Location> getReceivingStageLocations(Long warehouseId)   {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/receiving-stage")
                .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<Location>> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(warehouseId).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Location>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                Location.class,
                builder.buildAndExpand(warehouseId).toUriString(),
                HttpMethod.GET,
                null
        );




    }

    @Cacheable(cacheNames = "InboundService_WarehouseConfiguration", unless="#result == null")
    public WarehouseConfiguration getWarehouseConfiguration(Long warehouseId)   {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout//warehouse-configuration/by-warehouse/{id}");
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
}
