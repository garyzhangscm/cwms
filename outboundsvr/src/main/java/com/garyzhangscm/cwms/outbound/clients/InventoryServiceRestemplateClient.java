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

import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class InventoryServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceRestemplateClient.class);


    @Autowired
    private RestTemplateProxy restTemplateProxy;

    @Cacheable(cacheNames = "OutboundService_Item", unless="#result == null")
    public Item getItemById(Long id) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
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
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/itemPackageTypes/{id}");
        return restTemplateProxy.exchange(
                ItemPackageType.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    @Cacheable(cacheNames = "OutboundService_Item", unless="#result == null")
     public Item getItemByName(Long warehouseId, Long clientId,  String name) {

        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("apigateway").port(5555)
                            .path("/api/inventory/items")
                            .queryParam("name", URLEncoder.encode(name, "UTF-8"))
                            // .queryParam("name", name)
                            .queryParam("warehouseId", warehouseId);

            logger.debug("start to query item with name {}",
                    URLEncoder.encode(name, "UTF-8"));

            if (Objects.nonNull(clientId)) {
                builder = builder.queryParam("clientIds", String.valueOf(clientId));
            }
/**
            // logger.debug("Start to get item: {} / {}", name, warehouseId);
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
                    // builder.build().toUriString(),
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


    @Cacheable(cacheNames = "OutboundService_ItemFamily", unless="#result == null")
    public ItemFamily getItemFamilyById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
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

    @Cacheable(cacheNames = "OutboundService_ItemFamily", unless="#result == null")
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


    @Cacheable(cacheNames = "OutboundService_InventoryStatus", unless="#result == null")
    public InventoryStatus getInventoryStatusById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory-status/{id}");
/**
        ResponseBodyWrapper<InventoryStatus> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
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

    @Cacheable(cacheNames = "OutboundService_InventoryStatus", unless="#result == null")
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

    public List<Inventory> getPickableInventory(Long itemId, Long inventoryStatusId, Long locationId,
                                                String color, String productSize,
                                                String style,
                                                String inventoryAttribute1,
                                                String inventoryAttribute2,
                                                String inventoryAttribute3,
                                                String inventoryAttribute4,
                                                String inventoryAttribute5,
                                                String allocateByReceiptNumber,
                                                String skipLocationIds)  {
        return getPickableInventory(itemId, inventoryStatusId, locationId, "",
                color, productSize, style, inventoryAttribute1,
                inventoryAttribute2, inventoryAttribute3, inventoryAttribute4,
                inventoryAttribute5, allocateByReceiptNumber, skipLocationIds);
    }
    public List<Inventory> getPickableInventory(Long itemId, Long inventoryStatusId,
                                                Long locationId, String lpn,
                                                String color, String productSize,
                                                String style,
                                                String inventoryAttribute1,
                                                String inventoryAttribute2,
                                                String inventoryAttribute3,
                                                String inventoryAttribute4,
                                                String inventoryAttribute5,
                                                String allocateByReceiptNumber,
                                                String skipLocationIds)  {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories/pickable")
                        .queryParam("includeDetails", false)
                        .queryParam("itemId", itemId)
                        .queryParam("inventoryStatusId", inventoryStatusId)
                        .queryParam("lpnLimit", 500);

        if (Objects.nonNull(locationId)) {
            builder = builder.queryParam("locationId", locationId);
        }
        if (Strings.isNotBlank(lpn)) {
            builder = builder.queryParam("lpn", lpn);
        }
        if (Strings.isNotBlank(skipLocationIds)) {
            builder = builder.queryParam("skipLocationIds", skipLocationIds);

        }
        try {
            if (Strings.isNotBlank(color)) {
                builder = builder.queryParam("color", URLEncoder.encode(color, "UTF-8"));
            }
            if (Strings.isNotBlank(productSize)) {
                builder = builder.queryParam("productSize", URLEncoder.encode(productSize, "UTF-8"));
            }
            if (Strings.isNotBlank(style)) {
                builder = builder.queryParam("style", URLEncoder.encode(style, "UTF-8"));
            }
            if (Strings.isNotBlank(inventoryAttribute1)) {
                builder = builder.queryParam("attribute1", URLEncoder.encode(inventoryAttribute1, "UTF-8"));
            }
            if (Strings.isNotBlank(inventoryAttribute2)) {
                builder = builder.queryParam("attribute2", URLEncoder.encode(inventoryAttribute2, "UTF-8"));
            }
            if (Strings.isNotBlank(inventoryAttribute3)) {
                builder = builder.queryParam("attribute3", URLEncoder.encode(inventoryAttribute3, "UTF-8"));
            }
            if (Strings.isNotBlank(inventoryAttribute4)) {
                builder = builder.queryParam("attribute4", URLEncoder.encode(inventoryAttribute4, "UTF-8"));
            }
            if (Strings.isNotBlank(inventoryAttribute5)) {
                builder = builder.queryParam("attribute5", URLEncoder.encode(inventoryAttribute5, "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw OrderOperationException.raiseException("Can't get the pickable inventory. Fail to construct the query for inventory");
        }
        if (Strings.isNotBlank(allocateByReceiptNumber)) {
            builder = builder.queryParam("receiptNumber", allocateByReceiptNumber);
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

        List<Inventory> pickableInventory = restTemplateProxy.exchangeList(
                Inventory.class,
                builder.build(true).toUriString(),
                HttpMethod.GET,
                null
        );

        logger.debug("return {} pickable inventory with criteria ",
                pickableInventory.size());
        logger.debug("itemId = {}", itemId);
        logger.debug("inventoryStatusId = {}", inventoryStatusId);
        logger.debug("lpn = {}", Strings.isBlank(lpn) ? "N/A" : lpn);
        logger.debug("productSize = {}", Strings.isBlank(productSize) ? "N/A" : productSize);
        logger.debug("style = {}", Strings.isBlank(style) ? "N/A" : style);
        logger.debug("color = {}", Strings.isBlank(color) ? "N/A" : color);
        logger.debug("productSize = {}", Strings.isBlank(productSize) ? "N/A" : productSize);
        logger.debug("inventoryAttribute1 = {}", Strings.isBlank(inventoryAttribute1) ? "N/A" : inventoryAttribute1);
        logger.debug("inventoryAttribute2 = {}", Strings.isBlank(inventoryAttribute2) ? "N/A" : inventoryAttribute2);
        logger.debug("inventoryAttribute3 = {}", Strings.isBlank(inventoryAttribute3) ? "N/A" : inventoryAttribute3);
        logger.debug("inventoryAttribute4 = {}", Strings.isBlank(inventoryAttribute4) ? "N/A" : inventoryAttribute4);
        logger.debug("inventoryAttribute5 = {}", Strings.isBlank(inventoryAttribute5) ? "N/A" : inventoryAttribute5);
        logger.debug("locationId = {}", Objects.isNull(locationId) ? "N/A" : locationId);
        logger.debug("=========   Pickable   Inventory   ===============");
        pickableInventory.forEach(
                inventory -> logger.debug(">> id: {}, LPN : {}", inventory.getId(), inventory.getLpn())
        );
        return pickableInventory;

    }

    public List<Inventory> getPickableInventory(Long itemId, Long inventoryStatusId, Long locationId, String lpn) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
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
        return restTemplateProxy.exchangeList(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }


    public List<Inventory> getInventoryByLocationAndItemName(Location location, String itemName) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("locationId", location.getId())
                        .queryParam("warehouseId", location.getWarehouse().getId())
                        .queryParam("itemName", itemName);
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
    public List<Inventory> getInventoryByLpn(Long warehouseId, String lpn) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("lpn", lpn)
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
    public List<Inventory> getInventoryByLocation(Location location) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("locationId", location.getId())
                        .queryParam("warehouseId", location.getWarehouse().getId());
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

    public List<Inventory> getPendingInventoryByLocation(Location location) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories/pending")
                        .queryParam("locationId", location.getId());
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

    public List<Inventory> getPickedInventory(Long warehouseId, List<Pick> picks) {
        return getPickedInventory(warehouseId, picks, null);
    }

    public List<Inventory> getPickedInventory(Long warehouseId, List<Pick> picks, Boolean includeVirturalInventory) {

        return getPickedInventory(warehouseId, picks, includeVirturalInventory, null);
    }


    public List<Inventory> getPickedInventory(Long warehouseId, List<Pick> picks, Boolean includeVirturalInventory,
                                              Long locationId) {

        return getPickedInventory(warehouseId, picks, includeVirturalInventory,
                locationId, true);
    }
    public List<Inventory> getPickedInventory(Long warehouseId, List<Pick> picks, Boolean includeVirturalInventory,
                                              Long locationId, Boolean includeDetails) {
        // Convert a list of picks into a list of pick ids and join them into a single string with comma
        // Then we can call the inventory service endpoint to get all the picked inventory with those picks
        String pickIds =  picks.stream().map(Pick::getId).map(String::valueOf).collect(Collectors.joining(","));

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("pickIds", pickIds)
                        .queryParam("warehouseId", warehouseId);
        if (Objects.nonNull(includeVirturalInventory)) {
            builder = builder.queryParam("includeVirturalInventory", includeVirturalInventory);
        }
        if (Objects.nonNull(locationId)) {
            builder = builder.queryParam("locationId", locationId);
        }
        if (Objects.nonNull(includeDetails)) {
            builder = builder.queryParam("includeDetails", includeDetails);

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

    public List<Inventory> getInventoryByLocationGroup(Long warehouseId, Item item, Long inventoryStatusId, LocationGroup locationGroup) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("itemName", item.getName())
                        .queryParam("inventoryStatusId", inventoryStatusId)
                        .queryParam("locationGroupId", locationGroup.getId());
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



    public List<MovementPath> getPickMovementPath(Pick pick) {
        return getPickMovementPath(pick.getWarehouseId(), pick.getSourceLocation(), pick.getDestinationLocation());
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

    public List<Inventory> split(Inventory inventory, Long newQuantity) {
        return split(inventory, "", newQuantity);
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

    public Inventory moveInventory(Inventory inventory, Pick pick, Location nextLocation )   {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory/{id}/move")
                        .queryParam("pickId", pick.getId());


/**
        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = null;
        try {
            responseBodyWrapper = restTemplate.exchange(
                    builder.buildAndExpand(inventory.getId()).toUriString(),
                    HttpMethod.POST,
                    getHttpEntity(objectMapper.writeValueAsString(nextLocation)),
                    new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();
        } catch (JsonProcessingException e) {
            throw RequestValidationFailException.raiseException("Can't move inventory due to JsonProcessingException: " + e.getMessage());
        }

        return responseBodyWrapper.getData();
**/
            return restTemplateProxy.exchange(
                    Inventory.class,
                    builder.buildAndExpand(inventory.getId()).toUriString(),
                    HttpMethod.POST,
                    nextLocation
            );
    }


    public List<Inventory> getInventoryForPick(Pick pick)   {
        return getInventoryForPick(pick, "");
    }
    public List<Inventory> getInventoryForPick(Pick pick, String lpn)   {
        return getInventoryForPick(pick, lpn, true);
    }
    public List<Inventory> getInventoryForPick(Pick pick, String lpn, boolean includeDetails)   {
        try {

            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("apigateway").port(5555)
                            .path("/api/inventory/inventories")
                            .queryParam("itemName",  URLEncoder.encode(pick.getItem().getName(), "UTF-8") )
                            .queryParam("location", URLEncoder.encode(pick.getSourceLocation().getName(), "UTF-8") )
                            .queryParam("maxLPNCount", 2)
                            .queryParam("inventoryStatusId", pick.getInventoryStatusId())
                            .queryParam("warehouseId", pick.getWarehouseId())
                                    .queryParam("includeDetails", includeDetails);
            if (Strings.isNotBlank(lpn)) {
                builder.queryParam("lpn", lpn);
            }

            if (StringUtils.isNotBlank(pick.getColor())) {
                builder = builder.queryParam("color", URLEncoder.encode(pick.getColor(), "UTF-8"));
            }
            if (StringUtils.isNotBlank(pick.getProductSize())) {
                builder = builder.queryParam("productSize", URLEncoder.encode(pick.getProductSize(), "UTF-8"));
            }
            if (StringUtils.isNotBlank(pick.getStyle())) {
                builder = builder.queryParam("style", URLEncoder.encode(pick.getStyle(), "UTF-8"));
            }
            if (StringUtils.isNotBlank(pick.getInventoryAttribute1())) {
                builder = builder.queryParam("attribute1", URLEncoder.encode(pick.getInventoryAttribute1(), "UTF-8"));
            }
            if (StringUtils.isNotBlank(pick.getInventoryAttribute2())) {
                builder = builder.queryParam("attribute2", URLEncoder.encode(pick.getInventoryAttribute2(), "UTF-8"));
            }
            if (StringUtils.isNotBlank(pick.getInventoryAttribute3())) {
                builder = builder.queryParam("attribute3", URLEncoder.encode(pick.getInventoryAttribute3(), "UTF-8"));
            }
            if (StringUtils.isNotBlank(pick.getInventoryAttribute4())) {
                builder = builder.queryParam("attribute4", URLEncoder.encode(pick.getInventoryAttribute4(), "UTF-8"));
            }
            if (StringUtils.isNotBlank(pick.getInventoryAttribute5())) {
                builder = builder.queryParam("attribute5", URLEncoder.encode(pick.getInventoryAttribute5(), "UTF-8"));
            }
            if (StringUtils.isNotBlank(pick.getAllocateByReceiptNumber())) {
                builder = builder.queryParam("receiptNumber", URLEncoder.encode(pick.getAllocateByReceiptNumber(), "UTF-8"));
            }
/**
            ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

            return responseBodyWrapper.getData();
**/
            return restTemplateProxy.exchangeList(
                    Inventory.class,
                    builder.build(true).toUriString(),
                    HttpMethod.GET,
                    null
            );
        }
        catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            throw ResourceNotFoundException.raiseException("can't find the inventory by item name " + pick.getItem().getName() + ", "
                    + " location: " + pick.getSourceLocation().getName() + ", warehouse id: " + pick.getWarehouseId());
        }

    }

    public Inventory shipInventory(Inventory inventory, Location nextLocation) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory/{id}/ship");
        return restTemplateProxy.exchange(
                Inventory.class,
                builder.buildAndExpand(inventory.getId()).toUriString(),
                HttpMethod.POST,
                nextLocation
        );
    }
    public Inventory moveInventory(Inventory inventory, Location nextLocation){
        return moveInventory(inventory, nextLocation, "");
    }
    public Inventory moveInventory(Inventory inventory, Location nextLocation, String destinationLpn)  {
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

    public List<Inventory> markLPNAllocated(Long warehouseId, String lpn, Long allocatedByPickId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory/mark-lpn-allocated")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("lpn", lpn)
                        .queryParam("allocatedByPickId", allocatedByPickId);
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
    public List<Inventory>  releaseLPNAllocated(Long warehouseId,
                                                String lpn, Long allocatedByPickId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory/release-lpn-allocated")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("lpn", lpn)
                        .queryParam("allocatedByPickId", allocatedByPickId);
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

    public InventoryStatus getAvailableInventoryStatus(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
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
     * Generate cycle count on the location
     * @param warehouseId
     * @param locationName
     */
    public void generateCycleCount(Long warehouseId, String locationName) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/cycle-count-requests")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("cycleCountRequestType", CycleCountRequestType.BY_LOCATION_RANGE)
                        .queryParam("beginValue", locationName)
                        .queryParam("endValue", locationName)
                        .queryParam("includeEmptyLocation", true);
/**
        ResponseBodyWrapper<String> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();

        logger.debug("Cycle count is generated: \n {}", responseBodyWrapper.getData());
**/
        restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public List<Inventory> processBulkPick(BulkPick bulkPick, Location nextLocation, String lpn) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories/process-bulk-pick")
                        .queryParam("warehouseId", bulkPick.getWarehouseId())
                        .queryParam("nextLocationId", nextLocation.getId())
                        .queryParam("lpn", lpn);

        /**
        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = null;
        try {
            responseBodyWrapper = restTemplate.exchange(
            builder.toUriString(),
            HttpMethod.POST,
            getHttpEntity(objectMapper.writeValueAsString(bulkPick)),
            new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return responseBodyWrapper.getData();
         **/

        return restTemplateProxy.exchangeList(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.POST,
                bulkPick
        );
    }

    public List<Inventory> relabelInventory(Long warehouseId, String lpn, String newLPN, boolean mergeWithExistingInventory)   {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories/relabel-lpn")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("lpn", lpn)
                        .queryParam("newLPN", newLPN)
                        .queryParam("mergeWithExistingInventory", mergeWithExistingInventory);

        return restTemplateProxy.exchangeList(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.POST,
                null);
    }

    public Inventory relabelInventory(Long warehouseId, Long inventoryId, String newLPN, boolean mergeWithExistingInventory)   {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories/{id}/relabel")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("newLPN", newLPN)
                        .queryParam("mergeWithExistingInventory", mergeWithExistingInventory);

        return restTemplateProxy.exchange(
                Inventory.class,
                builder.buildAndExpand(inventoryId).toUriString(),
                HttpMethod.POST,
                null
        );
    }


    public InventoryConfiguration getInventoryConfiguration(Long warehouseId)   {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory_configuration")
                        .queryParam("warehouseId", warehouseId);

        return restTemplateProxy.exchange(
                InventoryConfiguration.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

}
