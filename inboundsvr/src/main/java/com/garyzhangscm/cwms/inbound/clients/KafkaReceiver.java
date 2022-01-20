package com.garyzhangscm.cwms.inbound.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.service.IntegrationService;
import com.garyzhangscm.cwms.inbound.service.ReceiptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
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
    private KafkaSender kafkaSender;
    @Autowired
    ReceiptService receiptService;


    /*
     * Integration process
     * -- Receipt
     * */
    @KafkaListener(topics = {"INTEGRATION_RECEIPT"})
    public void processReceipt(@Payload String receiptJsonRepresent,
                               @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String integrationIdJsonRepresent) throws JsonProcessingException {
        logger.info("# received integration - receipt data:\n {}", receiptJsonRepresent);
        logger.info("with id {}", objectMapper.readValue(integrationIdJsonRepresent, String.class));

        String[] key = objectMapper.readValue(integrationIdJsonRepresent, String.class).split("-");
        logger.debug("keys: {}", key);
        Long warehouseId = Long.parseLong(key[0]);
        Long integrationId = Long.parseLong(key[1]);

        try {
            Receipt receipt = objectMapper.readValue(receiptJsonRepresent, Receipt.class);
            logger.info("receipt: {}", receipt);

            integrationService.process(receipt);

            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    null, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_RECEIPT,
                    true, ""
            );
            kafkaSender.send(integrationResult);
        }
        catch (Exception ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    null, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_RECEIPT,
                    false, ex.getMessage()
            );
            kafkaSender.send(integrationResult);
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
