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

import com.garyzhangscm.cwms.common.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;

@Component
public class ResourceServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(ResourceServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;
/**
    @Cacheable(cacheNames = "CommonService_User", unless="#result == null")
    public User getUserById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/users/{id}");

        return restTemplateProxy.exchange(
                User.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }
 **/
    @Cacheable(cacheNames = "CommonService_User", unless="#result == null")
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
/**
    @Cacheable(cacheNames = "CommonService_Role", unless="#result == null")
    public Role getRoleById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/roles/{id}");

        ResponseBodyWrapper<Role> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Role>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
**/

/**
    @Cacheable(cacheNames = "CommonService_Role", unless="#result == null")
    public Role getRoleByName(Long companyId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/roles")
                        .queryParam("name", name)
                        .queryParam("companyId", companyId);

        ResponseBodyWrapper<List<Role>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Role>>>() {}).getBody();

        List<Role> roles = responseBodyWrapper.getData();

        if (roles.size() != 1) {
            return null;
        }
        else {
            return roles.get(0);
        }

    }
**/
/**
    @Cacheable(cacheNames = "CommonService_WorkingTeam", unless="#result == null")
    public WorkingTeam getWorkingTeamById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/working-teams/{id}");

        ResponseBodyWrapper<WorkingTeam> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WorkingTeam>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
 **/
/**
    @Cacheable(cacheNames = "CommonService_WorkingTeam", unless="#result == null")
    public WorkingTeam getWorkingTeamByName(Long companyId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/working-teams")
                        .queryParam("name", name)
                        .queryParam("companyId", companyId);

        ResponseBodyWrapper<List<WorkingTeam>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<WorkingTeam>>>() {}).getBody();

        List<WorkingTeam> workingTeams = responseBodyWrapper.getData();

        if (workingTeams.size() != 1) {
            return null;
        }
        else {
            return workingTeams.get(0);
        }

    }
    **/

    public String validateNewUsername(Long companyId, Long warehouseId, String value) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/resource/users/validate-new-username")
                        .queryParam("companyId", companyId)
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("username", value);
/**
        ResponseBodyWrapper<String> responseBodyWrapper
                = restTemplate.exchange(
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

        return restTemplateProxy.exchange(
                String.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );


    }


}
