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

import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;

@Component
public class InventoryServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceRestemplateClient.class);
/**
    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;
    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;
**/

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    // @Cacheable(cacheNames = "WorkOrderService_Inventory", unless="#result == null")
    public Inventory getInventoryById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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

    @Cacheable(cacheNames = "WorkOrderService_QCRule", unless="#result == null")
    public QCRule getQCRuleById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/qc-rules/{id}");
/**
        ResponseBodyWrapper<QCRule> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<QCRule>>() {}).getBody();

        return responseBodyWrapper.getData();
**/

        return restTemplateProxy.exchange(
                QCRule.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    @Cacheable(cacheNames = "WorkOrderService_Item", unless="#result == null")
    public Item getItemById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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

    @Cacheable(cacheNames = "WorkOrderService_Item", unless="#result == null")
    public Item getItemByName(Long warehouseId, String name) {
        return getItemByName(warehouseId, null, name);
    }
    @Cacheable(cacheNames = "WorkOrderService_Item", unless="#result == null")
    public Item getItemByName(Long warehouseId, Long clientId, String name) {

        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("zuulserver").port(5555)
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

            // logger.debug(">> get {} item", items.size());
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


    @Cacheable(cacheNames = "WorkOrderService_InventoryStatus", unless="#result == null")
    public InventoryStatus getInventoryStatusById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventory-status/{id}");
/**
        ResponseBodyWrapper<InventoryStatus> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<InventoryStatus>>() {}).getBody();

        return responseBodyWrapper.getData();
**/

        return restTemplateProxy.exchange(
                InventoryStatus.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    @Cacheable(cacheNames = "WorkOrderService_InventoryStatus", unless="#result == null")
    public InventoryStatus getInventoryStatusByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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

    public List<Inventory> findInventoryByLPN(Long warehouseId, String lpn) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("lpn", lpn);
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

    public List<Inventory> findDeliveredInventory(Long warehouseId,
                                                  Long productionLocationId, String pickIds) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("locationId", productionLocationId)
                        .queryParam("pickIds", pickIds);
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
    public List<Inventory> getProducedByProduct(Long warehouseId, String workOrderByProductIds, String lpn) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("workOrderByProductIds", workOrderByProductIds);
        if (Strings.isNotBlank(lpn)) {
            builder = builder.queryParam("lpn", lpn);
        }
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
    public List<Inventory>  getReturnedInventory(Long warehouseId, String workOrderLineIds) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("workOrderLineIds", workOrderLineIds);
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

    public Inventory reverseProduction(Long inventoryId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/reverse-production/{id}");
/**
        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(inventoryId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                Inventory.class,
                builder.buildAndExpand(inventoryId).toUriString(),
                HttpMethod.POST,
                null
        );



    }


    public Inventory reverseByProduct(Long inventoryId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/reverse-by-product/{id}");
/**
        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(inventoryId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Inventory.class,
                builder.buildAndExpand(inventoryId).toUriString(),
                HttpMethod.POST,
                null
        );


    }

    public List<Inventory> findProducedInventoryByLPN(Long warehouseId, Long workOrderId,
                                                      String lpn)   {

        UriComponentsBuilder builder =
                null;
        try {
            builder = UriComponentsBuilder.newInstance()
                    .scheme("http").host("zuulserver").port(5555)
                    .path("/api/inventory/inventories")
                    .queryParam("warehouseId", warehouseId)
                    .queryParam("workOrderId", workOrderId)
                    .queryParam("lpn", URLEncoder.encode(lpn, "UTF-8"));

            logger.debug("Start to find produced inventory by work order id {}",
                    workOrderId);
            /**
            ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.build(true).toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

            logger.debug("Returned");
            List<Inventory> inventories = responseBodyWrapper.getData();
            logger.debug("got {} inventory record produced by work order",
                    inventories.size());
            return inventories;
             **/
            return restTemplateProxy.exchangeList(
                    Inventory.class,
                    builder.build(true).toUriString(),
                    HttpMethod.GET,
                    null
            );


        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
            throw ResourceNotFoundException.raiseException("can't find the produced inventory  by work order id " +
                   workOrderId + ",  lpn: " + lpn);

        }

    }
    public List<Inventory> findProducedInventory(Long warehouseId, Long workOrderId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("workOrderId", workOrderId);
/**
        logger.debug("Start to find produced inventory by work order id {}",
                workOrderId);
        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        logger.debug("Returned");
        List<Inventory> inventories = responseBodyWrapper.getData();
        logger.debug("got {} inventory record produced by work order",
                inventories.size());
        return inventories;
 **/

        return restTemplateProxy.exchangeList(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }

    public List<Inventory> findInventoryByLocation(Long warehouseId, Long locationId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("locationId", locationId);
/**
        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        List<Inventory> inventories = responseBodyWrapper.getData();
        return inventories;
 **/

        return restTemplateProxy.exchangeList(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

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

        /**
        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = null;
        try {
            responseBodyWrapper = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.PUT,
                    getHttpEntity(objectMapper.writeValueAsString(inventory)),
                    new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw WorkOrderException.raiseException("Can't add inventory due to JsonProcessingException: " + e.getMessage());
        }

        return responseBodyWrapper.getData();
         **/

        return restTemplateProxy.exchange(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.PUT,
                inventory
        );
    }

    /**
     * Return material from teh work order line. The inventory structure should already
     * have the work order line.id setup
     * @param inventory
     * @return
     */
    public Inventory receiveInventoryFromWorkOrderLine(Inventory inventory) {

        logger.debug("Start to receive material by lpn {}, work order line id {}",
                inventory.getLpn(), inventory.getWorkOrderLineId());
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/receive")
                        .queryParam("documentNumber", inventory.getWorkOrderLineId());
/**
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
 **/
        return restTemplateProxy.exchange(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.PUT,
                inventory
        );


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
/**
        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchangeList(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );

    }
    public List<Inventory> consumeMaterialForWorkOrderLine(Long workOrderLineId, Long warehouseId,
                                                           Long quantity, Long inboundLocationId,
                                                           Long inventoryId, String lpn,
                                                           Boolean nonPickedInventory) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories/consume/workOrderLine/{id}")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("quantity", quantity)
                        .queryParam("locationId", inboundLocationId);
        if (Objects.nonNull(inventoryId)) {
            builder = builder.queryParam("inventoryId", inventoryId);
        }
        if (Strings.isNotBlank(lpn)) {
            builder = builder.queryParam("lpn", lpn);
        }
        if (Objects.nonNull(nonPickedInventory)) {
            builder = builder.queryParam("nonPickedInventory", nonPickedInventory);
        }

        /**
        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(workOrderLineId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();
         **/
        return restTemplateProxy.exchangeList(
                Inventory.class,
                builder.buildAndExpand(workOrderLineId).toUriString(),
                HttpMethod.POST,
                null
        );

    }

    public List<Inventory> getPickableInventory(Long itemId, Long inventoryStatusId, Long locationId, String lpn) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories/pickable")
                        .queryParam("includeDetails", false)
                        .queryParam("itemId", itemId)
                        .queryParam("inventoryStatusId", inventoryStatusId);

        if (Objects.nonNull(locationId)) {
            builder = builder.queryParam("locationId", locationId);
        }

        if (Strings.isNotBlank(lpn)) {
            builder = builder.queryParam("lpn", lpn);

        }
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

    public Item getLastItemFromSiloLocation(Long warehouseId, String siloDeviceName) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/items/last-item-from-silo-location")
                        .queryParam("includeDetails", false)
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("locationName", siloDeviceName);
/**
        ResponseBodyWrapper<Item> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Item>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                Item.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }
    public InventoryStatus getAvailableInventoryStatus(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventory-statuses/available")
                        .queryParam("warehouseId", warehouseId);

        return restTemplateProxy.exchange(
                InventoryStatus.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public Item createKitItem(Long warehouseId, Long id, Long billOfMaterialId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/items/{id}/create-kit-item")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("billOfMaterialId", billOfMaterialId);


        return restTemplateProxy.exchange(
                Item.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.PUT,
                null
        );
    }
}
