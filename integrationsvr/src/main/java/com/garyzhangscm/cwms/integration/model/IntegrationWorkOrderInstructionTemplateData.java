package com.garyzhangscm.cwms.integration.model;

import java.time.LocalDateTime;

public interface IntegrationWorkOrderInstructionTemplateData {

    Long getId();

    Integer getSequence();

    String getInstruction();


    IntegrationBillOfMaterialData getBillOfMaterial();


    public IntegrationStatus getStatus() ;

}
