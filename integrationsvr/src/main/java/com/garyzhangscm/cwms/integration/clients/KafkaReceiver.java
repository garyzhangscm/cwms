package com.garyzhangscm.cwms.integration.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.model.InventoryAdjustmentConfirmation;
import com.garyzhangscm.cwms.integration.model.InventoryAttributeChangeConfirmation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;

import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Component
@Profile("aws-dev")
public class KafkaReceiver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReceiver.class);

    @Autowired
    private ObjectMapper objectMapper;



    @KafkaListener(topics = {"INTEGRATION_INVENTORY_ADJUSTMENT_CONFIRMATION"})
    public void processInventoryAdjustmentConfirmationIntegration(@Payload String inventoryAdjustmentConfirmationJsonRepresent)  {
        logger.info("# received inventory adjustment confirmation data: {}", inventoryAdjustmentConfirmationJsonRepresent);

        try {
            InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation
                    = objectMapper.readValue(inventoryAdjustmentConfirmationJsonRepresent, InventoryAdjustmentConfirmation.class);
            logger.info("InventoryAdjustmentConfirmation: {}", inventoryAdjustmentConfirmation);



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



        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }



}
