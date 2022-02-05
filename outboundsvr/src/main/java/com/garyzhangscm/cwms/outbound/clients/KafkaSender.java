package com.garyzhangscm.cwms.outbound.clients;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.model.*;
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

    private void send(String topic, String key, String message) {

        logger.debug("====> Start to send to kafka: {} / {} / {}"
                ,topic, key, message);
        kafkaTemplate.send(topic, key, message);
    }


    public <K, V> void send(String topic, K key, V value) {
        try {
            send(topic, objectMapper.writeValueAsString(key), objectMapper.writeValueAsString(value));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }

    public void send(OrderActivity orderActivity) {
        try {
            send("order_activity", objectMapper.writeValueAsString(orderActivity));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }

    public void send(OrderConfirmation orderConfirmation) {
        try {
            send("INTEGRATION_CONFIRMATION",
                    IntegrationType.INTEGRATION_ORDER_CONFIRMATION.name(),
                    objectMapper.writeValueAsString(orderConfirmation));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }
    public void send(AllocationTransactionHistory allocationTransactionHistory) {
        try {
            send("ALLOCATION_TRANSACTION_HISTORY",
                    objectMapper.writeValueAsString(allocationTransactionHistory));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }


    public void send(WarehouseTransferReceipt warehouseTransferReceipt) {
        try {
            send("WAREHOUSE_TRANSFER_RECEIPT", objectMapper.writeValueAsString(warehouseTransferReceipt));
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

}
