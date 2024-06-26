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
import com.garyzhangscm.cwms.resources.model.SystemControlledNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Collections;

@Component
public class CommonServiceRestemplateClient implements  InitiableServiceRestemplateClient{

    private static final Logger logger = LoggerFactory.getLogger(CommonServiceRestemplateClient.class);

    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;

    public String initTestData(Long companyId, String warehouseName) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/test-data/init")
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
                        .path("/api/common/test-data/init/{name}")
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
                        .path("/api/common/test-data");

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
                        .path("/api/common/test-data/clear")
                        .queryParam("warehouseId", warehouseId);

        ResponseEntity<String> restExchange
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                String.class);
        return restExchange.getBody();
    }

    public String getNextNumber(Long warehouseId, String variable) {

        logger.debug("Start to get next number for {} / {}",
                warehouseId, variable);
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

        logger.debug(">> Next number is: {}", responseBodyWrapper.getData().getNextNumber());
        return responseBodyWrapper.getData().getNextNumber();
    }

}
