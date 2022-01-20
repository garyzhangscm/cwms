package com.garyzhangscm.cwms.common.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.service.IntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Component

public class KafkaReceiver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReceiver.class);

    @Autowired
    IntegrationService integrationService;

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaSender kafkaSender;

    // private ObjectMapper mapper = new ObjectMapper();

    @KafkaListener(topics = {"INTEGRATION_CUSTOMER"})
    public void listenForCustomer(@Payload String customerJsonRepresent,
                                  @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String integrationIdJsonRepresent) throws JsonProcessingException {
        logger.info("# received customer data: {}", customerJsonRepresent);
        logger.info("with id {}", objectMapper.readValue(integrationIdJsonRepresent, String.class));

        String[] key = objectMapper.readValue(integrationIdJsonRepresent, String.class).split("-");
        Long warehouseId = Long.parseLong(key[0]);
        Long integrationId = Long.parseLong(key[1]);

        try {
            Customer customer = objectMapper.readValue(customerJsonRepresent, Customer.class);

            logger.info("# customer data after parsing: {}", customer);
            integrationService.save(customer);


            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    null, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_CUSTOMER,
                    true, ""
            );
            kafkaSender.send(integrationResult);
        }
        catch (Exception ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    null, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_CUSTOMER,
                    false, ex.getMessage()
            );
            kafkaSender.send(integrationResult);
        }


    }

    @KafkaListener(topics = {"INTEGRATION_CLIENT"})
    public void listenForClient(@Payload String clientJsonRepresent,
                                @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String integrationIdJsonRepresent) throws JsonProcessingException {
        logger.info("# received client data: {}", clientJsonRepresent);
        logger.info("with id {}", objectMapper.readValue(integrationIdJsonRepresent, String.class));

        String[] key = objectMapper.readValue(integrationIdJsonRepresent, String.class).split("-");
        Long warehouseId = Long.parseLong(key[0]);
        Long integrationId = Long.parseLong(key[1]);

        try {
            Client client = objectMapper.readValue(clientJsonRepresent, Client.class);
            logger.info("# client data after parsing: {}", client);
            integrationService.save(client);

            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    null, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_CLIENT,
                    true, ""
            );
            kafkaSender.send(integrationResult);
        }
        catch (Exception ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    null, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_CLIENT,
                    false, ex.getMessage()
            );
            kafkaSender.send(integrationResult);
        }

    }

    @KafkaListener(topics = {"INTEGRATION_SUPPLIER"})
    public void listenForSupplier(@Payload String supplierJsonRepresent,
                                  @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String integrationIdJsonRepresent) throws JsonProcessingException {
        logger.info("# received supplier data: {}", supplierJsonRepresent);
        logger.info("with id {}", objectMapper.readValue(integrationIdJsonRepresent, String.class));

        String[] key = objectMapper.readValue(integrationIdJsonRepresent, String.class).split("-");
        Long warehouseId = Long.parseLong(key[0]);
        Long integrationId = Long.parseLong(key[1]);


        try {

            Supplier supplier = objectMapper.readValue(supplierJsonRepresent, Supplier.class);
            logger.info("# supplier data after parsing: {}", supplier);
            integrationService.save(supplier);

            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    null, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_SUPPLIER,
                    true, ""
            );
            kafkaSender.send(integrationResult);
        }
        catch (Exception ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    null, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_SUPPLIER,
                    false, ex.getMessage()
            );
            kafkaSender.send(integrationResult);
        }

    }

}
