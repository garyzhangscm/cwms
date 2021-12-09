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

import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class InboundServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InboundServiceRestemplateClient.class);

    @Autowired
    OAuth2RestOperations restTemplate;


    public Receipt getReceiptById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inbound/receipts/{id}");

        ResponseBodyWrapper<Receipt> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Receipt>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public ReceiptLine reverseReceivedInventory(Long receiptId, Long receiptLineId, Long quantity,
                                                Boolean inboundQCRequired, Boolean reverseQCQuantity) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inbound/receipts/{receiptId}/lines/{receiptLineId}/reverse")
                .queryParam("quantity", quantity);
        if (Boolean.TRUE.equals(inboundQCRequired)) {
            builder = builder.queryParam("inboundQCRequired", true)
                    .queryParam("reverseQCQuantity", reverseQCQuantity);
        }


        ResponseBodyWrapper<ReceiptLine> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(receiptId, receiptLineId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<ReceiptLine>>() {}).getBody();

        return responseBodyWrapper.getData();

    }



}
