package com.garyzhangscm.cwms.integration.model;


public interface IntegrationItemFamilyData {

    public Long getId() ;
    public String getName();
    public String getDescription();

    public Long getCompanyId();
    public String getCompanyCode() ;
    public Long getWarehouseId();
    public String getWarehouseName() ;

    public IntegrationStatus getStatus();

}
