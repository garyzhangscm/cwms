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

import com.garyzhangscm.cwms.workorder.ResponseBodyWrapper;
import com.garyzhangscm.cwms.workorder.model.AllocationResult;
import com.garyzhangscm.cwms.workorder.model.SiloInformationResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


@Component
public class SiloRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(SiloRestemplateClient.class);


    public String getSiloInformation(String token) {
        RestTemplate restTemplate = new RestTemplate();
        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .scheme("https").host("mysilotrackcloud.com")
                        .path("/arch/cfc/controller/Locations.cfc")
                        .queryParam("page", "1")
                        .queryParam("orderBy", "l.name")
                        .queryParam("orderDirection", "ASC")
                        .queryParam("recordsPerPage", "50")
                        .queryParam("date", "1675808007371")
                .queryParam("method", "getAllDevices");


        HttpEntity<String> entity = getHttpEntity(token);

        logger.debug("entity ==============>\n{}", entity);
        ResponseEntity<String> responseBodyWrapper
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);

        logger.debug("get response from silo request: \n {}",
                responseBodyWrapper.getBody());
        return responseBodyWrapper.getBody();

    }


    private HttpEntity<String> getHttpEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        // MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        // headers.setContentType(type);
        // headers.add("Accept", "application/json, text/plain, */*");
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        headers.add("accept-encoding", "gzip, deflate, br");
        headers.add("authorization", "{\"value\":\"" + token + "\"}");
        return new HttpEntity<String>("", headers);
    }


}
