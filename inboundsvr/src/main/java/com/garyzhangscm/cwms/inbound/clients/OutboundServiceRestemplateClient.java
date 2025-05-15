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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;


@Component

public class OutboundServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(OutboundServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;


    public void addRequestReturnQuantity(
             Long warehouseId, Long orderLineId, Long requestReturnQuantity) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/orders/lines/{id}/add-request-return-quantity")
                .queryParam("warehouseId", warehouseId)
                .queryParam("requestReturnQuantity", requestReturnQuantity);
/**
        restTemplate.postForEntity(
                    builder.buildAndExpand(orderLineId).toUriString(), null, Void.class);

**/
       restTemplateProxy.exchange(
               Void.class,
               builder.buildAndExpand(orderLineId).toUriString(),
                HttpMethod.POST,
                null
        );
    }
/**
    public void addActualReturnQuantity(
            Long warehouseId, Long orderLineId, Long actualReturnQuantity) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/orders/lines/{id}/add-actual-return-quantity")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("actualReturnQuantity", actualReturnQuantity);

        restTemplate.postForEntity(
                builder.buildAndExpand(orderLineId).toUriString(), null, Void.class);


    }
**/

}
