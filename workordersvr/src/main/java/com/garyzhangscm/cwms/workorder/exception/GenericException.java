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

package com.garyzhangscm.cwms.workorder.exception;

import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

public class GenericException extends RuntimeException {


    private final ExceptionCode exceptionCode;
    private final HashMap<String, Object> data = new HashMap<>();

    public GenericException(ExceptionCode exceptionCode, Map<String, Object> data) {
        super(exceptionCode.getMessage());
        this.exceptionCode = exceptionCode;
        if (!ObjectUtils.isEmpty(data)) {
            this.data.putAll(data);
        }
    }

    protected GenericException(ExceptionCode exceptionCode, Map<String, Object> data, Throwable cause) {
        super(exceptionCode.getMessage(), cause);
        this.exceptionCode = exceptionCode;
        if (!ObjectUtils.isEmpty(data)) {
            this.data.putAll(data);
        }
    }

    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public static Map<String, Object> createDefaultData(String message) {
        // Get the method / class that raise the exception
        // new Throwable().getStackTrace() will get the stack information and
        // we will assume the second line in the stack should be the method
        // that raise the exception
        String path = new Throwable().getStackTrace()[1].toString();
        Map<String, Object> data = new HashMap<>();
        data.put("error_message", message);
        data.put("path", path);
        return data;
    }
}
