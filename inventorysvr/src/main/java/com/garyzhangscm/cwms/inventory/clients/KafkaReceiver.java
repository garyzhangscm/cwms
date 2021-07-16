package com.garyzhangscm.cwms.inventory.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.IntegrationService;
import com.garyzhangscm.cwms.inventory.service.InventoryActivityService;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component

public class KafkaReceiver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReceiver.class);

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    IntegrationService integrationService;
    @Autowired
    InventoryActivityService inventoryActivityService;
    @Autowired
    InventoryService inventoryService;

    @KafkaListener(topics = {"short_allocation"})
    public void listen(ConsumerRecord<?, ?> record) {
        Optional<?> kafkaMessage = Optional.ofNullable(record.value());
        if (kafkaMessage.isPresent()) {
            Object message = kafkaMessage.get();
            logger.info("----------------- record =" + record);
            logger.info("------------------ message =" + message);
        }
    }

    /*
     * Integration process
     * -- item
     * -- item package type
     * -- Item unit of measure
     * */

    @KafkaListener(topics = {"INTEGRATION_ITEM_FAMILY"})
    public void processItemFamily(@Payload String itemFamilyJsonRepresent)  {
        logger.info("# received integration - item family data: {}", itemFamilyJsonRepresent);
        try {
            ItemFamily itemFamily = objectMapper.readValue(itemFamilyJsonRepresent, ItemFamily.class);
            logger.info("Item Family: {}", itemFamily);

            integrationService.process(itemFamily);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }

    @KafkaListener(topics = {"INTEGRATION_ITEM"})
    public void processItem(@Payload String itemJsonRepresent)  {
        logger.info("# received integration - item data: {}", itemJsonRepresent);
        try {
            Item item = objectMapper.readValue(itemJsonRepresent, Item.class);
            logger.info("Item: {}", item);

            integrationService.process(item);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }

    @KafkaListener(topics = {"INTEGRATION_ITEM_PACKAGE_TYPE"})
    public void processItemPackageTypeIntegration(@Payload String itemPackageTypeRepresent,
                                                  @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String itemJsonRepresent)  {
        logger.debug("# received item package type data's  header: {}", itemPackageTypeRepresent);
        logger.debug("# received item unit of measure data's  header: {}", itemJsonRepresent);
        try {
            Item item = objectMapper.readValue(itemJsonRepresent, Item.class);
            logger.info("Item: {}", item);
            ItemPackageType itemPackageType = objectMapper.readValue(itemPackageTypeRepresent, ItemPackageType.class);
            logger.info("Item package type: {}", itemPackageType);

            integrationService.process(item, itemPackageType);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }

    @KafkaListener(topics = {"INTEGRATION_ITEM_UNIT_OF_MEASURE"})
    public void processItemUnitOfMeasureIntegration(@Payload String itemUnitOfMeasureJsonRepresent,
                                                    @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String itemJsonRepresent)  {
        logger.info("# received item unit of measure data: {}", itemUnitOfMeasureJsonRepresent);
        logger.debug("# received item unit of measure data's  header: {}", itemJsonRepresent);
        try {
            Item item = objectMapper.readValue(itemJsonRepresent, Item.class);
            logger.info("Item: {}", item);
            ItemUnitOfMeasure itemUnitOfMeasure = objectMapper.readValue(itemUnitOfMeasureJsonRepresent, ItemUnitOfMeasure.class);
            logger.info("itemUnitOfMeasure: {}", itemUnitOfMeasure);

            integrationService.process(item, itemUnitOfMeasure);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }
    @KafkaListener(topics = {"INVENTORY_ACTIVITY"})
    public void processInventoryActivity(@Payload String inventoryActivityJsonRepresent)  {
        logger.info("# received inventory activity data: {}", inventoryActivityJsonRepresent);
        try {
            InventoryActivity inventoryActivity = objectMapper.readValue(inventoryActivityJsonRepresent, InventoryActivity.class);
            // logger.info("InventoryActivity: {}", inventoryActivity);

            inventoryActivityService.processInventoryActivityMessage(inventoryActivity);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }
    @KafkaListener(topics = {"INVENTORY_ADJUSTMENT_REQUEST_PROCESSED"})
    public void processInventoryAdjustRequest(@Payload String processInventoryAdjustRequestJsonRepresent)  {
        logger.info("# received inventory adjust request data: {}", processInventoryAdjustRequestJsonRepresent);
        try {
            InventoryAdjustmentRequest inventoryAdjustmentRequest
                    = objectMapper.readValue(processInventoryAdjustRequestJsonRepresent, InventoryAdjustmentRequest.class);
            // logger.info("InventoryActivity: {}", inventoryAdjustmentRequest);

            inventoryService.processInventoryAdjustRequest(inventoryAdjustmentRequest);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }

}
