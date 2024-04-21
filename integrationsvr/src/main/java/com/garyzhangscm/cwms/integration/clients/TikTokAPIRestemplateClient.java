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

package com.garyzhangscm.cwms.integration.clients;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.garyzhangscm.cwms.integration.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.integration.model.usps.AddressValidateResponse;
import com.garyzhangscm.cwms.integration.model.usps.Error;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

@Component
public class TikTokAPIRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(TikTokAPIRestemplateClient.class);



    @Value("${tiktok.appKey:NOT-SET-YET}")
    private String appKey;

    @Value("${tiktok.appSecret:NOT-SET-YET}")
    private String appSecret;

    // domain to auth with auth code and get the access token and refresh token
    // eg: auth.tiktok-shops.com
    @Value("${tiktok.domain.auth:NOT-SET-YET}")
    private String authDomain;


    @Autowired
    @Qualifier("noAuthRestTemplate")
    RestTemplate noAuthRestTemplate;

    public String getSellerToken(String authCode)   {
        // https://auth.tiktok-shops.com/api/v2/token/get?app_key=123abcd&auth_code=ROW_FeBoANmHP3yqdoUI9fZOCw&app_secret=
        //         15abf8a4972afd1f275d5b19bfa9a17e0d142aa7&grant_type=authorized_code


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("https").host(authDomain)
                        .path("/api/v2/token/get")
                        .queryParam("app_key", appKey)
                        .queryParam("app_secret", appSecret)
                        .queryParam("auth_code", authCode)
                        .queryParam("grant_type", "authorized_code");

        String url = builder.toUriString();
        logger.debug("start to get seller's token by url \n{}", url);

        String response = noAuthRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class).getBody();

        logger.debug("get response: \n{}", response);
        return  response;

    }

}
