package com.garyzhangscm.cwms.inbound.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.inbound.model.Receipt;
import com.garyzhangscm.cwms.inbound.model.ReceiptStatus;
import com.garyzhangscm.cwms.inbound.service.IntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Profile("aws-dev")
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
    @KafkaListener(topics = {"INTEGRATION_RECEIPT"})
    public void processReceipt(@Payload String receiptJsonRepresent)  {
        logger.info("# received integration - receipt data:\n {}", receiptJsonRepresent);
        try {
            Receipt receipt = objectMapper.readValue(receiptJsonRepresent, Receipt.class);

            logger.info("receipt: {}", receipt);


            integrationService.process(receipt);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }




}
