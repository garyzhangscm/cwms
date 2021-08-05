package com.garyzhangscm.cwms.outbound.clients;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.model.OrderActivity;
import com.garyzhangscm.cwms.outbound.model.OrderConfirmation;
import com.garyzhangscm.cwms.outbound.model.WarehouseTransferReceipt;
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

    public void send(OrderActivity orderActivity) {
        try {
            send(orderActivity.getOrderActivityType().toString(), objectMapper.writeValueAsString(orderActivity));
        }
        catch (Exception ex) {
            send("SYSTEM_ERROR", ex.getMessage());
        }
    }

    public void send(OrderConfirmation orderConfirmation) {
        try {
            send("INTEGRATION_ORDER_CONFIRMATION", objectMapper.writeValueAsString(orderConfirmation));
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

}
