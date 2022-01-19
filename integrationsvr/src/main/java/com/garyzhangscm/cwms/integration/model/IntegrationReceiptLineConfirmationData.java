package com.garyzhangscm.cwms.integration.model;

import java.time.LocalDateTime;

public interface IntegrationReceiptLineConfirmationData {

    public Long getId();

    public String getNumber();

    public IntegrationReceiptConfirmationData getReceipt();

    public Long getWarehouseId();

    public String getWarehouseName();

    public Long getItemId();

    public String getItemName();

    public Long getExpectedQuantity();

    public Long getReceivedQuantity();

    public Long getOverReceivingQuantity();

    public Double getOverReceivingPercent();

    public IntegrationStatus getStatus();

}
