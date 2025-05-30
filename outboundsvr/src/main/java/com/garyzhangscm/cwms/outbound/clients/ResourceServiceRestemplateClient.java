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
import com.garyzhangscm.cwms.outbound.model.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;


@Component
public class ResourceServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(ResourceServiceRestemplateClient.class);
 
    @Autowired
    private RestTemplateProxy restTemplateProxy;

    public ReportHistory generateReport(Long warehouseId, ReportType type,
                                 Report reportData, String locale) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/reports/{warehouseId}/{type}")
                        .queryParam("locale", locale);
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

    public String printReport(Long companyId,
                            Long warehouseId,
                            ReportType type,
                            String filename,
                            String findPrinterBy,
                            String printerName)
            throws JsonProcessingException, UnsupportedEncodingException {
        String url = "/api/resource/report-histories/print/"
                + companyId + "/" + warehouseId + "/" + type + "/" + filename;
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path(url);
        if (Strings.isNotBlank(findPrinterBy)) {
            builder = builder.queryParam("findPrinterBy", URLEncoder.encode(findPrinterBy, "UTF-8") );
        }
        if (Strings.isNotBlank(printerName)) {
            builder = builder.queryParam("printerName", URLEncoder.encode(printerName, "UTF-8") );
        }
/**
        ResponseBodyWrapper<String> responseBodyWrapper
                = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.POST,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();

        return responseBodyWrapper.getData();
**/

        return restTemplateProxy.exchange(
                String.class,
                builder.build(true).toUriString(),
                HttpMethod.POST,
                null
        );

    }

    @Cacheable(cacheNames = "OutboundService_User", unless="#result == null")
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
        List<User> users =  restTemplateProxy.exchangeList(
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


    public FileUploadType getFileUploadType(String typename) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/file-upload/types/{typename}");
        return restTemplateProxy.exchange(
                FileUploadType.class,
                builder.buildAndExpand(typename).toUriString(),
                HttpMethod.GET,
                null
        );


    }

    public WorkTask addWorkTask(Long warehouseId, WorkTask workTask)  {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/work-tasks")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<WorkTask> responseBodyWrapper
                = null;
        try {
            responseBodyWrapper = restTemplateProxy.getRestTemplate().exchange(
                    builder.toUriString(),
                    HttpMethod.PUT,
                    getHttpEntity(objectMapper.writeValueAsString(workTask)),
                    new ParameterizedTypeReference<ResponseBodyWrapper<WorkTask>>() {}).getBody();

            return responseBodyWrapper.getData();
        } catch (JsonProcessingException e) {
            e.printStackTrace();

        }
        return null;
**/
        return restTemplateProxy.exchange(
                WorkTask.class,
                builder.toUriString(),
                HttpMethod.PUT,
                workTask
        );

    }

    public WorkTask getWorkTaskById(Long warehouseId, Long workTaskId)  {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/work-tasks/{id}")
                        .queryParam("warehouseId", warehouseId);
/**
        ResponseBodyWrapper<WorkTask> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.buildAndExpand(workTaskId).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WorkTask>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                WorkTask.class,
                builder.buildAndExpand(workTaskId).toUriString(),
                HttpMethod.GET,
                null
        );

    }


    public WorkTask cancelWorkTaskById(Long warehouseId, Long workTaskId)  {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/work-tasks/{id}")
                        .queryParam("warehouseId", warehouseId);

        return restTemplateProxy.exchange(
                WorkTask.class,
                builder.buildAndExpand(workTaskId).toUriString(),
                HttpMethod.DELETE,
                null
        );

    }
    public WorkTask completeWorkTask(Long warehouseId, Long workTaskId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/work-tasks/{id}/complete")
                        .queryParam("warehouseId", warehouseId);

        return restTemplateProxy.exchange(
                WorkTask.class,
                builder.buildAndExpand(workTaskId).toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public WorkTask assingUser(Long warehouseId, Long workTaskId, Long userId)  {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/work-tasks/{id}/assign-user")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("userId", userId);

        /**
        ResponseBodyWrapper<WorkTask> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                    builder.buildAndExpand(workTaskId).toUriString(),
                    HttpMethod.POST,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<WorkTask>>() {}).getBody();

        return responseBodyWrapper.getData();
         **/
        return restTemplateProxy.exchange(
                WorkTask.class,
                builder.buildAndExpand(workTaskId).toUriString(),
                HttpMethod.POST,
                null
        );
    }


    public WorkTask unassingUser(Long warehouseId, Long workTaskId)  {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/work-tasks/{id}/unassign-user")
                        .queryParam("warehouseId", warehouseId);

        /**
         ResponseBodyWrapper<WorkTask> responseBodyWrapper
         = restTemplateProxy.getRestTemplate().exchange(
         builder.buildAndExpand(workTaskId).toUriString(),
         HttpMethod.POST,
         null,
         new ParameterizedTypeReference<ResponseBodyWrapper<WorkTask>>() {}).getBody();

         return responseBodyWrapper.getData();
         **/
        return restTemplateProxy.exchange(
                WorkTask.class,
                builder.buildAndExpand(workTaskId).toUriString(),
                HttpMethod.POST,
                null
        );
    }

    /**
    private HttpEntity<String> getHttpEntity(String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        return new HttpEntity<String>(requestBody, headers);
    }
     **/

    public ReportHistory combineLabels(Long companyId,
                                       Long warehouseId,
                                       List<ReportHistory> reportHistories) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/report-histories/labels/combine")
                        .queryParam("companyId", companyId)
                        .queryParam("warehouseId", warehouseId);

        return restTemplateProxy.exchange(
                ReportHistory.class,
                builder.toUriString(),
                HttpMethod.POST,
                reportHistories
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
