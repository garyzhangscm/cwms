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


import com.garyzhangscm.cwms.inbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

@Component
public class AdminServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceRestemplateClient.class);


    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;

    @Autowired
    private RestTemplateProxy restTemplateProxy;


    @Cacheable(cacheNames = "InboundService_BillableActivityType", unless="#result == null")
    public BillableActivityType getBillableActivityTypeById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/admin/billable-activity-types/{id}");

        ResponseBodyWrapper<BillableActivityType> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                    null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<BillableActivityType>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

}
