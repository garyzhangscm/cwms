package com.garyzhangscm.cwms.quickbook;

import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import com.garyzhangscm.cwms.quickbook.service.QuickBookOnlineTokenService;
import com.intuit.oauth2.exception.OAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RefreshTokenJob {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenJob.class);


    @Autowired
    private QuickBookOnlineTokenService quickBookOnlineTokenService;
    /**
     * refresh token very 10 minutes
     */
    @Scheduled(fixedDelay = 600000)
    public void refreshToken() {
        logger.debug("start refresh quickbook token job");
        List<QuickBookOnlineToken> quickBookOnlineTokens =
                quickBookOnlineTokenService.findAll(null, null);
        quickBookOnlineTokens.forEach(
                quickBookOnlineToken -> {
                    try {
                        logger.debug("start to refresh quickbook token for " +
                                " company {}, " +
                                " warehouse {}," +
                                " realm id {}",
                                quickBookOnlineToken.getCompanyId(),
                                quickBookOnlineToken.getWarehouseId(),
                                quickBookOnlineToken.getRealmId());
                        quickBookOnlineTokenService.requestRefreshToken(
                                quickBookOnlineToken.getCompanyId(),
                                quickBookOnlineToken.getWarehouseId(),
                                quickBookOnlineToken.getRealmId()
                        );
                    } catch (OAuthException e) {
                        e.printStackTrace();
                    }
                }
        );

    }



}
