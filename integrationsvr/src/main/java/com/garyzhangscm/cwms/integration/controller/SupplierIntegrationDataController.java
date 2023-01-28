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
public class SupplierIntegrationDataController {


    private static final Logger logger = LoggerFactory.getLogger(SupplierIntegrationDataController.class);

    @Autowired
    private IntegrationDataService integrationDataService;
 

    //
    // Supplier Related
    //
    @RequestMapping(value="/suppliers", method = RequestMethod.GET)
    public List<? extends IntegrationSupplierData> getIntegrationSupplierData(
            @RequestParam Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")  String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")   Long id) {

        return integrationDataService.getSupplierData(
                warehouseId, startTime, endTime, date, statusList, id);
    }

    @RequestMapping(value="/suppliers/{id}", method = RequestMethod.GET)
    public IntegrationSupplierData getIntegrationSupplierData(@PathVariable Long id) {

        return integrationDataService.getSupplierData(id);
    }

    @RequestMapping(value="/suppliers/{id}/resend", method = RequestMethod.POST)
    public IntegrationSupplierData resendSupplierData(@PathVariable Long id) {

        return integrationDataService.resendSupplierData(id);
    }

    @RequestMapping(value="/suppliers", method = RequestMethod.PUT)
    public ResponseBodyWrapper addIntegrationSupplierData(@RequestBody Supplier supplier) {

        IntegrationSupplierData supplierData =
                integrationDataService.addIntegrationSupplierData(supplier);
        return ResponseBodyWrapper.success(String.valueOf(supplierData.getId()));
    }

    /**
    @RequestMapping(value="/dblink/suppliers", method = RequestMethod.PUT)
    public ResponseBodyWrapper copyIntegrationSupplierData(@RequestBody DBBasedSupplier dbBasedSupplier) {

        dbBasedSupplier.setId(null);
        integrationDataService.addIntegrationSupplierData(dbBasedSupplier);


        return ResponseBodyWrapper.success("success");
    }
**/


}
