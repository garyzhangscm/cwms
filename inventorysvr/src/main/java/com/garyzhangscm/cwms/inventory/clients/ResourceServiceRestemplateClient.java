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

package com.garyzhangscm.cwms.inventory.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.exception.MissingInformationException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ResourceServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(ResourceServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    @Cacheable(cacheNames = "InventoryService_User")
    public User getUserById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/users/{id}");
/**
        ResponseBodyWrapper<User> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<User>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                User.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }
    @Cacheable(cacheNames = "InventoryService_User")
    public User getUserByUsername(Long companyId, String username) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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

    @Cacheable(cacheNames = "InventoryService_Role")
    public Role getRoleById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/roles/{id}");
/**
        ResponseBodyWrapper<Role> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Role>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Role.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }



    @Cacheable(cacheNames = "InventoryService_Role")
    public Role getRoleByName(Long companyId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/roles")
                        .queryParam("name", name)
                        .queryParam("companyId", companyId);
/**
        ResponseBodyWrapper<List<Role>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Role>>>() {}).getBody();

        List<Role> roles = responseBodyWrapper.getData();
 **/
        List<Role> roles = restTemplateProxy.exchangeList(
                Role.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );


        if (roles.size() != 1) {
            return null;
        }
        else {
            return roles.get(0);
        }

    }




    public ReportHistory generateReport(Long warehouseId, ReportType type,
                                        Report reportData, String locale ) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/reports/{warehouseId}/{type}")
                        .queryParam("locale", locale);
/**
        ResponseBodyWrapper<ReportHistory> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(warehouseId, type).toUri(),
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
                            String fileName,
                            String printerName) {
        logger.debug("start to print report with ");
        logger.debug("# company id: {}", companyId);
        logger.debug("# warehouse id: {}", warehouseId);
        logger.debug("# type: {}", type);
        logger.debug("# file name: {}", fileName);
        logger.debug("# printer name: {}", printerName);

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/report-histories/print/{companyId}/{warehouseId}/{type}/{fileName}")
                        .queryParam("printerName", printerName)
                        .queryParam("copies", 1);

        return restTemplateProxy.exchange(
                String.class,
                builder.buildAndExpand(companyId, warehouseId, type, fileName).toUriString(),
                HttpMethod.POST,
                null
        );
    }
    public String validateCSVFile(Long warehouseId,
                                  String type, String headers) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/file-upload/validate-csv-file")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("type", type)
                        .queryParam("headers", headers);
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


    @Cacheable(cacheNames = "InventoryService_ArchiveConfiguration", unless="#result == null")
    public ArchiveConfiguration getArchiveConfiguration(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/resource/archive-configuration")
                        .queryParam("warehouseId", warehouseId);

        return restTemplateProxy.exchange(
                ArchiveConfiguration.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }
}
