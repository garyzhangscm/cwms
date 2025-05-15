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

package com.garyzhangscm.cwms.zuulsvr.clients;

import com.garyzhangscm.cwms.zuulsvr.ResponseBodyWrapper;
import com.garyzhangscm.cwms.zuulsvr.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

@Component
public class LayoutServiceRestemplateClient {

    @Autowired
    private RestTemplate restTemplate;



    /**
     * Please note since Redis will always return int when the value is within the int range
     * we will need to change the return type to Number and may need to cast when we call this command
     * @param warehouseId
     * @return
     */
    @Cacheable(cacheNames = "ZuulService_CompanyByWarehouseId", unless="#result == null")
    public Number getCompanyIdByWarehouseId(Long warehouseId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/warehouses/{warehouseId}/company-id");

        ResponseBodyWrapper<Long> responseBodyWrapper
                = restTemplate.exchange(
                builder.buildAndExpand(warehouseId).toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<Long>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    @Cacheable(cacheNames = "ZuulService_Company", unless="#result == null")
    public Company getCompanyByCode(String companyCode) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        .path("/api/layout/companies")
                .queryParam("code", companyCode);

        ResponseBodyWrapper<List<Company>> responseBodyWrapper =
                restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<List<Company>>>() {
                    }).getBody();

        List<Company> companies = responseBodyWrapper.getData();
        if (companies.size() != 1) {
            return null;
        }
        else {
            return companies.get(0);
        }
    }

}
