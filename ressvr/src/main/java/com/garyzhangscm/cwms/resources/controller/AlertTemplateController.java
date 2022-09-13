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

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.model.Alert;
import com.garyzhangscm.cwms.resources.model.AlertTemplate;
import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.service.AlertService;
import com.garyzhangscm.cwms.resources.service.AlertTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class AlertTemplateController {

    @Autowired
    AlertTemplateService alertTemplateService;

    @RequestMapping(value="/alert-templates", method = RequestMethod.GET)
    public List<AlertTemplate> findAllAlertTemplates(@RequestParam Long companyId,
                                                     @RequestParam(value = "type", defaultValue = "", required = false) String type,
                                                     @RequestParam(value = "deliveryChannel", defaultValue = "", required = false) String deliveryChannel) {
        return alertTemplateService.findAll(companyId, type, deliveryChannel);
    }


    @RequestMapping(value="/alert-templates/{id}", method = RequestMethod.GET)
    public AlertTemplate findById(@PathVariable Long id) {
        return alertTemplateService.findById(id);
    }



    @RequestMapping(value="/alert-templates", method = RequestMethod.PUT)
    @BillableEndpoint
    public AlertTemplate addAlertTemplate(@RequestParam Long companyId,
                                          @RequestBody AlertTemplate alertTemplate) {
        return alertTemplateService.addAlertTemplate(alertTemplate);
    }
    @RequestMapping(value="/alert-templates/{id}", method = RequestMethod.POST)
    @BillableEndpoint
    public AlertTemplate changeAlertTemplate(@RequestParam Long companyId,
                                          @PathVariable Long id,
                                          @RequestBody AlertTemplate alertTemplate) {
        return alertTemplateService.changeAlertTemplate(id, alertTemplate);
    }

    @RequestMapping(value="/alert-templates/{id}", method = RequestMethod.DELETE)
    @BillableEndpoint
    public ResponseBodyWrapper removeAlertTemplate(@RequestParam Long companyId,
                                                   @PathVariable Long id) {
        alertTemplateService.removeAlertTemplate(id);
        return ResponseBodyWrapper.success("alert template with id " + id + " is removed");
    }



}
