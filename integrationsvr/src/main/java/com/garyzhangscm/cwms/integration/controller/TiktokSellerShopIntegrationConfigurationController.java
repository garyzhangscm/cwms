package com.garyzhangscm.cwms.integration.controller;



import com.garyzhangscm.cwms.integration.ResponseBodyWrapper;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokSellerShopIntegrationConfiguration;
import com.garyzhangscm.cwms.integration.service.tiktok.TikTokSellerShopIntegrationConfigurationService;
import com.garyzhangscm.cwms.integration.service.tiktok.TikTokService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/tiktok-config")
public class TiktokSellerShopIntegrationConfigurationController {
    private static final Logger logger = LoggerFactory.getLogger(TiktokSellerShopIntegrationConfigurationController.class);

    @Autowired
    private TikTokSellerShopIntegrationConfigurationService tikTokSellerShopIntegrationConfigurationService;


    /**
     * Webhook URL - Optionally, enter the URL to receive push notifications (it can be the URL of your system).
     * @return
     */
    @RequestMapping(value="/seller-shop-integration/configuration", method = RequestMethod.GET)
    public List<TikTokSellerShopIntegrationConfiguration> getTikTokSellerShopIntegrationConfigurationByCompany(@RequestParam Long companyId,
                                                                                                               @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId) {
        return tikTokSellerShopIntegrationConfigurationService.getTikTokSellerShopIntegrationConfigurationByCompany(companyId, clientId);
    }

    @RequestMapping(value="/seller-shop-integration/configuration/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeTikTokSellerShopIntegrationConfigurationByCompany(@RequestParam Long companyId,
                                                                                               @PathVariable Long id) {
        tikTokSellerShopIntegrationConfigurationService.removeTikTokSellerShopIntegrationConfigurationByCompany(id);

        return ResponseBodyWrapper.success("tiktok seller shop integration configuration with ID " + id + " is removed!");
    }

    @RequestMapping(value="/seller-shop-integration/configuration/{id}", method = RequestMethod.PUT)
    public TikTokSellerShopIntegrationConfiguration changeTikTokSellerShopIntegrationConfigurationByCompany(@RequestParam Long companyId,
                                                                                               @PathVariable Long id,
                                                                                                            @RequestBody TikTokSellerShopIntegrationConfiguration tikTokSellerShopIntegrationConfiguration) {
        return tikTokSellerShopIntegrationConfigurationService.changeTikTokSellerShopIntegrationConfigurationByCompany(id, tikTokSellerShopIntegrationConfiguration);

    }


    @RequestMapping(value="/seller-shop-integration/seller-auth/state", method = RequestMethod.GET)
    public ResponseBodyWrapper<String> getTikTokSellerShopIntegrationSellerAuthState(@RequestParam Long companyId,
                                                                                     @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId) {
        return ResponseBodyWrapper.success(tikTokSellerShopIntegrationConfigurationService.getStateCode(companyId, clientId));
    }

    @RequestMapping(value="/seller-shop-integration/seller-auth/url", method = RequestMethod.GET)
    public ResponseBodyWrapper<String> getTikTokSellerShopIntegrationSellerAuthUrl(@RequestParam Long companyId,
                                                                                   @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId) {
        return ResponseBodyWrapper.success(tikTokSellerShopIntegrationConfigurationService.getTikTokSellerShopIntegrationSellerAuthUrl(companyId, clientId));
    }
}
