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

import com.garyzhangscm.cwms.layout.ResponseBodyWrapper;
import com.garyzhangscm.cwms.layout.model.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@CacheConfig(cacheNames = "common")
public class CommonServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(CommonServiceRestemplateClient.class);

    @Autowired
    OAuth2RestTemplate restTemplate;


    @Cacheable
    public Policy getPolicyByKey(String key) {
        logger.debug("Start to get policy by url:\n {}", "http://zuulserver:5555/api/common/policies?key={key}");
        logger.debug("key >> {}", key);
        ResponseBodyWrapper<List<Policy>> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/common/policies?key={key}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Policy>>>() {
                }, key).getBody();

        List<Policy> policies = responseBodyWrapper.getData();
        if (policies.size() > 0) {

            logger.debug("Find result {} for policy {}",
                    policies.get(0).getValue(), key);
            return policies.get(0);
        }
        else {
            return null;
        }

    }


}
