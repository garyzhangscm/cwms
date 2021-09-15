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

package com.garyzhangscm.cwms.auth.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserLoginEvent {


    private String username;
    private String token;


    private Long lastLoginCompanyId;
    private Long lastLoginWarehouseId;


    public UserLoginEvent(){}
    public UserLoginEvent(Long lastLoginCompanyId,
                          Long lastLoginWarehouseId,
                          String username, String token){
        this.lastLoginCompanyId = lastLoginCompanyId;
        this.lastLoginWarehouseId = lastLoginWarehouseId;
        this.username = username;
        this.token = token;
    }


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getLastLoginCompanyId() {
        return lastLoginCompanyId;
    }

    public void setLastLoginCompanyId(Long lastLoginCompanyId) {
        this.lastLoginCompanyId = lastLoginCompanyId;
    }

    public Long getLastLoginWarehouseId() {
        return lastLoginWarehouseId;
    }

    public void setLastLoginWarehouseId(Long lastLoginWarehouseId) {
        this.lastLoginWarehouseId = lastLoginWarehouseId;
    }
}
