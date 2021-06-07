package com.garyzhangscm.cwms.integration.model;

import java.time.LocalDateTime;

public interface IntegrationBillOfMaterialLineData {

    Long getId();

    String getNumber() ;
    Long getItemId() ;

    String getItemName() ;


    public Long getCompanyId();
    public String getCompanyCode() ;
    public Long getWarehouseId();
    public String getWarehouseName() ;

    Long getExpectedQuantity() ;

    Long getInventoryStatusId() ;

    String getInventoryStatusName() ;

    IntegrationBillOfMaterialData getBillOfMaterial();


    public IntegrationStatus getStatus() ;

    public LocalDateTime getInsertTime() ;

    public LocalDateTime getLastUpdateTime();
}
