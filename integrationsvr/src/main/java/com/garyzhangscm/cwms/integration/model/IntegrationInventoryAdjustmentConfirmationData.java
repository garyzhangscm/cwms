package com.garyzhangscm.cwms.integration.model;


public interface IntegrationInventoryAdjustmentConfirmationData {


    public Long getId() ;

    public Long getItemId() ;
    public String getItemName() ;

    // only needed when communication with quickbook
    // list id is the unique id with quickbook
    public String getQuickbookListId() ;

    public Long getWarehouseId() ;

    public String getWarehouseName();

    public Long getAdjustQuantity() ;

    public Long getInventoryStatusId() ;

    public String getInventoryStatusName();
    public Long getClientId() ;
    public String getClientName() ;

    public IntegrationStatus getStatus() ;


}
