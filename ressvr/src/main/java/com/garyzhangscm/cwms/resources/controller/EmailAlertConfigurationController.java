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

import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.model.EmailAlertConfiguration;
import com.garyzhangscm.cwms.resources.service.EmailAlertConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class EmailAlertConfigurationController {

    @Autowired
    EmailAlertConfigurationService emailAlertConfigurationService;

    @RequestMapping(value="/email-alert-configurations", method = RequestMethod.GET)
    public EmailAlertConfiguration findByCompany(@RequestParam Long companyId) {
        return emailAlertConfigurationService.findByCompany(companyId);
    }


    @RequestMapping(value="/email-alert-configurations/{id}", method = RequestMethod.GET)
    public EmailAlertConfiguration findById(@PathVariable Long id) {
        return emailAlertConfigurationService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/email-alert-configurations", method = RequestMethod.PUT)
    public EmailAlertConfiguration addEmailAlertConfiguration(@RequestBody EmailAlertConfiguration emailAlertConfiguration) {
        return emailAlertConfigurationService.addEmailAlertConfiguration(emailAlertConfiguration);
    }

    @BillableEndpoint
    @RequestMapping(value="/email-alert-configurations/{id}", method = RequestMethod.POST)
    public EmailAlertConfiguration changeEmailAlertConfiguration(@PathVariable Long id,
                                       @RequestBody EmailAlertConfiguration emailAlertConfiguration) {
        return emailAlertConfigurationService.changeEmailAlertConfiguration(id, emailAlertConfiguration);
    }



}
