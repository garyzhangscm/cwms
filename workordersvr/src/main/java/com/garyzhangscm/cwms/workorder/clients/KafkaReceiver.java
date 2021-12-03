package com.garyzhangscm.cwms.workorder.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.model.BillOfMaterial;
import com.garyzhangscm.cwms.workorder.model.IntegrationResult;
import com.garyzhangscm.cwms.workorder.model.IntegrationType;
import com.garyzhangscm.cwms.workorder.model.WorkOrder;
import com.garyzhangscm.cwms.workorder.service.IntegrationService;
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
    IntegrationService integrationService;
    @Autowired
    private KafkaSender kafkaSender;


    /*
     * Integration process
     * -- WorkOrder
     * */
    @KafkaListener(topics = {"INTEGRATION_WORK_ORDER"})
    public void processWorkOrder(@Payload String orderJsonRepresent,
                                 @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String integrationId)  {
        logger.info("# received integration - work order data:\n {}", orderJsonRepresent);

        try {
            WorkOrder workOrder = objectMapper.readValue(orderJsonRepresent, WorkOrder.class);
            logger.info("WORK ORDER: {}", workOrder);

            integrationService.process(workOrder);



            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    Long.parseLong(integrationId),
                    IntegrationType.INTEGRATION_WORK_ORDER,
                    true, ""
            );
            kafkaSender.send(integrationResult);
        }
        catch (Exception ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    Long.parseLong(integrationId),
                    IntegrationType.INTEGRATION_WORK_ORDER,
                    false, ex.getMessage()
            );
            kafkaSender.send(integrationResult);
        }

    }

    /*
     * Integration process
     * -- B.O.M
     * */
    @KafkaListener(topics = {"INTEGRATION_BILL_OF_MATERIAL"})
    public void processBillOfMaterial(@Payload String bomJsonRepresent,
                                      @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String integrationId)  {
        logger.info("# received integration - bill of material data:\n {}", bomJsonRepresent);

        try {
            BillOfMaterial billOfMaterial = objectMapper.readValue(bomJsonRepresent, BillOfMaterial.class);
            logger.info("billOfMaterial: {}", billOfMaterial);

            integrationService.process(billOfMaterial);



            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    Long.parseLong(integrationId),
                    IntegrationType.INTEGRATION_BILL_OF_MATERIAL,
                    true, ""
            );
            kafkaSender.send(integrationResult);
        }
        catch (Exception ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
            // SEND the integration result back
            IntegrationResult integrationResult = new IntegrationResult(
                    Long.parseLong(integrationId),
                    IntegrationType.INTEGRATION_BILL_OF_MATERIAL,
                    false, ex.getMessage()
            );
            kafkaSender.send(integrationResult);
        }


    }




}
