package com.garyzhangscm.cwms.integration.service.tiktok;

import com.garyzhangscm.cwms.integration.clients.TikTokAPIRestemplateClient;
import com.garyzhangscm.cwms.integration.model.Company;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokIntegrationAppConfiguration;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokSellerShopIntegrationConfiguration;
import com.garyzhangscm.cwms.integration.repository.tiktok.TikTokSellerShopIntegrationConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class TikTokSellerShopIntegrationConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(TikTokSellerShopIntegrationConfigurationService.class);


    @Autowired
    private TikTokSellerShopIntegrationConfigurationRepository tikTokSellerShopIntegrationConfigurationRepository;

    @Autowired
    private TikTokAPIRestemplateClient tikTokAPIRestemplateClient;

    /**
     * For each seller, when the seller first approve the APP, we will get an authCode. We will use this code
     * to get the access token and refresh token
     * @param company
     * @param authCode
     */
    public void initTikTokSellerShopIntegrationConfiguration(Company company, String authCode) {
        tikTokAPIRestemplateClient.getSellerToken(authCode);
    }

    public TikTokSellerShopIntegrationConfiguration getTikTokSellerShopIntegrationConfigurationByCompany(Long companyId) {

        return tikTokSellerShopIntegrationConfigurationRepository.findByCompanyId(companyId);
    }

}
