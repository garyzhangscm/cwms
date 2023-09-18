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
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.lightMES.*;
import com.garyzhangscm.cwms.workorder.service.LightMESConfigurationService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


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


    public List<LightStatus> getLightStatusInBatch(Long warehouseId, List<String> simList) {

        LightMESConfiguration lightMESConfiguration = getLightMESConfiguration(warehouseId);
        if (Strings.isBlank(lightMESConfiguration.getMachineListQueryUrl())) {

            throw WorkOrderException.raiseException("Endpoint for query machine list is not setup for Light MES system in the current warehouse");
        }

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme(lightMESConfiguration.getProtocol())
                        .host(lightMESConfiguration.getHost())
                        .port(lightMESConfiguration.getPort())
                        .path(lightMESConfiguration.getBatchLightStatusQueryUrl());

        String requestBody = "{ \"simList\":[";
        requestBody += simList.stream().map(sim -> "\"" + sim + "\"").collect(Collectors.joining(","));
        requestBody += "]}";

        logger.debug("will get current state for a list of sim: \n {}", requestBody);

        HttpEntity<String> entity = getHttpEntity(lightMESConfiguration.getAccessKeyId(),
                lightMESConfiguration.getAccessKeySecret(),
                requestBody);
        String url = builder.toUriString();

        logger.debug("start to send getLightStatusInBatch request with entity \n {}",
                entity);

        LightMESResponseWrapper lightMESResponseWrapper
                = getSiloRestTemplate().exchange(
                url,
                HttpMethod.POST,
                entity,
                LightMESResponseWrapper.class).getBody();
        try {
            if (!Boolean.TRUE.equals(lightMESResponseWrapper.getSuccess())) {
                throw WorkOrderException.raiseException("Error " + lightMESResponseWrapper.getMessage() +
                        " while try to get machine list");
            }

            String json = objectMapper.writeValueAsString(lightMESResponseWrapper.getData());
            List<LightStatus> lightStatuses = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, LightStatus.class));

            return lightStatuses;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }


    }

    public int getSingleLightPulseByTimeRange(Long warehouseId,
                                           ZonedDateTime startTime, ZonedDateTime endTime,
                                           String sim) {

        logger.debug("start to get pulse count for sim {}, within time range [{}, {}]",
                sim,
                startTime, endTime);

        LightMESConfiguration lightMESConfiguration = getLightMESConfiguration(warehouseId);
        if (Strings.isBlank(lightMESConfiguration.getSingleLightPulseQueryUrl())) {

            throw WorkOrderException.raiseException("Endpoint for query single light pulse count is not setup for Light MES system in the current warehouse");
        }

        ZoneId zoneId = ZoneId.systemDefault();
        if (Strings.isNotBlank(lightMESConfiguration.getTimeZone())) {
            zoneId = ZoneId.of(lightMESConfiguration.getTimeZone());
        }
        logger.debug("will convert the time into Light MES's zone {}", zoneId);

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme(lightMESConfiguration.getProtocol())
                        .host(lightMESConfiguration.getHost())
                        .port(lightMESConfiguration.getPort())
                        .path(lightMESConfiguration.getSingleLightPulseQueryUrl());


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        HttpEntity<String> entity = getHttpEntity(lightMESConfiguration.getAccessKeyId(),
                lightMESConfiguration.getAccessKeySecret(),
                "{ \"sim\": \"" + sim + "\", " +
                      "  \"startTime\": \"" + startTime.withZoneSameInstant(zoneId).format(formatter) + "\", " +
                        "  \"endTime\": \"" + endTime.withZoneSameInstant(zoneId).format(formatter) + "\"" +
                      "}");

        logger.debug("start to send getSingleLightPulseByTimeRange request with entity \n {}",
                entity);

        String url = builder.toUriString();

        LightMESResponseWrapper lightMESResponseWrapper
                = getSiloRestTemplate().exchange(
                url,
                HttpMethod.POST,
                entity,
                LightMESResponseWrapper.class).getBody();
        try {
            if (!Boolean.TRUE.equals(lightMESResponseWrapper.getSuccess())) {
                throw WorkOrderException.raiseException("Error " + lightMESResponseWrapper.getMessage() +
                        " while try to get machine " +  sim + "'s pulse count");
            }

            String json = objectMapper.writeValueAsString(lightMESResponseWrapper.getData());
            logger.debug("Get result {} for getSingleLightPulseByTimeRange", json);
            PulseCount pulseCount = objectMapper.readValue(json, PulseCount.class);

            return pulseCount.getCountSize();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return 0;
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

        logger.debug("start to send getMachineList request with entity \n {}",
                entity);

        LightMESResponseWrapper lightMESResponseWrapper
                = getSiloRestTemplate().exchange(
                url,
                HttpMethod.POST,
                entity,
                LightMESResponseWrapper.class).getBody();


        try {


            if (!Boolean.TRUE.equals(lightMESResponseWrapper.getSuccess())) {
                throw WorkOrderException.raiseException("Error " + lightMESResponseWrapper.getMessage() +
                        " while try to get machine list");
            }

            String json = objectMapper.writeValueAsString(lightMESResponseWrapper.getData());
            List<Machine> machines = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Machine.class));

            for(Machine machine : machines) {

                logger.debug("machine {}: mid - {}, sim - {}, status - {}",
                        machine.getMachineNo(),
                        machine.getMid(),
                        machine.getSim(),
                        machine.getStatus());
            }
            return machines;
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
        headers.add("Content-Type", "application/json");
        // headers.add("accept-encoding", "gzip, deflate, br");
        headers.add("AccessKeyId", accessKeyId);
        headers.add("AccessKeySecret", accessKeySecret);
        return new HttpEntity<>(body, headers);
    }



}
