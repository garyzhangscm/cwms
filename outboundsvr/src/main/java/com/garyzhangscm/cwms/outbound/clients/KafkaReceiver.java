package com.garyzhangscm.cwms.outbound.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.service.IntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Component
public class KafkaReceiver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReceiver.class);

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    IntegrationService integrationService;


    /*
     * Integration process
     * -- Receipt
     * */
    @KafkaListener(topics = {"INTEGRATION_ORDER"})
    public void processOrder(@Payload String orderJsonRepresent)  {
        logger.info("# received integration - order data:\n {}", orderJsonRepresent);
        try {
            Order order = objectMapper.readValue(orderJsonRepresent, Order.class);
            logger.info("order: {}", order);

            integrationService.process(order);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }




}
