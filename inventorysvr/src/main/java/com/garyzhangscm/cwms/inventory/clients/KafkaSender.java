package com.garyzhangscm.cwms.inventory.clients;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.inventory.model.InventoryActivity;
import com.garyzhangscm.cwms.inventory.model.InventoryAdjustmentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaSender {
    private static final Logger logger = LoggerFactory.getLogger(KafkaSender.class);

    @Autowired
    private KafkaTemplate kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;


    public void send(String topic, String message) {

        logger.debug("====> Start to send to kafka: {} / {}"
                     ,topic, message);
        kafkaTemplate.send(topic, message);
    }
    public void send(String topic, String key, String message) {

        logger.debug("====> Start to send to kafka: {} / {} / {}"
                ,topic, key, message);
        kafkaTemplate.send(topic, key, message);
    }


    public void send(InventoryActivity inventoryActivity) {
        try {

            // send("INVENTORY-ACTIVITY", mapper.writeValueAsString(inventoryActivity));
            send("INVENTORY_ACTIVITY", objectMapper.writeValueAsString(inventoryActivity));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }

    public void send(InventoryAdjustmentRequest inventoryAdjustmentRequest) {
        try {

            // send("INVENTORY-ACTIVITY", mapper.writeValueAsString(inventoryActivity));
            send("INVENTORY_ADJUSTMENT_REQUEST_PROCESSED", objectMapper.writeValueAsString(inventoryAdjustmentRequest));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }
}
