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

public class LoginResponseWrapper {
    private int result;
    private String message;
    private JWTTokenWrapper user;


    private LoginResponseWrapper(int result, String message, JWTTokenWrapper userInfo) {
        this.result = result;
        this.message = message;
        this.user = userInfo;
    }

    public static LoginResponseWrapper of(int result, String message, JWTTokenWrapper userInfo) {
        return new LoginResponseWrapper(result, message, userInfo);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public JWTTokenWrapper getUser() {
        return user;
    }

    public void setUser(JWTTokenWrapper user) {
        this.user = user;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }
}
