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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.ResponseBodyWrapper;
import com.garyzhangscm.cwms.workorder.model.InventoryStatus;
import com.garyzhangscm.cwms.workorder.model.Item;
import com.garyzhangscm.cwms.workorder.model.ItemFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceRestemplateClient.class);

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


        logger.debug("Start to get item: {} / {}", name, warehouseId);
        ResponseBodyWrapper<List<Item>> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Item>>>() {},
                name, warehouseId).getBody();

        List<Item> items = responseBodyWrapper.getData();
        logger.debug(">> get {} item", items.size());
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

}
