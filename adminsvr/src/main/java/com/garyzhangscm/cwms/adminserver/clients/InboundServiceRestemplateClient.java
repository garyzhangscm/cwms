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
import com.garyzhangscm.cwms.adminserver.model.BillableActivity;
import com.garyzhangscm.cwms.adminserver.model.wms.Inventory;
import com.garyzhangscm.cwms.adminserver.model.wms.PutawayConfiguration;
import com.garyzhangscm.cwms.adminserver.model.wms.Receipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZonedDateTime;
import java.util.List;

@Component
public class InboundServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InboundServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    public Receipt checkInReceipt(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/inbound/receipts/{id}/check-in");
/**
        ResponseBodyWrapper<Receipt> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.PUT,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Receipt>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                Receipt.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.PUT,
                null
        );

    }

    @Cacheable(cacheNames = "AdminService_Receipt", unless="#result == null")
    public Receipt getReceiptByNumber(Long warehouseId, String receiptNumber) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/inbound/receipts")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("number", receiptNumber);
/**
        ResponseBodyWrapper<List<Receipt>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Receipt>>>() {}).getBody();

        List<Receipt> receipts = responseBodyWrapper.getData();
**/
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

    public PutawayConfiguration createPutawayConfiguration(PutawayConfiguration putawayConfiguration) throws JsonProcessingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/inbound/putaway-configuration");
/**
        ResponseBodyWrapper<PutawayConfiguration> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(putawayConfiguration)),
                new ParameterizedTypeReference<ResponseBodyWrapper<PutawayConfiguration>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                PutawayConfiguration.class,
                builder.toUriString(),
                HttpMethod.POST,
                putawayConfiguration
        );

    }

    public List<PutawayConfiguration> getPutawayConfigurationByItemFamily(Long warehouseId, String itemFamilyName) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/inbound/putaway-configuration")
                        .queryParam("itemFamilyName", itemFamilyName)
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<PutawayConfiguration>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<PutawayConfiguration>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                PutawayConfiguration.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }
    public Inventory receive(Long receiptId, Long receiptLineId,
                             Inventory inventory) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/inbound/receipts/{receiptId}/lines/{receiptLineId}/receive");
/**
        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(receiptId, receiptLineId).toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(inventory)),
                new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Inventory.class,
                builder.buildAndExpand(receiptId, receiptLineId).toUriString(),
                HttpMethod.POST,
                inventory
        );
    }

    public Receipt completeReceipt(Long receiptId) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/inbound/receipts/{id}/complete");
/**
        ResponseBodyWrapper<Receipt> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(receiptId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Receipt>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Receipt.class,
                builder.buildAndExpand(receiptId).toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public Inventory allocateLocationForPutaway(Inventory inventory) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/inbound/putaway-configuration/allocate-location");
/**
        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(inventory)),
                new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.POST,
                inventory
        );
    }

    public PutawayConfiguration addPutawayConfiguration(
            PutawayConfiguration putawayConfiguration
    ) throws JsonProcessingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inbound/putaway-configuration");
/**
        ResponseBodyWrapper<PutawayConfiguration> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.PUT,
                getHttpEntity(objectMapper.writeValueAsString(putawayConfiguration)),
                new ParameterizedTypeReference<ResponseBodyWrapper<PutawayConfiguration>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                PutawayConfiguration.class,
                builder.toUriString(),
                HttpMethod.PUT,
                putawayConfiguration
        );

    }

    public List<BillableActivity> getBillableActivities(
            Long warehouseId, Long clientId, ZonedDateTime startTime,
            ZonedDateTime endTime, Boolean includeLineActivity )   {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inbound/receipt-billable-activities/billable-activity")
                .queryParam("warehouseId", warehouseId)
                        .queryParam("clientId", clientId)
                        .queryParam("startTime", startTime)
                        .queryParam("endTime", endTime)
                        .queryParam("includeLineActivity", includeLineActivity);
/**
        ResponseBodyWrapper<List<BillableActivity>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<BillableActivity>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return  restTemplateProxy.exchangeList(
                BillableActivity.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

}
