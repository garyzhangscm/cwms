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
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.clients.PrintingServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.exception.MissingInformationException;
import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.PrinterRepository;
import com.garyzhangscm.cwms.resources.repository.PrintingRequestRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class PrintingRequestService {
    private static final Logger logger = LoggerFactory.getLogger(PrintingRequestService.class);

    @Autowired
    private PrintingRequestRepository printingRequestRepository;
    @Autowired
    private ReportHistoryService reportHistoryService;

    @Autowired
    private LayoutServiceRestemplateClient layoutServiceRestemplateClient;

    public PrintingRequest findById(Long id) {
        PrintingRequest printingRequest =  printingRequestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException(
                        "printing request not found by id: " + id));
        return printingRequest;
    }

    public List<PrintingRequest> findAll(Long warehouseId,
                                         Boolean notPrintedRequestOnly) {

        return printingRequestRepository.findAll(
                (Root<PrintingRequest> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));


                    if (Boolean.TRUE.equals(notPrintedRequestOnly)) {
                        predicates.add(criteriaBuilder.isNull(root.get("printingTime")));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    public List<PrintingRequest> findPendingPrintingRequest(Long warehouseId) {
        return findAll(warehouseId, true);
    }

    public PrintingRequest save(PrintingRequest printingRequest) {
        return printingRequestRepository.save(printingRequest);
    }


    public PrintingRequest addPrintingRequest(PrintingRequest printingRequest) {
        return save(printingRequest);

    }

    public void delete(Long id) {
        printingRequestRepository.deleteById(id);

    }

    public List<PrintingRequest> processPendingPrintingRequests(Long warehouseId) {
        List<PrintingRequest> printingRequests =
                findPendingPrintingRequest(warehouseId);
        logger.debug("find {} pending printing requests",
                printingRequests.size());

        return printingRequests.stream().map(
                printingRequest -> processPrintingRequest(printingRequest)
        ).collect(Collectors.toList());

    }

    public PrintingRequest processPrintingRequest(PrintingRequest printingRequest) {


        printingRequest.setPrintingTime(LocalDateTime.now());
        return save(printingRequest);
    }

    public List<PrintingRequest> findPendingPrintingRequest(
            Long companyId, String companyCode, Long warehouseId, String warehouseName) {
        if (Objects.nonNull(warehouseId)) {
            return findPendingPrintingRequest(warehouseId);
        }

        // we can get warehouse from
        // 1. company id + warehouse name
        // 2. company code + warehouse name
        if (Objects.nonNull(companyId) && Strings.isNotBlank(warehouseName)) {

            Warehouse warehouse =
                    layoutServiceRestemplateClient.getWarehouseByName(
                            companyId, warehouseName
                    );
            if (Objects.nonNull(warehouse)) {
                return findPendingPrintingRequest(warehouse.getId());
            }
        }
        else if (Strings.isNotBlank(companyCode) && Strings.isNotBlank(warehouseName)) {

            Warehouse warehouse =
                    layoutServiceRestemplateClient.getWarehouseByName(companyCode, warehouseName);
            if (Objects.nonNull(warehouse)) {
                return findPendingPrintingRequest(warehouse.getId());
            }
        }
        logger.debug("we don't have enough information to find the right printing request");
        return new ArrayList<>();
    }

    public PrintingRequest generatePrintingRequestByReportHistory(
            Long warehouseId, Long reportHistoryId, String printerName, Integer copies) {

        ReportHistory reportHistory = reportHistoryService.findById(reportHistoryId);
        PrintingRequest printingRequest = new PrintingRequest(
                warehouseId, reportHistory, printerName, copies
        );

        return save(printingRequest);
    }

    public PrintingRequest markPrintingRequestProcessed(Long warehouseId, Long id) {
        PrintingRequest printingRequest = findById(id);
        printingRequest.setPrintingTime(LocalDateTime.now());
        return save(printingRequest);
    }
}
