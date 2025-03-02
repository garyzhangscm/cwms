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


import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
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
import java.util.Objects;

@Component
public class CommonServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(CommonServiceRestemplateClient.class);



    @Autowired
    private RestTemplateProxy restTemplateProxy;

    public List<Client> getAllClients(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/clients")
                        .queryParam("warehouseId", warehouseId);

        return restTemplateProxy.exchangeList(
                Client.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }


    @Cacheable(cacheNames = "IntegrationService_Client", unless="#result == null")
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

    @Cacheable(cacheNames = "IntegrationService_Supplier", unless="#result == null")
    public Supplier getSupplierByName(Long warehouseId, String name) {

        UriComponentsBuilder builder =
                null;
        try {
            builder = UriComponentsBuilder.newInstance()
                    .scheme("http").host("apigateway").port(5555)
                    .path("/api/common/suppliers")
                    .queryParam("warehouseId", warehouseId)
                    .queryParam("name", URLEncoder.encode(name, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
/**
 ResponseBodyWrapper<List<Supplier>> responseBodyWrapper
 = restTemplate.exchange(
 builder.toUriString(),
 HttpMethod.GET,
 null,
 new ParameterizedTypeReference<ResponseBodyWrapper<List<Supplier>>>() {}).getBody();

 List<Supplier> suppliers = responseBodyWrapper.getData();
 **/

        List<Supplier> suppliers = restTemplateProxy.exchangeList(
                Supplier.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

        if (suppliers.size() == 0) {
            return null;
        }
        else {
            return suppliers.get(0);

        }
    }


    @Cacheable(cacheNames = "IntegrationService_UnitOfMeasure", unless="#result == null")
    public UnitOfMeasure getUnitOfMeasureByName(Long companyId,
                                                Long warehouseId, String name) {
        return getUnitOfMeasureByName(companyId, warehouseId, name,
                null, null);
    }
    @Cacheable(cacheNames = "IntegrationService_UnitOfMeasure", unless="#result == null")
    public UnitOfMeasure getUnitOfMeasureByName(Long companyId,
                                                Long warehouseId, String name,
                                                Boolean companyUnitOfMeasure,
                                                Boolean warehouseSpecificUnitOfMeasure) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/unit-of-measures")
                        .queryParam("name", name);

        if (Objects.nonNull(companyId)) {
            builder = builder.queryParam("companyId", companyId);
        }
        if (Objects.nonNull(warehouseId)) {
            builder = builder.queryParam("warehouseId", warehouseId);
        }
        if (Objects.nonNull(companyUnitOfMeasure)) {
            builder = builder.queryParam("companyUnitOfMeasure", companyUnitOfMeasure);
        }
        if (Objects.nonNull(warehouseSpecificUnitOfMeasure)) {
            builder = builder.queryParam("warehouseSpecificUnitOfMeasure", warehouseSpecificUnitOfMeasure);
        }
/**
 ResponseBodyWrapper<List<UnitOfMeasure>> responseBodyWrapper
 = restTemplateProxy.getRestTemplate().exchange(
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


    @Cacheable(cacheNames = "IntegrationService_Customer", unless="#result == null")
    public Customer getCustomerByName(Long companyId, Long warehouseId, String name) {

        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.newInstance()
                            .scheme("http").host("apigateway").port(5555)
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

    @Cacheable(cacheNames = "IntegrationService_Carrier", unless="#result == null")
    public Carrier getCarrierByName(Long warehouseId, String name) throws UnsupportedEncodingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/carriers")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("name", URLEncoder.encode(name, "UTF-8"));
/**
        ResponseBodyWrapper<List<Carrier>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Carrier>>>() {}).getBody();

        List<Carrier> carriers = responseBodyWrapper.getData();
 **/

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
    @Cacheable(cacheNames = "IntegrationService_Carrier", unless="#result == null")
    public Carrier getCarrierById(Long id) {

        // ResponseBodyWrapper<Carrier> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/common/carriers/{id}",
        //        HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Carrier>>() {}, id).getBody();


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/carriers/{id}");

        return restTemplateProxy.exchange(
                Carrier.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );
    }

    @Cacheable(cacheNames = "IntegrationService_CarrierServiceLevel", unless="#result == null")
    public CarrierServiceLevel getCarrierServiceLevelByName(Long warehouseId, String name) throws UnsupportedEncodingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/common/carrier-service-levels")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("name", URLEncoder.encode(name, "UTF-8"));
/**
        ResponseBodyWrapper<List<CarrierServiceLevel>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<CarrierServiceLevel>>>() {}).getBody();

        List<CarrierServiceLevel> carrierServiceLevels = responseBodyWrapper.getData();
   **/


        List<CarrierServiceLevel> carrierServiceLevels = restTemplateProxy.exchangeList(
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


    public Supplier getSupplierByQuickbookListId(Long warehouseId, String quickbookListId) throws UnsupportedEncodingException {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl("http://apigateway:5555/api/common/suppliers")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("quickbookListId", URLEncoder.encode(quickbookListId, "UTF-8"));
        /**
        ResponseBodyWrapper<List<Supplier>> responseBodyWrapper = restTemplate.exchange(builder.toUriString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Supplier>>>() {}).getBody();
        List<Supplier> suppliers = responseBodyWrapper.getData();
**/
        List<Supplier> suppliers = restTemplateProxy.exchangeList(
                Supplier.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

        if (suppliers.size() == 0) {
            throw MissingInformationException.raiseException("can't find supplier with warehouse id " +
                    warehouseId + ", quickbook list id " + quickbookListId);
        }
        else {
            return suppliers.get(0);

        }
    }

}
