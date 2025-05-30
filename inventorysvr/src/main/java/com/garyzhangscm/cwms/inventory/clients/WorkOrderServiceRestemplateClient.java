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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;


@Component
public class WorkOrderServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WorkOrderServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;


    public WorkOrder getWorkOrderById(Long id) {
        return getWorkOrderById(id, true, true);

    }
    @Cacheable(cacheNames = "InventoryService_WorkOrder", unless="#result == null")
    public WorkOrder getWorkOrderById(Long id, boolean loadDetails, boolean loadWorkOrderDetails) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
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


    @Cacheable(cacheNames = "InventoryService_BillOfMaterial", unless="#result == null")
    public BillOfMaterial getBillOfMaterialById(Long id,  Boolean loadDetails) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/workorder/bill-of-materials/{id}");
        if (Objects.nonNull(loadDetails)) {
            builder = builder.queryParam("loadDetails", loadDetails);
        }

        return restTemplateProxy.exchange(
                BillOfMaterial.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }


    @Cacheable(cacheNames = "InventoryService_WorkOrderQCSample", unless="#result == null")
    public WorkOrderQCSample getWorkOrderQCSampleByNumber(Long warehouseId,
                                                          String number) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/workorder/qc-samples")
                .queryParam("warehouseId", warehouseId)
                .queryParam("number", number);
/**
        ResponseBodyWrapper<List<WorkOrderQCSample>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<WorkOrderQCSample>>>() {}).getBody();

        List<WorkOrderQCSample> workOrderQCSamples =
                responseBodyWrapper.getData();
 **/

        List<WorkOrderQCSample> workOrderQCSamples =  restTemplateProxy.exchangeList(
                WorkOrderQCSample.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
        if (workOrderQCSamples.size() != 1) {
            return null;
        }
        return workOrderQCSamples.get(0);

    }

    @Cacheable(cacheNames = "InventoryService_WorkOrderQCSample", unless="#result == null")
    public WorkOrderQCSample getWorkOrderQCSampleById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/workorder/qc-samples/{id}");
/**
        ResponseBodyWrapper<WorkOrderQCSample> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrderQCSample>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                WorkOrderQCSample.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );


    }

    /**
     * Notify the work order service when the inventory is delivered in the production line's stage
     * for the work order
     * @param workOrderLineId inventory is picked for the work order line
     * @param quantityBeingPicked total quantity being picked
     * @param deliveredLocationId id of the production line's stage location
     * @param inventoryId optional. ID of the inventory being deposit
     * @return when the inventory will be consumed(when deliver / by transaction / when work order close)
     */
    public WorkOrderMaterialConsumeTiming inventoryDeliveredForWorkOrderLine(Long workOrderLineId,
                                                         Long quantityBeingPicked,
                                                         Long deliveredLocationId,
                                                                             Long inventoryId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/workorder/work-orders/lines/{id}/inventory-being-delivered")
                        .queryParam("quantityBeingDelivered", quantityBeingPicked)
                        .queryParam("deliveredLocationId", deliveredLocationId);

        if (Objects.nonNull(inventoryId)) {
            builder = builder.queryParam("inventoryId", inventoryId);
        }
        /**
        ResponseBodyWrapper<WorkOrderMaterialConsumeTiming> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(workOrderLineId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrderMaterialConsumeTiming>>() {}).getBody();

        return responseBodyWrapper.getData();
         **/

        return restTemplateProxy.exchange(
                WorkOrderMaterialConsumeTiming.class,
                builder.buildAndExpand(workOrderLineId).toUriString(),
                HttpMethod.POST,
                null
        );


    }

    public WorkOrder addQCQuantity(Long workOrderId, Long qcQuantity) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/workorder/work-orders/{id}/add-qc-quantity")
                        .queryParam("qcQuantity", qcQuantity);
/**
        ResponseBodyWrapper<WorkOrder> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(workOrderId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrder>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                WorkOrder.class,
                builder.buildAndExpand(workOrderId).toUriString(),
                HttpMethod.POST,
                null
        );

    }

    public ReportHistory printLPNLabel(Long workOrderId, String lpn, Long quantity, String printerName) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/workorder/work-orders/{workOrderId}/pre-print-lpn-label")
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
                builder.buildAndExpand(workOrderId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<ReportHistory>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                ReportHistory.class,
                builder.buildAndExpand(workOrderId).toUriString(),
                HttpMethod.POST,
                null
        );

    }


    public String handleItemOverride( Long warehouseId, Long oldItemId, Long newItemId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/workorder/work-orders/item-override")
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

}
