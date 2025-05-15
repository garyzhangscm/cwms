package com.garyzhangscm.cwms.outbound.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ShortAllocationProcessingJob {

    private static final Logger logger = LoggerFactory.getLogger(ShortAllocationProcessingJob.class);

    @Autowired
    private OutboundConfigurationService outboundConfigurationService;

    @Autowired
    ShortAllocationService shortAllocationService;



    /**
    @Scheduled(fixedDelay = 15000)
    public void processShortAllocation() throws IOException {
        logger.debug("# start JOB to process short allocation data @ {}", LocalDateTime.now());
        if (!outboundConfigurationService.isShortAutoReallocationEnabled()){
            logger.debug("Short allocation auto allocating is not enabled, return");
            return;
        }
        setupOAuth2Context();

        List<ShortAllocation> shortAllocationList = shortAllocationService.findAll();

        shortAllocationList.forEach(shortAllocation ->
                shortAllocationService.processShortAllocation(shortAllocation)
        );
        logger.debug("# {} short allocation allocated @ {}",
                shortAllocationList.size() , LocalDateTime.now());
    }
     **/




}
