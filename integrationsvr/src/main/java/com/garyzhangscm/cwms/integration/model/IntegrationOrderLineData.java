package com.garyzhangscm.cwms.integration.model;

import java.time.LocalDateTime;

public interface IntegrationOrderLineData {

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

    IntegrationOrderData getOrder() ;

    Long getCarrierId() ;

    String getCarrierName() ;

    Long getCarrierServiceLevelId() ;

    String getCarrierServiceLevelName() ;

    public IntegrationStatus getStatus() ;


}
