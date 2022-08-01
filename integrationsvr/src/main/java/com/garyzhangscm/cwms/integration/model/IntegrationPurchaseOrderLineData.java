package com.garyzhangscm.cwms.integration.model;


public interface IntegrationPurchaseOrderLineData {

    Long getId();

    String getNumber();
    IntegrationPurchaseOrderData getPurchaseOrder();

    public Long getCompanyId();
    public String getCompanyCode() ;
    public Long getWarehouseId();
    public String getWarehouseName() ;

    Long getItemId() ;

    String getItemName();

    Long getExpectedQuantity() ;


    public IntegrationStatus getStatus() ;

}
