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

import com.garyzhangscm.cwms.inventory.model.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;

@Component
public class InboundServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InboundServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    @Cacheable(cacheNames = "InventoryService_Receipt", unless="#result == null")
    public Receipt getReceiptById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inbound/receipts/{id}");
/**
        ResponseBodyWrapper<Receipt> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Receipt>>() {}).getBody();

        return responseBodyWrapper.getData();
**/

        return restTemplateProxy.exchange(
                Receipt.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );
    }
    @Cacheable(cacheNames = "InventoryService_Receipt", unless="#result == null")
    public Receipt getReceiptByNumber(Long warehouseId, String receiptNumber) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inbound/receipts")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("number", receiptNumber);
        List<Receipt> receipts =  restTemplateProxy.exchangeList(
                Receipt.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

        if (receipts.size() == 0) {
            return null;
        }
        else {
            return receipts.get(0);
        }

    }


    public ReceiptLine reverseReceivedInventory(Long receiptId, Long receiptLineId, Long quantity,
                                                Boolean inboundQCRequired, Boolean reverseQCQuantity) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inbound/receipts/{receiptId}/lines/{receiptLineId}/reverse")
                .queryParam("quantity", quantity);
        if (Boolean.TRUE.equals(inboundQCRequired)) {
            builder = builder.queryParam("inboundQCRequired", true)
                    .queryParam("reverseQCQuantity", reverseQCQuantity);
        }

/**
        ResponseBodyWrapper<ReceiptLine> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(receiptId, receiptLineId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<ReceiptLine>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                ReceiptLine.class,
                builder.buildAndExpand(receiptId, receiptLineId).toUriString(),
                HttpMethod.POST,
                null
        );

    }


    public String handleItemOverride( Long warehouseId, Long oldItemId, Long newItemId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inbound/inbound-configuration/item-override")
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


    public ReportHistory printLPNLabel(Long receiptLineId, String lpn, Long quantity, String printerName) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inbound/receipts/receipt-lines/{receiptLineId}/pre-print-lpn-label")
                        .queryParam("lpn", lpn);
        if (Objects.nonNull(quantity)) {
            builder = builder.queryParam("quantity", quantity);
        }
        if (Strings.isNotBlank(printerName)) {
            builder = builder.queryParam("printerName", printerName);
        }
/**
        ResponseBodyWrapper<ReportHistory> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(receiptLineId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<ReportHistory>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                ReportHistory.class,
                builder.buildAndExpand(receiptLineId).toUriString(),
                HttpMethod.POST,
                null
        );
    }


}
