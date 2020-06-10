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
import com.garyzhangscm.cwms.inventory.model.SystemControlledNumber;
import com.garyzhangscm.cwms.inventory.model.UnitOfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class CommonServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(CommonServiceRestemplateClient.class);

    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;

    public Client getClientById(Long id) {

        UriComponentsBuilder builder =
            UriComponentsBuilder.newInstance()
                    .scheme("http").host("zuulservice")
                    .path("/api/common/client/{id}");

        ResponseBodyWrapper<Client> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Client>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public Client getClientByName(String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/common/clients")
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

    public Supplier getSupplierByName(String name) {

        UriComponentsBuilder builder =
            UriComponentsBuilder.newInstance()
                    .scheme("http").host("zuulservice")
                    .path("/api/common/suppliers")
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

    public UnitOfMeasure getUnitOfMeasureById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/common/unit-of-measure/{id}");

        ResponseBodyWrapper<UnitOfMeasure> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<UnitOfMeasure>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public UnitOfMeasure getUnitOfMeasureByName(String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/common/unit-of-measure")
                        .queryParam("name", name);

        ResponseBodyWrapper<UnitOfMeasure> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<UnitOfMeasure>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public String getNextNumber(String variable) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/common/system-controlled-number/{variable}/next");
        ResponseBodyWrapper<SystemControlledNumber> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(variable).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<SystemControlledNumber>>() {}).getBody();

        return responseBodyWrapper.getData().getNextNumber();
    }
    public String getNextLpn() {
        return getNextNumber("lpn");
    }
    public String getNextInventoryActivityTransactionId() {
        return getNextNumber("inventory-activity-transaction-id");
    }
    public String getNextInventoryActivityTransactionGroupId() {
        return getNextNumber("inventory-activity-transaction-group-id");
    }
    public String getNextCycleCountBatchId() {
        return getNextNumber("cycle-count-batch-id");
    }

}
