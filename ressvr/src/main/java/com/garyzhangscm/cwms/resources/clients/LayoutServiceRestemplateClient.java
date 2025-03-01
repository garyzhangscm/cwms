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

package com.garyzhangscm.cwms.resources.clients;

import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

@Component
public class LayoutServiceRestemplateClient implements  InitiableServiceRestemplateClient{

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    @Cacheable(cacheNames = "ResourceService_Company", unless="#result == null")
    public Company getCompanyById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/companies/{id}");

        return restTemplateProxy.exchange(
                Company.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    @Cacheable(cacheNames = "ResourceService_Company", unless="#result == null")
    public Company getCompanyByCode(String companyCode) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/companies")
                        .queryParam("code", companyCode);



        List<Company> companies
                = restTemplateProxy.exchangeList(
                Company.class,
                builder.toUriString(),
                HttpMethod.GET,
                null);


        if (companies.size() != 1) {
            return null;
        }
        else {
            return companies.get(0);
        }
    }


    @Cacheable(cacheNames = "ResourceService_Warehouse", unless="#result == null")
    public Warehouse getWarehouseByName(String companyCode, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/warehouses")
                        .queryParam("companyCode", companyCode)
                        .queryParam("name", name);



        List<Warehouse> warehouses
                = restTemplateProxy.exchangeList(
                Warehouse.class,
                builder.toUriString(),
                HttpMethod.GET,
                null);


        if (warehouses.size() != 1) {
            return null;
        }
        else {
            return warehouses.get(0);
        }
    }


    @Cacheable(cacheNames = "ResourceService_Warehouse", unless="#result == null")
    public Warehouse getWarehouseByName(Long companyId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/warehouses")
                        .queryParam("companyId", companyId)
                        .queryParam("name", name);


        List<Warehouse> warehouses
                = restTemplateProxy.exchangeList(
                Warehouse.class,
                builder.toUriString(),
                HttpMethod.GET,
                null);

        if (warehouses.size() != 1) {
            return null;
        }
        else {
            return warehouses.get(0);
        }
    }

    @Cacheable(cacheNames = "ResourceService_Warehouse", unless="#result == null")
    public Warehouse getWarehouseById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/warehouses/{id}");

        return restTemplateProxy.exchange(
                Warehouse.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public String initTestData(Long companyId, String warehouseName) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/test-data/init")
                        .queryParam("companyId", companyId)
                        .queryParam("warehouseName", warehouseName);


        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public String initTestData(Long companyId, String name, String warehouseName) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/test-data/init/{name}")
                        .queryParam("companyId", companyId)
                        .queryParam("warehouseName", warehouseName);


        return restTemplateProxy.exchange(
                String.class,
                builder.buildAndExpand(name).toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public String[] getTestDataNames() {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/test-data");


        return restTemplateProxy.exchange(
                String[].class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    @Cacheable(cacheNames = "ResourceService_Location", unless="#result == null")
    public Location getLocationByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations")
                        .queryParam("name", name)
                        .queryParam("warehouseId", warehouseId);

        Location[] locations =  restTemplateProxy.exchange(
                Location[].class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );


        if (locations.length != 1) {
            return null;
        }
        else {
            return locations[0];
        }
    }
    @Cacheable(cacheNames = "ResourceService_LocationGroup", unless="#result == null")
    public LocationGroup getLocationGroupByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locationgroups")
                        .queryParam("name", name)
                        .queryParam("warehouseId", warehouseId);


        LocationGroup[] locationGroups =  restTemplateProxy.exchange(
                LocationGroup[].class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );


        if (locationGroups.length != 1) {
            return null;
        }
        else {

            return locationGroups[0];
        }
    }

    @Cacheable(cacheNames = "ResourceService_LocationGroupType", unless="#result == null")
    public LocationGroupType getLocationGroupTypeByName(String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locationgrouptypes")
                        .queryParam("name", name);

        LocationGroupType[] locationGroupTypes =  restTemplateProxy.exchange(
                LocationGroupType[].class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );


        if (locationGroupTypes.length != 1) {
            return null;
        }
        else {

            return locationGroupTypes[0];
        }
    }



    @Cacheable(cacheNames = "ResourceService_Location", unless="#result == null")
    public Location getLocationById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/{id}");


        return restTemplateProxy.exchange(
                Location.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public boolean contains(String name) {
        return Arrays.stream(getTestDataNames()).anyMatch(dataName -> dataName.equals(name));
    }

    public String clearData(Long warehouseId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/test-data/clear")
                        .queryParam("warehouseId", warehouseId);


        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public Location createRFLocation(Long warehouseId, String rfCode) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/rf")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("rfCode", rfCode);


        return restTemplateProxy.exchange(
                Location.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );
    }


    public Location removeRFLocation(Long warehouseId, String rfCode) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/rf")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("rfCode", rfCode);



        return restTemplateProxy.exchange(
                Location.class,
                builder.toUriString(),
                HttpMethod.DELETE,
                null
        );
    }


    @Cacheable(cacheNames = "ResourceService_WarehouseConfiguration", unless="#result == null")
    public WarehouseConfiguration getWarehouseConfiguration(Long warehouseId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/warehouse-configuration/by-warehouse/{warehouseId}") ;



        return restTemplateProxy.exchange(
                WarehouseConfiguration.class,
                builder.buildAndExpand(warehouseId).toUriString(),
                HttpMethod.GET,
                null
        );
    }
}
