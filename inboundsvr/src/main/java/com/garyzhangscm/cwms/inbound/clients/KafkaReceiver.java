package com.garyzhangscm.cwms.inbound.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.inbound.model.Receipt;
import com.garyzhangscm.cwms.inbound.model.ReceiptStatus;
import com.garyzhangscm.cwms.inbound.model.WarehouseTransferReceipt;
import com.garyzhangscm.cwms.inbound.service.IntegrationService;
import com.garyzhangscm.cwms.inbound.service.ReceiptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component

public class KafkaReceiver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReceiver.class);

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    IntegrationService integrationService;
    @Autowired
    ReceiptService receiptService;


    /*
     * Integration process
     * -- Receipt
     * */
    @KafkaListener(topics = {"INTEGRATION_RECEIPT"})
    public void processReceipt(@Payload String receiptJsonRepresent)  {
        logger.info("# received integration - receipt data:\n {}", receiptJsonRepresent);
        try {
            Receipt receipt = objectMapper.readValue(receiptJsonRepresent, Receipt.class);

            logger.info("receipt: {}", receipt);


            integrationService.process(receipt);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }

    /*
     * Receipt for warehouse transfer
     * */
    @KafkaListener(topics = {"WAREHOUSE_TRANSFER_RECEIPT"})
    public void processWarehouseTransferReceipt(@Payload String warehouseTransferReceiptJsonRepresent)  {
        logger.info("# received warehouse transfer receipt - receipt data:\n {}", warehouseTransferReceiptJsonRepresent);
        try {
            WarehouseTransferReceipt warehouseTransferReceipt =
                    objectMapper.readValue(warehouseTransferReceiptJsonRepresent, WarehouseTransferReceipt.class);

            logger.info("warehouseTransferReceipt: {}", warehouseTransferReceipt);


            receiptService.processWarehouseTransferReceiptRequest(warehouseTransferReceipt);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }



}
