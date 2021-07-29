package com.garyzhangscm.cwms.integration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class IntegrationProcessingJob {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationProcessingJob.class);

    @Autowired
    private Integration integration;

    @Scheduled(fixedDelay = 2000)
    public void processInboundIntegration() {

        logger.debug("# process inbound integration data @ {}", LocalDateTime.now());
        integration.listen();
    }

    @Scheduled(fixedDelay = 2000)
    public void processOutboundIntegration() {

        logger.debug("# process outbound integration data @ {}", LocalDateTime.now());
        integration.send();
    }


}
