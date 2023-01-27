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
public class ReceiptIntegrationDataController {


    private static final Logger logger = LoggerFactory.getLogger(ReceiptIntegrationDataController.class);

    @Autowired
    private IntegrationDataService integrationDataService;
 

    //
    // Receipt Related
    //
    @RequestMapping(value="/receipts", method = RequestMethod.GET)
    public List<? extends IntegrationReceiptData> getIntegrationReceiptData(
            @RequestParam String companyCode,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "")  Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")  String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")   Long id) {

        return integrationDataService.getReceiptData(
                companyCode, warehouseId, startTime, endTime, date, statusList, id);
    }

    @RequestMapping(value="/receipts/{id}", method = RequestMethod.GET)
    public IntegrationReceiptData getIntegrationReceiptData(@PathVariable Long id) {

        return integrationDataService.getReceiptData(id);
    }

    @RequestMapping(value="/receipts/{id}/resend", method = RequestMethod.POST)
    public IntegrationReceiptData resendIntegrationReceiptData(@PathVariable Long id) {

        return integrationDataService.resendReceiptData(id);
    }
    @RequestMapping(value="/receipts", method = RequestMethod.PUT)
    public IntegrationReceiptData addIntegrationReceiptData(@RequestBody Receipt receipt) {

        logger.debug("Start to add receipt: \n{}", receipt);
        return integrationDataService.addReceiptData(receipt);
    }
/***
    @RequestMapping(value="/dblink/receipt", method = RequestMethod.PUT)
    public ResponseBodyWrapper addIntegrationReceiptData(
            @RequestBody DBBasedReceipt dbBasedReceipt
    ){
        logger.debug("Start to save dbbased receipt into database \n{}",
                dbBasedReceipt);
        integrationDataService.addReceiptData(dbBasedReceipt);
        return ResponseBodyWrapper.success("success");
    }
            **/
    //
    // Receipt Confirmation Related
    //
    @RequestMapping(value="/receipt-confirmations", method = RequestMethod.GET)
    public List<? extends IntegrationReceiptConfirmationData> getIntegrationReceiptConfirmationData(
            @RequestParam Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "warehouseName", required = false, defaultValue = "") String warehouseName,
            @RequestParam(name = "number", required = false, defaultValue = "") String number,
            @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId,
            @RequestParam(name = "clientName", required = false, defaultValue = "") String clientName,
            @RequestParam(name = "supplierId", required = false, defaultValue = "") Long supplierId,
            @RequestParam(name = "supplierName", required = false, defaultValue = "") String supplierName,
            @RequestParam(name = "statusList", required = false, defaultValue = "")   String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")  Long id
    ) {

        return integrationDataService.getIntegrationReceiptConfirmationData(warehouseId, warehouseName,
                number, clientId, clientName,
                supplierId, supplierName,
                startTime, endTime, date, statusList, id);
    }

    @RequestMapping(value="/receipt-confirmations/{id}", method = RequestMethod.GET)
    public IntegrationReceiptConfirmationData getIntegrationReceiptConfirmationData(@PathVariable Long id) {

        return integrationDataService.getIntegrationReceiptConfirmationData(id);
    }

    @RequestMapping(value="/receipt-confirmations/{id}/resend", method = RequestMethod.POST)
    public IntegrationReceiptConfirmationData resendReceiptConfirmationData(@PathVariable Long id) {

        return integrationDataService.resendReceiptConfirmationData(id);
    }


    @RequestMapping(value="/receipt-confirmations/query/pending", method = RequestMethod.GET)
    public List<? extends IntegrationReceiptConfirmationData> getPendingIntegrationReceiptConfirmationData(
            @RequestParam(name = "warehouseId", required = false, defaultValue = "")   Long warehouseId,
            @RequestParam(name = "companyCode", required = false, defaultValue = "")   String companyCode,
            @RequestParam(name = "warehouseName", required = false, defaultValue = "")   String warehouseName
    ) {

        // make sure the user either pass in warehouse id, or the combination of companyCode and warehouseName
        if (Objects.isNull(warehouseId) && (Strings.isBlank(companyCode) || Strings.isBlank(warehouseName))) {
            logger.debug("either warehouse id or combination of company code and warehouse name needs to be passed in");
            logger.debug("warehouse id : {}", Objects.isNull(warehouseId) ? "N/A" : warehouseId);
            logger.debug("company code & warehouse name : {} : {}",
                    companyCode, warehouseName);
            throw MissingInformationException.raiseException(
                    "either warehouse id or combination of company code and warehouse name needs to be passed in");

        }
        return integrationDataService.getPendingIntegrationReceiptConfirmationData(warehouseId, companyCode, warehouseName);
    }


}
