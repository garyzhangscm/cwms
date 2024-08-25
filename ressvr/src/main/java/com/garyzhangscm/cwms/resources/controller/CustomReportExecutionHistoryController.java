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

import com.garyzhangscm.cwms.resources.model.CustomReportExecutionHistory;
import com.garyzhangscm.cwms.resources.service.CustomReportExecutionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

@RestController
public class CustomReportExecutionHistoryController {

    @Autowired
    CustomReportExecutionHistoryService customReportExecutionHistoryService;

    @RequestMapping(value="/customer-report-execution-histories", method = RequestMethod.GET)
    public List<CustomReportExecutionHistory> findAllCustomReportExecutionHistories(
            @RequestParam Long companyId,
            @RequestParam Long warehouseId,
            @RequestParam(value = "customReportId", defaultValue = "", required = false) Long customReportId,
            @RequestParam(value = "customReportExecutionHistoryIDs", defaultValue = "", required = false) String customReportExecutionHistoryIDs,
            @RequestParam(value = "includeExpiredExecutionHistory", defaultValue = "", required = false) Boolean includeExpiredExecutionHistory) {
        return customReportExecutionHistoryService.findAll(companyId, warehouseId, customReportId,
                customReportExecutionHistoryIDs, includeExpiredExecutionHistory);
    }

    @RequestMapping(value="/customer-report-execution-histories/by-customer-report/{customReportId}", method = RequestMethod.GET)
    public List<CustomReportExecutionHistory> findByCustomReport(
            @RequestParam Long companyId,
            @RequestParam(value = "warehouseId", defaultValue = "", required = false) Long warehouseId,
            @PathVariable  Long customReportId) {
        return customReportExecutionHistoryService.findByCustomReport(companyId, warehouseId, customReportId);
    }


    @RequestMapping(value="/customer-report-execution-histories/download/{id}", method = RequestMethod.GET)
    public ResponseEntity<Resource> downloadCustomReportResultFile(@PathVariable Long id)
            throws FileNotFoundException {

        File customReportResultFile = customReportExecutionHistoryService.getCustomReportResultFile(id);
        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(customReportResultFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + customReportResultFile.getName())
                .contentLength(customReportResultFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

}
