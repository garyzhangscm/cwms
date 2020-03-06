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

    @Scheduled(fixedRate = 120000)
    public void processIntegration() {

        logger.debug("# porcess integration data @ {}", LocalDateTime.now());
        integration.listen();
    }


}
