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
import com.garyzhangscm.cwms.adminserver.model.wms.IntegrationData;
import com.garyzhangscm.cwms.adminserver.model.wms.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


@Component
public class IntegrationServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationServiceRestemplateClient.class);


    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Autowired
    // private OAuth2RestOperations restTemplate;
    RestTemplate restTemplate;

    private <T> IntegrationData sendData(String subUrl, T data) throws JsonProcessingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/integration/integration-data/" + subUrl);

        ResponseBodyWrapper<IntegrationData> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(data)),
                new ParameterizedTypeReference<ResponseBodyWrapper<IntegrationData>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    private <T> IntegrationData getData(String subUrl, Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/integration/integration-data/" + subUrl + "/" + id);


        ResponseBodyWrapper<IntegrationData> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<IntegrationData>>() {}).getBody();

        logger.debug("get IntegrationData by id {}\n{}", id, responseBodyWrapper.getData());

        return responseBodyWrapper.getData();

    }

    public IntegrationData sendItemData(Item item) throws JsonProcessingException {

        return sendData("items", item);

    }

    public IntegrationData getItemData(Long id)  {

        return getData("items", id);

    }

    public String clearData(String warehouseName) {
        Long warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseByName(warehouseName).getId();
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
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


    private HttpEntity<String> getHttpEntity(String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        return new HttpEntity<String>(requestBody, headers);
    }

}
