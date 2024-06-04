package com.garyzhangscm.cwms.integration.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.ResponseBodyWrapper;
import com.garyzhangscm.cwms.integration.model.shopify.OAuthUrl;
import com.garyzhangscm.cwms.integration.service.shopify.OAuthService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * Endpoint to process OAuth with shop
 * https://shopify.dev/docs/apps/build/authentication-authorization/get-access-tokens/auth-code-grant/implement-auth-code-grants-manually#redirect-using-a-3xx-redirect
 *
 */
@Controller
@RequestMapping(value="/shopify/shop-oauth")
public class ShopifyOAuthController {
    private static final Logger logger = LoggerFactory.getLogger(ShopifyOAuthController.class);

    @Autowired
    private OAuthService oAuthService;


    @Autowired
    @Qualifier("getObjMapper")
    private ObjectMapper objectMapper;


    /**
     * Webhook URL - Optionally, enter the URL to receive push notifications (it can be the URL of your system).
     * @return
     */
    @ResponseBody
    @RequestMapping(value="/url")
    public OAuthUrl getOAuthUrl(@RequestParam Long companyId,
                                @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId) {

        return oAuthService.findByCompanyIdAndClientId(companyId, clientId);
    }


    @RequestMapping(value="/process-shop-oauth")
    public ModelAndView processShopOAuth(@RequestParam(name = "hmac", required = false, defaultValue = "") String hmac,
                                   @RequestParam(name = "shop", required = false, defaultValue = "") String shop,
                                   @RequestParam(name = "timestamp", required = false, defaultValue = "") String timestamp) {

        logger.debug("Start to process shop oauth with parameters");
        logger.debug("hmac: {}\nshop: {}\ntimestamp: {}\n",
                Strings.isBlank(hmac) ? "N/A": hmac,
                Strings.isBlank(shop) ? "N/A": shop,
                Strings.isBlank(timestamp) ? "N/A": timestamp);

        boolean result = oAuthService.validateShopOAuthRequest(hmac, shop, timestamp);
        logger.debug("validate the parameters, result is {}", result);

        // redirect to the URL for user's authorization
        // https://{shop}.myshopify.com/admin/oauth/authorize?
        // client_id={client_id}
        // &scope={scopes}
        // &redirect_uri={redirect_uri}
        // &state={nonce}
        // &grant_options[]={access_mode}
        String redirectUrlForUserAuthorization = oAuthService.getRedirectUrlForUserAuthorization(shop);
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

        boolean result = oAuthService.validateShopOAuthRequest(hmac, shop, timestamp);
        logger.debug("validate the parameters, result is {}", result);

        String redirectUrlAfterUserAuthorization = "https://prod.claytechsuite.com/#/integration/tiktok/tts/config";
        logger.debug("Redirect after user authorization\n{}", redirectUrlAfterUserAuthorization);
        return new ModelAndView("redirect:" + redirectUrlAfterUserAuthorization);
    }
}
