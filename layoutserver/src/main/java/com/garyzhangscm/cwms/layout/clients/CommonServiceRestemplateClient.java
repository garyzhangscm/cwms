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

import com.garyzhangscm.cwms.layout.model.Client;
import com.garyzhangscm.cwms.layout.model.Policy;
import com.garyzhangscm.cwms.layout.model.Warehouse;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;


@Component
public class CommonServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(CommonServiceRestemplateClient.class);


    @Autowired
    private RestTemplateProxy restTemplateProxy;


    @Cacheable(cacheNames = "LayoutService_Client", unless="#result == null")
    public Client getClientByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/clients")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("name", name);

        List<Client> clients
                = restTemplateProxy.exchangeList(
                Client.class,
                builder.toUriString(),
                HttpMethod.GET,
                null);

        if (clients.size() == 0) {
            return null;
        }
        else {
            return clients.get(0);
        }
    }


    @Cacheable(cacheNames = "LayoutService_Policy", unless="#result == null")
    public Policy getPolicyByKey(Long warehouseId, String key) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/policies")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("key", key);


        List<Policy> policies = restTemplateProxy.exchangeList(
                Policy.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
        if (policies.size() > 0) {

            logger.debug("Find result {} for policy {}",
                    policies.get(0).getValue(), key);
            return policies.get(0);
        }
        else {
            logger.debug("Find None for policy {}", key);
            return null;
        }

    }

    public Policy createPolicy(Policy policy)  {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/policies");


        return restTemplateProxy.exchange(
                Policy.class,
                builder.toUriString(),
                HttpMethod.POST,
                policy
        );
    }


    public String removePolicy(Warehouse warehouse, String key) {
        UriComponentsBuilder builder =
            UriComponentsBuilder.newInstance()
                    .scheme("http").host("apigateway").port(5555)
                    .path("/api/common/policies")
                .queryParam("warehouseId", warehouse.getId());

        if (Strings.isNotBlank(key)) {
            builder = builder.queryParam("key", key);
        }



        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.DELETE,
                null
        );
    }
}
