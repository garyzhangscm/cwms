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

import com.garyzhangscm.cwms.workorder.model.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component

public class OutboundServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(OutboundServiceRestemplateClient.class);
/**
    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;
    // private ObjectMapper mapper = new ObjectMapper();
**/
    @Autowired
    private RestTemplateProxy restTemplateProxy;

    public AllocationResult allocateWorkOrder(WorkOrder workOrder, Long productionLineId, Long quantity) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/allocation/work-order");

        if (Objects.nonNull(productionLineId)) {
            builder = builder.queryParam("productionLineId", productionLineId);
        }
        if (Objects.nonNull(quantity)) {
            builder = builder.queryParam("quantity", quantity);
        }
/**
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
 **/

        return restTemplateProxy.exchange(
                AllocationResult.class,
                builder.toUriString(),
                HttpMethod.POST,
                workOrder
        );

    }

    public Pick cancelPick(Long pickId){

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/picks/{id}");

/**
        ResponseBodyWrapper<Pick> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(pickId).toUriString(),
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Pick>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Pick.class,
                builder.buildAndExpand(pickId).toUriString(),
                HttpMethod.DELETE,
                null
        );

    }


    @Cacheable(cacheNames = "WorkOrderService_OrderLine", unless="#result == null")
    public OrderLine getOrderLineById(Long orderLineId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/orders/lines/{id}");
/**
        ResponseBodyWrapper<OrderLine> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(orderLineId).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<OrderLine>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                OrderLine.class,
                builder.buildAndExpand(orderLineId).toUriString(),
                HttpMethod.GET,
                null
        );



    }
    public List<Pick> getWorkOrderPicks(WorkOrder workOrder)   {

        // make sure the work order has at least one line
        String workOrderLineIds = getWorkOrderLineIds(workOrder);
        if (Strings.isBlank(workOrderLineIds) ||
               workOrderLineIds.trim().equals(",")) {
            // there's no line in the work order
            // let's return empty list as there's no
            // picks as long as there's no work order line
            return new ArrayList<>();
        }
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/picks")
                        .queryParam("workOrderLineIds", workOrderLineIds)
                        .queryParam("warehouseId", workOrder.getWarehouseId());
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

    public List<ShortAllocation> getWorkOrderShortAllocations(WorkOrder workOrder)  {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/shortAllocations")
                        .queryParam("warehouseId", workOrder.getWarehouseId())
                        .queryParam("workOrderLineIds", getWorkOrderLineIds(workOrder));
/**
        ResponseBodyWrapper<List<ShortAllocation>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<ShortAllocation>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                ShortAllocation.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    private String getWorkOrderLineIds(WorkOrder workOrder) {
        return workOrder.getWorkOrderLines().stream()
                  .map(WorkOrderLine::getId).map(String::valueOf).collect(Collectors.joining(","));

    }

    public List<Pick> getWorkOrderLinePicks(WorkOrderLine workOrderLine)  {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/picks")
                        .queryParam("workOrderLineId", workOrderLine.getId())
                        .queryParam("warehouseId", workOrderLine.getWorkOrder().getWarehouseId())
                        .queryParam("loadDetails", false);

/**
        ResponseBodyWrapper<List<Pick>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper< List<Pick>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                Pick.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public List<ShortAllocation> getWorkOrderLineShortAllocations(WorkOrderLine workOrderLine) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/shortAllocations")
                        .queryParam("warehouseId", workOrderLine.getWorkOrder().getWarehouseId())
                        .queryParam("workOrderLineId", workOrderLine.getId())
                        .queryParam("loadDetails", false);
/**
        ResponseBodyWrapper<List<ShortAllocation>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper< List<ShortAllocation>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                ShortAllocation.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }
    public OrderLine registerProductionPlanLine(Long orderLineId, ProductionPlanLine productionPlanLine) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/orders/lines/{id}/production-plan-line/register")
                        .queryParam("productionPlanLineQuantity", productionPlanLine.getExpectedQuantity());
/**
        ResponseBodyWrapper<OrderLine> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(orderLineId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<OrderLine>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                OrderLine.class,
                builder.buildAndExpand(orderLineId).toUriString(),
                HttpMethod.POST,
                null
        );
    }
    public OrderLine registerProductionPlanLineProduced(Long orderLineId, Long producedQuantity) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/orders/lines/{id}/production-plan/produced")
                        .queryParam("producedQuantity", producedQuantity);
/**
        ResponseBodyWrapper<OrderLine> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(orderLineId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<OrderLine>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                OrderLine.class,
                builder.buildAndExpand(orderLineId).toUriString(),
                HttpMethod.POST,
                null
        );

    }


    public List<Pick> generateManualPick(Long warehouseId, Long workOrderId, String lpn,
                                        long productionLineId,  long pickableQuantity){

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/picks/generate-manual-pick-for-work-order")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("workOrderId", workOrderId)
                        .queryParam("productionLineId", productionLineId)
                        .queryParam("pickableQuantity", pickableQuantity)
                        .queryParam("lpn", lpn);
/**
        ResponseBodyWrapper<List<Pick>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Pick>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                Pick.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public AllocationResult allocateWorkOrderLine(WorkOrderLine workOrderLine,
                                                  Long productionLineId,
                                                  Long allocatingQuantity) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/outbound/allocation/work-order-line")
                .queryParam("workOrderId", workOrderLine.getWorkOrder().getId());

        if (Objects.nonNull(productionLineId)) {
            builder = builder.queryParam("productionLineId", productionLineId);
        }
        if (Objects.nonNull(allocatingQuantity)) {
            builder = builder.queryParam("quantity", allocatingQuantity);
        }
/**
        ResponseBodyWrapper<AllocationResult> responseBodyWrapper
                = null;
        try {
            responseBodyWrapper = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.POST,
                    getHttpEntity(objectMapper.writeValueAsString(workOrderLine)),
                    new ParameterizedTypeReference<ResponseBodyWrapper<AllocationResult>>() {}).getBody();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw WorkOrderException.raiseException("Can't allocate work order line due to JsonProcessingException: " + e.getMessage());
        }

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                AllocationResult.class,
                builder.toUriString(),
                HttpMethod.POST,
                workOrderLine
        );

    }

}
