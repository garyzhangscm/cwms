package com.garyzhangscm.cwms.inbound.clients;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.inbound.model.*;
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

    // A customized JSON mapper
    @Qualifier("getObjMapper")
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


    public void send(IntegrationResult integrationResult) {
        try {

            // send("INVENTORY-ACTIVITY", mapper.writeValueAsString(inventoryActivity));
            send("INTEGRATION_RESULT", objectMapper.writeValueAsString(integrationResult));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }


    public void send(ReceiptConfirmation receiptConfirmation) {
        try {

            // send("INVENTORY-ACTIVITY", mapper.writeValueAsString(inventoryActivity));
            send("INTEGRATION_CONFIRMATION",
                    IntegrationType.INTEGRATION_RECEIPT_CONFIRMATION.name(),
                    objectMapper.writeValueAsString(receiptConfirmation));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }

    public void send(CustomerReturnOrderConfirmation customerReturnOrderConfirmation) {
        try {

            // send("INVENTORY-ACTIVITY", mapper.writeValueAsString(inventoryActivity));
            send("INTEGRATION_CUSTOMER_RETURN_ORDER_CONFIRMATION",
                    IntegrationType.INTEGRATION_CUSTOMER_RETURN_ORDER_CONFIRMATION.name(),
                    objectMapper.writeValueAsString(customerReturnOrderConfirmation));
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
    public void send(BillableActivity billableActivity) {
        try {

            // send("INVENTORY-ACTIVITY", mapper.writeValueAsString(inventoryActivity));
            send("BILLABLE_ACTIVITY", objectMapper.writeValueAsString(billableActivity));
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
