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

import com.garyzhangscm.cwms.integration.model.Order;
import com.garyzhangscm.cwms.integration.model.OrderLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class OutbuondServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(OutbuondServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    @Cacheable(cacheNames = "IntegrationService_Order", unless="#result == null")
    public Order getOrderById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/outbound/orders/{id}");
/**
        ResponseBodyWrapper<Order> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Order>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                Order.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );



    }

    @Cacheable(cacheNames = "IntegrationService_OrderLine", unless="#result == null")
    public OrderLine getOrderLineById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/outbound/orders/lines/{id}");
        return restTemplateProxy.exchange(
                OrderLine.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );



    }

    @Cacheable(cacheNames = "IntegrationService_Order", unless="#result == null")
    public Order getOrderByNumber(Long warehosueId, String number) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/outbound/orders")
                .queryParam("warehouseId", warehosueId)
                .queryParam("number", number)
                .queryParam("loadDetails", false);
/**
        ResponseBodyWrapper<List<Order>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Order>>>() {}).getBody();

        List<Order> orders = responseBodyWrapper.getData();
 **/
        List<Order> orders = restTemplateProxy.exchangeList(
                Order.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

        if (orders.size() == 0 ) {
            return  null;
        }
        else {
            return orders.get(0);
        }

    }

}
