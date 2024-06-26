package com.garyzhangscm.cwms.integration.model;


public interface IntegrationWorkOrderByProductData {

    Long getId();

    Long getItemId() ;

    String getItemName() ;


    public Long getCompanyId();
    public String getCompanyCode() ;
    public Long getWarehouseId();
    public String getWarehouseName() ;

    Long getExpectedQuantity() ;

    Long getInventoryStatusId() ;

    String getInventoryStatusName() ;

    IntegrationWorkOrderData getWorkOrder();


    public IntegrationStatus getStatus() ;


}
