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
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.service.SiloAPICallHistoryService;
import com.garyzhangscm.cwms.workorder.service.SiloConfigurationService;
import com.garyzhangscm.cwms.workorder.service.SiloDeviceAPICallHistoryService;
import org.apache.logging.log4j.util.Strings;
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

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SiloConfigurationService siloConfigurationService;
    @Autowired
    private SiloAPICallHistoryService siloAPICallHistoryService;
    @Autowired
    private SiloDeviceAPICallHistoryService siloDeviceAPICallHistoryService;

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

    private SiloConfiguration getSileConfiguration(Long warehouseId) {
        SiloConfiguration siloConfiguration = siloConfigurationService.findByWarehouseId(warehouseId);
        if (Objects.isNull(siloConfiguration)) {
            throw WorkOrderException.raiseException("silo system is not configured for the current warehouse");
        }

        return siloConfiguration;
    }

    public String loginSilo(Long warehouseId) {
        SiloConfiguration siloConfiguration = getSileConfiguration(warehouseId);

        RestTemplate restTemplate = new RestTemplate();

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        // .scheme("https").host("mysilotrackcloud.com")
                        .scheme(siloConfiguration.getWebAPIProtocol())
                        .host(siloConfiguration.getWebAPIUrl())
                        .path("/arch/cfc/controller/jwtLogin.cfc")
                        .queryParam("method", "login");


        HttpEntity<String> entity = getHttpEntity("",
                "{\"username\":\"" + siloConfiguration.getWebAPIUsername() + "\", " +
                        "\"password\":\"" + siloConfiguration.getWebAPIPassword() + "\"," +
                        "\"rememberme\":\"true\"}");

        logger.debug("entity for silo login ==============>\n{}", entity);
        ResponseEntity<String> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.POST,
                entity,
                String.class);

        logger.debug("get response from silo login request: \n {}",
                responseBodyWrapper.getBody());

        addSiloAPICallHistory(warehouseId, "login",
                "username=" + siloConfiguration.getWebAPIUsername() + "&rememberme=true",
                responseBodyWrapper.getBody());

        return responseBodyWrapper.getBody();

    }

    public SiloDeviceResponseWrapper getSiloDevices(Long warehouseId, String token) {

        SiloConfiguration siloConfiguration = getSileConfiguration(warehouseId);
        Long currentTimeMills = System.currentTimeMillis();

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        // .scheme("https").host("mysilotrackcloud.com")
                        .scheme(siloConfiguration.getWebAPIProtocol())
                        .host(siloConfiguration.getWebAPIUrl())
                        .path("/arch/cfc/controller/Locations.cfc")
                        .queryParam("page", "1")
                        .queryParam("orderBy", "l.name")
                        .queryParam("orderDirection", "ASC")
                        .queryParam("recordsPerPage", "50")
                        .queryParam("date", currentTimeMills)
                .queryParam("method", "getAllDevices");

        HttpEntity<String> entity = getHttpEntity(token, "");
        String url = builder.toUriString();

        logger.debug("getSiloDevices with entity ==============>\n{}", entity);
        ResponseEntity<String> responseBodyWrapper
                = getSiloRestTemplate().exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class);


        logger.debug("get response from getSiloDevices request: \n {}",
                responseBodyWrapper.getBody());

        addSiloAPICallHistory(warehouseId, "getAllDevices",
                url.substring(url.indexOf("?")),
                responseBodyWrapper.getBody());

        try {
            SiloDeviceResponseWrapper siloDeviceResponseWrapper =
                    objectMapper.readValue(responseBodyWrapper.getBody(), SiloDeviceResponseWrapper.class);

            addSiloDeviceAPICallHistory(warehouseId, currentTimeMills, siloDeviceResponseWrapper);
            return siloDeviceResponseWrapper;
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

    private void addSiloAPICallHistory(Long warehouseId, String method, String parameters, String response) {
        if (Strings.isNotBlank(response) && response.length() > 1000) {
            response = response.substring(0, 1000);
        }
        SiloAPICallHistory siloAPICallHistory = new SiloAPICallHistory(
                warehouseId, method, parameters, response
        );

        siloAPICallHistoryService.addSiloAPICallHistory(siloAPICallHistory);

    }

    private void addSiloDeviceAPICallHistory(Long warehouseId, Long webAPICallTimeStamp,
                                             SiloDeviceResponseWrapper siloDeviceResponseWrapper) {
        if (siloDeviceResponseWrapper.getSiloDevices() == null ||
                siloDeviceResponseWrapper.getSiloDevices().isEmpty()) {
            // we don't have any device information returned from the web api call
            SiloDeviceAPICallHistory siloDeviceAPICallHistory =
                    new SiloDeviceAPICallHistory(warehouseId, webAPICallTimeStamp);

            siloDeviceAPICallHistoryService.addSiloDeviceAPICallHistory(siloDeviceAPICallHistory);

        }
        else {
            siloDeviceResponseWrapper.getSiloDevices().forEach(
                    siloDevice -> {

                        SiloDeviceAPICallHistory siloDeviceAPICallHistory =
                                new SiloDeviceAPICallHistory(warehouseId, webAPICallTimeStamp,
                                        siloDevice.getLocationName(),
                                        siloDevice.getName(),
                                        siloDevice.getDeviceId(),
                                        siloDevice.getMaterial(),
                                        siloDevice.getDistance(),
                                        siloDevice.getTimeStamp(),
                                        siloDevice.getStatusCode(),
                                        siloDeviceResponseWrapper.getToken());

                        siloDeviceAPICallHistoryService.addSiloDeviceAPICallHistory(siloDeviceAPICallHistory);
                    }
            );
        }

    }

}
