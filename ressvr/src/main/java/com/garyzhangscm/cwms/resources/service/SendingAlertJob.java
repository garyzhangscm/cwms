package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class SendingAlertJob {

    private static final Logger logger = LoggerFactory.getLogger(SendingAlertJob.class);

    @Autowired
    private AlertService alertService;

    // send alert every 15 seconds
    @Scheduled(fixedDelay = 15000)
    public void sendAlert() throws IOException {
        logger.debug("# start JOB to send alert @ {}", LocalDateTime.now());
        List<Alert> alerts = alertService.findPendingAlerts();
        logger.debug(">> found  {} alert to be sent", alerts.size());

        alerts.forEach(
                alert -> alertService.sendAlert(alert)
        );


    }




}
