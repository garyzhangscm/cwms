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


public class UserAuthentication {

    private String token;
    private String refreshToken;
    private String name;
    private String email;
    // private Long id;
    // private Long time;
    private int refreshIn;
    // expired in minutes
    private int expiredIn;

/**
    public JWTTokenWrapper(String token, String name, String email, Long id, Long time,
                           int refreshIn, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.name = name;
        this.email = email;
        this.id = id;
        this.time = time;
        this.refreshIn = refreshIn;
    }
**/
    public UserAuthentication() {

    }

    public UserAuthentication(String token,
                              String refreshToken,
                              String name,
                              String email,
                              int refreshIn,
                              int expiredIn) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.name = name;
        this.email = email;
        this.refreshIn = refreshIn;
        this.expiredIn = expiredIn;

    }
    public UserAuthentication(User user) {
        this.token = user.getCurrentToken();
        this.refreshToken = user.getRefreshToken();
        this.name = user.getUsername();
        this.email = user.getEmail();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }


    public int getRefreshIn() {
        return refreshIn;
    }

    public void setRefreshIn(int refreshIn) {
        this.refreshIn = refreshIn;
    }

    public int getExpiredIn() {
        return expiredIn;
    }

    public void setExpiredIn(int expiredIn) {
        this.expiredIn = expiredIn;
    }
}
