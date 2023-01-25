package com.garyzhangscm.cwms.adminserver.controller;


import com.garyzhangscm.cwms.adminserver.ResponseBodyWrapper;
import com.garyzhangscm.cwms.adminserver.model.BillingRequest;
import com.garyzhangscm.cwms.adminserver.model.Invoice;
import com.garyzhangscm.cwms.adminserver.model.InvoiceDocument;
import com.garyzhangscm.cwms.adminserver.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam Long companyId,
            @RequestParam Long warehouseId,
            @RequestParam Long clientId
    )  {
        return invoiceService.generateInvoice(number, referenceNumber, comment,
                startTime, endTime,
                companyId, warehouseId, clientId
        );
    }

    /**
     * Vendor can submit invoice to the system
     * @param number
     * @param referenceNumber
     * @param comment
     * @param invoiceDate
     * @param dueDate
     * @param companyId
     * @param warehouseId
     * @param clientId
     * @param totalCharge
     * @return
     */
    @RequestMapping(value="/invoices/vendor", method = RequestMethod.POST)
    public Invoice generateVendorInvoice(
            @RequestParam String number,
            @RequestParam(name = "referenceNumber", required = false, defaultValue = "") String referenceNumber,
            @RequestParam(name = "comment", required = false, defaultValue = "") String comment,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate dueDate,
            @RequestParam Long companyId,
            @RequestParam Long warehouseId,
            @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId,
            @RequestParam Double totalCharge,
            @RequestBody List<InvoiceDocument> invoiceDocuments
    ) throws IOException {
        return invoiceService.generateVendorInvoice(number, referenceNumber, comment,
                invoiceDate, dueDate,
                companyId, warehouseId, clientId, totalCharge, invoiceDocuments
        );
    }

    @RequestMapping(value="/invoices/from-billing-request", method = RequestMethod.POST)
    public Invoice generateInvoiceFromBillingRequest(
            @RequestParam String number,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
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

    @RequestMapping(method=RequestMethod.POST, value="/invoices/upload/pdf/{companyId}/{warehouseId}")
    public ResponseBodyWrapper uploadInvoiceDocument(
            @PathVariable Long companyId,
            @PathVariable Long warehouseId,
            @RequestParam("file") MultipartFile file) throws IOException {


        String filePath = invoiceService.uploadInvoiceDocument(companyId, warehouseId, file);
        return  ResponseBodyWrapper.success(filePath);
    }
    @RequestMapping(method=RequestMethod.POST, value="/invoices/upload/pdf/{companyId}/{warehouseId}/{invoiceId}")
    public ResponseBodyWrapper uploadInvoiceDocument(
            @PathVariable Long companyId,
            @PathVariable Long warehouseId,
            @PathVariable Long invoiceId,
            @RequestParam("file") MultipartFile file) throws IOException {


        String filePath = invoiceService.uploadInvoiceDocument(companyId, warehouseId, invoiceId, file);
        return  ResponseBodyWrapper.success(filePath);
    }
}
