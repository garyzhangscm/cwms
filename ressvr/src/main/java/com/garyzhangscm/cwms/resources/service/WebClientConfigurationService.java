/**
 * Copyright 2018
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

package com.garyzhangscm.cwms.resources.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.WebClientTabDisplayConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class WebClientConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(WebClientConfigurationService.class);
    @Autowired
    private WebClientTabDisplayConfigurationService webClientTabDisplayConfigurationService;

    @Autowired
    private UserService userService;

    public WebClientConfiguration getWebClientConfiguration(Long companyId, Long warehouseId, Long userId) {
        return getWebClientConfiguration(
                companyId, warehouseId, userService.findById(userId)
        );
    }
    public WebClientConfiguration getWebClientConfiguration(Long companyId, Long warehouseId, String username) {
        return getWebClientConfiguration(
                companyId, warehouseId, userService.findByUsername(companyId, username)
        );
    }
    public WebClientConfiguration getWebClientConfiguration(Long companyId, Long warehouseId, User user) {
        logger.debug("Start to get web client tab display configuration for {} / {} / {}",
                companyId, warehouseId, user.getName());
        Collection<WebClientTabDisplayConfiguration> webClientTabDisplayConfigurations =
                webClientTabDisplayConfigurationService
                        .getWebClientTabDisplayConfigurationService(companyId, warehouseId, user);

        Map<String, Boolean> tabDisplayConfiguration = new HashMap<>();

        webClientTabDisplayConfigurations.forEach(webClientTabDisplayConfiguration -> {
            tabDisplayConfiguration.put(
                    webClientTabDisplayConfiguration.getName(),
                    webClientTabDisplayConfiguration.getDisplayFlag()
            );
        });

        WebClientConfiguration webClientConfiguration = new WebClientConfiguration();
        webClientConfiguration.setTabDisplayConfiguration(
                tabDisplayConfiguration
        );
        return webClientConfiguration;


    }

}
