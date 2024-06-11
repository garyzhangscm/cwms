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

package com.garyzhangscm.cwms.layout.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.layout.ResponseBodyWrapper;
import com.garyzhangscm.cwms.layout.model.FileUploadType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;


@Component
public class ResourceServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(ResourceServiceRestemplateClient.class);

    @Autowired
    OAuth2RestOperations restTemplate;

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RestTemplateProxy restTemplateProxy;


    public String validateCSVFile(Long companyId, Long warehouseId,
                                  String type, String headers, Boolean ignoreUnknownFields) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/file-upload/validate-csv-file")
                        .queryParam("companyId", companyId)
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("type", type)
                        .queryParam("headers", headers);

        if (Objects.nonNull(ignoreUnknownFields)) {
            builder = builder.queryParam("ignoreUnknownFields", ignoreUnknownFields);
        }

        ResponseBodyWrapper<String> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();

        return responseBodyWrapper.getData();

    }


    public FileUploadType getFileUploadType(Long companyId, Long warehouseId,
                                            String type) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/file-upload/types/{type}")
                        .queryParam("companyId", companyId)
                        .queryParam("warehouseId", warehouseId);

        return restTemplateProxy.exchange(
                FileUploadType.class,
                builder.buildAndExpand(type).toUriString(),
                HttpMethod.GET,
                null
        );

    }

}
