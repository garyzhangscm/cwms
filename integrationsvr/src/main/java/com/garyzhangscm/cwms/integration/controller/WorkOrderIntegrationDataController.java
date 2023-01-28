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

package com.garyzhangscm.cwms.integration.controller;


import com.garyzhangscm.cwms.integration.ResponseBodyWrapper;
import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.service.IntegrationDataService;
import org.apache.logging.log4j.util.Strings;
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
public class WorkOrderIntegrationDataController {


    private static final Logger logger = LoggerFactory.getLogger(WorkOrderIntegrationDataController.class);

    @Autowired
    private IntegrationDataService integrationDataService;



    //
    // Work Order Related
    //
    @RequestMapping(value="/work-orders", method = RequestMethod.GET)
    public List<? extends IntegrationWorkOrderData> getIntegrationWorkOrderData(
            @RequestParam String companyCode,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "")  Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")   String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")   Long id) {

        return integrationDataService.getWorkOrderData(
                companyCode, warehouseId, startTime, endTime, date, statusList, id);
    }

    @RequestMapping(value="/work-orders/{id}", method = RequestMethod.GET)
    public IntegrationWorkOrderData getIntegrationWorkOrderData(@PathVariable Long id) {

        return integrationDataService.getWorkOrderData(id);
    }
    @RequestMapping(value="/work-orders/{id}/resend", method = RequestMethod.POST)
    public IntegrationWorkOrderData resendWorkOrderData(@PathVariable Long id) {

        return integrationDataService.resendWorkOrderData(id);
    }

    @RequestMapping(value="/work-orders", method = RequestMethod.PUT)
    public ResponseBodyWrapper saveIntegrationWorkOrderData(
            @RequestBody DBBasedWorkOrder dbBasedWorkOrder
    ){


        IntegrationWorkOrderData workOrderData =
                integrationDataService.addIntegrationWorkOrderData(dbBasedWorkOrder);

        return ResponseBodyWrapper.success(String.valueOf(workOrderData.getId()));
    }

    /**
    @RequestMapping(value="/dblink/work-order", method = RequestMethod.PUT)
    public ResponseBodyWrapper saveIntegrationWorkOrderData(
            @RequestBody DBBasedWorkOrder dbBasedWorkOrder
    ){

        logger.debug("Start to save dbbased work order into database \n{}",
                dbBasedWorkOrder);
        integrationDataService.addIntegrationWorkOrderData(dbBasedWorkOrder);
        return ResponseBodyWrapper.success("success");
    }
            */

    //
    // Work Order Confirmation Related
    //
    @RequestMapping(value="/work-order-confirmations", method = RequestMethod.GET)
    public List<? extends IntegrationWorkOrderConfirmationData> getIntegrationWorkOrderConfirmationData(
            @RequestParam Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "warehouseName", required = false, defaultValue = "") String warehouseName,
            @RequestParam(name = "number", required = false, defaultValue = "") String number,
            @RequestParam(name = "statusList", required = false, defaultValue = "")   String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")  Long id) {

        return integrationDataService.getIntegrationWorkOrderConfirmationData(warehouseId, warehouseName,
                number, startTime, endTime, date, statusList, id);
    }

    @RequestMapping(value="/work-order-confirmations/{id}", method = RequestMethod.GET)
    public IntegrationWorkOrderConfirmationData getIntegrationWorkOrderConfirmationData(@PathVariable Long id) {

        return integrationDataService.getIntegrationWorkOrderConfirmationData(id);
    }
    @RequestMapping(value="/work-order-confirmations/{id}/resend", method = RequestMethod.POST)
    public IntegrationWorkOrderConfirmationData resendWorkOrderConfirmationData(@PathVariable Long id) {

        return integrationDataService.resendWorkOrderConfirmationData(id);
    }
    @RequestMapping(value="/work-order-confirmations/query/pending", method = RequestMethod.GET)
    public List<? extends IntegrationWorkOrderConfirmationData> getPendingIntegrationWorkOrderConfirmationData(
            @RequestParam(name = "warehouseId", required = false, defaultValue = "")   Long warehouseId,
            @RequestParam(name = "companyCode", required = false, defaultValue = "")   String companyCode,
            @RequestParam(name = "warehouseName", required = false, defaultValue = "")   String warehouseName) {

        // make sure the user either pass in warehouse id, or the combination of companyCode and warehouseName
        if (Objects.isNull(warehouseId) && (Strings.isBlank(companyCode) || Strings.isBlank(warehouseName))) {
            logger.debug("either warehouse id or combination of company code and warehouse name needs to be passed in");
            logger.debug("warehouse id : {}", Objects.isNull(warehouseId) ? "N/A" : warehouseId);
            logger.debug("company code & warehouse name : {} : {}",
                    companyCode, warehouseName);
            throw MissingInformationException.raiseException(
                    "either warehouse id or combination of company code and warehouse name needs to be passed in");

        }
        return integrationDataService.getPendingIntegrationWorkOrderConfirmationData(warehouseId, companyCode, warehouseName);
    }
}
