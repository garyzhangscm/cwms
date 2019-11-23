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

package com.garyzhangscm.cwms.common;

import com.garyzhangscm.cwms.common.exception.GenericException;

public class ResponseBodyWrapper<T> {
    private int result;
    private String message;
    private T data;

    public ResponseBodyWrapper(int result, String message, T data) {
        this.result = result;
        this.message = message;
        this.data = data;
    }
    public static ResponseBodyWrapper raiseError(int errorCode, String errorMessage) {
        return new ResponseBodyWrapper<String>(errorCode, errorMessage, "");
    }

    public static ResponseBodyWrapper raiseError(GenericException exception) {
        return new ResponseBodyWrapper<String>(exception.getCode(), exception.getMessage(), "");
    }
    public static ResponseBodyWrapper success(String message, String body) {
        return new ResponseBodyWrapper<String>(0,  message, body);
    }
    public static ResponseBodyWrapper success(String body) {
        return new ResponseBodyWrapper<String>(0,  "", body);
    }


    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
