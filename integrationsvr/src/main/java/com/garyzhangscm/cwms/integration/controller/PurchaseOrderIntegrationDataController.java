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
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.service.IntegrationDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;


@RestController
@RequestMapping(value = "/integration-data")
public class PurchaseOrderIntegrationDataController {


    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderIntegrationDataController.class);

    @Autowired
    private IntegrationDataService integrationDataService;

 

    //
    // Purchase Order Related
    //
    @RequestMapping(value="/purchase-orders", method = RequestMethod.GET)
    public List<? extends IntegrationPurchaseOrderData> getIntegrationPurchaseOrderData(
            @RequestParam String companyCode,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "")  Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")  String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")   Long id) {

        return integrationDataService.getPurchaseOrderData(
                companyCode, warehouseId, startTime, endTime, date, statusList, id);
    }

    @RequestMapping(value="/purchase-orders/{id}", method = RequestMethod.GET)
    public IntegrationPurchaseOrderData getIntegrationPurchaseOrderData(@PathVariable Long id) {

        return integrationDataService.getPurchaseOrderData(id);
    }

    @RequestMapping(value="/purchase-orders/{id}/resend", method = RequestMethod.POST)
    public IntegrationPurchaseOrderData resendIntegrationPurchaseOrderData(@PathVariable Long id) {

        return integrationDataService.resendPurchaseOrderData(id);
    }
    @RequestMapping(value="/purchase-orders", method = RequestMethod.PUT)
    public ResponseBodyWrapper addIntegrationPurchaseOrderData(@RequestBody PurchaseOrder purchaseOrder) {

        IntegrationPurchaseOrderData purchaseOrderData =
                integrationDataService.addPurchaseOrderData(purchaseOrder);
        return ResponseBodyWrapper.success(String.valueOf(purchaseOrderData.getId()));
    }
/**
    @RequestMapping(value="/dblink/purchase-orders", method = RequestMethod.PUT)
    public ResponseBodyWrapper addIntegrationPurchaseOrderData(
            @RequestBody DBBasedPurchaseOrder dbBasedPurchaseOrder
    ){
        logger.debug("Start to save dbbased purchase order into database \n{}",
                dbBasedPurchaseOrder);
        integrationDataService.addPurchaseOrderData(dbBasedPurchaseOrder);
        return ResponseBodyWrapper.success("success");
    }
            */

}
