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
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Component
public class LayoutServiceRestemplateClient implements  InitiableServiceRestemplateClient{
    @Autowired
    OAuth2RestTemplate restTemplate;

    public String initTestData() {
        ResponseEntity<String> restExchange = restTemplate.exchange("http://zuulserver:5555/api/layout/test-data/init",
                HttpMethod.POST, null, String.class);
        return restExchange.getBody();
    }

    public String initTestData(String name) {
        ResponseEntity<String> restExchange = restTemplate.exchange("http://zuulserver:5555/api/layout/test-data/init/{name}",
                HttpMethod.POST, null, String.class, name);
        return restExchange.getBody();
    }

    public String[] getTestDataNames() {
        ResponseBodyWrapper<String[]> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/layout/test-data",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<String[]>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public boolean contains(String name) {
        return Arrays.stream(getTestDataNames()).anyMatch(dataName -> dataName.equals(name));
    }

}
