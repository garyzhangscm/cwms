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

import com.garyzhangscm.cwms.resources.model.AlertSubscription;
import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.model.WebMessageAlert;
import com.garyzhangscm.cwms.resources.service.AlertSubscriptionService;
import com.garyzhangscm.cwms.resources.service.WebMessageAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WebMessageAlertController {

    @Autowired
    WebMessageAlertService webMessageAlertService;

    @RequestMapping(value="/web-message-alerts", method = RequestMethod.GET)
    public List<WebMessageAlert> findAllWebMessageAlert(@RequestParam Long companyId,
                                                        @RequestParam(value = "alertId", defaultValue = "", required = false) Long alertId,
                                                        @RequestParam(value = "username", defaultValue = "", required = false) String username,
                                                        @RequestParam(value = "readFlag", defaultValue = "", required = false) Boolean readFlag,
                                                        @RequestParam(value = "pi", defaultValue = "", required = false) Integer pageNumber,
                                                        @RequestParam(value = "ps", defaultValue = "", required = false) Integer pageSize) {
        return webMessageAlertService.findAll(companyId, username, alertId, readFlag, pageNumber, pageSize);
    }


    @RequestMapping(value="/web-message-alerts/new-message-count", method = RequestMethod.GET)
    public Integer getNewWebMessageAlertCount(@RequestParam Long companyId,
                                           String username) {
        return webMessageAlertService.getUserUnreadWebMessageAlert(companyId, username).size();
    }

    @RequestMapping(value="/web-message-alerts/{id}/read", method = RequestMethod.POST)
    public WebMessageAlert readWebMessageAlert(@RequestParam Long companyId,
                                               @PathVariable Long id) {
        return webMessageAlertService.readWebMessageAlert(id);
    }


}
