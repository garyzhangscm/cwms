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

package com.garyzhangscm.cwms.resources.clients;

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.model.Inventory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

@Component
public class InventoryServiceRestemplateClient implements  InitiableServiceRestemplateClient{

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    public String initTestData(Long companyId, String warehouseName) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/test-data/init")
                        .queryParam("companyId", companyId)
                        .queryParam("warehouseName", warehouseName);

        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );

    }

    public String initTestData(Long companyId, String name, String warehouseName) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/test-data/init/{name}")
                        .queryParam("companyId", companyId)
                        .queryParam("warehouseName", warehouseName);

        return restTemplateProxy.exchange(
                String.class,
                builder.buildAndExpand(name).toUriString(),
                HttpMethod.POST,
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


        return restTemplateProxy.exchangeList(
                Inventory.class,
                builder.toUriString(),
                HttpMethod.GET,
                null);

    }


    public String[] getTestDataNames() {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/test-data");

        return restTemplateProxy.exchange(
                String[].class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }
    public boolean contains(String name) {
        return Arrays.stream(getTestDataNames()).anyMatch(dataName -> dataName.equals(name));
    }

    public String clearData(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/inventory/test-data/clear")
                        .queryParam("warehouseId", warehouseId);


        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );
    }
}
