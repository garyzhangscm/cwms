package com.garyzhangscm.cwms.integration.service.tiktok;

import com.garyzhangscm.cwms.integration.clients.TikTokAPIRestemplateClient;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class TikTokOrderService {

    private static final Logger logger = LoggerFactory.getLogger(TikTokOrderService.class);

    @Autowired
    private TikTokAPIRestemplateClient tikTokAPIRestemplateClient;

    public TikTokOrder queryTiktokOrderById(String accessToken, String shopCipher, String orderId) {
        List<TikTokOrder> tikTokOrderList =
                tikTokAPIRestemplateClient.getTikTokOrderDetails(accessToken, shopCipher, orderId);

        return tikTokOrderList.size() == 0 ? null : tikTokOrderList.get(0);
    }

}
