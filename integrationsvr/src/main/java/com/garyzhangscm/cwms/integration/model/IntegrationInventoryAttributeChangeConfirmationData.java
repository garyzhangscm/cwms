package com.garyzhangscm.cwms.integration.model;



public interface IntegrationInventoryAttributeChangeConfirmationData {
    public Long getId() ;
    public Long getItemId();

    public String getItemName();

    public Long getWarehouseId() ;

    public String getWarehouseName() ;

    public Long getQuantity() ;

    public Long getInventoryStatusId();

    public String getInventoryStatusName();

    public Long getClientId() ;

    public String getClientName();

    public String getAttributeName();

    public String getOriginalValue() ;
    public String getNewValue() ;
    public IntegrationStatus getStatus();


}
