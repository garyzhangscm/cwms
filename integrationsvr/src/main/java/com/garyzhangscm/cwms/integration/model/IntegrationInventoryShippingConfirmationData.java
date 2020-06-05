package com.garyzhangscm.cwms.integration.model;


import java.time.LocalDateTime;

/**
 * Outbound shipping confirmation integration at inventory / LPN level
 */

public interface IntegrationInventoryShippingConfirmationData {


    public Long getId();
    public String getShipmentNumber();

    public String getShipmentLineNumber();

    public String getOrderNumber();

    public String getOrderLineNumber();

    public Long getItemId();

    public String getItemName();

    public Long getWarehouseId();

    public String getWarehouseName();
    public Long getOrderExpectedQuantity();

    public Long getOrderShippedQuantity();

    public Long getShipmentShippedQuantity();
    public Long getInventoryStatusId();
    public String getInventoryStatusName();
    public Long getCarrierId();
    public String getCarrierName();
    public Long getCarrierServiceLevelId();
    public String getCarrierServiceLevelName();
    public IntegrationStatus getStatus();
    public LocalDateTime getInsertTime();
    public LocalDateTime getLastUpdateTime();
    public String getLpn();

    public Long getQuantity();
}
