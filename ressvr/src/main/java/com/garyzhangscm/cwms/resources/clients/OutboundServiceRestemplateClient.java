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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;

@Component
public class OutboundServiceRestemplateClient implements  InitiableServiceRestemplateClient{

    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;

    public String initTestData(String warehouseName) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/test-data/init")
                        .queryParam("warehouseName", warehouseName);

        ResponseEntity<String> restExchange
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.POST,
                        null,
                        String.class);
        return restExchange.getBody();
    }

    public String initTestData(String name, String warehouseName) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/test-data/init/{name}")
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
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/test-data");

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

}
