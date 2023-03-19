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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Component
public class RestTemplateProxy {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateProxy.class);

    @Autowired
    private OAuth2RestOperations restTemplate;

    @Autowired
    @Qualifier("autoLoginRestTemplate")
    private RestTemplate autoLoginRestTemplate;

    public RestOperations getRestTemplate()  {

        boolean inUserContext = true;
        try {
            logger.debug("Objects.isNull(restTemplate.getAccessToken())? {}",
                    Objects.isNull(restTemplate.getAccessToken()));
            if (Objects.isNull(restTemplate.getAccessToken())) {
                inUserContext = false;
            }
        }
        catch (Exception exception) {
            inUserContext = false;
        }
        logger.debug("we are in the user context? {}", inUserContext);
        if (inUserContext) {
            return restTemplate;
        }
        else {
            return autoLoginRestTemplate;
        }
    }
}
