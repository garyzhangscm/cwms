package com.garyzhangscm.cwms.integration.service.tiktok;

import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.model.Company;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;


@Service
public class TikTokService {

    private static final Logger logger = LoggerFactory.getLogger(TikTokService.class);


    @Autowired
    private TikTokSellerShopIntegrationConfigurationService tikTokSellerShopIntegrationConfigurationService;

    /**
     * When the seller authorizes our APP, we will save the auth code, which we will use
     * later on to get the access code.
     * Auth code: permanent value, as long as the seller auth the APP
     * access code(refresh code): temporary code, we will need the auth code to initial a
     *               access code and use the refresh code to get new access code, when it is
     *               expired. Access code is used to get the information from the seller via API
     * @param authCode auth code
     * @param state identifier used to identify the compay information for this seller
     */
    public void processSellerAuthCallback(String authCode, String state) {
        tikTokSellerShopIntegrationConfigurationService.initTikTokSellerShopIntegrationConfiguration(
            authCode, state
        );
    }


}
