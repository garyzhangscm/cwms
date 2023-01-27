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


import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.model.IntegrationInventoryAdjustmentConfirmationData;
import com.garyzhangscm.cwms.integration.model.IntegrationInventoryAttributeChangeConfirmationData;
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
public class InventoryIntegrationDataController {


    private static final Logger logger = LoggerFactory.getLogger(InventoryIntegrationDataController.class);

    @Autowired
    private IntegrationDataService integrationDataService;

    //
    // Inventory Adjustment Confirmation Related
    //
    @RequestMapping(value="/inventory-adjustment-confirmations", method = RequestMethod.GET)
    public List<? extends IntegrationInventoryAdjustmentConfirmationData> getInventoryAdjustmentConfirmationData(
            @RequestParam Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")   String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")  Long id
            ) {

        return integrationDataService.getInventoryAdjustmentConfirmationData(warehouseId, startTime,
                endTime, date, statusList, id);
    }

    @RequestMapping(value="/inventory-adjustment-confirmations/query/pending", method = RequestMethod.GET)
    public List<? extends IntegrationInventoryAdjustmentConfirmationData> getPendingInventoryAdjustmentConfirmationData(
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
        return integrationDataService.getPendingInventoryAdjustmentConfirmationData(warehouseId, companyCode, warehouseName);
    }
    @RequestMapping(value="/inventory-adjustment-confirmations/{id}", method = RequestMethod.GET)
    public IntegrationInventoryAdjustmentConfirmationData getInventoryAdjustmentConfirmationData(@PathVariable Long id) {

        return integrationDataService.getInventoryAdjustmentConfirmationData(id);
    }

    @RequestMapping(value="/inventory-adjustment-confirmations/{id}/resend", method = RequestMethod.POST)
    public IntegrationInventoryAdjustmentConfirmationData resendInventoryAdjustmentConfirmationData(@PathVariable Long id) {

        return integrationDataService.resendInventoryAdjustmentConfirmationData(id);
    }

    //
    // Inventory Attribute Change Confirmation Related
    //
    @RequestMapping(value="/inventory-attribute-change-confirmations", method = RequestMethod.GET)
    public List<? extends IntegrationInventoryAttributeChangeConfirmationData> getInventoryAttributeChangeConfirmationData(
            @RequestParam Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")   String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")  Long id) {
        return integrationDataService.getInventoryAttributeChangeConfirmationData(
                warehouseId, startTime, endTime, date, statusList, id);
    }
    @RequestMapping(value="/inventory-attribute-change-confirmations/{id}", method = RequestMethod.GET)
    public IntegrationInventoryAttributeChangeConfirmationData getInventoryAttributeChangeConfirmationData(Long id) {
        return integrationDataService.getInventoryAttributeChangeConfirmationData(id);
    }
    @RequestMapping(value="/inventory-attribute-change-confirmations/{id}/resend", method = RequestMethod.POST)
    public IntegrationInventoryAttributeChangeConfirmationData resendInventoryAttributeChangeConfirmationData(Long id) {
        return integrationDataService.resendInventoryAttributeChangeConfirmationData(id);
    }

    @RequestMapping(value="/inventory-attribute-change-confirmations/query/pending", method = RequestMethod.GET)
    public List<? extends IntegrationInventoryAttributeChangeConfirmationData> getPendingInventoryAttributeChangeConfirmationData(
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
        return integrationDataService.getPendingInventoryAttributeChangeConfirmationData(warehouseId, companyCode, warehouseName);
    }

}
