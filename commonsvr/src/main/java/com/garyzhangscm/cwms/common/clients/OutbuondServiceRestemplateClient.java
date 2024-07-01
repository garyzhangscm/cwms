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

package com.garyzhangscm.cwms.common.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.common.ResponseBodyWrapper;
import com.garyzhangscm.cwms.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

@Component

public class OutbuondServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(OutbuondServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    public String validateNewOrderNumber(Long warehouseId, String orderNumber) {

        UriComponentsBuilder builder =
                null;
        try {
            builder = UriComponentsBuilder.newInstance()
                    .scheme("http").host("zuulserver").port(5555)
                    .path("/api/outbound/orders/validate-new-order-number")
                    .queryParam("warehouseId", warehouseId)
                    .queryParam("orderNumber", URLEncoder.encode(orderNumber, "UTF-8"));
/**
            ResponseBodyWrapper<String> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.POST,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();

            return responseBodyWrapper.getData();
**/
            return restTemplateProxy.exchange(
                    String.class,
                    builder.build(true).toUriString(),
                    HttpMethod.POST,
                    null
            );

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw ResourceNotFoundException.raiseException("can't find the order by name " + orderNumber);
        }

    }



    public Integer getOrderCountForCustomer(Long warehouseId, Long customerId) {

        UriComponentsBuilder builder =
            UriComponentsBuilder.newInstance()
                .scheme("http").host("zuulserver").port(5555)
                .path("/api/outbound/orders/count")
                .queryParam("warehouseId", warehouseId)
                .queryParam("customerId", customerId);
        return restTemplateProxy.exchange(
                Integer.class,
                builder.build(true).toUriString(),
                HttpMethod.GET,
                null
        );

    }




}
