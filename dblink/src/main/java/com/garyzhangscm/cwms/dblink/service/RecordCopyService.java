package com.garyzhangscm.cwms.dblink.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RecordCopyService {

    private static final Logger logger = LoggerFactory.getLogger(RecordCopyService.class);

    @Autowired
    private DBBasedWorkOrderService dbBasedWorkOrderService;

    @Autowired
    private DBBasedItemService dbBasedItemService;
    @Autowired
    private DBBasedItemPackageTypeService dbBasedItemPackageTypeService;

    @Scheduled(fixedDelay = 15000)
    public void copyRecord() {

        logger.debug("# copy data data @ {}", LocalDateTime.now());

        logger.debug("@{}, Start to process work order data", LocalDateTime.now());
        dbBasedWorkOrderService.sendIntegrationData();

        logger.debug("@{}, Start to process item data", LocalDateTime.now());
        dbBasedItemService.sendIntegrationData();


        logger.debug("@{}, Start to process item package type data", LocalDateTime.now());
        dbBasedItemPackageTypeService.sendIntegrationData();

    }
}