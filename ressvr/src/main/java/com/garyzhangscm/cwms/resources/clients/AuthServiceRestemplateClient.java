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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.model.UserAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.util.List;

@Component
public class AuthServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceRestemplateClient.class);
    @Autowired
    OAuth2RestTemplate restTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    public List<UserAuth> getUserAuthByUsernames(String usernames) {
        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/auth/users?")
                .append("usernames={usernames}");

        List<UserAuth> userAuths = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<UserAuth>>() {},
                usernames).getBody();

        return userAuths;

    }

    public UserAuth getUserAuthByUsername(String username) {
        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/auth/users?")
                .append("usernames={username}");

        List<UserAuth> userAuths = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<UserAuth>>() {},
                username).getBody();

        if (userAuths.size() == 0) {
            return null;
        }
        return userAuths.get(0);
    }

    public UserAuth changeUserAuth(UserAuth userAuth) throws IOException {

        String requestBody = mapper.writeValueAsString(userAuth);

        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        HttpEntity<String> httpEntity = new HttpEntity<String>(requestBody, headers);



        return restTemplate.exchange(
                "http://zuulserver:5555/api/auth/users",
                HttpMethod.POST, httpEntity,
                new ParameterizedTypeReference<UserAuth>() {}).getBody();


    }








}
