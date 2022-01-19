package com.garyzhangscm.cwms.integration.model;

import java.time.LocalDateTime;

public interface IntegrationWorkOrderInstructionData {

    Long getId();

    Integer getSequence();

    String getInstruction();



    IntegrationWorkOrderData getWorkOrder();


    public IntegrationStatus getStatus() ;


}
