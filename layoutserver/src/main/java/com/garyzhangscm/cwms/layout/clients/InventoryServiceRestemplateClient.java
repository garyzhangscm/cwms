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

package com.garyzhangscm.cwms.layout.clients;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.layout.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;


@Component
public class InventoryServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceRestemplateClient.class);



    @Autowired
    private RestTemplateProxy restTemplateProxy;



    public Integer getInventoryCountByLocationGroup(Long warehouseId, Long locationGroupId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories/count")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("locationGroupId", locationGroupId);


        return restTemplateProxy.exchange(
                Integer.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public Integer getInventoryCountByPickZone(Long warehouseId, Long pickZoneId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories/count")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("pickZoneId", pickZoneId);


        return restTemplateProxy.exchange(
                Integer.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }


    public Integer getInventoryCountByLocations(Long warehouseId, String locationIds) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories/count")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("locationIds", locationIds);


        return restTemplateProxy.exchange(
                Integer.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }


    public InventoryStatus addInventoryStatus(
            InventoryStatus inventoryStatus
    ) throws JsonProcessingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventory-statuses")
                .queryParam("warehouseId", inventoryStatus.getWarehouseId());


        return restTemplateProxy.exchange(
                InventoryStatus.class,
                builder.toUriString(),
                HttpMethod.PUT,
                inventoryStatus
        );


    }


    /**
     * Remove inventory from a warehouse, location group and location. Location group and location
     * id are optional. This is a function to be called when we remove the warehouse, a location group
     * or location
     * @param warehouseId
     * @param locationGroupId
     * @param locationId
     * @return
     */
    @Async("asyncExecutor")
    public String removeInventory(Long warehouseId, Long locationGroupId, Long locationId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/inventories")
                        .queryParam("warehouseId", warehouseId);

        if (Objects.nonNull(locationGroupId)) {
            builder = builder.queryParam("locationGroupId", locationGroupId);
        }
        if (Objects.nonNull(locationId)) {
            builder = builder.queryParam("locationId", locationId);
        }

        logger.debug("start to remove inventory from {}", builder.toUriString());


        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.DELETE,
                null
        );

    }
}
