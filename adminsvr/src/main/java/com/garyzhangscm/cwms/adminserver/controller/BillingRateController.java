package com.garyzhangscm.cwms.adminserver.controller;


import com.garyzhangscm.cwms.adminserver.model.BillingRate;
import com.garyzhangscm.cwms.adminserver.service.BillingRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BillingRateController {

    @Autowired
    BillingRateService billingRateService;

    @RequestMapping(value="/billing-rates", method = RequestMethod.GET)
    public List<BillingRate> getBillingRates(
            @RequestParam Long companyId,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "") Long warehouseId,
            @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId,
            @RequestParam(name = "billableCategory", required = false, defaultValue = "") String billableCategory,
            @RequestParam(name = "includeDetails", required = false, defaultValue = "") Boolean includeDetails
            )  {
        return billingRateService.findAll(
                companyId, warehouseId, clientId, billableCategory, false, includeDetails
        );
    }

    @RequestMapping(value="/billing-rates/{id}", method = RequestMethod.GET)
    public BillingRate getBillingRate(
            @RequestParam Long companyId,
            @PathVariable Long id
    )  {
        return billingRateService.findById(id);
    }

    @RequestMapping(value="/billing-rates", method = RequestMethod.POST)
    public BillingRate saveBillingRate(
            @RequestParam Long companyId,
            @RequestBody BillingRate billingRate
    )  {
        return billingRateService.saveBillingRate(billingRate);
    }

    @RequestMapping(value="/billing-rates/batch", method = RequestMethod.POST)
    public List<BillingRate> saveBillingRates(
            @RequestParam Long companyId,
            @RequestBody List<BillingRate> billingRates
    )  {
        return billingRateService.saveBillingRates(billingRates);
    }

}
