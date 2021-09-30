package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.model.DBBasedInventoryAdjustmentConfirmation;
import com.garyzhangscm.cwms.dblink.model.DBBasedOrderConfirmation;
import com.garyzhangscm.cwms.dblink.model.DBBasedReceiptConfirmation;
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
    private DBBasedSupplierService dbBasedSupplierService;

    @Autowired
    private DBBasedWorkOrderService dbBasedWorkOrderService;

    @Autowired
    private DBBasedItemService dbBasedItemService;
    @Autowired
    private DBBasedItemPackageTypeService dbBasedItemPackageTypeService;

    @Autowired
    private DBBasedInventoryAdjustmentConfirmationIntegration dbBasedInventoryAdjustmentConfirmationIntegration;
    @Autowired
    private DBBasedOrderConfirmationIntegration dbBasedOrderConfirmationIntegration;
    @Autowired
    private DBBasedReceiptConfirmationIntegration dbBasedReceiptConfirmationIntegration;

    @Scheduled(fixedDelay = 1000)
    public void copyRecord() {

        logger.debug("# copy data data @ {}", LocalDateTime.now());

        logger.debug("@{}, Start to process work order data", LocalDateTime.now());
        dbBasedWorkOrderService.sendIntegrationData();

        logger.debug("@{}, Start to process item data", LocalDateTime.now());
        dbBasedItemService.sendIntegrationData();


        logger.debug("@{}, Start to process item package type data", LocalDateTime.now());
        dbBasedItemPackageTypeService.sendIntegrationData();


        logger.debug("@{}, Start to process supplier data", LocalDateTime.now());
        dbBasedSupplierService.sendIntegrationData();

    }

    public DBBasedInventoryAdjustmentConfirmation saveIntegration(DBBasedInventoryAdjustmentConfirmation dbBasedInventoryAdjustmentConfirmation) {
        return dbBasedInventoryAdjustmentConfirmationIntegration.save(
                dbBasedInventoryAdjustmentConfirmation
        );
    }

    public DBBasedOrderConfirmation saveIntegration(DBBasedOrderConfirmation dbBasedOrderConfirmation) {
        return dbBasedOrderConfirmationIntegration.save(
                dbBasedOrderConfirmation
        );
    }

    public DBBasedReceiptConfirmation saveIntegration(DBBasedReceiptConfirmation dbBasedReceiptConfirmation) {
        return dbBasedReceiptConfirmationIntegration.save(
                dbBasedReceiptConfirmation
        );
    }
}
