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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.ResponseBodyWrapper;
import com.garyzhangscm.cwms.workorder.exception.GenericException;
import com.garyzhangscm.cwms.workorder.model.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component

public class OutboundServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(OutboundServiceRestemplateClient.class);

    @Autowired
    OAuth2RestTemplate restTemplate;


    private ObjectMapper mapper = new ObjectMapper();

    public AllocationResult allocateWorkOrder(WorkOrder workOrder) throws IOException {

        String requestBody = mapper.writeValueAsString(workOrder);

        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        HttpEntity<String> httpEntity = new HttpEntity<String>(requestBody, headers);

        ResponseBodyWrapper<AllocationResult> responseBodyWrapper = restTemplate.exchange(
                "http://zuulserver:5555/api/outbound/allocation/work-order",
                HttpMethod.POST, httpEntity,
                new ParameterizedTypeReference<ResponseBodyWrapper<AllocationResult>>() {}).getBody();

        return responseBodyWrapper.getData();

    }

    public List<Pick> getWorkOrderPicks(WorkOrder workOrder) throws IOException {

        StringBuilder url = new StringBuilder()
                            .append("http://zuulserver:5555/api/outbound/picks?")
                            .append("workOrderLineIds={workOrderLineIds}");

        ResponseBodyWrapper<List<Pick>> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<Pick>>>() {},
                getWorkOrderLineIds(workOrder)).getBody();

        return responseBodyWrapper.getData();
    }

    public List<ShortAllocation> getWorkOrderShortAllocations(WorkOrder workOrder)  {

        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/outbound/shortAllocations?")
                .append("workOrderLineIds={workOrderLineIds}");

        ResponseBodyWrapper<List<ShortAllocation>> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null,
                new ParameterizedTypeReference<ResponseBodyWrapper<List<ShortAllocation>>>() {},
                getWorkOrderLineIds(workOrder)).getBody();

        return responseBodyWrapper.getData();
    }

    private String getWorkOrderLineIds(WorkOrder workOrder) {
        return workOrder.getWorkOrderLines().stream()
                  .map(WorkOrderLine::getId).map(String::valueOf).collect(Collectors.joining(","));

    }

    public List<Pick> getWorkOrderLinePicks(WorkOrderLine workOrderLine)  {

        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/outbound/picks?")
                .append("workOrderLineId={workOrderLineId}");

        ResponseBodyWrapper< List<Pick>> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null,
                new ParameterizedTypeReference<ResponseBodyWrapper< List<Pick>>>() {},
                workOrderLine.getId()).getBody();

        return responseBodyWrapper.getData();
    }

    public List<ShortAllocation> getWorkOrderLineShortAllocations(WorkOrderLine workOrderLine) {

        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/outbound/shortAllocations?")
                .append("workOrderLineId={workOrderLineId}");

        ResponseBodyWrapper<List<ShortAllocation>> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null,
                new ParameterizedTypeReference<ResponseBodyWrapper< List<ShortAllocation>>>() {},
                workOrderLine.getId()).getBody();

        return responseBodyWrapper.getData();
    }

}
