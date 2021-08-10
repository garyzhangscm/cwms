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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.outbound.model.*;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
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
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class InventoryServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceRestemplateClient.class);


    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;
    // private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;

    @Cacheable(cacheNames = "item")
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


        ResponseBodyWrapper<List<Item>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Item>>>() {}).getBody();

        List<Item> items = responseBodyWrapper.getData();
        if (items.size() == 0) {
            return null;
        }
        else {
            return items.get(0);
        }
    }


    public ItemFamily getItemFamilyById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/item-family/{id}");
        ResponseBodyWrapper<ItemFamily> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<ItemFamily>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public ItemFamily getItemFamilyByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/item-families")
                        .queryParam("name", name)
                        .queryParam("warehouseId", warehouseId);

        ResponseBodyWrapper<List<ItemFamily>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<ItemFamily>>>() {}).getBody();

        List<ItemFamily> itemFamilies = responseBodyWrapper.getData();
        if (itemFamilies.size() == 0) {
            return null;
        }
        else {
            return itemFamilies.get(0);
        }
    }


    @Cacheable(cacheNames = "inventory-status")
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

    public List<Inventory> getPickableInventory(Long itemId, Long inventoryStatusId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories/pickable")
                        .queryParam("itemId", itemId)
                        .queryParam("inventoryStatusId", inventoryStatusId);

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Inventory> getInventoryByLocationAndItemName(Location location, String itemName) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("locationId", location.getId())
                        .queryParam("warehouseId", location.getWarehouse().getId())
                        .queryParam("itemName", itemName);

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public List<Inventory> getInventoryByLpn(Long warehouseId, String lpn) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("lpn", lpn)
                        .queryParam("warehouseId", warehouseId);

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public List<Inventory> getInventoryByLocation(Location location) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("locationId", location.getId())
                        .queryParam("warehouseId", location.getWarehouse().getId());

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Inventory> getPendingInventoryByLocation(Location location) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories/pending")
                        .queryParam("locationId", location.getId());

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Inventory> getPickedInventory(Long warehouseId, List<Pick> picks) {
        // Convert a list of picks into a list of pick ids and join them into a single string with comma
        // Then we can call the inventory service endpoint to get all the picked inventory with those picks
        String pickIds =  picks.stream().map(Pick::getId).map(String::valueOf).collect(Collectors.joining(","));

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("pickIds", pickIds)
                        .queryParam("warehouseId", warehouseId);

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Inventory> getInventoryByLocationGroup(Long warehouseId, Item item, Long inventoryStatusId, LocationGroup locationGroup) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("itemName", item.getName())
                        .queryParam("inventoryStatusId", inventoryStatusId)
                        .queryParam("locationGroupId", locationGroup.getId());

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }



    public List<MovementPath> getPickMovementPath(Pick pick) {
        return getPickMovementPath(pick.getWarehouseId(), pick.getSourceLocation(), pick.getDestinationLocation());
    }

    @Cacheable(cacheNames = "movement-path")
    public List<MovementPath> getPickMovementPath(Long warehouseId, Location sourceLocation, Location destinationLocation) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/movement-path/match")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("fromLocationId", sourceLocation.getId())
                        .queryParam("fromLocation", sourceLocation.getName())
                        .queryParam("fromLocationGroupId", sourceLocation.getLocationGroup().getId())
                        .queryParam("toLocationId", destinationLocation.getId())
                        .queryParam("toLocation", destinationLocation.getName())
                        .queryParam("toLocationGroupId", destinationLocation.getLocationGroup().getId());

        ResponseBodyWrapper<List<MovementPath>> responseBodyWrapper =
                restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<MovementPath>>>() {}).getBody();

        return responseBodyWrapper.getData();
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
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventory/{id}/split")
                        .queryParam("newLpn", newLpn)
                        .queryParam("newQuantity", newQuantity);

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(inventory.getId()).toUriString(),
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public Inventory moveInventory(Inventory inventory, Pick pick, Location nextLocation )   {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventory/{id}/move")
                        .queryParam("pickId", pick.getId());


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
    }


    public List<Inventory> getInventoryForPick(Pick pick) {
        return getInventoryForPick(pick, "");
    }
    public List<Inventory> getInventoryForPick(Pick pick, String lpn) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("itemName", pick.getItem().getName())
                        .queryParam("location", pick.getSourceLocation().getName())
                        .queryParam("warehouseId", pick.getWarehouseId());
        if (Strings.isNotBlank(lpn)) {
            builder.queryParam("lpn", lpn);
        }

        // If this is a allocated by LPN, then only pick the specific LPN
        if (StringUtils.isNotBlank(pick.getLpn())) {
            builder = builder.queryParam("lpn", pick.getLpn());
        }


        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();

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

        ResponseBodyWrapper<Inventory> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(inventory.getId()).toUriString(),
                        HttpMethod.POST,
                        getHttpEntity(objectMapper.writeValueAsString(nextLocation)),
                        new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Inventory> markLPNAllocated(Long warehouseId, String lpn, Long allocatedByPickId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventory/mark-lpn-allocated")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("lpn", lpn)
                        .queryParam("allocatedByPickId", allocatedByPickId);

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public List<Inventory>  releaseLPNAllocated(Long warehouseId,
                                                String lpn, Long allocatedByPickId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/inventory/inventory/release-lpn-allocated")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("lpn", lpn)
                        .queryParam("allocatedByPickId", allocatedByPickId);

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
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
