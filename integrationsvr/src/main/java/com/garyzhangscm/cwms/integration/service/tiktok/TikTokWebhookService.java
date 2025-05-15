package com.garyzhangscm.cwms.integration.service.tiktok;

import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokSellerAuthorizedShop;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokSellerShopIntegrationConfiguration;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokWebhookEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class TikTokWebhookService {

    private static final Logger logger = LoggerFactory.getLogger(TikTokWebhookService.class);


    @Autowired
    private TikTokSellerShopIntegrationConfigurationService tikTokSellerShopIntegrationConfigurationService;

    @Autowired
    private TikTokSellerShopService tikTokSellerShopService;


    @Autowired
    List<TikTokWebhookDataProcessService> tikTokWebhookDataProcessServices;

    private TikTokWebhookDataProcessService getTikTokWebhookDataProcessService(int type) {
        return tikTokWebhookDataProcessServices.stream().filter(
                tikTokWebhookDataProcessService -> tikTokWebhookDataProcessService.canHandleType(type)
        ).findFirst().orElseThrow(
                () ->
                        ResourceNotFoundException.raiseException("can't find class to process tiktok webhook event with type " + type)
        );
    }
    public void processTiktokWebhook(TikTokWebhookEventData tikTokWebhookEventData) {

        // get the service that can process the event
        TikTokWebhookDataProcessService tikTokWebhookDataProcessService =
                getTikTokWebhookDataProcessService(tikTokWebhookEventData.getType());

        tikTokWebhookDataProcessService.processData(
                tikTokWebhookEventData.getShopId(),
                tikTokWebhookEventData.getData());

    }
}
