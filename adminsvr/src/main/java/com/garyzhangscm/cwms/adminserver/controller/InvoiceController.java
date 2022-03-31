package com.garyzhangscm.cwms.adminserver.controller;


import com.garyzhangscm.cwms.adminserver.model.BillableRequest;
import com.garyzhangscm.cwms.adminserver.model.BillableRequestSummaryByCompany;
import com.garyzhangscm.cwms.adminserver.model.BillingRequest;
import com.garyzhangscm.cwms.adminserver.model.Invoice;
import com.garyzhangscm.cwms.adminserver.service.BillableRequestService;
import com.garyzhangscm.cwms.adminserver.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@RestController
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @RequestMapping(value="/invoices", method = RequestMethod.GET)
    public List<Invoice> getInvoices(
            @RequestParam Long companyId,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "") Long warehouseId,
            @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId,
            @RequestParam(name = "number", required = false, defaultValue = "") String number,
            @RequestParam(name = "includeDetails", required = false, defaultValue = "") Boolean includeDetails
            )  {


        return invoiceService.findAll(
                companyId, warehouseId, clientId, number
        );
    }

    @RequestMapping(value="/invoices", method = RequestMethod.POST)
    public Invoice generateInvoice(
            @RequestParam String number,
            @RequestParam(name = "referenceNumber", required = false, defaultValue = "") String referenceNumber,
            @RequestParam(name = "comment", required = false, defaultValue = "") String comment,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  LocalDateTime endTime,
            @RequestParam Long companyId,
            @RequestParam Long warehouseId,
            @RequestParam Long clientId
    )  {
        return invoiceService.generateInvoice(number, referenceNumber, comment,
                startTime, endTime,
                companyId, warehouseId, clientId
        );
    }

    @RequestMapping(value="/invoices/from-billing-request", method = RequestMethod.POST)
    public Invoice generateInvoiceFromBillingRequest(
            @RequestParam String number,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  LocalDateTime endTime,
            @RequestParam Long companyId,
            @RequestParam Long warehouseId,
            @RequestParam(name = "referenceNumber", required = false, defaultValue = "") String referenceNumber,
            @RequestParam(name = "comment", required = false, defaultValue = "") String comment,
            @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId,
            @RequestBody List<BillingRequest> billingRequests
    )  {
        return invoiceService.generateInvoiceFromBillingRequest(number, referenceNumber, comment,
                startTime, endTime,
                companyId, warehouseId, clientId, billingRequests
        );
    }
}
