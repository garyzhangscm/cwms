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

package com.garyzhangscm.cwms.adminserver.service;

import com.garyzhangscm.cwms.adminserver.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.adminserver.model.*;
import com.garyzhangscm.cwms.adminserver.repository.BillingRateRepository;
import com.garyzhangscm.cwms.adminserver.repository.BillingRequestRepository;
import com.garyzhangscm.cwms.adminserver.repository.InvoiceRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvoiceService {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);
    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    @Autowired
    private BillingRequestService billingRequestService;


    @Autowired
    private List<BillingService> billingServices;

    public Invoice findById(Long id) {
        return findById(id, true);
    }
    public Invoice findById(Long id, boolean includeDetails) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("invoice not found by id: " + id));
        if (Objects.nonNull(invoice) && includeDetails) {
            loadDetails(invoice);
        }
        return invoice;
    }

    public Invoice save(Invoice invoice) {
        return save(invoice, true);
    }
    public Invoice save(Invoice invoice, boolean loadDetails) {
        Invoice newInvoice =  invoiceRepository.save(invoice);
        if (loadDetails) {

            loadDetails(newInvoice);
        }
        return newInvoice;
    }



    /**
     * return the exactly matched record if the exactmath
     * @param companyId
     * @param warehouseId
     * @param number
     * @return
     */
    public List<Invoice> findAll(Long companyId,
                                     Long warehouseId,
                                     Long clientId,
                                     String number) {
        return findAll(companyId, warehouseId, clientId, number, true);
    }


    public List<Invoice> findAll(Long companyId,
                                     Long warehouseId,
                                     Long clientId,
                                     String number,
                                     boolean includeDetails) {

        List<Invoice> invoices =  invoiceRepository.findAll(
                (Root<Invoice> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {

                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    if (Objects.isNull(clientId)) {
                        // if the client id is not passed in, then we will only return the
                        // rate that defined for the warehouse, not for any client
                        predicates.add(criteriaBuilder.isNull(root.get("clientId")));
                    }
                    else {
                        predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
                    }
                    if (StringUtils.isNotBlank(number)) {
                        if (number.contains("%")) {

                            predicates.add(criteriaBuilder.like(root.get("number"), number));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("number"), number));
                        }
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "warehouseId", "clientId")
        );

        if (!invoices.isEmpty() && includeDetails) {

            loadDetails(invoices);
        }

        return invoices;
    }


    private void loadDetails(List<Invoice> invoices) {
        invoices.forEach(
                this::loadDetails
        );
    }
    private void loadDetails(Invoice invoice) {
        if (Objects.nonNull(invoice.getClientId()) &&
                Objects.isNull(invoice.getClient())) {
            invoice.setClient(
                    commonServiceRestemplateClient.getClientById(
                            invoice.getClientId()
                    )
            );
        }
    }

    public Invoice generateInvoice(String number, String referenceNumber, String comment,
                                   LocalDateTime startTime, LocalDateTime endTime,
                                   Long companyId, Long warehouseId, Long clientId) {
        List<BillingRequest> billingRequests = billingServices.stream().map(
                billingService -> billingService.generateBillingRequest(
                        startTime, endTime, companyId, warehouseId, clientId,
                        "", true
                )
        ).collect(Collectors.toList());

        return generateInvoiceFromBillingRequest(
                number, referenceNumber, comment,
                startTime, endTime, companyId, warehouseId, clientId,
                billingRequests
        );

    }
    public String getNextNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "invoice-number");
    }

    public Invoice generateInvoiceFromBillingRequest(
            String number, String referenceNumber, String comment, LocalDateTime startTime,
            LocalDateTime endTime, Long companyId, Long warehouseId, Long clientId,
            List<BillingRequest> billingRequests) {

        Invoice invoice = new Invoice(
                companyId, warehouseId,
                clientId, number, referenceNumber,
                comment, startTime, endTime, 0.0
        );
        billingRequests.forEach(
                billingRequest -> {
                    // if we haven't saved the billing request yet, serialize it first
                    if (Objects.isNull(billingRequest.getId())) {
                        billingRequest = billingRequestService.addBillingRequest(billingRequest);
                    }
                    InvoiceLine invoiceLine = new InvoiceLine(invoice, billingRequest);
                    invoice.addLine(invoiceLine);
                    invoice.setTotalCharge(
                            invoice.getTotalCharge() + invoiceLine.getTotalCharge()
                    );
                }
        );
        return save(invoice);
    }
}
