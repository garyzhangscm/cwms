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

package com.garyzhangscm.cwms.resources.clients;

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.service.ReportHistoryService;
import net.sf.jasperreports.engine.json.expression.filter.FilterExpression;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.util.Arrays;

@Component
public class PrintingServiceRestemplateClient  {

    private static final Logger logger
            = LoggerFactory.getLogger(PrintingServiceRestemplateClient.class);

    private final String PRINTING_SERVER_URL = "http://10.0.10.5:10888/printing/pdf";

    public void sendPrintingRequest(File file, String printer) {

        logger.debug("Start to send file {} to printing server: {}",
                file.getName(), PRINTING_SERVER_URL);

        String url = PRINTING_SERVER_URL;
        // if printer is specified, add printer to the parameters
        if (Strings.isNotBlank(printer)) {
            url +="?printer=" + printer;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        LinkedMultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("file", new FileSystemResource(file));


        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(params, headers);


        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate
                .postForEntity(url, requestEntity, String.class);

        logger.debug("get response for printing request: \n > status: {}  \n > body: {}",
                response.getStatusCodeValue(),
                response.getBody());


    }

}
