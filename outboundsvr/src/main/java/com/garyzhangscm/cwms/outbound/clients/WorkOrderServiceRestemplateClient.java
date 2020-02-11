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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WorkOrderServiceRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(WorkOrderServiceRestemplateClient.class);
    private ObjectMapper mapper = new ObjectMapper();
    @Autowired
    OAuth2RestTemplate restTemplate;

    public WorkOrder getWorkOrderById(Long id) {

        ResponseBodyWrapper<WorkOrder> responseBodyWrapper = restTemplate.exchange("http://zuulserver:5555/api/workorder/work-orders/{id}",
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrder>>() {}, id).getBody();

        return responseBodyWrapper.getData();

    }

    public WorkOrder registerPickCancelled(Long workOrderId, Long cancelledQuantity){

        StringBuilder url = new StringBuilder()
                .append("http://zuulserver:5555/api/workorder/work-orders/{id}/pick-cancelled?")
                .append("cancelledQuantity={cancelledQuantity}");
        ResponseBodyWrapper<WorkOrder> responseBodyWrapper = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ResponseBodyWrapper<WorkOrder>>() {},
                workOrderId, cancelledQuantity).getBody();

        return responseBodyWrapper.getData();
    }


}
