package com.garyzhangscm.cwms.integration.model;


public interface IntegrationWorkOrderInstructionTemplateData {

    Long getId();

    Integer getSequence();

    String getInstruction();


    IntegrationBillOfMaterialData getBillOfMaterial();


    public IntegrationStatus getStatus() ;

}
