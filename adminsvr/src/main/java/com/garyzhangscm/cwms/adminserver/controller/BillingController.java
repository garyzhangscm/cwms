package com.garyzhangscm.cwms.adminserver.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.model.BillableRequest;
import com.garyzhangscm.cwms.adminserver.model.BillableRequestSummaryByCompany;
import com.garyzhangscm.cwms.adminserver.service.BillableRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

@RestController
public class BillingController {

    @Autowired
    BillableRequestService billableRequestService;

    @RequestMapping(value="/billing/billable-request-summary/{companyId}", method = RequestMethod.GET)
    public Collection<BillableRequestSummaryByCompany> getBillableRequestSummaryByCompany(
            @PathVariable Long companyId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  LocalDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            )  {
        return billableRequestService.getBillableRequestSummaryByCompany(
                companyId, startTime, endTime, date
        );
    }

    @RequestMapping(value="/billing/billable-request", method = RequestMethod.GET)
    public Collection<BillableRequest> getBillableRequest(
            @RequestParam(name = "companyId", required = false, defaultValue = "") Long companyId,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "") Long warehouseId,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  LocalDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate date
    )  {
        return billableRequestService.findAll(
                companyId, warehouseId, startTime, endTime, date
        );
    }
}
