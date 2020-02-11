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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class InventoryServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceRestemplateClient.class);
    private ObjectMapper mapper = new ObjectMapper();
    @Autowired
    OAuth2RestTemplate restTemplate;

    public Item getItemById(Long id) {

        ResponseBodyWrapper<Item> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/inventory/item/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Item>>() {}, id).getBody();

        return responseBodyWrapper.getData();

    }

    public Item getItemByName(Long warehouseId, String name) {
        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/inventory/items?")
                .append("name={name}")
                .append("&warehouseId={warehouseId}");

        ResponseBodyWrapper<List<Item>> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Item>>>() {},
                name, warehouseId).getBody();

        List<Item> items = responseBodyWrapper.getData();
        if (items.size() == 0) {
            return null;
        }
        else {
            return items.get(0);
        }
    }


    public ItemFamily getItemFamilyById(Long id) {

        ResponseBodyWrapper<ItemFamily> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/inventory/item-family/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<ItemFamily>>() {}, id).getBody();

        return responseBodyWrapper.getData();

    }

    public ItemFamily getItemFamilyByName(Long warehouseId, String name) {

        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/inventory/item-families?")
                .append("name={name}")
                .append("&warehouseId={warehouseId}");
        ResponseBodyWrapper<List<ItemFamily>> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<ItemFamily>>>() {},
                name, warehouseId).getBody();

        List<ItemFamily> itemFamilies = responseBodyWrapper.getData();
        if (itemFamilies.size() == 0) {
            return null;
        }
        else {
            return itemFamilies.get(0);
        }
    }


    public InventoryStatus getInventoryStatusById(Long id) {

        ResponseBodyWrapper<InventoryStatus> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/inventory/inventory-status/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<InventoryStatus>>() {}, id).getBody();

        return responseBodyWrapper.getData();

    }

    public InventoryStatus getInventoryStatusByName(Long warehouseId, String name) {

        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/inventory/inventory-statuses?")
                .append("name={name}")
                .append("&warehouseId={warehouseId}");

        ResponseBodyWrapper<List<InventoryStatus>> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<InventoryStatus>>>() {},
                name, warehouseId).getBody();

        List<InventoryStatus> inventoryStatuses = responseBodyWrapper.getData();
        if (inventoryStatuses.size() == 0) {
            return null;
        }
        else {
            return inventoryStatuses.get(0);
        }
    }

    public List<Inventory> getPickableInventory(Long itemId, Long inventoryStatusId) {
        StringBuilder url = new StringBuilder()
                                .append("http://zuulserver:5555/api/inventory/inventories/pickable?" )
                                .append("itemId={itemId}")
                                .append("&inventoryStatusId={inventoryStatusId}");

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper = restTemplate.exchange(url.toString(),
                HttpMethod.GET, null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {},
                itemId, inventoryStatusId).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Inventory> getInventoryByLocation(Location location) {

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/inventory/inventories?location={location}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}, location.getName()).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Inventory> getPendingInventoryByLocation(Location location) {

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/inventory/inventories/pending?locationId={locationId}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}, location.getId()).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Inventory> getPickedInventory(Long warehouseId, List<Pick> picks) {
        // Convert a list of picks into a list of pick ids and join them into a single string with comma
        // Then we can call the inventory service endpoint to get all the picked inventory with those picks
        String pickIds =  picks.stream().map(Pick::getId).map(String::valueOf).collect(Collectors.joining(","));

        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/inventory/inventories?")
                .append("pickIds={pickIds}")
                .append("&warehouseId={warehouseId}");

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {},
                pickIds, warehouseId).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Inventory> getInventoryByLocationGroup(Item item, InventoryStatus inventoryStatus, LocationGroup locationGroup) {
        StringBuilder url = new StringBuilder()
                                .append("http://zuulserver:5555/api/inventory/inventories?")
                                .append("itemName={itemName}")
                                .append("&inventory_status_id={inventoryStatusId}")
                                .append("&location_group_id={locationGroupId}");

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {},
                item.getName(),
                inventoryStatus.getId(),
                locationGroup.getId()).getBody();

        return responseBodyWrapper.getData();
    }



    public List<MovementPath> getPickMovementPath(Pick pick) {
        return getPickMovementPath(pick.getWarehouseId(), pick.getSourceLocation(), pick.getDestinationLocation());
    }

    public List<MovementPath> getPickMovementPath(Long warehouseId, Location sourceLocation, Location destinationLocation) {

        StringBuilder url = new StringBuilder();
        url.append("http://zuulserver:5555/api/inventory/movement-path/match?")
                .append("warehouseId={warehouseId}")
                .append("&from_location_id={fromLocationId}")
                .append("&from_location={fromLocationName}")
                .append("&from_location_group_id={fromLocationGroupId}")
                .append("&to_location_id={toLocationId}")
                .append("&to_location={toLocationName}")
                .append("&to_location_group_id={toLocationGroupId}");
        ResponseBodyWrapper<List<MovementPath>> responseBodyWrapper = restTemplate.exchange(url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<MovementPath>>>() {},
                warehouseId,
                sourceLocation.getId(),
                sourceLocation.getName(),
                sourceLocation.getLocationGroup().getId(),
                destinationLocation.getId(),
                destinationLocation.getName(),
                destinationLocation.getLocationGroup().getId()).getBody();

        return responseBodyWrapper.getData();
    }

    public Inventory markAsPicked(Inventory inventory, Pick pick) {

        ResponseBodyWrapper<Inventory> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/inventory/inventory/{id}/picked?pickId={pickId}",
                HttpMethod.POST, null, new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {},
                inventory.getId(), pick.getId()).getBody();

        return responseBodyWrapper.getData();
    }
    public List<Inventory> split(Inventory inventory, String newLpn, Long newQuantity) {

        StringBuilder url = new StringBuilder();
        url.append("http://zuulserver:5555/api/inventory/inventory/{id}/split?")
                .append("newLpn={newLpn}")
                .append("&newQuantity={newQuantity}");

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper = restTemplate.exchange(url.toString(),
                HttpMethod.POST, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {},
                inventory.getId(), newLpn, newQuantity).getBody();

        return responseBodyWrapper.getData();
    }

    public Inventory moveInventory(Inventory inventory, Pick pick, Location nextLocation) throws IOException {

        logger.debug("start to move inventory {} to location {} for the pick {}",
                inventory.getLpn(), nextLocation.getName(),  pick.getNumber());
        String requestBody = mapper.writeValueAsString(nextLocation);

        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        HttpEntity<String> httpEntity = new HttpEntity<String>(requestBody, headers);

        ResponseBodyWrapper<Inventory> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/inventory/inventory/{id}/move?pickId={pickId}",
                HttpMethod.POST, httpEntity, new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {},
                inventory.getId(), pick.getId()).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Inventory> getInventoryForPick(Pick pick) {

        StringBuilder url = new StringBuilder();
        url.append("http://zuulserver:5555/api/inventory/inventories?")
                .append("itemName={itemName}")
                .append("&location={locationName}")
                .append("&warehouseId={warehouseId}");

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper = restTemplate.exchange(url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {},
                pick.getItem().getName(),
                pick.getSourceLocation().getName(),
                pick.getWarehouseId()).getBody();

        return responseBodyWrapper.getData();

    }

    public List<Inventory> unpick(String lpn) {

        StringBuilder url = new StringBuilder();
        url.append("http://zuulserver:5555/api/inventory/inventories/unpick?")
                .append("lpn={lpn}");

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper = restTemplate.exchange(url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {},
                lpn).getBody();

        return responseBodyWrapper.getData();

    }
    public Inventory unpick(Inventory inventory) {

        StringBuilder url = new StringBuilder();
        url.append("http://zuulserver:5555/api/inventory/inventory/{id}/unpick");

        ResponseBodyWrapper<Inventory> responseBodyWrapper = restTemplate.exchange(url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {},
                inventory.getId()).getBody();

        return responseBodyWrapper.getData();

    }

    public Inventory moveInventory(Inventory inventory, Location nextLocation) throws IOException {

        String requestBody = mapper.writeValueAsString(nextLocation);

        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        HttpEntity<String> httpEntity = new HttpEntity<String>(requestBody, headers);

        ResponseBodyWrapper<Inventory> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/inventory/inventory/{id}/move",
                HttpMethod.POST, httpEntity, new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {},
                inventory.getId()).getBody();

        return responseBodyWrapper.getData();
    }




}
