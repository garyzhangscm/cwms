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
            @RequestParam(name="name", required = false, defaultValue = "") String name,
            @RequestParam(name="type", required = false, defaultValue = "") String type) {
        return reportHistoryService.findAll(companyId, warehouseId, name, type);
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

    @RequestMapping(value="/report-histories/download/{filename}", method = RequestMethod.GET)
    public ResponseEntity<Resource>  downloadReport(@PathVariable String filename)
            throws FileNotFoundException {


        File reportResultFile = reportHistoryService.getReportFile(filename);
        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(reportResultFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + filename)
                .contentLength(reportResultFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

}