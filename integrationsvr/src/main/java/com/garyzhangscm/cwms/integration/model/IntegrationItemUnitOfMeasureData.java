package com.garyzhangscm.cwms.integration.model;


public interface IntegrationItemUnitOfMeasureData {

    public Long getId();
    public Long getUnitOfMeasureId();

    public Integer getQuantity() ;
    public Double getWeight() ;
    public Double getLength() ;

    public Double getWidth() ;
    public Double getHeight() ;
    public DBBasedItemPackageType getItemPackageType();

    public String getUnitOfMeasureName();

    public Long getCompanyId();
    public String getCompanyCode() ;
    public Long getWarehouseId();
    public String getWarehouseName() ;

    public IntegrationStatus getStatus() ;

    public Long getItemId() ;

    public String getItemName();
    public Long getItemPackageTypeId() ;
    public String getItemPackageTypeName() ;
}
