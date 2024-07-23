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

import com.garyzhangscm.cwms.resources.model.WebPageTableColumnConfiguration;
import com.garyzhangscm.cwms.resources.service.WebPageTableColumnConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WebPageTableColumnConfigurationController {

    @Autowired
    WebPageTableColumnConfigurationService webPageTableColumnConfigurationService;

    @RequestMapping(value="/web-page-table-column-configuration", method = RequestMethod.GET)
    public List<WebPageTableColumnConfiguration> findAllWebPageTableColumnConfiguration(@RequestParam Long companyId,
                                                                                        @RequestParam(value = "userId", defaultValue = "", required = false) Long userId,
                                                                                        @RequestParam(value = "webPageName", defaultValue = "", required = false) String webPageName,
                                                                                        @RequestParam(value = "tableName", defaultValue = "", required = false) String tableName,
                                                                                        @RequestParam(value = "columnName", defaultValue = "", required = false) String columnName) {
        return webPageTableColumnConfigurationService.findAll(companyId, userId, webPageName, tableName, columnName);
    }


    @RequestMapping(value="/web-page-table-column-configuration/{id}", method = RequestMethod.GET)
    public WebPageTableColumnConfiguration findById(@PathVariable Long id) {
        return webPageTableColumnConfigurationService.findById(id);
    }

    @RequestMapping(value="/web-page-table-column-configuration", method = RequestMethod.PUT)
    public WebPageTableColumnConfiguration addWebPageTableColumnConfiguration(
            @RequestParam Long companyId,
            @RequestBody WebPageTableColumnConfiguration webPageTableColumnConfiguration
    ) {
        webPageTableColumnConfiguration.setCompanyId(companyId);
        return webPageTableColumnConfigurationService.addWebPageTableColumnConfiguration(webPageTableColumnConfiguration);
    }

    @RequestMapping(value="/web-page-table-column-configuration/{id}", method = RequestMethod.POST)
    public WebPageTableColumnConfiguration changeWebPageTableColumnConfiguration(
            @PathVariable Long id,
            @RequestParam Long companyId,
            @RequestBody WebPageTableColumnConfiguration webPageTableColumnConfiguration
    ) {
        webPageTableColumnConfiguration.setCompanyId(companyId);
        return webPageTableColumnConfigurationService.changeWebPageTableColumnConfiguration(id, webPageTableColumnConfiguration);
    }

    @RequestMapping(value="/web-page-table-column-configuration/add-by-page-and-table", method = RequestMethod.PUT)
    public List<WebPageTableColumnConfiguration> addWebPageTableColumnConfigurations(
            @RequestParam Long companyId,
            @RequestBody List<WebPageTableColumnConfiguration> webPageTableColumnConfigurations
    ) {
        webPageTableColumnConfigurations.forEach(
                webPageTableColumnConfiguration -> webPageTableColumnConfiguration.setCompanyId(companyId)
        );
        return webPageTableColumnConfigurationService.addWebPageTableColumnConfigurations(webPageTableColumnConfigurations);
    }

    @RequestMapping(value="/web-page-table-column-configuration/modify-by-page-and-table", method = RequestMethod.POST)
    public List<WebPageTableColumnConfiguration> changeWebPageTableColumnConfigurations(
            @RequestParam Long companyId,
            @RequestBody List<WebPageTableColumnConfiguration> webPageTableColumnConfigurations
    ) {
        webPageTableColumnConfigurations.forEach(
                webPageTableColumnConfiguration -> webPageTableColumnConfiguration.setCompanyId(companyId)
        );
        return webPageTableColumnConfigurationService.changeWebPageTableColumnConfigurations(webPageTableColumnConfigurations);
    }





}
