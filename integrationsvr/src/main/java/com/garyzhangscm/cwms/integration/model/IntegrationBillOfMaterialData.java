package com.garyzhangscm.cwms.integration.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface IntegrationBillOfMaterialData {

    Long getId();

    String getNumber();

    Long getCompanyId();

    String getCompanyCode();

    String getWarehouseName();

    Long getWarehouseId();

    Set<? extends IntegrationBillOfMaterialLineData> getBillOfMaterialLines();

    Set<? extends IntegrationWorkOrderInstructionTemplateData> getWorkOrderInstructionTemplates();

    Set<? extends IntegrationBillOfMaterialByProductData> getBillOfMaterialByProducts();
    Long getItemId();
    String getItemName();
    Long getExpectedQuantity();

    IntegrationStatus getStatus();



}
