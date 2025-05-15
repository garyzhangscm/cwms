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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.model.shipengine.RateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;



@Component
public class ShipEngineRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(ShipEngineRestemplateClient.class);

    @Value("${parcel.shipEngine.apiKey}")
    private String apiKey;


    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    public String getRate(RateRequest rateRequest)
            throws JsonProcessingException {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("https").host("api.shipengine.com")
                        .path("/v1/rates");

        String rateRequestJSON = objectMapper.writeValueAsString(rateRequest);
        logger.debug("send request to the server \n{}", rateRequestJSON);

        RestTemplate restTemplate = new RestTemplate();

        String result =
                restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.POST,
                        getHttpEntity(rateRequestJSON),
                        new ParameterizedTypeReference<String>() {}).getBody();

        return result;

    }



    private HttpEntity<String> getHttpEntity(String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        headers.add("Api-Key", apiKey);
        return new HttpEntity<String>(requestBody, headers);
    }

}
