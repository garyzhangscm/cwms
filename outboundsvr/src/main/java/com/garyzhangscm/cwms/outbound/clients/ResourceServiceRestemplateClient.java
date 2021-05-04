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
import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.Report;
import com.garyzhangscm.cwms.outbound.model.ReportHistory;
import com.garyzhangscm.cwms.outbound.model.ReportType;
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


@Component
public class ResourceServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(ResourceServiceRestemplateClient.class);

    @Autowired
    OAuth2RestOperations restTemplate;

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    public ReportHistory generateReport(Long warehouseId, ReportType type,
                                 Report reportData, String locale)
            throws JsonProcessingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/reports/{warehouseId}/{type}")
                        .queryParam("locale", locale);

        ResponseBodyWrapper<ReportHistory> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(warehouseId, type).toUriString(),
                        HttpMethod.POST,
                        getHttpEntity(objectMapper.writeValueAsString(reportData)),
                        new ParameterizedTypeReference<ResponseBodyWrapper<ReportHistory>>() {}).getBody();

        return responseBodyWrapper.getData();

    }


    private HttpEntity<String> getHttpEntity(String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        return new HttpEntity<String>(requestBody, headers);
    }

}
