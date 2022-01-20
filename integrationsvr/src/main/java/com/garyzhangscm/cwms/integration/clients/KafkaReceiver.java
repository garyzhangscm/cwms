package com.garyzhangscm.cwms.integration.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.integration.model.*;

import com.garyzhangscm.cwms.integration.service.IntegrationDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;

import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Component

public class KafkaReceiver {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReceiver.class);

    // Custmoized JSON mapper
    @Autowired
    @Qualifier("getObjMapper")
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaSender kafkaSender;

    @Autowired
    private IntegrationDataService integrationDataService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public void processInventoryAdjustmentConfirmationIntegration( String inventoryAdjustmentConfirmationJsonRepresent)  {
        logger.info("# received inventory adjustment confirmation data: {}", inventoryAdjustmentConfirmationJsonRepresent);

        try {
            InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation
                    = objectMapper.readValue(inventoryAdjustmentConfirmationJsonRepresent, InventoryAdjustmentConfirmation.class);
            logger.info("InventoryAdjustmentConfirmation: {}", inventoryAdjustmentConfirmation);


            integrationDataService.sendInventoryAdjustmentConfirmationData(inventoryAdjustmentConfirmation);


        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }


    /**
     * We get feedback from business services that either successfully processed the integration data, or fail due to some reason
     * @param integrationResultJsonRepresent
     */
    @KafkaListener(topics = {"INTEGRATION_RESULT"})
    public void processIntegrationResult(@Payload String integrationResultJsonRepresent)  {
        logger.info("# received integration result data: {}", integrationResultJsonRepresent);

        try {
            IntegrationResult integrationResult
                    = objectMapper.readValue(integrationResultJsonRepresent, IntegrationResult.class);
            logger.info("integrationResult: {}", integrationResult);


            integrationDataService.saveIntegrationResult(integrationResult);
            sendIntegrationCompleteAlert(integrationResult);


        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }

    private void sendIntegrationCompleteAlert(IntegrationResult integrationResult) {
        // setup the company ID, if we haven't done so
        if (Objects.isNull(integrationResult.getCompanyId())
                && Objects.nonNull(integrationResult.getWarehouseId())) {
            integrationResult.setCompanyId(
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(
                            integrationResult.getWarehouseId()
                    ).getCompany().getId()
            );
            logger.debug("Get company id {} from warehouse id {}",
                    integrationResult.getCompanyId(), integrationResult.getWarehouseId());
        }
        Alert alert = integrationResult.isSuccess() ?
                new Alert(integrationResult.getCompanyId(),
                    AlertType.INTEGRATION_SUCCESS,
                    "INTEGRATION-" + integrationResult.getIntegrationType() + "-" + integrationResult.getIntegrationId(),
                    "Integration type " + integrationResult.getIntegrationType() +
                            ", id: " + integrationResult.getIntegrationId() + " succeed!",
                    "Integration Succeed: \n" +
                            "Type: " + integrationResult.getIntegrationType() + "\n" +
                            "Id: " + integrationResult.getIntegrationId() + "\n")
        :
                new Alert(integrationResult.getCompanyId(),
                    AlertType.INTEGRATION_FAIL,
                    "INTEGRATION-" + integrationResult.getIntegrationType() + "-" + integrationResult.getIntegrationId(),
                    "Integration type " + integrationResult.getIntegrationType() +
                            ", id: " + integrationResult.getIntegrationId() + " fail!",
                    "Integration Fail: \n" +
                            "Type: " + integrationResult.getIntegrationType() + "\n" +
                            "Id: " + integrationResult.getIntegrationId() + "\n" +
                            "Error: " + integrationResult.getErrorMessage());


        kafkaSender.send(alert);
    }

    public void processInventoryAttributeChangeConfirmationIntegration( String inventoryAttributeChangeConfirmationJsonRepresent)  {
        logger.info("# received inventory attribute change confirmation data: {}", inventoryAttributeChangeConfirmationJsonRepresent);

        try {
            InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation
                    = objectMapper.readValue(inventoryAttributeChangeConfirmationJsonRepresent, InventoryAttributeChangeConfirmation.class);
            logger.info("inventoryAttributeChangeConfirmation: {}", inventoryAttributeChangeConfirmation);


            integrationDataService.sendInventoryAttributeChangeConfirmationData(inventoryAttributeChangeConfirmation);

        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }



    public void processOrderConfirmationIntegration( String orderConfirmationJsonRepresent)  {
        logger.info("# received order confirmation data: {}", orderConfirmationJsonRepresent);

        try {
            OrderConfirmation orderConfirmation
                    = objectMapper.readValue(orderConfirmationJsonRepresent, OrderConfirmation.class);
            logger.info("orderConfirmation: {}", orderConfirmation);


            integrationDataService.sendIntegrationOrderConfirmationData(orderConfirmation);


        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }


    public void processWorkOrderConfirmationIntegration(String workOrderConfirmationJsonRepresent)  {
        logger.info("# received work order confirmation data: {}", workOrderConfirmationJsonRepresent);

        try {
            WorkOrderConfirmation workOrderConfirmation
                    = objectMapper.readValue(workOrderConfirmationJsonRepresent, WorkOrderConfirmation.class);
            logger.info("workOrderConfirmation: {}", workOrderConfirmation);


            integrationDataService.sendIntegrationWorkOrderConfirmationData(workOrderConfirmation);


        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }

    public void processReceiptConfirmationIntegration(String receiptConfirmationJsonRepresent)  {
        logger.info("# received inventory adjustment confirmation data: {}", receiptConfirmationJsonRepresent);

        try {
            ReceiptConfirmation receiptConfirmation
                    = objectMapper.readValue(receiptConfirmationJsonRepresent, ReceiptConfirmation.class);
            logger.info("receiptConfirmation: {}", receiptConfirmation);


            integrationDataService.sendIntegrationReceiptConfirmationData(receiptConfirmation);


        }
        catch (JsonProcessingException ex) {
            logger.debug("JsonProcessingException: {}", ex.getMessage());
        }

    }

    @KafkaListener(topics = {"INTEGRATION_CONFIRMATION"})
    public void processConfirmationIntegration(@Payload String confirmationJsonRepresent,
                                               @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String integrationTypeJsonRepresent)  {
        logger.info("# received confirmation data of type {} :\n {}",
                integrationTypeJsonRepresent, confirmationJsonRepresent);

        IntegrationType integrationType = IntegrationType.valueOf(integrationTypeJsonRepresent);
        switch (integrationType) {
            case INTEGRATION_INVENTORY_ADJUSTMENT_CONFIRMATION:
                processInventoryAdjustmentConfirmationIntegration(confirmationJsonRepresent);
                break;
            case INTEGRATION_INVENTORY_ATTRIBUTE_CHANGE_CONFIRMATION:
                processInventoryAttributeChangeConfirmationIntegration(confirmationJsonRepresent);
                break;
            case INTEGRATION_ORDER_CONFIRMATION:
                processOrderConfirmationIntegration(confirmationJsonRepresent);
                break;
            case INTEGRATION_RECEIPT_CONFIRMATION:
                processReceiptConfirmationIntegration(confirmationJsonRepresent);
                break;
            case INTEGRATION_WORK_ORDER_CONFIRMATION:
                processWorkOrderConfirmationIntegration(confirmationJsonRepresent);
                break;
            case INTEGRATION_CUSTOMER_RETURN_ORDER_CONFIRMATION:
                processCustomerReturnOrderConfirmationIntegration(confirmationJsonRepresent);
                break;
            default:
                logger.debug("==> {} is not supported!!!!", integrationTypeJsonRepresent);
        }

    }

    private void processCustomerReturnOrderConfirmationIntegration(String confirmationJsonRepresent) {
        throw new UnsupportedOperationException("customer return order configmration integratino is not support yet!!!");
    }
}
