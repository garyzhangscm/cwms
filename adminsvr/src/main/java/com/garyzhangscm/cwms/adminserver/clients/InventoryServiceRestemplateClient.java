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
import com.garyzhangscm.cwms.adminserver.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.adminserver.model.ClientInventoryAgingSnapshot;
import com.garyzhangscm.cwms.adminserver.model.ClientLocationUtilizationSnapshotBatch;
import com.garyzhangscm.cwms.adminserver.model.InventoryAgingForBilling;
import com.garyzhangscm.cwms.adminserver.model.wms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class InventoryServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    @Cacheable(cacheNames = "AdminService_Item", unless="#result == null")
    public Item getItemById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/items/{id}");
/**
        ResponseBodyWrapper<Item> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Item>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                Item.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    @Cacheable(cacheNames = "AdminService_Item", unless="#result == null")
    public Item getItemByName(Long warehouseId, Long clientId, String name) {

        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("apigateway").port(5555)
                            .path("/api/inventory/items")
                            .queryParam("name", URLEncoder.encode(name, "UTF-8"))
                            .queryParam("warehouseId", warehouseId);

            if (Objects.nonNull(clientId)) {
                builder = builder.queryParam("clientIds", String.valueOf(clientId));
            }


            // logger.debug("Start to get item: {} / {}", name, warehouseId);
            /**
            ResponseBodyWrapper<List<Item>> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<Item>>>() {}).getBody();

            List<Item> items = responseBodyWrapper.getData();
             **/

            List<Item> items =  restTemplateProxy.exchangeList(
                    Item.class,
                    builder.build(true).toUriString(),
                    HttpMethod.GET,
                    null
            );
            logger.debug(">> get {} item", items.size());
            if (items.size() == 0) {
                return null;
            }
            else {
                return items.get(0);
            }
        }
        catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            throw ResourceNotFoundException.raiseException("can't find the item by name " + name);
        }
    }

    @Cacheable(cacheNames = "AdminService_ItemFamily", unless="#result == null")
    public ItemFamily getItemFamilyByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/item-families")
                        .queryParam("name", name)
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<ItemFamily>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<ItemFamily>>>() {}).getBody();

        List<ItemFamily> itemFamilies = responseBodyWrapper.getData();
 **/

        List<ItemFamily> itemFamilies =  restTemplateProxy.exchangeList(
                ItemFamily.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
        if (itemFamilies.size() == 0) {
            return null;
        }
        else {
            return itemFamilies.get(0);
        }
    }


    // @Cacheable(cacheNames = "AdminService_Inventory", unless="#result == null")
    public Inventory getInventoryById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory/{id}");
