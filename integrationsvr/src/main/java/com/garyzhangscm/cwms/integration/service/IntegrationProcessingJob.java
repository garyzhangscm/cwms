package com.garyzhangscm.cwms.integration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class IntegrationProcessingJob {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationProcessingJob.class);

    @Value("${host.api.enabled:true}")
    Boolean hostAPIEnabled;

    @Autowired
    private Integration integration;

    @Scheduled(fixedDelay = 2000)
    public void processInboundIntegration() {

        logger.debug("# process inbound integration data @ local date {}", LocalDateTime.now());
        integration.listen();
    }


    // we will on long push the integration data.
    // instead, we will let the client pull data from us
    // the client may be
    // 1. dblink tools: internal tools used for ERP integration via database
    // 2. quickbook online: internal tools to connect to quickbook online
    // 3. quickbook local plugin: local plugin to connect to quickbook desktop
    /**
    @Scheduled(fixedDelay = 2000)
    public void processOutboundIntegration() {

        logger.debug("# process outbound integration data @ local date {}", LocalDateTime.now());

        if (Boolean.TRUE.equals(hostAPIEnabled)) {

            integration.send();
        }
        else {
            logger.debug("Host API Endpoint is not enabled");
        }
    }
    **/


}
