package com.garyzhangscm.cwms.common.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.common.model.Customer;
import com.garyzhangscm.cwms.common.service.IntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Component
@Profile("aws-dev")
public class KafkaReceiver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReceiver.class);

    @Autowired
    IntegrationService integrationService;

    private ObjectMapper mapper = new ObjectMapper();

    @KafkaListener(topics = {"INTEGRATION-CUSTOMER"})
    public void listen(@Payload String customerJsonRepresent,
                       @Headers MessageHeaders headers) throws JsonProcessingException {
        logger.info("# received customer data: {}", customerJsonRepresent);
        Customer customer = mapper.readValue(customerJsonRepresent, Customer.class);
        logger.info("# customer data after parsing: {}", customer);
        integrationService.save(customer);

        logger.debug("# received customer data's  header:");
        headers.keySet().forEach(key -> {
            logger.info("{}: {}", key, headers.get(key));
        });
    }

}
