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
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;


@Component
public class CommonServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(CommonServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    @Cacheable(cacheNames = "OutboundService_Trailer", unless="#result == null")
    public Trailer getTrailerById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/trailers/{id}");
/**
        ResponseBodyWrapper<Trailer> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Trailer>>() {}).getBody();

        return responseBodyWrapper.getData();
**/

        return restTemplateProxy.exchange(
                Trailer.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    @Cacheable(cacheNames = "OutboundService_Client", unless="#result == null")
    public Client getClientById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/clients/{id}");
/**
        ResponseBodyWrapper<Client> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
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
    @Cacheable(cacheNames = "OutboundService_Client", unless="#result == null")
    public Client getClientByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/clients")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("name", name);
/**
        ResponseBodyWrapper<List<Client>> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Client>>>() {}).getBody();

        List<Client> clients = responseBodyWrapper.getData();
**/
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
    @Cacheable(cacheNames = "OutboundService_Supplier", unless="#result == null")
    public Supplier getSupplierById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/suppliers/{id}");
/**
        ResponseBodyWrapper<Supplier> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Supplier>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Supplier.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }
    @Cacheable(cacheNames = "OutboundService_Carrier", unless="#result == null")
    public Carrier getCarrierByName(Long warehouseId, String name) throws UnsupportedEncodingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/carriers")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("name", URLEncoder.encode(name, "UTF-8"));


        List<Carrier> carriers = restTemplateProxy.exchangeList(
                Carrier.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
        if (carriers.size() == 0) {
            return null;
        }
        else {
            return carriers.get(0);
        }
    }

    @Cacheable(cacheNames = "OutboundService_Carrier", unless="#result == null")
    public Carrier getCarrierById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/carriers/{id}");
/**
        ResponseBodyWrapper<Carrier> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Carrier>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                Carrier.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }
    @Cacheable(cacheNames = "OutboundService_CarrierServiceLevel", unless="#result == null")
    public CarrierServiceLevel getCarrierServiceLevelByName(Long warehouseId, String name) throws UnsupportedEncodingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/carrier-service-levels")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("name", URLEncoder.encode(name, "UTF-8"));


        List<CarrierServiceLevel> carrierServiceLevels =
                restTemplateProxy.exchangeList(
                        CarrierServiceLevel.class,
                        builder.toUriString(),
                        HttpMethod.GET,
                        null
                );
        if (carrierServiceLevels.size() == 0) {
            return null;
        }
        else {
            return carrierServiceLevels.get(0);
        }
    }

    @Cacheable(cacheNames = "OutboundService_CarrierServiceLevel", unless="#result == null")
    public CarrierServiceLevel getCarrierServiceLevelById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/carrier-service-levels/{id}");
/**
        ResponseBodyWrapper<CarrierServiceLevel> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<CarrierServiceLevel>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                CarrierServiceLevel.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );
    }

    @Cacheable(cacheNames = "OutboundService_Customer", unless="#result == null")
    public Customer getCustomerById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/customers/{id}");
/**
        ResponseBodyWrapper<Customer> responseBodyWrapper
                = restTemplate.exchange(
                        builder.buildAndExpand(id).toUriString(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ResponseBodyWrapper<Customer>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                Customer.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );
    }
    @Cacheable(cacheNames = "OutboundService_Customer", unless="#result == null")
    public Customer getCustomerByName(Long companyId, Long warehouseId, String name) {

        logger.debug("Start to find customer by name {}", name);
        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("zuulserver").port(5555)
                            .path("/api/common/customers")
                            .queryParam("name", URLEncoder.encode(name, "UTF-8"));

            if (Objects.nonNull(companyId)) {
                builder = builder.queryParam("companyId", companyId);
            }
            if (Objects.nonNull(warehouseId)) {
                builder = builder.queryParam("warehouseId", warehouseId);
            }
            /**
            ResponseBodyWrapper<List<Customer>> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<Customer>>>() {
                    }).getBody();

            List<Customer> customers = responseBodyWrapper.getData();
             **/

            List<Customer> customers = restTemplateProxy.exchangeList(
                    Customer.class,
                    builder.build(true).toUriString(),
                    HttpMethod.GET,
                    null
            );
            if (customers.size() == 0) {
                return null;
            } else {
                return customers.get(0);
            }
        }
        catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            throw ResourceNotFoundException.raiseException("can't find the customer by name " + name);
        }

    }


    @Cacheable(cacheNames = "OutboundService_UnitOfMeasure", unless="#result == null")
    public UnitOfMeasure getUnitOfMeasureByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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
        List<UnitOfMeasure> unitOfMeasures = restTemplateProxy.exchangeList(
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

    @Cacheable(cacheNames = "OutboundService_UnitOfMeasure", unless="#result == null")
    public UnitOfMeasure getUnitOfMeasureById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
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
    public String getNextNumber(Long warehouseId, String variable) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/system-controlled-number/{variable}/next")
                .queryParam("warehouseId", warehouseId);

            logger.debug("We will try the rest template with OAuth first");
/**
            ResponseBodyWrapper<SystemControlledNumber> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.buildAndExpand(variable).toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<SystemControlledNumber>>() {}).getBody();

            return responseBodyWrapper.getData().getNextNumber();
**/

            SystemControlledNumber systemControlledNumber = restTemplateProxy.exchange(
                    SystemControlledNumber.class,
                    builder.buildAndExpand(variable).toUriString(),
                    HttpMethod.GET,
                    null
            );
            return systemControlledNumber.getNextNumber();
    }


    @Cacheable(cacheNames = "OutboundService_TrailerAppointment", unless="#result == null")
    public TrailerAppointment getTrailerAppointmentById(Long trailerAppointmentId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/trailers/appointments/{id}");
/**
        ResponseBodyWrapper<TrailerAppointment> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(trailerAppointmentId).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<TrailerAppointment>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                TrailerAppointment.class,
                builder.buildAndExpand(trailerAppointmentId).toUriString(),
                HttpMethod.GET,
                null
        );
    }

    @Cacheable(cacheNames = "OutboundService_TrailerAppointment", unless="#result == null")
    public TrailerAppointment getTrailerAppointmentByNumber(Long warehouseId, String number) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/trailer-appointments")
                .queryParam("warehouseId", warehouseId);
        if (Strings.isNotBlank(number)) {
            builder = builder.queryParam("number", number);
        }

/**
            ResponseBodyWrapper<List<TrailerAppointment>> responseBodyWrapper
                    = restTemplateProxy.getRestTemplate().exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<TrailerAppointment>>>() {}).getBody();

            List<TrailerAppointment> trailerAppointments =
                    responseBodyWrapper.getData();

**/
            List<TrailerAppointment> trailerAppointments = restTemplateProxy.exchangeList(
                TrailerAppointment.class,
                builder.toUriString(),
                HttpMethod.GET,
                    null
            );

            if (trailerAppointments.isEmpty() || trailerAppointments.size() > 1) {
                return null;
            }
            else {
                return trailerAppointments.get(0);
            }
    }
    public TrailerAppointment addTrailerAppointment(Long warehouseId, String trailerNumber,
                                                    String trailerAppointmentNumber,
                                                    String trailerAppointmentDescription,
                                                    TrailerAppointmentType type) throws UnsupportedEncodingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/trailer-appointments/new")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("trailerNumber", URLEncoder.encode(trailerNumber, "UTF-8") )
                        .queryParam("number", URLEncoder.encode(trailerAppointmentNumber, "UTF-8") )
                        .queryParam("type", type);
        if (Strings.isNotBlank(trailerAppointmentDescription)) {
            builder = builder.queryParam("description", URLEncoder.encode(trailerAppointmentDescription, "UTF-8") );
        }

