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

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.model.Company;
import com.garyzhangscm.cwms.resources.model.Location;
import com.garyzhangscm.cwms.resources.model.Warehouse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

@Component
public class LayoutServiceRestemplateClient implements  InitiableServiceRestemplateClient{

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

    public Company getCompanyByCode(String companyCode) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/companies")
                        .queryParam("code", companyCode);


        ResponseBodyWrapper<List<Company>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Company>>>() {
                }).getBody();

        List<Company> companies = responseBodyWrapper.getData();
        if (companies.size() != 1) {
            return null;
        }
        else {
            return companies.get(0);
        }
    }
    public List<Company> getCompanies() {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/companies");

        ResponseBodyWrapper<List<Company>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Company>>>() {}).getBody();
        return responseBodyWrapper.getData();
    }

    public Warehouse getWarehouseByName(String companyCode, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/warehouses")
                        .queryParam("companyCode", companyCode)
                        .queryParam("name", name);


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

    public String initTestData(Long companyId, String warehouseName) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/test-data/init")
                        .queryParam("companyId", companyId)
                        .queryParam("warehouseName", warehouseName);

        ResponseEntity<String> restExchange
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.POST,
                        null,
                        String.class);
        return restExchange.getBody();
    }

    public String initTestData(Long companyId, String name, String warehouseName) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/test-data/init/{name}")
                        .queryParam("companyId", companyId)
                        .queryParam("warehouseName", warehouseName);

        ResponseEntity<String> restExchange
                = restTemplate.exchange(
                        builder.buildAndExpand(name).toUriString(),
                        HttpMethod.POST,
                        null,
                        String.class);
        return restExchange.getBody();
    }

    public String[] getTestDataNames() {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/test-data");

        ResponseBodyWrapper<String[]> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<String[]>>() {}).getBody();

        return responseBodyWrapper.getData();

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

        ResponseEntity<String> restExchange
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                String.class);
        return restExchange.getBody();
    }

    public Location createRFLocation(Long warehouseId, String rfCode) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/layout/locations/rf")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("rfCode", rfCode);

        ResponseEntity<Location> restExchange
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                Location.class);
        return restExchange.getBody();
    }
}
