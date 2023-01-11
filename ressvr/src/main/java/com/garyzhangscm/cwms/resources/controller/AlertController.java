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

import com.garyzhangscm.cwms.resources.model.Alert;
import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
public class AlertController {

    @Autowired
    AlertService alertService;

    @RequestMapping(value="/alerts", method = RequestMethod.GET)
    public List<Alert> findAllAlerts(@RequestParam Long companyId,
                                     @RequestParam(value = "type", defaultValue = "", required = false) String type,
                                     @RequestParam(value = "status", defaultValue = "", required = false) String status,
                                     @RequestParam(value = "keyWords", defaultValue = "", required = false) String keyWords,
                                     @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
                                     @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endTime,
                                     @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return alertService.findAll(companyId, type, status, keyWords, startTime, endTime, date);
    }


    @RequestMapping(value="/alerts/{id}", method = RequestMethod.GET)
    public Alert findById(@PathVariable Long id) {
        return alertService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/alerts/{id}/reset", method = RequestMethod.POST)
    public Alert resetAlert(@PathVariable Long id) {
        return alertService.resetAlert(id);
    }



}
