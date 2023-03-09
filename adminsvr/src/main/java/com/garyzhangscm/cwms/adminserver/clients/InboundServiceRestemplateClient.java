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
import com.garyzhangscm.cwms.adminserver.model.BillableActivity;
import com.garyzhangscm.cwms.adminserver.model.wms.Inventory;
import com.garyzhangscm.cwms.adminserver.model.wms.Location;
import com.garyzhangscm.cwms.adminserver.model.wms.PutawayConfiguration;
import com.garyzhangscm.cwms.adminserver.model.wms.Receipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZonedDateTime;
import java.util.List;

@Component
public class InboundServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InboundServiceRestemplateClient.class);

    @Autowired
    // OAuth2RestTemplate restTemplate;
    // private OAuth2RestOperations restTemplate;
    RestTemplate restTemplate;

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Cacheable(cacheNames = "AdminService_Receipt", unless="#result == null")
    public Receipt getReceiptById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/inbound/receipts/{id}");

        ResponseBodyWrapper<Receipt> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Receipt>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public Receipt checkInReceipt(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/inbound/receipts/{id}/check-in");

        ResponseBodyWrapper<Receipt> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.PUT,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Receipt>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    @Cacheable(cacheNames = "AdminService_Receipt", unless="#result == null")
    public Receipt getReceiptByNumber(Long warehouseId, String receiptNumber) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/inbound/receipts")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("number", receiptNumber);

        ResponseBodyWrapper<List<Receipt>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Receipt>>>() {}).getBody();

        List<Receipt> receipts = responseBodyWrapper.getData();
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

        ResponseBodyWrapper<PutawayConfiguration> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(putawayConfiguration)),
                new ParameterizedTypeReference<ResponseBodyWrapper<PutawayConfiguration>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<PutawayConfiguration> getPutawayConfigurationByItemFamily(Long warehouseId, String itemFamilyName) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/inbound/putaway-configuration")
                        .queryParam("itemFamilyName", itemFamilyName)
                        .queryParam("warehouseId", warehouseId);

        ResponseBodyWrapper<List<PutawayConfiguration>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<PutawayConfiguration>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public Inventory receive(Long receiptId, Long receiptLineId,
                             Inventory inventory) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/inbound/receipts/{receiptId}/lines/{receiptLineId}/receive");

        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(receiptId, receiptLineId).toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(inventory)),
                new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public Receipt completeReceipt(Long receiptId) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/inbound/receipts/{id}/complete");

        ResponseBodyWrapper<Receipt> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(receiptId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Receipt>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public Inventory allocateLocationForPutaway(Inventory inventory) throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/inbound/putaway-configuration/allocate-location");

        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(inventory)),
                new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public PutawayConfiguration addPutawayConfiguration(
            PutawayConfiguration putawayConfiguration
    ) throws JsonProcessingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inbound/putaway-configuration");

        ResponseBodyWrapper<PutawayConfiguration> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.PUT,
                getHttpEntity(objectMapper.writeValueAsString(putawayConfiguration)),
                new ParameterizedTypeReference<ResponseBodyWrapper<PutawayConfiguration>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public List<BillableActivity> findBillableActivities(
            Long warehouseId, Long clientId, ZonedDateTime startTime,
            ZonedDateTime endTime, Boolean includeLineActivity
    ) throws JsonProcessingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inbound/receipt-billable-activities/billable-activity")
                .queryParam("warehouseId", warehouseId)
                        .queryParam("clientId", clientId)
                        .queryParam("startTime", startTime)
                        .queryParam("endTime", endTime)
                        .queryParam("includeLineActivity", includeLineActivity);

        ResponseBodyWrapper<List<BillableActivity>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<BillableActivity>>>() {}).getBody();

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
