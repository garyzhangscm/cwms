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

package com.garyzhangscm.cwms.dblink.controller;


import com.garyzhangscm.cwms.dblink.model.DBBasedWorkOrder;
import com.garyzhangscm.cwms.dblink.service.DBBasedWorkOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;


@RestController
@RequestMapping(value = "/integration-data")
public class IntegrationWorkOrderDataController {


    private static final Logger logger = LoggerFactory.getLogger(IntegrationWorkOrderDataController.class);

    @Autowired
    private DBBasedWorkOrderService dbBasedWorkOrderService;



    @RequestMapping(value="/work-orders", method = RequestMethod.GET)
    public List<DBBasedWorkOrder> getIntegrationWorkOrderData(
            @RequestParam String companyCode,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "")  Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")   String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")   Long id,
            @RequestParam(name = "limit", required = false, defaultValue = "60")  int limit) {

        logger.debug("Start to get work order integration data with parameters");
        logger.debug("companyCode = {}", companyCode);
        logger.debug("warehouseId = {}", Objects.isNull(warehouseId) ? "N/A" : warehouseId);
        logger.debug("startTime = {}", Objects.isNull(startTime) ? "N/A" : startTime);
        logger.debug("endTime = {}", Objects.isNull(endTime) ? "N/A" : endTime);
        logger.debug("date = {}", Objects.isNull(date) ? "N/A" : date);
        logger.debug("statusList = {}", statusList);
        logger.debug("id = {}", Objects.isNull(id) ? "N/A" : id);
        logger.debug("limit = {}", Objects.isNull(limit) ? "N/A" : limit);

        List<DBBasedWorkOrder> dbBasedWorkOrders = dbBasedWorkOrderService.findAll(
                companyCode, warehouseId, startTime, endTime, date, statusList, id,
                limit);

        logger.debug("find {} work order integration data based on the parameters",
                dbBasedWorkOrders.size());

        return dbBasedWorkOrders;
    }


    @RequestMapping(value="/work-orders/{id}/reset-status", method = RequestMethod.POST)
    public DBBasedWorkOrder resetWorkOrderIntegrationStatus(@PathVariable Long id) {


        return dbBasedWorkOrderService.resetStatus(id);
    }


}
