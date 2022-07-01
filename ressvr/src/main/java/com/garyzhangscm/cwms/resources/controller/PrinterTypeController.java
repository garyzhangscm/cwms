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
import com.garyzhangscm.cwms.resources.service.PrinterTypeService;
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
public class PrinterTypeController {
    private static final Logger logger
            = LoggerFactory.getLogger(PrinterTypeController.class);


    @Autowired
    PrinterTypeService printerTypeService;



    @RequestMapping(value="/printer-types", method = RequestMethod.GET)
    public List<PrinterType> findAllPrinterTypes(
            Long companyId,
            @RequestParam(name="name", required = false, defaultValue = "") String name) {
        return printerTypeService.findAll(companyId, name);
    }

    @BillableEndpoint
    @RequestMapping(value="/printer-types", method = RequestMethod.PUT)
    public PrinterType addPrinterType(@RequestParam Long companyId,
                                      @RequestBody PrinterType printerType) throws IOException {

        printerType.setCompanyId(companyId);
        return printerTypeService.addPrinterType(printerType);
    }

    @BillableEndpoint
    @RequestMapping(value="/printer-types/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removePrinterType(@PathVariable Long id) {
        printerTypeService.delete(id);
         return ResponseBodyWrapper.success("printer type id " + id + " is removed");
    }



    @RequestMapping(value="/printer-types/{id}", method = RequestMethod.GET)
    public PrinterType findPrinterType(@PathVariable Long id) {
        return printerTypeService.findById(id);
    }


}
