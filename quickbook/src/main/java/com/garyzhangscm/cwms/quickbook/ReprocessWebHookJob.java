package com.garyzhangscm.cwms.quickbook;

import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import com.garyzhangscm.cwms.quickbook.model.QuickBookWebhookHistory;
import com.garyzhangscm.cwms.quickbook.service.QuickBookOnlineTokenService;
import com.garyzhangscm.cwms.quickbook.service.QuickBookWebhookHistoryService;
import com.garyzhangscm.cwms.quickbook.service.queue.QueueService;
import com.intuit.oauth2.exception.OAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReprocessWebHookJob {

    private static final Logger logger = LoggerFactory.getLogger(ReprocessWebHookJob.class);


    @Autowired
    private QuickBookWebhookHistoryService quickBookWebhookHistoryService;
    @Autowired
    private QueueService queueService;
    /**
     * refresh token very 1 minutes
     */
    @Scheduled(fixedDelay = 60000)
    public void processPendingWebHook() {
        logger.debug("start to process pending webhook");
        List<QuickBookWebhookHistory> quickBookWebhookHistories =
                quickBookWebhookHistoryService.findPendingWebhookRequest();
        quickBookWebhookHistories.forEach(
                quickBookWebhookHistory -> {

                    logger.debug("reprocess webhook history: id {}, payload \n{}",
                            quickBookWebhookHistory.getId(),
                            quickBookWebhookHistory.getPayload());
                    queueService.add(quickBookWebhookHistory.getPayload());
                }
        );

    }



}
