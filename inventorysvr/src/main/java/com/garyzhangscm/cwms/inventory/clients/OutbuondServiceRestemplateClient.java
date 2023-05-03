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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

@Component

public class OutbuondServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(OutbuondServiceRestemplateClient.class);


    @Autowired
    private RestTemplateProxy restTemplateProxy;

    // @Cacheable(cacheNames = "InventoryService_Pick", unless="#result == null")
    public Pick getPickById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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



    public Pick unpick(Long pickId, Long unpickQuantity) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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

        return restTemplateProxy.exchange(
                Pick.class,
                builder.buildAndExpand(pickId).toUriString(),
                HttpMethod.POST,
                null
        );

    }

    public void refreshPickMovement(Long pickId, Long destinationLocationId,Long quantity) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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
                        .scheme("http").host("zuulserver").port(5555)
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
                        .scheme("http").host("zuulserver").port(5555)
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




}
