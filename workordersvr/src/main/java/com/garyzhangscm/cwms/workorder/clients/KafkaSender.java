package com.garyzhangscm.cwms.workorder.clients;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaSender {
    private static final Logger logger = LoggerFactory.getLogger(KafkaSender.class);

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;
    // private ObjectMapper mapper = new ObjectMapper();

    public void send(String topic, String message) {

        logger.debug("====> Start to send to kafka: {} / {}"
                     ,topic, message);
        kafkaTemplate.send(topic, message);
    }
    public <K, V> void send(String topic, K key, V value) {
        try {
            send(topic, objectMapper.writeValueAsString(key), objectMapper.writeValueAsString(value));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }


    public void send(WorkOrderConfirmation workOrderConfirmation) {
        try {
            send("INTEGRATION_CONFIRMATION",
                    IntegrationType.INTEGRATION_WORK_ORDER_CONFIRMATION.name(),
                    objectMapper.writeValueAsString(workOrderConfirmation));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }

    public void send(ChangeWorkOrderLineDeliveryQuantityRequest changeWorkOrderLineDeliveryQuantityRequest) {
        try {
            send("WORK_ORDER_LINE_DELIVERY_QUANTITY_CHANGE", objectMapper.writeValueAsString(changeWorkOrderLineDeliveryQuantityRequest));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }


    public void send(BillableRequest billableRequest) {
        try {

            // send("INVENTORY-ACTIVITY", mapper.writeValueAsString(inventoryActivity));
            send("BILLABLE_REQUEST", objectMapper.writeValueAsString(billableRequest));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }


    public void send(IntegrationResult integrationResult) {
        try {

            // send("INVENTORY-ACTIVITY", mapper.writeValueAsString(inventoryActivity));
            send("INTEGRATION_RESULT", objectMapper.writeValueAsString(integrationResult));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }


    public void send(Alert alert) {
        try {

            logger.debug("Start to send alert \n{}", alert);
            // send("INVENTORY-ACTIVITY", mapper.writeValueAsString(inventoryActivity));
            send("ALERT", objectMapper.writeValueAsString(alert));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }
}