/**
        ResponseBodyWrapper<TrailerAppointment> responseBodyWrapper
                = restTemplateProxy.getRestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<TrailerAppointment>>() {}).getBody();

        return responseBodyWrapper.getData();

**/
        return restTemplateProxy.exchange(
                TrailerAppointment.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );
    }

    public TrailerAppointment changeTrailerAppointmentStatus(Long trailerAppointmentId,
                                                          TrailerAppointmentStatus trailerAppointmentStatus) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/trailer-appointments/{id}/change-status")
                .queryParam("status", trailerAppointmentStatus);
/**
        ResponseBodyWrapper<TrailerAppointment> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(trailerAppointmentId).toUriString(),
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<TrailerAppointment>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchange(
                TrailerAppointment.class,
                builder.buildAndExpand(trailerAppointmentId).toUriString(),
                HttpMethod.POST,
                null
        );
    }

    @Cacheable(cacheNames = "OutboundService_Unit", unless="#result == null")
    public List<Unit> getAllUnits(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/units")
                        .queryParam("warehouseId", warehouseId);

        return restTemplateProxy.exchangeList(
                Unit.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public TrailerAppointment createTrailerAppointment(Long warehouseId, String load) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/trailer-appointments/new")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("trailerNumber", load)
                        .queryParam("number", load)
                        .queryParam("description", load)
                        .queryParam("type", TrailerAppointmentType.SHIPPING);

        return restTemplateProxy.exchange(
                TrailerAppointment.class,
                builder.toUriString(),
                HttpMethod.POST,
                null
        );
    }
}
