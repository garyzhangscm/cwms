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


import com.garyzhangscm.cwms.workorder.ResponseBodyWrapper;
import com.garyzhangscm.cwms.workorder.exception.GenericException;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.Company;
import com.garyzhangscm.cwms.workorder.model.Location;

import com.garyzhangscm.cwms.workorder.model.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;


import java.util.List;

@Component
@CacheConfig(cacheNames = "warehouseLayout")
public class WarehouseLayoutServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseLayoutServiceRestemplateClient.class);

    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;

    public Company getCompanyById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/companies/{id}");

        ResponseBodyWrapper<Company> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Company>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public Location getLocationById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/{id}");

        ResponseBodyWrapper<Location> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Location>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public Location getLocationByName(String companyCode, String warehouseName, String name) {
        Warehouse warehouse = getWarehouseByName(companyCode, warehouseName);
        if (warehouse == null) {
            throw ResourceNotFoundException.raiseException("warehouse name (" + warehouseName +  ")is not valid");
        }
        return getLocationByName(warehouse.getId(), name);
    }

    public Location getLocationByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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
        // logger.debug(">> Get {} locations by name: {}", locations.length, name);
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
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/warehouses/{id}");

        ResponseBodyWrapper<Warehouse> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Warehouse>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public Warehouse getWarehouseByName(String companyCode, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/warehouses")
                        .queryParam("name", name)
                        .queryParam("companyCode", companyCode);

        // logger.debug("Start to get warehouse by name: {}, /n >> {}",
        //         name, builder.toUriString());

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



}
