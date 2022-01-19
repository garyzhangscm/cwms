package com.garyzhangscm.cwms.integration.model;


import java.time.LocalDateTime;

public interface IntegrationWorkOrderByProductConfirmationData {

    public Long getId();

    public Long getItemId();
    public String getItemName();
    public Long getExpectedQuantity();
    public Long getProducedQuantity();

    public Long getInventoryStatusId();
    public String getInventoryStatusName();

    public IntegrationWorkOrderConfirmationData getWorkOrderConfirmation();

    public IntegrationStatus getStatus() ;

}
