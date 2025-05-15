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

import com.garyzhangscm.cwms.inventory.model.Pick;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Component

public class OutbuondServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(OutbuondServiceRestemplateClient.class);


    @Autowired
    private RestTemplateProxy restTemplateProxy;

    // @Cacheable(cacheNames = "InventoryService_Pick", unless="#result == null")
    public Pick getPickById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/picks/{id}");
/**
        ResponseBodyWrapper<Pick> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Pick>>() {}).getBody();

        return responseBodyWrapper.getData();
**/

        return restTemplateProxy.exchange(
                Pick.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );
    }



    public List<Pick> unpick(Long pickId, Long unpickQuantity) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/picks/{id}/unpick")
                        .queryParam("unpickQuantity", unpickQuantity);
/**
        ResponseBodyWrapper<Pick> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(pickId).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Pick>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                Pick.class,
                builder.buildAndExpand(pickId).toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public void refreshPickMovement(Long pickId, Long destinationLocationId,Long quantity) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/pick-movement/refresh")
                        .queryParam("pickId", pickId)
                        .queryParam("destinationLocationId", destinationLocationId)
                        .queryParam("quantity", quantity);
/**
        restTemplate.exchange(
                builder.buildAndExpand(pickId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();
**/

        restTemplateProxy.exchange(
                String.class,
                builder.buildAndExpand(pickId).toUriString(),
                HttpMethod.POST,
                null
        );

    }


    public List<Pick> getWorkOrderPicks(Long warehouseId, String workOrderLineIds) throws IOException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/picks")
                        .queryParam("workOrderLineIds", workOrderLineIds)
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<Pick>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Pick>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                Pick.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }
    public String handleItemOverride( Long warehouseId, Long oldItemId, Long newItemId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/outbound-configuration/item-override")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("oldItemId", oldItemId)
                        .queryParam("newItemId", newItemId);
        /**
        ResponseBodyWrapper<String> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();

        return responseBodyWrapper.getData();
         **/

        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );

    }


    public long getQuantityInOrder(Long warehouseId,
                                    Long clientId,
                                    Long itemId,
                                    Long inventoryStatusId,
                                    String color,
                                    String productSize,
                                    String style,
                                    String attribute1,
                                   String attribute2,
                                   String attribute3,
                                   String attribute4,
                                   String attribute5,
                                   boolean exactMatch) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/orders/quantity-in-order")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("itemId", itemId)
                        .queryParam("inventoryStatusId", inventoryStatusId)
                        .queryParam("exactMatch", exactMatch);
        if (Objects.nonNull(clientId)) {
            builder = builder.queryParam("clientId", clientId);
        }
        if (Strings.isNotBlank(color)) {
            builder = builder.queryParam("color", color);
        }
        if (Strings.isNotBlank(productSize)) {
            builder = builder.queryParam("productSize", productSize);
        }
        if (Strings.isNotBlank(style)) {
            builder = builder.queryParam("style", style);
        }
        if (Strings.isNotBlank(attribute1)) {
            builder = builder.queryParam("inventoryAttribute1", attribute1);
        }
        if (Strings.isNotBlank(attribute2)) {
            builder = builder.queryParam("inventoryAttribute2", attribute2);
        }
        if (Strings.isNotBlank(attribute3)) {
            builder = builder.queryParam("inventoryAttribute3", attribute3);
        }
        if (Strings.isNotBlank(attribute4)) {
            builder = builder.queryParam("inventoryAttribute4", attribute4);
        }
        if (Strings.isNotBlank(attribute5)) {
            builder = builder.queryParam("inventoryAttribute5", attribute5);
        }
        return restTemplateProxy.exchange(
                Long.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }



    public long getQuantityInOrderPick(Long warehouseId,
                                   Long clientId,
                                   Long itemId,
                                   Long inventoryStatusId,
                                   String color,
                                   String productSize,
                                   String style,
                                   boolean exactMatch) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/picks/quantity-in-order-pick")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("itemId", itemId)
                        .queryParam("inventoryStatusId", inventoryStatusId)
                        .queryParam("exactMatch", exactMatch);
        if (Objects.nonNull(clientId)) {
            builder = builder.queryParam("clientId", clientId);
        }
        if (Strings.isNotBlank(color)) {
            builder = builder.queryParam("color", color);
        }
        if (Strings.isNotBlank(productSize)) {
            builder = builder.queryParam("productSize", productSize);
        }
        if (Strings.isNotBlank(style)) {
            builder = builder.queryParam("style", style);
        }
        return restTemplateProxy.exchange(
                Long.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }


    public List<Pick> getOpenPicks(Long warehouseId, Long clientId, Long itemId, Long inventoryStatusId,
                                   Long locationId)  {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/picks")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("itemId", itemId)
                        .queryParam("inventoryStatusId", inventoryStatusId)
                        .queryParam("loadDetails", false)
                        .queryParam("openPickOnly", true);

        if (Objects.nonNull(clientId)) {
            builder = builder.queryParam("clientId", clientId);
        }
        if (Objects.nonNull(locationId)) {
            builder = builder.queryParam("locationId", locationId);
        }
        return restTemplateProxy.exchangeList(
                Pick.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

}
