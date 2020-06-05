package com.garyzhangscm.cwms.integration.model;

import java.time.LocalDateTime;
import java.util.List;

public interface IntegrationReceiptConfirmationData {

    public Long getId();
    public String getNumber();

    public Long getWarehouseId();

    public String getWarehouseName();

    public Long getClientId();

    public String getClientName();

    public Long getSupplierId();

    public String getSupplierName();

    public List<? extends  IntegrationReceiptLineConfirmationData> getReceiptLines();

    public Boolean getAllowUnexpectedItem();

    public IntegrationStatus getStatus();

    public LocalDateTime getInsertTime();

    public LocalDateTime getLastUpdateTime();
}
