package com.garyzhangscm.cwms.quickbook.service;

import com.garyzhangscm.cwms.quickbook.clients.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.quickbook.model.Customer;
import com.garyzhangscm.cwms.quickbook.model.Vendor;
import com.garyzhangscm.cwms.quickbook.model.WMSCustomerWrapper;
import com.garyzhangscm.cwms.quickbook.model.WMSSupplierWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerIntegrationService.class);


    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;

    public void sendIntegrationData(Customer customer, Long companyId, Long warehouseId) {

        logger.debug("start to send integration data for customer {} - {} {}",
                customer.getCompanyName(),
                customer.getGivenName(),
                customer.getFamilyName());

        // setup the missing field
        customer.setCompanyId(companyId);
        customer.setWarehouseId(warehouseId);


        try {
            String result = integrationServiceRestemplateClient.sendIntegrationData("customer",
                    new WMSCustomerWrapper(customer));
            logger.debug("# get result " + result);
        }
        catch (Exception ex) {
            ex.printStackTrace();

        }
    }


}
