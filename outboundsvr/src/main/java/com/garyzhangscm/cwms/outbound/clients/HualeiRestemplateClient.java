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

package com.garyzhangscm.cwms.outbound.clients;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.exception.ExceptionCode;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.model.hualei.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class HualeiRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(HualeiRestemplateClient.class);


    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplateProxy restTemplateProxy;


    @Value("${outbound.order.document.folder}")
    private String orderDocumentFolder;

    public ShipmentResponse sendHualeiShippingRequest(HualeiConfiguration hualeiConfiguration,
                                                      ShipmentRequest shipmentRequest)   {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme(hualeiConfiguration.getCreateOrderProtocol())
                        .host(hualeiConfiguration.getCreateOrderHost())
                        .port(hualeiConfiguration.getCreateOrderPort())
                        .path(hualeiConfiguration.getCreateOrderEndpoint());

        HttpEntity entity = null;
        try {
            entity = getHttpEntity(shipmentRequest);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GenericException(ExceptionCode.SYSTEM_FATAL_ERROR,
                    GenericException.createDefaultData("can't process the hualei shipment request"));
        }

        String response = restTemplateProxy.getRestTemplate().exchange(
                 builder.toUriString(),
                 HttpMethod.POST,
                 entity,
                 String.class).getBody();

        ShipmentResponse shipmentResponse = null;
        try {
            shipmentResponse = objectMapper.readValue(response, ShipmentResponse.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new GenericException(ExceptionCode.SYSTEM_FATAL_ERROR,
                    GenericException.createDefaultData("can't decode the response from hualei"));
        }
        try {
            shipmentResponse.setMessage(
                    java.net.URLDecoder.decode(shipmentResponse.getMessage(), StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return shipmentResponse;
    }



    public File getHualeiShippingLabelFile(Long warehouseId,
                                           Long orderId,
                                           HualeiConfiguration hualeiConfiguration,
                                           ShippingLabelFormat shippingLabelFormat,
                                           String hualeiOrderId)   {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme(hualeiConfiguration.getPrintLabelProtocol())
                        .host(hualeiConfiguration.getPrintLabelHost())
                        .port(hualeiConfiguration.getPrintLabelPort())
                        .path(hualeiConfiguration.getPrintLabelEndpoint())
                        .queryParam("PrintType", shippingLabelFormat)
                        .queryParam("order_id", hualeiOrderId);


        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
        HttpEntity<String> entity = new HttpEntity<>(headers);


        ResponseEntity<byte[]> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                byte[].class);

        logger.debug("get response with status {}", response.getStatusCode());

        if(response.getStatusCode().equals(HttpStatus.OK))
        {
            byte[] shippingLabelFile = response.getBody();
            String fileName = response.getHeaders().getContentDisposition().getFilename();

            logger.debug("start to download file {} with size {}",
                    fileName, shippingLabelFile.length);

            String directory = getUploadOrderDocumentFilePath(warehouseId, orderId) + fileName;
            logger.debug("save to folder and file {}",
                    directory);
            Path path = Paths.get(directory);
            try {
                Files.write(path, shippingLabelFile);
                logger.debug("shipping label saved!");
                return path.toFile();
            } catch (IOException e) {
                e.printStackTrace();
                throw new GenericException(ExceptionCode.SYSTEM_FATAL_ERROR,
                        GenericException.createDefaultData("can't save file to " + directory));
            }

        }

        return null;
    }

    private String getUploadOrderDocumentFilePath(Long warehouseId, Long orderId) {

        if (!orderDocumentFolder.endsWith("/")) {
            return orderDocumentFolder + "/" + warehouseId + "/" + orderId + "/";
        }
        else  {

            return orderDocumentFolder + warehouseId + "/" + orderId + "/";
        }
    }

    public List<HualeiTrackStatusResponseData> refreshHualeiPackageStatus(String trackingNumbers, HualeiConfiguration hualeiConfiguration) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme(hualeiConfiguration.getGetPackageStatusProtocol())
                        .host(hualeiConfiguration.getGetPackageStatusHost())
                        .port(hualeiConfiguration.getGetPackageStatusPort())
                        .path(hualeiConfiguration.getGetPackageStatusEndpoint())
                        .queryParam("documentCode", trackingNumbers);


        String response = restTemplateProxy.getRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                String.class).getBody();
        logger.debug("get response for tracking Hualei tracking number \n{}",
                response);

        /**
        List<HualeiTrackResponse> trackResponses =  restTemplateProxy.exchangeList(
                HualeiTrackResponse.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
         **/

        List<HualeiTrackStatusResponse> trackResponses = null;
        try {
            trackResponses = objectMapper.readValue(response,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, HualeiTrackStatusResponse.class));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw OrderOperationException.raiseException("fail to get tracking number status from hualei");
        }

        return trackResponses.stream().map(HualeiTrackStatusResponse::getData)
                .flatMap(List::stream).collect(Collectors.toList());
    }

    public List<HualeiTrackNumberResponseData> refreshHualeiPackageTrackingNumbers(String orderIds, HualeiConfiguration hualeiConfiguration) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme(hualeiConfiguration.getGetTrackingNumberProtocol())
                        .host(hualeiConfiguration.getGetTrackingNumberHost())
                        .port(hualeiConfiguration.getGetTrackingNumberPort())
                        .path(hualeiConfiguration.getGetTrackingNumberEndpoint())
                        .queryParam("order_id", orderIds);


        String response = restTemplateProxy.getRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                String.class).getBody();
        logger.debug("get response for get Hualei tracking number \n{}",
                response);

        /**
         List<HualeiTrackResponse> trackResponses =  restTemplateProxy.exchangeList(
         HualeiTrackResponse.class,
         builder.toUriString(),
         HttpMethod.GET,
         null
         );
         **/

        List<HualeiTrackNumberResponse> trackNumberResponses = null;
        try {
            trackNumberResponses = objectMapper.readValue(response,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, HualeiTrackNumberResponse.class));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw OrderOperationException.raiseException("fail to get tracking number status from hualei");
        }

        return trackNumberResponses.stream().map(HualeiTrackNumberResponse::getData)
                .flatMap(List::stream).collect(Collectors.toList());
    }

    private HttpEntity<LinkedMultiValueMap<String, String>> getHttpEntity(ShipmentRequest shipmentRequest) throws JsonProcessingException {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());

        LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("param", objectMapper.writeValueAsString(shipmentRequest.getShipmentRequestParameters()));
        // parameters.add("param", getParameters(shipmentRequest));
        parameters.add("getTrackingNumber", shipmentRequest.getGetTrackingNumber());

        HttpEntity<LinkedMultiValueMap<String, String>> requestEntity =
                new HttpEntity<>(parameters, headers);

        return requestEntity;
    }

}
