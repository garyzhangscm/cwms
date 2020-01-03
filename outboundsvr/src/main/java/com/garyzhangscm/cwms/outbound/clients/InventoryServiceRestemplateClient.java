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
import com.garyzhangscm.cwms.outbound.model.InventoryStatus;
import com.garyzhangscm.cwms.outbound.model.Item;
import com.garyzhangscm.cwms.outbound.model.ItemFamily;
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

    public Item getItemByName(String name) {

        ResponseBodyWrapper<List<Item>> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/inventory/items?name={name}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Item>>>() {}, name).getBody();

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

    public ItemFamily getItemFamilyByName(String name) {

        ResponseBodyWrapper<List<ItemFamily>> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/inventory/item-families?name={name}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<ItemFamily>>>() {}, name).getBody();

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

    public InventoryStatus getInventoryStatusByName(String name) {

        ResponseBodyWrapper<List<InventoryStatus>> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/inventory/inventory-statuses?name={name}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<InventoryStatus>>>() {}, name).getBody();

        List<InventoryStatus> inventoryStatuses = responseBodyWrapper.getData();
        if (inventoryStatuses.size() == 0) {
            return null;
        }
        else {
            return inventoryStatuses.get(0);
        }
    }


}
