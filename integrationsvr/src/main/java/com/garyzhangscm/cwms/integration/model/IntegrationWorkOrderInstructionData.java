package com.garyzhangscm.cwms.integration.model;


public interface IntegrationWorkOrderInstructionData {

    Long getId();

    Integer getSequence();

    String getInstruction();



    IntegrationWorkOrderData getWorkOrder();


    public IntegrationStatus getStatus() ;


}
