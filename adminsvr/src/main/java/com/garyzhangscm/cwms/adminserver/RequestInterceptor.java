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

package com.garyzhangscm.cwms.adminserver;


import com.garyzhangscm.cwms.adminserver.clients.AuthServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

@Component
public class RequestInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RequestInterceptor.class);
    @Autowired
    AuthServiceRestemplateClient authServiceRestemplateClient;


    @Autowired
    UserService userService;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        // If the call is from a web client, then we will do nothing.
        // Otherwise, we will log in as a specific predefined user
        // so that we can still use the OAuth2 framework
        /****
        if (StringUtils.isNotBlank(userService.getCurrentUserName())) {

            logger.debug("web service call from web client by user {}", userService.getCurrentUserName());
            return execution.execute(request, body);
        }
         ***/
        // logger.debug("Start to get current token");
        String token = authServiceRestemplateClient.getCurrentLoginUser().getToken();

        HttpHeaders headers = request.getHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        headers.set("Authorization", "Bearer "+token);

        // Add
        return execution.execute(request, body);
    }
}
