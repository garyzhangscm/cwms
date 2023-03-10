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

package com.garyzhangscm.cwms.outbound.controller;

import com.garyzhangscm.cwms.outbound.model.BillableActivity;
import com.garyzhangscm.cwms.outbound.model.ClientRestriction;
import com.garyzhangscm.cwms.outbound.model.ClientValidationEndpoint;
import com.garyzhangscm.cwms.outbound.service.OrderBillableActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
public class OrderBillableActivitiesController {
    private static final Logger logger = LoggerFactory.getLogger(OrderBillableActivitiesController.class);
    @Autowired
    private OrderBillableActivityService orderBillableActivityService;



    @ClientValidationEndpoint
    @RequestMapping(value="/order-billable-activities/billable-activity", method = RequestMethod.GET)
    public List<BillableActivity> findBillableActivities(@RequestParam Long warehouseId,
                                                         @RequestParam Long clientId,
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
                                                         @RequestParam(name="includeLineActivity", required = false, defaultValue = "true") Boolean includeLineActivity,
                                                         ClientRestriction clientRestriction) {
        return orderBillableActivityService.findBillableActivities(warehouseId, clientId, startTime,
                endTime, includeLineActivity,  clientRestriction);
    }



}
