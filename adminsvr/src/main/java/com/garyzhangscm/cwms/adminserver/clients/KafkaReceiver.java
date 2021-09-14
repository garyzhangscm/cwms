package com.garyzhangscm.cwms.adminserver.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.adminserver.model.BillableRequest;
import com.garyzhangscm.cwms.adminserver.service.BillableRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component

public class KafkaReceiver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReceiver.class);

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BillableRequestService billableRequestService;


    @KafkaListener(topics = {"BILLABLE_REQUEST"})
    public void processBillableRequest(@Payload String itemFamilyJsonRepresent)  {
        logger.info("# received billable request : {}", itemFamilyJsonRepresent);
        try {
            BillableRequest billableRequest = objectMapper.readValue(itemFamilyJsonRepresent, BillableRequest.class);
            logger.info("BillableRequest: {}", billableRequest);

            billableRequestService.save(billableRequest);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }



}
