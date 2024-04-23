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
import com.garyzhangscm.cwms.integration.exception.ExceptionCode;
import com.garyzhangscm.cwms.integration.exception.GenericException;
import com.garyzhangscm.cwms.integration.model.tiktok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Component
public class TikTokAPIRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(TikTokAPIRestemplateClient.class);



    @Value("${tiktok.appKey:NOT-SET-YET}")
    private String appKey;

    @Value("${tiktok.appSecret:NOT-SET-YET}")
    private String appSecret;

    // domain to auth with auth code and get the access token and refresh token
    // eg: auth.tiktok-shops.com
    @Value("${tiktok.domain.userAuth:NOT-SET-YET}")
    private String userAuthDomain;


    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("noAuthRestTemplate")
    RestTemplate noAuthRestTemplate;

    @Autowired
    TiktokRestTemplateProxy tiktokRestTemplateProxy;

    public TiktokRequestAccessTokenAPICallResponse requestSellerAccessToken(String authCode)   {
        // https://auth.tiktok-shops.com/api/v2/token/get?app_key=123abcd&auth_code=ROW_FeBoANmHP3yqdoUI9fZOCw&app_secret=
        //         15abf8a4972afd1f275d5b19bfa9a17e0d142aa7&grant_type=authorized_code


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("https").host(userAuthDomain)
                        .path("/api/v2/token/get")
                        .queryParam("app_key", appKey)
                        .queryParam("app_secret", appSecret)
                        .queryParam("auth_code", authCode)
                        .queryParam("grant_type", "authorized_code");

        String url = builder.toUriString();
        //logger.debug("start to get seller's token by url \n{}", url);

        TiktokAPICallResponse<TiktokRequestAccessTokenAPICallResponse> response =
                noAuthRestTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        TiktokAPICallResponse.class).getBody();
        //logger.debug("Get response for request access token:\n{}", response);


        try {

            // response.getData() is of type linkedHashMap
            // we will need to cast the data into json format , then
            // cast the JSON back to the POJO
            String json = objectMapper.writeValueAsString(response.getData());
            //logger.debug("convert to JSON:\n{}", json);
            // logger.debug("after cast to JSON: \n {}", json);

            return objectMapper.readValue(json, TiktokRequestAccessTokenAPICallResponse.class);
            // logger.debug("resultT class is {}", resultT.getClass().getName());
            // return resultT;

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GenericException(ExceptionCode.SYSTEM_FATAL_ERROR,
                    GenericException.createDefaultData(e.getMessage()));
        }

    }

    public List<TikTokSellerAuthorizedShop> getAuthorizedShops(String accessToken) {

        String path = "/api/shop/get_authorized_shop";
        List<Pair<String, String>> parameters = List.of(
                Pair.of("access_token", accessToken),
                Pair.of("app_key", appKey),
                Pair.of("shop_id", ""),
                Pair.of("version", "202212"),
                Pair.of("timestamp", String.valueOf(System.currentTimeMillis()  / 1000))
        );

        logger.debug("start to get valid shops for access token {}",
                accessToken);
        TikTokAuthorizedSellerShopListWrapper shopListWrapper =
                tiktokRestTemplateProxy.exchange(
                        TikTokAuthorizedSellerShopListWrapper.class,
                        path,
                        parameters,
                        HttpMethod.GET,
                        null,
                        accessToken);
        logger.debug("Get response from the path {}\n{}",
                path, shopListWrapper);

        return shopListWrapper.getShops();
    }

    public List<TikTokSellerShop> getActiveShops(String accessToken) {
        String path = "/seller/202309/shops";
        List<Pair<String, String>> parameters = List.of(
                Pair.of("app_key", appKey),
                // Pair.of("access_token", accessToken),
                Pair.of("timestamp", String.valueOf(System.currentTimeMillis() / 1000))
        );

        logger.debug("start to get valid shops for access token {}",
                accessToken);
        TikTokSellerShopListWrapper shopListWrapper =
                tiktokRestTemplateProxy.exchange(
                        TikTokSellerShopListWrapper.class,
                        path,
                        parameters,
                        HttpMethod.GET,
                        null, accessToken);
        logger.debug("Get response from the path {}\n{}",
                path, shopListWrapper);

        return shopListWrapper.getShops();
    }


}
