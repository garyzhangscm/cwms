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


import com.garyzhangscm.cwms.inbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

@Component
@CacheConfig(cacheNames = "common")
public class CommonServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(CommonServiceRestemplateClient.class);


    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;

    public Client getClientById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/clients/{id}");

        ResponseBodyWrapper<Client> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                    null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Client>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public Client getClientByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/clients")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("name", name);

        ResponseBodyWrapper<List<Client>> responseBodyWrapper
                = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<Client>>>() {}).getBody();

        List<Client> clients = responseBodyWrapper.getData();
        if (clients.size() == 0) {
            return null;
        }
        else {
            return clients.get(0);
        }
    }
    public Supplier getSupplierById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/suppliers/{id}");

        ResponseBodyWrapper<Supplier> responseBodyWrapper
                = restTemplate.exchange(
                    builder.buildAndExpand(id).toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<Supplier>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public Supplier getSupplierByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/suppliers")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("name", name);

        ResponseBodyWrapper<List<Supplier>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Supplier>>>() {}).getBody();

        List<Supplier> suppliers = responseBodyWrapper.getData();
        if (suppliers.size() == 0) {
            return null;
        }
        else {
            return suppliers.get(0);

        }
    }

    @Cacheable(cacheNames = "inbound_policy", unless="#result == null")
    public Policy getPolicyByKey(Long warehouseId, String key) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/policies")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("key", key);

        ResponseBodyWrapper<List<Policy>> responseBodyWrapper
                = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<Policy>>>() {}).getBody();

        List<Policy> policies = responseBodyWrapper.getData();
        if (policies.size() > 0) {
            return policies.get(0);
        }
        else {
            return null;
        }

    }

    public String getNextNumber(Long warehouseId, String variable) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/system-controlled-number/{variable}/next")
                        .queryParam("warehouseId", warehouseId);
        ResponseBodyWrapper<SystemControlledNumber> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(variable).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<SystemControlledNumber>>() {}).getBody();


        return responseBodyWrapper.getData().getNextNumber();
    }

    public List<String> getNextNumberInBatch(Long warehouseId, String variable, int batch) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/system-controlled-number/{variable}/batch/next")
                .queryParam("batch", batch)
                .queryParam("warehouseId", warehouseId);
        ResponseBodyWrapper<List<SystemControlledNumber>> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(variable).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<SystemControlledNumber>>>() {}).getBody();


        return responseBodyWrapper.getData().stream().map(systemControlledNumber -> systemControlledNumber.getNextNumber()).collect(Collectors.toList());
    }

}
