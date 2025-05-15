package com.garyzhangscm.cwms.integration.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokWebhookEventData;
import com.garyzhangscm.cwms.integration.service.shopify.ShopifyIntegrationService;
import com.garyzhangscm.cwms.integration.service.tiktok.TikTokService;
import com.garyzhangscm.cwms.integration.service.tiktok.TikTokWebhookService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 * Endpoint to integrate with tiktok
 * https://partner.tiktokshop.com/docv2/page/656559fcf4488c02dfe8ce82#Back%20To%20Top
 *
 */
@Controller
@RequestMapping(value="/shopify/integration")
public class ShopifyEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(ShopifyEndpoint.class);

    @Autowired
    private ShopifyIntegrationService shopifyIntegrationService;


    @Autowired
    @Qualifier("getObjMapper")
    private ObjectMapper objectMapper;


    /**
     * Webhook URL - Optionally, enter the URL to receive push notifications (it can be the URL of your system).
     * @return
     */
    @RequestMapping(value="/webhook")
    public String processWebhook(@RequestBody String shopifyWebhookEventDataString) {

        logger.debug("Start to process shopify webhook with data");
        logger.debug(shopifyWebhookEventDataString);

        return "success";
    }

}
