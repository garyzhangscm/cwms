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

package com.garyzhangscm.cwms.workorder.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.JsonMimeInterceptor;
import com.garyzhangscm.cwms.workorder.StatefulRestTemplateInterceptor;
import com.garyzhangscm.cwms.workorder.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;


@Component
public class SiloRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(SiloRestemplateClient.class);

    @Value("${silo.username:paul.harper}")
    String username;
    @Value("${silo.password:@Ecotech123}")
    String password;

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    private RestTemplate restTemplate;

    private RestTemplate getSiloRestTemplate() {

        if (Objects.isNull(restTemplate)) {
            restTemplate = new RestTemplate();
            /**
            List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

            //Add the Jackson Message converter
            MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();

            // Note: here we are making this converter to process any kind of response,
            // not only application/*json, which is the default behaviour
            converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
            messageConverters.add(converter);
            restTemplate.setMessageConverters(messageConverters);

             **/
            restTemplate.setInterceptors(
                    Arrays.asList(new ClientHttpRequestInterceptor[]{
                            new JsonMimeInterceptor(),  new StatefulRestTemplateInterceptor()}));
            // restTemplate.getInterceptors().add(new StatefulRestTemplateInterceptor());
        }

        return restTemplate;
    }


    public SiloDeviceResponseWrapper getSiloDevices(String token) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("https").host("mysilotrackcloud.com")
                        .path("/arch/cfc/controller/Locations.cfc")
                        .queryParam("page", "1")
                        .queryParam("orderBy", "l.name")
                        .queryParam("orderDirection", "ASC")
                        .queryParam("recordsPerPage", "50")
                        .queryParam("date", System.currentTimeMillis())
                .queryParam("method", "getAllDevices");

        HttpEntity<String> entity = getHttpEntity(token, "");

        logger.debug("entity ==============>\n{}", entity);
        ResponseEntity<String> responseBodyWrapper
                = getSiloRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);


        logger.debug("get response from getSiloDevices request: \n {}",
                responseBodyWrapper.getBody());
        try {
            SiloDeviceResponseWrapper siloGroupResponseWrapper =
                    objectMapper.readValue(responseBodyWrapper.getBody(), SiloDeviceResponseWrapper.class);

            return siloGroupResponseWrapper;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }


    }
    public String loginSilo() {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("https").host("mysilotrackcloud.com")
                        .path("/arch/cfc/controller/jwtLogin.cfc")
                        .queryParam("method", "login");


        HttpEntity<String> entity = getHttpEntity("",
                "{\"username\":\"" + username + "\", " +
                        "\"password\":\"" + password + "\"," +
                        "\"rememberme\":\"true\"}");

        logger.debug("entity ==============>\n{}", entity);
        ResponseEntity<String> responseBodyWrapper
                = getSiloRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.POST,
                entity,
                String.class);

        logger.debug("get response from silo login request: \n {}",
                responseBodyWrapper.getBody());
        return responseBodyWrapper.getBody();

    }

    public SiloGroupResponseWrapper getGroups(String token) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("https").host("mysilotrackcloud.com")
                        .path("/arch/cfc/controller/Locations.cfc")
                        .queryParam("method", "getGroups")
                .queryParam("status_cde", "a");


        HttpEntity<String> entity = getHttpEntity(token, "");


        logger.debug("entity ==============>\n{}", entity);
/**
        SiloGroupResponseWrapper responseBodyWrapper
                = getSiloRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<SiloGroupResponseWrapper>() {}).getBody();

**/
        ResponseEntity<String> responseBodyWrapper
                = getSiloRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);


        logger.debug("get response from getGroups request: \n {}",
                responseBodyWrapper.getBody());
        try {
            SiloGroupResponseWrapper siloGroupResponseWrapper =
                    objectMapper.readValue(responseBodyWrapper.getBody(), SiloGroupResponseWrapper.class);

            return siloGroupResponseWrapper;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }


    private HttpEntity<String> getHttpEntity(String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        // MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        // headers.setContentType(type);
        // headers.add("Accept", "application/json, text/plain, */*");
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        headers.add("accept-encoding", "gzip, deflate, br");
        headers.add("authorization", "{\"value\":\"" + token + "\"}");
        return new HttpEntity<>(body, headers);
    }


}
