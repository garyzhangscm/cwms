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

import com.garyzhangscm.cwms.resources.PrinterConfiguration;
import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.model.Printer;
import com.garyzhangscm.cwms.resources.model.ReportType;
import com.garyzhangscm.cwms.resources.service.ReportHistoryService;
import net.sf.jasperreports.engine.json.expression.filter.FilterExpression;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

@Component
public class PrintingServiceRestemplateClient  {

    private static final Logger logger
            = LoggerFactory.getLogger(PrintingServiceRestemplateClient.class);

    @Autowired
    private PrinterConfiguration printerConfiguration;

    public List<Printer> getPrinters(String name, String printerType) throws URISyntaxException {

        String url = printerConfiguration.getUrl() + "/printers";

        logger.debug("start to get printers from the server {}, \n filter by name: {}, type {}",
                url,
                Strings.isBlank(name) ? "N/A" : name,
                Strings.isBlank(printerType) ? "N/A" : name);

        UriComponentsBuilder builder =
                UriComponentsBuilder.newInstance()
                        .uri(new URI(url));
        if (Strings.isNotBlank(name)) {
            builder = builder.queryParam("name", name);
        }
        if (Strings.isNotBlank(printerType)) {
            builder = builder.queryParam("printerType", printerType);
        }

        logger.debug("start to get printers from URL {}",
                builder.toUriString());

        RestTemplate restTemplate = new RestTemplate();


        List<Printer> printers
                = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Printer>>() {}).getBody();
        return printers;


    }
    public void sendPrintingRequest(File file, ReportType reportType, String printer, int copies,
                                    Boolean collated) {
        logger.debug("Start to send file {} to printing server: {}, copies: {}",
                file.getName(), printerConfiguration.getUrl(), copies);

        logger.debug("Report type {} is label? {}",
                reportType, reportType.isLabel());
        String url = printerConfiguration.getUrl() + "/printing/" +
                (reportType.isLabel() ? "label" : "pdf") +
                "?copies=" + copies;
        // if printer is specified, add printer to the parameters
        if (Strings.isNotBlank(printer)) {
            url +="&printer=" + printer;
        }

        if (Boolean.TRUE.equals(collated)) {
            url +="&collated=true";
        }
        else {

            url +="&collated=false";
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
