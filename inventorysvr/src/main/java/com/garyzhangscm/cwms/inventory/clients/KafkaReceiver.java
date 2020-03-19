package com.garyzhangscm.cwms.inventory.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.inventory.model.InventoryActivity;
import com.garyzhangscm.cwms.inventory.model.Item;
import com.garyzhangscm.cwms.inventory.model.ItemUnitOfMeasure;
import com.garyzhangscm.cwms.inventory.service.IntegrationService;
import com.garyzhangscm.cwms.inventory.service.InventoryActivityService;
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
@Profile("aws-dev")
public class KafkaReceiver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReceiver.class);

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    IntegrationService integrationService;
    @Autowired
    InventoryActivityService inventoryActivityService;

    @KafkaListener(topics = {"short-allocation"})
    public void listen(ConsumerRecord<?, ?> record) {
        Optional<?> kafkaMessage = Optional.ofNullable(record.value());
        if (kafkaMessage.isPresent()) {
            Object message = kafkaMessage.get();
            logger.info("----------------- record =" + record);
            logger.info("------------------ message =" + message);
        }
    }
    @KafkaListener(topics = {"INTEGRATION-ITEM-UNIT-OF-MEASURE"})
    public void processItemUnitOfMeasureIntegration(@Payload String itemUnitOfMeasureJsonRepresent,
                                                    @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String itemJsonRepresent)  {
        logger.info("# received item unit of measure data: {}", itemUnitOfMeasureJsonRepresent);
        logger.debug("# received item unit of measure data's  header: {}", itemJsonRepresent);
        try {
            Item item = mapper.readValue(itemJsonRepresent, Item.class);
            logger.info("Item: {}", item);
            ItemUnitOfMeasure itemUnitOfMeasure = mapper.readValue(itemUnitOfMeasureJsonRepresent, ItemUnitOfMeasure.class);
            logger.info("itemUnitOfMeasure: {}", itemUnitOfMeasure);

            integrationService.process(item, itemUnitOfMeasure);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }
    @KafkaListener(topics = {"INVENTORY-ACTIVITY"})
    public void processInventoryActivity(@Payload String inventoryActivityJsonRepresent)  {
        logger.info("# received inventory activity data: {}", inventoryActivityJsonRepresent);
        try {
            InventoryActivity inventoryActivity = objectMapper.readValue(inventoryActivityJsonRepresent, InventoryActivity.class);
            logger.info("InventoryActivity: {}", inventoryActivity);

            inventoryActivityService.processInventoryActivityMessage(inventoryActivity);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }

}
