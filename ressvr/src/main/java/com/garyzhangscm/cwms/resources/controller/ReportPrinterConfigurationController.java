/**
 * Copyright 2018
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

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.clients.PrintingServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.model.ReportHistory;
import com.garyzhangscm.cwms.resources.model.ReportPrinterConfiguration;
import com.garyzhangscm.cwms.resources.service.ReportHistoryService;
import com.garyzhangscm.cwms.resources.service.ReportPrinterConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;


@RestController
public class ReportPrinterConfigurationController {
    private static final Logger logger
            = LoggerFactory.getLogger(ReportPrinterConfigurationController.class);


    @Autowired
    ReportPrinterConfigurationService reportPrinterConfigurationService;
    @Autowired
    PrintingServiceRestemplateClient printingServiceRestemplateClient;


    @RequestMapping(value="/report-printer-configuration", method = RequestMethod.GET)
    public List<ReportPrinterConfiguration> findAllReports(
            Long warehouseId,
            @RequestParam(name="type", required = false, defaultValue = "") String type,
            @RequestParam(name="criteriaValue", required = false, defaultValue = "") String criteriaValue) {
        return reportPrinterConfigurationService.findAll(warehouseId, type, criteriaValue);
    }



    @RequestMapping(value="/report-printer-configuration/{id}", method = RequestMethod.GET)
    public ReportPrinterConfiguration findReportPrinterConfiguration(@PathVariable Long id) {

        return reportPrinterConfigurationService.findById(id);
    }

    @RequestMapping(value="/report-printer-configuration/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeReportPrinterConfiguration(@PathVariable Long id) {

         reportPrinterConfigurationService.deleteReportPrinterConfiguration(id);
        return ResponseBodyWrapper.success("Report Printer Configuration Removed");
    }




    @RequestMapping(value="/report-printer-configuration", method = RequestMethod.PUT)
    public ReportPrinterConfiguration addReportPrinterConfiguration(
            @RequestBody ReportPrinterConfiguration reportPrinterConfiguration
    ) {

        return reportPrinterConfigurationService.addReportPrinterConfiguration(reportPrinterConfiguration);
    }

    @RequestMapping(value="/printers", method = RequestMethod.GET)
    public List<String> getPrinters() {

        return printingServiceRestemplateClient.getPrinters();
    }

    @RequestMapping(value="/report-printer-configuration/{id}", method = RequestMethod.POST)
    public ReportPrinterConfiguration changeReportPrinterConfiguration(@PathVariable Long id,
                                                                       @RequestBody ReportPrinterConfiguration reportPrinterConfiguration) {

        return reportPrinterConfigurationService.changeReportPrinterConfiguration(reportPrinterConfiguration);
    }

}