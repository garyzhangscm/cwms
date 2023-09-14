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
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.model.lightMES.LightMESConfiguration;
import com.garyzhangscm.cwms.workorder.model.lightMES.LightMESResponseWrapper;
import com.garyzhangscm.cwms.workorder.model.lightMES.LightStatus;
import com.garyzhangscm.cwms.workorder.model.lightMES.Machine;
import com.garyzhangscm.cwms.workorder.service.LightMESConfigurationService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@Component
public class LightMESRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(LightMESRestemplateClient.class);

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LightMESConfigurationService lightMESConfigurationService;


    private RestTemplate restTemplate;

    private RestTemplate getSiloRestTemplate() {

        if (Objects.isNull(restTemplate)) {
            restTemplate = new RestTemplate();

            restTemplate.setInterceptors(
                    Arrays.asList(new ClientHttpRequestInterceptor[]{
                            new JsonMimeInterceptor()}));
            // restTemplate.getInterceptors().add(new StatefulRestTemplateInterceptor());
        }

        return restTemplate;
    }

    private LightMESConfiguration getLightMESConfiguration(Long warehouseId) {
        LightMESConfiguration lightMESConfiguration = lightMESConfigurationService.findByWarehouse(warehouseId);
        if (Objects.isNull(lightMESConfiguration)) {
            throw WorkOrderException.raiseException("Light MES system is not configured for the current warehouse");
        }
        if (Strings.isBlank(lightMESConfiguration.getAccessKeyId())) {
            throw WorkOrderException.raiseException("Access Key ID is not setup for Light MES system in the current warehouse");
        }
        if (Strings.isBlank(lightMESConfiguration.getAccessKeySecret())) {
            throw WorkOrderException.raiseException("Access Key Secret is not setup for Light MES system in the current warehouse");
        }
        if (Strings.isBlank(lightMESConfiguration.getHost())) {
            throw WorkOrderException.raiseException("Host is not setup for Light MES system in the current warehouse");
        }

        if (Strings.isBlank(lightMESConfiguration.getProtocol())) {
            lightMESConfiguration.setProtocol("http");
        }
        if (Strings.isBlank(lightMESConfiguration.getPort())) {
            lightMESConfiguration.setPort("80");
        }

        return lightMESConfiguration;
    }


    public LightStatus getSingleLightStatus(Long warehouseId, String sim) {

        LightMESConfiguration lightMESConfiguration = getLightMESConfiguration(warehouseId);
        if (Strings.isBlank(lightMESConfiguration.getSingleLightStatusQueryUrl())) {

            throw WorkOrderException.raiseException("Endpoint for query single light status is not setup for Light MES system in the current warehouse");
        }

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme(lightMESConfiguration.getProtocol())
                        .host(lightMESConfiguration.getHost())
                        .port(lightMESConfiguration.getPort())
                        .path(lightMESConfiguration.getSingleLightStatusQueryUrl());

        HttpEntity<String> entity = getHttpEntity(lightMESConfiguration.getAccessKeyId(),
                lightMESConfiguration.getAccessKeySecret(),
                "{ \"sim\": \"" + sim + "\"}");
        String url = builder.toUriString();

        ResponseEntity<String> responseBodyWrapper
                = getSiloRestTemplate().exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class);


        logger.debug("get response from getSingleLightStatus request: \n {}",
                responseBodyWrapper.getBody());

        try {
            LightMESResponseWrapper<LightStatus> lightMESResponseWrapper =
                    objectMapper.readValue(responseBodyWrapper.getBody(), LightMESResponseWrapper.class);


            logger.debug("status for light {} is {}",
                    lightMESResponseWrapper.getData().getSim(),
                    lightMESResponseWrapper.getData().getCurrentState());
            return lightMESResponseWrapper.getData();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }


    }

    public List<Machine> getMachineList(Long warehouseId) {

        LightMESConfiguration lightMESConfiguration = getLightMESConfiguration(warehouseId);
        if (Strings.isBlank(lightMESConfiguration.getMachineListQueryUrl())) {

            throw WorkOrderException.raiseException("Endpoint for query machine list is not setup for Light MES system in the current warehouse");
        }

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme(lightMESConfiguration.getProtocol())
                        .host(lightMESConfiguration.getHost())
                        .port(lightMESConfiguration.getPort())
                        .path(lightMESConfiguration.getMachineListQueryUrl());

        HttpEntity<String> entity = getHttpEntity(lightMESConfiguration.getAccessKeyId(),
                lightMESConfiguration.getAccessKeySecret(),
                "{ \"pageNum\": 1, \"pageSize\": 100 }");
        String url = builder.toUriString();

        ResponseEntity<String> responseBodyWrapper
                = getSiloRestTemplate().exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class);


        logger.debug("get response from getMachineList request: \n {}",
                responseBodyWrapper.getBody());

        try {
            LightMESResponseWrapper<List<Machine>> lightMESResponseWrapper =
                    objectMapper.readValue(responseBodyWrapper.getBody(), LightMESResponseWrapper.class);


            for(Machine machine : lightMESResponseWrapper.getData()) {

                logger.debug("machine {}: mid - {}, sim - {}, status - {}",
                        machine.getMachineNo(),
                        machine.getMid(),
                        machine.getSim(),
                        machine.getStatus());
            }
            return lightMESResponseWrapper.getData();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }


    }

    private HttpEntity<String> getHttpEntity(String accessKeyId, String accessKeySecret, String body) {
        HttpHeaders headers = new HttpHeaders();
        // MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        // headers.setContentType(type);
        // headers.add("Accept", "application/json, text/plain, */*");
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        // headers.add("accept-encoding", "gzip, deflate, br");
        headers.add("AccessKeyId", accessKeyId);
        headers.add("AccessKeySecret", accessKeySecret);
        return new HttpEntity<>(body, headers);
    }



}
