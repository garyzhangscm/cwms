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


import com.garyzhangscm.cwms.resources.model.UserAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Component
public class AuthServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    public List<UserAuth> getUserAuthByUsernames(Long companyId, String usernames)   {

        try {

            UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                    .scheme("http").host("apigateway").port(5555)
                    .path("/api/auth/users")
                    .queryParam("companyId", companyId)
                    .queryParam("usernames", URLEncoder.encode(usernames, "UTF-8"));


            List<UserAuth> userAuths
                    = restTemplateProxy.exchangeList(
                    UserAuth.class,
                    builder.build(true).toUriString(),
                    HttpMethod.GET,
                    null);

            return userAuths;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    public UserAuth getUserAuthByUsername(Long companyId, String username)   {

        try {

            UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                    .scheme("http").host("apigateway").port(5555)
                    .path("/api/auth/users")
                    .queryParam("companyId", companyId)
                    .queryParam("usernames", URLEncoder.encode(username, "UTF-8"));


            List<UserAuth> userAuths
                    = restTemplateProxy.exchangeList(
                    UserAuth.class,
                    builder.toUriString(),
                    HttpMethod.GET,
                    null);

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
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/auth/users");


        return restTemplateProxy.exchange(
                UserAuth.class,
                builder.toUriString(),
                HttpMethod.POST,
                userAuth
        );

    }

}
