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

import com.garyzhangscm.cwms.integration.ResponseBodyWrapper;
import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;

@Component
public class CommonServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(CommonServiceRestemplateClient.class);

    @Autowired
    RestTemplate restTemplate;

    public Client getClientById(Long id) {

        ResponseBodyWrapper<Client> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/common/clients/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Client>>() {}, id).getBody();

        return responseBodyWrapper.getData();

    }
    public Client getClientByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder
                        .fromHttpUrl("http://zuulserver:5555/api/common/clients")
                        .queryParam("warehouseId", warehouseId)
                .queryParam("name", name);

        ResponseBodyWrapper<List<Client>> responseBodyWrapper = restTemplate.exchange(builder.toUriString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Client>>>() {}).getBody();
        List<Client> clients = responseBodyWrapper.getData();
        if (clients.size() == 0) {
            return null;
        }
        else {
            return clients.get(0);
        }
    }
    public Supplier getSupplierById(Long id) {

        ResponseBodyWrapper<Supplier> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/common/suppliers/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Supplier>>() {}, id).getBody();

        return responseBodyWrapper.getData();
    }
    public Supplier getSupplierByName(Long warehouseId, String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl("http://zuulserver:5555/api/common/suppliers")
                        .queryParam("warehouseId", warehouseId)
                .queryParam("name", name);
        ResponseBodyWrapper<List<Supplier>> responseBodyWrapper = restTemplate.exchange(builder.toUriString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Supplier>>>() {}).getBody();
        List<Supplier> suppliers = responseBodyWrapper.getData();
        if (suppliers.size() == 0) {
            throw MissingInformationException.raiseException("can't find supplier with warehouse id " +
                    warehouseId + ", name " + name);
        }
        else {
            return suppliers.get(0);

        }
    }

    public UnitOfMeasure getUnitOfMeasureById(Long id) {

        ResponseBodyWrapper<UnitOfMeasure> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/common/unit-of-measures/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<UnitOfMeasure>>() {}, id).getBody();

        return responseBodyWrapper.getData();
    }

    public UnitOfMeasure getUnitOfMeasureByName(Long companyId, Long warehouseId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/unit-of-measures")
                        .queryParam("warehouseId", warehouseId)
                        .queryParam("companyId", companyId)
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


    public Customer getCustomerByName(Long companyId, Long warehouseId, String name) {

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
            ResponseBodyWrapper<List<Customer>> responseBodyWrapper
                    = restTemplate.exchange(
                    builder.build(true).toUri(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ResponseBodyWrapper<List<Customer>>>() {
                    }).getBody();

            List<Customer> customers = responseBodyWrapper.getData();
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

    public Carrier getCarrierByName(String name) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("zuulserver").port(5555)
                        .path("/api/common/carriers")
                        .queryParam("name", name);

        ResponseBodyWrapper<List<Carrier>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Carrier>>>() {}).getBody();

        List<Carrier> carriers = responseBodyWrapper.getData();
        if (carriers.size() == 0) {
            return null;
        }
        else {
            return carriers.get(0);
        }
    }
    public Carrier getCarrierById(Long id) {

        ResponseBodyWrapper<Carrier> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/common/carriers/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<Carrier>>() {}, id).getBody();

        return responseBodyWrapper.getData();
    }


}
