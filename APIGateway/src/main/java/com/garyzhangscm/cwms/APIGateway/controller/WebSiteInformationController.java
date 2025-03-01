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

package com.garyzhangscm.cwms.APIGateway.controller;

import com.garyzhangscm.cwms.APIGateway.AccessControlFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
// @RequestMapping(value = "/api/resource/site-information/default")
@RequestMapping(value = "/xxxx/site-information/default")
public class WebSiteInformationController {

    final Logger logger =
            LoggerFactory.getLogger(WebSiteInformationController.class);
    @Autowired
    private Environment env;

    @RequestMapping(method =  RequestMethod.GET)
    public String getBasicApplicationInformation() {
        logger.debug("return site information");

        return "{\n" +
                "  \"app\": {\n" +
                "    \"name\": \"WMS - Auth\",\n" +
                "    \"description\": \"Cloud based WMS. Fueled by NG-ZORRO & NG-Alain\"\n" +
                "    \"version\": \"1\"\n" +
                "  }" +
                "}";
    }
}
