package com.garyzhangscm.cwms.integration.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokWebhookEventData;
import com.garyzhangscm.cwms.integration.service.tiktok.TikTokService;
import com.garyzhangscm.cwms.integration.service.tiktok.TikTokWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Endpoint to integrate with tiktok
 * https://partner.tiktokshop.com/docv2/page/656559fcf4488c02dfe8ce82#Back%20To%20Top
 *
 */
@Controller
@RequestMapping(value="/tiktok")
public class TiktokEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(TiktokEndpoint.class);

    @Autowired
    private TikTokService tikTokService;
    @Autowired
    private TikTokWebhookService tikTokWebhookService;


    @Autowired
    @Qualifier("getObjMapper")
    private ObjectMapper objectMapper;

    @Value("${tiktok.redirectUrlAfterAppAuth:https://prod.claytechsuite.com}")
    private String redirectUrlAfterAppAuth;

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
    public ModelAndView processSellerAuthCallback(@RequestParam String code,
                                                  @RequestParam String state) {
        logger.debug("start to process tiktok seller auth call back with code {} and state {}",
                code, state);
        tikTokService.processSellerAuthCallback(code, state);

        return new ModelAndView("redirect:" + redirectUrlAfterAppAuth);
    }
    /**
     public ResponseBodyWrapper<String> processSellerAuthCallback(@RequestParam String code,
     @RequestParam String state) {
     logger.debug("start to process tiktok seller auth call back with code {} and state {}",
     code, state);
     tikTokService.processSellerAuthCallback(code, state);

     return ResponseBodyWrapper.success("shop register. please go back to the CWMS http://prod.claytechsuite.com");
     }
     **/


    /**
     * Webhook URL - Optionally, enter the URL to receive push notifications (it can be the URL of your system).
     * @return
     */
    @RequestMapping(value="/webhook")
    public String processWebhook(@RequestBody String tikTokWebhookEventDataString) {

        /**
         * Order created webhook example:
         * {
         *     "type":1,
         *     "tts_notification_id":"7361542585208735531",
         *     "shop_id":"7495718048120343464",
         *     "timestamp":1713992700,
         *     "data":
         *         {
         *             "is_on_hold_order":true,
         *             "order_id":"576622844933738562",
         *             "order_status":"ON_HOLD",
         *             "update_time":1713992700
         *           }
         *}
         * Order cancelled webhook example:
         * {
         *     "type":11,
         *     "tts_notification_id":"7361547917001312046",
         *     "shop_id":"7495718048120343464",
         *     "timestamp":1713993942,
         *     "data":{
         *         "cancel_id":"4035238689362972738",
         *         "cancel_status":"CANCELLATION_REQUEST_SUCCESS",
         *         "cancellations_role":"BUYER",
         *         "order_id":"576622844933738562",
         *         "update_time":1713993942
         *         }
         * }
         */
        logger.debug("Start to process tiktok webhook with data");
        logger.debug(tikTokWebhookEventDataString);

        try {
            TikTokWebhookEventData tikTokWebhookEventData
                    = objectMapper.readValue(tikTokWebhookEventDataString, TikTokWebhookEventData.class);

            // we will reasonably assume the shop only belong to one company and client

            logger.debug("convert to tikTokWebhookEventData\n{}", tikTokWebhookEventData);

            tikTokWebhookService.processTiktokWebhook(tikTokWebhookEventData);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "success";
    }
}
