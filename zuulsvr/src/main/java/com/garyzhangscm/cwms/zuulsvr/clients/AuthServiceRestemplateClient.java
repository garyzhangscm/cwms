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

package com.garyzhangscm.cwms.zuulsvr.clients;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


@Component
public class AuthServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceRestemplateClient.class);

    @Autowired
    private RestTemplate restTemplate;

    @Cacheable(cacheNames = "ZuulService_CompanyAccess", unless="#result == null")
    public Boolean validateCompanyAccess(Long companyId, String token)  {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/auth/users/company-access-validation")
                        .queryParam("companyId", companyId)
                        .queryParam("token", token)
                        .queryParam("innerCall", "true");

        return restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Boolean>() {}).getBody();


    }



    @Cacheable(cacheNames = "ZuulService_UserByToken", unless="#result == null")
    public String getUserNameByToken(Long companyId, String token)  {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/auth/users/username-by-token")
                        .queryParam("companyId", companyId)
                        .queryParam("token", token)
                .queryParam("innerCall", "true");

        return restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<String>() {}).getBody();


    }


}
