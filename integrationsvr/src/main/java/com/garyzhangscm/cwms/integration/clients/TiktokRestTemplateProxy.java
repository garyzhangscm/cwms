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

package com.garyzhangscm.cwms.integration.clients;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.exception.ExceptionCode;
import com.garyzhangscm.cwms.integration.exception.GenericException;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.tiktok.TiktokAPICallResponse;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class TiktokRestTemplateProxy {


    private static final Logger logger = LoggerFactory.getLogger(TiktokRestTemplateProxy.class);


    @Value("${tiktok.appKey:NOT-SET-YET}")
    private String appKey;

    @Value("${tiktok.appSecret:NOT-SET-YET}")
    private String appSecret;

    // for normal API call
    @Value("${tiktok.domain.apiEndpoint:NOT-SET-YET}")
    private String apiEndpointDomain;

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    public <T> T exchange(Class<T> t, String path, List<Pair<String, String>> parameters, HttpMethod method,
                          Object body, String accessToken) {

        String requestBodyString = "";

        try {
            requestBodyString =
                    Objects.isNull(body) ?
                            null : objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GenericException(ExceptionCode.SYSTEM_FATAL_ERROR,
                    GenericException.createDefaultData(e.getMessage()));
        }

        HttpEntity entity = getHttpEntity(requestBodyString, accessToken);

        String signature =
                getSignature(path, parameters, requestBodyString);

        String uri = getUrl(path, parameters, signature);

        logger.debug("start to call api {} with uri\n{}", path, uri);

        RestTemplate restTemplate = new RestTemplate();
        TiktokAPICallResponse response = restTemplate.exchange(
                uri,
                method,
                entity,
                TiktokAPICallResponse.class).getBody();

        logger.debug("Get response: {}", response);

        if (response.getCode() != 0) {
            throw new GenericException(ExceptionCode.SYSTEM_FATAL_ERROR,
                    GenericException.createDefaultData(response.getMessage()));
        }

        try {

            // response.getData() is of type linkedHashMap
            // we will need to cast the data into json format , then
            // cast the JSON back to the POJO
            String json = objectMapper.writeValueAsString(response.getData());
            logger.debug("after cast to JSON: \n {}", json);

            return objectMapper.readValue(json, t);
            // logger.debug("resultT class is {}", resultT.getClass().getName());
            // return resultT;

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GenericException(ExceptionCode.SYSTEM_FATAL_ERROR,
                    GenericException.createDefaultData(e.getMessage()));
        }
    }

    public <T> List<T> exchangeList(Class<T> t, String path, List<Pair<String, String>> parameters,
                                    HttpMethod method,
                                    Object body, String accessToken) {
        String requestBodyString = "";

        try {
            requestBodyString =
                    Objects.isNull(body) ?
                            null : objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GenericException(ExceptionCode.SYSTEM_FATAL_ERROR,
                    GenericException.createDefaultData(e.getMessage()));
        }

        HttpEntity entity = getHttpEntity(requestBodyString, accessToken);

        String signature =
                getSignature(path, parameters, requestBodyString);

        String uri = getUrl(path, parameters, signature);

        RestTemplate restTemplate = new RestTemplate();
        TiktokAPICallResponse response = restTemplate.exchange(
                uri,
                method,
                entity,
                TiktokAPICallResponse.class).getBody();

        logger.debug("get result from uri {}\n{}",
                uri, response);
        if (response.getCode() != 0) {
            throw new GenericException(ExceptionCode.SYSTEM_FATAL_ERROR,
                    GenericException.createDefaultData(response.getMessage()));
        }

        try {

            // response.getData() is of type linkedHashMap
            // we will need to cast the data into json format , then
            // cast the JSON back to the POJO
            String json = objectMapper.writeValueAsString(response.getData());
            logger.debug("after cast to JSON: \n {}", json);

            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, t));
            // logger.debug("resultT class is {}", resultT.getClass().getName());
            // return resultT;

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GenericException(ExceptionCode.SYSTEM_FATAL_ERROR,
                    GenericException.createDefaultData(e.getMessage()));
        }
    }

    private HttpEntity<String> getHttpEntity(String requestBody, String accessToken) {

        MediaType mediaType = MediaType.parseMediaType("application/json; charset=UTF-8");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        headers.add("x-tts-access-token", accessToken);
        return new HttpEntity<String>(requestBody, headers);
    }

    private String getUrl(String path, List<Pair<String, String>> parameters, String sign){
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("https").host(apiEndpointDomain)
                        .path(path)
                        .queryParam("sign", sign);

        parameters.forEach(
                parameter -> builder.queryParam(
                        parameter.getFirst(), parameter.getSecond()
                )
        );
        return builder.toUriString();
    }

    /**
     * Calculate the signature for tiktok API Call
     * Follow the link to see how to generate the signature
     * https://partner.tiktokshop.com/docv2/page/64f199709495ef0281851fd0#Back%20To%20Top
     * @return
     */
    public String getSignature(String path, List<Pair<String, String>> parameters, String requestBody) {

        logger.debug("start to calculate the signature for parameters \n{}", parameters);
        logger.debug("path: {}", path);
        logger.debug("requestbody: {}", requestBody);
        // 1. params except 'sign' & 'access_token'
        parameters = parameters.stream().filter(
                parameter -> !parameter.getFirst().equalsIgnoreCase("sign") &&
                        !parameter.getFirst().equalsIgnoreCase("access_token")
        ).collect(Collectors.toList());

        // 2. reorder the parameters' key in alphabetical order
        Collections.sort(parameters, Comparator.comparing(Pair::getFirst));
        logger.debug("after sorted the parameters are\n{}", parameters);

        // 3. Concatenate all the parameters in the format of {key}{value}
        String input = parameters.stream().map(
                parameter -> parameter.getFirst() + parameter.getSecond()
        ).collect(Collectors.joining());
        logger.debug("Concatenate all the parameters: \n{}", input);

        // 4. append the request path
        input = path + input;

        // 5. append the body, if there's body
        if (!Strings.isBlank(requestBody)) {
            input = input + requestBody;
        }

        // 6. wrap the string generated in step 5 with the App secret
        input = appSecret + input + appSecret;

        logger.debug("we will start to run SHA256 algorithm on the final string \n{}",
                input);
        
        // String sign = generateSHA256(input, appSecret);
        String sign = "";
        try {
            sign = encode(appSecret, input);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        logger.debug("get sign \n{} from input \n{} and secret\n{}",
                sign, input, appSecret);
        return sign;
    }

    public static String encode(String key, String data) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        sha256_HMAC.init(secret_key);

        logger.debug("Hex value: {}", Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes("UTF-8"))));
        // logger.debug("Base64 value: {}", Base64.encodeBase64String(sha256_HMAC.doFinal(data.getBytes("UTF-8"))));

        return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes("UTF-8")));

    }

    private String generateSHA256(String input, String appSecret) {
        // encode the digest byte stream in hexadecimal and use sha256 to generate sign with salt(secret)
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            // use the app secret as salt
            logger.debug("");
            digest.update(appSecret.getBytes());
            byte[] result = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(result);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw ResourceNotFoundException.raiseException("Can't process SHA256 algorithm on the tiktok API call");
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
