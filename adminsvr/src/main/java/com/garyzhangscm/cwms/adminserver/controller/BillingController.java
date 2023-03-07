package com.garyzhangscm.cwms.adminserver.controller;


import com.garyzhangscm.cwms.adminserver.model.BillableRequest;
import com.garyzhangscm.cwms.adminserver.model.BillableHTTPRequestSummaryByCompany;
import com.garyzhangscm.cwms.adminserver.service.BillableHTTPRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;

@RestController
public class BillingController {

    @Autowired
    BillableHTTPRequestService billableHTTPRequestService;

    @RequestMapping(value="/billing/billable-request-summary/{companyId}", method = RequestMethod.GET)
    public Collection<BillableHTTPRequestSummaryByCompany> getBillableRequestSummaryByCompany(
            @PathVariable Long companyId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            )  {
        return billableHTTPRequestService.getBillableRequestSummaryByCompany(
                companyId, startTime, endTime, date
        );
    }

    @RequestMapping(value="/billing/billable-request", method = RequestMethod.GET)
    public Collection<BillableRequest> getBillableRequest(
            @RequestParam(name = "companyId", required = false, defaultValue = "") Long companyId,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "") Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date
    )  {
        return billableHTTPRequestService.findAll(
                companyId, warehouseId, startTime, endTime, date
        );
    }
}
