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

package com.garyzhangscm.cwms.dblink.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.dblink.ResponseBodyWrapper;
import com.garyzhangscm.cwms.dblink.exception.SystemFatalException;
import com.garyzhangscm.cwms.dblink.model.DBBasedInventoryAdjustmentConfirmation;
import com.garyzhangscm.cwms.dblink.model.DBBasedOrderConfirmation;
import com.garyzhangscm.cwms.dblink.model.DBBasedReceiptConfirmation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;


@Component
public class IntegrationServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationServiceRestemplateClient.class);


    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;
    // private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private RestTemplateProxy restTemplateProxy;

    @Value("${cwms_app_server:prod.claytechsuite.com}")
    String appServerURL;

    @Value("${cwms_app_server_scheme:http}")
    String appServerScheme;

    @Value("${cwms_app_server_port:80}")
    String appServerPort;


    @Value("${cwms_company_code:00001}")
    String companyCode;
    @Value("${cwms_warehouse_name:NotExist}")
    String warehouseName;

    /***
    @Value("${integration.host.ip}")
    private String hostIP;
    @Value("${integration.host.port}")
    private String hostPort;
**/

    public <T> String sendIntegrationData(String subUrl, T data)   {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("http").host("apigateway").port(5555)
                        // .scheme("http").host("10.0.10.37").port(32262)

                        // .path("/api/integration/integration-data/dblink/" + subUrl);
                        .path("/api/integration/integration-data/" + subUrl);

        return restTemplateProxy.exchangeForString(
                builder.toUriString(),
                HttpMethod.PUT,
                data
        );
    }

    public String saveIntegrationResult(String subUrl, long id, boolean succeed, String errorMessage) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme(appServerScheme).host(appServerURL).port(appServerPort)
                        // .scheme("http").host("10.0.10.37").port(32262)
                        .path("/api/integration/integration-data/" + subUrl + "/{id}/result")
                        .queryParam("succeed", succeed)
                        .queryParam("errorMessage", errorMessage);


        return restTemplateProxy.exchangeForString(
                builder.buildAndExpand(id).toUriString(),
                HttpMethod.POST,
                null
        );
    }



    public List<DBBasedInventoryAdjustmentConfirmation> getPendingInventoryAdjustmentConfirmationIntegrationData() {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme(appServerScheme).host(appServerURL).port(appServerPort)
                        .path("/api/integration/integration-data/inventory-adjustment-confirmations/query/pending")
                        .queryParam("companyCode", companyCode)
                        .queryParam("warehouseName", warehouseName);


        return restTemplateProxy.exchangeList(
                DBBasedInventoryAdjustmentConfirmation.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }
    public List<DBBasedReceiptConfirmation> getPendingReceiptConfirmationIntegrationData() {

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme(appServerScheme).host(appServerURL).port(appServerPort)
                        .path("/api/integration/integration-data/receipt-confirmations/query/pending")
                        .queryParam("companyCode", companyCode)
                        .queryParam("warehouseName", warehouseName);
/**
        ResponseBodyWrapper<List<DBBasedReceiptConfirmation>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<DBBasedReceiptConfirmation>>>() {}).getBody();

        return responseBodyWrapper.getData();
**/
        return restTemplateProxy.exchangeList(
                DBBasedReceiptConfirmation.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

    public List<DBBasedOrderConfirmation> getPendingSalesOrderConfirmationIntegrationData() {
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme(appServerScheme).host(appServerURL).port(appServerPort)
                        .path("/api/integration/integration-data/order-confirmations/query/pending")
                        .queryParam("companyCode", companyCode)
                        .queryParam("warehouseName", warehouseName);
/**
        ResponseBodyWrapper<List<DBBasedOrderConfirmation>> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<DBBasedOrderConfirmation>>>() {}).getBody();

        return responseBodyWrapper.getData();
 ***/

        return restTemplateProxy.exchangeList(
                DBBasedOrderConfirmation.class,
                builder.toUriString(),
                HttpMethod.GET,
                null
        );
    }

/**
    private HttpEntity<String> getHttpEntity(String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        return new HttpEntity<String>(requestBody, headers);
    }
 ***/

}
