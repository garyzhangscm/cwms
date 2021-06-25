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

package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.model.WebClientConfiguration;
import com.garyzhangscm.cwms.resources.service.WebClientConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class WebClientConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(WebClientConfigurationController.class);

    @Autowired
    private WebClientConfigurationService webClientConfigurationService;

    @RequestMapping(value = "/web-client-configuration")
    public WebClientConfiguration getSiteInformation(@RequestParam(name = "companyId", required = false, defaultValue = "") Long companyId,
                                                     @RequestParam(name = "warehouseId", required = false, defaultValue = "") Long warehouseId,
                                                     @RequestParam(name = "username", required = false, defaultValue = "") String username) {
        return webClientConfigurationService.getWebClientConfiguration(
                companyId, warehouseId, username
        );
    }




}
