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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.resources.LoginResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.exception.UserOperationException;
import com.garyzhangscm.cwms.resources.model.LoginCredential;
import com.garyzhangscm.cwms.resources.model.User;
import com.garyzhangscm.cwms.resources.model.UserAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class AuthServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceRestemplateClient.class);
    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;

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
    RestTemplate noTokenRestTemplate;

    @Value("${admin.login.username}")
    private String username;

    @Value("${admin.login.password}")
    private String password;

    public List<UserAuth> getUserAuthByUsernames(Long companyId, String usernames)   {

        try {

            UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                    .scheme("http").host("zuulserver").port(5555)
                    .path("/api/auth/users")
                    .queryParam("companyId", companyId)
                    .queryParam("usernames", URLEncoder.encode(usernames, "UTF-8"));

            List<UserAuth> userAuths
                    = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<UserAuth>>() {}).getBody();

            return userAuths;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    public UserAuth getUserAuthByUsername(Long companyId, String username)   {

        try {

            UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                    .scheme("http").host("zuulserver").port(5555)
                    .path("/api/auth/users")
                    .queryParam("companyId", companyId)
                    .queryParam("usernames", URLEncoder.encode(username, "UTF-8"));

            List<UserAuth> userAuths
                    = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<UserAuth>>() {}).getBody();

            if (userAuths.isEmpty()) {
                return null;
            }
            return userAuths.get(0);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        catch (Exception ex) {
            return  null;
        }
    }

    public UserAuth changeUserAuth(UserAuth userAuth)  {
        logger.debug("Start to change user auth: {}", userAuth);
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/auth/users");

        try {
            return restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.POST,
                    getHttpEntity(objectMapper.writeValueAsString(userAuth)),
                    new ParameterizedTypeReference<UserAuth>() {}).getBody();
        } catch (JsonProcessingException e) {
            throw UserOperationException.raiseException("Can't change user's auth information due to JsonProcessingException: " + e.getMessage());
        }


    }

    @Cacheable(cacheNames = "ResourceService_CompanyAccess", unless="#result == null")
    public Boolean validateCompanyAccess(Long companyId, String token)  {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/auth/users/company-access-validation")
                .queryParam("companyId", companyId)
                        .queryParam("token", token);

        return restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Boolean>() {}).getBody();


    }

    public User login() throws IOException {
        LoginCredential loginCredential = new LoginCredential(1L,username, password);

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

    public User getCurrentLoginUser() throws IOException {
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

    private HttpEntity<String> getHttpEntity(String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        return new HttpEntity<String>(requestBody, headers);
    }








}
