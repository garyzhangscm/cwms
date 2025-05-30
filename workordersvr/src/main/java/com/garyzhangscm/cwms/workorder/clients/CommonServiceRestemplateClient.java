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

import com.garyzhangscm.cwms.workorder.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

@Component
public class CommonServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(CommonServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    public String getNextNumber(Long warehouseId, String variable) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/system-controlled-number/{variable}/next")
                .queryParam("warehouseId", warehouseId);
        /**
        ResponseBodyWrapper<SystemControlledNumber> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(variable).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<SystemControlledNumber>>() {}).getBody();

        return responseBodyWrapper.getData().getNextNumber();
         **/

        return restTemplateProxy.exchange(
                SystemControlledNumber.class,
                builder.buildAndExpand(variable).toUriString(),
                HttpMethod.GET,
                null
        ).getNextNumber();

    }
    public String getNextWorkOrderNumber(Long warehouseId) {
        return getNextNumber(warehouseId, "work-order-number");
    }

    @Cacheable(cacheNames = "WorkOrderService_Client", unless="#result == null")
    public Client getClientById(Long id) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/clients/{id}");

/**
        ResponseBodyWrapper<Client> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Client>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Client.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );


    }


    @Cacheable(cacheNames = "WorkOrderService_Client", unless="#result == null")
    public Client getClientByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                null;
        try {
            builder = UriComponentsBuilder.newInstance()
                    .scheme("http").host("apigateway").port(5555)
                    .path("/api/common/clients")
                    .queryParam("warehouseId", warehouseId)
                    .queryParam("name", URLEncoder.encode(name, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        List<Client> clients = restTemplateProxy.exchangeList(
                Client.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

        if (clients.size() == 0) {
            return null;
        }
        else {
            return clients.get(0);
        }
    }


    @Cacheable(cacheNames = "WorkOrderService_UnitOfMeasure", unless="#result == null")
    public UnitOfMeasure getUnitOfMeasureById(Long id) {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/unit-of-measures/{id}");

/**
        ResponseBodyWrapper<UnitOfMeasure> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<UnitOfMeasure>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                UnitOfMeasure.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );


    }
    @Cacheable(cacheNames = "WorkOrderService_UnitOfMeasure", unless="#result == null")
    public UnitOfMeasure getUnitOfMeasureByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/unit-of-measures")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("name", name);
/**
        ResponseBodyWrapper<List<UnitOfMeasure>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<UnitOfMeasure>>>() {}).getBody();

        List<UnitOfMeasure> unitOfMeasures = responseBodyWrapper.getData();
 **/

        List<UnitOfMeasure> unitOfMeasures =  restTemplateProxy.exchangeList(
                UnitOfMeasure.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );


        if (unitOfMeasures.size() != 1) {
            return null;
        }
        else {
            return unitOfMeasures.get(0);
        }
    }

    public List<String> getNextNumberInBatch(Long warehouseId, String variable, int batch) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/system-controlled-number/{variable}/batch/next")
                        .queryParam("batch", batch)
                        .queryParam("warehouseId", warehouseId);
        /**
        ResponseBodyWrapper<List<String>> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(variable).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<String>>>() {}).getBody();


        return responseBodyWrapper.getData();
         **/

        return restTemplateProxy.exchangeList(
                String.class,
                builder.buildAndExpand(variable).toUriString(),
                HttpMethod.GET,
                null
        );
    }

    @Cacheable(cacheNames = "OutboundService_Unit", unless="#result == null")
    public List<Unit> getUnitsByWarehouse(Long warehouseId) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/units")
                        .queryParam("warehouseId", warehouseId);

        return restTemplateProxy.exchangeList(
                Unit.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }


}
