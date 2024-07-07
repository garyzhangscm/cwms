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

package com.garyzhangscm.cwms.inbound.clients;

import com.garyzhangscm.cwms.inbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inbound.model.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class InventoryServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    @Cacheable(cacheNames = "InboundService_Item", unless="#result == null")
    public Item getItemById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/items/{id}");

/**
        ResponseBodyWrapper<Item> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
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

    public ItemPackageType getItemPackageTypeById(Long id) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/itemPackageTypes/{id}");
        return restTemplateProxy.exchange(
                ItemPackageType.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    @Cacheable(cacheNames = "InboundService_Item", unless="#result == null" )
    public Item getItemByName(Long warehouseId, Long clientId, String name) {
        logger.debug("Start to get item by name {} / {}",
                warehouseId, name);

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
/**
            ResponseBodyWrapper<List<Item>> responseBodyWrapper
                    = restTemplateProxy.getRestTemplate().exchange(
                    builder.build(true).toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<Item>>>() {}).getBody();

            List<Item> items = responseBodyWrapper.getData();
 **/
            List<Item> items = restTemplateProxy.exchangeList(
                    Item.class,
                    builder.build(true).toUriString(),
                    HttpMethod.GET,
                    null
            );


            logger.debug(">> get {} item", items.size());

            if (Objects.isNull(items) || items.size() == 0) {
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

    public Item createItem(Item item) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/items");


        return restTemplateProxy.exchange(
                Item.class,
                builder.toUriString(),
                HttpMethod.POST,
                item
        );


    }

    public Item changeItem(Item item) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/items/{id}");


        return restTemplateProxy.exchange(
                Item.class,
                builder.buildAndExpand(item.getId()).toUriString(),
                HttpMethod.PUT,
                item
        );


    }


    @Cacheable(cacheNames = "InboundService_ItemFamily", unless="#result == null")
    public ItemFamily getItemFamilyById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/item-family/{id}");
