package com.garyzhangscm.cwms.adminserver.controller;


import com.garyzhangscm.cwms.adminserver.model.BillingRate;
import com.garyzhangscm.cwms.adminserver.model.BillingRequest;
import com.garyzhangscm.cwms.adminserver.service.BillingRateService;
import com.garyzhangscm.cwms.adminserver.service.BillingRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class BillingRequestController {

    @Autowired
    private BillingRequestService billingRequestService;

    @RequestMapping(value="/billing-requests", method = RequestMethod.GET)
    public List<BillingRequest> getBillingRequests(
            @RequestParam Long companyId,
            @RequestParam Long warehouseId,
            @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId,
            @RequestParam(name = "number", required = false, defaultValue = "") String number,
            @RequestParam(name = "includeDetails", required = false, defaultValue = "") Boolean includeDetails
            )  {
        return billingRequestService.findAll(
                companyId, warehouseId, clientId, number,  includeDetails
        );
    }

    @RequestMapping(value="/billing-requests", method = RequestMethod.POST)
    public List<BillingRequest> generateBillingRequests(
            @RequestParam Long companyId,
            @RequestParam Long warehouseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  LocalDateTime endTime,
            @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId,
            @RequestParam(name = "number", required = false, defaultValue = "") String number,
            @RequestParam(name = "serialize", required = false, defaultValue = "") Boolean serialize
    )  {
        return billingRequestService.generateBillingRequest(
                startTime, endTime,
                companyId, warehouseId, clientId, number,  serialize
        );
    }

}