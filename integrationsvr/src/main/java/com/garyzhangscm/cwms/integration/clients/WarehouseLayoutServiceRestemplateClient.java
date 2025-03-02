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

import com.garyzhangscm.cwms.integration.model.Company;
import com.garyzhangscm.cwms.integration.model.Warehouse;

import com.garyzhangscm.cwms.integration.model.WarehouseConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;

@Component
public class WarehouseLayoutServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseLayoutServiceRestemplateClient.class);

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    @Cacheable(cacheNames = "IntegrationService_WarehouseConfiguration", unless="#result == null")
    public WarehouseConfiguration getWarehouseConfiguration(Long warehouseId)   {


        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout//warehouse-configuration/by-warehouse/{id}");
/**
 ResponseBodyWrapper<WarehouseConfiguration> responseBodyWrapper
 = restTemplate.exchange(
 builder.buildAndExpand(warehouseId).toUriString(),
 HttpMethod.GET,
 null,
 new ParameterizedTypeReference<ResponseBodyWrapper<WarehouseConfiguration>>() {}).getBody();

 return responseBodyWrapper.getData();
 **/

        return restTemplateProxy.exchange(
                WarehouseConfiguration.class,
                builder.buildAndExpand(warehouseId).toUriString(),
                HttpMethod.GET,
                null
        );
    }

    @Cacheable(cacheNames = "IntegrationService_Company", unless="#result == null")
    public Company getCompanyByCode(String companyCode) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl("http://apigateway:5555/api/layout/companies")
                        .queryParam("code", companyCode);
/**
        ResponseBodyWrapper<List<Company>> responseBodyWrapper = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Company>>>() {
                }).getBody();

        List<Company> companies = responseBodyWrapper.getData();
 **/
        List<Company> companies = restTemplateProxy.exchangeList(
                Company.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

        if (companies.size() != 1) {
            return null;
        }
        else {
            return companies.get(0);
        }
    }
    @Cacheable(cacheNames = "IntegrationService_Company", unless="#result == null")
    public Company getCompanyById(Long id) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/companies/{id}");

        return restTemplateProxy.exchange(
                Company.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );

    }

    @Cacheable(cacheNames = "IntegrationService_Warehouse", unless="#result == null")
    public Warehouse getWarehouseByName(String companyCode, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl("http://apigateway:5555/api/layout/warehouses")
                        .queryParam("companyCode", companyCode)
                .queryParam("name", name);
/**
        ResponseBodyWrapper<List<Warehouse>> responseBodyWrapper = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Warehouse>>>() {
                }).getBody();

        List<Warehouse> warehouses = responseBodyWrapper.getData();
 **/
        List<Warehouse> warehouses = restTemplateProxy.exchangeList(
                Warehouse.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

        if (warehouses.size() != 1) {
            return null;
        }
        else {
            return warehouses.get(0);
        }
    }

    @Cacheable(cacheNames = "IntegrationService_Warehouse", unless="#result == null")
    public Warehouse getWarehouseByName(Long companyId, String name) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl("http://apigateway:5555/api/layout/warehouses")
                        .queryParam("companyId", companyId)
                        .queryParam("name", name);
/**
        ResponseBodyWrapper<List<Warehouse>> responseBodyWrapper = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Warehouse>>>() {
                }).getBody();

        List<Warehouse> warehouses = responseBodyWrapper.getData();
 **/
        List<Warehouse> warehouses = restTemplateProxy.exchangeList(
                Warehouse.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

        if (warehouses.size() != 1) {
            return null;
        }
        else {
            return warehouses.get(0);
        }
    }

    /**
     * Get the warehouse id by
     * 1. warehouse id
     * 2. Company ID + Warehouse Name
     * 3. Company Code + Warehouse Name
     * @param companyId
     * @param companyCode
     * @param warehouseId
     * @param warehouseName
     * @return
     */
    public Long getWarehouseId(Long companyId, String companyCode,
                                    Long warehouseId, String warehouseName) {

        // If warehouse Id is passed in, then return it
        // otherwise, get warehouse by
        // 1. Company ID + Warehouse Name
        // 2. Company Code + Warehouse Name
        if (Objects.nonNull(warehouseId)) {
            return warehouseId;
        }
        Warehouse warehouse = null;
        if (Objects.nonNull(companyId) && StringUtils.isNotBlank(warehouseName)) {
            warehouse = getWarehouseByName(companyId, warehouseName);

        }
        else if (StringUtils.isNotBlank(companyCode) && StringUtils.isNotBlank(warehouseName)) {
            warehouse = getWarehouseByName(companyCode, warehouseName);
        }

        return Objects.isNull(warehouse) ? null : warehouse.getId();

    }
    @Cacheable(cacheNames = "IntegrationService_Warehouse", unless="#result == null")
    public Warehouse getWarehouseById(Long id) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouses/{id}");

/**
        ResponseBodyWrapper<Warehouse> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Warehouse>>() {}).getBody();

        return responseBodyWrapper.getData();
 **/
        return restTemplateProxy.exchange(
                Warehouse.class,
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.GET,
                null
        );


    }
    public List<Company> getAllCompanies() {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/companies");
        return  restTemplateProxy.exchangeList(
                Company.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }
    public List<Warehouse> getWarehouseByCompany(Long companyId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouses")
                        .queryParam("companyId", companyId);

        logger.debug("Start to get warehouse by companyId: {}, /n >> {}",
                companyId, builder.toUriString());
        return restTemplateProxy.exchangeList(
                Warehouse.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );

    }



}
