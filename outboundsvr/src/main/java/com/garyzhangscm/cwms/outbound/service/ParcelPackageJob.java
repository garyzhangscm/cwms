package com.garyzhangscm.cwms.outbound.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class ParcelPackageJob {

    private static final Logger logger = LoggerFactory.getLogger(ParcelPackageJob.class);

    @Autowired
    private ParcelPackageService parcelPackageService;

    /**
     * Refresh package status every 5 minutes
     * @throws IOException
     */
    @Scheduled(fixedDelay = 300000)
    public void refreshPackageStatus() throws IOException {
        logger.debug("# start to refresh package status @ {}", LocalDateTime.now());

        // right now only refresh for the package that is from hualei system
        parcelPackageService.refreshHualeiPackageStatus();

    }



}
