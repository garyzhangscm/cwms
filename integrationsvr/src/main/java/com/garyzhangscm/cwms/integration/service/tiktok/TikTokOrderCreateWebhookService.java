package com.garyzhangscm.cwms.integration.service.tiktok;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.exception.SystemFatalException;
import com.garyzhangscm.cwms.integration.model.InventoryAdjustmentConfirmation;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokOrder;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokOrderCreateWebhookData;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokSellerAuthorizedShop;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokSellerShopIntegrationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


@Service
public class TikTokOrderCreateWebhookService implements TikTokWebhookDataProcessService{

    private static final Logger logger = LoggerFactory.getLogger(TikTokOrderCreateWebhookService.class);

    @Autowired
    private TikTokOrderService tikTokOrderService;

    @Autowired
    private TikTokSellerShopIntegrationConfigurationService tikTokSellerShopIntegrationConfigurationService;
    @Autowired
    private TikTokSellerShopService tikTokSellerShopService;

    // Custmoized JSON mapper
    @Autowired
    @Qualifier("getObjMapper")
    private ObjectMapper objectMapper;

    @Override
    public void processData(String shopId, Object data)  {

        TikTokSellerShopIntegrationConfiguration tikTokSellerShopIntegrationConfiguration
                = tikTokSellerShopIntegrationConfigurationService.getTikTokSellerShopIntegrationConfigurationByShop(
                shopId
        );
        TikTokSellerAuthorizedShop shop = tikTokSellerShopService.findAuthorizedShopByAuthCodeAndShopId(
                tikTokSellerShopIntegrationConfiguration.getAuthCode(),
                shopId
        );

        logger.debug("start to use TikTokOrderCreateWebhookService to process data \n{}", data);

        TikTokOrderCreateWebhookData tikTokOrderCreateWebhookData
                = objectMapper.convertValue(data, TikTokOrderCreateWebhookData.class);
        logger.debug("after convert to TikTokOrderCreateWebhookData \n{}", tikTokOrderCreateWebhookData);

        // start to process the order
        // let's get the order information first

        TikTokOrder tikTokOrder = tikTokOrderService.queryTiktokOrderById(
                tikTokSellerShopIntegrationConfiguration.getAccessToken(),
                shop.getCipher(),
                tikTokOrderCreateWebhookData.getOrderId()
        );

        if (Objects.isNull(tikTokOrder)) {
            logger.debug("fail to find order details by id {}",
                    tikTokOrderCreateWebhookData.getOrderId());
        }
        else {
            logger.debug("found order by id {}\n{}",
                    tikTokOrderCreateWebhookData.getOrderId(),
                    tikTokOrder);
        }



    }

    @Override
    public boolean canHandleType(int webhookType) {
        return webhookType == 1;
    }
}
