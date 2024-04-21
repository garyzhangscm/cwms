package com.garyzhangscm.cwms.integration.controller;


import com.garyzhangscm.cwms.integration.model.tiktok.TikTokSellerShopIntegrationConfiguration;
import com.garyzhangscm.cwms.integration.service.tiktok.TikTokSellerShopIntegrationConfigurationService;
import com.garyzhangscm.cwms.integration.service.tiktok.TikTokService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ws.server.endpoint.annotation.Endpoint;

@Endpoint
@RequestMapping(value="/tiktok", method = RequestMethod.GET)
public class TiktokSellerShopIntegrationConfigurationController {
    private static final Logger logger = LoggerFactory.getLogger(TiktokSellerShopIntegrationConfigurationController.class);

    @Autowired
    private TikTokSellerShopIntegrationConfigurationService tikTokSellerShopIntegrationConfigurationService;


    /**
     * Webhook URL - Optionally, enter the URL to receive push notifications (it can be the URL of your system).
     * @return
     */
    @RequestMapping(value="/seller-shop-integration/configuration", method = RequestMethod.GET)
    public TikTokSellerShopIntegrationConfiguration getTikTokSellerShopIntegrationConfigurationByCompany(@RequestParam Long companyId) {
        return tikTokSellerShopIntegrationConfigurationService.getTikTokSellerShopIntegrationConfigurationByCompany(companyId);
    }
}
