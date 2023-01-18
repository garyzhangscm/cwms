package com.garyzhangscm.cwms.integration.model;

import java.util.List;

public interface IntegrationWorkOrderConfirmationData {

    public Long getId();

    public String getNumber();
    public String getProductionLineName() ;
    public Long getItemId();

    public String getItemName() ;

    public Long getWarehouseId();
    public String getWarehouseName();

    public String getBillOfMaterialName();

    public Long getExpectedQuantity();
    public Long getProducedQuantity();

    public List<? extends IntegrationWorkOrderLineConfirmationData> getWorkOrderLineConfirmations();

    public List<? extends IntegrationWorkOrderByProductConfirmationData> getWorkOrderByProductConfirmations();

    public IntegrationStatus getStatus() ;

}
