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

package com.garyzhangscm.cwms.integration.clients;

import com.garyzhangscm.cwms.integration.ResponseBodyWrapper;
import com.garyzhangscm.cwms.integration.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class CommonServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(CommonServiceRestemplateClient.class);

    @Autowired
    RestTemplate restTemplate;

    public Client getClientById(Long id) {

        ResponseBodyWrapper<Client> responseBodyWrapper = restTemplate.exchange("http://zuulservice/api/common/client/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Client>>() {}, id).getBody();

        return responseBodyWrapper.getData();

    }
    public Client getClientByName(String name) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://zuulservice/api/common/clients")
                .queryParam("name", name);
        ResponseBodyWrapper<List<Client>> responseBodyWrapper = restTemplate.exchange(builder.toUriString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Client>>>() {}).getBody();
        List<Client> clients = responseBodyWrapper.getData();
        if (clients.size() == 0) {
            return null;
        }
        else {
            return clients.get(0);
        }
    }
    public Supplier getSupplierById(Long id) {

        ResponseBodyWrapper<Supplier> responseBodyWrapper = restTemplate.exchange("http://zuulservice/api/common/supplier/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Supplier>>() {}, id).getBody();

        return responseBodyWrapper.getData();
    }
    public Supplier getSupplierByName(String name) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://zuulservice/api/common/suppliers")
                .queryParam("name", name);
        ResponseBodyWrapper<List<Supplier>> responseBodyWrapper = restTemplate.exchange(builder.toUriString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Supplier>>>() {}).getBody();
        List<Supplier> suppliers = responseBodyWrapper.getData();
        if (suppliers.size() == 0) {
            return null;
        }
        else {
            return suppliers.get(0);

        }
    }

    public UnitOfMeasure getUnitOfMeasureById(Long id) {

        ResponseBodyWrapper<UnitOfMeasure> responseBodyWrapper = restTemplate.exchange("http://zuulservice/api/common/unit-of-measure/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<UnitOfMeasure>>() {}, id).getBody();

        return responseBodyWrapper.getData();
    }
    public UnitOfMeasure getUnitOfMeasureByName(String name) {


        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://zuulservice/api/common/unit-of-measure")
                .queryParam("name", name);

        logger.debug("builder.toUriString(): " + builder.toUriString());
        ResponseBodyWrapper<UnitOfMeasure> responseBodyWrapper = restTemplate.exchange(builder.toUriString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<UnitOfMeasure>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public Customer getCustomerByName(String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/common/customers")
                        .queryParam("name", name);

        ResponseBodyWrapper<List<Customer>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Customer>>>() {}).getBody();

        List<Customer> customers = responseBodyWrapper.getData();
        if (customers.size() == 0) {
            return null;
        }
        else {
            return customers.get(0);
        }
    }

    public Carrier getCarrierByName(String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/common/carriers")
                        .queryParam("name", name);

        ResponseBodyWrapper<List<Carrier>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Carrier>>>() {}).getBody();

        List<Carrier> carriers = responseBodyWrapper.getData();
        if (carriers.size() == 0) {
            return null;
        }
        else {
            return carriers.get(0);
        }
    }
    public Carrier getCarrierById(Long id) {

        ResponseBodyWrapper<Carrier> responseBodyWrapper = restTemplate.exchange("http://zuulservice/api/common/carriers/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Carrier>>() {}, id).getBody();

        return responseBodyWrapper.getData();
    }


}
