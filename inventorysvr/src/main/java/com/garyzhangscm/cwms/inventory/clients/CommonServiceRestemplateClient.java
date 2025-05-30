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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.inventory.model.*;
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

    @Cacheable(cacheNames = "InventoryService_Client", unless="#result == null")
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

    @Cacheable(cacheNames = "InventoryService_Client", unless="#result == null")
    public Client getClientByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                null;
        try {
            builder = UriComponentsBuilder.newInstance()
                    .scheme("http").host("apigateway").port(5555)
                    .path("/api/common/clients")
                    .queryParam("warehouseId", warehouseId)
                    .queryParam("name", URLEncoder.encode(name, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
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

    public List<Client> getAllClients(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/clients")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<Client>> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Client>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/


        return restTemplateProxy.exchangeList(
                Client.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }


    @Cacheable(cacheNames = "InventoryService_Supplier", unless="#result == null")
    public Supplier getSupplierById(Long supplierId) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/suppliers/{id}");
/*
        ResponseBodyWrapper<Supplier> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(supplierId).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Supplier>>() {}).getBody();

        return responseBodyWrapper.getData();
        */

        return restTemplateProxy.exchange(
                Supplier.class,
                builder.buildAndExpand(supplierId).toUriString(),
                HttpMethod.GET,
                null
        );
    }
    @Cacheable(cacheNames = "InventoryService_Supplier", unless="#result == null")
    public Supplier getSupplierByName(Long warehouseId, String name) {

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

    // @Cacheable(cacheNames = "inventory_unitOfMeasure", unless="#result == null")

    @Cacheable(cacheNames = "InventoryService_UnitOfMeasure", unless="#result == null")
    public UnitOfMeasure getUnitOfMeasureById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/unit-of-measures/{id}");
/**
        ResponseBodyWrapper<UnitOfMeasure> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
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

    // @Cacheable(cacheNames = "inventory_velocity", unless="#result == null",  key = "new org.springframework.cache.interceptor.SimpleKey('warehouse_', #warehouseId.toString())")

    @Cacheable(cacheNames = "InventoryService_Velocities", key = "new org.springframework.cache.interceptor.SimpleKey('warehouse_', #warehouseId.toString())")
    public List<Velocity> getVelocitesByWarehouse(Long warehouseId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/velocities")
                .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<Velocity>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Velocity>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/


       return restTemplateProxy.exchangeList(
               Velocity.class,
               builder.toUriString(),
               HttpMethod.GET,
                null
        );
    }

    @Cacheable(cacheNames = "InventoryService_Velocity", unless="#result == null")
    public Velocity getVelocityById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/velocities/{id}");
/**
        ResponseBodyWrapper<Velocity> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Velocity>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Velocity.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    // @Cacheable(cacheNames = "inventory_abc-category", unless="#result == null", key = "new org.springframework.cache.interceptor.SimpleKey('warehouse_', #warehouseId.toString())")
    @Cacheable(cacheNames = "InventoryService_ABCCategories", unless="#result == null", key = "new org.springframework.cache.interceptor.SimpleKey('warehouse_', #warehouseId.toString())")
    public List<ABCCategory> getABCCategoriesByWarehouse(Long warehouseId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/abc-categories")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<ABCCategory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<ABCCategory>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                ABCCategory.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }
    @Cacheable(cacheNames = "InventoryService_ABCCategory", unless="#result == null")
    public ABCCategory getABCCategoryById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/abc-categories/{id}");
/**
        ResponseBodyWrapper<ABCCategory> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<ABCCategory>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                ABCCategory.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }


    @Cacheable(cacheNames = "InventoryService_UnitOfMeasure", unless="#result == null")
    public UnitOfMeasure getUnitOfMeasureByName(Long warehouseId, String name) {
        return getUnitOfMeasureByName(null, warehouseId, name, null, null);
    }
    @Cacheable(cacheNames = "InventoryService_UnitOfMeasure", unless="#result == null")
    public UnitOfMeasure getUnitOfMeasureByName(Long companyId,
                                                Long warehouseId, String name,
                                                Boolean companyUnitOfMeasure,
                                                Boolean warehouseSpecificUnitOfMeasure) {

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
        if (Objects.nonNull(companyUnitOfMeasure)) {
            builder = builder.queryParam("companyUnitOfMeasure", companyUnitOfMeasure);
        }
        if (Objects.nonNull(warehouseSpecificUnitOfMeasure)) {
            builder = builder.queryParam("warehouseSpecificUnitOfMeasure", warehouseSpecificUnitOfMeasure);
        }
/**
        ResponseBodyWrapper<List<UnitOfMeasure>> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<UnitOfMeasure>>>() {}).getBody();

        List<UnitOfMeasure> unitOfMeasures = responseBodyWrapper.getData();
**/

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


    @Cacheable(cacheNames = "InventoryService_Unit", unless="#result == null")
    public List<Unit> getUnitsByWarehouse(Long warehouseId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/units")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<Unit>> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Unit>>>() {}).getBody();


        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                Unit.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public UnitOfMeasure createUnitOfMeasure(UnitOfMeasure unitOfMeasure) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/unit-of-measures") ;
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

        logger.debug("Start to get next number for {} / {}",
                warehouseId, variable);
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

        logger.debug(">> Next number is: {}", responseBodyWrapper.getData().getNextNumber());
        return responseBodyWrapper.getData().getNextNumber();
         **/
        return restTemplateProxy.exchange(
                    SystemControlledNumber.class,
                    builder.buildAndExpand(variable).toUriString(),
                    HttpMethod.GET,
                    null
                ).getNextNumber();
    }
    public String getNextLpn(Long warehouseId) {
        return getNextNumber(warehouseId, "lpn");
    }
    public String getNextInventoryActivityTransactionId(Long warehouseId) {
        return getNextNumber(warehouseId, "inventory-activity-transaction-id");
    }
    public String getNextInventoryActivityTransactionGroupId(Long warehouseId) {
        return getNextNumber(warehouseId, "inventory-activity-transaction-group-id");
    }
    public String getNextCycleCountBatchId(Long warehouseId) {
        return getNextNumber(warehouseId, "cycle-count-batch-id");
    }

    public String getNextQCInspectionRequest(Long warehouseId) {

        return getNextNumber(warehouseId, "qc-inspection-request");
    }

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

        logger.debug("getPolicyByKey returns: {}",
                responseBodyWrapper.getData());
        List<Policy> policies = responseBodyWrapper.getData();
 **/

        List<Policy> policies =  restTemplateProxy.exchangeList(
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
}
