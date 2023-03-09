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

package com.garyzhangscm.cwms.inbound.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.inbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
public class ReceiptBillableActivitiesController {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptBillableActivitiesController.class);
    @Autowired
    private ReceiptBillableActivityService receiptBillableActivityService;



    @ClientValidationEndpoint
    @RequestMapping(value="/receipt-billable-activities/billable-activity", method = RequestMethod.GET)
    public List<BillableActivity> findBillableActivities(@RequestParam Long warehouseId,
                                         @RequestParam Long clientId,
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
                                         @RequestParam(name="includeLineActivity", required = false, defaultValue = "true") Boolean includeLineActivity,
                                         ClientRestriction clientRestriction) {
        return receiptBillableActivityService.findBillableActivities(warehouseId, clientId, startTime,
                endTime, includeLineActivity,  clientRestriction);
    }



}
