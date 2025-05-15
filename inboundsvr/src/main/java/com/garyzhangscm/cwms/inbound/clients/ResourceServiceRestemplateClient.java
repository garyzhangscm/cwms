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

package com.garyzhangscm.cwms.inbound.clients;

import com.garyzhangscm.cwms.inbound.model.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;


@Component
public class ResourceServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(ResourceServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    public ReportHistory generateReport(Long warehouseId, ReportType type,
                                        Report reportData, String locale,
                                        String printerName) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/reports/{warehouseId}/{type}")
                        .queryParam("locale", locale);

        if (Strings.isNotBlank(printerName)) {
            builder = builder.queryParam("printerName", printerName);
        }
/**
        ResponseBodyWrapper<ReportHistory> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(warehouseId, type).toUriString(),
                        HttpMethod.POST,
                        getHttpEntity(objectMapper.writeValueAsString(reportData)),
                        new ParameterizedTypeReference<ResponseBodyWrapper<ReportHistory>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                ReportHistory.class,
                builder.buildAndExpand(warehouseId, type).toUriString(),
                HttpMethod.POST,
                reportData
        );


    }

    @Cacheable(cacheNames = "InboundService_User", unless="#result == null")
    public User getUserByUsername(Long companyId, String username) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/users")
                        .queryParam("username", username)
                        .queryParam("companyId", companyId);
/**
        ResponseBodyWrapper<List<User>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<User>>>() {}).getBody();

        List<User> users = responseBodyWrapper.getData();
 **/

        List<User> users = restTemplateProxy.exchangeList(
                User.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

        if (users.size() != 1) {
            return null;
        }
        else {
            return users.get(0);
        }

    }

    public String validateCSVFile(Long companyId, Long warehouseId,
                                  String type, String headers, Boolean ignoreUnknownFields) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/file-upload/validate-csv-file")
                        .queryParam("companyId", companyId)
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("type", type)
                        .queryParam("headers", headers);
        if (Objects.nonNull(ignoreUnknownFields)) {
            builder = builder.queryParam("ignoreUnknownFields", ignoreUnknownFields);
        }
/**
        ResponseBodyWrapper<String> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );

    }

    public FileUploadType getFileUploadType(Long companyId, Long warehouseId,
                                  String type) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
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
