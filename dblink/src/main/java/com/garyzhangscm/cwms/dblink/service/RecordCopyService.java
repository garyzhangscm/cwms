package com.garyzhangscm.cwms.dblink.service;

import com.garyzhangscm.cwms.dblink.client.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.dblink.model.DBBasedInventoryAdjustmentConfirmation;
import com.garyzhangscm.cwms.dblink.model.DBBasedOrderConfirmation;
import com.garyzhangscm.cwms.dblink.model.DBBasedReceiptConfirmation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RecordCopyService {

    private static final Logger logger = LoggerFactory.getLogger(RecordCopyService.class);

    @Autowired
    private DBBasedSupplierService dbBasedSupplierService;

    @Autowired
    private DBBasedWorkOrderService dbBasedWorkOrderService;

    @Autowired
    private DBBasedReceiptService dbBasedReceiptService;

    @Autowired
    private DBBasedItemService dbBasedItemService;
    @Autowired
    private DBBasedCustomerService dbBasedCustomerService;
    @Autowired
    private DBBasedItemPackageTypeService dbBasedItemPackageTypeService;

    @Autowired
    private DBBasedInventoryAdjustmentConfirmationIntegration dbBasedInventoryAdjustmentConfirmationIntegration;
    @Autowired
    private DBBasedOrderConfirmationIntegration dbBasedOrderConfirmationIntegration;
    @Autowired
    private DBBasedReceiptConfirmationIntegration dbBasedReceiptConfirmationIntegration;

    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;

    @Scheduled(fixedDelay = 1000)
    public void copyRecord() {

        logger.debug("# copy data data @ {}", LocalDateTime.now());

        processInboundIntegrationData();



    }

    // send data from intermidiate DB to WMS
    private void processInboundIntegrationData() {


        logger.debug("@{}, Start to process supplier data", LocalDateTime.now());
        dbBasedSupplierService.sendIntegrationData();

        // logger.debug("@{}, Start to process customer data", LocalDateTime.now());
        // dbBasedCustomerService.sendIntegrationData();

        logger.debug("@{}, Start to process item data", LocalDateTime.now());
        dbBasedItemService.sendIntegrationData();

        logger.debug("@{}, Start to process work order data", LocalDateTime.now());
        dbBasedWorkOrderService.sendIntegrationData();

        logger.debug("@{}, Start to process receipt data", LocalDateTime.now());
        dbBasedReceiptService.sendIntegrationData();


        logger.debug("@{}, Start to process item package type data", LocalDateTime.now());
        dbBasedItemPackageTypeService.sendIntegrationData();
    }

    // send data from WMS to intermidiate DB
    private void processOutboundIntegrationData() {


        logger.debug("@{}, Start to process inventory adjustment configuration", LocalDateTime.now());
        syncWMSInventoryAdjustmentConfirmation();

        logger.debug("@{}, Start to process receipt confirmation", LocalDateTime.now());
        syncWMSReceiptConfirmation();

        logger.debug("@{}, Start to process sales order confirmation", LocalDateTime.now());
        syncWMSOrderConfirmation();

    }

    private void syncWMSOrderConfirmation() {
        List<DBBasedOrderConfirmation> dbBasedOrderConfirmations =
                integrationServiceRestemplateClient.getPendingSalesOrderConfirmationIntegrationData();

        dbBasedOrderConfirmations.forEach(
                dbBasedOrderConfirmation -> {
                    saveIntegration(dbBasedOrderConfirmation);
                    // sent the result back to success
                    integrationServiceRestemplateClient.saveIntegrationResult("order-confirmations",
                            dbBasedOrderConfirmation.getId(),
                            true,
                            "");
                }
        );
    }

    private void syncWMSReceiptConfirmation() {
        List<DBBasedReceiptConfirmation> dbBasedReceiptConfirmations =
                integrationServiceRestemplateClient.getPendingReceiptConfirmationIntegrationData();

        dbBasedReceiptConfirmations.forEach(
                dbBasedReceiptConfirmation -> {
                    saveIntegration(dbBasedReceiptConfirmation);
                    // sent the result back to success
                    integrationServiceRestemplateClient.saveIntegrationResult("receipt-confirmations",
                            dbBasedReceiptConfirmation.getId(),
                            true,
                            "");
                }
        );
    }


    private void syncWMSInventoryAdjustmentConfirmation() {

        List<DBBasedInventoryAdjustmentConfirmation> dbBasedInventoryAdjustmentConfirmations =
                integrationServiceRestemplateClient.getPendingInventoryAdjustmentConfirmationIntegrationData();

        dbBasedInventoryAdjustmentConfirmations.forEach(
                dbBasedInventoryAdjustmentConfirmation -> {
                    saveIntegration(dbBasedInventoryAdjustmentConfirmation);
                    // sent the result back to success
                    integrationServiceRestemplateClient.saveIntegrationResult("inventory-adjustment-confirmations",
                            dbBasedInventoryAdjustmentConfirmation.getId(),
                            true,
                            "");
                }
        );
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
