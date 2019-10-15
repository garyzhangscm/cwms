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

package com.garyzhangscm.cwms.layout.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/config")
public class ConfigurationController {

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping(method = RequestMethod.GET)
    public String getConfiguration() {
        String response = "error";
        try {
            URI uri = new URI("http://192.168.99.100:8888/layoutservice/default");
            response = restTemplate.getForObject(uri,
                    String.class );

        }
        catch(URISyntaxException ex) {}
        return response;
    }
    @RequestMapping(value = "/eureka", method = RequestMethod.GET)
    public String getConfigurationByEureka() {
        String response = "error";
        try {
            URI uri = new URI("http://CONFIGSERVER:8888/layoutservice/default");
            response = restTemplate.getForObject(uri,
                    String.class );

        }
        catch(URISyntaxException ex) {}
        return response;
    }


}
