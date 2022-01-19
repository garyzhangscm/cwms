package com.garyzhangscm.cwms.integration.model;


import java.time.LocalDateTime;

public interface IntegrationInventoryAdjustmentConfirmationData {


    public Long getId() ;

    public Long getItemId() ;
    public String getItemName() ;

    public Long getWarehouseId() ;

    public String getWarehouseName();

    public Long getAdjustQuantity() ;

    public Long getInventoryStatusId() ;

    public String getInventoryStatusName();
    public Long getClientId() ;
    public String getClientName() ;

    public IntegrationStatus getStatus() ;


}
