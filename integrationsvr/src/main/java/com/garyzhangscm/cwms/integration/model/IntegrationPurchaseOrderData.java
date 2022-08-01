package com.garyzhangscm.cwms.integration.model;


import java.util.List;

public interface IntegrationPurchaseOrderData {

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

    List<? extends IntegrationPurchaseOrderLineData> getPurchaseOrderLines();
    Boolean getAllowUnexpectedItem();


    public IntegrationStatus getStatus() ;

    public void setStatus(IntegrationStatus status) ;

    public void setErrorMessage(String errorMessage) ;
}
