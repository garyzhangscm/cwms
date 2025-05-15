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


import com.garyzhangscm.cwms.inbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;

@Component
public class CommonServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(CommonServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;


    @Cacheable(cacheNames = "InboundService_Client", unless="#result == null")
    public Client getClientById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/clients/{id}");
/**
        ResponseBodyWrapper<Client> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                    null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Client>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                Client.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }
    @Cacheable(cacheNames = "InboundService_Client", unless="#result == null")
    public Client getClientByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/clients")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("name", name);
/**
        ResponseBodyWrapper<List<Client>> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<Client>>>() {}).getBody();

        List<Client> clients = responseBodyWrapper.getData();
 **/
        List<Client> clients = restTemplateProxy.exchangeList(
                Client.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );


        if (clients.size() == 0) {
            return null;
        }
        else {
            return clients.get(0);
        }
    }
    @Cacheable(cacheNames = "InboundService_Supplier", unless="#result == null")
    public Supplier getSupplierById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/suppliers/{id}");
/**
        ResponseBodyWrapper<Supplier> responseBodyWrapper
                = restTemplate.exchange(
                    builder.buildAndExpand(id).toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<Supplier>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                Supplier.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }
    @Cacheable(cacheNames = "InboundService_Supplier", unless="#result == null")
    public Supplier getSupplierByName(Long warehouseId, String name)  {

        UriComponentsBuilder builder =
                null;
        try {
            builder = UriComponentsBuilder.newInstance()
                    .scheme("http").host("apigateway").port(5555)
                    .path("/api/common/suppliers")
                    .queryParam("warehouseId", warehouseId)
                    .queryParam("name", URLEncoder.encode(name, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
/**
        ResponseBodyWrapper<List<Supplier>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Supplier>>>() {}).getBody();

        List<Supplier> suppliers = responseBodyWrapper.getData();
 **/

        List<Supplier> suppliers = restTemplateProxy.exchangeList(
                Supplier.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

        if (suppliers.size() == 0) {
            return null;
        }
        else {
            return suppliers.get(0);

        }
    }

    @Cacheable(cacheNames = "InboundService_Policy", unless="#result == null")
    public Policy getPolicyByKey(Long warehouseId, String key) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/policies")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("key", key);
/**
        ResponseBodyWrapper<List<Policy>> responseBodyWrapper
                = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<Policy>>>() {}).getBody();

        List<Policy> policies = responseBodyWrapper.getData();
 **/
        List<Policy> policies = restTemplateProxy.exchangeList(
                Policy.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );


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
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/system-controlled-number/{variable}/next")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<SystemControlledNumber> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.buildAndExpand(variable).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<SystemControlledNumber>>() {}).getBody();


        return responseBodyWrapper.getData().getNextNumber();
 **/
        return restTemplateProxy.exchange(
                SystemControlledNumber.class,
                builder.buildAndExpand(variable).toUriString(),
                HttpMethod.GET,
                null
        ).getNextNumber();

    }

    public List<String> getNextNumberInBatch(Long warehouseId, String variable, int batch) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/system-controlled-number/{variable}/batch/next")
                .queryParam("batch", batch)
                .queryParam("warehouseId", warehouseId);
        /**
        ResponseBodyWrapper<List<String>> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(variable).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<String>>>() {}).getBody();


        return responseBodyWrapper.getData();
         **/
        return restTemplateProxy.exchangeList(
                String.class,
                builder.buildAndExpand(variable).toUriString(),
                HttpMethod.GET,
                null
        );


    }

    public Customer getCustomerById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/customers/{id}");
/**
        ResponseBodyWrapper<Customer> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Customer>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Customer.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );
    }

    @Cacheable(cacheNames = "InboundService_UnitOfMeasure", unless="#result == null")
    public UnitOfMeasure getUnitOfMeasureByName(Long companyId,
                                                Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/unit-of-measures")
                        .queryParam("name", name);

        if (Objects.nonNull(companyId)) {
            builder = builder.queryParam("companyId", companyId);
        }
        if (Objects.nonNull(warehouseId)) {
            builder = builder.queryParam("warehouseId", warehouseId);
        }

        List<UnitOfMeasure> unitOfMeasures =  restTemplateProxy.exchangeList(
                UnitOfMeasure.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

        if (unitOfMeasures.size() != 1) {
            return null;
        }
        else {
            return unitOfMeasures.get(0);
        }
    }
}