/**
        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Inventory.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public List<Inventory> findInventoryByReceipt(Long warehouseId, Long receiptId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("receiptId", receiptId)
                        .queryParam("warehouseId", warehouseId);

/**
        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    @Cacheable(cacheNames = "AdminService_InventoryStatus", unless="#result == null")
    public InventoryStatus getInventoryStatusByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory-statuses")
                        .queryParam("name", name)
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<InventoryStatus>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<InventoryStatus>>>() {}).getBody();

        List<InventoryStatus> inventoryStatuses = responseBodyWrapper.getData();
 **/

        List<InventoryStatus> inventoryStatuses =  restTemplateProxy.exchangeList(
                InventoryStatus.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
        if (inventoryStatuses.size() == 0) {
            return null;
        }
        else {
            return inventoryStatuses.get(0);
        }
    }


    // @Cacheable(cacheNames = "AdminService_AvailableInventoryStatus", unless="#result == null")
    public InventoryStatus getAvailableInventoryStatus(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory-statuses/available")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<InventoryStatus> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<InventoryStatus>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                InventoryStatus.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }


    public Inventory createInventory(Inventory inventory) throws JsonProcessingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory-adj");
/**
        ResponseBodyWrapper<Inventory> responseBodyWrapper
                    = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.PUT,
                        getHttpEntity(objectMapper.writeValueAsString(inventory)),
                        new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.PUT,
                inventory
        );

    }

    public List<Inventory> getPickedInventory(Long warehouseId, List<Pick> picks) {
        // Convert a list of picks into a list of pick ids and join them into a single string with comma
        // Then we can call the inventory service endpoint to get all the picked inventory with those picks
        String pickIds =  picks.stream().map(Pick::getId).map(String::valueOf).collect(Collectors.joining(","));

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("pickIds", pickIds)
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchangeList(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public List<MovementPath> getPickMovementPath(Long warehouseId, Location sourceLocation, Location destinationLocation) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/movement-path/match")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("fromLocationId", sourceLocation.getId())
                        .queryParam("fromLocation", sourceLocation.getName())
                        .queryParam("fromLocationGroupId", sourceLocation.getLocationGroup().getId())
                        .queryParam("toLocationId", destinationLocation.getId())
                        .queryParam("toLocation", destinationLocation.getName())
                        .queryParam("toLocationGroupId", destinationLocation.getLocationGroup().getId());
/**
        ResponseBodyWrapper<List<MovementPath>> responseBodyWrapper =
                restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<MovementPath>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                MovementPath.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }
    /**
     * Split the inventory into 2,
     * the first one in the list is the original inventory with updated quantity
     * the second one in the list is the new inventory
     * @param inventory the inventory to be split
     * @param newLpn the new LPN for the split inventory
     * @param newQuantity The new quantity for the split inventory
     * @return A list of 2 inventory, the 1st is the original inventory with reduced quantity
     */
    public List<Inventory> split(Inventory inventory, String newLpn, Long newQuantity) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory/{id}/split")
                        .queryParam("newLpn", newLpn)
                        .queryParam("newQuantity", newQuantity);
/**
        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(inventory.getId()).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                Inventory.class,
                builder.buildAndExpand(inventory.getId()).toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public Inventory moveInventory(Inventory inventory, Location nextLocation) throws JsonProcessingException {
        return moveInventory(inventory, nextLocation, "");
    }
    public Inventory moveInventory(Inventory inventory, Location nextLocation, String destinationLpn) throws JsonProcessingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory/{id}/move")
                .queryParam("destinationLpn", destinationLpn);
/**
        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(inventory.getId()).toUriString(),
                        HttpMethod.POST,
                        getHttpEntity(objectMapper.writeValueAsString(nextLocation)),
                        new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Inventory.class,
                builder.buildAndExpand(inventory.getId()).toUriString(),
                HttpMethod.POST,
                nextLocation
        );
    }

    public Inventory adjustInventoryQuantity(Inventory inventory, Long newQuantity) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory/{id}/adjust-quantity")
                        .queryParam("newQuantity", newQuantity);
/**
        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(inventory.getId()).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                Inventory.class,
                builder.buildAndExpand(inventory.getId()).toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public InventoryAdjustmentThreshold createInventoryAdjustmentThreshold(InventoryAdjustmentThreshold inventoryAdjustmentThreshold) throws JsonProcessingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory-adjustment-thresholds");
/**
        ResponseBodyWrapper<InventoryAdjustmentThreshold> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.PUT,
                getHttpEntity(objectMapper.writeValueAsString(inventoryAdjustmentThreshold)),
                new ParameterizedTypeReference<ResponseBodyWrapper<InventoryAdjustmentThreshold>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                InventoryAdjustmentThreshold.class,
                builder.toUriString(),
                HttpMethod.PUT,
                inventoryAdjustmentThreshold
        );

    }


    public List<InventoryAdjustmentThreshold> getInventoryAdjustmentThresholdByItem(Long warehouseId, String itemName) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory-adjustment-thresholds")
                .queryParam("itemName", itemName)
                .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<InventoryAdjustmentThreshold>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<InventoryAdjustmentThreshold>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                InventoryAdjustmentThreshold.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public List<InventoryAdjustmentThreshold> getInventoryAdjustmentThresholdByItemFamily(Long warehouseId, String itemFamilyName) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory-adjustment-thresholds")
                        .queryParam("itemFamilyName", itemFamilyName)
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<List<InventoryAdjustmentThreshold>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<InventoryAdjustmentThreshold>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                InventoryAdjustmentThreshold.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
                );
    }
    public List<InventoryAdjustmentRequest> getPendingInventoryAdjustmentRequestByInventoryId(Long warehouseId, Long inventoryId) {

        return getInventoryAdjustmentRequestByInventoryId(warehouseId, inventoryId, InventoryAdjustmentRequestStatus.PENDING);
    }
    public List<InventoryAdjustmentRequest> getInventoryAdjustmentRequestByInventoryId(Long warehouseId, Long inventoryId,
                                                                                       InventoryAdjustmentRequestStatus status) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory-adjustment-requests")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("inventoryId", inventoryId)
                        .queryParam("status", status);
/**
        ResponseBodyWrapper<List<InventoryAdjustmentRequest>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<InventoryAdjustmentRequest>>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchangeList(
                InventoryAdjustmentRequest.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public InventoryAdjustmentRequest approveInventoryAdjustmentRequest(Long inventoryAdjustmentRequestId) {
        return processInventoryAdjustmentRequest(inventoryAdjustmentRequestId, true);
    }
    public InventoryAdjustmentRequest disapproveInventoryAdjustmentReuqest(Long inventoryAdjustmentRequestId) {
        return processInventoryAdjustmentRequest(inventoryAdjustmentRequestId, false);
    }

    public InventoryAdjustmentRequest processInventoryAdjustmentRequest(Long inventoryAdjustmentRequestId, boolean approved) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory-adjustment-requests/{id}/process")
                        .queryParam("approved", approved);
/**
        ResponseBodyWrapper<InventoryAdjustmentRequest> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(inventoryAdjustmentRequestId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<InventoryAdjustmentRequest>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                InventoryAdjustmentRequest.class,
                builder.buildAndExpand(inventoryAdjustmentRequestId).toUriString(),
                HttpMethod.POST,
                null
        );
    }


    public List<CycleCountRequest> requestCycleCount(long warehouseId, String beginLocation,String endLocation, boolean includeEmptyLocation) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/cycle-count-requests")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("cycleCountRequestType", CycleCountRequestType.BY_LOCATION_RANGE)
                        .queryParam("beginValue", beginLocation)
                        .queryParam("endValue", endLocation)
                        .queryParam("includeEmptyLocation", includeEmptyLocation);
/**
        ResponseBodyWrapper<List<CycleCountRequest>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<CycleCountRequest>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                CycleCountRequest.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );

    }

    public List<CycleCountResult> confirmCycleCountRequests(CycleCountRequest cycleCountRequest, List<CycleCountResult> cycleCountResults)
             {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/cycle-count-request/{id}/confirm");
/**
        ResponseBodyWrapper<List<CycleCountResult>> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(cycleCountRequest.getId()).toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(cycleCountResults)),
                new ParameterizedTypeReference<ResponseBodyWrapper<List<CycleCountResult>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                CycleCountResult.class,
                builder.buildAndExpand(cycleCountRequest.getId()).toUriString(),
                HttpMethod.POST,
                cycleCountResults
        );
    }

    public List<CycleCountResult> confirmCycleCountRequests(List<CycleCountRequest> cycleCountRequests) {
        String cycleCountRequestIds = cycleCountRequests.stream().map(CycleCountRequest::getId).
                map(String::valueOf).collect(Collectors.joining(","));
        return confirmCycleCountRequests(cycleCountRequestIds);

    }
    public List<CycleCountResult> confirmCycleCountRequests(String cycleCountRequestIds) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/cycle-count-request/confirm")
                        .queryParam("cycleCountRequestIds", cycleCountRequestIds);
/**
        ResponseBodyWrapper<List<CycleCountResult>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<CycleCountResult>>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchangeList(
                CycleCountResult.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );

    }

    public CycleCountRequest getCycleCountRequestById(long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/cycle-count-request/{id}");
/**
        ResponseBodyWrapper<CycleCountRequest> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<CycleCountRequest>>() {}).getBody();

        return responseBodyWrapper.getData();
**/

        return restTemplateProxy.exchange(
                CycleCountRequest.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public AuditCountRequest getAuditCountRequestById(long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/audit-count-request/{id}");
/**
        ResponseBodyWrapper<AuditCountRequest> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<AuditCountRequest>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                AuditCountRequest.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public List<CycleCountRequest> cancelCycleCountRequests(String cycleCountRequestIds) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/cycle-count-request/cancel")
                        .queryParam("cycleCountRequestIds", cycleCountRequestIds);
/**
        ResponseBodyWrapper<List<CycleCountRequest>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<CycleCountRequest>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                CycleCountRequest.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );

    }

    public List<CycleCountRequest> reopenCycleCountRequests(String cycleCountRequestIds) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/cycle-count-request/reopen")
                        .queryParam("cycleCountRequestIds", cycleCountRequestIds);
/**
        ResponseBodyWrapper<List<CycleCountRequest>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<CycleCountRequest>>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchangeList(
                CycleCountRequest.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );

    }
    public List<AuditCountResult> confirmAuditCountRequest(AuditCountResult auditCountResult) throws JsonProcessingException {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/audit-count-result/{batchId}/{locationId}/confirm");
/**
        ResponseBodyWrapper<List<AuditCountResult>> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(auditCountResult.getBatchId(), auditCountResult.getLocationId()).toUriString(),
                HttpMethod.POST,
                getHttpEntity(objectMapper.writeValueAsString(Collections.singletonList(auditCountResult))),
                new ParameterizedTypeReference<ResponseBodyWrapper<List<AuditCountResult>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                AuditCountResult.class,
                builder.buildAndExpand(auditCountResult.getBatchId(), auditCountResult.getLocationId()).toUriString(),
                HttpMethod.POST,
                Collections.singletonList(auditCountResult)
        );


    }

    public List<ClientLocationUtilizationSnapshotBatch> getLocationUtilizationSnapshotByClient(
            Long warehouseId, Long clientId, ZonedDateTime startTime, ZonedDateTime endTime) {

            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("apigateway").port(5555)
                            .path("/api/inventory/client-location-utilization-snapshots")
                            .queryParam("warehouseId", warehouseId)
                            .queryParam("loadDetails", false)
                            .queryParam("startTime", startTime)
                            .queryParam("endTime", endTime);
            if (Objects.nonNull(clientId)) {
                builder = builder.queryParam("clientId", clientId);
            }


            // logger.debug("Start to get item: {} / {}", name, warehouseId);
        /**
            ResponseBodyWrapper<List<ClientLocationUtilizationSnapshotBatch>> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<ClientLocationUtilizationSnapshotBatch>>>() {}).getBody();

            return responseBodyWrapper.getData();
         **/

        return restTemplateProxy.exchangeList(
                ClientLocationUtilizationSnapshotBatch.class,
                builder.build(true).toUriString(),
                HttpMethod.GET,
                null
        );
    }


    public List<ClientInventoryAgingSnapshot> getClientInventoryAgingSnapshot(
            Long warehouseId, Long clientId, ZonedDateTime startTime, ZonedDateTime endTime) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory-aging-snapshots/by-client/group-by-lpn")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("loadDetails", false)
                        .queryParam("startTime", startTime)
                        .queryParam("endTime", endTime);
        if (Objects.nonNull(clientId)) {
            builder = builder.queryParam("clientId", clientId);
        }


        // logger.debug("Start to get item: {} / {}", name, warehouseId);
        /**
        ResponseBodyWrapper<List<ClientInventoryAgingSnapshot>> responseBodyWrapper
                = restTemplate.exchange(
                builder.build(true).toUri(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<ClientInventoryAgingSnapshot>>>() {}).getBody();

        return responseBodyWrapper.getData();
         **/

        return restTemplateProxy.exchangeList(
                ClientInventoryAgingSnapshot.class,
                builder.build(true).toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public ClientInventoryAgingSnapshot getClientInventoryAgingSnapshotGroupByLPNForBilling(
            Long warehouseId, Long clientId, ZonedDateTime startTime, ZonedDateTime endTime) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory-aging-snapshots/by-client/billing/group-by-lpn")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("loadDetails", false)
                        .queryParam("startTime", startTime)
                        .queryParam("endTime", endTime);
        if (Objects.nonNull(clientId)) {
            builder = builder.queryParam("clientId", clientId);
        }


        // logger.debug("Start to get item: {} / {}", name, warehouseId);

        return restTemplateProxy.exchange(
                ClientInventoryAgingSnapshot.class,
                builder.build(true).toUriString(),
                HttpMethod.GET,
                null
        );
    }


    public List<InventoryAgingForBilling> getInventoryAgingForBilling(
            Long warehouseId, Long clientId, String billableCategory,
            ZonedDateTime startTime, ZonedDateTime endTime,
            Boolean includeDaysSinceInWarehouseForStorageFee) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories/inventory-aging-for-billing")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("billableCategory", billableCategory);

        if (Objects.nonNull(clientId)) {
            builder = builder.queryParam("clientId", clientId);
        }
        if (Objects.nonNull(startTime)) {
            builder = builder.queryParam("startTime", startTime);
        }
        if (Objects.nonNull(endTime)) {
            builder = builder.queryParam("endTime", endTime);
        }
        if (Objects.nonNull(includeDaysSinceInWarehouseForStorageFee)) {
            builder = builder.queryParam("includeDaysSinceInWarehouseForStorageFee", includeDaysSinceInWarehouseForStorageFee);
        }

        return restTemplateProxy.exchangeList(
                InventoryAgingForBilling.class,
                builder.build(true).toUriString(),
                HttpMethod.GET,
                null
        );
    }
}
