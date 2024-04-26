package com.garyzhangscm.cwms.integration.service.tiktok;



public interface TikTokWebhookDataProcessService {

    public void processData(String shopId, Object data);

    public boolean canHandleType(int webhookType);
}
