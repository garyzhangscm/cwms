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
public class ClientIntegrationDataController {


    private static final Logger logger = LoggerFactory.getLogger(ClientIntegrationDataController.class);

    @Autowired
    private IntegrationDataService integrationDataService;


    //
    // Client Related
    //
    @RequestMapping(value="/clients", method = RequestMethod.GET)
    public List<? extends IntegrationClientData> getIntegrationClientData(
            @RequestParam Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "")
              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "")
              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date,
            @RequestParam(name = "statusList", required = false, defaultValue = "")  String statusList,
            @RequestParam(name = "id", required = false, defaultValue = "") Long id) {

        return integrationDataService.getClientData(
                warehouseId, startTime, endTime, date, statusList, id
        );
    }

    @RequestMapping(value="/clients/{id}", method = RequestMethod.GET)
    public IntegrationClientData getIntegrationClientData(@PathVariable Long id) {

        return integrationDataService.getClientData(id);
    }

    @RequestMapping(value="/clients/{id}/resend", method = RequestMethod.POST)
    public IntegrationClientData resendClientData(@PathVariable Long id) {

        return integrationDataService.resendClientData(id);
    }


    @RequestMapping(value="/clients", method = RequestMethod.PUT)
    public IntegrationClientData addIntegrationClientData(@RequestBody Client client) {

        return integrationDataService.addIntegrationClientData(client);
    }

}
