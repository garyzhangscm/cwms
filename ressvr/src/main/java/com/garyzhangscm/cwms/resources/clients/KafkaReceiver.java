package com.garyzhangscm.cwms.resources.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.resources.model.Alert;
import com.garyzhangscm.cwms.resources.model.UserLoginEvent;
import com.garyzhangscm.cwms.resources.service.AlertService;
import com.garyzhangscm.cwms.resources.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component

public class KafkaReceiver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReceiver.class);

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;
    @Autowired
    private AlertService alertService;


    @KafkaListener(topics = {"USER_LOGIN"})
    public void processUserLoginEvent(@Payload String userLoginEventJsonRepresent)  {
        logger.info("# received user login event : {}", userLoginEventJsonRepresent);
        try {
            UserLoginEvent userLoginEvent = objectMapper.readValue(userLoginEventJsonRepresent, UserLoginEvent.class);
            logger.info("userLoginEvent: {}", userLoginEvent);

            userService.recordLoginEvent(userLoginEvent);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }


    @KafkaListener(topics = {"ALERT"})
    public void processALERT(@Payload String alertJsonRepresent)  {
        logger.info("# received alert : {}", alertJsonRepresent);
        try {
            Alert alert = objectMapper.readValue(alertJsonRepresent, Alert.class);
            logger.info("Alert: {}", alert);

            alertService.addAlert(alert);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }


}
