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

package com.garyzhangscm.cwms.adminserver.clients;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.model.wms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;


@Component
public class IntegrationServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationServiceRestemplateClient.class);


    @Autowired
    private RestTemplateProxy restTemplateProxy;

    public <T> IntegrationData sendData(String subUrl, T data) throws JsonProcessingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/integration/integration-data/" + subUrl);
/**
        ResponseBodyWrapper<IntegrationData> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.PUT,
                getHttpEntity(objectMapper.writeValueAsString(data)),
                new ParameterizedTypeReference<ResponseBodyWrapper<IntegrationData>>() {}).getBody();

        return responseBodyWrapper.getData();
**/

        return restTemplateProxy.exchange(
                IntegrationData.class,
                builder.toUriString(),
                HttpMethod.PUT,
                data
        );
    }
    public List<IntegrationData> getDataByParams(String subUrl, Map<String, String> params) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/integration/integration-data/" + subUrl);

        params.forEach((key, value) -> {
            builder.queryParam(key, value);
        });

/**
        ResponseBodyWrapper<List<IntegrationData>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<IntegrationData>>>() {}).getBody();

        logger.debug("get IntegrationData by params {}\n{}", params, responseBodyWrapper.getData());

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchangeList(
                IntegrationData.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }
    public IntegrationData getData(String subUrl, Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/integration/integration-data/" + subUrl + "/" + id);

/**
        ResponseBodyWrapper<IntegrationData> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<IntegrationData>>() {}).getBody();

        // logger.debug("get IntegrationData by id {}\n{}", id, responseBodyWrapper.getData());

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                IntegrationData.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }
}
