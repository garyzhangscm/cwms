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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.adminserver.ResponseBodyWrapper;
import com.garyzhangscm.cwms.adminserver.model.User;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;


@Component
public class ResourceServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(ResourceServiceRestemplateClient.class);

    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;



    @Autowired
    // OAuth2RestTemplate restTemplate;
    // private OAuth2RestOperations restTemplate;
    RestTemplate restTemplate;

    @Autowired
    @Qualifier("getObjMapper")
    private ObjectMapper objectMapper;


    public void initData(String warehouseName) {

        // Initial testing data
        // Note: init all(call /api/resource/test-data/init with
        // only warehouse name) has some issue so far so we will
        // init data one by one
        List<String> dataNames = getDataNames();
        dataNames.forEach(dataName -> initData(dataName, warehouseName));
    }

    public String initData(String dataName, String warehouseName) {
        logger.debug("Start to init data for {}", dataName);
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/test-data/init/" + dataName)
                        .queryParam("warehouseName", warehouseName);

        ResponseBodyWrapper<String> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    private List<String> getDataNames() {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/test-data");

        ResponseBodyWrapper<List<String>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<String>>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public String clearData(String warehouseName) {
        Long warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseByName(warehouseName).getId();
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/test-data/clear")
                        .queryParam("warehouseId", warehouseId);

        ResponseBodyWrapper<String> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public Boolean validateSystemAdminUser(String username) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/users/is-system-admin")
                        .queryParam("username", username);

        ResponseBodyWrapper<Boolean> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Boolean>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public User createUser(User user) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/users");

        ResponseBodyWrapper<User> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.PUT,
                getHttpEntity(objectMapper.writeValueAsString(user)),
                new ParameterizedTypeReference<ResponseBodyWrapper<User>>() {}).getBody();

        return responseBodyWrapper.getData();


    }

    @Cacheable(cacheNames = "admin_company", unless="#result == null")
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

    public User getUserByUsername(Long companyId, String username) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/users")
                        .queryParam("username", username)
                        .queryParam("companyId", companyId);

        ResponseBodyWrapper<List<User>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<User>>>() {}).getBody();

        List<User> users = responseBodyWrapper.getData();

        if (users.size() != 1) {
            return null;
        }
        else {
            return users.get(0);
        }

    }

    public User getUserByUsernameAndToken(String username, String token) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/users-by-token")
                        .queryParam("username", username)
                        .queryParam("token", token);

        ResponseBodyWrapper<User> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<User>>() {}).getBody();

        return responseBodyWrapper.getData();


    }

    @Cacheable(cacheNames = "admin_warehouse", unless="#result == null")
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



    private HttpEntity<String> getHttpEntity(String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        return new HttpEntity<String>(requestBody, headers);
    }


}
