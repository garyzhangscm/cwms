package com.garyzhangscm.cwms.integration.service.tiktok;

import com.garyzhangscm.cwms.integration.model.tiktok.TikTokIntegrationAppConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class TikTokIntegrationAppConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(TikTokIntegrationAppConfigurationService.class);



    @Value("${tiktok.appKey:NOT-SET-YET}")
    private String appKey;

    @Value("${tiktok.appSecret:NOT-SET-YET}")
    private String appSecret;

    private static TikTokIntegrationAppConfiguration tikTokIntegrationAppConfiguration;

    /**
     * There should be only one tiktok APP configuration since the whole CWMS will be
     * treat as one APP in tiktok
     * @return
     */
    private TikTokIntegrationAppConfiguration getTikTokIntegrationAppConfiguration() {

        if (Objects.isNull(tikTokIntegrationAppConfiguration)) {
            tikTokIntegrationAppConfiguration = new TikTokIntegrationAppConfiguration(
                    appKey, appSecret
            );
        }

        return tikTokIntegrationAppConfiguration;
    }
}
