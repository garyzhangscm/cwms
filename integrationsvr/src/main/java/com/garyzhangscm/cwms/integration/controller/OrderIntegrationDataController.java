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
public class OrderIntegrationDataController {


    private static final Logger logger = LoggerFactory.getLogger(OrderIntegrationDataController.class);

    @Autowired
    private IntegrationDataService integrationDataService;

    //
    // Order Related
    //
    @RequestMapping(value="/orders", method = RequestMethod.GET)
    public List<? extends IntegrationOrderData> getIntegrationOrderData(
            @RequestParam String companyCode,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "")  Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")   String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")   Long id) {

        return integrationDataService.getOrderData(
                companyCode, warehouseId, startTime, endTime, date, statusList, id);
    }

    @RequestMapping(value="/orders/{id}", method = RequestMethod.GET)
    public IntegrationOrderData getIntegrationOrderData(@PathVariable Long id) {

        return integrationDataService.getOrderData(id);
    }
    @RequestMapping(value="/orders/{id}/resend", method = RequestMethod.POST)
    public IntegrationOrderData resendOrderData(@PathVariable Long id) {

        return integrationDataService.resendOrderData(id);
    }
    @RequestMapping(value="/orders", method = RequestMethod.PUT)
    public IntegrationOrderData addIntegrationOrderData(@RequestBody Order order,
                                                       @RequestParam(name = "immediateProcess", defaultValue = "false", required = false) Boolean immediateProcess,
                                                        @RequestParam(name = "validateAddress", defaultValue = "false", required = false) Boolean validateAddress) {

        logger.debug("start to save order into integration table \n{}", order);

        return integrationDataService.addOrderData(order, immediateProcess, validateAddress);
    }

    /**
    @RequestMapping(value="/dblink/orders", method = RequestMethod.PUT)
    public ResponseBodyWrapper addIntegrationOrderData(@RequestBody DBBasedOrder dbBasedOrder) {

        integrationDataService.addOrderData(dbBasedOrder);
        return ResponseBodyWrapper.success("success");
    }
    */

    //
    // Order Confirmation Related
    //
    @RequestMapping(value="/order-confirmations", method = RequestMethod.GET)
    public List<? extends IntegrationOrderConfirmationData> getIntegrationOrderConfirmationData(
            @RequestParam(name = "warehouseName", required = false, defaultValue = "") String warehouseName,
            @RequestParam(name = "number", required = false, defaultValue = "") String number,
            @RequestParam Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")   String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")  Long id) {

        return integrationDataService.getIntegrationOrderConfirmationData(warehouseId, warehouseName,
                number,  startTime, endTime, date, statusList, id);
    }

    @RequestMapping(value="/order-confirmations/last-minute", method = RequestMethod.GET)
    public List<? extends IntegrationOrderConfirmationData> getLastMinuteIntegrationOrderConfirmationData(
            @RequestParam Long warehouseId,
            @RequestParam(name = "minutes", required = false, defaultValue = "1") int minutes) {
        ZonedDateTime endTime = ZonedDateTime.now();
        ZonedDateTime startTime = endTime.minusMinutes(minutes);

        logger.debug("start to get order confirmation from {} - {}",
                startTime, endTime);

        return integrationDataService.getIntegrationOrderConfirmationData(warehouseId, null,
                null,  startTime, endTime, null, null, null);
    }
    @RequestMapping(value="/order-confirmations/last-hour", method = RequestMethod.GET)
    public List<? extends IntegrationOrderConfirmationData> getLastHourIntegrationOrderConfirmationData(
            @RequestParam Long warehouseId,
            @RequestParam(name = "hours", required = false, defaultValue = "1") int hours) {
        ZonedDateTime endTime = ZonedDateTime.now();
        ZonedDateTime startTime = endTime.minusHours(hours);
        logger.debug("start to get order confirmation from {} - {}",
                startTime, endTime);

        return integrationDataService.getIntegrationOrderConfirmationData(warehouseId, null,
                null,  startTime, endTime, null, null, null);
    }
    @RequestMapping(value="/order-confirmations/last-day", method = RequestMethod.GET)
    public List<? extends IntegrationOrderConfirmationData> getLastDayIntegrationOrderConfirmationData(
            @RequestParam Long warehouseId,
            @RequestParam(name = "days", required = false, defaultValue = "1") int days) {

        ZonedDateTime endTime = ZonedDateTime.now();
        ZonedDateTime startTime = endTime.minusDays(days);
        logger.debug("start to get order confirmation from {} - {}",
                startTime, endTime);

        return integrationDataService.getIntegrationOrderConfirmationData(warehouseId, null,
                null,  startTime, endTime, null, null, null);
    }
    @RequestMapping(value="/order-confirmations/last-week", method = RequestMethod.GET)
    public List<? extends IntegrationOrderConfirmationData> getLastWeekIntegrationOrderConfirmationData(
            @RequestParam Long warehouseId) {
        ZonedDateTime endTime = ZonedDateTime.now();
        ZonedDateTime startTime = endTime.minusDays(7);

        return integrationDataService.getIntegrationOrderConfirmationData(warehouseId, null,
                null,  startTime, endTime, null, null, null);
    }
    @RequestMapping(value="/order-confirmations/last-month", method = RequestMethod.GET)
    public List<? extends IntegrationOrderConfirmationData> getLastMonthIntegrationOrderConfirmationData(
            @RequestParam Long warehouseId) {
        ZonedDateTime endTime = ZonedDateTime.now();
        ZonedDateTime startTime = endTime.minusDays(30);

        return integrationDataService.getIntegrationOrderConfirmationData(warehouseId, null,
                null,  startTime, endTime, null, null, null);
    }


    @RequestMapping(value="/order-confirmations/{id}", method = RequestMethod.GET)
    public IntegrationOrderConfirmationData getIntegrationOrderConfirmationData(@PathVariable Long id) {

        return integrationDataService.getIntegrationOrderConfirmationData(id);
    }

    @RequestMapping(value="/order-confirmations/{id}/resend", method = RequestMethod.POST)
    public IntegrationOrderConfirmationData resendOrderConfirmationData(@PathVariable Long id) {

        return integrationDataService.resendOrderConfirmationData(id);
    }

    @RequestMapping(value="/order-confirmations/query/pending", method = RequestMethod.GET)
    public List<? extends IntegrationOrderConfirmationData> getPendingIntegrationOrderConfirmationData(
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
        return integrationDataService.getPendingIntegrationOrderConfirmationData(warehouseId, companyCode, warehouseName);
    }

    @RequestMapping(value="/order-confirmations/{id}/result", method = RequestMethod.POST)
    public ResponseBodyWrapper saveInventoryReceiptConfirmationResult(@PathVariable Long id,
                                                                      boolean succeed,
                                                                      String errorMessage) {

        IntegrationOrderConfirmationData integrationOrderConfirmationData =
                integrationDataService.saveOrderConfirmationResult(id, succeed, errorMessage);

        return ResponseBodyWrapper.success(String.valueOf(integrationOrderConfirmationData.getId()));
    }


}
