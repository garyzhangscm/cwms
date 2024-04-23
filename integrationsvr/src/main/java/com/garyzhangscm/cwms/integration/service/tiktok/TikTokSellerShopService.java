package com.garyzhangscm.cwms.integration.service.tiktok;

import com.garyzhangscm.cwms.integration.clients.TikTokAPIRestemplateClient;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokSellerAuthorizedShop;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokSellerShop;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokSellerShopIntegrationConfiguration;
import com.garyzhangscm.cwms.integration.repository.tiktok.TikTokSellerAuthorizedShopRepository;
import com.garyzhangscm.cwms.integration.repository.tiktok.TikTokSellerShopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;


@Service
public class TikTokSellerShopService {

    private static final Logger logger = LoggerFactory.getLogger(TikTokSellerShopService.class);


    @Autowired
    private TikTokSellerShopRepository tikTokSellerShopRepository;
    @Autowired
    private TikTokSellerAuthorizedShopRepository tikTokSellerAuthorizedShopRepository;
    @Autowired
    private TikTokAPIRestemplateClient tikTokAPIRestemplateClient;

    public TikTokSellerShop save(TikTokSellerShop tikTokSellerShop) {
        return tikTokSellerShopRepository.save(tikTokSellerShop);
    }

    public TikTokSellerAuthorizedShop save(TikTokSellerAuthorizedShop tikTokSellerAuthorizedShop) {
        return tikTokSellerAuthorizedShopRepository.save(tikTokSellerAuthorizedShop);
    }


    public TikTokSellerShop saveOrUpdate(TikTokSellerShop tikTokSellerShop) {
        if (Objects.isNull(tikTokSellerShop.getId()) &&
                Objects.nonNull(findShopByAuthCodeAndShopId(
                        tikTokSellerShop.getAuthCode(),
                        tikTokSellerShop.getShopId()
                ))) {
            tikTokSellerShop.setId(
                    findShopByAuthCodeAndShopId(
                            tikTokSellerShop.getAuthCode(),
                            tikTokSellerShop.getShopId()
                    ).getId()
            );
        }

        return save(tikTokSellerShop);
    }

    public TikTokSellerAuthorizedShop saveOrUpdate(TikTokSellerAuthorizedShop tikTokSellerAuthorizedShop) {
        if (Objects.isNull(tikTokSellerAuthorizedShop.getId()) &&
                Objects.nonNull(findAuthorizedShopByAuthCodeAndShopId(
                        tikTokSellerAuthorizedShop.getAuthCode(),
                        tikTokSellerAuthorizedShop.getShopId()
                ))) {
            tikTokSellerAuthorizedShop.setId(
                    findAuthorizedShopByAuthCodeAndShopId(
                            tikTokSellerAuthorizedShop.getAuthCode(),
                            tikTokSellerAuthorizedShop.getShopId()
                    ).getId()
            );
        }

        return save(tikTokSellerAuthorizedShop);
    }

    private TikTokSellerAuthorizedShop findAuthorizedShopByAuthCodeAndShopId(String authCode, String shopId) {
        return tikTokSellerAuthorizedShopRepository.findByAuthCodeAndShopId(authCode, shopId);
    }
    private TikTokSellerShop findShopByAuthCodeAndShopId(String authCode, String shopId) {
        return tikTokSellerShopRepository.findByAuthCodeAndShopId(authCode, shopId);
    }


    public TikTokSellerShop findByCompanyIdAndClientIdAndShopId(Long companyId, Long clientId, String shopId) {
        return tikTokSellerShopRepository.findByCompanyIdAndClientIdAndShopId(companyId, clientId, shopId);
    }


    /**
     * Setup all the authorized shops for the seller
     * @param tikTokSellerShopIntegrationConfiguration
     */
    public void setupTiktokSellerShops(TikTokSellerShopIntegrationConfiguration tikTokSellerShopIntegrationConfiguration) {
        List<TikTokSellerAuthorizedShop> authorizedShops =
                tikTokAPIRestemplateClient.getAuthorizedShops(tikTokSellerShopIntegrationConfiguration.getAccessToken());

        logger.debug("Got {} authorized shops from seller {}",
                authorizedShops.size(), tikTokSellerShopIntegrationConfiguration.getSellerName());
        authorizedShops.forEach(
                shop -> {
                    shop.setAuthCode(tikTokSellerShopIntegrationConfiguration.getAuthCode());
                    shop.setCompanyId(tikTokSellerShopIntegrationConfiguration.getCompanyId());
                    shop.setClientId(tikTokSellerShopIntegrationConfiguration.getClientId());

                    logger.debug(">> Shop id and name: {} / {}", shop.getShopId(),
                            shop.getName());
                    saveOrUpdate(shop);
                }
        );



    }
}
