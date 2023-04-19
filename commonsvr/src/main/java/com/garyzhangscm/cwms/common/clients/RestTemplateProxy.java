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

package com.garyzhangscm.cwms.common.clients;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.common.ResponseBodyWrapper;
import com.garyzhangscm.cwms.common.exception.ExceptionCode;
import com.garyzhangscm.cwms.common.exception.GenericException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Component
public class RestTemplateProxy {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateProxy.class);

    @Autowired
    OAuth2RestOperations restTemplate;

    @Autowired
    @Qualifier("autoLoginRestTemplate")
    RestTemplate autoLoginRestTemplate;

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;


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

    public <T> T exchange(Class<T> t, String uri, HttpMethod method,
                          Object obj) {
        HttpEntity entity = null;
        try {
            entity = Objects.isNull(obj) ?
                    null : getHttpEntity(objectMapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GenericException(ExceptionCode.SYSTEM_FATAL_ERROR,
                    GenericException.createDefaultData(e.getMessage()));
        }

        ResponseBodyWrapper response = getRestTemplate().exchange(
                uri,
                method,
                entity,
                ResponseBodyWrapper.class).getBody();

        if (response.getResult() != 0) {
            throw new GenericException(ExceptionCode.SYSTEM_FATAL_ERROR,
                    GenericException.createDefaultData(response.getMessage()));
        }

        try {

            // response.getData() is of type linkedHashMap
            // we will need to cast the data into json format , then
            // cast the JSON back to the POJO
            String json = objectMapper.writeValueAsString(response.getData());
            // logger.debug("after cast to JSON: \n {}", json);

            return objectMapper.readValue(json, t);
            // logger.debug("resultT class is {}", resultT.getClass().getName());
            // return resultT;

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GenericException(ExceptionCode.SYSTEM_FATAL_ERROR,
                    GenericException.createDefaultData(e.getMessage()));
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
