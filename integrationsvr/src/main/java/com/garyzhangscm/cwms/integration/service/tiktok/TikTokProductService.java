package com.garyzhangscm.cwms.integration.service.tiktok;

import com.garyzhangscm.cwms.integration.clients.TikTokAPIRestemplateClient;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokOrder;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class TikTokProductService {

    private static final Logger logger = LoggerFactory.getLogger(TikTokProductService.class);

    @Autowired
    private TikTokAPIRestemplateClient tikTokAPIRestemplateClient;

    public TikTokProduct queryTiktokProductById(String accessToken, String shopCipher, String productId) {
        return tikTokAPIRestemplateClient.queryTiktokProductById(accessToken, shopCipher, productId);

    }

}
