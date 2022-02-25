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
import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;


@Component
public class CommonServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(CommonServiceRestemplateClient.class);
    @Autowired
    // OAuth2RestTemplate restTemplate;
    private OAuth2RestOperations restTemplate;


    public Trailer getTrailerById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/trailers/{id}");

        ResponseBodyWrapper<Trailer> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Trailer>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public Client getClientById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/clients/{id}");

        ResponseBodyWrapper<Client> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Client>>() {}).getBody();

        return responseBodyWrapper.getData();

    }
    public Client getClientByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/clients")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("name", name);

        ResponseBodyWrapper<List<Client>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Client>>>() {}).getBody();

        List<Client> clients = responseBodyWrapper.getData();
        if (clients.size() == 0) {
            return null;
        }
        else {
            return clients.get(0);
        }
    }

    public Carrier getCarrierById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/carriers/{id}");

        ResponseBodyWrapper<Carrier> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Carrier>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public CarrierServiceLevel getCarrierServiceLevelById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/carrier-service-levels/{id}");

        ResponseBodyWrapper<CarrierServiceLevel> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<CarrierServiceLevel>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public Customer getCustomerById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/customers/{id}");

        ResponseBodyWrapper<Customer> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Customer>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public Customer getCustomerByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/customers")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("name", name);

        ResponseBodyWrapper<List<Customer>> responseBodyWrapper
                = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                    null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<List<Customer>>>() {}).getBody();

        List<Customer> customers = responseBodyWrapper.getData();
        if (customers.size() == 0) {
            return null;
        }
        else {
            return customers.get(0);
        }
    }


    public UnitOfMeasure getUnitOfMeasureByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/unit-of-measures")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("name", name);

        ResponseBodyWrapper<List<UnitOfMeasure>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<UnitOfMeasure>>>() {}).getBody();

        List<UnitOfMeasure> unitOfMeasures = responseBodyWrapper.getData();

        if (unitOfMeasures.size() != 1) {
            return null;
        }
        else {
            return unitOfMeasures.get(0);
        }
    }

    public UnitOfMeasure getUnitOfMeasureById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/unit-of-measures/{id}");

        ResponseBodyWrapper<UnitOfMeasure> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<UnitOfMeasure>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
    public String getNextNumber(Long warehouseId, String variable) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/system-controlled-number/{variable}/next")
                .queryParam("warehouseId", warehouseId);
        ResponseBodyWrapper<SystemControlledNumber> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(variable).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<SystemControlledNumber>>() {}).getBody();

        return responseBodyWrapper.getData().getNextNumber();
    }


    public TrailerAppointment getTrailerAppointmentById(Long trailerAppointmentId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/trailers/appointments/{id}");

        ResponseBodyWrapper<TrailerAppointment> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(trailerAppointmentId).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<TrailerAppointment>>() {}).getBody();

        return responseBodyWrapper.getData();
    }
}
