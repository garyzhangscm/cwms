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

package com.garyzhangscm.cwms.inventory.clients;

import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.model.Client;
import com.garyzhangscm.cwms.inventory.model.Supplier;
import com.garyzhangscm.cwms.inventory.model.UnitOfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CommonServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(CommonServiceRestemplateClient.class);

    @Autowired
    OAuth2RestTemplate restTemplate;

    public Client getClientById(Long id) {

        ResponseBodyWrapper<Client> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/common/client/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Client>>() {}, id).getBody();

        return responseBodyWrapper.getData();

    }
    public Client getClientByName(String name) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://zuulserver:5555/api/common/client")
                .queryParam("name", name);
        ResponseBodyWrapper<Client> responseBodyWrapper = restTemplate.exchange(builder.toUriString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Client>>() {}).getBody();
        return responseBodyWrapper.getData();
    }
    public Supplier getSupplierById(Long id) {

        ResponseBodyWrapper<Supplier> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/common/supplier/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Supplier>>() {}, id).getBody();

        return responseBodyWrapper.getData();
    }
    public Supplier getSupplierByName(String name) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://zuulserver:5555/api/common/supplier")
                .queryParam("name", name);
        ResponseBodyWrapper<Supplier> responseBodyWrapper = restTemplate.exchange(builder.toUriString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Supplier>>() {}).getBody();
        return responseBodyWrapper.getData();
    }

    public UnitOfMeasure getUnitOfMeasureById(Long id) {

        ResponseBodyWrapper<UnitOfMeasure> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/common/unit-of-measure/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<UnitOfMeasure>>() {}, id).getBody();

        return responseBodyWrapper.getData();
    }
    public UnitOfMeasure getUnitOfMeasureByName(String name) {


        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://zuulserver:5555/api/common/unit-of-measure")
                .queryParam("name", name);

        logger.debug("builder.toUriString(): " + builder.toUriString());
        ResponseBodyWrapper<UnitOfMeasure> responseBodyWrapper = restTemplate.exchange(builder.toUriString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<UnitOfMeasure>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

}
