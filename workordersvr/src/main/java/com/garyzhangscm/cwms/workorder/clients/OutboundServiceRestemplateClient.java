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

package com.garyzhangscm.cwms.workorder.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.ResponseBodyWrapper;
import com.garyzhangscm.cwms.workorder.exception.GenericException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component

public class OutboundServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(OutboundServiceRestemplateClient.class);

    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;
    // private ObjectMapper mapper = new ObjectMapper();

    public AllocationResult allocateWorkOrder(WorkOrder workOrder, Long productionLineId, Long quantity) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/outbound/allocation/work-order");

        if (Objects.nonNull(productionLineId)) {
            builder = builder.queryParam("productionLineId", productionLineId);
        }
        if (Objects.nonNull(quantity)) {
            builder = builder.queryParam("quantity", quantity);
        }

        ResponseBodyWrapper<AllocationResult> responseBodyWrapper
                = null;
        try {
            responseBodyWrapper = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.POST,
                    getHttpEntity(objectMapper.writeValueAsString(workOrder)),
                    new ParameterizedTypeReference<ResponseBodyWrapper<AllocationResult>>() {}).getBody();
        } catch (JsonProcessingException e) {
            throw WorkOrderException.raiseException("Can't allocate work order due to JsonProcessingException: " + e.getMessage());
        }

        return responseBodyWrapper.getData();

    }


    @RequestMapping(value="/picks/{id}", method = RequestMethod.DELETE)
    public Pick cancelPick(Long pickId){

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/outbound/picks/{id}");


        ResponseBodyWrapper<Pick> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(pickId).toUriString(),
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Pick>>() {}).getBody();

        return responseBodyWrapper.getData();
    }


    public OrderLine getOrderLineById(Long orderLineId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/outbound/orders/lines/{id}");

        ResponseBodyWrapper<OrderLine> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(orderLineId).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<OrderLine>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public List<Pick> getWorkOrderPicks(WorkOrder workOrder)   {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/outbound/picks")
                        .queryParam("workOrderLineIds", getWorkOrderLineIds(workOrder))
                        .queryParam("warehouseId", workOrder.getWarehouseId());

        ResponseBodyWrapper<List<Pick>> responseBodyWrapper
                = restTemplate.exchange(
                            builder.toUriString(),
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<ResponseBodyWrapper<List<Pick>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<ShortAllocation> getWorkOrderShortAllocations(WorkOrder workOrder)  {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/outbound/shortAllocations")
                        .queryParam("warehouseId", workOrder.getWarehouseId())
                        .queryParam("workOrderLineIds", getWorkOrderLineIds(workOrder));

        ResponseBodyWrapper<List<ShortAllocation>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<ShortAllocation>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    private String getWorkOrderLineIds(WorkOrder workOrder) {
        return workOrder.getWorkOrderLines().stream()
                  .map(WorkOrderLine::getId).map(String::valueOf).collect(Collectors.joining(","));

    }

    public List<Pick> getWorkOrderLinePicks(WorkOrderLine workOrderLine)  {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/outbound/picks")
                        .queryParam("workOrderLineId", workOrderLine.getId())
                        .queryParam("warehouseId", workOrderLine.getWorkOrder().getWarehouseId())
                        .queryParam("loadDetails", false);


        ResponseBodyWrapper<List<Pick>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper< List<Pick>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<ShortAllocation> getWorkOrderLineShortAllocations(WorkOrderLine workOrderLine) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/outbound/shortAllocations")
                        .queryParam("warehouseId", workOrderLine.getWorkOrder().getWarehouseId())
                        .queryParam("workOrderLineId", workOrderLine.getId())
                        .queryParam("loadDetails", false);

        ResponseBodyWrapper<List<ShortAllocation>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper< List<ShortAllocation>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public OrderLine registerProductionPlanLine(Long orderLineId, ProductionPlanLine productionPlanLine) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/outbound/orders/lines/{id}/production-plan-line/register")
                        .queryParam("productionPlanLineQuantity", productionPlanLine.getExpectedQuantity());

        ResponseBodyWrapper<OrderLine> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(orderLineId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<OrderLine>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public OrderLine registerProductionPlanLineProduced(Long orderLineId, Long producedQuantity) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/outbound/orders/lines/{id}/production-plan/produced")
                        .queryParam("producedQuantity", producedQuantity);

        ResponseBodyWrapper<OrderLine> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(orderLineId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<OrderLine>>() {}).getBody();

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
