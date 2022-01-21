package com.garyzhangscm.cwms.integration.clients;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class KafkaSender {
    private static final Logger logger = LoggerFactory.getLogger(KafkaSender.class);

    @Autowired
    private KafkaTemplate kafkaTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    private void send(String topic, String message) {

        logger.debug("====> Start to send to kafka: {} / {}"
                     ,topic, message);
        kafkaTemplate.send(topic, message);
    }
    private void send(String topic, String key, String message) {

        logger.debug("====> Start to send to kafka: {} / {} / {}"
                ,topic, key, message);
        kafkaTemplate.send(topic, key, message);
    }



    public <T> void send(IntegrationType integrationType, Long id, T data) {
        try {
            send(integrationType.name(), id.toString(), mapper.writeValueAsString(data));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }

    public <T> void send(IntegrationType integrationType, T data) {
        try {
            send(integrationType.name(), mapper.writeValueAsString(data));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }

    public <K, V> void send(IntegrationType integrationType, K key, V value) {
        try {
            send(integrationType.name(), mapper.writeValueAsString(key), mapper.writeValueAsString(value));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }


    public void send(BillableRequest billableRequest) {
        try {

            // send("INVENTORY-ACTIVITY", mapper.writeValueAsString(inventoryActivity));
            send("BILLABLE_REQUEST", mapper.writeValueAsString(billableRequest));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }

    public void send(Alert alert) {
        try {

                logger.debug("Start to send alert \n{}", alert);
            // send("INVENTORY-ACTIVITY", mapper.writeValueAsString(inventoryActivity));
            send("ALERT", mapper.writeValueAsString(alert));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }
}
