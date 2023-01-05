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
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.service.PrintingRequestService;
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
public class PrintingRequestController {
    private static final Logger logger
            = LoggerFactory.getLogger(PrintingRequestController.class);


    @Autowired
    PrintingRequestService printingRequestService;


    @RequestMapping(value="/printing-requests/pending", method = RequestMethod.GET)
    public List<PrintingRequest> findAllPendingPrintingRequest(
            @RequestParam(name = "companyId", defaultValue = "", required = false) Long companyId,
            @RequestParam(name = "companyCode", defaultValue = "", required = false) String companyCode,
            @RequestParam(name = "warehouseId", defaultValue = "", required = false) Long warehouseId,
            @RequestParam(name = "warehouseName", defaultValue = "", required = false) String warehouseName
    ) {
        return printingRequestService.findPendingPrintingRequest(
                companyId, companyCode, warehouseId, warehouseName);
    }

    @RequestMapping(value="/printing-requests/by-report-history", method = RequestMethod.PUT)
    public PrintingRequest generatePrintingRequestByReportHistory(
            Long warehouseId, Long reportHistoryId,
            String printerName, Integer copies
    ) {
        return printingRequestService.generatePrintingRequestByReportHistory(
                warehouseId, reportHistoryId, printerName, copies);
    }

    @RequestMapping(value="/printing-requests/by-url", method = RequestMethod.PUT)
    public PrintingRequest generatePrintingRequestByURL(
            Long warehouseId, String url,
            String printerName, Integer copies,
            String reportType
    ) {
        return printingRequestService.generatePrintingRequestByURL(
                warehouseId, url, printerName, copies, reportType);
    }

    @RequestMapping(value="/printing-requests/{id}/processed", method = RequestMethod.POST)
    public PrintingRequest markPrintingRequestProcessed(
            @PathVariable Long id,
            Long warehouseId,
            @RequestParam String result,
            @RequestParam(name =  "errorMessage", required = false, defaultValue = "") String errorMessage
    ) {
        return printingRequestService.markPrintingRequestProcessed(
                warehouseId, id, result, errorMessage);
    }

}
