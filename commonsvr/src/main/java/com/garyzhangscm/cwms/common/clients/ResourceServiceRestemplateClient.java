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

import com.garyzhangscm.cwms.common.ResponseBodyWrapper;
import com.garyzhangscm.cwms.common.model.Role;
import com.garyzhangscm.cwms.common.model.User;
import com.garyzhangscm.cwms.common.model.WorkingTeam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class ResourceServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(ResourceServiceRestemplateClient.class);

    @Autowired
    OAuth2RestOperations restTemplate;


    public User getUserById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/resource/users/{id}");

        ResponseBodyWrapper<User> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<User>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public User getUserByUsername(Long companyId, String username) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/resource/users")
                        .queryParam("username", username)
                        .queryParam("companyId", companyId);

        ResponseBodyWrapper<List<User>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<User>>>() {}).getBody();

        List<User> users = responseBodyWrapper.getData();

        if (users.size() != 1) {
            return null;
        }
        else {
            return users.get(0);
        }

    }

    public Role getRoleById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/resource/roles/{id}");

        ResponseBodyWrapper<Role> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Role>>() {}).getBody();

        return responseBodyWrapper.getData();

    }



    public Role getRoleByName(Long companyId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
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


    public WorkingTeam getWorkingTeamById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/resource/working-teams/{id}");

        ResponseBodyWrapper<WorkingTeam> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<WorkingTeam>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public WorkingTeam getWorkingTeamByName(Long companyId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
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

    public String validateNewUsername(Long companyId, Long warehouseId, String value) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulservice")
                        .path("/api/resource/users/validate-new-username")
                        .queryParam("companyId", companyId)
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("username", value);

        ResponseBodyWrapper<String> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<String>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
}