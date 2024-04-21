package com.garyzhangscm.cwms.integration.controller;


import com.garyzhangscm.cwms.integration.repository.tiktok.TikTokSellerShopIntegrationConfigurationRepository;
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
public class TiktokEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(TiktokEndpoint.class);

    @Autowired
    private TikTokService tikTokService;

    /**
     * Endpoint triggered when the seller approve the APP. It will include auth_code, which
     * we can use to get the access_token to access the seller's store
     *
     * From Toktok https://partner.tiktokshop.com/docv2/page/64f1994264ed2e0295f3d631
     * Redirect URL - Enter the URL at which to receive your authorization code.
     *   After the seller authorizes the app, it will jump to this URL (it can be the URL of your system's web page)
     *   and transmit the auth_code, which you can use to get the access_token.
     *
     * @param code seller's authorization code
     * @param state a unique identifier that we generated and append in the AUTH url , which we send to the seller.
     *              The same value will be passed back to the call back redirect url. We can contain company information
     *              in this state value so that in this call back method, we knows which company we will need to attach
     *              this seller to
     * @return
     */
    @RequestMapping(value="/redirect", method = RequestMethod.GET)
    public void processSellerAuthCallback(@RequestParam String code,
                                          @RequestParam String state) {
        tikTokService.processSellerAuthCallback(code, state);
    }


    /**
     * Webhook URL - Optionally, enter the URL to receive push notifications (it can be the URL of your system).
     * @return
     */
    @RequestMapping(value="/webhook", method = RequestMethod.GET)
    public String processWebhook( ) {
        return "success";
    }
}
