package com.garyzhangscm.cwms.integration.model;


import java.time.LocalDateTime;
import java.util.List;

public interface IntegrationWorkOrderLineConfirmationData {

    public Long getId();
    public String getNumber() ;
    public Long getItemId();
    public String getItemName();
    public Long getExpectedQuantity();
    public Long getOpenQuantity();
    public Long getInprocessQuantity();
    public Long getDeliveredQuantity();
    public Long getConsumedQuantity();

    public Long getScrappedQuantity() ;

    public Long getReturnedQuantity();
    public Long getInventoryStatusId();
    public String getInventoryStatusName();

    public IntegrationWorkOrderConfirmationData getWorkOrderConfirmation();

    public IntegrationStatus getStatus() ;

}
