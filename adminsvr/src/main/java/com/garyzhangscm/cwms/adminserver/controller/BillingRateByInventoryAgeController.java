package com.garyzhangscm.cwms.adminserver.controller;


import com.garyzhangscm.cwms.adminserver.ResponseBodyWrapper;
import com.garyzhangscm.cwms.adminserver.model.BillingRate;
import com.garyzhangscm.cwms.adminserver.model.BillingRateByInventoryAge;
import com.garyzhangscm.cwms.adminserver.service.BillingRateByInventoryAgeService;
import com.garyzhangscm.cwms.adminserver.service.BillingRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BillingRateByInventoryAgeController {

    @Autowired
    BillingRateByInventoryAgeService billingRateByInventoryAgeService;

    @RequestMapping(value="/billing-rate-by-inventory-ages", method = RequestMethod.GET)
    public List<BillingRateByInventoryAge> getBillingRateByInventoryAges(
            @RequestParam Long companyId,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "") Long warehouseId,
            @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId,
            @RequestParam(name = "startInventoryAge", required = false, defaultValue = "") Integer startInventoryAge,
            @RequestParam(name = "endInventoryAge", required = false, defaultValue = "") Integer endInventoryAge,
            @RequestParam(name = "includeDetails", required = false, defaultValue = "") Boolean includeDetails
            )  {
        return billingRateByInventoryAgeService.findAll(
                companyId, warehouseId, clientId, startInventoryAge, endInventoryAge, false, includeDetails
        );
    }

    @RequestMapping(value="/billing-rate-by-inventory-ages/{id}", method = RequestMethod.GET)
    public BillingRateByInventoryAge getBillingRateByInventoryAge(
            @RequestParam Long companyId,
            @PathVariable Long id
    )  {
        return billingRateByInventoryAgeService.findById(id);
    }
    @RequestMapping(value="/billing-rate-by-inventory-ages/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeBillingRateByInventoryAge(
            @RequestParam Long companyId,
            @PathVariable Long id
    )  {
        billingRateByInventoryAgeService.removeBillingRateByInventoryAge(id);

        return ResponseBodyWrapper.success("billing rate by inventory age with id " + id + " is removed");
    }


    @RequestMapping(value="/billing-rate-by-inventory-ages", method = RequestMethod.POST)
    public BillingRateByInventoryAge saveBillingRateByInventoryAge(
            @RequestParam Long companyId,
            @RequestBody BillingRateByInventoryAge billingRateByInventoryAge
    )  {
        return billingRateByInventoryAgeService.saveBillingRateByInventoryAge(billingRateByInventoryAge);
    }

    @RequestMapping(value="/billing-rate-by-inventory-ages/batch", method = RequestMethod.POST)
    public List<BillingRateByInventoryAge> saveBillingRateByInventoryAges(
            @RequestParam Long companyId,
            @RequestBody List<BillingRateByInventoryAge> billingRateByInventoryAges
    )  {
        return billingRateByInventoryAgeService.saveBillingRateByInventoryAges(billingRateByInventoryAges);
    }

}
