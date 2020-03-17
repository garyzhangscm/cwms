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

package com.garyzhangscm.cwms.workorder;

import com.garyzhangscm.cwms.workorder.exception.ExceptionResponse;
import com.garyzhangscm.cwms.workorder.exception.GenericException;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * We will use this controller advice to process
 * 1. any exceptions and return JSON with exception information
 * 2. JSON with data
 * No matter whether there's exception or not, the return JSON will always in the
 * format of
 * {
 *     result: 0,
 *     message: "",
 *     data: any type of data
 * }
 * while
 * ** Result: 0 = no error, otherwise, error code
 * ** message: empty string = no error, otherwise error message
 * ** data: when no error, we will return the JSON format of the return object. When there's error, empty string
 */
@RestControllerAdvice
public class BaseGlobalResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    /**
     * Only process when the client want to have a JSON return
     */
    private final List<MediaType> jsonMediaTypes = Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8);

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @ExceptionHandler(GenericException.class)
    public ResponseBodyWrapper defaultErrorHandler(GenericException ex, HttpServletRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(ex, request.getRequestURI());
        return new ResponseBodyWrapper(
                ex.getExceptionCode().getCode(),
                ex.getExceptionCode().getMessage(), exceptionResponse);
    }

    @Override
    public Object beforeBodyWrite(
            Object obj, MethodParameter methodParameter, MediaType mediaType,
            Class<? extends HttpMessageConverter<?>> converterType,
            ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        //Only process when the client request a JSON response
        if (!jsonMediaTypes.contains(mediaType)) {
            return obj;
        }
        // Only process when the result is not in the right format yet
        // Note when there's exception from the method, we will still get here and
        // the obj will be an instance of LinkedHashMap with error information and
        // html error code
        // eg:
        // {
        //     timestamp: xxxxx,
        //     status: 500
        //     code:   this is the exception code
        //     message:  this is the exception message
        // }
        // After this method(beforeBodyWrite) returns, spring will still call
        // the @ExceptionHandler marked method to process the exceptions.
        if (obj == null || !(obj instanceof ResponseBodyWrapper)) {
            obj = new ResponseBodyWrapper(0, "", obj);
        }
        return obj;
    }

}