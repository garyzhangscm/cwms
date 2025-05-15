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
import com.garyzhangscm.cwms.adminserver.model.User;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;


@Component
public class ResourceServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(ResourceServiceRestemplateClient.class);

    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Autowired
    private RestTemplateProxy restTemplateProxy;


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
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/test-data/init/" + dataName)
                        .queryParam("warehouseName", warehouseName);
/**
        ResponseBodyWrapper<String> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );
    }

    private List<String> getDataNames() {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/test-data");
/**
        ResponseBodyWrapper<List<String>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<String>>>() {}).getBody();

        return responseBodyWrapper.getData();
   **/
        return restTemplateProxy.exchangeList(
                String.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public String clearData(String warehouseName) {
        Long warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseByName(warehouseName).getId();
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/test-data/clear")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<String> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );



    }

    public Boolean validateSystemAdminUser(String username) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/users/is-system-admin")
                        .queryParam("username", username);
/**
        ResponseBodyWrapper<Boolean> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Boolean>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                Boolean.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public User createUser(User user) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/users");
/**
        ResponseBodyWrapper<User> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.PUT,
                getHttpEntity(objectMapper.writeValueAsString(user)),
                new ParameterizedTypeReference<ResponseBodyWrapper<User>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                User.class,
                builder.toUriString(),
                HttpMethod.PUT,
                user
        );

    }

    @Cacheable(cacheNames = "AdminService_UserByToken", unless="#result == null")
    public User getUserByUsernameAndToken(String username, String token) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/users-by-token")
                        .queryParam("username", username)
                        .queryParam("token", token);
/**
        ResponseBodyWrapper<User> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<User>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                User.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
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

}
