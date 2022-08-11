package com.garyzhangscm.cwms.quickbook.controller;

import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineConfiguration;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import com.garyzhangscm.cwms.quickbook.service.QuickBookOnlineConfigurationService;
import com.garyzhangscm.cwms.quickbook.service.QuickBookOnlineTokenService;
import com.intuit.oauth2.exception.OAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class QuickBookOnlineConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(QuickBookOnlineConfigurationController.class);

    @Autowired
    private QuickBookOnlineConfigurationService quickBookOnlineConfigurationService;


    @RequestMapping(value="/online/configuration", method = RequestMethod.GET)
    public QuickBookOnlineConfiguration getConfiguration(
            @RequestParam Long companyId,
            @RequestParam Long warehouseId) {


        return quickBookOnlineConfigurationService.findByWarehouseId(warehouseId);
    }

    @RequestMapping(value="/online/configuration", method = RequestMethod.POST)
    public QuickBookOnlineConfiguration saveConfiguration(
            @RequestParam Long companyId,
            @RequestParam Long warehouseId,
            @RequestBody QuickBookOnlineConfiguration quickBookOnlineConfiguration) throws OAuthException {

        return quickBookOnlineConfigurationService.saveConfiguration(
                companyId, warehouseId, quickBookOnlineConfiguration);
    }


}
