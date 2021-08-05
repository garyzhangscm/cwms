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
import com.garyzhangscm.cwms.inventory.model.Company;
import com.garyzhangscm.cwms.inventory.model.WorkOrder;
import com.garyzhangscm.cwms.inventory.model.WorkOrderLine;
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

    @Cacheable(cacheNames = "workOrder")
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


    public WorkOrderLine inventoryDeliveredForWorkOrderLine(Long workOrderLineId,
                                                         Long quantityBeingPicked,
                                                         Long deliveredLocationId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/workorder/work-orders/lines/{id}/inventory-being-delivered")
                        .queryParam("quantityBeingDelivered", quantityBeingPicked)
                        .queryParam("deliveredLocationId", deliveredLocationId);

        ResponseBodyWrapper<WorkOrderLine> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(workOrderLineId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrderLine>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
}
