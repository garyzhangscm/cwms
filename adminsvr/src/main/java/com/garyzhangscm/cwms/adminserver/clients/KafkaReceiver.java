package com.garyzhangscm.cwms.adminserver.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.adminserver.model.BillableActivity;
import com.garyzhangscm.cwms.adminserver.model.BillableRequest;
import com.garyzhangscm.cwms.adminserver.model.WarehouseConfiguration;
import com.garyzhangscm.cwms.adminserver.service.BillableActivityService;
import com.garyzhangscm.cwms.adminserver.service.BillableRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component

public class KafkaReceiver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReceiver.class);

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BillableRequestService billableRequestService;
    @Autowired
    private BillableActivityService billableActivityService;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @KafkaListener(topics = {"BILLABLE_REQUEST"})
    public void processBillableRequest(@Payload String billableRequestJsonRepresent)  {
        logger.info("# received billable request : {}", billableRequestJsonRepresent);

        try {
            BillableRequest billableRequest = objectMapper.readValue(billableRequestJsonRepresent, BillableRequest.class);
            logger.info("BillableRequest: {}", billableRequest);
            if (Objects.nonNull(billableRequest.getWarehouseId())) {
                // see if the billable request process is enabled in the warehouse level
                WarehouseConfiguration warehouseConfiguration
                        = warehouseLayoutServiceRestemplateClient.getWarehouseConfiguration(
                                billableRequest.getWarehouseId()
                );
                if (Objects.nonNull(warehouseConfiguration) && Boolean.TRUE.equals(warehouseConfiguration.getBillingRequestEnabledFlag())) {

                    logger.info("BillableRequest is enabled for warehouse {}", billableRequest.getWarehouseId());
                    billableRequestService.createBillableRequest(billableRequest);
                }
                else {

                    logger.info("BillableRequest is disabled for warehouse {}", billableRequest.getWarehouseId());
                }
            }
            else {
                logger.debug("warehouse id is not passed in, not possible to see if the billable function is enabled, we will process the billable reuqest any way");

                billableRequestService.createBillableRequest(billableRequest);
            }


        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }


    @KafkaListener(topics = {"BILLABLE_ACTIVITY"})
    public void processBillableActivity(@Payload String billableActivityJsonRepresent)  {
        logger.info("# received billable activity : {}", billableActivityJsonRepresent);

        try {
            BillableActivity billableActivity = objectMapper.readValue(billableActivityJsonRepresent, BillableActivity.class);
            logger.info("billableActivity: {}", billableActivity);
            billableActivityService.addBillableActivity(billableActivity);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }

}
