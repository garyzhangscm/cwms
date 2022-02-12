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

package com.garyzhangscm.cwms.integration.clients;


import com.garyzhangscm.cwms.integration.exception.GenericException;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.ItemFamily;
import org.springframework.web.client.RestTemplate;
import com.garyzhangscm.cwms.integration.ResponseBodyWrapper;
import com.garyzhangscm.cwms.integration.model.InventoryStatus;
import com.garyzhangscm.cwms.integration.model.Item;

import com.garyzhangscm.cwms.integration.model.ItemPackageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

@Component
public class InventoryServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceRestemplateClient.class);


    @Autowired
    // OAuth2RestTemplate restTemplate;
    RestTemplate restTemplate;

    public ItemFamily getItemFamilyByName(Long companyId, Long warehouseId, String name)  {
        logger.debug("Start to get item family by name");
        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("zuulserver").port(5555)
                            .path("/api/inventory/item-families")
                            .queryParam("name", URLEncoder.encode(name, "UTF-8"))
                            .queryParam("companyId", companyId)
                            .queryParam("warehouseId", warehouseId);


            ResponseBodyWrapper<List<ItemFamily>> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<ItemFamily>>>() {
                    }).getBody();

            logger.debug("get response from itemFamilybyname:\n {}",
                    responseBodyWrapper);
            List<ItemFamily> itemFamilies = responseBodyWrapper.getData();

            if (itemFamilies.size() == 0) {
                return null;
            } else {
                return itemFamilies.get(0);
            }
        }
        catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            throw ResourceNotFoundException.raiseException("can't find the item family by name " + name);
        }
    }
    public ItemFamily getItemFamilyById(Long id)  {

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


    public Item getItemByName(Long warehouseId, String name)  {
        logger.debug("Start to get item by name");
        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("zuulserver").port(5555)
                            .path("/api/inventory/items")
                            .queryParam("name", URLEncoder.encode(name, "UTF-8"))
                            .queryParam("warehouseId", warehouseId);


            ResponseBodyWrapper<List<Item>> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<Item>>>() {
                    }).getBody();

            logger.debug("get response from itembyname:\n {}",
                    responseBodyWrapper);
            List<Item> items = responseBodyWrapper.getData();

            if (items.size() == 0) {
                return null;
            } else {
                return items.get(0);
            }
        }
        catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            throw ResourceNotFoundException.raiseException("can't find the item by name " + name);
        }
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

    public ItemPackageType getItemPackageTypeByName(Long warehouseId, Long itemId, String name) {
        try{

            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("zuulserver").port(5555)
                            .path("/api/inventory/itemPackageTypes")
                            .queryParam("name", URLEncoder.encode(name, "UTF-8"))
                            .queryParam("warehouseId", warehouseId)
                            .queryParam("itemId", itemId);

            ResponseBodyWrapper<List<ItemPackageType>> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<ItemPackageType>>>() {}).getBody();

            List<ItemPackageType> itemPackageTypes = responseBodyWrapper.getData();
            if (itemPackageTypes.size() == 0) {
                return null;
            }
            else {
                return itemPackageTypes.get(0);
            }

        }
        catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            throw ResourceNotFoundException.raiseException("can't find the item by name " + name);
        }
    }

}
