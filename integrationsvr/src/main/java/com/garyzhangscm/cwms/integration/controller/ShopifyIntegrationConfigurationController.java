package com.garyzhangscm.cwms.integration.controller;


import com.garyzhangscm.cwms.integration.model.ClientRestriction;
import com.garyzhangscm.cwms.integration.model.ClientValidationEndpoint;
import com.garyzhangscm.cwms.integration.model.shopify.ShopifyIntegrationConfiguration;
import com.garyzhangscm.cwms.integration.service.shopify.ShopifyIntegrationConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;


@Controller
@RequestMapping(value="/shopify/integration/configuration")
public class ShopifyIntegrationConfigurationController {
    private static final Logger logger = LoggerFactory.getLogger(ShopifyIntegrationConfigurationController.class);

    @Autowired
    private ShopifyIntegrationConfigurationService shopifyIntegrationConfigurationService;


    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ClientValidationEndpoint
    public List<ShopifyIntegrationConfiguration> getShopifyIntegrationConfigurationService(
            @RequestParam Long companyId,
            @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId,
            @RequestParam(name = "shop", required = false, defaultValue = "") String shop,
            ClientRestriction clientRestriction) {

        return shopifyIntegrationConfigurationService.findAll(companyId, clientId, shop, clientRestriction);
    }

}
