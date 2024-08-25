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

package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.model.CustomReport;
import com.garyzhangscm.cwms.resources.model.CustomReportExecutionHistory;
import com.garyzhangscm.cwms.resources.model.RF;
import com.garyzhangscm.cwms.resources.service.CustomReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class CustomReportController {

    @Autowired
    CustomReportService customReportService;

    @RequestMapping(value="/customer-reports", method = RequestMethod.GET)
    public List<CustomReport> findAllCustomReports(@RequestParam Long companyId,
                                                   @RequestParam(value = "warehouseId", defaultValue = "", required = false) Long warehouseId,
                                                   @RequestParam(value = "name", defaultValue = "", required = false) String name) {
        return customReportService.findAll(companyId, warehouseId, name);
    }


    @RequestMapping(value="/customer-reports/{id}", method = RequestMethod.GET)
    public CustomReport findById(@PathVariable Long id) {
        return customReportService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/customer-reports", method = RequestMethod.PUT)
    public CustomReport addCustomReport(@RequestBody CustomReport customReport) {
        return customReportService.addCustomReport(customReport);
    }

    @BillableEndpoint
    @RequestMapping(value="/customer-reports/{id}", method = RequestMethod.POST)
    public CustomReport changeCustomReport(@PathVariable Long id,
                                        @RequestBody CustomReport customReport) {
        return customReportService.changeCustomReport(id, customReport);
    }

    @BillableEndpoint
    @RequestMapping(value="/customer-reports/{id}", method = RequestMethod.DELETE)
    public Boolean removeCustomReport(@PathVariable Long id) {
        customReportService.delete(id);
        return true;
    }


    @BillableEndpoint
    @RequestMapping(value="/customer-reports/{id}/run", method = RequestMethod.POST)
    public CustomReportExecutionHistory runCustomReport(@PathVariable Long id,
                                                        @RequestParam Long companyId,
                                                        @RequestParam Long warehouseId,
                                                        @RequestBody CustomReport customReport) {
        return customReportService.runCustomReport(id, companyId, warehouseId, customReport);
    }



}
