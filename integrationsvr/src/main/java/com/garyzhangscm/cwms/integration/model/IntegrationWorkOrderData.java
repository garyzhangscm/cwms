package com.garyzhangscm.cwms.integration.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface IntegrationWorkOrderData {

    Long getId();

    String getNumber();

    Long getCompanyId();

    String getCompanyCode();

    String getWarehouseName();

    Long getWarehouseId();

    Set<? extends IntegrationWorkOrderLineData> getWorkOrderLines();

    Set<? extends IntegrationWorkOrderInstructionData> getWorkOrderInstructions();

    Set<? extends IntegrationWorkOrderByProductData> getWorkOrderByProduct();

    Long getItemId();
    String getItemName();
    Long getExpectedQuantity();
    String getPoNumber();

    IntegrationStatus getStatus();



}
