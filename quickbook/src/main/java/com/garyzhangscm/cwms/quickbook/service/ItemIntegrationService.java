package com.garyzhangscm.cwms.quickbook.service;

import com.garyzhangscm.cwms.quickbook.clients.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.quickbook.model.Item;
import com.garyzhangscm.cwms.quickbook.model.WMSItemWrapper;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ItemIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(ItemIntegrationService.class);


    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;

    public void sendIntegrationData(Item item, Long companyId, Long warehouseId) {

        logger.debug("start to send integration data for item {}", item.getName());

        // setup the missing field
        if (Strings.isBlank(item.getDescription())) {
            item.setDescription(item.getName());
        }
        item.setCompanyId(companyId);
        item.setWarehouseId(warehouseId);


        try {
            String result = integrationServiceRestemplateClient.sendIntegrationData("item",
                    new WMSItemWrapper(item));
            logger.debug("# get result " + result);
        }
        catch (Exception ex) {
            ex.printStackTrace();

        }
    }

}
