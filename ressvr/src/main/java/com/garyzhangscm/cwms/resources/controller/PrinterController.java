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
import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.model.Printer;
import com.garyzhangscm.cwms.resources.model.PrinterType;
import com.garyzhangscm.cwms.resources.service.PrinterService;
import com.garyzhangscm.cwms.resources.service.PrinterTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;


@RestController
public class PrinterController {
    private static final Logger logger
            = LoggerFactory.getLogger(PrinterController.class);


    @Autowired
    PrinterService printerService;


    /**
     * Return the printers that already installed in the server
     * @return a list of printer name return from the server
     */
    @RequestMapping(value="/server-printers", method = RequestMethod.GET)
    public List<String> getServerPrinters(Long warehouseId,
                                          @RequestParam(name="printingStrategy", required = false, defaultValue = "") String printingStrategy) {

        return printerService.getServerPrinters(warehouseId, printingStrategy);
    }


    @RequestMapping(value="/printers", method = RequestMethod.GET)
    public List<Printer> findAllPrinters(Long warehouseId,
                                         @RequestParam(name="name", required = false, defaultValue = "") String name,
                                         @RequestParam(name="printerType", required = false, defaultValue = "") String printerType) {
        return printerService.findAll(warehouseId, name, printerType);
    }

    @BillableEndpoint
    @RequestMapping(value="/printers", method = RequestMethod.PUT)
    public Printer addPrinter(@RequestParam Long warehouseId,
                              @RequestBody Printer printer) throws IOException {

        printer.setWarehouseId(warehouseId);
        return printerService.addPrinter(printer);
    }

    @BillableEndpoint
    @RequestMapping(value="/printers/{id}", method = RequestMethod.POST)
    public Printer changePrinter(@PathVariable Long id,
                                 @RequestParam Long warehouseId,
                                 @RequestBody Printer printer) throws IOException {

        printer.setWarehouseId(warehouseId);
        return printerService.changePrinter(id, printer);
    }


    @BillableEndpoint
    @RequestMapping(value="/printers/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removePrinterType(@PathVariable Long id) {
        printerService.delete(id);
         return ResponseBodyWrapper.success("printer type id " + id + " is removed");
    }



    @RequestMapping(value="/printers/{id}", method = RequestMethod.GET)
    public Printer findPrinterType(@PathVariable Long id) {
        return printerService.findById(id);
    }

    
}
