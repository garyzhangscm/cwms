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


import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.ItemFamily;
import org.apache.logging.log4j.util.Strings;
import org.springframework.cache.annotation.Cacheable;
import com.garyzhangscm.cwms.integration.model.InventoryStatus;
import com.garyzhangscm.cwms.integration.model.Item;

import com.garyzhangscm.cwms.integration.model.ItemPackageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;

@Component
public class InventoryServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceRestemplateClient.class);


    @Autowired
    private RestTemplateProxy restTemplateProxy;

    @Cacheable(cacheNames = "IntegrationService_ItemFamily", unless="#result == null")
    public ItemFamily getItemFamilyByName(Long companyId, Long warehouseId, String name)  {
        logger.debug("Start to get item family by name");
        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("apigateway").port(5555)
                            .path("/api/inventory/item-families")
                            .queryParam("name", URLEncoder.encode(name, "UTF-8"))
                            .queryParam("companyId", companyId)
                            .queryParam("warehouseId", warehouseId);

/**
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
**/
            List<ItemFamily> itemFamilies = restTemplateProxy.exchangeList(
                    ItemFamily.class,
                    builder.build(true).toUriString(),
                    HttpMethod.GET,
                    null
            );

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
    @Cacheable(cacheNames = "IntegrationService_ItemFamily", unless="#result == null")
    public ItemFamily getItemFamilyById(Long id)  {

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

    @Cacheable(cacheNames = "IntegrationService_Item", unless="#result == null")
    public Item getItemById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
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


    @Cacheable(cacheNames = "IntegrationService_Item", unless="#result == null")
    public Item getItemByName(Long companyId, Long warehouseId, Long clientId, String name)  {
        logger.debug("Start to get item by name {}", name);
        if (Strings.isBlank(name)) {
            return null;
        }
        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("apigateway").port(5555)
                            .path("/api/inventory/items")
                            .queryParam("name", URLEncoder.encode(name, "UTF-8"))
                            .queryParam("warehouseId", warehouseId)
                            .queryParam("companyId", companyId);


            if (Objects.nonNull(clientId)) {
                builder = builder.queryParam("clientIds", String.valueOf(clientId));
            }
/**
            ResponseBodyWrapper<List<Item>> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<Item>>>() {
                    }).getBody();

            // logger.debug("get response from itembyname:\n {}",
            //         responseBodyWrapper);
            List<Item> items = responseBodyWrapper.getData();
 **/
            List<Item> items = restTemplateProxy.exchangeList(
                    Item.class,
                    builder.build(true).toUriString(),
                    HttpMethod.GET,
                    null
            );


            logger.debug("get {} items by getItemByName with name {}",
                    items.size(), name);

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

    public Item getItemByQuickbookListId(Long companyId, Long warehouseId, String itemQuickbookListId)  {
        logger.debug("Start to get item by quickbook list id {}", itemQuickbookListId);
        if (Strings.isBlank(itemQuickbookListId)) {
            return null;
        }
        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("apigateway").port(5555)
                            .path("/api/inventory/items")
                            .queryParam("quickbookListId", URLEncoder.encode(itemQuickbookListId, "UTF-8"))
                            .queryParam("warehouseId", warehouseId)
                            .queryParam("companyId", companyId);

/**
            ResponseBodyWrapper<List<Item>> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<Item>>>() {
                    }).getBody();

            // logger.debug("get response from getItemByQuickbookListId:\n {}",
            //         responseBodyWrapper);
            List<Item> items = responseBodyWrapper.getData();
 **/
            List<Item> items = restTemplateProxy.exchangeList(
                    Item.class,
                    builder.build(true).toUriString(),
                    HttpMethod.GET,
                    null
            );

            logger.debug("Get {} items from getItemByQuickbookListId by list id {}",
                    items.size(), itemQuickbookListId);
            if (items.size() == 0) {
                return null;
            } else {
                return items.get(0);
            }
        }
        catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            throw ResourceNotFoundException.raiseException("can't find the item by quickbook list id " + itemQuickbookListId);
        }
    }


    @Cacheable(cacheNames = "IntegrationService_InventoryStatus", unless="#result == null")
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

    @Cacheable(cacheNames = "IntegrationService_ItemPackageType", unless="#result == null")
    public ItemPackageType getItemPackageTypeByName(Long companyId, Long warehouseId, Long itemId, String name) {
        try{

            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("apigateway").port(5555)
                            .path("/api/inventory/itemPackageTypes")
                            .queryParam("name", URLEncoder.encode(name, "UTF-8"))
                            .queryParam("companyId", companyId)
                            .queryParam("warehouseId", warehouseId)
                            .queryParam("itemId", itemId);
/**
            ResponseBodyWrapper<List<ItemPackageType>> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<ItemPackageType>>>() {}).getBody();

            List<ItemPackageType> itemPackageTypes = responseBodyWrapper.getData();
 **/
            List<ItemPackageType> itemPackageTypes = restTemplateProxy.exchangeList(
                    ItemPackageType.class,
                    builder.build(true).toUriString(),
                    HttpMethod.GET,
                    null
            );

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

    public InventoryStatus getAvailableInventoryStatus(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory-statuses/available")
                        .queryParam("warehouseId", warehouseId);
        return restTemplateProxy.exchange(
                InventoryStatus.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }
}
