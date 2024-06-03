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
@RequestMapping(value="/shopify")
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


    @RequestMapping(value="/shop-oauth")
    public ModelAndView processShopOAuth(@RequestParam(name = "hmac", required = false, defaultValue = "") String hmac,
                                   @RequestParam(name = "shop", required = false, defaultValue = "") String shop,
                                   @RequestParam(name = "timestamp", required = false, defaultValue = "") String timestamp) {

        logger.debug("Start to process shop oauth with parameters");
        logger.debug("hmac: {}\nshop: {}\ntimestamp: {}\n",
                Strings.isBlank(hmac) ? "N/A": hmac,
                Strings.isBlank(shop) ? "N/A": shop,
                Strings.isBlank(timestamp) ? "N/A": timestamp);

        boolean result = shopifyIntegrationService.validateShopOAuthRequest(hmac, shop, timestamp);
        logger.debug("validate the parameters, result is {}", result);

        // redirect to the URL for user's authorization
        // https://{shop}.myshopify.com/admin/oauth/authorize?
        // client_id={client_id}
        // &scope={scopes}
        // &redirect_uri={redirect_uri}
        // &state={nonce}
        // &grant_options[]={access_mode}
        String redirectUrlForUserAuthorization = shopifyIntegrationService.getRedirectUrlForUserAuthorization(shop);
        logger.debug("Redirect for user authorization\n{}", redirectUrlForUserAuthorization);
        return new ModelAndView("redirect:" + redirectUrlForUserAuthorization);
    }


    @RequestMapping(value="/user-authorization-callback")
    public ModelAndView processUserAuthorizationCallback(
            @RequestParam(name = "code", required = false, defaultValue = "") String code,
            @RequestParam(name = "hmac", required = false, defaultValue = "") String hmac,
            @RequestParam(name = "host", required = false, defaultValue = "") String host,
            @RequestParam(name = "shop", required = false, defaultValue = "") String shop,
            @RequestParam(name = "timestamp", required = false, defaultValue = "") String timestamp) {

        logger.debug("Start to process shop oauth with parameters");
        logger.debug("code:{}\nhmac: {}\nhost: {}\nshop: {}\ntimestamp: {}\n",
                Strings.isBlank(code) ? "N/A": code,
                Strings.isBlank(hmac) ? "N/A": hmac,
                Strings.isBlank(host) ? "N/A": host,
                Strings.isBlank(shop) ? "N/A": shop,
                Strings.isBlank(timestamp) ? "N/A": timestamp);

        boolean result = shopifyIntegrationService.validateShopOAuthRequest(hmac, shop, timestamp);
        logger.debug("validate the parameters, result is {}", result);

        String redirectUrlAfterUserAuthorization = "https://prod.claytechsuite.com/#/integration/tiktok/tts/config";
        logger.debug("Redirect after user authorization\n{}", redirectUrlAfterUserAuthorization);
        return new ModelAndView("redirect:" + redirectUrlAfterUserAuthorization);
    }
}
