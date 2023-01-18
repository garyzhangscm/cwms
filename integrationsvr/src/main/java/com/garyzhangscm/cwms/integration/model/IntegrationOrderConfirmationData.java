package com.garyzhangscm.cwms.integration.model;


import java.util.List;

public interface IntegrationOrderConfirmationData {

    public Long getId();

    public String getNumber();

    public Long getWarehouseId() ;

    public String getWarehouseName();


    public List<? extends IntegrationOrderLineConfirmationData> getOrderLines() ;

    public IntegrationStatus getStatus() ;

}
