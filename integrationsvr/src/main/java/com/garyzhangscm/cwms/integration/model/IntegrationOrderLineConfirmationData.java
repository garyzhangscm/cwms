package com.garyzhangscm.cwms.integration.model;



public interface IntegrationOrderLineConfirmationData {

    public Long getId();
    public String getNumber();

    public Long getItemId();

    public String getItemName();
    public Long getWarehouseId();
    public String getWarehouseName();

    public Long getExpectedQuantity();

    public Long getOpenQuantity();

    public Long getInprocessQuantity();

    public Long getShippedQuantity();

    public Long getInventoryStatusId();

    public String getInventoryStatusName();

    public IntegrationOrderConfirmationData getOrder();

    public Long getCarrierId();

    public String getCarrierName();
    public Long getCarrierServiceLevelId();
    public String getQuickbookTxnLineID();

    public String getCarrierServiceLevelName();

    public IntegrationStatus getStatus();

}
