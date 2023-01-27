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
public class CustomerIntegrationDataController {


    private static final Logger logger = LoggerFactory.getLogger(CustomerIntegrationDataController.class);

    @Autowired
    private IntegrationDataService integrationDataService;
 

    //
    // Integration - Customer
    //
    @RequestMapping(value="/customers", method = RequestMethod.GET)
    public List<? extends IntegrationCustomerData> getIntegrationCustomerData(
            @RequestParam Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")  String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "")  Long id) {

        return integrationDataService.getCustomerData(
                warehouseId, startTime, endTime, date, statusList, id);
    }

    @RequestMapping(value="/customers/{id}", method = RequestMethod.GET)
    public IntegrationCustomerData getIntegrationCustomerData(@PathVariable Long id) {

        return integrationDataService.getCustomerData(id);
    }
    @RequestMapping(value="/customers/{id}/resend", method = RequestMethod.POST)
    public IntegrationCustomerData resendCustomerData(@PathVariable Long id) {

        return integrationDataService.resendCustomerData(id);
    }

    @RequestMapping(value="/customers", method = RequestMethod.PUT)
    public IntegrationCustomerData addIntegrationCustomerData(@RequestBody Customer customer) {

        return integrationDataService.addIntegrationCustomerData(customer);
    }

    /**
    @RequestMapping(value="/dblink/customer", method = RequestMethod.PUT)
    public ResponseBodyWrapper saveIntegrationCustomerData(
            @RequestBody DBBasedCustomer dbBasedCustomer
    ){

        logger.debug("Start to save dbBasedCustomer into database \n{}",
                dbBasedCustomer);
        integrationDataService.addIntegrationCustomerData(dbBasedCustomer);
        return ResponseBodyWrapper.success("success");
    }
            **/

}
