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
import com.garyzhangscm.cwms.adminserver.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.adminserver.model.wms.*;
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

    @Cacheable(cacheNames = "AdminService_Client", unless="#result == null")
    public Client getClientById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/clients/{id}");
/**
        ResponseBodyWrapper<Client> responseBodyWrapper
                = restTemplate.exchange(
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

    @Cacheable(cacheNames = "AdminService_Client", unless="#result == null")
    public Client getClientByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                null;
        try {
            builder = UriComponentsBuilder.newInstance()
                    .scheme("http").host("zuulserver").port(5555)
                    .path("/api/common/clients")
                    .queryParam("name", URLEncoder.encode(name, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
/**
        ResponseBodyWrapper<List<Client>> responseBodyWrapper
                = restTemplate.exchange(
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

    @Cacheable(cacheNames = "AdminService_Supplier", unless="#result == null")
    public Supplier getSupplierByName(String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/suppliers")
                        .queryParam("name", name);
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
    @Cacheable(cacheNames = "AdminService_Customer", unless="#result == null")
    public Customer getCustomerByName(Long companyId, Long warehouseId, String name) {

        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("zuulserver").port(5555)
                            .path("/api/common/customers")
                            .queryParam("name", URLEncoder.encode(name, "UTF-8"));

            if (Objects.nonNull(companyId)) {
                builder = builder.queryParam("companyId", companyId);
            }
            if (Objects.nonNull(warehouseId)) {
                builder = builder.queryParam("warehouseId", warehouseId);
            }
            /**
            ResponseBodyWrapper<List<Customer>> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<Customer>>>() {
                    }).getBody();

            List<Customer> customers = responseBodyWrapper.getData();
**/

            List<Customer> customers = restTemplateProxy.exchangeList(
                    Customer.class,
                    builder.build(true).toUriString(),
                    HttpMethod.GET,
                    null
            );

            if (customers.size() == 0) {
                return null;
            } else {
                return customers.get(0);
            }
        }
        catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            throw ResourceNotFoundException.raiseException("can't find the customer by name " + name);
        }

    }

    @Cacheable(cacheNames = "AdminService_UnitOfMeasure", unless="#result == null")
    public UnitOfMeasure getUnitOfMeasureById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/unit-of-measures/{id}");
/**
        ResponseBodyWrapper<UnitOfMeasure> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<UnitOfMeasure>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                UnitOfMeasure.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );
    }
    public List<UnitOfMeasure> getUnitOfMeasureByWarehouseId(Long warehouseId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/unit-of-measures")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<UnitOfMeasure>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<UnitOfMeasure>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                UnitOfMeasure.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    @Cacheable(cacheNames = "AdminService_UnitOfMeasure", unless="#result == null")
    public UnitOfMeasure getUnitOfMeasureByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/unit-of-measures")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("name", name);
/**
        ResponseBodyWrapper<List<UnitOfMeasure>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<UnitOfMeasure>>>() {}).getBody();

        List<UnitOfMeasure> unitOfMeasures = responseBodyWrapper.getData();
**/
        List<UnitOfMeasure> unitOfMeasures = restTemplateProxy.exchangeList(
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


    public UnitOfMeasure createUnitOfMeasure(UnitOfMeasure unitOfMeasure) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/unit-of-measures");
/**
        ResponseBodyWrapper<UnitOfMeasure> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(unitOfMeasure)),
                new ParameterizedTypeReference<ResponseBodyWrapper<UnitOfMeasure>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                UnitOfMeasure.class,
                builder.toUriString(),
                HttpMethod.POST,
                unitOfMeasure
        );

    }


    public String getNextNumber(Long warehouseId, String variable) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/system-controlled-number/{variable}/next")
                        .queryParam("warehouseId", warehouseId);
        /**
        ResponseBodyWrapper<SystemControlledNumber> responseBodyWrapper
                = restTemplate.exchange(
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

    public List<SystemControlledNumber> getSystemControlledNumberByWarehouseId(Long warehouseId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/system-controlled-numbers")
                .queryParam("warehouseId", warehouseId);
        /**
        ResponseBodyWrapper<List<SystemControlledNumber>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<SystemControlledNumber>>>() {}).getBody();

        return responseBodyWrapper.getData();
         **/

        return restTemplateProxy.exchangeList(
                SystemControlledNumber.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }


    public SystemControlledNumber addSystemControlledNumbers(SystemControlledNumber systemControlledNumber) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/system-controlled-numbers");
/**
        ResponseBodyWrapper<SystemControlledNumber> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.PUT,
                getHttpEntity(objectMapper.writeValueAsString(systemControlledNumber)),
                new ParameterizedTypeReference<ResponseBodyWrapper<SystemControlledNumber>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                SystemControlledNumber.class,
                builder.toUriString(),
                HttpMethod.PUT,
                systemControlledNumber
        );
    }


    public String getNextLpn(Long warehouseId) {
        return getNextNumber(warehouseId, "lpn");
    }

    public List<Policy> getPoliciesByWarehouseId(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/policies")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<Policy>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Policy>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                Policy.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }
}
