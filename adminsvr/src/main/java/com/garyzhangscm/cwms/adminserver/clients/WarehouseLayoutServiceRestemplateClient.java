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

package com.garyzhangscm.cwms.adminserver.clients;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.model.WarehouseConfiguration;
import com.garyzhangscm.cwms.adminserver.model.wms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class WarehouseLayoutServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseLayoutServiceRestemplateClient.class);



    @Autowired
    private RestTemplateProxy restTemplateProxy;

    @Cacheable(cacheNames = "AdminService_WarehouseConfiguration", unless="#result == null")
    public WarehouseConfiguration getWarehouseConfiguration(Long warehouseId)   {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouse-configuration/by-warehouse/{id}");
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


    @Cacheable(cacheNames = "AdminService_Location", unless="#result == null")
    public Location getLocationById(Long id) {
        UriComponentsBuilder builder =
            UriComponentsBuilder.newInstance()
                    .scheme("http").host("apigateway").port(5555)
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

    @Cacheable(cacheNames = "AdminService_Location", unless="#result == null")
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
        List<Location> locations =  restTemplateProxy.exchangeList(
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

    public LocationGroup createLocationGroup(LocationGroup locationGroup) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locationgroups");
/**
        ResponseBodyWrapper<LocationGroup> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(locationGroup)),
                new ParameterizedTypeReference<ResponseBodyWrapper<LocationGroup>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                LocationGroup.class,
                builder.toUriString(),
                HttpMethod.POST,
                locationGroup
        );


    }
    public Location createLocation(Location location)   {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations")
                .queryParam("warehouseId", location.getWarehouse().getId());
/**
        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(location)),
                new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                Location.class,
                builder.toUriString(),
                HttpMethod.POST,
                location
        );
    }


    @Cacheable(cacheNames = "AdminService_Warehouse", unless="#result == null")
    public Warehouse getWarehouseById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouses/{id}");
/**
        ResponseBodyWrapper<Warehouse> responseBodyWrapper
                = restTemplate.exchange(
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
    public List<Warehouse> getWarehouseByCompany(Long companyId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouses")
                        .queryParam("companyId", companyId);

        logger.debug("Start to get warehouse by companyId: {}, /n >> {}",
                companyId, builder.toUriString());
/**
        ResponseBodyWrapper<List<Warehouse>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Warehouse>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                Warehouse.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    @Cacheable(cacheNames = "AdminService_Company", unless="#result == null")
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
/**
        ResponseBodyWrapper<List<Company>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Company>>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
    logger.debug("start to get all company from url \n{}",
            builder.toUriString());

        return  restTemplateProxy.exchangeList(
                Company.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }


    public Warehouse getWarehouseByName(String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouses")
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

    public List<LocationGroup> getLocationGroupByWarehouseId(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locationgroups")
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
    @Cacheable(cacheNames = "AdminService_LocationGroup", unless="#result == null")
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

    public Company createCompany(Company company) throws JsonProcessingException {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/companies");
/**
        ResponseBodyWrapper<Company> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(company)),
                new ParameterizedTypeReference<ResponseBodyWrapper<Company>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Company.class,
                builder.toUriString(),
                HttpMethod.POST,
                company
        );

    }

    public Warehouse createWarehouse(Long companyId, Warehouse warehouse) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouses")
                        .queryParam("companyId", companyId.toString());
/**
        ResponseBodyWrapper<Warehouse> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(warehouse)),
                new ParameterizedTypeReference<ResponseBodyWrapper<Warehouse>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Warehouse.class,
                builder.toUriString(),
                HttpMethod.POST,
                warehouse
        );


    }



    public List<LocationGroupType> getStorageLocationTypes() {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locationgrouptypes/storage-locations");
/**
        ResponseBodyWrapper<List<LocationGroupType>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<LocationGroupType>>>() {}).getBody();

        return responseBodyWrapper.getData();
**/

        return restTemplateProxy.exchangeList(
                LocationGroupType.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );


    }

    public String getNextCompanyCode() {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/companies/code/next");
/**
        ResponseBodyWrapper<String> responseBodyWrapper
                = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    @Cacheable(cacheNames = "AdminService_Company", unless="#result == null")
    public Company getCompanyByCode(String companyCode) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl("http://apigateway:5555/api/layout/companies")
                        .queryParam("code", companyCode);
/**
        ResponseBodyWrapper<List<Company>> responseBodyWrapper = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Company>>>() {
                }).getBody();

        List<Company> companies = responseBodyWrapper.getData();
**/
        List<Company> companies =  restTemplateProxy.exchangeList(
                Company.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
        if (companies.size() != 1) {
            return null;
        }
        else {
            return companies.get(0);
        }
    }
    public List<Location> getLocationsByWarehouseId(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/locations")
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
}
