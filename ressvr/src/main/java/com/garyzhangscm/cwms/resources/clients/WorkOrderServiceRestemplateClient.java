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
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class WorkOrderServiceRestemplateClient implements  InitiableServiceRestemplateClient{
    @Autowired
    OAuth2RestTemplate restTemplate;

    public String initTestData(String warehouseName) {
        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/workorder/test-data/init?")
                .append("warehouseName={warehouseName}");
        ResponseEntity<String> restExchange = restTemplate.exchange(
                url.toString(),
                HttpMethod.POST, null, String.class,
                warehouseName);
        return restExchange.getBody();
    }

    public String initTestData(String name, String warehouseName) {
        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/workorder/test-data/init/{name}?")
                .append("warehouseName={warehouseName}");
        ResponseEntity<String> restExchange = restTemplate.exchange(
                url.toString(),
                HttpMethod.POST, null, String.class,
                name, warehouseName);
        return restExchange.getBody();
    }

    public String[] getTestDataNames() {
        ResponseBodyWrapper<String[]> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/workorder/test-data",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<String[]>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public boolean contains(String name) {
        return Arrays.stream(getTestDataNames()).anyMatch(dataName -> dataName.equals(name));
    }

}
