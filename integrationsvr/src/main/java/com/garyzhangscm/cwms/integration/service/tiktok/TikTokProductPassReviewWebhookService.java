package com.garyzhangscm.cwms.integration.service.tiktok;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.model.tiktok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service
public class TikTokProductPassReviewWebhookService implements TikTokWebhookDataProcessService{

    private static final Logger logger = LoggerFactory.getLogger(TikTokProductPassReviewWebhookService.class);

    @Autowired
    private TikTokProductService tikTokProductService;

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

        logger.debug("start to use TikTokProductPassReviewWebhookService to process data \n{}", data);

        TikTokProductPassReviewWebhookData tikTokProductPassReviewWebhookData
                = objectMapper.convertValue(data, TikTokProductPassReviewWebhookData.class);
        logger.debug("after convert to tikTokProductPassReviewWebhookData \n{}", tikTokProductPassReviewWebhookData);

        // start to process the order
        // let's get the order information first

        TikTokProduct tikTokProduct = tikTokProductService.queryTiktokProductById(
                tikTokSellerShopIntegrationConfiguration.getAccessToken(),
                shop.getCipher(),
                tikTokProductPassReviewWebhookData.getProductId()
        );

        if (Objects.isNull(tikTokProduct)) {
            logger.debug("fail to find production details by id {}",
                    tikTokProductPassReviewWebhookData.getProductId());
        }
        else {
            logger.debug("found product by id {}\n{}",
                    tikTokProductPassReviewWebhookData.getProductId(),
                    tikTokProduct);
            // when we create a new tiktok product, we will
            // automatically create new SKUs
            saveSKUs(tikTokSellerShopIntegrationConfiguration.getCompanyId(),
                    tikTokSellerShopIntegrationConfiguration.getClientId(),
                    tikTokProduct);
        }



    }

    /**
     *
     * @param companyId
     * @param clientId
     * @param tikTokProduct
     */
    private void saveSKUs(Long companyId, Long clientId, TikTokProduct tikTokProduct) {
    }

    @Override
    public boolean canHandleType(int webhookType) {
        return webhookType == 5;
    }
}
