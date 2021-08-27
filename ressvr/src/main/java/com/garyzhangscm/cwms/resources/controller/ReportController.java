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
import com.garyzhangscm.cwms.resources.model.ReportType;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;


@RestController
public class ReportController {
    private static final Logger logger
            = LoggerFactory.getLogger(ReportController.class);


    @Autowired
    ReportService reportService;



    @RequestMapping(value="/reports", method = RequestMethod.GET)
    public List<Report> findAllReports(
            @RequestParam(name="companyId", required = false ) Long companyId,
            @RequestParam(name="warehouseId", required = false ) Long warehouseId,
            @RequestParam(name="type", required = false, defaultValue = "") String type) {
        return reportService.findAll(companyId, warehouseId, type);
    }

    @RequestMapping(value="/reports", method = RequestMethod.PUT)
    public Report addReport(@RequestParam Long warehouseId,
                            @RequestParam String username,
                            @RequestParam Boolean companySpecific,
                            @RequestParam Boolean warehouseSpecific,
                            @RequestBody Report report) throws IOException {

        return reportService.addReport(warehouseId, username, companySpecific, warehouseSpecific, report);
    }

    @RequestMapping(value="/reports/{id}", method = RequestMethod.POST)
    public Report changeReport(@PathVariable Long id,
                           @RequestBody Report report) {
        return reportService.changeReport(report);
    }


    @RequestMapping(value="/reports/{id}", method = RequestMethod.GET)
    public Report findReport(@PathVariable Long id) {
        return reportService.findById(id);
    }

    @RequestMapping(value="/reports/{warehouseId}/{type}", method = RequestMethod.POST)
    public ReportHistory generateReport(@PathVariable Long warehouseId,
                                        @PathVariable String type,
                                        @RequestBody Report reportData,
                                        @RequestParam(name = "locale", defaultValue = "en", required = false) String locale) throws IOException, JRException {

        // logger.debug("####   Report   Data  / 1  ######");
        // logger.debug(reportData.toString());
        return reportService.generateReport(
                warehouseId,
                ReportType.valueOf(type),
                reportData,
                locale);
    }

    @RequestMapping(method=RequestMethod.POST, value="/reports/templates/upload/{warehouseId}")
    public ResponseBodyWrapper uploadReportTemplate(
            @PathVariable Long warehouseId,
            @RequestParam("file") MultipartFile file) throws IOException {


        String filePath = reportService.uploadReportTemplate(warehouseId, file);
        return  ResponseBodyWrapper.success(filePath);
    }

    @RequestMapping(method=RequestMethod.GET,
            value="/reports/templates/upload/{warehouseId}/{username}/{fileName}")
    public ResponseEntity<Resource> getTemporaryReportTemplate(
            @PathVariable Long warehouseId,
            @PathVariable String username,
            @PathVariable String fileName) throws FileNotFoundException {


        File reportTemplateFile = reportService.getTemporaryReportTemplate(warehouseId, username, fileName);

        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(reportTemplateFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + fileName)
                .contentLength(reportTemplateFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @RequestMapping(method=RequestMethod.GET, value="/reports/templates")
    public ResponseEntity<Resource> getReportTemplate(
            @RequestParam String fileName,
            @RequestParam(name = "companyId", defaultValue = "", required = false) Long companyId,
            @RequestParam(name = "warehouseId", defaultValue = "", required = false) Long warehouseId) throws FileNotFoundException {


        File reportTemplateFile = reportService.getReportTemplate(companyId, warehouseId , fileName);

        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(reportTemplateFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + fileName)
                .contentLength(reportTemplateFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
