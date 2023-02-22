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

package com.garyzhangscm.cwms.layout.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.layout.ResponseBodyWrapper;
import com.garyzhangscm.cwms.layout.model.Client;
import com.garyzhangscm.cwms.layout.model.Policy;
import com.garyzhangscm.cwms.layout.model.Warehouse;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;


@Component
public class CommonServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(CommonServiceRestemplateClient.class);

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;

    @Cacheable(cacheNames = "LayoutService_Client", unless="#result == null")
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

    @Cacheable(cacheNames = "LayoutService_Client", unless="#result == null")
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

    @Cacheable(cacheNames = "LayoutService_Policy", unless="#result == null")
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

            logger.debug("Find result {} for policy {}",
                    policies.get(0).getValue(), key);
            return policies.get(0);
        }
        else {
            logger.debug("Find None for policy {}", key);
            return null;
        }

    }

    public Policy createPolicy(Policy policy) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/policies");

        ResponseBodyWrapper<Policy> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(policy)),
                new ParameterizedTypeReference<ResponseBodyWrapper<Policy>>() {}).getBody();

        return responseBodyWrapper.getData();
    }


    private HttpEntity<String> getHttpEntity(String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        return new HttpEntity<String>(requestBody, headers);
    }


    public String removePolicy(Warehouse warehouse, String key) {
        UriComponentsBuilder builder =
            UriComponentsBuilder.newInstance()
                    .scheme("http").host("zuulserver").port(5555)
                    .path("/api/common/policies")
                .queryParam("warehouseId", warehouse.getId());

        if (Strings.isNotBlank(key)) {
            builder = builder.queryParam("key", key);
        }

        ResponseBodyWrapper<String> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
}
