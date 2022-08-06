package com.garyzhangscm.cwms.integration.model;

public interface IntegrationShipmentLineData {
    public Long getId();
    public Long getCompanyId();
    public String getCompanyCode();
    public String getWarehouseName();
    public Long getWarehouseId();
    public String getNumber();
    public IntegrationShipmentData getShipment();
    public Long getOrderLineId();

    public String getOrderLineNumber();
    public IntegrationStatus getStatus();

    public String getErrorMessage();
}
