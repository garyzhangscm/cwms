package com.garyzhangscm.cwms.integration.clients;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.model.*;
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

    private ObjectMapper mapper = new ObjectMapper();

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


    public void send(Customer customer) {
        try {
            send("INTEGRATION-CUSTOMER", mapper.writeValueAsString(customer));
        }
        catch (Exception ex) {
            send("SYSTEM-ERROR", ex.getMessage());
        }
    }

    public void send(Client client) {
        try {
            send("INTEGRATION-CLIENT", mapper.writeValueAsString(client));
        }
        catch (Exception ex) {
            send("SYSTEM-ERROR", ex.getMessage());
        }
    }

    public void send(Supplier supplier) {
        try {
            send("INTEGRATION-SUPPLIER", mapper.writeValueAsString(supplier));
        }
        catch (Exception ex) {
            send("SYSTEM-ERROR", ex.getMessage());
        }
    }

    public void send(Item item) {
        try {
            send("INTEGRATION-ITEM", mapper.writeValueAsString(item));
        }
        catch (Exception ex) {
            send("SYSTEM-ERROR", ex.getMessage());
        }
    }

    public void send(ItemFamily itemFamily) {
        try {
            send("INTEGRATION-ITEM-FAMILY", mapper.writeValueAsString(itemFamily));
        }
        catch (Exception ex) {
            send("SYSTEM-ERROR", ex.getMessage());
        }
    }

    public void send(ItemPackageType itemPackageType) {
        try {
            send("INTEGRATION-ITEM-PACKAGE-TYPE", mapper.writeValueAsString(itemPackageType));
        }
        catch (Exception ex) {
            send("SYSTEM-ERROR", ex.getMessage());
        }
    }

    public void send(Item item, ItemUnitOfMeasure itemUnitOfMeasure) {
        try {
            send("INTEGRATION-ITEM-UNIT-OF-MEASURE",
                    mapper.writeValueAsString(item), mapper.writeValueAsString(itemUnitOfMeasure));
        }
        catch (Exception ex) {
            send("SYSTEM-ERROR", ex.getMessage());
        }
    }
}
