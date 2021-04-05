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

import com.garyzhangscm.cwms.resources.model.Role;
import com.garyzhangscm.cwms.resources.model.User;
import com.garyzhangscm.cwms.resources.service.ReportService;
import net.sf.jasperreports.engine.JRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(name="name", required = false, defaultValue = "") String name,
            @RequestParam(name="type", required = false, defaultValue = "") String type) {
        return reportService.findAll(companyId, warehouseId, name, type);
    }

    @RequestMapping(value="/reports", method = RequestMethod.PUT)
    public Report addReport(@RequestBody Report report) {

        return reportService.saveOrUpdate(report);
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

    @RequestMapping(value="/reports/{warehouseId}/{name}", method = RequestMethod.POST)
    public ResponseBodyWrapper generateReport(@PathVariable Long warehouseId,
                                 @PathVariable String name,
                                 @RequestBody Report reportData,
                                 @RequestParam(name = "locale", defaultValue = "en", required = false) String locale) throws IOException, JRException {

        String reportResultFile =  reportService.generateReport(
                warehouseId,
                name,
                reportData,
                locale);
        logger.debug("Successfully write the report into file {}",
                reportResultFile);
        return ResponseBodyWrapper.success(reportResultFile);
    }


}
