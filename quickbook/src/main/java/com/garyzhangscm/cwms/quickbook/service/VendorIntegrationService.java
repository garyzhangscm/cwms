package com.garyzhangscm.cwms.quickbook.service;

import com.garyzhangscm.cwms.quickbook.clients.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.quickbook.model.WMSSupplierWrapper;
import com.garyzhangscm.cwms.quickbook.model.Vendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VendorIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(VendorIntegrationService.class);


    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;

    public void sendIntegrationData(Vendor vendor, Long companyId, Long warehouseId) {

        logger.debug("start to send integration data for vendor {} - {} {}",
                vendor.getCompanyName(),
                vendor.getGivenName(),
                vendor.getFamilyName());

        // setup the missing field
        vendor.setCompanyId(companyId);
        vendor.setWarehouseId(warehouseId);


        try {
            String result = integrationServiceRestemplateClient.sendIntegrationData("suppliers",
                    new WMSSupplierWrapper(vendor));
            logger.debug("# get result " + result);
        }
        catch (Exception ex) {
            ex.printStackTrace();

        }
    }


}
