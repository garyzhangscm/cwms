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
import com.garyzhangscm.cwms.resources.service.AlertSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AlertSubscriptionController {

    @Autowired
    AlertSubscriptionService alertSubscriptionService;

    @RequestMapping(value="/alert-subscriptions", method = RequestMethod.GET)
    public List<AlertSubscription> findAllAlertSubscription(@RequestParam Long companyId,
                                                            @RequestParam(value = "username", defaultValue = "", required = false) String username,
                                                            @RequestParam(value = "type", defaultValue = "", required = false) String type,
                                                            @RequestParam(value = "deliveryChannel", defaultValue = "", required = false) String deliveryChannel) {
        return alertSubscriptionService.findAll(companyId, username, type, deliveryChannel);
    }


    @RequestMapping(value="/alert-subscriptions/{id}", method = RequestMethod.GET)
    public AlertSubscription findById(@PathVariable Long id) {
        return alertSubscriptionService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/alert-subscriptions", method = RequestMethod.PUT)
    public AlertSubscription addAlertSubscription(@RequestBody AlertSubscription alertSubscription) {
        return alertSubscriptionService.addAlertSubscription(alertSubscription);
    }

    @BillableEndpoint
    @RequestMapping(value="/alert-subscriptions/{id}", method = RequestMethod.DELETE)
    public AlertSubscription removeAlertSubscription(@PathVariable Long id) {
        return alertSubscriptionService.removeAlertSubscription(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/alert-subscriptions/subscribe", method = RequestMethod.POST)
    public AlertSubscription subscribe(@RequestParam Long companyId,
                                                  @RequestParam String alertType,
                                                  @RequestParam String alertDeliveryChannel,
                                                  @RequestParam String username) {
        return alertSubscriptionService.subscribe(
                companyId, alertType, alertDeliveryChannel, username
        );
    }

    @BillableEndpoint
    @RequestMapping(value="/alert-subscriptions/unsubscribe", method = RequestMethod.POST)
    public AlertSubscription unsubscribe(@RequestParam Long companyId,
                                       @RequestParam String alertType,
                                       @RequestParam String alertDeliveryChannel,
                                       @RequestParam String username) {
        return alertSubscriptionService.unsubscribe(
                companyId, alertType, alertDeliveryChannel, username
        );
    }

    @BillableEndpoint
    @RequestMapping(value="/alert-subscriptions/change-key-words", method = RequestMethod.POST)
    public AlertSubscription changeKeyWords(@RequestParam Long companyId,
                                         @RequestParam String alertType,
                                         @RequestParam String alertDeliveryChannel,
                                         @RequestParam String username,
                                         @RequestParam String keyWordsList) {
        return alertSubscriptionService.changeKeyWords(
                companyId, alertType, alertDeliveryChannel, username, keyWordsList
        );
    }

}
