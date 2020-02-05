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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.inbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inbound.model.*;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

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

    public Inventory addInventory(Inventory inventory) throws IOException {

        // Convert the inventory to JSON and send to the inventory service

        logger.debug("start to addInventory");
        String requestBody = mapper.writeValueAsString(inventory);
        logger.debug("add inventory: {}", requestBody);
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        HttpEntity<String> httpEntity = new HttpEntity<String>(requestBody, headers);

        ResponseBodyWrapper<Inventory> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/inventory/inventories",
                HttpMethod.POST, httpEntity, new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Inventory> findInventoryByReceipt(Long warehouseId, Long receiptId) {
        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/inventory/inventories?")
                .append("receipt_id={receiptId}")
                .append("&warehouseId={warehouseId}");

        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {},
                receiptId, warehouseId).getBody();

        return responseBodyWrapper.getData();
    }

    public List<Inventory> findInventoryByItem(Item item) {
        ResponseBodyWrapper<List<Inventory>> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/inventory/inventories?itemName={itemName}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Inventory>>>() {}, item.getName()).getBody();

        return responseBodyWrapper.getData();

    }

    public Inventory setupMovementPath(long inventoryId, List<InventoryMovement> inventoryMovements) throws IOException {

        String requestBody = mapper.writeValueAsString(inventoryMovements);
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        HttpEntity<String> httpEntity = new HttpEntity<String>(requestBody, headers);
        logger.debug("start to call inventory / movements with payload:\n{}", requestBody);

        ResponseBodyWrapper<Inventory> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/inventory/inventory/{id}/movements",
                HttpMethod.POST, httpEntity, new ParameterizedTypeReference<ResponseBodyWrapper<Inventory>>() {}, inventoryId).getBody();

        Inventory inventory = responseBodyWrapper.getData();
        logger.debug("setupMovementPath returns {}", inventory.getInventoryMovements());
        return responseBodyWrapper.getData();

    }








}
