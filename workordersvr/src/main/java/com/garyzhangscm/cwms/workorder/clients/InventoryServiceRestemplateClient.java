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
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.Inventory;
import com.garyzhangscm.cwms.workorder.model.InventoryStatus;
import com.garyzhangscm.cwms.workorder.model.Item;
import com.garyzhangscm.cwms.workorder.model.WorkOrder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;

@Component
public class InventoryServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceRestemplateClient.class);

    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;
    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    public Inventory getInventoryById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventory/{id}");

        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public Item getItemById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/items/{id}");

        ResponseBodyWrapper<Item> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Item>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public Item getItemByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/items")
                        .queryParam("name", name)
                        .queryParam("warehouseId", warehouseId);


        // logger.debug("Start to get item: {} / {}", name, warehouseId);
        ResponseBodyWrapper<List<Item>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Item>>>() {}).getBody();

        List<Item> items = responseBodyWrapper.getData();
        // logger.debug(">> get {} item", items.size());
        if (items.size() == 0) {
            return null;
        }
        else {
            return items.get(0);
        }
    }


    public InventoryStatus getInventoryStatusById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventory-status/{id}");

        ResponseBodyWrapper<InventoryStatus> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<InventoryStatus>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public InventoryStatus getInventoryStatusByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventory-statuses")
                        .queryParam("name", name)
                        .queryParam("warehouseId", warehouseId);

        ResponseBodyWrapper<List<InventoryStatus>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<InventoryStatus>>>() {}).getBody();

        List<InventoryStatus> inventoryStatuses = responseBodyWrapper.getData();
        if (inventoryStatuses.size() == 0) {
            return null;
        }
        else {
            return inventoryStatuses.get(0);
        }
    }

    public List<Inventory> findDeliveredInventory(Long warehouseId,
                                                  Long productionLocationId, String pickIds) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("locationId", productionLocationId)
                        .queryParam("pickIds", pickIds);

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public List<Inventory> getProducedByProduct(Long warehouseId, String workOrderByProductIds) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("workOrderByProductIds", workOrderByProductIds);

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public List<Inventory>  getReturnedInventory(Long warehouseId, String workOrderLineIds) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("workOrderLineIds", workOrderLineIds);

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public List<Inventory> findProducedInventory(Long warehouseId, Long workOrderId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("workOrderId", workOrderId);

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public Inventory receiveInventoryFromWorkOrder(WorkOrder workOrder, Inventory inventory) {

        // Convert the inventory to JSON and send to the inventory service
        logger.debug("Will receive inventory under the document number {}",
                workOrder.getNumber());

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/receive")
                .queryParam("documentNumber", workOrder.getNumber());

        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = null;
        try {
            responseBodyWrapper = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.PUT,
                    getHttpEntity(objectMapper.writeValueAsString(inventory)),
                    new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();
        } catch (JsonProcessingException e) {
            throw WorkOrderException.raiseException("Can't add inventory due to JsonProcessingException: " + e.getMessage());
        }

        return responseBodyWrapper.getData();
    }


    public Inventory unpickFromWorkOrder(Inventory inventory,
                                         Long destinationLocationId,
                                         String destinationLocationName,
                                         boolean immediateMove) {

        // Convert the inventory to JSON and send to the inventory service
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("warehouseId", String.valueOf(inventory.getWarehouseId()));
        if (Objects.nonNull(destinationLocationId)) {
            params.add("destinationLocationId", String.valueOf(destinationLocationId));
        }
        if (StringUtils.isNotBlank(destinationLocationName)) {
            params.add("destinationLocationName", destinationLocationName);
        }
        params.add("immediateMove", String.valueOf(immediateMove));

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventory/{id}/unpick")
                .queryParams(params);


        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = restTemplate.exchange(
                    builder.buildAndExpand(inventory.getId()).toUriString(),
                    HttpMethod.POST,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();


        return responseBodyWrapper.getData();
    }


    /**
     * Consume all the materials when complete the work order
     * If there's still material left, we will need to go through
     * the return process
     *
     * @param warehouseId
     * @param workOrderLineIds
     * @return
     */
    public List<Inventory> consumeAllMaterials(Long warehouseId, String workOrderLineIds) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories/consume")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("workOrderLineIds", workOrderLineIds);

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public List<Inventory> consumeMaterialForWorkOrderLine(Long workOrderLineId, Long warehouseId, Long quantity, Long inboundLocationId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories/consume/workOrderLine/{id}")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("quantity", quantity)
                        .queryParam("locationId", inboundLocationId);

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(workOrderLineId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

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
