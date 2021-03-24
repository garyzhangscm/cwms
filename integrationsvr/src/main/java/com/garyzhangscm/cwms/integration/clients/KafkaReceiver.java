package com.garyzhangscm.cwms.integration.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.model.*;

import com.garyzhangscm.cwms.integration.service.IntegrationDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;

import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Component

public class KafkaReceiver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReceiver.class);

    // Custmoized JSON mapper
    @Autowired
    @Qualifier("getObjMapper")
    private ObjectMapper objectMapper;

    @Autowired
    private IntegrationDataService integrationDataService;


    @KafkaListener(topics = {"INTEGRATION_INVENTORY_ADJUSTMENT_CONFIRMATION"})
    public void processInventoryAdjustmentConfirmationIntegration(@Payload String inventoryAdjustmentConfirmationJsonRepresent)  {
        logger.info("# received inventory adjustment confirmation data: {}", inventoryAdjustmentConfirmationJsonRepresent);

        try {
            InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation
                    = objectMapper.readValue(inventoryAdjustmentConfirmationJsonRepresent, InventoryAdjustmentConfirmation.class);
            logger.info("InventoryAdjustmentConfirmation: {}", inventoryAdjustmentConfirmation);


            integrationDataService.sendInventoryAdjustmentConfirmationData(inventoryAdjustmentConfirmation);


        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }


    @KafkaListener(topics = {"INTEGRATION_INVENTORY_ATTRIBUTE_CHANGE_CONFIRMATION"})
    public void processInventoryAttributeChangeConfirmationIntegration(@Payload String inventoryAttributeChangeConfirmationJsonRepresent)  {
        logger.info("# received inventory attribute change confirmation data: {}", inventoryAttributeChangeConfirmationJsonRepresent);

        try {
            InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation
                    = objectMapper.readValue(inventoryAttributeChangeConfirmationJsonRepresent, InventoryAttributeChangeConfirmation.class);
            logger.info("inventoryAttributeChangeConfirmation: {}", inventoryAttributeChangeConfirmation);


            integrationDataService.sendInventoryAttributeChangeConfirmationData(inventoryAttributeChangeConfirmation);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }



    @KafkaListener(topics = {"INTEGRATION_ORDER_CONFIRMATION"})
    public void processOrderConfirmationIntegration(@Payload String orderConfirmationJsonRepresent)  {
        logger.info("# received order confirmation data: {}", orderConfirmationJsonRepresent);

        try {
            OrderConfirmation orderConfirmation
                    = objectMapper.readValue(orderConfirmationJsonRepresent, OrderConfirmation.class);
            logger.info("orderConfirmation: {}", orderConfirmation);


            integrationDataService.sendIntegrationOrderConfirmationData(orderConfirmation);


        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }


    @KafkaListener(topics = {"INTEGRATION_WORK_ORDER_CONFIRMATION"})
    public void processWorkOrderConfirmationIntegration(@Payload String workOrderConfirmationJsonRepresent)  {
        logger.info("# received work order confirmation data: {}", workOrderConfirmationJsonRepresent);

        try {
            WorkOrderConfirmation workOrderConfirmation
                    = objectMapper.readValue(workOrderConfirmationJsonRepresent, WorkOrderConfirmation.class);
            logger.info("workOrderConfirmation: {}", workOrderConfirmation);


            integrationDataService.sendIntegrationWorkOrderConfirmationData(workOrderConfirmation);


        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }

    @KafkaListener(topics = {"INTEGRATION_RECEIPT_CONFIRMATION"})
    public void processReceiptConfirmationIntegration(@Payload String receiptConfirmationJsonRepresent)  {
        logger.info("# received inventory adjustment confirmation data: {}", receiptConfirmationJsonRepresent);

        try {
            ReceiptConfirmation receiptConfirmation
                    = objectMapper.readValue(receiptConfirmationJsonRepresent, ReceiptConfirmation.class);
            logger.info("receiptConfirmation: {}", receiptConfirmation);


            integrationDataService.sendIntegrationReceiptConfirmationData(receiptConfirmation);


        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }
}
