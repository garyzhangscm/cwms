package com.garyzhangscm.cwms.outbound.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.AllocationTransactionHistoryService;
import com.garyzhangscm.cwms.outbound.service.IntegrationService;
import com.garyzhangscm.cwms.outbound.service.OrderActivityService;
import com.garyzhangscm.cwms.outbound.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Objects;


@Component
public class KafkaReceiver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReceiver.class);

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    IntegrationService integrationService;

    @Autowired
    private UserService userService;


    @Autowired
    private OrderActivityService orderActivityService;
    @Autowired
    private AllocationTransactionHistoryService allocationTransactionHistoryService;

    @Autowired
    private KafkaSender kafkaSender;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    /*
     * Integration process
     * -- ORDER
     * */
    @KafkaListener(topics = {"INTEGRATION_ORDER"})
    public void processOrder(@Payload String orderJsonRepresent,
                             @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String integrationIdJsonRepresent) throws JsonProcessingException {
        // logger.info("# received integration - order data:\n {}", orderJsonRepresent);
        // logger.info("with id {}", objectMapper.readValue(integrationIdJsonRepresent, String.class));

        String[] key = objectMapper.readValue(integrationIdJsonRepresent, String.class).split("-");
        Long warehouseId = Long.parseLong(key[0]);
        Long integrationId = Long.parseLong(key[1]);

        try {
            Order order = objectMapper.readValue(orderJsonRepresent, Order.class);
            // logger.info("order: {}", order);

            integrationService.process(order);



            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    null, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_ORDER,
                    true, ""
            );
            kafkaSender.send(integrationResult);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            logger.debug("JsonProcessingException: {}", ex.getMessage());
            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    null, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_ORDER,
                    false, ex.getMessage()
            );
            kafkaSender.send(integrationResult);
        }

    }


    /*
     * Allocation request
     * */
    @KafkaListener(topics = {"ALLOCATION_REQUEST"})
    public void processAllocationRequest(@Payload String allocationRequestJsonRepresent,
                                         @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String oAuth2AccessTokenJsonRepresent)  {
        // logger.info("# received integration - allocation request data:\n {}", allocationRequestJsonRepresent);

        // logger.info("# oAuth2AccessTokenJsonRepresent:\n {}", oAuth2AccessTokenJsonRepresent);


    }


    @KafkaListener(topics = {"order_activity"})
    public void processOrderActivity(@Payload String orderActivityJsonRepresent)  {
        // logger.info("# received  order activity data:\n {}", orderActivityJsonRepresent);

        try {
            OrderActivity orderActivity = objectMapper.readValue(orderActivityJsonRepresent, OrderActivity.class);
            // logger.info("orderActivity: {}", orderActivity);


            orderActivityService.addOrderActivity(orderActivity);
        }
        catch (Exception ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
            ex.printStackTrace();
        }

    }

    @KafkaListener(topics = {"ALLOCATION_TRANSACTION_HISTORY"})
    public void processAllocationTransactionHistory(@Payload String allocationTransactionHistoryJsonRepresent)  {
        // logger.info("# received allocation transaction history data:\n {}", allocationTransactionHistoryJsonRepresent);

        try {
            AllocationTransactionHistory allocationTransactionHistory
                    = objectMapper.readValue(allocationTransactionHistoryJsonRepresent, AllocationTransactionHistory.class);
            // logger.info("allocationTransactionHistory: \n{}", allocationTransactionHistory);


            allocationTransactionHistoryService.addAllocationTransactionHistory(allocationTransactionHistory);
        }
        catch (Exception ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
            ex.printStackTrace();
        }

    }

    @KafkaListener(topics = {"INTEGRATION_STOP"})
    public void listenForStop(@Payload String trailerAppointmentJsonRepresent,
                              @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String integrationIdJsonRepresent) throws JsonProcessingException {
        // logger.info("# received trailer appointment data: {}", trailerAppointmentJsonRepresent);
        // logger.info("with id {}", objectMapper.readValue(integrationIdJsonRepresent, String.class));

        String[] key = objectMapper.readValue(integrationIdJsonRepresent, String.class).split("-");
        Long companyId = Long.parseLong(key[0]);
        Long warehouseId = Long.parseLong(key[1]);
        Long integrationId = Long.parseLong(key[2]);


        try {

            TrailerAppointment trailerAppointment =
                    objectMapper.readValue(trailerAppointmentJsonRepresent, TrailerAppointment.class);
            // logger.info("# trailer appointment data after parsing: {}", trailerAppointment);
            integrationService.process(trailerAppointment);

            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    null, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_TRAILER_APPOINTMENT,
                    true, ""
            );
            kafkaSender.send(integrationResult);
        }
        catch (Exception ex) {
            // logger.debug("JsonProcessingException: {}", ex.getMessage());
            ex.printStackTrace();
            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    null, warehouseId, integrationId,
                    IntegrationType.INTEGRATION_TRAILER_APPOINTMENT,
                    false, ex.getMessage()
            );
            kafkaSender.send(integrationResult);
        }

    }

}
