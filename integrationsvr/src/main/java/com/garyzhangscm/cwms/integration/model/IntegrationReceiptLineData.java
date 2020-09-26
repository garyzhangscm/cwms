package com.garyzhangscm.cwms.integration.model;


import java.time.LocalDateTime;

public interface IntegrationReceiptLineData {

    Long getId();

    String getNumber();
    IntegrationReceiptData getReceipt();

    public Long getCompanyId();
    public String getCompanyCode() ;
    public Long getWarehouseId();
    public String getWarehouseName() ;

    Long getItemId() ;

    String getItemName();

    Long getExpectedQuantity() ;

    Long getOverReceivingQuantity();

    Double getOverReceivingPercent();


    public IntegrationStatus getStatus() ;

    public LocalDateTime getInsertTime() ;

    public LocalDateTime getLastUpdateTime();
}
