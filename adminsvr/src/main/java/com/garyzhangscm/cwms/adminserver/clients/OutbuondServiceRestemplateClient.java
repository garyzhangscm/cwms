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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.adminserver.ResponseBodyWrapper;
import com.garyzhangscm.cwms.adminserver.model.wms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component

public class OutbuondServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(OutbuondServiceRestemplateClient.class);

    @Autowired
    // OAuth2RestOperations restTemplate;
    RestTemplate restTemplate;
    @Autowired
    @Qualifier("getObjMapper")
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;


    public Order getOrderByNumber(Long warehouseId, String orderNumber) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/orders")
                        .queryParam("number", orderNumber)
                        .queryParam("warehouseId", warehouseId);

        ResponseBodyWrapper<List<Order>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Order>>>() {}).getBody();

        List<Order> orders = responseBodyWrapper.getData();
        if (orders.size() != 1) {
            return null;
        }
        return orders.get(0);

    }


    public Pick getPickById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/picks/{id}");

        ResponseBodyWrapper<Pick> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Pick>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public List<Pick> getPicksByOrder(Long warehouseId, Order order) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/picks")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("orderId", order.getId());

        ResponseBodyWrapper<List<Pick>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Pick>>>() {}).getBody();

        return responseBodyWrapper.getData();

    }


    public List<Pick> getPicksByShortAllocation(Long warehouseId, ShortAllocation shortAllocation) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/picks")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("shortAllocationId", shortAllocation.getId());

        ResponseBodyWrapper<List<Pick>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Pick>>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public List<ShortAllocation> getShortAllocationsByOrder(Long warehouseId, Order order) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/shortAllocations")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("orderId", order.getId());

        ResponseBodyWrapper<List<ShortAllocation>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<ShortAllocation>>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public Pick unpick(Long pickId, Long unpickQuantity) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/picks/{id}/unpick")
                        .queryParam("unpickQuantity", unpickQuantity);

        ResponseBodyWrapper<Pick> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(pickId).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Pick>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public void refreshPickMovement(Long pickId, Long destinationLocationId,Long quantity) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/pick-movement/refresh")
                        .queryParam("pickId", pickId)
                        .queryParam("destinationLocationId", destinationLocationId)
                        .queryParam("quantity", quantity);

        restTemplate.exchange(
                builder.buildAndExpand(pickId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();


    }

    public Order allocateOrder(Order order) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/orders/{id}/allocate");
        ResponseBodyWrapper<Order> responseBodyWrapper =
                restTemplate.exchange(
                        builder.buildAndExpand(order.getId()).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Order>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public AllocationConfiguration createAllocationConfiguration(AllocationConfiguration allocationConfiguration) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/allocation-configuration");
        ResponseBodyWrapper<AllocationConfiguration> responseBodyWrapper =
                restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.POST,
                        getHttpEntity(objectMapper.writeValueAsString(allocationConfiguration)),
                        new ParameterizedTypeReference<ResponseBodyWrapper<AllocationConfiguration>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public List<AllocationConfiguration> getAllocationConfiguration(Warehouse warehouse,
                                                                    ItemFamily itemFamily) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/allocation-configuration")
                        .queryParam("warehouseId", warehouse.getId())
                        .queryParam("itemFamilyId", itemFamily.getId());

        ResponseBodyWrapper<List<AllocationConfiguration>> responseBodyWrapper =
                restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<AllocationConfiguration>>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public PickableUnitOfMeasure addPickableUnitOfMeasure(PickableUnitOfMeasure pickableUnitOfMeasure) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/pickable-unit-of-measures");
        ResponseBodyWrapper<PickableUnitOfMeasure> responseBodyWrapper =
                restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.POST,
                        getHttpEntity(objectMapper.writeValueAsString(pickableUnitOfMeasure)),
                        new ParameterizedTypeReference<ResponseBodyWrapper<PickableUnitOfMeasure>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public AllocationConfiguration addPickableUnitOfMeasure(AllocationConfiguration allocationConfiguration,
                                                          PickableUnitOfMeasure pickableUnitOfMeasure) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/allocation-configuration/{id}/pickable-unit-of-measures");

        ResponseBodyWrapper<AllocationConfiguration> responseBodyWrapper =
                restTemplate.exchange(
                        builder.buildAndExpand(allocationConfiguration.getId()).toUriString(),
                        HttpMethod.POST,
                        getHttpEntity(objectMapper.writeValueAsString(pickableUnitOfMeasure)),
                        new ParameterizedTypeReference<ResponseBodyWrapper<AllocationConfiguration>>() {}).getBody();

        return responseBodyWrapper.getData();

    }


    public Pick confirmPick(Pick pick, Long confirmQuantity) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/picks/{id}/confirm")
                        .queryParam("quantity", confirmQuantity);

        ResponseBodyWrapper<Pick> responseBodyWrapper =
                restTemplate.exchange(
                        builder.buildAndExpand(pick.getId()).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Pick>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public Pick confirmPick(Pick pick) {

        // confirm with all remaining quantity
        return confirmPick(pick, pick.getQuantity() - pick.getPickedQuantity());

    }

    public Order completeOrder(Order order) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/orders/{id}/complete");

        ResponseBodyWrapper<Order> responseBodyWrapper =
                restTemplate.exchange(
                        builder.buildAndExpand(order.getId()).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Order>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Inventory> getPickedInventoryByOrder(Long warehouseId, Order order) {
        List<Pick> picks = getPicksByOrder(warehouseId, order);

        return inventoryServiceRestemplateClient.getPickedInventory(warehouseId, picks);
    }

    public EmergencyReplenishmentConfiguration createEmergencyReplenishmentConfiguration(EmergencyReplenishmentConfiguration configuration) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/emergency-replenishment-configuration");
        ResponseBodyWrapper<EmergencyReplenishmentConfiguration> responseBodyWrapper =
                restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.POST,
                        getHttpEntity(objectMapper.writeValueAsString(configuration)),
                        new ParameterizedTypeReference<ResponseBodyWrapper<EmergencyReplenishmentConfiguration>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<EmergencyReplenishmentConfiguration> getEmergencyReplenishmentConfiguration(Warehouse warehouse, ItemFamily itemFamily) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/outbound/emergency-replenishment-configuration")
                        .queryParam("warehouseId", warehouse.getId())
                        .queryParam("itemFamilyId", itemFamily.getId());
        ResponseBodyWrapper<List<EmergencyReplenishmentConfiguration>> responseBodyWrapper =
                restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<EmergencyReplenishmentConfiguration>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    private HttpEntity<String> getHttpEntity(String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        return new HttpEntity<String>(requestBody, headers);
    }


}