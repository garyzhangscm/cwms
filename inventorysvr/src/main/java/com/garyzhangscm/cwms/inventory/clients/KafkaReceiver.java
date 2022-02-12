package com.garyzhangscm.cwms.inventory.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.IntegrationService;
import com.garyzhangscm.cwms.inventory.service.InventoryActivityService;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.util.Strings;
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
    @Autowired
    private KafkaSender kafkaSender;


    /*
     * Integration process
     * -- item
     * -- item package type
     * -- Item unit of measure
     * */

    @KafkaListener(topics = {"INTEGRATION_ITEM_FAMILY"})
    public void processItemFamily(@Payload String itemFamilyJsonRepresent,
                                  @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String integrationIdJsonRepresent) throws JsonProcessingException {
        logger.info("# received integration - item family data: {}", itemFamilyJsonRepresent);
        logger.info("with id {}", objectMapper.readValue(integrationIdJsonRepresent, String.class));

        String[] key = objectMapper.readValue(integrationIdJsonRepresent, String.class).split("-");
        Long warehouseId = Long.parseLong(key[0]);
        Long integrationId = Long.parseLong(key[1]);

        try {

            ItemFamily itemFamily = objectMapper.readValue(itemFamilyJsonRepresent, ItemFamily.class);
            logger.info("Item Family: {}", itemFamily);

            integrationService.process(itemFamily);


            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    null, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_ITEM_FAMILY,
                    true, ""
            );
            kafkaSender.send(integrationResult);
        }
        catch (Exception ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    null, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_ITEM_FAMILY,
                    false, ex.getMessage()
            );
            kafkaSender.send(integrationResult);
        }

    }

    @KafkaListener(topics = {"INTEGRATION_ITEM"})
    public void processItem(@Payload String itemJsonRepresent,
                            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String integrationIdJsonRepresent) throws JsonProcessingException {
        logger.info("# received integration - item data: {}", itemJsonRepresent);
        logger.info("with id {}", objectMapper.readValue(integrationIdJsonRepresent, String.class));

        // for item, the key will be companyId - wareouseId - integrationId,
        // while warehouseId is optional
        String[] key = objectMapper.readValue(integrationIdJsonRepresent, String.class).split("-");
        Long companyId = Long.parseLong(key[0]);
        Long warehouseId = Strings.isNotBlank(key[1]) ? Long.parseLong(key[1]) : null;
        Long integrationId = Long.parseLong(key[2]);

        try {

            Item item = objectMapper.readValue(itemJsonRepresent, Item.class);
            logger.info("Item: {}", item);

            integrationService.process(item);

            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    companyId, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_ITEM,
                    true, ""
            );
            kafkaSender.send(integrationResult);
        }
        catch (Exception ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    companyId, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_ITEM,
                    false, ex.getMessage()
            );
            kafkaSender.send(integrationResult);
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