/**
        ResponseBodyWrapper<ItemFamily> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                    null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<ItemFamily>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                ItemFamily.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );


    }

    @Cacheable(cacheNames = "InboundService_ItemFamily", unless="#result == null")
    public ItemFamily getItemFamilyByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/item-families")
                        .queryParam("name", name)
                        .queryParam("warehouseId", warehouseId);

/**
        ResponseBodyWrapper<List<ItemFamily>> responseBodyWrapper = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<ItemFamily>>>() {}).getBody();

        List<ItemFamily> itemFamilies = responseBodyWrapper.getData();
 **/
        List<ItemFamily> itemFamilies = restTemplateProxy.exchangeList(
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


    @Cacheable(cacheNames = "InboundService_InventoryStatus", unless="#result == null")
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

    @Cacheable(cacheNames = "InboundService_InventoryStatus", unless="#result == null")
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

        List<InventoryStatus> inventoryStatuses = restTemplateProxy.exchangeList(
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

    public InventoryStatus getAvailableInventoryStatus(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventory-statuses/available")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<InventoryStatus> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
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
/**
    public Inventory receiveInventory(Inventory inventory) {
        return receiveInventory(inventory, "");

    }
 **/
    public Inventory receiveInventory(Inventory inventory, String documentNumber) {

        // Convert the inventory to JSON and send to the inventory service


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/receive");
        if (Strings.isNotBlank(documentNumber)) {
            builder = builder.queryParam("documentNumber", documentNumber);
        }

        /**
        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = null;
        try {
            responseBodyWrapper = restTemplateProxy.getRestTemplate().exchange(
                    builder.toUriString(),
                    HttpMethod.PUT,
                    getHttpEntity(objectMapper.writeValueAsString(inventory)),
                    new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();
        } catch (JsonProcessingException e) {
            throw ReceiptOperationException.raiseException("Can't add inventory due to JsonProcessingException: " + e.getMessage());
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

    public List<Inventory> findInventoryByReceipt(
            Long warehouseId, Long receiptId,
            String inventoryIds,
            Boolean notPutawayInventoryOnly) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("receiptId", receiptId)
                        .queryParam("warehouseId", warehouseId);

        if (Objects.nonNull(inventoryIds)) {
            builder.queryParam("inventoryIds", inventoryIds);
        }
        if (Objects.nonNull(notPutawayInventoryOnly)) {
            builder.queryParam("notPutawayInventoryOnly", notPutawayInventoryOnly);
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

    public List<Inventory> findInventoryByReceipts(
            Long warehouseId, String receiptIds) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("receiptIds", receiptIds)
                        .queryParam("warehouseId", warehouseId);

        return restTemplateProxy.exchangeList(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );


    }

    public List<Inventory> findInventoryByCustomerReturnOrder(
            Long warehouseId, Long customerReturnOrderId,
            String inventoryIds,
            Boolean notPutawayInventoryOnly) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("customerReturnOrderId", customerReturnOrderId)
                        .queryParam("warehouseId", warehouseId);

        if (Objects.nonNull(inventoryIds)) {
            builder.queryParam("inventoryIds", inventoryIds);
        }
        if (Objects.nonNull(notPutawayInventoryOnly)) {
            builder.queryParam("notPutawayInventoryOnly", notPutawayInventoryOnly);
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

    public List<Inventory> findInventoryByItem(Long warehouseId, Item item) {
        UriComponentsBuilder builder =
                null;
        try {
            builder = UriComponentsBuilder.newInstance()
                    .scheme("http").host("zuulserver").port(5555)
                    .path("/api/inventory/inventories")
                    .queryParam("itemName", URLEncoder.encode(item.getName(), "UTF-8"))
            .queryParam("warehouseId", warehouseId);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new ArrayList<>();
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

    public Inventory setupMovementPath(long inventoryId, List<InventoryMovement> inventoryMovements) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventory/{id}/movements");
/**
        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = null;
        try {
            responseBodyWrapper = restTemplate.exchange(
                    builder.buildAndExpand(inventoryId).toUriString(),
                    HttpMethod.POST,
                    getHttpEntity(objectMapper.writeValueAsString(inventoryMovements)),
                    new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();
        } catch (JsonProcessingException e) {
            throw ReceiptOperationException.raiseException("Can't setup the movement path due to JsonProcessingException: " + e.getMessage());
        }

        Inventory inventory = responseBodyWrapper.getData();
        logger.debug("setupMovementPath returns {}", inventory.getInventoryMovements());
        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Inventory.class,
                builder.buildAndExpand(inventoryId).toUriString(),
                HttpMethod.POST,
                inventoryMovements
        );
    }

    public Inventory moveInventory(Inventory inventory, Location nextLocation) throws IOException {
        return moveInventory(inventory, nextLocation, "");
    }
    public Inventory moveInventory(Inventory inventory, Location nextLocation, String destinationLpn) throws IOException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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


    public String validateNewLPN(Long warehouseId, String lpn) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories/validate-new-lpn")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("lpn", lpn);
/**
        ResponseBodyWrapper<String> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
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

    public Inventory clearMovementPath(long inventoryId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventory/{id}/movements");
/**
        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = restTemplate.exchange(
                    builder.buildAndExpand(inventoryId).toUriString(),
                    HttpMethod.DELETE,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();


        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Inventory.class,
                builder.buildAndExpand(inventoryId).toUriString(),
                HttpMethod.DELETE,
                null
        );
    }
    public String removeInventoryAtLocation(Long warehouseId, Long locationId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("locationId", locationId);


        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.DELETE,
                null
        );

    }
    public String removeInventoryAtLocation(Long warehouseId, String locationName) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("location", locationName);


        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.DELETE,
                null
        );

    }
    public String removeInventoryByLpn(Long warehouseId, String lpn) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("lpn", lpn);


        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.DELETE,
                null
        );

    }

    public List<Inventory> findInventoryByLPN(Long warehouseId, String lpn) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("lpn", lpn);

        return restTemplateProxy.exchangeList(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );


    }

}
