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

import com.garyzhangscm.cwms.resources.PrinterConfiguration;
import com.garyzhangscm.cwms.resources.clients.PrintingServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.Printer;
import com.garyzhangscm.cwms.resources.model.PrinterType;
import com.garyzhangscm.cwms.resources.model.ReportType;
import com.garyzhangscm.cwms.resources.repository.PrinterRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

                        if (name.contains("%")) {

                            predicates.add(criteriaBuilder.like(root.get("name"), name));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("name"), name));
                        }

                    }
                    if (Strings.isNotBlank(printerType)) {
                        Join<Printer, PrinterType> joinPrinterType= root.join("reportType", JoinType.INNER);

                        if (printerType.contains("%")) {

                            predicates.add(criteriaBuilder.like(joinPrinterType.get("name"), printerType));
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
    public List<String> getServerPrinters() {
        if (Boolean.TRUE.equals(printerConfiguration.getTestPrintersOnly())) {
            return printerConfiguration.getTestPrinters().stream().map(printer -> printer.getName()).collect(Collectors.toList());
        }
        return printingServiceRestemplateClient.getPrinters();
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
