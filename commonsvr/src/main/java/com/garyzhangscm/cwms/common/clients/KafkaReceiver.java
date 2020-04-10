package com.garyzhangscm.cwms.common.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.model.Customer;
import com.garyzhangscm.cwms.common.model.Supplier;
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

    @Autowired
    private ObjectMapper objectMapper;

    // private ObjectMapper mapper = new ObjectMapper();

    @KafkaListener(topics = {"INTEGRATION-CUSTOMER"})
    public void listenForCustomer(@Payload String customerJsonRepresent) throws JsonProcessingException {
        logger.info("# received customer data: {}", customerJsonRepresent);
        Customer customer = objectMapper.readValue(customerJsonRepresent, Customer.class);
        logger.info("# customer data after parsing: {}", customer);
        integrationService.save(customer);

    }

    @KafkaListener(topics = {"INTEGRATION-CLIENT"})
    public void listenForClient(@Payload String clientJsonRepresent) throws JsonProcessingException {
        logger.info("# received client data: {}", clientJsonRepresent);
        Client client = objectMapper.readValue(clientJsonRepresent, Client.class);
        logger.info("# client data after parsing: {}", client);
        integrationService.save(client);
    }

    @KafkaListener(topics = {"INTEGRATION-SUPPLIER"})
    public void listenForSupplier(@Payload String supplierJsonRepresent) throws JsonProcessingException {
        logger.info("# received supplier data: {}", supplierJsonRepresent);
        Supplier supplier = objectMapper.readValue(supplierJsonRepresent, Supplier.class);
        logger.info("# supplier data after parsing: {}", supplier);
        integrationService.save(supplier);
    }

}
