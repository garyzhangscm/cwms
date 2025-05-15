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
import com.garyzhangscm.cwms.resources.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class CustomReportController {

    @Autowired
    CustomReportService customReportService;
    @Autowired
    private UserService userService;

    @RequestMapping(value="/customer-reports", method = RequestMethod.GET)
    public List<CustomReport> findAllCustomReports(@RequestParam Long companyId,
                                                   @RequestParam(value = "warehouseId", defaultValue = "", required = false) Long warehouseId,
                                                   @RequestParam(value = "name", defaultValue = "", required = false) String name) {

        return hideDatabaseInformation(companyId, customReportService.findAll(companyId, warehouseId, name));
    }



    @RequestMapping(value="/customer-reports/{id}", method = RequestMethod.GET)
    public CustomReport findById(@PathVariable Long id) {
        CustomReport customReport =  customReportService.findById(id);
        return hideDatabaseInformation(customReport.getCompanyId(), customReport);
    }

    @BillableEndpoint
    @RequestMapping(value="/customer-reports", method = RequestMethod.PUT)
    public CustomReport addCustomReport(@RequestBody CustomReport customReport) {
        return hideDatabaseInformation(customReport.getCompanyId(),  customReportService.addCustomReport(customReport));
    }

    @BillableEndpoint
    @RequestMapping(value="/customer-reports/{id}", method = RequestMethod.POST)
    public CustomReport changeCustomReport(@PathVariable Long id,
                                        @RequestBody CustomReport customReport) {
        return hideDatabaseInformation(customReport.getCompanyId(),   customReportService.changeCustomReport(id, customReport));
    }

    @BillableEndpoint
    @RequestMapping(value="/customer-reports/{id}", method = RequestMethod.DELETE)
    public Boolean removeCustomReport(@PathVariable Long id) {
        customReportService.delete(id);
        return true;
    }


    /***
     * run the customre report with parameters from the client.
     * we will use the request body to retreive the report's parameters
     * @param id
     * @param companyId
     * @param warehouseId
     * @param customReport
     * @return
     */
    @BillableEndpoint
    @RequestMapping(value="/customer-reports/{id}/run", method = RequestMethod.POST)
    public CustomReportExecutionHistory runCustomReport(@PathVariable Long id,
                                                        @RequestParam Long companyId,
                                                        @RequestParam Long warehouseId,
                                                        @RequestBody CustomReport customReport) {
        return hideDatabaseInformation(companyId, customReportService.runCustomReport(id, companyId, warehouseId, customReport));
    }


    private List<CustomReport> hideDatabaseInformation(Long companyId, List<CustomReport> customReports) {
        return customReports.stream().map(customReport -> hideDatabaseInformation(companyId, customReport))
                .collect(Collectors.toList());
    }

    /***
     * Hide the database information before sending to the end user
     * only system admin can access all information
     * @param customReport
     * @return
     */
    private CustomReport hideDatabaseInformation(Long companyId, CustomReport customReport) {
        if (Boolean.TRUE.equals(userService.getCurrentUser(companyId).getSystemAdmin())) {
            return customReport;
        }
        customReport.setQuery(
                customReport.getQuery().substring(0, 3) +
                        " *************** " +
                        customReport.getQuery().substring(customReport.getQuery().length() - 3)
        );
        customReport.setCompanyIdFieldName("***");
        customReport.setWarehouseIdFieldName("***");
        customReport.setGroupBy("***");
        customReport.setSortBy("***");
        customReport.getCustomReportParameters().forEach(
                customReportParameter -> {
                    customReportParameter.setName("***");
                }
        );

        return customReport;
    }


    private CustomReportExecutionHistory hideDatabaseInformation(Long companyId, CustomReportExecutionHistory customReportExecutionHistory) {
        if (Boolean.TRUE.equals(userService.getCurrentUser(companyId).getSystemAdmin())) {
            return customReportExecutionHistory;
        }
        customReportExecutionHistory.setQuery(
                customReportExecutionHistory.getQuery().substring(0, 3) +
                        " *************** " +
                        customReportExecutionHistory.getQuery().substring(customReportExecutionHistory.getQuery().length() - 3)
        );
        return customReportExecutionHistory;
    }
}
