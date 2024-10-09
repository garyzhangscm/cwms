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

package com.garyzhangscm.cwms.outbound.clients;


import com.garyzhangscm.cwms.outbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;


@Component
public class WorkOrderServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WorkOrderServiceRestemplateClient.class);


    @Autowired
    private RestTemplateProxy restTemplateProxy;

    public WorkOrder getWorkOrderById(Long id) {
        return getWorkOrderById(id, true, true);

    }

    @Cacheable(cacheNames = "OutboundService_WorkOrder", unless="#result == null")
    public WorkOrder getWorkOrderById(Long id, boolean loadDetails, boolean loadWorkOrderDetails) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/workorder/work-orders/{id}")
                .queryParam("loadDetails", loadDetails)
                        .queryParam("loadWorkOrderDetails", loadWorkOrderDetails);

        return restTemplateProxy.exchange(
                WorkOrder.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    @Cacheable(cacheNames = "OutboundService_WorkOrderLine", unless="#result == null")
    public WorkOrderLine getWorkOrderLineById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/workorder/work-orders/lines/{id}");
/**
        ResponseBodyWrapper<WorkOrderLine> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrderLine>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                WorkOrderLine.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }
    @Cacheable(cacheNames = "OutboundService_WorkOrder", unless="#result == null")
    public WorkOrder getWorkOrderByNumber(Long warehouseId, String number) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/workorder/work-orders")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("number", number);
/**
        ResponseBodyWrapper<List<WorkOrder>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<WorkOrder>>>() {}).getBody();

        // Even the web call will return a list of work order, since we specify the number and warehouse id
        // we are expecting only one result(or null)
        List<WorkOrder> workOrders = responseBodyWrapper.getData();
 **/

        List<WorkOrder> workOrders =  restTemplateProxy.exchangeList(
                WorkOrder.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
        if (workOrders.size() != 1) {
            return null;
        }
        else {
            return workOrders.get(0);
        }

    }

    public WorkOrder registerPickCancelled(Long workOrderLineId,
                                           Long cancelledQuantity,
                                           Long destinationLocationId){
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/workorder/work-orders/lines/{id}/pick-cancelled")
                        .queryParam("cancelledQuantity", cancelledQuantity)
                        .queryParam("destinationLocationId", destinationLocationId);
/**
        ResponseBodyWrapper<WorkOrder> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(workOrderLineId).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrder>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                WorkOrder.class,
                builder.buildAndExpand(workOrderLineId).toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public WorkOrder registerShortAllocationCancelled(Long workOrderLineId, Long cancelledQuantity){
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/workorder/work-orders/lines/{id}/short-allocation-cancelled")
                        .queryParam("cancelledQuantity", cancelledQuantity);
/**
        ResponseBodyWrapper<WorkOrder> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(workOrderLineId).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrder>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                WorkOrder.class,
                builder.buildAndExpand(workOrderLineId).toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public WorkOrder createWorkOrderForShortAllocation(Long id, Long bomId, String workOrderNumber, Long workOrderQuantity) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/workorder/work-orders/create-for-short-allocation")
                        .queryParam("shortAllocationId", id)
                        .queryParam("billOfMaterialId", bomId)
                        .queryParam("workOrderNumber", workOrderNumber)
                        .queryParam("expectedQuantity", workOrderQuantity);
/**
        ResponseBodyWrapper<WorkOrder> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrder>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                WorkOrder.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public WorkOrderLine inventoryPickedForWorkOrderLine(Long workOrderLineId,
                                                         Long quantityBeingPicked,
                                                         Long deliveredLocationId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/workorder/work-orders/lines/{id}/inventory-being-delivered")
                        .queryParam("quantityBeingDelivered", quantityBeingPicked)
                        .queryParam("deliveredLocationId", deliveredLocationId);
/**
        ResponseBodyWrapper<WorkOrderLine> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(workOrderLineId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrderLine>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                WorkOrderLine.class,
                builder.buildAndExpand(workOrderLineId).toUriString(),
                HttpMethod.POST,
                null
        );

    }

}
