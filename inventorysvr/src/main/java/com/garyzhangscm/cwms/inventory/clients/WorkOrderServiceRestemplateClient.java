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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;


@Component
public class WorkOrderServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WorkOrderServiceRestemplateClient.class);

    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;


    @Cacheable(cacheNames = "inventory_workorder", unless="#result == null")
    public WorkOrder getWorkOrderById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/workorder/work-orders/{id}");

        ResponseBodyWrapper<WorkOrder> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrder>>() {}).getBody();

        return responseBodyWrapper.getData();

    }


    @Cacheable(cacheNames = "inventory_workorder_qcsample", unless="#result == null")
    public WorkOrderQCSample getWorkOrderQCSampleByNumber(Long warehouseId,
                                                          String number) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/workorder/qc-samples")
                .queryParam("warehouseId", warehouseId)
                .queryParam("number", number);

        ResponseBodyWrapper<List<WorkOrderQCSample>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<WorkOrderQCSample>>>() {}).getBody();

        List<WorkOrderQCSample> workOrderQCSamples =
                responseBodyWrapper.getData();
        if (workOrderQCSamples.size() != 1) {
            return null;
        }
        return workOrderQCSamples.get(0);

    }

    @Cacheable(cacheNames = "inventory_workorder_qcsample", unless="#result == null")
    public WorkOrderQCSample getWorkOrderQCSampleById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/workorder/qc-samples/{id}");

        ResponseBodyWrapper<WorkOrderQCSample> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrderQCSample>>() {}).getBody();

        return responseBodyWrapper.getData();

    }


    public WorkOrderMaterialConsumeTiming inventoryDeliveredForWorkOrderLine(Long workOrderLineId,
                                                         Long quantityBeingPicked,
                                                         Long deliveredLocationId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/workorder/work-orders/lines/{id}/inventory-being-delivered")
                        .queryParam("quantityBeingDelivered", quantityBeingPicked)
                        .queryParam("deliveredLocationId", deliveredLocationId);

        ResponseBodyWrapper<WorkOrderMaterialConsumeTiming> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(workOrderLineId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrderMaterialConsumeTiming>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public WorkOrder addQCQuantity(Long workOrderId, Long qcQuantity) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/workorder/work-orders/{id}/add-qc-quantity")
                        .queryParam("qcQuantity", qcQuantity);

        ResponseBodyWrapper<WorkOrder> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(workOrderId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrder>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
}
