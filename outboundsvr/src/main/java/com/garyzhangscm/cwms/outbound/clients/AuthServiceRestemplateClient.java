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
import com.garyzhangscm.cwms.outbound.LoginResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.LoginCredential;
import com.garyzhangscm.cwms.outbound.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;


@Component
public class AuthServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceRestemplateClient.class);

    private User currentLoginUser;

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;
    // private ObjectMapper mapper = new ObjectMapper();

    // User a new rest template for login. The global auto-wirable
    // rest template will try to add an user token to the http header
    // which will call the getCurrentLoginUser() to get the token.
    // if the user has not login in yet, then it will call login()
    // to login a specific user for the integration, which will make
    // the call a infinite recursive call.
    @Autowired
    @Qualifier("noTokenRestTemplate")
    RestTemplate restTemplate;


    public User login() throws IOException {
        LoginCredential loginCredential = new LoginCredential(1L,"GZHANG", "GZHANG");

        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/auth/login?")
                .append("_allow_anonymous=true");

        String requestBody = objectMapper.writeValueAsString(loginCredential);
        logger.debug("LOGIN WITH: {}", requestBody);
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        HttpEntity<String> httpEntity = new HttpEntity<String>(requestBody, headers);


        LoginResponseBodyWrapper loginResponseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.POST, httpEntity,
                new ParameterizedTypeReference<LoginResponseBodyWrapper>() {}).getBody();

        logger.debug("Get user from auth server: {}", loginResponseBodyWrapper.getUser());
        currentLoginUser = loginResponseBodyWrapper.getUser();
        return loginResponseBodyWrapper.getUser();
    }


    public User getCurrentLoginUser() throws IOException{
        if (currentLoginUser == null) {
            return login();
        }
        else if (currentLoginUser.getTime().toLocalDateTime().plusSeconds(currentLoginUser.getRefreshIn()).isBefore(LocalDateTime.now())) {
            // The login is expired, let's log in again
            return login();
        }
        else {
            // current login is still valid

            return currentLoginUser;
        }
    }
}
