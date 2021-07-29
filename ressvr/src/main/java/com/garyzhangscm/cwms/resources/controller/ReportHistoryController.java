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
import com.garyzhangscm.cwms.resources.model.Report;
import com.garyzhangscm.cwms.resources.model.ReportHistory;
import com.garyzhangscm.cwms.resources.service.ReportHistoryService;
import com.garyzhangscm.cwms.resources.service.ReportService;
import net.sf.jasperreports.engine.JRException;
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
public class ReportHistoryController {
    private static final Logger logger
            = LoggerFactory.getLogger(ReportHistoryController.class);


    @Autowired
    ReportHistoryService reportHistoryService;


    @RequestMapping(value="/report-histories", method = RequestMethod.GET)
    public List<ReportHistory> findAllReports(
            @RequestParam(name="companyId", required = false ) Long companyId,
            @RequestParam(name="warehouseId", required = false ) Long warehouseId,
            @RequestParam(name="type", required = false, defaultValue = "") String type) {
        return reportHistoryService.findAll(companyId, warehouseId, type);
    }



    @RequestMapping(value="/report-histories/{id}", method = RequestMethod.GET)
    public ReportHistory findReport(@PathVariable Long id) {

        return reportHistoryService.findById(id);
    }

    @RequestMapping(value="/report-histories/{id}/download", method = RequestMethod.GET)
    public ResponseEntity<Resource> downloadReport(@PathVariable Long id)
            throws FileNotFoundException {

        File reportResultFile = reportHistoryService.getReportFile(id);
        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(reportResultFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + reportResultFile.getName())
                .contentLength(reportResultFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @RequestMapping(value="/report-histories/download/{companyId}/{warehouseId}/{type}/{filename}", method = RequestMethod.GET)
    public ResponseEntity<Resource>  downloadReport(@PathVariable String filename,
                                                    @PathVariable String type,
                                                    @PathVariable Long companyId,
                                                    @PathVariable Long warehouseId)
            throws FileNotFoundException {


        File reportResultFile = reportHistoryService.getReportFile(companyId, warehouseId, type, filename);
        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(reportResultFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + filename)
                .contentLength(reportResultFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @RequestMapping(value="/report-histories/print/{companyId}/{warehouseId}/{type}/{filename}", method = RequestMethod.POST)
    public ResponseBodyWrapper<String>  printReport(@PathVariable String filename,
                                                    @PathVariable String type,
                                                    @PathVariable Long companyId,
                                                    @PathVariable Long warehouseId,
                                                    @RequestParam(name = "findPrinterBy", required = false, defaultValue = "") String findPrinterBy,
                                                    @RequestParam(name = "printerName", required = false, defaultValue = "") String printerName,
                                                    @RequestParam(name = "copies", required = false, defaultValue = "") Integer copies)
            throws FileNotFoundException, IOException {

        logger.debug("Start to print report for {} / {}, copies: {}",
                type, filename, copies);

        logger.debug("> will find a printer by name {} / or value {}",
                printerName, findPrinterBy);
        // print the report from the printer attached to the server
        File reportResultFile =
                reportHistoryService.printReport(companyId, warehouseId, type, filename,
                        findPrinterBy, printerName, copies);
        return ResponseBodyWrapper.success(reportResultFile.getName() + " printed!");

    }

}
