package com.garyzhangscm.cwms.workorder.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.model.BillOfMaterial;
import com.garyzhangscm.cwms.workorder.model.WorkOrder;
import com.garyzhangscm.cwms.workorder.service.IntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
     * -- WorkOrder
     * */
    @KafkaListener(topics = {"INTEGRATION_WORK_ORDER"})
    public void processWorkOrder(@Payload String orderJsonRepresent)  {
        logger.info("# received integration - work order data:\n {}", orderJsonRepresent);
        try {
            WorkOrder workOrder = objectMapper.readValue(orderJsonRepresent, WorkOrder.class);
            logger.info("WORK ORDER: {}", workOrder);

            integrationService.process(workOrder);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }

    /*
     * Integration process
     * -- B.O.M
     * */
    @KafkaListener(topics = {"INTEGRATION_BILL_OF_MATERIAL"})
    public void processBillOfMaterial(@Payload String orderJsonRepresent)  {
        logger.info("# received integration - bill of material data:\n {}", orderJsonRepresent);
        try {
            BillOfMaterial billOfMaterial = objectMapper.readValue(orderJsonRepresent, BillOfMaterial.class);
            logger.info("billOfMaterial: {}", billOfMaterial);

            integrationService.process(billOfMaterial);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }




}
