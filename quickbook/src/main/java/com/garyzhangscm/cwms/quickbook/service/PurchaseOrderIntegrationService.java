package com.garyzhangscm.cwms.quickbook.service;

import com.garyzhangscm.cwms.quickbook.clients.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.quickbook.model.PurchaseOrder;
import com.garyzhangscm.cwms.quickbook.model.Vendor;
import com.garyzhangscm.cwms.quickbook.model.WMSPurchaseOrderWrapper;
import com.garyzhangscm.cwms.quickbook.model.WMSSupplierWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PurchaseOrderIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderIntegrationService.class);


    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;

    public void sendIntegrationData(PurchaseOrder purchaseOrder, Long companyId, Long warehouseId) {

        logger.debug("start to send integration data for purchase order {}",
                purchaseOrder.getDocNumber());

        // setup the missing field
        purchaseOrder.setCompanyId(companyId);
        purchaseOrder.setWarehouseId(warehouseId);


        try {
            String result = integrationServiceRestemplateClient.sendIntegrationData("purchase-orders",
                    new WMSPurchaseOrderWrapper(purchaseOrder));
            logger.debug("# get result " + result);
        }
        catch (Exception ex) {
            ex.printStackTrace();

        }
    }


}
