package com.garyzhangscm.cwms.quickbook.service;

import com.garyzhangscm.cwms.quickbook.clients.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.quickbook.model.Invoice;
import com.garyzhangscm.cwms.quickbook.model.PurchaseOrder;
import com.garyzhangscm.cwms.quickbook.model.WMSOrderWrapper;
import com.garyzhangscm.cwms.quickbook.model.WMSPurchaseOrderWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OutboundOrderIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(OutboundOrderIntegrationService.class);


    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;

    public void sendIntegrationData(Invoice invoice, Long companyId, Long warehouseId) {

        logger.debug("start to send integration data for invoice {}",
                invoice.getDocNumber());

        // setup the missing field
        invoice.setCompanyId(companyId);
        invoice.setWarehouseId(warehouseId);


        try {
            String result = integrationServiceRestemplateClient.sendIntegrationData("orders",
                    new WMSOrderWrapper(invoice));
            logger.debug("# get result " + result);
        }
        catch (Exception ex) {
            ex.printStackTrace();

        }
    }


}
