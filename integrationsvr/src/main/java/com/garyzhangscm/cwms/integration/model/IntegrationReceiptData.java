package com.garyzhangscm.cwms.integration.model;


import java.time.LocalDateTime;
import java.util.List;

public interface IntegrationReceiptData {

    Long getId();

    String getNumber();

    public Long getCompanyId();
    public String getCompanyCode() ;
    public Long getWarehouseId();
    public String getWarehouseName() ;

    Long getClientId();

    String getClientName();

    Long getSupplierId();

    String getSupplierName();

    List<? extends IntegrationReceiptLineData> getReceiptLines();
    Boolean getAllowUnexpectedItem();


    public IntegrationStatus getStatus() ;

    public LocalDateTime getInsertTime() ;

    public LocalDateTime getLastUpdateTime();
}
