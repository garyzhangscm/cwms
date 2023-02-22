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

package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.model.WarehouseConfiguration;
import com.garyzhangscm.cwms.resources.PrinterConfiguration;
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.clients.PrintingServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.PrinterRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class PrinterService  {
    private static final Logger logger = LoggerFactory.getLogger(PrinterService.class);

    @Autowired
    private ReportPrinterConfigurationService reportPrinterConfigurationService;

    @Autowired
    PrintingServiceRestemplateClient printingServiceRestemplateClient;
    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;

    @Autowired
    private PrinterRepository printerRepository;


    @Autowired
    private PrinterConfiguration printerConfiguration;

    public Printer findById(Long id) {
        Printer printer =  printerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("printer not found by id: " + id));
        return printer;
    }

    public List<Printer> findAll(Long warehouseId,
                                 String name,
                                 String printerType) {

        return printerRepository.findAll(
                (Root<Printer> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Strings.isNotBlank(name)) {

                        if (name.contains("*")) {

                            predicates.add(criteriaBuilder.like(root.get("name"), name.replaceAll("\\*", "%")));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("name"), name));
                        }

                    }
                    if (Strings.isNotBlank(printerType)) {
                        Join<Printer, PrinterType> joinPrinterType= root.join("reportType", JoinType.INNER);

                        if (printerType.contains("*")) {

                            predicates.add(criteriaBuilder.like(joinPrinterType.get("name"), printerType.replaceAll("\\*", "%")));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(joinPrinterType.get("name"), printerType));
                        }
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );



    }

    public Printer findByName(Long warehouseId, String name) {
        return printerRepository.findByWarehouseIdAndName(warehouseId, name);
    }

    public Printer save(Printer printer) {
        return printerRepository.save(printer);
    }

    public Printer saveOrUpdate(Printer printer) {
        if (Objects.isNull(printer.getId()) &&
                !Objects.isNull(findByName(printer.getWarehouseId(), printer.getName()))) {
            printer.setId(findByName(printer.getWarehouseId(), printer.getName()).getId());
        }
        return save(printer);
    }

    public Printer addPrinter(Printer printer) {
        return saveOrUpdate(printer);

    }

    public void delete(Long id) {
        printerRepository.deleteById(id);

    }
    public List<String> getServerPrinters(Long warehouseId, String printingStrategyName) {
        // there're 3 ways to print document
        // 1. print from the server that host this resource service
        // 2. print from a centralized client but data from server
        // 3. print from local by Ldoop
        // for option 1, we will connect to the printing service and get all printers that connect to the printing service
        // 's host. In this way there's no need to setup anything in the system, you will only need to connect the printer
        // to the server that host the printing service(as of now, it is integrated inthe local plug service)
        // for option 2, the printing request will be saved on server but printed from a local PC that has local plugin service
        // installed. In this case, you will need to setup the printer in the web portal's printer page, then connect the printer
        // to the PC that host the local plugin service
        // for option 3, you will need to install lodop(https://ng-alain.com/components/lodop/zh) locally. Then the web page will
        // send printing request to this plugin and the lodop will get the printer list from the local PC. There's nothing needs to be
        // configured
        // we can tell which option the warehouse is using from the printingStrategy variable. If this variable is not passed in
        // we will get from the warehouse's configuration
        PrintingStrategy printingStrategy;
        if (Strings.isNotBlank(printingStrategyName)) {
            printingStrategy = PrintingStrategy.valueOf(printingStrategyName);
        }
        else {
            WarehouseConfiguration warehouseConfiguration =
                    layoutServiceRestemplateClient.getWarehouseConfiguration(warehouseId);
            if (Objects.isNull(warehouseConfiguration) || Objects.isNull(warehouseConfiguration.getPrintingStrategy())) {
                throw ResourceNotFoundException.raiseException("Can't find the printing strategy for warehouse " + warehouseId);
            }
            printingStrategy = warehouseConfiguration.getPrintingStrategy();
        }

        /**
        if (Boolean.TRUE.equals(printerConfiguration.getTestPrintersOnly())) {
            return printerConfiguration.getTestPrinters().stream().map(printer -> printer.getName()).collect(Collectors.toList());
        }
         **/
        if (printingStrategy.equals(PrintingStrategy.SERVER_PRINTER)) {
            // option 1: get printers that connect to the server that host the printing service(now integrated in local plugin service)
            return printingServiceRestemplateClient.getPrinters();
        }
        else if (printingStrategy.equals(PrintingStrategy.LOCAL_PRINTER_SERVER_DATA)) {
            // option 2: get printers from the printer table
            return findAll(warehouseId, null, null).stream().map(Printer::getName).collect(Collectors.toList());
        }
        else {

            throw ResourceNotFoundException.raiseException("Can't find printers with warehouse " + warehouseId + " and strategy " + printingStrategy);
        }

    }


    public String getPrinter(Long companyId, Long warehouseId, ReportType reportType, String findPrinterByValue, String printerName) {
        // if the printer name is passed it, return it
        if (Strings.isNotBlank(printerName)) {
            return printerName;
        }
        // otherwise, get from the configuration
        return reportPrinterConfigurationService.getPrinterName(
                warehouseId, reportType, findPrinterByValue
        );
    }

    public Printer changePrinter(Long id, Printer printer) {
        printer.setId(id);
        return saveOrUpdate(printer);
    }
}
